package com.nexapos.retail.ui.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.CountUp
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.ResponsiveSplit
import com.nexapos.retail.ui.components.formatNum
import com.nexapos.retail.ui.components.isPortrait
import com.nexapos.retail.ui.sale.SaleSnapshot
import com.nexapos.retail.ui.sale.SellingViewModel
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

private fun rs(n: Int) = "Rs " + formatNum(n.toDouble(), 0)

private val receiptDateFmt = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.US)
private val barcodeDateFmt = java.text.SimpleDateFormat("ddMMyyyy", java.util.Locale.US)

private val receiptPaper = Color(0xFFFBF7EE)
private val receiptInk = Color(0xFF14110C)
private val receiptMuted = Color(0xFF5B5246)
private val receiptDash = Color(0xFFC8BDA5)

@Composable
fun PosReceiptScreen(
    vm: SellingViewModel,
    onNewSale: () -> Unit,
    onBack: () -> Unit,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    val sale = vm.lastSale ?: return
    NavShell(active = "pos", onNav = onNav) {
        AppBar(
            title = "Sale complete",
            subtitle = "Invoice ${sale.invoiceNo} · paid in ${sale.pay}",
            right = {
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.raised)
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Back to POS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                }
            },
        )
        Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
            ResponsiveSplit(
                portrait = isPortrait(),
                secondaryWidthDp = 360,
                primary = { mod -> Confirmation(sale, vm.nextInvoiceNo, onNewSale, mod) },
                secondary = { mod ->
                    Box(mod, contentAlignment = Alignment.Center) { ReceiptPaper(sale) }
                },
            )
        }
    }
}

@Composable
private fun Confirmation(
    sale: SaleSnapshot,
    nextInvoiceNo: String,
    onNewSale: () -> Unit,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var showShare by remember { mutableStateOf(false) }

    if (showShare) {
        ShareReceiptDialog(
            sale = sale,
            onDismiss = { showShare = false },
        )
    }

    Column(modifier, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(c.emerald))
            Text("PAYMENT CONFIRMED", fontSize = 13.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.Bold, color = c.emerald)
        }
        Spacer(Modifier.height(20.dp))
        CountUp(sale.total.toDouble(), prefix = "Rs ", decimals = 0, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            if (sale.creditDue > 0) {
                "on credit · balance due ${rs(sale.creditDue)}"
            } else {
                "received · change ${rs(maxOf(0, sale.change))}"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.muted,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionBtn(PosIcons.print, "Print receipt", primary = true) { ReceiptOutput.print(context, sale) }
            ActionBtn(PosIcons.share, "SMS / WhatsApp", primary = false) { showShare = true }
            ActionBtn(PosIcons.download, "PDF", primary = false) { ReceiptOutput.sharePdf(context, sale) }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.amberSoft), contentAlignment = Alignment.Center) {
                PosIcon(PosIcons.cart, tint = c.amberPress, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("New ticket $nextInvoiceNo ready", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Text("Auto-incremented · scan to begin", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.amber)
                    .clickable { onNewSale() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Start", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                PosIcon(PosIcons.arrowR, tint = Color.White, size = 14.dp)
            }
        }
    }
}

@Composable
private fun ActionBtn(
    paths: List<String>,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) c.amber else c.raised)
            .then(if (primary) Modifier else Modifier.border(1.dp, c.hairline, RoundedCornerShape(14.dp)))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosIcon(paths, tint = if (primary) Color.White else c.ink, size = 18.dp)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (primary) Color.White else c.ink)
    }
}

@Composable
private fun ShareReceiptDialog(
    sale: SaleSnapshot,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Send receipt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (sale.customerPhone.isNotBlank()) {
                        "Send to ${sale.customerName} (${sale.customerPhone})."
                    } else {
                        "No phone on file — you'll pick the recipient in the messaging app."
                    },
                    fontSize = 12.sp,
                    color = c.muted,
                )
                ShareOption(PosIcons.download, "Receipt PDF", "The full receipt as a PDF — send via WhatsApp, email, etc.") {
                    ReceiptOutput.sharePdf(context, sale)
                    onDismiss()
                }
                ShareOption(PosIcons.mobile, "SMS (text)", "Opens your default messaging app") {
                    ReceiptOutput.sendSms(context, sale.customerPhone, ReceiptOutput.messageText(context, sale))
                    onDismiss()
                }
                ShareOption(PosIcons.share, "WhatsApp", "Opens WhatsApp with the receipt text") {
                    ReceiptOutput.sendWhatsApp(context, sale.customerPhone, ReceiptOutput.messageText(context, sale))
                    onDismiss()
                }
            }
        },
    )
}

