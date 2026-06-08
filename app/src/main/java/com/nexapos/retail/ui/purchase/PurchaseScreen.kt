package com.nexapos.retail.ui.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.BadgeKind
import com.nexapos.retail.ui.components.Chip
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.SearchField
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.StatusBadge
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PO(val id: Long, val no: String, val supplier: String, val createdAt: Long, val items: Int, val amt: Int, val status: String, val pay: String)

private val poDateFmt = SimpleDateFormat("dd MMM HH:mm", Locale.US)
private val poExportDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
private val STATUS_CHIPS = listOf("All", "Received", "Partial", "Pending", "Cancelled")

private fun startOfMonth(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun purchasesCsv(rows: List<PO>): String {
    val sb = StringBuilder()
    sb.append("PO No,Supplier,Date,Items,Amount (Rs),Payment,Status\r\n")
    rows.forEach { r ->
        val cells =
            listOf(
                r.no,
                r.supplier,
                poExportDateFmt.format(Date(r.createdAt)),
                r.items.toString(),
                r.amt.toString(),
                r.pay,
                r.status,
            )
        sb.append(cells.joinToString(",") { cell -> csvCell(cell) }).append("\r\n")
    }
    return sb.toString()
}

private fun csvCell(raw: String): String {
    val needsQuote = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val escaped = raw.replace("\"", "\"\"")
    return if (needsQuote) "\"$escaped\"" else escaped
}

@Composable
fun PurchaseListScreen(
    vm: PurchasesViewModel,
    onNav: (String) -> Unit,
    onNewPurchase: () -> Unit,
    onScanReceipt: () -> Unit,
    onOpen: (purchaseId: Long) -> Unit,
) {
    val c = PosTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf("All") }
    var thisMonthOnly by remember { mutableStateOf(false) }
    val monthStart = remember { startOfMonth() }
    val rows =
        remember(vm.purchases, query, chip, thisMonthOnly, monthStart) {
            vm.purchases
                .map { p -> PO(p.id, p.code, p.supplierName, p.createdAt, p.itemCount, (p.totalCents / 100).toInt(), p.status, p.paymentMethod) }
                .filter { po -> !thisMonthOnly || po.createdAt >= monthStart }
                .filter { po -> query.isBlank() || po.no.contains(query, true) || po.supplier.contains(query, true) }
                .filter { po -> chip == "All" || po.status.equals(chip, ignoreCase = true) }
        }

    // SAF: user picks where to save the CSV.
    val exportLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(purchasesCsv(rows).toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    android.widget.Toast.makeText(
                        context,
                        "Exported ${rows.size} purchase${if (rows.size == 1) "" else "s"}",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }.onFailure { e ->
                    android.widget.Toast.makeText(
                        context,
                        "Export failed: ${e.message}",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

    NavShell(active = "purchase", onNav = onNav) {
        AppBar(
            title = "Purchases",
            subtitle = "${vm.orderCount} orders · ${rsStr(vm.monthTotal)} this month",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(
                        PosIcons.filter,
                        if (thisMonthOnly) "This month ●" else "This month",
                    ) { thisMonthOnly = !thisMonthOnly }
                    SecBtn(PosIcons.download, "Export") {
                        if (rows.isEmpty()) {
                            android.widget.Toast.makeText(
                                context,
                                "Nothing to export with current filters.",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            exportLauncher.launch("nexapos-purchases.csv")
                        }
                    }
                    SecBtn(PosIcons.scan, "Scan receipt", onScanReceipt)
                    PrimaryBtn(PosIcons.plus, "New purchase", onNewPurchase)
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(query, { query = it }, "Search PO number, supplier…", Modifier.width(360.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STATUS_CHIPS.forEach { Chip(it, chip == it) { chip = it } }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp)) {
            Column(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp))) {
                Row(
                    Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Th("PO · SUPPLIER", Modifier.weight(1f))
                    Th("AMOUNT", Modifier.width(110.dp), TextAlign.End)
                    Th("STATUS", Modifier.width(96.dp), TextAlign.End)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    rows.forEach { row -> PORow(row) { onOpen(row.id) } }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Row(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Showing ${rows.size} of ${vm.orderCount}", fontSize = 12.sp, color = c.muted)
                }
            }
        }
    }
}

@Composable
private fun PORow(
    p: PO,
    onOpen: () -> Unit,
) {
    val c = PosTheme.colors
    // Date · items · payment collapse into a subtitle so the row fits any width.
    val sub = "${poDateFmt.format(Date(p.createdAt))} · ${p.items} item${if (p.items == 1) "" else "s"} · ${p.pay.replace('_', ' ').replaceFirstChar { it.uppercase() }}"
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${p.no} · ${p.supplier}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(sub, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(rsStr(p.amt), Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.End)
        Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
            StatusBadge(
                p.status,
                if (p.status == "received") {
                    BadgeKind.PAID
                } else if (p.status == "partial") {
                    BadgeKind.AMBER
                } else {
                    BadgeKind.DUE
                },
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun Th(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(text, modifier = modifier, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted, textAlign = align)
}
