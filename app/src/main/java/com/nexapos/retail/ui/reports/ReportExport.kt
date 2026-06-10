@file:Suppress("MatchingDeclarationName")

package com.nexapos.retail.ui.reports

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.csvCell
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Canonical export shape for any report. Each report builds one of these from
 * its computed numbers; both the CSV and PDF exporters render from this.
 *
 * - [summary] is a label/value pair list shown at the top of the document.
 *   Use for totals, counts, period info, etc.
 * - [columns] + [rows] is the tabular body. May be empty for summary-only
 *   reports like P&L / Cashflow.
 */
data class ReportData(
    val title: String,
    val subtitle: String,
    val periodLabel: String,
    val summary: List<Pair<String, String>>,
    val columns: List<String>,
    val rows: List<List<String>>,
) {
    /** Slug used for the suggested file name. */
    val fileSlug: String
        get() =
            title.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "report" }
}

// ---------------------------------------------------------------------------
// CSV / HTML builders — pure functions, easy to unit-test.
// ---------------------------------------------------------------------------

private val exportTsFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

internal fun reportCsv(data: ReportData): String {
    val sb = StringBuilder()
    // Header block as a series of "Label,Value" rows so Excel users can read it.
    sb.append("Report,").append(csvCell(data.title)).append("\r\n")
    sb.append("Period,").append(csvCell(data.periodLabel)).append("\r\n")
    sb.append("Generated,").append(csvCell(exportTsFmt.format(Date()))).append("\r\n")
    sb.append("\r\n")
    if (data.summary.isNotEmpty()) {
        sb.append("Summary\r\n")
        data.summary.forEach { (label, value) ->
            sb.append(csvCell(label)).append(",").append(csvCell(value)).append("\r\n")
        }
        sb.append("\r\n")
    }
    if (data.columns.isNotEmpty() && data.rows.isNotEmpty()) {
        sb.append(data.columns.joinToString(",") { csvCell(it) }).append("\r\n")
        data.rows.forEach { row ->
            sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
        }
    }
    return sb.toString()
}

internal fun reportHtml(
    data: ReportData,
    businessName: String,
): String {
    val biz = htmlEscape(businessName)
    val title = htmlEscape(data.title)
    val subtitle = htmlEscape(data.subtitle)
    val period = htmlEscape(data.periodLabel)
    val generated = htmlEscape(exportTsFmt.format(Date()))

    val summaryHtml =
        if (data.summary.isEmpty()) {
            ""
        } else {
            buildString {
                append("<table class=\"summary\"><tbody>")
                data.summary.forEach { (label, value) ->
                    append("<tr><td class=\"label\">${htmlEscape(label)}</td>")
                    append("<td class=\"value\">${htmlEscape(value)}</td></tr>")
                }
                append("</tbody></table>")
            }
        }

    val tableHtml =
        if (data.columns.isEmpty() || data.rows.isEmpty()) {
            "<p class=\"empty\">No line items.</p>"
        } else {
            buildString {
                append("<table class=\"rows\"><thead><tr>")
                data.columns.forEach { col -> append("<th>${htmlEscape(col)}</th>") }
                append("</tr></thead><tbody>")
                data.rows.forEach { row ->
                    append("<tr>")
                    row.forEach { cell -> append("<td>${htmlEscape(cell)}</td>") }
                    append("</tr>")
                }
                append("</tbody></table>")
            }
        }

    return """
        <!DOCTYPE html><html><head><meta charset="utf-8"><title>$title</title>
        <style>
          * { box-sizing: border-box; }
          body { margin: 24px; font-family: Arial, Helvetica, sans-serif; color: #111; }
          header { border-bottom: 2px solid #111; padding-bottom: 10px; margin-bottom: 18px; }
          header .biz { font-size: 11px; letter-spacing: 1.2px; text-transform: uppercase; color: #555; }
          header h1 { margin: 4px 0 2px 0; font-size: 22px; }
          header .meta { font-size: 11px; color: #555; }
          h2 { font-size: 12px; letter-spacing: 1px; text-transform: uppercase; color: #555; margin: 18px 0 8px 0; }
          table.summary { border-collapse: collapse; margin: 6px 0 12px 0; }
          table.summary td { padding: 4px 14px 4px 0; font-size: 12px; }
          table.summary td.label { color: #555; }
          table.summary td.value { font-family: 'Courier New', monospace; font-weight: 700; }
          table.rows { width: 100%; border-collapse: collapse; margin-top: 4px; }
          table.rows th { font-size: 10px; letter-spacing: .8px; text-align: left;
              border-bottom: 1.5px solid #111; padding: 6px 8px; color: #555; text-transform: uppercase; }
          table.rows td { font-size: 11px; padding: 6px 8px;
              border-bottom: 1px solid #ddd; vertical-align: top; }
          table.rows tr:nth-child(even) td { background: #fafafa; }
          .empty { font-size: 12px; color: #777; font-style: italic; }
          footer { margin-top: 30px; padding-top: 10px; border-top: 1px solid #ddd;
              font-size: 10px; color: #888; }
          @media print { body { margin: 12mm; } }
        </style></head>
        <body>
          <header>
            <div class="biz">$biz</div>
            <h1>$title</h1>
            <div class="meta">${if (subtitle.isNotBlank()) "$subtitle · " else ""}Period: $period · Generated: $generated</div>
          </header>
          ${if (data.summary.isNotEmpty()) "<h2>Summary</h2>$summaryHtml" else ""}
          ${if (data.columns.isNotEmpty()) "<h2>Details</h2>$tableHtml" else ""}
          <footer>Generated by NexaPOS</footer>
        </body></html>
        """.trimIndent()
}

private fun htmlEscape(raw: String): String =
    raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

// ---------------------------------------------------------------------------
// Print → "Save as PDF" via the Android system print framework.
// ---------------------------------------------------------------------------

private fun printReportPdf(
    context: Context,
    data: ReportData,
) {
    val html = reportHtml(data, BusinessProfile.name(context))
    val webView = WebView(context)
    webView.webViewClient =
        object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String,
            ) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter("nexapos-${data.fileSlug}")
                printManager.print(
                    "NexaPOS ${data.title}",
                    adapter,
                    PrintAttributes.Builder().build(),
                )
            }
        }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

// ---------------------------------------------------------------------------
// UI: the two-button Export row used by every report.
// ---------------------------------------------------------------------------

/**
 * Two compact pills — "Excel" (writes a .csv via the system file picker, Excel
 * opens it natively) and "PDF" (renders the report HTML through Android's
 * print framework — user picks "Save as PDF" or a real printer).
 */
@Composable
fun ExportButtons(
    data: ReportData,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val csvLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(reportCsv(data).toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    Toast.makeText(context, "Exported ${data.title} (Excel)", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ExportPill(
            icon = PosIcons.download,
            label = "Excel",
            enabled = enabled,
        ) {
            if (!enabled) return@ExportPill
            csvLauncher.launch("nexapos-${data.fileSlug}.csv")
        }
        ExportPill(
            icon = PosIcons.print,
            label = "PDF",
            enabled = enabled,
        ) {
            if (!enabled) return@ExportPill
            printReportPdf(context, data)
        }
    }
}

@Composable
private fun ExportPill(
    icon: List<String>,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosIcon(icon, tint = if (enabled) c.ink else c.muted, size = 13.dp)
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) c.ink else c.muted,
        )
    }
}
