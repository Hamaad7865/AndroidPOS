package com.nexapos.retail.data.hardware.labels

import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.domain.hardware.LabelSpec
import com.nexapos.retail.util.Money

/** Label stock dimensions in millimetres (gap = space between labels). */
data class LabelSize(
    val widthMm: Int,
    val heightMm: Int,
    val gapMm: Int = 2,
)

/**
 * Pure TSPL command builders — no Android imports so they unit-test on the JVM
 * (mirrors [com.nexapos.retail.data.hardware.drawer.EscPos]). TSPL is the
 * lingua franca of affordable thermal label printers (XPrinter, TSC, HPRT,
 * Gprinter, iDPRT) over Bluetooth SPP / TCP 9100.
 *
 * Layout model: 203 dpi ⇒ 8 dots/mm; content flows top→bottom with a vertical
 * cursor (name up to 2 lines, SKU, optional price, then the barcode filling
 * the remaining height). Strings are sanitised because TSPL delimits with
 * double quotes. Commands are CRLF-terminated; bytes encode as windows-1252 so
 * Latin accents in product names survive (CODEPAGE 1252 is set in the header).
 */
object Tspl {
    private const val DOTS_PER_MM = 8 // 203 dpi
    private const val MARGIN_DOTS = 8
    private const val CRLF = "\r\n"

    // Built-in fonts: "2" = 12×20 dots, "1" = 8×12 dots (per cell, before scaling).
    private const val NAME_FONT = "2"
    private const val NAME_CHAR_W = 12
    private const val NAME_LINE_H = 26
    private const val SKU_FONT = "1"
    private const val SKU_CHAR_W = 8
    private const val SKU_LINE_H = 18
    private const val PRICE_LINE_H = 26

    // EAN-13 is always 95 modules wide; Code128 ≈ 11 modules/char + 35 overhead.
    private const val EAN13_MODULES = 95
    private const val CODE128_MODULES_PER_CHAR = 11
    private const val CODE128_OVERHEAD_MODULES = 35
    private const val HUMAN_READABLE_DOTS = 28
    private const val MIN_BAR_HEIGHT = 32
    private const val MAX_NAME_LINES = 2

    /** One complete label job: header + content + `PRINT 1,copies`. */
    fun label(
        spec: LabelSpec,
        size: LabelSize,
    ): ByteArray = labelText(spec, size).toByteArray(charset("windows-1252"))

    /** The label as a TSPL command string — exposed for tests. */
    fun labelText(
        spec: LabelSpec,
        size: LabelSize,
    ): String {
        val w = size.widthMm * DOTS_PER_MM
        val h = size.heightMm * DOTS_PER_MM
        val usable = w - 2 * MARGIN_DOTS
        val lines = mutableListOf<String>()
        lines += header(size)

        var y = MARGIN_DOTS
        // Name — wrapped to at most two centred lines.
        wrapName(sanitize(spec.name), usable / NAME_CHAR_W).forEach { line ->
            lines += text(centerX(w, line.length * NAME_CHAR_W), y, NAME_FONT, line)
            y += NAME_LINE_H
        }
        if (spec.sku.isNotBlank()) {
            val sku = sanitize(spec.sku)
            lines += text(centerX(w, sku.length * SKU_CHAR_W), y, SKU_FONT, sku)
            y += SKU_LINE_H
        }
        spec.priceCents?.let { cents ->
            val price = Money.format(cents)
            lines += text(centerX(w, price.length * NAME_CHAR_W), y, NAME_FONT, price)
            y += PRICE_LINE_H
        }

        val content = sanitize(spec.barcode)
        val symbology = if (Ean13.isValid(content)) "EAN13" else "128"
        // Printers compute the EAN-13 check digit themselves from 12 digits.
        val payload = if (symbology == "EAN13") content.take(12) else content
        val modules =
            if (symbology == "EAN13") {
                EAN13_MODULES
            } else {
                payload.length * CODE128_MODULES_PER_CHAR + CODE128_OVERHEAD_MODULES
            }
        // Narrow-bar width 2 dots scans best; drop to 1 only if the code can't fit.
        val narrow = if (modules * 2 <= usable) 2 else 1
        val barHeight = (h - y - HUMAN_READABLE_DOTS - MARGIN_DOTS).coerceAtLeast(MIN_BAR_HEIGHT)
        val barX = centerX(w, modules * narrow)
        lines += "BARCODE $barX,$y,\"$symbology\",$barHeight,1,0,$narrow,$narrow,\"$payload\""

        lines += "PRINT 1,${spec.copies.coerceAtLeast(1)}"
        return lines.joinToString(CRLF, postfix = CRLF)
    }

    /** A fixed test label proving size, text, and barcode all work. */
    fun testLabel(size: LabelSize): ByteArray =
        label(
            LabelSpec(name = "NexaPOS test label", sku = "TEST-01", barcode = "2000000000008", copies = 1),
            size,
        )

    private fun header(size: LabelSize): String =
        listOf(
            "SIZE ${size.widthMm} mm,${size.heightMm} mm",
            "GAP ${size.gapMm} mm,0 mm",
            "DIRECTION 1",
            "CODEPAGE 1252",
            "CLS",
        ).joinToString(CRLF)

    private fun text(
        x: Int,
        y: Int,
        font: String,
        content: String,
    ): String = "TEXT $x,$y,\"$font\",0,1,1,\"$content\""

    private fun centerX(
        labelWidthDots: Int,
        contentWidthDots: Int,
    ): Int = ((labelWidthDots - contentWidthDots) / 2).coerceAtLeast(MARGIN_DOTS)

    /** TSPL strings are double-quote-delimited and single-line. */
    private fun sanitize(s: String): String = s.replace("\"", "'").replace(Regex("[\r\n]+"), " ").trim()

    /**
     * Splits [name] into at most [MAX_NAME_LINES] lines of ≤ [maxChars],
     * breaking on spaces where possible; the final line is ellipsised if the
     * rest doesn't fit.
     */
    private fun wrapName(
        name: String,
        maxChars: Int,
    ): List<String> {
        if (maxChars < 1) return listOf("")
        if (name.length <= maxChars) return listOf(name)
        val lines = mutableListOf<String>()
        var rest = name
        while (lines.size < MAX_NAME_LINES && rest.isNotEmpty()) {
            if (rest.length <= maxChars) {
                lines += rest
                rest = ""
            } else if (lines.size == MAX_NAME_LINES - 1) {
                lines += rest.take(maxChars - 1) + "…"
                rest = ""
            } else {
                val cut = rest.take(maxChars + 1).lastIndexOf(' ').takeIf { it > 0 } ?: maxChars
                lines += rest.take(cut).trim()
                rest = rest.drop(cut).trim()
            }
        }
        return lines
    }
}
