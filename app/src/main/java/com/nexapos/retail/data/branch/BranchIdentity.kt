package com.nexapos.retail.data.branch

import android.content.Context
import com.nexapos.retail.data.security.SecureStore

/**
 * This install's identity in the multi-branch network: whether it is the head
 * office or a branch, its short branch code (e.g. "A", "HQ", "CUR1"), and a
 * display name. Stored in the Keystore-backed encrypted prefs, alongside the
 * licence. Independent of the licence: an install is only "configured" for sync
 * once a code is set.
 */
object BranchIdentity {
    private const val KEY_ROLE = "mb_role"
    private const val KEY_CODE = "mb_code"
    private const val KEY_NAME = "mb_name"

    enum class Role { HQ, BRANCH }

    fun role(context: Context): Role =
        if (SecureStore.prefs(context).getString(KEY_ROLE, null) == Role.HQ.name) Role.HQ else Role.BRANCH

    /** This install's branch code, or "" if not yet configured. */
    fun code(context: Context): String = SecureStore.prefs(context).getString(KEY_CODE, "") ?: ""

    fun name(context: Context): String = SecureStore.prefs(context).getString(KEY_NAME, "") ?: ""

    /** True once a non-blank branch code has been set. */
    fun isConfigured(context: Context): Boolean = code(context).isNotBlank()

    fun set(
        context: Context,
        role: Role,
        code: String,
        name: String,
    ) {
        SecureStore.prefs(context).edit()
            .putString(KEY_ROLE, role.name)
            .putString(KEY_CODE, normalizeCode(code))
            .putString(KEY_NAME, name.trim())
            .apply()
    }

    fun clear(context: Context) {
        SecureStore.prefs(context).edit()
            .remove(KEY_ROLE)
            .remove(KEY_CODE)
            .remove(KEY_NAME)
            .apply()
    }

    /**
     * Canonical branch code: upper-case alphanumerics only, max 4 chars. Used as
     * a Firestore document id, so it must be filesystem/path-safe and stable.
     */
    fun normalizeCode(raw: String): String = raw.uppercase().filter { it.isLetterOrDigit() }.take(4)
}
