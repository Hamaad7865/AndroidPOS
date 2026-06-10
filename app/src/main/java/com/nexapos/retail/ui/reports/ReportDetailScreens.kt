package com.nexapos.retail.ui.reports

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.domain.StaffPolicy
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.session.rememberIsAdmin
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ===========================================================================
// Dispatcher
// ===========================================================================

@Composable
fun ReportDetailScreen(
    reportId: String,
    vm: ReportsViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val title = REPORT_TITLES[reportId] ?: "Report"
    NavShell(active = "reports", onNav = onNav) {
        AppBar(
            title = title,
            subtitle = REPORT_SUBTITLES[reportId] ?: "",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackBtn(onBack)
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Defence in depth: the hub already hides these rows from cashiers,
            // but the route is still reachable — never render profit data here.
            val admin = rememberIsAdmin()
            when {
                !StaffPolicy.canSeeReport(admin, reportId) -> AdminOnly()
                else -> ReportBody(reportId, vm)
            }
        }
    }
}

@Composable
private fun ReportBody(
    reportId: String,
    vm: ReportsViewModel,
) {
    when (reportId) {
        "sales" -> SalesReport(vm)
        "purchase" -> PurchaseReport(vm)
        "due" -> DueReport(vm)
        "daybook", "all-tx" -> DayBookReport(vm)
        "bill-profit" -> BillWiseProfitReport(vm)
        "profit-loss" -> ProfitLossReport(vm)
        "cashflow" -> CashflowReport(vm)
        "tax" -> TaxReport(vm)
        "income" -> IncomeReport(vm)
        "income-categories" -> IncomeCategoriesReport(vm)
        "expense" -> ExpenseReport(vm)
        "product-sales" -> ProductSaleHistoryReport(vm)
        "product-purchases" -> ProductPurchaseHistoryReport(vm)
        "sales-return" -> SalesReturnReport(vm)
        "purchase-return" -> ComingLater(reportId)
        else -> NotFound()
    }
}

private val REPORT_TITLES =
    mapOf(
        "sales" to "Sales Report",
        "sales-return" to "Sales Return Report",
        "purchase" to "Purchase Report",
        "purchase-return" to "Purchase Return Report",
        "due" to "Due Report",
        "daybook" to "Day Book",
        "all-tx" to "All Transaction",
        "bill-profit" to "Bill-wise Profit",
        "profit-loss" to "Profit & Loss",
        "cashflow" to "Cashflow",
        "tax" to "Tax Report",
        "income" to "Income",
        "income-categories" to "Income Categories",
        "expense" to "Expense",
        "product-sales" to "Product Sale History",
        "product-purchases" to "Product Purchase History",
        "sales-return" to "Sales Return Report",
    )

private val REPORT_SUBTITLES =
    mapOf(
        "sales" to "Every completed sale, filterable by period",
        "purchase" to "Every purchase order, filterable by period",
        "due" to "Customers with outstanding balance",
        "daybook" to "Every transaction, newest first",
        "all-tx" to "Every transaction, newest first",
        "bill-profit" to "Revenue − cost of goods sold, per bill",
        "profit-loss" to "Revenue, COGS, expenses → net profit",
        "cashflow" to "Cash in vs cash out by period",
        "tax" to "VAT collected by period",
        "income" to "Manual income entries",
        "income-categories" to "Income aggregated by category",
        "expense" to "Manual expense entries",
        "product-sales" to "Per-SKU quantity sold and revenue",
        "product-purchases" to "Per-SKU quantity bought and cost",
        "sales-return" to "Returns and refunds by period",
    )

// ===========================================================================
// Period selector (Today / Week / Month / All)
// ===========================================================================

private enum class Period(val label: String) {
    TODAY("Today"),
    WEEK("This week"),
    MONTH("This month"),
    ALL("All time"),
}

private data class Range(val from: Long, val to: Long)

private fun rangeFor(period: Period): Range {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    return when (period) {
        Period.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            Range(cal.timeInMillis, Long.MAX_VALUE)
        }
        Period.WEEK -> Range(now - SEVEN_DAYS_MS, Long.MAX_VALUE)
        Period.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            Range(cal.timeInMillis, Long.MAX_VALUE)
        }
        Period.ALL -> Range(0L, Long.MAX_VALUE)
    }
}

private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

@Composable
private fun PeriodChips(
    selected: Period,
    onSelect: (Period) -> Unit,
) {
    val c = PosTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Period.entries.forEach { p ->
            val active = p == selected
            Box(
                Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(if (active) c.ink else c.raised)
                    .border(1.dp, if (active) c.ink else c.hairline, CircleShape)
                    .clickable { onSelect(p) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    p.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) c.surface else c.ink,
                )
            }
        }
    }
}

