package com.nexapos.retail.ui.purchase.receipt

/**
 * Best-effort extraction of supplier + line items from a receipt's OCR lines.
 *
 * Two passes:
 *  - [parseTable] for formal multi-column invoices (Code / Description / Qty /
 *    Unit Price / VAT / Total) — columns are located by the header cells'
 *    x-positions and each fragment is routed to its nearest column.
 *  - [parseSimple] for till receipts ("name … trailing price").
 *
 * The editable review screen is the safety net, so this aims for "good enough to
 * fix fast", not perfection. Pure (no Android deps) — unit-tested.
 */
object ReceiptParser {
    private val AMOUNT = Regex("""(?:rs\.?\s*)?(\d[\d.,]*\d|\d)\s*$""", RegexOption.IGNORE_CASE)
    private val LEADING_QTY = Regex("""^\s*(\d{1,3})\s*(?:x|\*)?\s+""", RegexOption.IGNORE_CASE)
    private val NON_ITEM =
        Regex(
            """\b(sub-?total|total|vat|tva|tax|balance|due|change|tendered|cash|amount|discount|rounding)\b""",
            RegexOption.IGNORE_CASE,
        )
    private val FOOTER =
        Regex("""\b(sub-?total|discount|grand\s*total|amount\s*due|balance)\b""", RegexOption.IGNORE_CASE)
    private val COMPANY =
        Regex(
            """\b(ltd|lt[ée]e|limited|& ?co|enterprises?|trading|hardwares?|quincaillerie|company|sarl|stores?)\b""",
            RegexOption.IGNORE_CASE,
        )

    fun parse(lines: List<OcrLine>): ParsedReceipt {
        if (lines.isEmpty()) {
            return ParsedReceipt("", emptyList(), listOf("Nothing was read from the image."))
        }
        parseTable(lines)?.let { return it }
        return parseSimple(lines)
    }

    // -----------------------------------------------------------------------
    // Column-aware pass — formal multi-column invoices.
    // -----------------------------------------------------------------------

    private enum class Col { CODE, DESC, QTY, PRICE, VAT, TOTAL }

    private val COLUMN_LABELS: List<Pair<Regex, Col>> =
        listOf(
            Regex("""description|particular|^item$|^product$""", RegexOption.IGNORE_CASE) to Col.DESC,
            Regex("""quantity|^qty$""", RegexOption.IGNORE_CASE) to Col.QTY,
            Regex("""unit ?price|unit ?cost|^price$|^rate$""", RegexOption.IGNORE_CASE) to Col.PRICE,
            Regex("""^vat$|^tax$|^tva$""", RegexOption.IGNORE_CASE) to Col.VAT,
            Regex("""total|^amount$""", RegexOption.IGNORE_CASE) to Col.TOTAL,
            Regex("""^code$|^ref$|item ?code|^sku$""", RegexOption.IGNORE_CASE) to Col.CODE,
        )

    private fun columnKind(text: String): Col? {
        val t = text.trim()
        return COLUMN_LABELS.firstOrNull { it.first.containsMatchIn(t) }?.second
    }

    private fun centreX(l: OcrLine): Int = (l.left + l.right) / 2

