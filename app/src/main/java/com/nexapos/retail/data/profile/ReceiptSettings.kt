package com.nexapos.retail.data.profile

import android.content.Context

/**
 * Receipt / printer preferences, configured in Settings → Printing & receipt.
 *
 * Printing on Android (sideloaded, no Play Store) goes through the OS print
 * framework, which works with any printer that has an Android print service
 * installed (most network/Wi-Fi printers, Mopria, and many thermal printers via
 * their vendor app) and always offers "Save as PDF". These settings shape the
 * rendered receipt — paper width and an optional footer line — and are read by
 * the receipt's Print / PDF actions.
 */
object ReceiptSettings {
    private const val PREFS = "nexapos_receipt"
    private const val KEY_PAPER = "paper_width"
    private const val KEY_FOOTER = "footer_note"

    /** Paper presets. Width in CSS millimetres used by the print HTML. */
    enum class Paper(val id: String, val label: String, val cssWidthMm: Int) {
        MM58("58mm", "58 mm thermal", 58),
        MM80("80mm", "80 mm thermal", 80),
        A4("a4", "A4 sheet", 190),
        ;

        companion object {
            fun from(id: String?): Paper = entries.firstOrNull { it.id == id } ?: MM80
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun paper(context: Context): Paper = Paper.from(prefs(context).getString(KEY_PAPER, Paper.MM80.id))

    fun footerNote(context: Context): String =
        prefs(context).getString(KEY_FOOTER, DEFAULT_FOOTER) ?: DEFAULT_FOOTER

    fun setPaper(
        context: Context,
        paper: Paper,
    ) = prefs(context).edit().putString(KEY_PAPER, paper.id).apply()

    fun setFooterNote(
        context: Context,
        note: String,
    ) = prefs(context).edit().putString(KEY_FOOTER, note).apply()

    const val DEFAULT_FOOTER = "Goods sold are not refundable. Thank you for shopping with us."
}
