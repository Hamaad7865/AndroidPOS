package com.nexapos.retail.ui.checkout

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.profile.ReceiptSettings
import com.nexapos.retail.ui.components.formatNum
import com.nexapos.retail.ui.sale.SaleSnapshot
import com.nexapos.retail.util.Money
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a completed [SaleSnapshot] into the shareable outputs offered on the
 * receipt screen:
 *  - [print]    → Android system print dialog (real printer or Save-as-PDF), from
 *                 the styled HTML in [html].
 *  - [sharePdf] → renders a styled receipt slip to a PDF file ([renderPdf]) and
 *                 opens the share sheet (WhatsApp / email — SMS is text-only).
 *  - [sendSms] / [sendWhatsApp] → open the messaging app pre-filled with text.
 *
 * The slip carries the business BRN and VAT registration number (via
 * [BusinessProfile.receiptLines]) so the sent receipt is a valid VAT receipt.
 */
object ReceiptOutput {
    private val dateFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

    /** How long a shared receipt PDF lingers in the cache before it's pruned. */
    private const val SHARE_CACHE_TTL_MS = 10 * 60 * 1000L

    private fun money(cents: Long) = Money.format(cents)

    // -----------------------------------------------------------------------
    // Messaging body (human-readable, reads well in an SMS/WhatsApp bubble).
    // -----------------------------------------------------------------------

    fun messageText(
        context: Context,
        sale: SaleSnapshot,
    ): String {
        val biz = BusinessProfile.name(context)
        val vatRegistered = BusinessProfile.vatRegistered(context)
        val sb = StringBuilder()
        sb.append("$biz — Receipt\n")
        sb.append("Invoice ${sale.invoiceNo} · ${dateFmt.format(Date(sale.createdAt))}\n")
        if (sale.customerName.isNotBlank()) sb.append("Customer: ${sale.customerName}\n")
        sb.append("\n")
        sale.lines.forEach { l ->
            sb.append("${l.qty}× ${l.product.name} — ${money(l.lineTotalCents)}\n")
            if (l.discountCents > 0L) sb.append("   − ${money(l.discountCents)} disc\n")
        }
        sb.append("\n")
        sb.append("Subtotal: ${money(sale.subtotalCents)}\n")
        if (vatRegistered) sb.append("VAT 15%: ${money(sale.vatCents)}\n")
        val msgDisc = sale.discountCents + sale.lines.sumOf { it.discountCents }
        if (msgDisc > 0L) sb.append("Discount: ${money(msgDisc)}\n")
        sb.append("TOTAL: ${money(sale.totalCents)}\n")
        sb.append("Paid (${sale.pay}): ${money(sale.receivedCents)}\n")
        if (sale.creditDueCents > 0L) sb.append("Balance due: ${money(sale.creditDueCents)}\n")
        sb.append("\nThank you!")
        return sb.toString()
    }

    /** Opens the system share sheet with a plain-text [body] under [subject]. */
    @Suppress("SwallowedException") // absence of a share target is reported via Toast
    fun shareText(
        context: Context,
        subject: String,
        body: String,
    ) {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        try {
            context.startActivity(Intent.createChooser(send, subject))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to share.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("SwallowedException") // absence of an SMS app is reported via Toast, not the exception
    fun sendSms(
        context: Context,
        phone: String,
        body: String,
    ) {
        val uri = android.net.Uri.parse("smsto:" + phone.trim())
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { putExtra("sms_body", body) }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("SwallowedException") // each failed target falls through to the next; final miss shows a Toast
    fun sendWhatsApp(
        context: Context,
        phone: String,
        body: String,
    ) {
        val digits = normalizePhone(phone)
        val url =
            if (digits.isNotEmpty()) {
                "https://wa.me/$digits?text=" + android.net.Uri.encode(body)
            } else {
                "https://api.whatsapp.com/send?text=" + android.net.Uri.encode(body)
            }
        val view = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
            val attempt = Intent(view)
            if (pkg != null) attempt.setPackage(pkg)
            try {
                context.startActivity(attempt)
                return
            } catch (e: ActivityNotFoundException) {
                // try the next option
            }
        }
        Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_LONG).show()
    }

    /** Strips formatting and applies the Mauritius country code to 8-digit local numbers. */
    private fun normalizePhone(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.length == 8) d = "230$d"
        return d
    }

