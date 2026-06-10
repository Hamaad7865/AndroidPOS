package com.nexapos.retail.ui.checkout

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.CountUp
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.ProductTile
import com.nexapos.retail.ui.components.ResponsiveSplit
import com.nexapos.retail.ui.components.isPortrait
import com.nexapos.retail.ui.sale.SellingViewModel
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.Money

private fun rs(cents: Long) = Money.format(cents)

private val checkoutHeaderDateFmt = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.US)

private fun checkoutDateLabel(): String = checkoutHeaderDateFmt.format(java.util.Date())

private val PAY_TYPES = listOf("cash" to "Cash", "card" to "Card", "mobile" to "Juice", "credit" to "Credit")
private val PAY_ICONS = mapOf("cash" to PosIcons.cash, "card" to PosIcons.card, "mobile" to PosIcons.mobile, "credit" to PosIcons.wallet)

@Composable
fun PosCheckoutScreen(
    vm: SellingViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    NavShell(active = "pos", onNav = onNav) {
        AppBar(
            title = "Checkout",
            subtitle = "Invoice ${vm.nextInvoiceNo} · ${checkoutDateLabel()}",
            right = {
                Row(
                    Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.raised)
                        .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PosIcon(PosIcons.close, tint = c.ink, size = 14.dp)
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                }
            },
        )
        Column(Modifier.weight(1f).fillMaxWidth().padding(18.dp)) {
            ResponsiveSplit(
                portrait = isPortrait(),
                secondaryWidthDp = 420,
                primary = { mod -> BillCard(vm, mod.padding(end = if (isPortrait()) 0.dp else 9.dp, bottom = if (isPortrait()) 9.dp else 0.dp)) },
                secondary = { mod -> PayCard(vm, onComplete, mod.padding(start = if (isPortrait()) 0.dp else 9.dp, top = if (isPortrait()) 9.dp else 0.dp)) },
            )
        }
    }
}

@Composable
private fun BillCard(
    vm: SellingViewModel,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    val vatRegistered = com.nexapos.retail.data.profile.BusinessProfile.vatRegistered(androidx.compose.ui.platform.LocalContext.current)
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        CustomerPickerDialog(vm = vm, onDismiss = { showPicker = false })
    }
    MachinedCard(modifier) {
        // Scrollable upper region so the layout survives short tablet screens;
        // the TOTAL stays pinned at the bottom.
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)) {
                Eyebrow("Bill to")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(vm.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
                        Text(
                            vm.selectedCustomer?.phone?.ifBlank { "—" } ?: "—",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.amber)
                    }
                }
                if (vm.creditNeedsCustomer) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.lowSoft)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PosIcon(PosIcons.bell, tint = c.low, size = 14.dp)
                        Text(
                            "Credit sale needs a customer — tap Change to select one.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.low,
                        )
                    }
                }
            }
            Divider()
            Column(Modifier.fillMaxWidth()) {
                vm.workingLines.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(c.raised2), contentAlignment = Alignment.Center) {
                            ProductTile(kind = l.product.kind, size = 36.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(l.product.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.ink)
                            Text("${l.product.sku} · ${l.qty} × ${rs(l.effectivePriceCents)}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
                        }
                        Text(rs(l.lineTotalCents), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    }
                    Divider(c.hairline2)
                }
            }
            // charges
            var showDiscount by remember { mutableStateOf(false) }
            if (showDiscount) DiscountDialog(vm) { showDiscount = false }
            Column(
                Modifier.fillMaxWidth().background(c.raised2).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.raised)
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                        .clickable { showDiscount = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Discount", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text(
                        if (vm.totalDiscountCents > 0L) "− " + rs(vm.totalDiscountCents) else "Add discount",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (vm.totalDiscountCents > 0L) c.ink else c.amber,
                    )
                }
                NumField(
                    label = "Shipping",
                    valueCents = vm.shippingCents,
                    onChange = { v ->
                        vm.shippingCents = v
                        if (!vm.isCredit) vm.receivedCents = vm.totalCents
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Divider()
        Column(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 16.dp)) {
            BreakdownRow("Subtotal", rs(vm.subtotalCents))
            BreakdownRow("Discount", "− ${rs(vm.totalDiscountCents)}")
            if (vatRegistered) BreakdownRow("VAT (15% incl.)", rs(vm.vatCents))
            if (vm.shippingCents > 0L) BreakdownRow("Shipping", "+ ${rs(vm.shippingCents)}")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", fontSize = 13.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
                CountUp(vm.totalCents / 100.0, prefix = "Rs ", decimals = 2, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            }
        }
    }
}

