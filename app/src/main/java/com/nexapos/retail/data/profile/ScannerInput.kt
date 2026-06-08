package com.nexapos.retail.data.profile

import android.content.Context

/**
 * External (USB/Bluetooth HID) barcode-scanner preferences, configured in
 * Settings → Barcode scanner. Read on every hardware key event in MainActivity.
 */
object ScannerInput {
    private const val PREFS = "nexapos_scanner"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TERMINATOR = "terminator"

    /** Which key a scanner sends after the barcode. Most send Enter; some send Tab. */
    enum class Terminator(val id: String, val label: String) {
        ENTER("enter", "Enter only"),
        TAB("tab", "Tab only"),
        BOTH("both", "Either (Enter or Tab)"),
        ;

        companion object {
            fun from(id: String?): Terminator = entries.firstOrNull { it.id == id } ?: BOTH
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun terminator(context: Context): Terminator =
        Terminator.from(prefs(context).getString(KEY_TERMINATOR, Terminator.BOTH.id))

    fun setEnabled(
        context: Context,
        value: Boolean,
    ) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun setTerminator(
        context: Context,
        value: Terminator,
    ) = prefs(context).edit().putString(KEY_TERMINATOR, value.id).apply()
}
