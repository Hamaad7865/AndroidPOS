package com.nexapos.retail.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Money is stored throughout the app as [Long] minor units (cents) to avoid
 * floating-point rounding errors. This object handles display formatting.
 */
object Money {
    /** Currency symbol shown in the UI. Change here to localize (e.g. "$", "€", "₨"). */
    const val CURRENCY_SYMBOL: String = "Rs"

    private val formatter: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    /** Formats minor units to a display string, e.g. 125000 -> "Rs 1,250.00". */
    fun format(cents: Long): String = "$CURRENCY_SYMBOL ${formatter.format(cents / 100.0)}"

    /** Parses a major-unit string like "12.50" into minor units (1250). Returns null if invalid. */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100)
    }

    /**
     * Plain editable string (no symbol) for prefilling a money input field:
     * 750 -> "7.5", 700 -> "7", 705 -> "7.05", 0 -> "". The inverse of
     * [parseToCents] for round-tripping an existing amount into a text field.
     */
    fun toInput(cents: Long): String {
        if (cents <= 0L) return ""
        val whole = cents / 100
        val frac = (cents % 100).toInt()
        return if (frac == 0) whole.toString() else "%d.%02d".format(whole, frac).trimEnd('0')
    }
}