    // -----------------------------------------------------------------------
    // Print — styled HTML through the system print dialog (printer / save-as-PDF).
    // -----------------------------------------------------------------------

    fun print(
        context: Context,
        sale: SaleSnapshot,
    ) {
        val paper = ReceiptSettings.paper(context)
        val widthCss = if (paper == ReceiptSettings.Paper.A4) "190mm" else "${paper.cssWidthMm}mm"
        val webView = WebView(context)
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view.createPrintDocumentAdapter("receipt-${sale.invoiceNo}")
                    printManager.print("Receipt ${sale.invoiceNo}", adapter, PrintAttributes.Builder().build())
                }
            }
        webView.loadDataWithBaseURL(null, html(context, sale, widthCss), "text/html", "UTF-8", null)
    }

    // -----------------------------------------------------------------------
    // PDF — render a styled receipt slip to a file and open the share sheet.
    // -----------------------------------------------------------------------

    @Suppress("TooGenericExceptionCaught") // any rendering/IO failure is surfaced to the user via Toast
    fun sharePdf(
        context: Context,
        sale: SaleSnapshot,
    ) {
        try {
            sharePdfFile(context, renderPdf(context, sale), sale.invoiceNo)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not create the receipt PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("SwallowedException") // missing share target is reported via Toast
    private fun sharePdfFile(
        context: Context,
        file: File,
        invoiceNo: String,
    ) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Receipt $invoiceNo")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        try {
            context.startActivity(Intent.createChooser(send, "Share receipt PDF"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to share the PDF.", Toast.LENGTH_LONG).show()
        }
    }

    /** Draws the styled receipt slip onto a single, content-sized PDF page. */
    @Suppress("LongMethod") // a receipt is a linear list of rows — clearer drawn top-to-bottom in one place
    private fun renderPdf(
        context: Context,
        sale: SaleSnapshot,
    ): File {
        val p = ReceiptPaints
        val w = PAGE_W
        val maxText = w - PAD_X * 2
        val ops = mutableListOf<Pair<Float, (Canvas, Float) -> Unit>>()

        fun lineH(paint: Paint) = paint.textSize * LINE_FACTOR

        fun centre(
            s: String,
            paint: Paint,
        ) =
            ops.add(lineH(paint) to { c: Canvas, y: Float -> c.drawText(s, w / 2f, y + paint.textSize, paint) })

        fun left(
            s: String,
            paint: Paint,
        ) =
            ops.add(lineH(paint) to { c: Canvas, y: Float -> c.drawText(fit(s, paint, maxText), PAD_X, y + paint.textSize, paint) })

        fun row(
            left: String,
            lp: Paint,
            right: String,
            rp: Paint,
        ) =
            ops.add(
                maxOf(lineH(lp), lineH(rp)) to { c: Canvas, y: Float ->
                    c.drawText(fit(left, lp, maxText * 0.62f), PAD_X, y + lp.textSize, lp)
                    c.drawText(right, w - PAD_X, y + rp.textSize, rp)
                },
            )

        fun dash() =
            ops.add(DASH_H to { c: Canvas, y: Float -> c.drawLine(PAD_X, y + DASH_H / 2, w - PAD_X, y + DASH_H / 2, p.dash) })

        centre(BusinessProfile.name(context), p.name)
        BusinessProfile.receiptLines(context).forEach { centre(it, p.sub) }
        dash()
        row("Invoice", p.label, sale.invoiceNo, p.valueR)
        row("Date", p.label, dateFmt.format(Date(sale.createdAt)), p.valueR)
        row("Customer", p.label, sale.customerName.ifBlank { "Walk-in" }, p.valueR)
        dash()
        sale.lines.forEach { l ->
            left(l.product.name, p.item)
            row("   ${l.qty} × ${formatNum(l.effectivePriceCents / 100.0, 2)}", p.qty, formatNum(l.lineTotalCents / 100.0, 2), p.amtR)
            if (l.discountCents > 0L) row("   − discount", p.qty, "-" + formatNum(l.discountCents / 100.0, 2), p.amtR)
        }
        dash()
        row("Subtotal", p.label, money(sale.subtotalCents), p.valueR)
        if (BusinessProfile.vatRegistered(context)) row("VAT 15% (incl.)", p.label, money(sale.vatCents), p.valueR)
        val pdfDisc = sale.discountCents + sale.lines.sumOf { it.discountCents }
        if (pdfDisc > 0L) row("Discount", p.label, money(pdfDisc), p.valueR)
        row("TOTAL", p.totalL, money(sale.totalCents), p.totalR)
        row("Paid · ${sale.pay}", p.label, money(sale.receivedCents), p.valueR)
        if (sale.creditDueCents > 0L) {
            row("Balance due", p.totalL, money(sale.creditDueCents), p.totalR)
        } else {
            row("Change", p.label, money(maxOf(0L, sale.changeCents)), p.valueR)
        }
        if (sale.note.isNotBlank()) left("Note: ${sale.note}", p.item)
        dash()
        ReceiptSettings.footerNote(context).takeIf { it.isNotBlank() }?.let { centre(it, p.foot) }
        centre("powered by NexaPOS · ${sale.invoiceNo}", p.foot)

        val height = (MARGIN_Y * 2 + ops.sumOf { it.first.toDouble() }).toInt()
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(w, height, 1).create())
        var y = MARGIN_Y
        ops.forEach { (h, draw) ->
            draw(page.canvas, y)
            y += h
        }
        doc.finishPage(page)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        // Prune previously-shared receipts so the cache can't grow unbounded;
        // keep very recent ones in case a share sheet is still reading them.
        val cutoff = System.currentTimeMillis() - SHARE_CACHE_TTL_MS
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        val file = File(dir, "receipt-${sale.invoiceNo}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Trims [s] with an ellipsis so it fits within [maxW] when drawn with [paint]. */
    private fun fit(
        s: String,
        paint: Paint,
        maxW: Float,
    ): String {
        if (paint.measureText(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && paint.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    /** The paints used by [renderPdf]; held once so the renderer stays readable. */
    private object ReceiptPaints {
        private val ink = Color.rgb(24, 24, 24)
        private val grey = Color.rgb(120, 120, 120)
        private val sans = Typeface.SANS_SERIF
        private val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        private val mono = Typeface.MONOSPACE

        private fun mk(
            tf: Typeface,
            size: Float,
            col: Int,
            align: Paint.Align,
        ) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tf
                textSize = size
                color = col
                textAlign = align
            }

        val name = mk(bold, 21f, ink, Paint.Align.CENTER)
        val sub = mk(sans, 11f, grey, Paint.Align.CENTER)
        val label = mk(sans, 12f, ink, Paint.Align.LEFT)
        val valueR = mk(sans, 12f, ink, Paint.Align.RIGHT)
        val item = mk(bold, 13f, ink, Paint.Align.LEFT)
        val qty = mk(mono, 11f, grey, Paint.Align.LEFT)
        val amtR = mk(mono, 12f, ink, Paint.Align.RIGHT)
        val totalL = mk(bold, 15f, ink, Paint.Align.LEFT)
        val totalR = mk(bold, 15f, ink, Paint.Align.RIGHT)
        val foot = mk(sans, 10f, grey, Paint.Align.CENTER)
        val dash =
            Paint().apply {
                color = Color.rgb(170, 170, 170)
                strokeWidth = 1.2f
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
            }
    }

    // -----------------------------------------------------------------------
    // Styled receipt HTML — used by [print].
    // -----------------------------------------------------------------------

    private fun html(
        context: Context,
        sale: SaleSnapshot,
        widthCss: String,
    ): String {
        val biz = esc(BusinessProfile.name(context))
        val headerLines = BusinessProfile.receiptLines(context).joinToString("") { "<div class=\"sub\">${esc(it)}</div>" }
        val footer = esc(ReceiptSettings.footerNote(context))
        val htmlDisc = sale.discountCents + sale.lines.sumOf { it.discountCents }
        val itemRows =
            sale.lines.joinToString("") { l ->
                """
                <tr><td colspan="2" class="nm">${esc(l.product.name)}</td></tr>
                <tr><td class="qty">${l.qty} × ${formatNum(l.effectivePriceCents / 100.0, 2)}</td>
                    <td class="amt">${formatNum(l.lineTotalCents / 100.0, 2)}</td></tr>
                ${if (l.discountCents > 0L) "<tr><td class=\"qty\">− discount</td><td class=\"amt\">-${formatNum(l.discountCents / 100.0, 2)}</td></tr>" else ""}
                """.trimIndent()
            }
        val tailRow =
            if (sale.creditDueCents > 0L) {
                "<tr><td class=\"k b\">BALANCE DUE</td><td class=\"amt b\">${money(sale.creditDueCents)}</td></tr>"
            } else {
                "<tr><td class=\"k\">Change</td><td class=\"amt\">${money(maxOf(0L, sale.changeCents))}</td></tr>"
            }
        return """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <style>
              @page { margin: 4mm; }
              * { box-sizing: border-box; }
              body { margin: 0; width: $widthCss; font-family: 'Courier New', monospace; color: #000; }
              .hd { text-align: center; }
              .hd .nm { font-size: 15px; font-weight: 700; }
              .hd .sub { font-size: 10px; }
              hr { border: none; border-top: 1px dashed #000; margin: 6px 0; }
              table { width: 100%; border-collapse: collapse; }
              td { font-size: 11px; padding: 1px 0; }
              td.nm { font-weight: 700; padding-top: 4px; }
              td.qty { color: #000; }
              td.amt { text-align: right; }
              td.k { }
              .b { font-weight: 800; font-size: 13px; }
              .meta td { font-size: 11px; }
              .ft { text-align: center; font-size: 10px; margin-top: 8px; }
            </style></head>
            <body>
              <div class="hd"><div class="nm">$biz</div>$headerLines</div>
              <hr>
              <table class="meta">
                <tr><td>Invoice</td><td class="amt">${esc(sale.invoiceNo)}</td></tr>
                <tr><td>Date</td><td class="amt">${esc(dateFmt.format(Date(sale.createdAt)))}</td></tr>
                <tr><td>Customer</td><td class="amt">${esc(sale.customerName)}</td></tr>
              </table>
              <hr>
              <table>$itemRows</table>
              <hr>
              <table>
                <tr><td class="k">Subtotal</td><td class="amt">${money(sale.subtotalCents)}</td></tr>
                ${if (BusinessProfile.vatRegistered(context)) "<tr><td class=\"k\">VAT 15%</td><td class=\"amt\">${money(sale.vatCents)}</td></tr>" else ""}
                ${if (htmlDisc > 0L) "<tr><td class=\"k\">Discount</td><td class=\"amt\">${money(htmlDisc)}</td></tr>" else ""}
                <tr><td class="k b">TOTAL</td><td class="amt b">${money(sale.totalCents)}</td></tr>
                <tr><td class="k">Paid · ${esc(sale.pay)}</td><td class="amt">${money(sale.receivedCents)}</td></tr>
                $tailRow
              </table>
              ${if (sale.note.isNotBlank()) "<div class=\"ft\" style=\"text-align:left;margin-top:6px\">Note: ${esc(sale.note)}</div>" else ""}
              <hr>
              <div class="ft">$footer</div>
              <div class="ft">powered by NexaPOS · ${esc(sale.invoiceNo)}</div>
            </body></html>
            """.trimIndent()
    }

    private fun esc(raw: String): String =
        raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private const val PAGE_W = 430
    private const val PAD_X = 26f
    private const val MARGIN_Y = 28f
    private const val DASH_H = 16f
    private const val LINE_FACTOR = 1.5f
}
