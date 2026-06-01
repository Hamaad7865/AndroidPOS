package com.nexapos.retail.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.security.DbKeyManager
import net.sqlcipher.database.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device backup and restore of the encrypted database.  A backup is simply a
 * copy of the SQLCipher database file written to a user-chosen folder via the
 * Storage Access Framework (works with a USB/SD card or a Google-Drive-synced
 * folder — no Google Cloud project, no recurring fees).  Because the DB is
 * encrypted with a portable passphrase (see [com.nexapos.retail.data.security.DbKeyManager]),
 * the copy is unreadable without that passphrase and restorable on any device.
 *
 * **Provenance**: a valid backup must be openable by SQLCipher with the current
 * passphrase and pass `PRAGMA cipher_integrity_check`.  Any file that fails this
 * check (foreign file, truncated download, wrong passphrase) is rejected during
 * restore — the live database is never touched.
 */
object BackupManager {
    const val DB_NAME = "nexapos.db"
    private const val RESTORE_NAME = "nexapos.db.restore"
    private const val RESTORE_TEMP_NAME = "nexapos.db.restore.tmp"
    private const val KEY_RESTORE_ERROR = "restore_failed"
    private val stampFmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    private fun dbFile(context: Context): File = context.getDatabasePath(DB_NAME)

    // -------------------------------------------------------------------------
    // Restore error marker — written when validation fails so the UI can surface
    // a one-shot "last restore failed" banner.
    // -------------------------------------------------------------------------

    private fun restoreErrorPrefs(context: Context) =
        context.getSharedPreferences("nexapos_restore_status", Context.MODE_PRIVATE)

    /** Returns true (and clears the flag) if the last restore attempt failed. */
    fun consumeRestoreError(context: Context): Boolean {
        val prefs = restoreErrorPrefs(context)
        val failed = prefs.getBoolean(KEY_RESTORE_ERROR, false)
        if (failed) prefs.edit().remove(KEY_RESTORE_ERROR).apply()
        return failed
    }

    private fun markRestoreError(context: Context) {
        restoreErrorPrefs(context).edit().putBoolean(KEY_RESTORE_ERROR, true).apply()
    }

    // -------------------------------------------------------------------------
    // Apply pending restore
    // -------------------------------------------------------------------------

    /**
     * If a restore file was staged on a previous run, validate it then swap it
     * in as the live DB.  Must be called in Application.onCreate BEFORE Room
     * opens the database.
     *
     * Validation: opens the staged file with SQLCipher using the current
     * passphrase from [DbKeyManager] and runs `PRAGMA cipher_integrity_check`.
     * If the check fails (wrong passphrase, corrupt file, foreign file) the live
     * DB is left untouched, the staged file is deleted, and a one-shot error
     * flag is written for the UI to surface.
     *
     * @return true if the live DB was replaced (app should re-open its DB).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun applyPendingRestore(context: Context): Boolean {
        val staged = context.getDatabasePath(RESTORE_NAME)
        if (!staged.exists()) return false

        val passphrase = DbKeyManager.getOrCreatePassphrase(context)
        val isValid = validateDbFile(context, staged, passphrase)

        if (!isValid) {
            staged.delete()
            markRestoreError(context)
            return false
        }

        val live = dbFile(context)
        val temp = context.getDatabasePath(RESTORE_TEMP_NAME)

        try {
            // WAL files from the live DB must be cleared before we replace the file.
            File("${live.path}-wal").delete()
            File("${live.path}-shm").delete()

            // Copy staged → temp, then atomically rename into place.
            staged.copyTo(temp, overwrite = true)
            temp.renameTo(live)
            return true
        } catch (e: Exception) {
            temp.delete()
            markRestoreError(context)
            return false
        } finally {
            staged.delete()
        }
    }

    // -------------------------------------------------------------------------
    // Backup
    // -------------------------------------------------------------------------

    /**
     * Writes a timestamped copy of the database into [treeUri]; returns the file
     * name.  A WAL checkpoint is flushed first so the copy is consistent.
     *
     * The resulting file is a standard SQLCipher database encrypted with the
     * device passphrase.  Its authenticity on restore is proved by successfully
     * decrypting it with that passphrase — no separate signature needed.
     */
    fun backupNow(
        context: Context,
        treeUri: Uri,
    ): String {
        (context.applicationContext as PosApplication).container.checkpoint()
        val resolver = context.contentResolver
        val parent =
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
        val name = "nexapos-${stampFmt.format(Date())}.db"
        val docUri =
            DocumentsContract.createDocument(resolver, parent, "application/octet-stream", name)
                ?: error("Could not create the backup file in that folder.")
        resolver.openOutputStream(docUri)?.use { out ->
            dbFile(context).inputStream().use { input -> input.copyTo(out) }
        } ?: error("Could not write the backup file.")
        BackupPrefs.setLastBackupAt(context, System.currentTimeMillis())
        return name
    }

    // -------------------------------------------------------------------------
    // Stage restore
    // -------------------------------------------------------------------------

    /** Copies a chosen backup file into place to be applied on the next launch. */
    fun stageRestore(
        context: Context,
        fileUri: Uri,
    ) {
        val staged = context.getDatabasePath(RESTORE_NAME)
        context.contentResolver.openInputStream(fileUri)?.use { input ->
            staged.outputStream().use { input.copyTo(it) }
        } ?: error("Could not read that backup file.")
    }

    // -------------------------------------------------------------------------
    // Restart helper
    // -------------------------------------------------------------------------

    /** Relaunches the app so a staged restore (or settings change) takes effect. */
    fun restart(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    // -------------------------------------------------------------------------
    // Private: SQLCipher validation
    // -------------------------------------------------------------------------

    /**
     * Opens [file] with SQLCipher using [passphrase] and runs
     * `PRAGMA cipher_integrity_check`.  Returns true only when the file decrypts
     * successfully and the check returns "ok".
     *
     * SQLCipher native libs must already be loaded (done in PosApplication before
     * this code runs).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun validateDbFile(
        context: Context,
        file: File,
        passphrase: String,
    ): Boolean {
        SQLiteDatabase.loadLibs(context)
        return try {
            val db =
                SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    passphrase,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                )
            db.use { database ->
                val cursor = database.rawQuery("PRAGMA cipher_integrity_check", null)
                cursor.use { c ->
                    c.moveToFirst() && c.getString(0) == "ok"
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
