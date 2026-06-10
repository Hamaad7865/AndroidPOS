package com.nexapos.retail.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.domain.StaffPolicy
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.session.rememberIsAdmin
import com.nexapos.retail.ui.theme.PosTheme

/**
 * Reports hub — a categorized list of every report the app can show. Tapping a
 * row navigates to "reports/<id>" which renders a single dedicated screen.
 *
 * Some entries are marked [available] = false — these need features the app
 * doesn't have yet (returns, double-entry bookkeeping, subscriptions). They're
 * listed so the shop owner sees what's coming, but tapping shows a polite
 * placeholder instead of leading nowhere.
 */
@Composable
fun ReportsScreen(
    onNav: (String) -> Unit,
    onOpenReport: (String) -> Unit,
) {
    val c = PosTheme.colors
    val admin = rememberIsAdmin()
    NavShell(active = "reports", onNav = onNav) {
        AppBar(
            title = "Reports",
            subtitle = if (admin) "Sales, purchases, profit, tax and more" else "Sales, purchases, tax and more",
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SECTIONS.forEach { section ->
                // Cashiers never see profit/cost reports (StaffPolicy).
                val items = section.items.filter { StaffPolicy.canSeeReport(admin, it.id) }
                if (items.isEmpty()) return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        section.title,
                        fontSize = 11.sp,
                        letterSpacing = 0.14.em,
                        fontWeight = FontWeight.SemiBold,
                        color = c.muted,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.raised)
                            .border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
                    ) {
                        items.forEachIndexed { i, item ->
                            ReportRow(
                                item = item,
                                onClick = { onOpenReport(item.id) },
                            )
                            if (i < items.size - 1) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(
    item: ReportItem,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(c.raised2).border(1.dp, c.hairline, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PosIcon(item.icon, tint = if (item.available) c.ink else c.muted, size = 16.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.available) c.ink else c.muted,
            )
            Text(
                if (item.available) item.subtitle else "${item.subtitle} · coming later",
                fontSize = 11.sp,
                color = c.muted,
            )
        }
        Text(
            "›",
            fontSize = 18.sp,
            color = c.muted,
        )
    }
}

internal data class ReportItem(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: List<String>,
    val available: Boolean = true,
)

internal data class ReportSection(
    val title: String,
    val items: List<ReportItem>,
)

private val SECTIONS =
    listOf(
        ReportSection(
            title = "Transactions",
            items =
                listOf(
                    ReportItem("sales", "Sales Report", "Every sale with totals and filters", PosIcons.cart),
                    ReportItem("sales-return", "Sales Return Report", "Returns and refunds", PosIcons.refresh),
                    ReportItem("purchase", "Purchase Report", "Every PO with totals and filters", PosIcons.truck),
                    ReportItem("due", "Due Report", "Customers with outstanding balance", PosIcons.bell),
                    ReportItem("daybook", "Day Book", "Every transaction, newest first", PosIcons.report),
                    ReportItem("all-tx", "All Transaction", "Same as Day Book — full chronological feed", PosIcons.report),
                    ReportItem("bill-profit", "Bill wise profit", "Per-sale margin (revenue − cost)", PosIcons.receipt),
                ),
        ),
        ReportSection(
            title = "Financial",
            items =
                listOf(
                    ReportItem("profit-loss", "Profit & Loss", "Revenue, costs, expenses → net profit", PosIcons.chart),
                    ReportItem("cashflow", "Cashflow", "Cash in vs cash out", PosIcons.cash),
                    ReportItem("tax", "Tax Report", "VAT collected by period", PosIcons.report),
                ),
        ),
        ReportSection(
            title = "Money",
            items =
                listOf(
                    ReportItem("income", "Income", "Manual income transactions", PosIcons.arrowUp),
                    ReportItem("income-categories", "Income Categories", "Income aggregated by category", PosIcons.filter),
                    ReportItem("expense", "Expense", "Manual expense transactions", PosIcons.arrowDn),
                ),
        ),
        ReportSection(
            title = "Per-product history",
            items =
                listOf(
                    ReportItem("product-sales", "Product Sale History", "Quantity sold and revenue per SKU", PosIcons.cart),
                    ReportItem("product-purchases", "Product Purchase History", "Quantity bought and cost per SKU", PosIcons.truck),
                ),
        ),
    )