    /**
     * Parses an invoice that has a Description + Quantity + Unit-Price column
     * header. Returns null when no such header exists (→ fall back to the simple
     * pass), so plain till receipts are unaffected.
     */
    private fun parseTable(lines: List<OcrLine>): ParsedReceipt? {
        val candidates = lines.mapNotNull { l -> columnKind(l.text)?.let { it to l } }
        val descCell = candidates.firstOrNull { it.first == Col.DESC }?.second ?: return null

        // Header cells share the Description cell's row; their x-centres become the columns.
        val rowTol = (descCell.bottom - descCell.top).coerceAtLeast(8) * 2
        val header = candidates.filter { kotlin.math.abs(it.second.top - descCell.top) <= rowTol }
        val anchors = header.associate { it.first to centreX(it.second) }
        if (Col.DESC !in anchors || Col.QTY !in anchors || Col.PRICE !in anchors) return null

        val headerMinTop = header.minOf { it.second.top }
        val headerLines = header.mapTo(HashSet()) { it.second }
        // The table body ends where the totals block begins.
        val footerTop =
            lines.filter { it.top > descCell.top && FOOTER.containsMatchIn(it.text) }
                .minOfOrNull { it.top } ?: Int.MAX_VALUE

        val anchorList = anchors.entries.toList()

        fun columnAt(l: OcrLine): Col =
            anchorList.minByOrNull { kotlin.math.abs(centreX(l) - it.value) }?.key ?: Col.TOTAL

        val body = lines.filter { it !in headerLines && it.top > headerMinTop && it.top < footerTop }
        val descCol = body.filter { columnAt(it) == Col.DESC }.sortedBy { it.top }
        val qtyCol = body.filter { columnAt(it) == Col.QTY }.sortedBy { it.top }
        val priceCol = body.filter { columnAt(it) == Col.PRICE }.sortedBy { it.top }
        if (descCol.isEmpty() || priceCol.isEmpty()) return null

        // Pair the columns top-to-bottom: the i-th description belongs with the i-th price/qty.
        val rowCount = minOf(descCol.size, priceCol.size)
        val items =
            (0 until rowCount).mapNotNull { i ->
                val name = descCol[i].text.trim()
                val unitCost = parseMoney(priceCol[i].text)
                val qty = qtyCol.getOrNull(i)?.text?.let(::parseQty) ?: 1
                if (name.length >= 2 && unitCost > 0) ReceiptDraftLine(name, qty, unitCost) else null
            }
        if (items.isEmpty()) return null

        val warnings =
            buildList {
                if (descCol.size != priceCol.size) add("Some rows didn't line up — please double-check the items.")
            }
        return ParsedReceipt(detectSupplier(lines, descCell.top), items, warnings)
    }

    /** Picks a supplier from the letterhead — prefers a business-name line (e.g. contains "Ltd"). */
    private fun detectSupplier(
        lines: List<OcrLine>,
        headerTop: Int,
    ): String {
        val letterhead = lines.filter { it.top < headerTop }.sortedBy { it.top }
        return letterhead.firstOrNull { COMPANY.containsMatchIn(it.text) && it.text.trim().length in 3..40 }
            ?.text?.trim()
            ?: letterhead.firstOrNull { !looksLikeAmountOrNoise(it.text) }?.text?.trim()
            ?: ""
    }

    private fun parseQty(text: String): Int = text.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 1

    // -----------------------------------------------------------------------
    // Simple pass — till receipts.
    // -----------------------------------------------------------------------

    @Suppress("LoopWithTooManyJumpStatements") // a filter loop; the early-continues read clearly
    private fun parseSimple(lines: List<OcrLine>): ParsedReceipt {
        val rows = mergeRows(lines)
        val supplier = rows.firstOrNull { !looksLikeAmountOrNoise(it.text) }?.text?.trim().orEmpty()

        val items = mutableListOf<ReceiptDraftLine>()
        var skipped = 0
        for (line in rows) {
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

    /**
     * ML Kit returns an item's name and its price as separate fragments on wide
     * receipts (left column vs right column). Regroup fragments sharing a vertical
     * band into one logical line, ordered left-to-right, so the price rejoins its
     * name before parsing.
     */
    private fun mergeRows(lines: List<OcrLine>): List<OcrLine> {
        val sorted = lines.sortedWith(compareBy({ it.top }, { it.left }))
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val band = rows.lastOrNull()
            val centre = (line.top + line.bottom) / 2
            if (band != null && centre >= band.minOf { it.top } && centre <= band.maxOf { it.bottom }) {
                band.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        return rows.map { row ->
            val ordered = row.sortedBy { it.left }
            OcrLine(
                text = ordered.joinToString(" ") { it.text.trim() },
                top = row.minOf { it.top },
                bottom = row.maxOf { it.bottom },
                left = ordered.first().left,
                right = ordered.maxOf { it.right },
            )
        }
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
