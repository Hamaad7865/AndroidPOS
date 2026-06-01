package com.nexapos.retail.data.profile

import android.content.Context

/**
 * Tiny key-value store for shop-identity strings shown on the Settings screen,
 * the POS app bar, and printed receipts. Each field is stored separately so
 * receipts and settings can format them however they want.
 *
 * Defaults to placeholders so a fresh install shows "Your Business" until the
 * owner sets it.
 */
object BusinessProfile {
    private const val PREFS = "nexapos_business"
    private const val KEY_NAME = "name"
    private const val KEY_ADDRESS = "address"
    private const val KEY_BRN = "brn"
    private const val KEY_VAT = "vat"
    private const val KEY_TAGLINE_LEGACY = "tagline" // legacy combined field from earlier builds

    const val DEFAULT_NAME = "Your Business"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun name(context: Context): String =
        prefs(context).getString(KEY_NAME, "")?.ifBlank { DEFAULT_NAME } ?: DEFAULT_NAME

    fun address(context: Context): String = prefs(context).getString(KEY_ADDRESS, "") ?: ""

    fun brn(context: Context): String = prefs(context).getString(KEY_BRN, "") ?: ""

    fun vat(context: Context): String = prefs(context).getString(KEY_VAT, "") ?: ""

    /** True once a business name is set. */
    fun isConfigured(context: Context): Boolean = !prefs(context).getString(KEY_NAME, "").isNullOrBlank()

    /** Combined display string for the receipt header — address line + BRN/VAT line. */
    fun receiptLines(context: Context): List<String> {
        val p = prefs(context)
        val address = p.getString(KEY_ADDRESS, "")?.trim().orEmpty()
        val brn = p.getString(KEY_BRN, "")?.trim().orEmpty()
        val vat = p.getString(KEY_VAT, "")?.trim().orEmpty()
        // Fall back to the legacy single "tagline" if individual fields are blank — keeps
        // older installs rendering until the owner re-saves in the new form.
        if (address.isBlank() && brn.isBlank() && vat.isBlank()) {
            val legacy = p.getString(KEY_TAGLINE_LEGACY, "")?.trim().orEmpty()
            return if (legacy.isNotEmpty()) listOf(legacy) else emptyList()
        }
        val identifiers =
            listOfNotNull(
                brn.takeIf { it.isNotEmpty() }?.let { "BRN $it" },
                vat.takeIf { it.isNotEmpty() }?.let { "VAT $it" },
            ).joinToString(" · ")
        return listOfNotNull(
            address.takeIf { it.isNotEmpty() },
            identifiers.takeIf { it.isNotEmpty() },
        )
    }

    fun setProfile(
        context: Context,
        name: String,
        address: String,
        brn: String,
        vat: String,
    ) {
        prefs(context).edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_ADDRESS, address.trim())
            .putString(KEY_BRN, brn.trim())
            .putString(KEY_VAT, vat.trim())
            // Clear the legacy combined field so receiptLines() prefers the new ones.
            .remove(KEY_TAGLINE_LEGACY)
            .apply()
    }
}
