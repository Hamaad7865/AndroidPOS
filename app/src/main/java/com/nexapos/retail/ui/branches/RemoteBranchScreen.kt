package com.nexapos.retail.ui.branches

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.branch.DayDoc
import com.nexapos.retail.data.branch.StockItemDto
import com.nexapos.retail.domain.branch.RemoteBranchRepository
import com.nexapos.retail.domain.branch.RemoteBranchState
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class Tab(val label: String) { OVERVIEW("Overview"), STOCK("Stock"), SALES("Sales") }

/** Read-only detail of one remote branch: Overview / Stock / Sales tabs. */
@Composable
fun RemoteBranchScreen(
    code: String,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { (context.applicationContext as PosApplication).container.remoteBranches() }
    var tab by remember { mutableStateOf(Tab.OVERVIEW) }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(title = "Branch $code", subtitle = "Read-only · synced data", right = { SecBtn(null, "Back", onBack) })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (repo == null) {
                    BranchCardBox { Text("Not connected.", fontSize = 12.sp, color = PosTheme.colors.muted) }
                    return@Column
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tab.entries.forEach { t -> TabChip(t.label, tab == t) { tab = t } }
                }
                when (tab) {
                    Tab.OVERVIEW -> Overview(repo, code)
                    Tab.STOCK -> Stock(repo, code)
                    Tab.SALES -> Sales(repo, code)
                }
            }
        }
    }
}

@Composable
private fun Overview(
    repo: RemoteBranchRepository,
    code: String,
) {
    val c = PosTheme.colors
    val state by remember(code) { repo.observeState(code) }.collectAsState(initial = RemoteBranchState(null, null))
    val s = state.summary
    BranchCardBox {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Today")
            SyncLabel(state.lastSyncAt)
        }
        Spacer(Modifier.height(8.dp))
        if (s == null) {
            Text("This branch hasn't synced any data yet.", fontSize = 12.sp, color = c.muted)
        } else {
            Text(Money.format(s.salesTodayCents), fontFamily = JetBrainsMono, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            Spacer(Modifier.height(10.dp))
            Stat("Tickets today", s.ticketsToday.toString())
            Stat("Stock value", Money.format(s.stockValueCents))
            Stat("Items in stock", s.itemCount.toString())
            Stat("Low stock", s.lowStockCount.toString())
            s.openShiftStaff?.let { Stat("Open shift", it) }
        }
    }
}

@Composable
private fun Stock(
    repo: RemoteBranchRepository,
    code: String,
) {
    val c = PosTheme.colors
    val stock by remember(code) { repo.observeStock(code) }.collectAsState(initial = emptyList<StockItemDto>())
    BranchCardBox {
        Eyebrow("Stock · ${stock.size} items")
        Spacer(Modifier.height(8.dp))
        if (stock.isEmpty()) {
            Text("No stock synced yet.", fontSize = 12.sp, color = c.muted)
        } else {
            stock.sortedBy { it.name }.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(item.sku.ifBlank { "no SKU" }, fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = c.muted)
                    }
                    Text(Money.format(item.priceCents), fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.ink)
                    QtyBadge(item.stockQty, item.lowStockThreshold)
                }
            }
        }
    }
}

@Composable
private fun Sales(
    repo: RemoteBranchRepository,
    code: String,
) {
    val c = PosTheme.colors
    var date by remember { mutableStateOf(LocalDate.now(ZoneId.systemDefault())) }
    val key = date.toString()
    val day by produceState<DayDoc?>(initialValue = null, code, key) { value = repo.day(code, key) }
    BranchCardBox {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TabChip("‹", false) { date = date.minusDays(1) }
            Text(key, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
            TabChip("›", false) { if (date < LocalDate.now()) date = date.plusDays(1) }
        }
        Spacer(Modifier.height(10.dp))
        val sales = day?.sales.orEmpty()
        if (sales.isEmpty()) {
            Text("No sales recorded on this day.", fontSize = 12.sp, color = c.muted)
        } else {
            sales.forEach { sale ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${sale.receiptNo} · ${sale.customerName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("${timeOf(sale.createdAt)} · ${sale.paymentMethod}", fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = c.muted)
                    }
                    Text(Money.format(sale.totalCents), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("${sales.size} sales · ${Money.format(sales.sumOf { it.totalCents })}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
        }
    }
}

private fun timeOf(epochMs: Long): String {
    val t = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}

@Composable
private fun Stat(
    label: String,
    value: String,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = c.muted)
        Text(value, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun QtyBadge(
    qty: Int,
    low: Int,
) {
    val c = PosTheme.colors
    val (bg, fg) =
        when {
            qty <= 0 -> c.crimsonSoft to c.crimson
            qty <= low -> c.lowSoft to c.low
            else -> c.emeraldSoft to c.emerald
        }
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text(qty.toString(), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) c.amber else c.raised2).border(1.dp, if (selected) c.amber else c.hairline, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else c.ink)
    }
}
