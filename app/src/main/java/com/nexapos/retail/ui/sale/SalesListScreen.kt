package com.nexapos.retail.ui.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.formatNum
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFmt = SimpleDateFormat("dd MMM · HH:mm", Locale.US)

private fun rs(cents: Long): String = "Rs " + formatNum((cents / 100.0), 0)

@Composable
fun SalesListScreen(
    vm: SalesListViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
    onReturn: (Long) -> Unit = {},
) {
    val c = PosTheme.colors

    SaleDetailsDialog(
        sale = vm.detailSale,
        items = vm.detailItems,
        onDismiss = { vm.closeDetails() },
        onReturn = { sale ->
            vm.closeDetails()
            onReturn(sale.id)
        },
    )

    NavShell(active = "pos", onNav = onNav) {
        AppBar(
            title = "Sales",
            subtitle = "${vm.itemsCount} recent · Rs ${formatNum(vm.totalRupees.toDouble(), 0)} total",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn("Back to POS", onBack)
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (vm.sales.isEmpty()) {
                EmptyState()
            } else {
                Column(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HeadCell("WHEN", Modifier.width(130.dp))
                        HeadCell("INVOICE", Modifier.width(110.dp))
                        HeadCell("PAYMENT", Modifier.width(90.dp))
                        HeadCell("STATUS", Modifier.weight(1f))
                        HeadCell("TENDERED", Modifier.width(110.dp), TextAlign.End)
                        HeadCell("TOTAL", Modifier.width(120.dp), TextAlign.End)
                        Spacer(Modifier.width(20.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        vm.sales.forEach { s -> SaleRow(s, onClick = { vm.openDetails(s) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PosIcon(PosIcons.receipt, tint = c.muted, size = 28.dp)
        Spacer(Modifier.height(10.dp))
        Text("No sales yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Complete a sale from the POS screen and it shows up here.",
            fontSize = 12.sp,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeadCell(
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
private fun SaleRow(
    s: Sale,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(dayFmt.format(Date(s.createdAt)), Modifier.width(130.dp), fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.muted)
        Text(s.receiptNo, Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Text(s.paymentMethod, Modifier.width(90.dp), fontSize = 12.sp, color = c.graphite)
        Box(Modifier.weight(1f)) { StatusBadge(s.status) }
        Text(rs(s.amountTenderedCents), Modifier.width(110.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.graphite, textAlign = TextAlign.End)
        Text(rs(s.totalCents), Modifier.width(120.dp), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.End)
        Text("›", Modifier.width(20.dp), fontSize = 18.sp, color = c.muted, textAlign = TextAlign.End)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun StatusBadge(status: String) {
    val c = PosTheme.colors
    val (bg, fg) =
        when (status.uppercase()) {
            "COMPLETED" -> c.emeraldSoft to c.emerald
            "VOID" -> c.lowSoft to c.low
            "REFUND" -> c.amberSoft to c.amberPress
            else -> c.hairline2 to c.muted
        }
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(status.uppercase(), fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun SecBtn(
    label: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}
