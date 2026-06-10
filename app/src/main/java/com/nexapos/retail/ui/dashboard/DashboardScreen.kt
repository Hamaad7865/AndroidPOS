package com.nexapos.retail.ui.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.CountUp
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    val greeting =
        remember {
            val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                h < 12 -> "Good morning"
                h < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }
    val today =
        remember {
            java.text.SimpleDateFormat("EEE dd MMM · HH:mm", java.util.Locale.US).format(java.util.Date())
        }
    NavShell(active = "home", onNav = onNav) {
        AppBar(
            title = greeting,
            subtitle = today,
            right = {
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.amber)
                        .clickable { onNav("pos") }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PosIcon(PosIcons.cart, tint = Color.White, size = 14.dp)
                    Text("Open POS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ShiftStatusBanner(onNav)
            // KPI band — real numbers only; trends/sparks appear once history exists.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Today's Sales", vm.todaySales, null, c.amber, PosIcons.cart, emptyList(), Modifier.weight(1f))
                StatCard("Stock Value", vm.stockValue, null, c.ink, PosIcons.box, emptyList(), Modifier.weight(1f))
                StatCard("Tickets today", vm.todayCount, null, c.graphite, PosIcons.receipt, emptyList(), Modifier.weight(1f), prefix = "")
                StatCard("Sales (week)", vm.salesWeek, null, c.emerald, PosIcons.chart, emptyList(), Modifier.weight(1f))
            }

            // Chart + top movers — placeholders until aggregations are wired
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DashboardPlaceholder(
                    "Sales vs Purchase chart",
                    "Appears once you have at least two days of sales/purchases recorded.",
                    Modifier.weight(1.65f),
                )
                DashboardPlaceholder(
                    "Top movers",
                    "Your best-selling SKUs over the past week will appear here.",
                    Modifier.weight(1f),
                )
            }

            // Inventory band
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Items in stock", vm.itemCount, null, c.ink, PosIcons.box, emptyList(), Modifier.weight(1f), prefix = "", sub = "SKUs")
                StatCard("Categories", vm.categoryCount, null, c.graphite, PosIcons.filter, emptyList(), Modifier.weight(1f), prefix = "")
                StatCard("Low stock", vm.lowStockCount, null, c.low, PosIcons.bell, emptyList(), Modifier.weight(1f), prefix = "", sub = "alerts")
                StatCard("Suppliers", vm.supplierCount, null, c.graphite, PosIcons.truck, emptyList(), Modifier.weight(1f), prefix = "", sub = "active")
            }

            // Quick actions + live activity — both wrapped as Machined cards so
            // they share the same chrome and stay vertically aligned in the row.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Machined(Modifier.weight(1.4f)) {
                    Eyebrow("Quick actions")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickTile("New sale", PosIcons.cart, c.amber, Modifier.weight(1f)) { onNav("pos") }
                        QuickTile("Add product", PosIcons.box, c.ink, Modifier.weight(1f)) { onNav("products") }
                        QuickTile("Record expense", PosIcons.wallet, c.crimson, Modifier.weight(1f)) { onNav("money") }
                        QuickTile("Purchase", PosIcons.truck, c.graphite, Modifier.weight(1f)) { onNav("purchase") }
                        QuickTile("Print labels", PosIcons.barcode, c.ink, Modifier.weight(1f)) { onNav("products") }
                    }
                }
                LiveActivityCard(
                    recentSales = vm.recentSales,
                    lowStockItems = vm.lowStockItems,
                    onSalesClick = { onNav("sales-list") },
                    onProductsClick = { onNav("products") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DashboardPlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    Machined(modifier) {
        Eyebrow(title)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.muted,
        )
    }
}

@Composable
private fun LiveActivityCard(
    recentSales: List<com.nexapos.retail.data.entity.Sale>,
    lowStockItems: List<com.nexapos.retail.data.entity.Product>,
    onSalesClick: () -> Unit,
    onProductsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    val dayFmt = remember { java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale.US) }
    Machined(modifier) {
        Eyebrow("Live activity")
        Spacer(Modifier.height(10.dp))
        if (recentSales.isEmpty() && lowStockItems.isEmpty()) {
            Text(
                "Recent sales and low-stock alerts will appear here once activity starts.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = c.muted,
            )
            return@Machined
        }
        // Recent sales
        if (recentSales.isNotEmpty()) {
            recentSales.take(MAX_RECENT_SALES).forEachIndexed { i, sale ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSalesClick() }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(c.emerald),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            sale.receiptNo,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.ink,
                            maxLines = 1,
                        )
                        Text(
                            "${sale.customerName} · ${dayFmt.format(java.util.Date(sale.createdAt))}",
                            fontSize = 11.sp,
                            color = c.muted,
                            maxLines = 1,
                        )
                    }
                    Text(
                        "Rs " + (sale.totalCents / 100),
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.ink,
                    )
                }
                if (i < recentSales.take(MAX_RECENT_SALES).size - 1) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                }
            }
        }
        // Low-stock alerts band
        if (lowStockItems.isNotEmpty()) {
            if (recentSales.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Spacer(Modifier.height(10.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onProductsClick() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PosIcon(PosIcons.bell, tint = c.low, size = 14.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "${lowStockItems.size} low-stock alert${if (lowStockItems.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.low,
                        maxLines = 1,
                    )
                    val sample = lowStockItems.take(2).joinToString(", ") { it.name }
                    val moreNote = if (lowStockItems.size > 2) " + ${lowStockItems.size - 2} more" else ""
                    Text(
                        sample + moreNote,
                        fontSize = 11.sp,
                        color = c.muted,
                        maxLines = 1,
                    )
                }
                Text("›", fontSize = 16.sp, color = c.muted)
            }
        }
    }
}

