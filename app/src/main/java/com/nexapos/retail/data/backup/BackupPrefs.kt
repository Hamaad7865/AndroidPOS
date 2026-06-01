package com.nexapos.retail.data.backup

import android.content.Context

/** Non-secret backup settings: the chosen destination folder and last run time. */
object BackupPrefs {
    private const val PREFS = "nexapos_backup"
    private const val KEY_FOLDER = "folder_uri"
    private const val KEY_LAST = "last_backup_at"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun folderUri(context: Context): String? = prefs(context).getString(KEY_FOLDER, null)

    fun setFolderUri(
        context: Context,
        uri: String,
    ) = prefs(context).edit().putString(KEY_FOLDER, uri).apply()

    fun lastBackupAt(context: Context): Long = prefs(context).getLong(KEY_LAST, 0L)

    fun setLastBackupAt(
        context: Context,
        ts: Long,
    ) = prefs(context).edit().putLong(KEY_LAST, ts).apply()
}
