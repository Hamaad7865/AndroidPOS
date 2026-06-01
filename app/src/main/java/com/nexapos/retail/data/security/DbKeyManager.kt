package com.nexapos.retail.data.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

/**
 * Generates once, then persists, the passphrase used to encrypt the SQLCipher
 * database. Stored only inside [SecureStore] (Keystore-backed).
 *
 * It is a *passphrase* (not a raw key) so the encrypted database file is portable:
 * SQLCipher writes its key-derivation salt into the file header, so any install
 * given the same passphrase can open a restored backup. The passphrase is shown
 * to the owner in Settings → Security so they can keep it for disaster recovery.
 */
object DbKeyManager {
    private const val KEY = "db_passphrase"
    private const val PASSPHRASE_BYTES = 24

    fun getOrCreatePassphrase(context: Context): String {
        val prefs = SecureStore.prefs(context)
        prefs.getString(KEY, null)?.let { return it }
        val raw = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val passphrase = Base64.encodeToString(raw, Base64.NO_WRAP)
        prefs.edit().putString(KEY, passphrase).apply()
        return passphrase
    }

    /** Overwrites the stored passphrase (used when restoring a backup from another install). */
    fun setPassphrase(
        context: Context,
        passphrase: String,
    ) {
        SecureStore.prefs(context).edit().putString(KEY, passphrase).apply()
    }
}
