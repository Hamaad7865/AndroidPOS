package com.nexapos.retail.ui.products

import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.ui.sale.PosProduct
import com.nexapos.retail.util.csvCell

/**
 * Pure document builders for the Products screen — kept free of Android/Compose
 * types so they can be unit-tested. [productsCsv] feeds the Export action;
 * [labelsHtml] feeds the Print-labels action (rendered by a WebView and handed
 * to the system print dialog).
 */

private const val MODULE_PX = 2
private const val BAR_HEIGHT_PX = 46

/**
 * Builds an Excel-friendly CSV (CRLF lines, quoted cells) of the given products.
 * [includeCost] is false for cashier exports — they must not see cost prices.
 * Import still round-trips either shape (only Name and Price are required).
 */
internal fun productsCsv(
    products: List<PosProduct>,
    includeCost: Boolean = true,
): String {
    val header =
        listOfNotNull(
            "Name",
            "SKU",
            "Barcode",
            "Category",
            "Cost (Rs)".takeIf { includeCost },
            "Price (Rs)",
            "Stock",
            "Stock value (Rs)",
        )
    val sb = StringBuilder()
    sb.append(header.joinToString(",") { csvCell(it) }).append("\r\n")
    products.forEach { p ->
        val row =
            listOfNotNull(
                p.name,
                p.sku,
                p.barcode.orEmpty(),
                p.cat,
                p.cost.toString().takeIf { includeCost },
                p.price.toString(),
                p.stock.toString(),
                (p.price * p.stock).toString(),
            )
        sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
    }
    return sb.toString()
}

// ---------------------------------------------------------------------------
// CSV import — pure parsing (no Android deps). Round-trips [productsCsv].
// ---------------------------------------------------------------------------

/** One product parsed from an import CSV row (whole-rupee money). */
internal data class ImportRow(
    val name: String,
    val sku: String,
    val barcode: String?,
    val category: String,
    val costRupees: Int,
    val priceRupees: Int,
    val stock: Int,
)

/** Outcome of parsing an import CSV, before any database work. */
internal data class ParsedImport(
    val rows: List<ImportRow>,
    val skipped: Int,
    val errors: List<String>,
    /** Set when nothing usable could be parsed (e.g. required columns missing). */
    val fatalError: String? = null,
    /**
     * Whether the file actually had a Cost / Stock column. When false the parsed
     * value is a meaningless 0 (absent cell) and the importer must PRESERVE the
     * existing product's cost/stock rather than overwrite it with 0 — otherwise a
     * price-only or cashier (cost-stripped) export silently wipes that data.
     */
    val hasCost: Boolean = true,
    val hasStock: Boolean = true,
)

private const val MAX_IMPORT_ERRORS = 8

/**
 * Parses an import CSV. The only required columns are **Name** and **Price**;
 * SKU, Barcode, Category, Cost and Stock are optional. Header matching is
 * case-insensitive and tolerant of the export's "Price (Rs)" style headers.
 */
internal fun parseProductsCsv(text: String): ParsedImport {
    val table = parseCsvTable(text)
    if (table.isEmpty()) return ParsedImport(emptyList(), 0, emptyList(), "The file is empty.")
    val header = table.first().map { normaliseHeader(it) }

    fun col(vararg keys: String) = header.indexOfFirst { it in keys }
    val nameIdx = col("name", "productname", "product", "item", "itemname")
    val priceIdx = col("price", "pricers", "saleprice", "sellingprice", "sellprice", "mrp", "rate", "unitprice")
    if (nameIdx < 0 || priceIdx < 0) {
        return ParsedImport(
            emptyList(),
            0,
            emptyList(),
            "CSV needs at least a \"Name\" and a \"Price\" column. Tip: tap Export first to get a file in the right format.",
        )
    }
    val skuIdx = col("sku", "code", "itemcode", "ref")
    val barcodeIdx = col("barcode", "ean", "ean13", "upc")
    val catIdx = col("category", "cat", "group", "department")
    val costIdx = col("cost", "costrs", "purchaseprice", "buyprice", "costprice")
    val stockIdx = col("stock", "qty", "quantity", "stockqty", "openingstock", "onhand")

    val rows = mutableListOf<ImportRow>()
    var skipped = 0
    val errors = mutableListOf<String>()

    fun note(msg: String) {
        if (errors.size < MAX_IMPORT_ERRORS) errors += msg
    }

    table.drop(1).forEachIndexed { i, raw ->
        val lineNo = i + 2 // 1-based, accounting for the header row
        if (raw.all { it.isBlank() }) return@forEachIndexed

        fun cell(idx: Int) = if (idx in raw.indices) raw[idx].trim() else ""
        val name = cell(nameIdx)
        val price = parseMoney(cell(priceIdx))
        when {
            name.isBlank() -> {
                skipped++
                note("Line $lineNo: missing product name — skipped.")
            }
            price <= 0 -> {
                skipped++
                note("Line $lineNo: \"$name\" has no valid price — skipped.")
            }
            else ->
                rows +=
                    ImportRow(
                        name = name,
                        sku = cell(skuIdx),
                        barcode = cell(barcodeIdx).takeIf { it.isNotEmpty() },
                        category = cell(catIdx),
                        costRupees = parseMoney(cell(costIdx)),
                        priceRupees = price,
                        stock = parseMoney(cell(stockIdx)),
                    )
        }
    }
    val fatal = if (rows.isEmpty()) "No valid product rows found in the file." else null
    return ParsedImport(rows, skipped, errors, fatal, hasCost = costIdx >= 0, hasStock = stockIdx >= 0)
}

