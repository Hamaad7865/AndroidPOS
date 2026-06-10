package com.nexapos.retail.util

/** Leading characters Excel/Sheets treat as the start of a formula. */
private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@')

/**
 * Escapes a value for a CSV cell (RFC-4180 quoting) and neutralises spreadsheet
 * **formula injection**: a cell beginning with `= + - @` is executed on open in
 * Excel / Google Sheets, so user-controlled text (customer/supplier/product
 * names, notes) starting with one of those is prefixed with a single quote.
 * Plain numbers — including negatives like `-50` — are left untouched so numeric
 * columns still parse.
 */
internal fun csvCell(raw: String): String {
    val guarded =
        if (raw.isNotEmpty() && raw.first() in FORMULA_TRIGGERS && raw.toDoubleOrNull() == null) {
            "'$raw"
        } else {
            raw
        }
    val needsQuote = guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val escaped = guarded.replace("\"", "\"\"")
    return if (needsQuote) "\"$escaped\"" else escaped
}