private const val MAX_RECENT_SALES = 3

@Composable
private fun Machined(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier
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
private fun QuickTile(
    label: String,
    icon: List<String>,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .height(80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
            PosIcon(icon, tint = c.surface, size = 18.dp)
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, color = c.ink, maxLines = 2)
    }
}

@Composable
private fun StatCard(
    eyebrow: String,
    value: Int,
    trend: Int?,
    color: Color,
    icon: List<String>,
    spark: List<Int>,
    modifier: Modifier = Modifier,
    prefix: String = "Rs ",
    sub: String? = null,
) {
    val c = PosTheme.colors
    Machined(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow)
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.raised2).border(1.dp, c.hairline, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                PosIcon(icon, tint = c.ink, size = 16.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            CountUp(value.toDouble(), prefix = prefix, decimals = 0, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            if (sub != null) {
                Spacer(Modifier.width(6.dp))
                Text(sub, fontSize = 15.sp, color = c.muted, fontWeight = FontWeight.Bold)
            }
        }
        if (trend != null || spark.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (trend != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PosIcon(if (trend >= 0) PosIcons.arrowUp else PosIcons.arrowDn, tint = if (trend >= 0) c.emerald else c.crimson, size = 12.dp)
                        Text("${kotlin.math.abs(trend)}%", fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (trend >= 0) c.emerald else c.crimson)
                        Text("vs last week", fontSize = 12.sp, color = c.muted)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (spark.isNotEmpty()) {
                    Spark(spark, color, Modifier.width(90.dp).height(28.dp))
                }
            }
        }
    }
}

/**
 * Till-shift status strip: green-ish when a shift is open, amber nudge when
 * not. Tapping it opens the Shift screen. Reads the open shift straight from
 * the container so the dashboard VM stays untouched.
 */
@Composable
private fun ShiftStatusBanner(onNav: (String) -> Unit) {
    val c = PosTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.nexapos.retail.PosApplication).container
    val shift by container.shiftRepository.observeOpenShift().collectAsState(initial = null)
    val open = shift != null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (open) c.raised else c.amberTint)
            .border(1.dp, if (open) c.hairline else c.amberSoft, RoundedCornerShape(12.dp))
            .clickable { onNav("shift") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosIcon(PosIcons.wallet, tint = if (open) c.emerald else c.amberPress, size = 16.dp)
        Column(Modifier.weight(1f)) {
            Text(
                if (open) {
                    "Shift open since " +
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(shift!!.openedAt)) +
                        " · ${shift!!.staffName}"
                } else {
                    "No shift open"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
            )
            Text(
                if (open) "Tap for live totals or to close the till" else "Open the till with a float so the day's cash is accountable",
                fontSize = 11.sp,
                color = c.muted,
            )
        }
        Text("›", fontSize = 18.sp, color = c.muted)
    }
}

@Composable
private fun Spark(
    data: List<Int>,
    color: Color,
    modifier: Modifier,
) {
    // Need at least 2 points to draw a line; bail out silently for empty / 1-pt data.
    if (data.size < 2) return
    Canvas(modifier) {
        val pad = 4f
        val max = (data.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
        // data.size >= 2 here so (data.size - 1) >= 1, no divide-by-zero.
        val step = (size.width - pad * 2) / (data.size - 1)
        val pts = data.mapIndexed { i, v -> Offset(pad + i * step, size.height - pad - (v / max) * (size.height - pad * 2)) }
        val line =
            Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
        val area =
            Path().apply {
                addPath(line)
                lineTo(pts.last().x, size.height - pad)
                lineTo(pts.first().x, size.height - pad)
                close()
            }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f))))
        drawPath(line, color, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 3f, pts.last())
    }
}