private fun normaliseHeader(raw: String): String = raw.lowercase().filter { it.isLetterOrDigit() }

/** Whole-rupee amount from a messy cell like "Rs 1,200.50" → 1200. */
private fun parseMoney(cell: String): Int =
    cell.replace(",", "").filter { it.isDigit() || it == '.' || it == '-' }
        .toDoubleOrNull()?.toInt() ?: 0

/**
 * Splits CSV [text] into rows of cells, honouring RFC-4180 quoting: quoted
 * fields may contain commas, line breaks, and "" escaped quotes.
 */
@Suppress("NestedBlockDepth") // an RFC-4180 quote/field/row state machine is inherently nested
internal fun parseCsvTable(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    // Strip a leading UTF-8 BOM (common from Excel exports). Compared by code
    // point (0xFEFF) on purpose: a literal BOM char in source is invisible and
    // was previously lost in an edit here, leaving startsWith("") — which is
    // always true and chopped the first character off every CSV (turning the
    // "Name" header into "ame"), breaking every import.
    val s = if (text.firstOrNull()?.code == 0xFEFF) text.substring(1) else text
    var i = 0

    fun endField() {
        row.add(field.toString())
        field.setLength(0)
    }

    fun endRow() {
        endField()
        rows.add(row)
        row = mutableListOf()
    }
    while (i < s.length) {
        val ch = s[i]
        when {
            inQuotes ->
                if (ch == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            ch == '"' -> inQuotes = true
            ch == ',' -> endField()
            ch == '\n' -> endRow()
            ch == '\r' -> if (i + 1 < s.length && s[i + 1] == '\n') Unit else endRow()
            else -> field.append(ch)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    // Drop fully-empty trailing rows.
    return rows.filterNot { it.size == 1 && it[0].isBlank() }
}

/**
 * Builds a printable HTML sheet of shelf/price labels — one card per product,
 * each with the business name, product name, price, a scannable EAN-13 (when the
 * product has a valid barcode) and the human-readable code beneath it.
 */
internal fun labelsHtml(
    products: List<PosProduct>,
    businessName: String,
): String {
    val biz = htmlEscape(businessName)
    val cards =
        products.joinToString("\n") { p ->
            val barcodeBlock =
                if (p.barcode != null && Ean13.isValid(p.barcode)) {
                    """<div class="bars">${barcodeBarsHtml(p.barcode)}</div>
                       <div class="code">${htmlEscape(p.barcode)}</div>"""
                } else {
                    """<div class="code">${htmlEscape(p.sku.ifBlank { "no code" })}</div>"""
                }
            """
            <div class="label">
              <div class="biz">$biz</div>
              <div class="name">${htmlEscape(p.name)}</div>
              <div class="price">Rs ${p.price}</div>
              $barcodeBlock
            </div>
            """.trimIndent()
        }
    return """
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <style>
          * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
          body { margin: 0; font-family: Arial, Helvetica, sans-serif; }
          .sheet { display: flex; flex-wrap: wrap; gap: 6px; padding: 8px; }
          .label {
            width: 5cm; height: 3.4cm; padding: 0.18cm 0.2cm;
            border: 1px dashed #bbb; border-radius: 4px;
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            text-align: center; overflow: hidden; page-break-inside: avoid;
          }
          .biz { font-size: 7px; letter-spacing: .4px; color: #555; text-transform: uppercase; }
          .name { font-size: 11px; font-weight: 700; margin: 1px 0; line-height: 1.1;
                  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
          .price { font-size: 14px; font-weight: 800; margin-bottom: 2px; }
          .bars { white-space: nowrap; font-size: 0; height: ${BAR_HEIGHT_PX}px; }
          .bars span { display: inline-block; height: ${BAR_HEIGHT_PX}px; vertical-align: top; }
          .code { font-family: 'Courier New', monospace; font-size: 9px; letter-spacing: 1px; margin-top: 1px; }
          @media print { .label { border-color: #ddd; } }
        </style></head>
        <body><div class="sheet">
        $cards
        </div></body></html>
        """.trimIndent()
}

/** Renders a valid EAN-13 as a run-length-collapsed strip of black/white bar spans. */
private fun barcodeBarsHtml(value: String): String {
    val bits = Ean13.encode(value)
    val sb = StringBuilder()
    var i = 0
    while (i < bits.length) {
        val bit = bits[i]
        var run = 1
        while (i + run < bits.length && bits[i + run] == bit) run++
        val color = if (bit == '1') "#000" else "#fff"
        sb.append("""<span style="width:${run * MODULE_PX}px;background:$color"></span>""")
        i += run
    }
    return sb.toString()
}

private fun htmlEscape(raw: String): String =
    raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