/**
 * Top bar of every report: period chips on the left, export buttons on the
 * right. Pass null for [period]/[onPeriodChange] for reports that aren't
 * period-filtered (e.g. Due Report, Product histories).
 */
@Composable
private fun ReportBar(
    period: Period?,
    onPeriodChange: (Period) -> Unit,
    data: ReportData,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (period != null) PeriodChips(period, onPeriodChange)
        Spacer(Modifier.weight(1f))
        ExportButtons(data = data, enabled = data.summary.isNotEmpty() || data.rows.isNotEmpty())
    }
}

// ===========================================================================
// Shared bits
// ===========================================================================

@Composable
private fun BackBtn(onBack: () -> Unit) {
    val c = PosTheme.colors
    Box(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable { onBack() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text("Back", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink) }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

@Composable
private fun SumLine(
    label: String,
    value: String,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color? = null,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (bold) c.ink else c.muted,
        )
        Text(
            value,
            fontFamily = JetBrainsMono,
            fontSize = if (bold) 16.sp else 13.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = color ?: c.ink,
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    val c = PosTheme.colors
    Card {
        Text(text, fontSize = 13.sp, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ===========================================================================
// 1. Sales Report
// ===========================================================================

private val dayFmt = SimpleDateFormat("dd MMM · HH:mm", Locale.US)

@Composable
private fun SalesReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.salesInRange(r.from, r.to)
    val totalRupees = (rows.sumOf { it.totalCents } / 100).toInt()
    val data =
        ReportData(
            title = "Sales Report",
            subtitle = "Every completed sale, filterable by period",
            periodLabel = period.label,
            summary =
                listOf(
                    "Sales count" to "${rows.size}",
                    "Total revenue" to rsStr(totalRupees),
                ),
            columns = listOf("When", "Receipt", "Customer", "Payment", "Total (Rs)"),
            rows =
                rows.map { s ->
                    listOf(
                        dayFmt.format(Date(s.createdAt)),
                        s.receiptNo,
                        s.customerName,
                        s.paymentMethod,
                        "${s.totalCents / 100}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Sales count", "${rows.size}")
        SumLine("Total revenue", rsStr(totalRupees), bold = true, color = PosTheme.colors.emerald)
    }
    if (rows.isEmpty()) {
        EmptyCard("No sales in this period yet.")
    } else {
        Card {
            Eyebrow("Sales")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "RECEIPT", "CUSTOMER", "PAYMENT", "TOTAL")
            Divider()
            rows.forEach { s ->
                TableRow5(
                    dayFmt.format(Date(s.createdAt)),
                    s.receiptNo,
                    s.customerName,
                    s.paymentMethod,
                    rsStr((s.totalCents / 100).toInt()),
                )
            }
        }
    }
}

// ===========================================================================
// 2. Purchase Report
// ===========================================================================

@Composable
private fun PurchaseReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.purchasesInRange(r.from, r.to)
    val totalRupees = (rows.sumOf { it.totalCents } / 100).toInt()
    val data =
        ReportData(
            title = "Purchase Report",
            subtitle = "Every purchase order, filterable by period",
            periodLabel = period.label,
            summary =
                listOf(
                    "Purchase count" to "${rows.size}",
                    "Total spent" to rsStr(totalRupees),
                ),
            columns = listOf("When", "PO #", "Supplier", "Status", "Payment", "Total (Rs)"),
            rows =
                rows.map { p ->
                    listOf(
                        dayFmt.format(Date(p.createdAt)),
                        p.code,
                        p.supplierName,
                        p.status,
                        p.paymentMethod,
                        "${p.totalCents / 100}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Purchase count", "${rows.size}")
        SumLine("Total spent", rsStr(totalRupees), bold = true, color = PosTheme.colors.crimson)
    }
    if (rows.isEmpty()) {
        EmptyCard("No purchases in this period yet.")
    } else {
        Card {
            Eyebrow("Purchases")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "PO #", "SUPPLIER", "STATUS", "TOTAL")
            Divider()
            rows.forEach { p ->
                TableRow5(
                    dayFmt.format(Date(p.createdAt)),
                    p.code,
                    p.supplierName,
                    p.status.uppercase(),
                    rsStr((p.totalCents / 100).toInt()),
                )
            }
        }
    }
}

// ===========================================================================
// 3. Due Report
// ===========================================================================

@Composable
private fun DueReport(vm: ReportsViewModel) {
    val customers = vm.customersDue
    val totalRupees = (customers.sumOf { it.balanceCents } / 100).toInt()
    val data =
        ReportData(
            title = "Due Report",
            subtitle = "Customers with outstanding balance",
            periodLabel = "All time",
            summary =
                listOf(
                    "Customers with balance" to "${customers.size}",
                    "Total outstanding" to rsStr(totalRupees),
                ),
            columns = listOf("Name", "Phone", "Locality", "Balance (Rs)"),
            rows =
                customers.map { p ->
                    listOf(p.name, p.phone, p.locality, "${p.balanceCents / 100}")
                },
        )

    ReportBar(null, {}, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Customers with balance", "${customers.size}")
        SumLine("Total outstanding", rsStr(totalRupees), bold = true, color = PosTheme.colors.crimson)
    }
    if (customers.isEmpty()) {
        EmptyCard("Nobody owes money — everyone is paid up.")
    } else {
        Card {
            Eyebrow("Customers")
            Spacer(Modifier.height(10.dp))
            TableHeader("NAME", "PHONE", "LOCALITY", "—", "BALANCE")
            Divider()
            customers.forEach { p ->
                TableRow5(p.name, p.phone.ifBlank { "—" }, p.locality.ifBlank { "—" }, "", rsStr((p.balanceCents / 100).toInt()))
            }
        }
    }
}

// ===========================================================================
// 4. Day Book (and 5. All Transaction — same content)
// ===========================================================================

@Composable
private fun DayBookReport(vm: ReportsViewModel) {
    val c = PosTheme.colors
    val entries = vm.entries
    val data =
        ReportData(
            title = "Day Book",
            subtitle = "Every transaction, newest first",
            periodLabel = "All recent",
            summary =
                listOf(
                    "Entries" to "${entries.size}",
                    "Net" to rsStr(vm.netRupees),
                ),
            columns = listOf("When", "Ref", "Type", "Detail", "Amount (Rs)"),
            rows =
                entries.map { e ->
                    val type =
                        when (e) {
                            is DayBookEntry.SaleEntry -> "Sale"
                            is DayBookEntry.IncomeEntry -> "Income"
                            is DayBookEntry.ExpenseEntry -> "Expense"
                            is DayBookEntry.PurchaseEntry -> "Purchase"
                            is DayBookEntry.ReturnEntry -> "Return"
                        }
                    val positive = e is DayBookEntry.SaleEntry || e is DayBookEntry.IncomeEntry
                    listOf(
                        dayFmt.format(Date(e.createdAt)),
                        e.ref,
                        type,
                        e.description,
                        (if (positive) "+" else "-") + "${e.rupees}",
                    )
                },
        )

    ReportBar(null, {}, data)
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Net (recent activity)")
                Spacer(Modifier.height(4.dp))
                Text(
                    rsStr(vm.netRupees),
                    fontFamily = JetBrainsMono,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (vm.netRupees < 0) c.crimson else c.emerald,
                )
                Text(
                    "Sales + income, minus expenses + purchases — ${entries.size} transactions",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(c.raised2),
                contentAlignment = Alignment.Center,
            ) { PosIcon(PosIcons.chart, tint = c.ink, size = 22.dp) }
        }
    }
    if (entries.isEmpty()) {
        EmptyCard("No activity yet — complete a sale, record income/expense or confirm a purchase.")
        return
    }
    Card {
        Eyebrow("Day book")
        Spacer(Modifier.height(10.dp))
        TableHeader("WHEN", "REF", "TYPE", "DETAIL", "AMOUNT")
        Divider()
        entries.forEach { e ->
            val type =
                when (e) {
                    is DayBookEntry.SaleEntry -> "Sale"
                    is DayBookEntry.IncomeEntry -> "Income"
                    is DayBookEntry.ExpenseEntry -> "Expense"
                    is DayBookEntry.PurchaseEntry -> "Purchase"
                    is DayBookEntry.ReturnEntry -> "Return"
                }
            val positive = e is DayBookEntry.SaleEntry || e is DayBookEntry.IncomeEntry
            TableRow5(
                dayFmt.format(Date(e.createdAt)),
                e.ref,
                type,
                e.description,
                (if (positive) "+ " else "− ") + rsStr(e.rupees),
                valueColor = if (positive) c.emerald else c.crimson,
            )
        }
    }
}

// ===========================================================================
// 6. Bill-wise profit
// ===========================================================================

@Composable
private fun BillWiseProfitReport(vm: ReportsViewModel) {
    LaunchedEffect(vm.sales.size) { vm.ensureSaleItemsLoaded() }
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.salesInRange(r.from, r.to)
    val costByProduct = vm.products.associate { it.id to it.costCents }

    data class BillProfit(val sale: Sale, val revenue: Int, val cogs: Int) {
        val gross get() = revenue - cogs
        val margin get() = if (revenue > 0) gross * 100 / revenue else 0
    }
    val bills =
        rows.map { sale ->
            val items = vm.saleItemsBySaleId[sale.id].orEmpty()
            val cogsCents =
                items.sumOf { item ->
                    val unit = item.productId?.let { costByProduct[it] } ?: 0L
                    unit * item.quantity
                }
            BillProfit(
                sale = sale,
                revenue = (sale.totalCents / 100).toInt(),
                cogs = (cogsCents / 100).toInt(),
            )
        }
    val totalRev = bills.sumOf { it.revenue }
    val totalCogs = bills.sumOf { it.cogs }
    val totalGross = totalRev - totalCogs
    val data =
        ReportData(
            title = "Bill-wise Profit",
            subtitle = "Revenue − cost of goods sold, per bill",
            periodLabel = period.label,
            summary =
                listOf(
                    "Revenue" to rsStr(totalRev),
                    "Cost of goods sold" to rsStr(totalCogs),
                    "Gross profit" to rsStr(totalGross),
                ),
            columns = listOf("When", "Receipt", "Revenue (Rs)", "COGS (Rs)", "Gross (Rs)", "Margin %"),
            rows =
                bills.map { b ->
                    listOf(
                        dayFmt.format(Date(b.sale.createdAt)),
                        b.sale.receiptNo,
                        "${b.revenue}",
                        "${b.cogs}",
                        "${b.gross}",
                        "${b.margin}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Revenue", rsStr(totalRev))
        SumLine("Cost of goods sold", rsStr(totalCogs))
        SumLine("Gross profit", rsStr(totalGross), bold = true, color = if (totalGross >= 0) PosTheme.colors.emerald else PosTheme.colors.crimson)
    }
    if (bills.isEmpty()) {
        EmptyCard("No sales in this period yet.")
    } else {
        Card {
            Eyebrow("Per bill")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "RECEIPT", "REVENUE", "COGS", "MARGIN")
            Divider()
            bills.forEach { b ->
                TableRow5(
                    dayFmt.format(Date(b.sale.createdAt)),
                    b.sale.receiptNo,
                    rsStr(b.revenue),
                    rsStr(b.cogs),
                    rsStr(b.gross) + " (${b.margin}%)",
                    valueColor = if (b.gross >= 0) PosTheme.colors.emerald else PosTheme.colors.crimson,
                )
            }
        }
    }
}

// ===========================================================================
// 7. Profit & Loss
// ===========================================================================

@Composable
private fun ProfitLossReport(vm: ReportsViewModel) {
    LaunchedEffect(vm.sales.size) { vm.ensureSaleItemsLoaded() }
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val periodSales = vm.salesInRange(r.from, r.to)
    val revenue = (periodSales.sumOf { it.totalCents } / 100).toInt()
    val cogs = vm.cogsRupees(periodSales)
    val gross = revenue - cogs
    val incomeR = vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_INCOME).sumOf { (it.amountCents / 100).toInt() }
    val expenseR = vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_EXPENSE).sumOf { (it.amountCents / 100).toInt() }
    val net = gross + incomeR - expenseR
    val data =
        ReportData(
            title = "Profit & Loss",
            subtitle = "Revenue, COGS, expenses → net profit",
            periodLabel = period.label,
            summary =
                listOf(
                    "Sales revenue" to rsStr(revenue),
                    "Cost of goods sold" to rsStr(cogs),
                    "Gross profit" to rsStr(gross),
                    "Other income" to rsStr(incomeR),
                    "Expenses" to rsStr(expenseR),
                    "Net profit" to rsStr(net),
                ),
            columns = emptyList(),
            rows = emptyList(),
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Revenue side")
        Spacer(Modifier.height(8.dp))
        SumLine("Sales revenue", rsStr(revenue))
        SumLine("Cost of goods sold", "− " + rsStr(cogs))
        Spacer(Modifier.height(4.dp))
        Divider()
        Spacer(Modifier.height(4.dp))
        SumLine("Gross profit", rsStr(gross), bold = true, color = if (gross >= 0) PosTheme.colors.emerald else PosTheme.colors.crimson)
    }
    Card {
        Eyebrow("Other")
        Spacer(Modifier.height(8.dp))
        SumLine("Other income", "+ " + rsStr(incomeR), color = PosTheme.colors.emerald)
        SumLine("Expenses", "− " + rsStr(expenseR), color = PosTheme.colors.crimson)
    }
    Card {
        Eyebrow("Net result")
        Spacer(Modifier.height(8.dp))
        SumLine(
            "Net profit",
            rsStr(net),
            bold = true,
            color = if (net >= 0) PosTheme.colors.emerald else PosTheme.colors.crimson,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Net = (Sales − COGS) + Other income − Expenses. COGS uses each product's current cost; restatement on cost changes is intentional.",
            fontSize = 11.sp,
            color = PosTheme.colors.muted,
        )
    }
}

