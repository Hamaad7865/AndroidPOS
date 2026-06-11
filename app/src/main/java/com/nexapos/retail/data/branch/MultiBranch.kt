package com.nexapos.retail.data.branch

import android.content.Context
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.security.SecureStore

/**
 * Multi-branch add-on state. The accepted licence code is stored in the
 * Keystore-backed encrypted prefs and RE-VERIFIED on every read, so editing the
 * prefs file can never fake an unlock — only a validly-signed code for THIS
 * business name passes [LicenseManager.verify].
 */
object MultiBranch {
    private const val KEY_LICENSE = "mb_license_code"

    /** The active licence for this business, or null if unlicensed / invalid / expired. */
    fun license(context: Context): LicenseManager.License? {
        val code = SecureStore.prefs(context).getString(KEY_LICENSE, null) ?: return null
        return LicenseManager.verify(code, BusinessProfile.name(context))
    }

    /** True when a valid multi-branch licence is installed for this business. */
    fun licensed(context: Context): Boolean = license(context) != null

    /**
     * Validates [code] for this business and, if it passes, stores it.
     * Returns the parsed [LicenseManager.License] on success, or null if the code
     * is invalid for this business (nothing is stored in that case).
     */
    fun activate(
        context: Context,
        code: String,
    ): LicenseManager.License? {
        val license = LicenseManager.verify(code, BusinessProfile.name(context)) ?: return null
        SecureStore.prefs(context).edit().putString(KEY_LICENSE, code.trim()).apply()
        return license
    }

    /** Removes the stored licence (re-locks the add-on). */
    fun deactivate(context: Context) {
        SecureStore.prefs(context).edit().remove(KEY_LICENSE).apply()
    }
}