@Composable
private fun ShareOption(
    icon: List<String>,
    label: String,
    sub: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(c.raised2), contentAlignment = Alignment.Center) {
            PosIcon(icon, tint = c.ink, size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(sub, fontSize = 11.sp, color = c.muted)
        }
        PosIcon(PosIcons.arrowR, tint = c.muted, size = 16.dp)
    }
}

@Composable
private fun ReceiptPaper(sale: SaleSnapshot) {
    val context = LocalContext.current
    val businessName = BusinessProfile.name(context)
    val receiptLines = BusinessProfile.receiptLines(context)
    val vatRegistered = BusinessProfile.vatRegistered(context)
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(animationSpec = tween(600), initialOffsetY = { it }) + fadeIn(tween(400)),
    ) {
        Column(
            Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(receiptPaper)
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(businessName, fontFamily = HankenGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = receiptInk)
            receiptLines.forEach { line ->
                Text(line, fontSize = 10.sp, color = receiptMuted)
            }
            DashedLine()
            RcRow("Invoice", sale.invoiceNo)
            RcRow("Date", receiptDateFmt.format(java.util.Date(sale.createdAt)))
            RcRow("Cashier", "—")
            RcRow("Customer", sale.customerName)
            DashedLine()
            sale.lines.forEach { l ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(l.product.name, fontFamily = HankenGrotesk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = receiptInk, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${l.qty} × ${formatNum(l.product.price.toDouble(), 0)}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = receiptInk)
                    Text(formatNum(l.lineTotal.toDouble(), 0), fontFamily = JetBrainsMono, fontSize = 11.sp, color = receiptInk)
                }
                Spacer(Modifier.height(4.dp))
            }
            DashedLine()
            RcMoney(if (vatRegistered) "Subtotal (incl. VAT)" else "Subtotal", sale.subtotal)
            if (vatRegistered) RcMoney("VAT 15% (incl.)", sale.vat)
            RcMoney("Discount", sale.discount)
            Box(Modifier.fillMaxWidth().height(1.dp).background(receiptInk).padding(vertical = 4.dp))
            RcMoney("TOTAL", sale.total, big = true)
            RcMoney("Paid · ${sale.pay}", sale.received)
            if (sale.creditDue > 0) {
                RcMoney("BALANCE DUE", sale.creditDue, big = true)
            } else {
                RcMoney("Change", maxOf(0, sale.change))
            }
            if (sale.creditDue > 0) {
                Text(
                    "On credit to ${sale.customerName} · added to their account.",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = receiptMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            DashedLine()
            Text(
                com.nexapos.retail.data.profile.ReceiptSettings.footerNote(context),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = receiptMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text("powered by NexaPOS", fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = receiptInk, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.height(40.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(48) { i ->
                    Box(Modifier.width(if (i % 3 == 0) 2.dp else 1.dp).fillMaxHeight().background(receiptInk))
                }
            }
            Text(
                "${sale.invoiceNo}-${barcodeDateFmt.format(java.util.Date(sale.createdAt))}",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 0.12.em,
                color = receiptInk,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DashedLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(receiptDash),
    )
}

@Composable
private fun RcRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 11.sp, color = receiptInk)
        Text(value, fontFamily = JetBrainsMono, fontSize = 11.sp, color = receiptInk)
    }
}

@Composable
private fun RcMoney(
    label: String,
    value: Int,
    big: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = HankenGrotesk, fontSize = if (big) 14.sp else 11.5.sp, fontWeight = if (big) FontWeight.ExtraBold else FontWeight.Medium, color = receiptInk)
        Text("Rs ${formatNum(value.toDouble(), 0)}", fontFamily = JetBrainsMono, fontSize = if (big) 14.sp else 11.5.sp, fontWeight = if (big) FontWeight.ExtraBold else FontWeight.Medium, color = receiptInk)
    }
}
