package com.nexapos.retail.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.BadgeKind
import com.nexapos.retail.ui.components.Chip
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.StatusBadge
import com.nexapos.retail.ui.components.TabBar
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.reports.ExportButtons
import com.nexapos.retail.ui.reports.ReportData
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Entry(
    val id: String,
    val d: String,
    val cat: String,
    val desc: String,
    val amt: Int,
    val acc: String,
    val who: String,
    val createdAt: Long = 0,
)

private val entryDateFmt = SimpleDateFormat("dd MMM", Locale.US)
private val ledgerDateFmt = SimpleDateFormat("dd MMM · HH:mm", Locale.US)
private val exportDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

private fun MoneyTxn.toEntry() =
    Entry(
        id = code.ifEmpty { "#$id" },
        d = entryDateFmt.format(Date(createdAt)),
        cat = category,
        desc = description,
        amt = (amountCents / 100).toInt(),
        acc = account,
        who = createdBy,
        createdAt = createdAt,
    )

/** A completed sale shown as an income row (read-only — created from the POS). */
private fun Sale.toIncomeEntry() =
    Entry(
        id = receiptNo,
        d = entryDateFmt.format(Date(createdAt)),
        cat = "Sale",
        desc = customerName.ifBlank { "Walk-in" },
        amt = (totalCents / 100).toInt(),
        acc = paymentMethod.lowercase().replaceFirstChar { it.uppercase() },
        who = "",
        createdAt = createdAt,
    )

/** Render shape for one ledger line in the table. */
private data class Ledg(val d: String, val ref: String, val type: String, val desc: String, val dr: Long, val cr: Long, val bal: Long)

private fun LedgerLine.toLedg() =
    Ledg(
        d = ledgerDateFmt.format(Date(createdAt)),
        ref = ref,
        type = type,
        desc = description,
        dr = inCents,
        cr = outCents,
        bal = balanceCents,
    )

private val MONEY_TABS = listOf("accounts" to "Cash & Bank", "income" to "Income", "expense" to "Expenses", "ledger" to "Ledger")

private fun tabRoute(id: String) = if (id == "accounts") "money" else id

private val RIGHT_COLS = setOf("AMOUNT", "DEBIT", "CREDIT", "BALANCE")

private fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
private fun MoneyShell(
    active: String,
    title: String,
    subtitle: String,
    onNav: (String) -> Unit,
    rightExtra: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    NavShell(active = "money", onNav = onNav) {
        // Note: NavRail/BottomNav use the parent "money" tab; the in-screen TabBar handles sub-routes.
        AppBar(title = title, subtitle = subtitle, right = rightExtra)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp)) {
            TabBar(MONEY_TABS, active) { onNav(tabRoute(it)) }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Hub / "Cash & Bank" — live dashboard
// ---------------------------------------------------------------------------

@Composable
fun MoneyHubScreen(
    vm: MoneyViewModel,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    MoneyShell(
        active = "accounts",
        title = "Money",
        subtitle = "${rsStr(vm.netMonth)} net this month",
        onNav = onNav,
        rightExtra = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecBtn(PosIcons.wallet, "Shift / Till") { onNav("shift") }
                SecBtn(PosIcons.plus, "Expense") { onNav("add-expense") }
                PrimaryBtn(PosIcons.plus, "Income") { onNav("add-income") }
            }
        },
    ) {
        // This month at a glance.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(20.dp),
        ) {
            Eyebrow("This month")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalStat("Income", rsStr(vm.incomeMonth), "${rsStr(vm.salesMonth)} from sales", 22.sp, Modifier.weight(1f), valueColor = c.emerald)
                TotalStat("Expenses", rsStr(vm.expenseMonth), "${vm.expenses.size} entries", 22.sp, Modifier.weight(1f), valueColor = c.crimson)
                TotalStat("Net", rsStr(vm.netMonth), if (vm.netMonth >= 0) "profit" else "loss", 22.sp, Modifier.weight(1f), valueColor = if (vm.netMonth >= 0) c.emerald else c.crimson)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Accounts rollup (from manual income/expense account labels).
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp))) {
            SectionHeader("Accounts")
            if (vm.accounts.isEmpty()) {
                Text(
                    "Record an income or expense with an account name (e.g. \"Till 01\", \"MCB Current\") and its balance shows up here.",
                    Modifier.padding(18.dp),
                    fontSize = 12.sp,
                    color = c.muted,
                )
            } else {
                vm.accounts.forEach { AccountRow(it) }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Recent activity — the top of the cash ledger.
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp))) {
            SectionHeader("Recent activity", trailing = { SecBtn(null, "Open ledger") { onNav("ledger") } })
            val recent = vm.ledger.take(8)
            if (recent.isEmpty()) {
                Text(
                    "Sales, income and expenses will appear here as you record them.",
                    Modifier.padding(18.dp),
                    fontSize = 12.sp,
                    color = c.muted,
                )
            } else {
                recent.forEach { ActivityRow(it) }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    trailing: @Composable () -> Unit = {},
) {
    val c = PosTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(label, Modifier.weight(1f))
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

@Composable
private fun TotalStat(
    label: String,
    value: String,
    sub: String?,
    size: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier,
    valueColor: Color? = null,
) {
    val c = PosTheme.colors
    Column(modifier) {
        Eyebrow(label)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = JetBrainsMono, fontSize = size, fontWeight = FontWeight.ExtraBold, color = valueColor ?: c.ink)
        if (sub != null) Text(sub, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
    }
}

@Composable
private fun AccountRow(a: AccountSummary) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.raised2), contentAlignment = Alignment.Center) {
            PosIcon(PosIcons.wallet, tint = c.graphite, size = 16.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(a.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("in ${rsStr(a.inCents)} · out ${rsStr(a.outCents)}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
        }
        Text(rsStr(a.netCents), fontFamily = JetBrainsMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (a.netCents >= 0L) c.emerald else c.crimson)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun ActivityRow(line: LedgerLine) {
    val c = PosTheme.colors
    val moneyIn = line.inCents > 0L
    val bg = if (moneyIn) c.emeraldSoft else c.crimsonSoft
    val fg = if (moneyIn) c.emerald else c.crimson
    val icon = if (moneyIn) PosIcons.arrowUp else PosIcons.arrowDn
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
            PosIcon(icon, tint = fg, size = 15.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(line.description, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${line.ref} · ${ledgerDateFmt.format(Date(line.createdAt))}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            (if (moneyIn) "+ " else "− ") + rsStr(if (moneyIn) line.inCents else line.outCents),
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

// ---------------------------------------------------------------------------
// Income / Expense tables
// ---------------------------------------------------------------------------

@Composable
fun IncomeListScreen(
    vm: MoneyViewModel,
    onNav: (String) -> Unit,
) {
    // Income = real sales takings + manual income entries, newest first.
    val rows =
        remember(vm.recentSales, vm.incomes) {
            (vm.recentSales.map { sale -> sale.toIncomeEntry() } + vm.incomes.map { txn -> txn.toEntry() })
                .sortedByDescending { entry -> entry.createdAt }
        }
    val data = entriesReportData("Income", rows, showWho = false)
    MoneyShell("income", "Income", "${rsStr(vm.incomeMonth)} this month · ${rows.size} recent", onNav, rightExtra = { PrimaryBtn(PosIcons.plus, "Add income") { onNav("add-income") } }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sales takings are included automatically.", Modifier.weight(1f), fontSize = 12.sp, color = PosTheme.colors.muted)
            ExportButtons(data, enabled = rows.isNotEmpty())
        }
        Spacer(Modifier.height(12.dp))
        TableCard {
            TableHeader(listOf("ID" to 90.dp, "DATE" to 90.dp, "CATEGORY" to 100.dp, "DESCRIPTION" to (-1).dp, "ACCOUNT" to 120.dp, "AMOUNT" to 120.dp))
            if (rows.isEmpty()) {
                EmptyTableHint("No income yet. Complete a sale or tap Add income.")
            } else {
                rows.forEach { e -> EntryRow(e, BadgeKind.PAID, PosTheme.colors.emerald, showWho = false) }
            }
        }
    }
}

@Composable
fun ExpenseListScreen(
    vm: MoneyViewModel,
    onNav: (String) -> Unit,
) {
    var chip by remember { mutableStateOf("All") }
    val chips = listOf("All", "Rent", "Salary", "Utilities", "Transport", "Maintenance", "Supplies")
    val rows =
        remember(vm.expenses, chip) {
            vm.expenses.map { txn -> txn.toEntry() }.filter { entry -> chip == "All" || entry.cat == chip }
        }
    val data = entriesReportData("Expenses", rows, showWho = true)
    MoneyShell("expense", "Expenses", "${rsStr(vm.expenseMonth)} this month · ${rows.size} entries", onNav, rightExtra = { PrimaryBtn(PosIcons.plus, "Record expense") { onNav("add-expense") } }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { Chip(it, chip == it) { chip = it } }
            }
            ExportButtons(data, enabled = rows.isNotEmpty())
        }
        Spacer(Modifier.height(14.dp))
        TableCard {
            TableHeader(listOf("ID" to 80.dp, "DATE" to 90.dp, "CATEGORY" to 100.dp, "DESCRIPTION" to (-1).dp, "ACCOUNT" to 120.dp, "AMOUNT" to 110.dp, "BY" to 80.dp))
            if (rows.isEmpty()) {
                EmptyTableHint("No expenses${if (chip == "All") "" else " under $chip"} yet. Tap Record expense.")
            } else {
                rows.forEach { e -> EntryRow(e, BadgeKind.GHOST, PosTheme.colors.crimson, showWho = true) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ledger — live combined cash journal
// ---------------------------------------------------------------------------

@Composable
fun LedgerScreen(
    vm: MoneyViewModel,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    var todayOnly by remember { mutableStateOf(false) }
    val all = vm.ledger
    val lines =
        remember(all, todayOnly) {
            if (todayOnly) all.filter { line -> line.createdAt >= startOfTodayMillis() } else all
        }
    val net = remember(lines) { lines.sumOf { line -> line.inCents - line.outCents } }
    val data = ledgerReportData(lines, if (todayOnly) "Today" else "Recent")
    MoneyShell(
        active = "ledger",
        title = "Ledger",
        subtitle = "${lines.size} entries · net ${rsStr(net)}",
        onNav = onNav,
        rightExtra = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecBtn(PosIcons.filter, if (todayOnly) "All dates" else "Today") { todayOnly = !todayOnly }
                ExportButtons(data, enabled = lines.isNotEmpty())
            }
        },
    ) {
        if (lines.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PosIcon(PosIcons.chart, tint = c.muted, size = 28.dp)
                Spacer(Modifier.height(10.dp))
                Text(if (todayOnly) "Nothing today" else "No ledger entries yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (todayOnly) {
                        "No sales, income or expenses recorded today. Switch to All dates to see everything."
                    } else {
                        "As you complete sales and record income/expenses, every line shows here with a running balance."
                    },
                    fontSize = 12.sp,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            TableCard {
                TableHeader(
                    listOf(
                        "DATE" to 130.dp,
                        "REF" to 90.dp,
                        "TYPE" to 80.dp,
                        "DESCRIPTION" to (-1).dp,
                        "DEBIT" to 110.dp,
                        "CREDIT" to 110.dp,
                        "BALANCE" to 110.dp,
                    ),
                )
                lines.forEach { LedgerRow(it.toLedg()) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared table primitives
// ---------------------------------------------------------------------------

@Composable
private fun TableCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = PosTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)), content = content)
}

@Composable
private fun EmptyTableHint(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(28.dp), fontSize = 12.sp, color = PosTheme.colors.muted, textAlign = TextAlign.Center)
}

@Composable
private fun TableHeader(cols: List<Pair<String, androidx.compose.ui.unit.Dp>>) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        cols.forEach { (label, w) ->
            val m = if (w.value < 0) Modifier.weight(1f) else Modifier.width(w)
            val align = if (label in RIGHT_COLS) TextAlign.End else TextAlign.Start
            Text(label, modifier = m, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted, textAlign = align)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}

@Composable
private fun EntryRow(
    e: Entry,
    catKind: BadgeKind,
    amtColor: Color,
    showWho: Boolean,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(e.id, Modifier.width(if (showWho) 80.dp else 90.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(e.d, Modifier.width(90.dp), fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.muted)
        Box(Modifier.width(100.dp)) { StatusBadge(e.cat.ifBlank { "—" }, catKind) }
        Text(e.desc.ifBlank { "—" }, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(e.acc.ifBlank { "—" }, Modifier.width(120.dp), fontSize = 12.sp, color = c.graphite, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(rsStr(e.amt), Modifier.width(if (showWho) 110.dp else 120.dp), fontFamily = JetBrainsMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = amtColor, textAlign = TextAlign.End)
        if (showWho) Text(e.who.ifBlank { "—" }, Modifier.width(80.dp), fontSize = 12.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun LedgerRow(e: Ledg) {
    val c = PosTheme.colors
    val badge =
        when (e.type) {
            "sale", "income" -> BadgeKind.PAID
            "expense", "refund", "purchase" -> BadgeKind.DUE
            else -> BadgeKind.GHOST
        }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(e.d, Modifier.width(130.dp), fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(e.ref, Modifier.width(90.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(80.dp)) { StatusBadge(e.type.uppercase(), badge) }
        Text(e.desc, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (e.dr > 0L) rsStr(e.dr) else "—", Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = if (e.dr > 0L) FontWeight.Bold else FontWeight.Normal, color = if (e.dr > 0L) c.emerald else c.muted, textAlign = TextAlign.End)
        Text(if (e.cr > 0L) rsStr(e.cr) else "—", Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = if (e.cr > 0L) FontWeight.Bold else FontWeight.Normal, color = if (e.cr > 0L) c.crimson else c.muted, textAlign = TextAlign.End)
        Text(rsStr(e.bal), Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.End)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

// ---------------------------------------------------------------------------
// Export builders
// ---------------------------------------------------------------------------

private fun entriesReportData(
    title: String,
    rows: List<Entry>,
    showWho: Boolean,
): ReportData {
    val total = rows.sumOf { it.amt }
    val columns =
        buildList {
            addAll(listOf("ID", "Date", "Category", "Description", "Account", "Amount"))
            if (showWho) add("By")
        }
    val body =
        rows.map { e ->
            buildList {
                addAll(listOf(e.id, e.d, e.cat, e.desc, e.acc, rsStr(e.amt)))
                if (showWho) add(e.who)
            }
        }
    return ReportData(
        title = title,
        subtitle = "",
        periodLabel = "Recent",
        summary = listOf("Entries" to "${rows.size}", "Total" to rsStr(total)),
        columns = columns,
        rows = body,
    )
}

private fun ledgerReportData(
    lines: List<LedgerLine>,
    periodLabel: String,
): ReportData {
    val totalIn = lines.sumOf { it.inCents }
    val totalOut = lines.sumOf { it.outCents }
    return ReportData(
        title = "Cash Ledger",
        subtitle = "",
        periodLabel = periodLabel,
        summary =
            listOf(
                "Entries" to "${lines.size}",
                "Money in" to rsStr(totalIn),
                "Money out" to rsStr(totalOut),
                "Net" to rsStr(totalIn - totalOut),
            ),
        columns = listOf("Date", "Ref", "Type", "Description", "Account", "Debit (in)", "Credit (out)", "Balance"),
        rows =
            lines.map { l ->
                listOf(
                    exportDateFmt.format(Date(l.createdAt)),
                    l.ref,
                    l.type,
                    l.description,
                    l.account,
                    if (l.inCents > 0L) rsStr(l.inCents) else "",
                    if (l.outCents > 0L) rsStr(l.outCents) else "",
                    rsStr(l.balanceCents),
                )
            },
    )
}
