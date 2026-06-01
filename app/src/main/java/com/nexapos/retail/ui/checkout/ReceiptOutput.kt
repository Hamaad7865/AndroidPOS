package com.nexapos.retail.ui.checkout

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a completed [SaleSnapshot] into the three shareable outputs offered on
 * the receipt screen:
 *  - [print]    → Android system print dialog (real printer or Save-as-PDF),
 *                 rendering HTML at the width configured in [ReceiptSettings].
 *  - [sharePdf] → renders a compact PDF slip and opens the share sheet.
 *  - [sendSms] / [sendWhatsApp] → open the messaging app pre-filled.
 */
object ReceiptOutput {
    private val dateFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

    private fun money(n: Int) = "Rs " + formatNum(n.toDouble(), 0)

    // -----------------------------------------------------------------------
    // Messaging body (human-readable, reads well in an SMS/WhatsApp bubble).
    // -----------------------------------------------------------------------

    fun messageText(
        context: Context,
        sale: SaleSnapshot,
    ): String {
        val biz = BusinessProfile.name(context)
        val sb = StringBuilder()
        sb.append("$biz — Receipt\n")
        sb.append("Invoice ${sale.invoiceNo} · ${dateFmt.format(Date(sale.createdAt))}\n")
        if (sale.customerName.isNotBlank()) sb.append("Customer: ${sale.customerName}\n")
        sb.append("\n")
        sale.lines.forEach { l ->
            sb.append("${l.qty}× ${l.product.name} — ${money(l.lineTotal)}\n")
        }
        sb.append("\n")
        sb.append("Subtotal: ${money(sale.subtotal)}\n")
        sb.append("VAT 15%: ${money(sale.vat)}\n")
        if (sale.discount > 0) sb.append("Discount: ${money(sale.discount)}\n")
        sb.append("TOTAL: ${money(sale.total)}\n")
        sb.append("Paid (${sale.pay}): ${money(sale.received)}\n")
        if (sale.creditDue > 0) sb.append("Balance due: ${money(sale.creditDue)}\n")
        sb.append("\nThank you!")
        return sb.toString()
    }

    // -----------------------------------------------------------------------
    // Generic plain-text share (account statements, etc.) → system share sheet.
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // SMS — opens the default SMS app pre-filled.
    // -----------------------------------------------------------------------

    @Suppress("SwallowedException") // absence of an SMS app is reported via Toast, not the exception
    fun sendSms(
        context: Context,
        phone: String,
        body: String,
    ) {
        val uri = Uri.parse("smsto:" + phone.trim())
        val intent =
            Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", body)
            }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // -----------------------------------------------------------------------
    // WhatsApp — prefers the installed app, falls back to the browser/web.
    // -----------------------------------------------------------------------