// ===========================================================================
// 8. Cashflow
// ===========================================================================

@Composable
private fun CashflowReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    // Cash actually received per sale = tendered − change. For a full-credit
    // sale that's 0 (nothing paid yet); for a partial credit it's the down-
    // payment; for cash/card it equals the total.
    val cashIn =
        (
            vm.salesInRange(r.from, r.to).sumOf { (it.amountTenderedCents - it.changeCents).coerceAtLeast(0) } / 100 +
                vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_INCOME).sumOf { it.amountCents } / 100
        ).toInt()
    val cashOut =
        (
            vm.purchasesInRange(r.from, r.to).sumOf { it.totalCents } / 100 +
                vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_EXPENSE).sumOf { it.amountCents } / 100
        ).toInt()
    val net = cashIn - cashOut
    val data =
        ReportData(
            title = "Cashflow",
            subtitle = "Cash in vs cash out by period",
            periodLabel = period.label,
            summary =
                listOf(
                    "Cash in" to rsStr(cashIn),
                    "Cash out" to rsStr(cashOut),
                    "Net cashflow" to rsStr(net),
                ),
            columns = emptyList(),
            rows = emptyList(),
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Cash flow")
        Spacer(Modifier.height(8.dp))
        SumLine("Cash in", "+ " + rsStr(cashIn), color = PosTheme.colors.emerald)
        SumLine("Cash out", "− " + rsStr(cashOut), color = PosTheme.colors.crimson)
        Spacer(Modifier.height(4.dp))
        Divider()
        Spacer(Modifier.height(4.dp))
        SumLine("Net cashflow", rsStr(net), bold = true, color = if (net >= 0) PosTheme.colors.emerald else PosTheme.colors.crimson)
        Spacer(Modifier.height(8.dp))
        Text(
            "Cash in = money actually received on sales (so a credit sale only counts its down-payment) + manual income. " +
                "Cash out = all purchase totals + manual expenses. Outstanding credit shows in the Due Report.",
            fontSize = 11.sp,
            color = PosTheme.colors.muted,
        )
    }
}

