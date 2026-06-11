package com.nexapos.retail.data.branch

import android.content.Context
import com.nexapos.retail.data.security.SecureStore

/**
 * The customer's Firebase project connection details, entered once in Settings
 * and stored in the Keystore-backed encrypted prefs. The app builds a NAMED
 * Firebase app from these (never the default), so there is no google-services.json
 * or google-services plugin, the build needs no Firebase config, and an
 * unconfigured install never touches Firebase.
 *
 * The three values are copied straight from the Firebase console
 * (Project settings → General → "Your apps" → the Android app): project id,
 * app id ("1:NNN:android:HEX"), and the Web/Android API key.
 */
object FirebaseConfig {
    private const val KEY_PROJECT = "fb_project_id"
    private const val KEY_APP_ID = "fb_app_id"
    private const val KEY_API_KEY = "fb_api_key"
    private const val KEY_EMAIL = "fb_email"

    data class Config(
        val projectId: String,
        val appId: String,
        val apiKey: String,
    )

    fun config(context: Context): Config? {
        val p = SecureStore.prefs(context)
        val projectId = p.getString(KEY_PROJECT, "").orEmpty().trim()
        val appId = p.getString(KEY_APP_ID, "").orEmpty().trim()
        val apiKey = p.getString(KEY_API_KEY, "").orEmpty().trim()
        return if (projectId.isNotBlank() && appId.isNotBlank() && apiKey.isNotBlank()) {
            Config(projectId, appId, apiKey)
        } else {
            null
        }
    }

    /** The business account email last used to sign in. */
    fun email(context: Context): String = SecureStore.prefs(context).getString(KEY_EMAIL, "").orEmpty()

    fun isConfigured(context: Context): Boolean = config(context) != null

    fun save(
        context: Context,
        projectId: String,
        appId: String,
        apiKey: String,
        email: String,
    ) {
        SecureStore.prefs(context).edit()
            .putString(KEY_PROJECT, projectId.trim())
            .putString(KEY_APP_ID, appId.trim())
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_EMAIL, email.trim())
            .apply()
    }

    fun clear(context: Context) {
        SecureStore.prefs(context).edit()
            .remove(KEY_PROJECT)
            .remove(KEY_APP_ID)
            .remove(KEY_API_KEY)
            .remove(KEY_EMAIL)
            .apply()
    }
}