    @Suppress("SwallowedException") // each failed target falls through to the next; final miss shows a Toast
    fun sendWhatsApp(
        context: Context,
        phone: String,
        body: String,
    ) {
        val digits = normalizePhone(phone)
        val url =
            if (digits.isNotEmpty()) {
                "https://wa.me/$digits?text=" + Uri.encode(body)
            } else {
                "https://api.whatsapp.com/send?text=" + Uri.encode(body)
            }
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // Try the consumer app, then WhatsApp Business, then let the OS resolve
        // (browser → web.whatsapp.com) so it never silently does nothing.
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
    // Print — system print dialog at the configured paper width.
    // -----------------------------------------------------------------------

    fun print(
        context: Context,
        sale: SaleSnapshot,
    ) {
        val html = html(context, sale)
        val webView = WebView(context)
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view.createPrintDocumentAdapter("receipt-${sale.invoiceNo}")
                    printManager.print(
                        "Receipt ${sale.invoiceNo}",
                        adapter,
                        PrintAttributes.Builder().build(),
                    )
                }
            }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun html(
        context: Context,
        sale: SaleSnapshot,
    ): String {
        val paper = ReceiptSettings.paper(context)
        val biz = esc(BusinessProfile.name(context))
        val headerLines = BusinessProfile.receiptLines(context).joinToString("") { "<div class=\"sub\">${esc(it)}</div>" }
        val footer = esc(ReceiptSettings.footerNote(context))
        val itemRows =
            sale.lines.joinToString("") { l ->
                """
                <tr><td colspan="2" class="nm">${esc(l.product.name)}</td></tr>
                <tr><td class="qty">${l.qty} × ${formatNum(l.product.price.toDouble(), 0)}</td>
                    <td class="amt">${formatNum(l.lineTotal.toDouble(), 0)}</td></tr>
                """.trimIndent()
            }
        val tailRow =
            if (sale.creditDue > 0) {
                "<tr><td class=\"k b\">BALANCE DUE</td><td class=\"amt b\">${money(sale.creditDue)}</td></tr>"
            } else {
                "<tr><td class=\"k\">Change</td><td class=\"amt\">${money(maxOf(0, sale.change))}</td></tr>"
            }
        // Page width: thermal rolls print edge-to-edge at their physical width;
        // A4 gets a comfortable slip in the corner.
        val widthCss = if (paper == ReceiptSettings.Paper.A4) "190mm" else "${paper.cssWidthMm}mm"
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
                <tr><td class="k">Subtotal</td><td class="amt">${money(sale.subtotal)}</td></tr>
                <tr><td class="k">VAT 15%</td><td class="amt">${money(sale.vat)}</td></tr>
                ${if (sale.discount > 0) "<tr><td class=\"k\">Discount</td><td class=\"amt\">${money(sale.discount)}</td></tr>" else ""}
                <tr><td class="k b">TOTAL</td><td class="amt b">${money(sale.total)}</td></tr>
                <tr><td class="k">Paid · ${esc(sale.pay)}</td><td class="amt">${money(sale.received)}</td></tr>
                $tailRow
              </table>
              <hr>
              <div class="ft">$footer</div>
              <div class="ft">powered by NexaPOS · ${esc(sale.invoiceNo)}</div>
            </body></html>
            """.trimIndent()
    }

    private fun esc(raw: String): String =
        raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // -----------------------------------------------------------------------
    // PDF — render a compact monospace slip and open the share sheet.
    // -----------------------------------------------------------------------

    @Suppress("TooGenericExceptionCaught") // any rendering/IO failure is surfaced to the user via Toast
    fun sharePdf(
        context: Context,
        sale: SaleSnapshot,
    ) {
        try {
            val file = renderPdf(context, sale)
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Receipt ${sale.invoiceNo}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(send, "Share receipt PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderPdf(
        context: Context,
        sale: SaleSnapshot,
    ): File {
        val cols = COLS_80MM
        val lines = monoLines(context, sale, cols)

        val paint =
            Paint().apply {
                typeface = Typeface.MONOSPACE
                textSize = TEXT_SIZE
                color = Color.BLACK
                isAntiAlias = true
            }
        val boldPaint =
            Paint(paint).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }

        val contentWidth = lines.maxOf { paint.measureText(it) }
        val pageW = (contentWidth + MARGIN_X * 2).toInt().coerceAtLeast(MIN_PAGE_W)
        val pageH = (MARGIN_TOP + lines.size * LINE_HEIGHT + MARGIN_BOTTOM).toInt()

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = doc.startPage(pageInfo)
        var y = MARGIN_TOP
        lines.forEach { line ->
            val p = if (line.startsWith("TOTAL") || line.startsWith("BALANCE")) boldPaint else paint
            page.canvas.drawText(line, MARGIN_X, y, p)
            y += LINE_HEIGHT
        }
        doc.finishPage(page)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "receipt-${sale.invoiceNo}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Monospace receipt lines, [cols] characters wide. Shared by the PDF renderer. */
    private fun monoLines(
        context: Context,
        sale: SaleSnapshot,
        cols: Int,
    ): List<String> {
        val out = mutableListOf<String>()
        out += center(BusinessProfile.name(context), cols)
        BusinessProfile.receiptLines(context).forEach { out += center(it, cols) }
        out += "-".repeat(cols)
        out += "Invoice: ${sale.invoiceNo}"
        out += "Date: ${dateFmt.format(Date(sale.createdAt))}"
        out += "Customer: ${sale.customerName}"
        out += "-".repeat(cols)
        sale.lines.forEach { l ->
            out += l.product.name.take(cols)
            out += lr("  ${l.qty} x ${formatNum(l.product.price.toDouble(), 0)}", formatNum(l.lineTotal.toDouble(), 0), cols)
        }
        out += "-".repeat(cols)
        out += lr("Subtotal", money(sale.subtotal), cols)
        out += lr("VAT 15%", money(sale.vat), cols)
        if (sale.discount > 0) out += lr("Discount", money(sale.discount), cols)
        out += lr("TOTAL", money(sale.total), cols)
        out += lr("Paid (${sale.pay})", money(sale.received), cols)
        if (sale.creditDue > 0) {
            out += lr("BALANCE DUE", money(sale.creditDue), cols)
        } else {
            out += lr("Change", money(maxOf(0, sale.change)), cols)
        }
        out += "-".repeat(cols)
        ReceiptSettings.footerNote(context).chunked(cols).forEach { out += center(it, cols) }
        out += center("powered by NexaPOS", cols)
        return out
    }

    private fun lr(
        left: String,
        right: String,
        cols: Int,
    ): String {
        val gap = cols - left.length - right.length
        return if (gap >= 1) {
            left + " ".repeat(gap) + right
        } else {
            val trimmed = left.take((cols - right.length - 1).coerceAtLeast(0))
            "$trimmed $right"
        }
    }

    private fun center(
        s: String,
        cols: Int,
    ): String {
        if (s.length >= cols) return s.take(cols)
        val pad = (cols - s.length) / 2
        return " ".repeat(pad) + s
    }

    private const val COLS_80MM = 42
    private const val TEXT_SIZE = 10f
    private const val LINE_HEIGHT = 14f
    private const val MARGIN_X = 14f
    private const val MARGIN_TOP = 22f
    private const val MARGIN_BOTTOM = 22f
    private const val MIN_PAGE_W = 220
}