// ===========================================================================
// 9. Tax Report
// ===========================================================================

@Composable
private fun TaxReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val sales = vm.salesInRange(r.from, r.to)
    val taxRupees = (sales.sumOf { it.taxCents } / 100).toInt()
    val gross = (sales.sumOf { it.totalCents } / 100).toInt()
    val data =
        ReportData(
            title = "Tax Report",
            subtitle = "VAT collected by period",
            periodLabel = period.label,
            summary =
                listOf(
                    "Sales count" to "${sales.size}",
                    "Gross sales" to rsStr(gross),
                    "VAT collected" to rsStr(taxRupees),
                ),
            columns = listOf("When", "Receipt", "Gross (Rs)", "VAT (Rs)", "Net (Rs)"),
            rows =
                sales.map { s ->
                    val grossR = (s.totalCents / 100).toInt()
                    val vatR = (s.taxCents / 100).toInt()
                    listOf(dayFmt.format(Date(s.createdAt)), s.receiptNo, "$grossR", "$vatR", "${grossR - vatR}")
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("VAT summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Sales in period", "${sales.size}")
        SumLine("Gross sales", rsStr(gross))
        SumLine("VAT collected", rsStr(taxRupees), bold = true, color = PosTheme.colors.amberPress)
    }
    if (sales.isEmpty()) {
        EmptyCard("No sales in this period yet.")
    } else {
        Card {
            Eyebrow("Sales breakdown")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "RECEIPT", "GROSS", "VAT", "NET")
            Divider()
            sales.forEach { s ->
                val grossR = (s.totalCents / 100).toInt()
                val vatR = (s.taxCents / 100).toInt()
                TableRow5(
                    dayFmt.format(Date(s.createdAt)),
                    s.receiptNo,
                    rsStr(grossR),
                    rsStr(vatR),
                    rsStr(grossR - vatR),
                )
            }
        }
    }
}