@Composable
private fun PayCard(
    vm: SellingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    MachinedCard(modifier) {
        // Payment type, amount and keypad fill the panel exactly (the keypad flexes to
        // the available height), so the panel always fits a tablet without scrolling.
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp)) {
                Eyebrow("Payment type")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PAY_TYPES.forEach { (id, label) ->
                        val on = vm.pay == id
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) c.ink else c.raised)
                                .border(1.5.dp, if (on) c.ink else c.hairline, RoundedCornerShape(12.dp))
                                .clickable { vm.setPaymentType(id) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            PosIcon(PAY_ICONS.getValue(id), tint = if (on) c.surface else c.ink, size = 20.dp)
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) c.surface else c.ink)
                        }
                    }
                }
            }
            Divider(c.hairline2)
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(if (vm.isCredit) "Paid now" else "Received")
                    Text(rs(vm.receivedCents), fontFamily = JetBrainsMono, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                }
                Column(Modifier.weight(1f)) {
                    if (vm.isCredit) {
                        Eyebrow("On credit (owed)")
                        Text(
                            rs(vm.creditDueCents),
                            fontFamily = JetBrainsMono,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.crimson,
                        )
                    } else {
                        Eyebrow(if (vm.changeCents >= 0L) "Change" else "Amount due")
                        Text(
                            rs(kotlin.math.abs(vm.changeCents)),
                            fontFamily = JetBrainsMono,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (vm.changeCents >= 0L) c.emerald else c.crimson,
                        )
                    }
                }
            }
            Divider(c.hairline2)
            // keypad — flexes to fill the remaining panel height so the panel never scrolls
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf("1", "2", "3", "C"),
                    listOf("4", "5", "6", "<"),
                    listOf("7", "8", "9", "0"),
                ).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth().heightIn(min = 40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val danger = k == "C" || k == "<"
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (danger) c.raised2 else c.raised)
                                    .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
                                    .clickable { vm.pressKey(k) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (k == "<") {
                                    PosIcon(PosIcons.close, tint = c.crimson, size = 20.dp)
                                } else {
                                    Text(k, fontFamily = JetBrainsMono, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (danger) c.crimson else c.ink)
                                }
                            }
                        }
                    }
                }
            }
        }
        // quick amounts + complete — pinned at the bottom, always reachable
        Column(Modifier.fillMaxWidth().background(c.raised2).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(vm.totalCents, 50_000L, 100_000L, 200_000L).forEachIndexed { i, v ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(c.raised)
                            .border(1.dp, c.hairline, CircleShape)
                            .clickable { vm.receivedCents = v },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (i == 0) "Exact" else rs(v), fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val canComplete = vm.canComplete
            val blockedMsg =
                when {
                    vm.creditNeedsCustomer -> "Select a customer for credit"
                    !vm.isFullyTendered -> "Amount received is less than total"
                    else -> null
                }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canComplete) c.amber else c.hairline)
                    .clickable(enabled = canComplete) {
                        vm.complete()
                        onComplete()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (blockedMsg != null) {
                    PosIcon(PosIcons.bell, tint = c.muted, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(blockedMsg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
                } else {
                    PosIcon(PosIcons.check, tint = Color.White, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (vm.isCredit) "Record credit · " else "Complete sale · ",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    CountUp(
                        (if (vm.isCredit) vm.creditDueCents else vm.totalCents) / 100.0,
                        prefix = "Rs ",
                        decimals = 2,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ---- customer picker (used to attach/change the customer, mandatory for credit) ----

@Composable
private fun CustomerPickerDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (adding) {
                TextButton(
                    onClick = {
                        vm.addCustomer(name, phone, locality)
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save & select") }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (adding) adding = false else onDismiss() }) {
                Text(if (adding) "Back" else "Cancel")
            }
        },
        title = { Text(if (adding) "Add customer" else "Select customer") },
        text = {
            if (adding) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditableField("Name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "Full name")
                    EditableField("Phone", phone, { phone = it }, Modifier.fillMaxWidth(), placeholder = "+230 …", mono = true)
                    EditableField("Locality", locality, { locality = it }, Modifier.fillMaxWidth(), placeholder = "Town")
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PickerRow(label = "Walk-in customer", sub = null, selected = vm.selectedCustomer == null) {
                        vm.selectCustomer(null)
                        onDismiss()
                    }
                    vm.customers.forEach { party ->
                        PickerRow(
                            label = party.name,
                            sub = party.phone.ifBlank { null },
                            selected = vm.selectedCustomer?.id == party.id,
                        ) {
                            vm.selectCustomer(party)
                            onDismiss()
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { adding = true }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                    ) {
                        Text("+  Add new customer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.amber)
                    }
                }
            }
        },
    )
}

@Composable
private fun PickerRow(
    label: String,
    sub: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.amberSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            if (sub != null) Text(sub, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
        }
        if (selected) PosIcon(PosIcons.check, tint = c.amberPress, size = 16.dp)
    }
}

// ---- small shared bits ----

@Composable
private fun MachinedCard(
    modifier: Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
        content = content,
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = c.muted)
        Text(value, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
    }
}

@Composable
private fun Divider(color: Color = PosTheme.colors.hairline) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
private fun NumField(
    label: String,
    valueCents: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    Column(modifier) {
        Text(label, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Rs", fontSize = 13.sp, color = c.muted)
            BasicTextField(
                value = Money.toInput(valueCents),
                onValueChange = { onChange(Money.parseToCents(it) ?: 0L) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(c.amber),
            )
        }
    }
}

@Composable
private fun DiscountDialog(
    vm: SellingViewModel,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    // Snapshot for Cancel/revert.
    val snapCart = remember { vm.discountCents }
    val snapIsPct = remember { vm.discountIsPercent }
    val snapPct = remember { vm.discountPercent }
    val snapLines = remember { vm.workingLines.associate { it.product.id to it.discountCents } }
    var tab by remember { mutableStateOf(0) }
    var selectedId by remember { mutableStateOf(vm.workingLines.firstOrNull()?.product?.id) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .padding(20.dp),
        ) {
            Text("Discount options", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left: items on the ticket. Tappable (in Item mode) to pick which line to discount.
                Column(
                    Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Items", fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
                    vm.workingLines.forEach { line ->
                        val sel = tab == 1 && line.product.id == selectedId
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (sel) c.amber.copy(alpha = 0.16f) else c.raised)
                                .border(1.dp, if (sel) c.amber else c.hairline, RoundedCornerShape(10.dp))
                                .clickable(enabled = tab == 1) { selectedId = line.product.id }
                                .padding(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.product.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(rs(line.lineTotalCents), fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.ink)
                            }
                            Text("${line.qty} × ${rs(line.effectivePriceCents)}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
                            if (line.discountCents > 0L) {
                                Text("− ${rs(line.discountCents)} disc", fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.amber)
                            }
                        }
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.hairline))
                // Right: discount controls + running summary.
                Column(Modifier.weight(0.58f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TabChip("Cart discount", tab == 0, Modifier.weight(1f)) { tab = 0 }
                        TabChip("Item discount", tab == 1, Modifier.weight(1f)) { tab = 1 }
                    }
                    if (tab == 0) CartControls(vm) else ItemControls(vm, selectedId)
                    Spacer(Modifier.weight(1f))
                    Divider()
                    SummaryRow("Subtotal", rs(vm.subtotalCents))
                    SummaryRow("Total discount", "− " + rs(vm.totalDiscountCents))
                    SummaryRow("Total", rs(vm.totalCents), bold = true)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.clearAllDiscounts() }) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    vm.restoreDiscounts(snapCart, snapIsPct, snapPct, snapLines)
                    onDismiss()
                }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.ink else c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) c.surface else c.ink)
    }
}

@Composable
private fun CartControls(vm: SellingViewModel) {
    val c = PosTheme.colors
    val isPct = vm.discountIsPercent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Apply cart discount", fontSize = 12.sp, color = c.muted)
            Row(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, c.hairline, RoundedCornerShape(7.dp))) {
                ModeChip("%", isPct) { vm.applyDiscount(isPercent = true, value = vm.discountPercent.toLong()) }
                ModeChip("Rs", !isPct) { vm.applyDiscount(isPercent = false, value = vm.discountCents) }
            }
        }
        DiscountInputRow(isPct = isPct, value = if (isPct) vm.discountPercent.toLong() else vm.discountCents) { v ->
            vm.applyDiscount(isPercent = isPct, value = v)
        }
    }
}

