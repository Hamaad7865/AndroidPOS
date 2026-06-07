package com.nexapos.retail.ui.purchase.receipt

/**
 * Best-effort extraction of supplier + line items from a receipt's OCR lines.
 * Intentionally forgiving: the editable review screen is the safety net, so this
 * aims for "good enough to fix fast", not perfection. Pure — unit-tested.
 */
object ReceiptParser {
    private val AMOUNT = Regex("""(?:rs\.?\s*)?(\d[\d.,]*\d|\d)\s*$""", RegexOption.IGNORE_CASE)
    private val LEADING_QTY = Regex("""^\s*(\d{1,3})\s*(?:x|\*)?\s+""", RegexOption.IGNORE_CASE)
    private val NON_ITEM =
        Regex(
            """\b(sub-?total|total|vat|tva|tax|balance|due|change|tendered|cash|amount|discount|rounding)\b""",
            RegexOption.IGNORE_CASE,
        )

    @Suppress("LoopWithTooManyJumpStatements") // a filter loop; the early-continues read clearly
    fun parse(lines: List<OcrLine>): ParsedReceipt {
        if (lines.isEmpty()) {
            return ParsedReceipt("", emptyList(), listOf("Nothing was read from the image."))
        }
        val ordered = lines.sortedBy { it.top }
        val supplier = ordered.firstOrNull { !looksLikeAmountOrNoise(it.text) }?.text?.trim().orEmpty()

        val items = mutableListOf<ReceiptDraftLine>()
        var skipped = 0
        for (line in ordered) {
            val raw = line.text.trim()
            if (raw.isBlank()) continue
            if (NON_ITEM.containsMatchIn(raw)) continue
            val amount = AMOUNT.find(raw) ?: continue
            val unitCost = parseMoney(amount.groupValues[1])
            if (unitCost <= 0) {
                skipped++
                continue
            }
            var name = raw.removeRange(amount.range).trim().trimEnd('-', '.', ' ')
            var qty = 1
            LEADING_QTY.find(name)?.let { m ->
                qty = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                name = name.removeRange(m.range).trim()
            }
            if (name.isBlank() || name.length < 2) {
                skipped++
                continue
            }
            items += ReceiptDraftLine(name = name, quantity = qty, unitCostRupees = unitCost)
        }

        val warnings =
            buildList {
                if (items.isEmpty()) add("No item lines were recognised — add them manually.")
                if (skipped > 0) add("$skipped line(s) couldn't be read clearly — please check.")
            }
        return ParsedReceipt(supplierGuess = supplier, lines = items, warnings = warnings)
    }

    private fun looksLikeAmountOrNoise(text: String): Boolean {
        val t = text.trim()
        if (t.length < 3) return true
        val digits = t.count { it.isDigit() }
        return digits > t.length / 2
    }

    private fun parseMoney(cell: String): Int =
        cell.replace(",", "").filter { it.isDigit() || it == '.' }
            .toDoubleOrNull()?.toInt() ?: 0
}