// ===========================================================================
// 10. Income (list of MoneyTxn type=INCOME)
// ===========================================================================

@Composable
private fun IncomeReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_INCOME)
    val total = (rows.sumOf { it.amountCents } / 100).toInt()
    val data =
        ReportData(
            title = "Income",
            subtitle = "Manual income entries",
            periodLabel = period.label,
            summary =
                listOf(
                    "Entries" to "${rows.size}",
                    "Total income" to rsStr(total),
                ),
            columns = listOf("When", "Ref", "Category", "Description", "Amount (Rs)"),
            rows =
                rows.map { t ->
                    listOf(
                        dayFmt.format(Date(t.createdAt)),
                        t.code.ifBlank { "I-${t.id}" },
                        t.category,
                        t.description,
                        "${t.amountCents / 100}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Entries", "${rows.size}")
        SumLine("Total income", rsStr(total), bold = true, color = PosTheme.colors.emerald)
    }
    if (rows.isEmpty()) {
        EmptyCard("No income recorded in this period.")
    } else {
        Card {
            Eyebrow("Entries")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "REF", "CATEGORY", "DESCRIPTION", "AMOUNT")
            Divider()
            rows.forEach { t ->
                TableRow5(
                    dayFmt.format(Date(t.createdAt)),
                    t.code.ifBlank { "I-${t.id}" },
                    t.category.ifBlank { "—" },
                    t.description.ifBlank { "—" },
                    rsStr((t.amountCents / 100).toInt()),
                    valueColor = PosTheme.colors.emerald,
                )
            }
        }
    }
}

