package com.nexapos.retail.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * A single Android-Keystore-backed encrypted preferences store for app secrets:
 * the SQLCipher database passphrase and hashed staff PINs. The master key never
 * leaves the device's hardware-backed keystore.
 */
object SecureStore {
    private const val PREFS_NAME = "nexapos_secure"

    fun prefs(context: Context): SharedPreferences {
        val app = context.applicationContext
        val masterKey =
            MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        return EncryptedSharedPreferences.create(
            app,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