@Composable
private fun ItemControls(
    vm: SellingViewModel,
    selectedId: String?,
) {
    val c = PosTheme.colors
    val line = vm.workingLines.firstOrNull { it.product.id == selectedId }
    if (line == null) {
        Text("Add an item to the ticket to discount it.", fontSize = 12.sp, color = c.muted)
        return
    }
    var pct by remember(selectedId) { mutableStateOf(false) }
    var pctInput by remember(selectedId) { mutableStateOf(0L) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(line.product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, c.hairline, RoundedCornerShape(7.dp))) {
                ModeChip("%", pct) { pct = true }
                ModeChip("Rs", !pct) { pct = false }
            }
        }
        DiscountInputRow(isPct = pct, value = if (pct) pctInput else line.discountCents) { v ->
            if (pct) pctInput = v
            vm.applyItemDiscount(line.product.id, isPercent = pct, value = v)
        }
    }
}

@Composable
private fun DiscountInputRow(
    isPct: Boolean,
    value: Long,
    modifier: Modifier = Modifier,
    onChange: (Long) -> Unit,
) {
    val c = PosTheme.colors
    Row(
        modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!isPct) Text("Rs", fontSize = 13.sp, color = c.muted)
        BasicTextField(
            // Percent mode is a plain 0..100 integer; Rs mode is a decimal amount → cents.
            value = if (isPct) (if (value == 0L) "" else value.toString()) else Money.toInput(value),
            onValueChange = {
                onChange(
                    if (isPct) {
                        it.filter { ch -> ch.isDigit() }.toLongOrNull() ?: 0L
                    } else {
                        Money.parseToCents(it) ?: 0L
                    },
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(c.amber),
        )
        if (isPct) Text("%", fontSize = 13.sp, color = c.muted)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) c.ink else c.muted)
        Text(value, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) c.ink else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) c.surface else c.ink)
    }
}