// ===========================================================================
// 11. Income Categories
// ===========================================================================

@Composable
private fun IncomeCategoriesReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.incomeByCategory(r.from, r.to)
    val total = rows.sumOf { it.second }
    val data =
        ReportData(
            title = "Income Categories",
            subtitle = "Income aggregated by category",
            periodLabel = period.label,
            summary =
                listOf(
                    "Categories" to "${rows.size}",
                    "Total income" to rsStr(total),
                ),
            columns = listOf("Category", "Amount (Rs)", "% of total"),
            rows =
                rows.map { (cat, amount) ->
                    val pct = if (total > 0) amount * 100 / total else 0
                    listOf(cat, "$amount", "$pct%")
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Categories", "${rows.size}")
        SumLine("Total income", rsStr(total), bold = true, color = PosTheme.colors.emerald)
    }
    if (rows.isEmpty()) {
        EmptyCard("No income in this period.")
    } else {
        Card {
            Eyebrow("By category")
            Spacer(Modifier.height(10.dp))
            rows.forEach { (cat, amount) ->
                val pct = if (total > 0) amount * 100 / total else 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.ink)
                        Text("$pct% of total", fontSize = 11.sp, color = PosTheme.colors.muted)
                    }
                    Text(rsStr(amount), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PosTheme.colors.emerald)
                }
                Divider()
            }
        }
    }
}

// ===========================================================================
// 12. Expense
// ===========================================================================

@Composable
private fun ExpenseReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.moneyInRange(r.from, r.to, MoneyTxn.TYPE_EXPENSE)
    val total = (rows.sumOf { it.amountCents } / 100).toInt()
    val data =
        ReportData(
            title = "Expense",
            subtitle = "Manual expense entries",
            periodLabel = period.label,
            summary =
                listOf(
                    "Entries" to "${rows.size}",
                    "Total expense" to rsStr(total),
                ),
            columns = listOf("When", "Ref", "Category", "Description", "Amount (Rs)"),
            rows =
                rows.map { t ->
                    listOf(
                        dayFmt.format(Date(t.createdAt)),
                        t.code.ifBlank { "E-${t.id}" },
                        t.category,
                        t.description,
                        "${t.amountCents / 100}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Entries", "${rows.size}")
        SumLine("Total expense", rsStr(total), bold = true, color = PosTheme.colors.crimson)
    }
    if (rows.isEmpty()) {
        EmptyCard("No expenses recorded in this period.")
    } else {
        Card {
            Eyebrow("Entries")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "REF", "CATEGORY", "DESCRIPTION", "AMOUNT")
            Divider()
            rows.forEach { t ->
                TableRow5(
                    dayFmt.format(Date(t.createdAt)),
                    t.code.ifBlank { "E-${t.id}" },
                    t.category.ifBlank { "—" },
                    t.description.ifBlank { "—" },
                    rsStr((t.amountCents / 100).toInt()),
                    valueColor = PosTheme.colors.crimson,
                )
            }
        }
    }
}

// ===========================================================================
// 13. Product Sale History
// ===========================================================================

@Composable
private fun ProductSaleHistoryReport(vm: ReportsViewModel) {
    LaunchedEffect(vm.sales.size) { vm.ensureSaleItemsLoaded() }
    val rows = vm.productActivity().filter { it.qtySold > 0 }.sortedByDescending { it.revenueRupees }
    val totalQty = rows.sumOf { it.qtySold }
    val totalRev = rows.sumOf { it.revenueRupees }
    val data =
        ReportData(
            title = "Product Sale History",
            subtitle = "Per-SKU quantity sold and revenue",
            periodLabel = "All time",
            summary =
                listOf(
                    "Products sold" to "${rows.size}",
                    "Total units" to "$totalQty",
                    "Total revenue" to rsStr(totalRev),
                ),
            columns = listOf("Product", "SKU", "Units", "Revenue (Rs)"),
            rows =
                rows.map { a ->
                    listOf(a.name, a.sku, "${a.qtySold}", "${a.revenueRupees}")
                },
        )

    ReportBar(null, {}, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Products sold", "${rows.size}")
        SumLine("Total units", "$totalQty")
        SumLine("Total revenue", rsStr(totalRev), bold = true, color = PosTheme.colors.emerald)
    }
    if (rows.isEmpty()) {
        EmptyCard("No products sold yet.")
    } else {
        Card {
            Eyebrow("Per product")
            Spacer(Modifier.height(10.dp))
            TableHeader("PRODUCT", "SKU", "UNITS", "—", "REVENUE")
            Divider()
            rows.forEach { a ->
                TableRow5(
                    a.name,
                    a.sku.ifBlank { "—" },
                    "${a.qtySold}",
                    "",
                    rsStr(a.revenueRupees),
                    valueColor = PosTheme.colors.emerald,
                )
            }
        }
    }
}

// ===========================================================================
// 14. Product Purchase History
// ===========================================================================

@Composable
private fun ProductPurchaseHistoryReport(vm: ReportsViewModel) {
    LaunchedEffect(vm.purchases.size) { vm.ensurePurchaseItemsLoaded() }
    val rows = vm.productActivity().filter { it.qtyPurchased > 0 }.sortedByDescending { it.purchaseCostRupees }
    val totalQty = rows.sumOf { it.qtyPurchased }
    val totalCost = rows.sumOf { it.purchaseCostRupees }
    val data =
        ReportData(
            title = "Product Purchase History",
            subtitle = "Per-SKU quantity bought and cost",
            periodLabel = "All time",
            summary =
                listOf(
                    "Products purchased" to "${rows.size}",
                    "Total units" to "$totalQty",
                    "Total spent" to rsStr(totalCost),
                ),
            columns = listOf("Product", "SKU", "Units", "Cost (Rs)"),
            rows =
                rows.map { a ->
                    listOf(a.name, a.sku, "${a.qtyPurchased}", "${a.purchaseCostRupees}")
                },
        )

    ReportBar(null, {}, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Products purchased", "${rows.size}")
        SumLine("Total units", "$totalQty")
        SumLine("Total spent", rsStr(totalCost), bold = true, color = PosTheme.colors.crimson)
    }
    if (rows.isEmpty()) {
        EmptyCard("No products purchased yet.")
    } else {
        Card {
            Eyebrow("Per product")
            Spacer(Modifier.height(10.dp))
            TableHeader("PRODUCT", "SKU", "UNITS", "—", "COST")
            Divider()
            rows.forEach { a ->
                TableRow5(
                    a.name,
                    a.sku.ifBlank { "—" },
                    "${a.qtyPurchased}",
                    "",
                    rsStr(a.purchaseCostRupees),
                    valueColor = PosTheme.colors.crimson,
                )
            }
        }
    }
}

// ===========================================================================
// 15. Sales Return Report
// ===========================================================================

@Composable
private fun SalesReturnReport(vm: ReportsViewModel) {
    var period by remember { mutableStateOf(Period.MONTH) }
    val r = rangeFor(period)
    val rows = vm.returnsInRange(r.from, r.to)
    val total = (rows.sumOf { it.totalCents } / 100).toInt()
    val cashRefunds = (rows.filter { it.refundMethod.equals("CASH", ignoreCase = true) }.sumOf { it.totalCents } / 100).toInt()
    val creditRefunds = total - cashRefunds
    val data =
        ReportData(
            title = "Sales Return Report",
            subtitle = "Returns and refunds by period",
            periodLabel = period.label,
            summary =
                listOf(
                    "Returns" to "${rows.size}",
                    "Cash refunds" to rsStr(cashRefunds),
                    "Credit refunds" to rsStr(creditRefunds),
                    "Total refunded" to rsStr(total),
                ),
            columns = listOf("When", "Return #", "Original", "Customer", "Method", "Refund (Rs)"),
            rows =
                rows.map { ret ->
                    listOf(
                        dayFmt.format(Date(ret.createdAt)),
                        ret.code,
                        ret.receiptNo,
                        ret.customerName,
                        ret.refundMethod,
                        "${ret.totalCents / 100}",
                    )
                },
        )

    ReportBar(period, { period = it }, data)
    Card {
        Eyebrow("Summary")
        Spacer(Modifier.height(8.dp))
        SumLine("Returns", "${rows.size}")
        SumLine("Cash refunds", rsStr(cashRefunds))
        SumLine("Credit refunds", rsStr(creditRefunds))
        SumLine("Total refunded", rsStr(total), bold = true, color = PosTheme.colors.crimson)
    }
    if (rows.isEmpty()) {
        EmptyCard("No returns in this period.")
    } else {
        Card {
            Eyebrow("Returns")
            Spacer(Modifier.height(10.dp))
            TableHeader("WHEN", "RETURN #", "ORIGINAL", "METHOD", "REFUND")
            Divider()
            rows.forEach { ret ->
                TableRow5(
                    dayFmt.format(Date(ret.createdAt)),
                    ret.code,
                    ret.receiptNo,
                    ret.refundMethod,
                    rsStr((ret.totalCents / 100).toInt()),
                    valueColor = PosTheme.colors.crimson,
                )
            }
        }
    }
}

// ===========================================================================
// Placeholders (purchase return)
// ===========================================================================

@Composable
private fun ComingLater(id: String) {
    val c = PosTheme.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(c.amberSoft),
                contentAlignment = Alignment.Center,
            ) { PosIcon(PosIcons.bell, tint = c.amberPress, size = 22.dp) }
            Column(Modifier.weight(1f)) {
                Text(REPORT_TITLES[id] ?: "Report", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Text(
                    when (id) {
                        "sales-return" -> "This report ships once the sales-return / refund flow is implemented."
                        "purchase-return" -> "This report ships once the purchase-return-to-supplier flow is implemented."
                        else -> "Not yet implemented."
                    },
                    fontSize = 12.sp,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun NotFound() {
    EmptyCard("Report not found.")
}

/** Shown when a cashier reaches a profit/cost report by route. */
@Composable
private fun AdminOnly() {
    val c = PosTheme.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(c.amberSoft),
                contentAlignment = Alignment.Center,
            ) { PosIcon(PosIcons.user, tint = c.amberPress, size = 22.dp) }
            Column(Modifier.weight(1f)) {
                Text("Admin only", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Text(
                    "This report shows cost and profit figures. Ask an admin to sign in to view it.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            }
        }
    }
}

// ===========================================================================
// Small table primitives
// ===========================================================================

@Composable
private fun TableHeader(
    a: String,
    b: String,
    c: String,
    d: String,
    e: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TH(a, Modifier.weight(1.2f))
        TH(b, Modifier.weight(1f))
        TH(c, Modifier.weight(1.2f))
        TH(d, Modifier.weight(1f))
        TH(e, Modifier.weight(0.9f), TextAlign.End)
    }
}

@Composable
private fun TH(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        letterSpacing = 0.06.em,
        fontWeight = FontWeight.SemiBold,
        color = PosTheme.colors.muted,
        textAlign = align,
    )
}

@Composable
private fun TableRow5(
    a: String,
    b: String,
    c: String,
    d: String,
    e: String,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val cols = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(a, Modifier.weight(1.2f), fontFamily = JetBrainsMono, fontSize = 12.sp, color = cols.muted, maxLines = 1)
        Text(b, Modifier.weight(1f), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cols.ink, maxLines = 1)
        Text(c, Modifier.weight(1.2f), fontSize = 12.sp, color = cols.ink, maxLines = 1)
        Text(d, Modifier.weight(1f), fontSize = 12.sp, color = cols.graphite, maxLines = 1)
        Text(
            e,
            Modifier.weight(0.9f),
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: cols.ink,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
    Divider()
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(PosTheme.colors.hairline2))
}
