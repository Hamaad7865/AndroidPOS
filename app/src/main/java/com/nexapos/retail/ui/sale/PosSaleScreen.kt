package com.nexapos.retail.ui.sale

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.CountUp
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.ProductThumb
import com.nexapos.retail.ui.components.ProductTile
import com.nexapos.retail.ui.components.ResponsiveSplit
import com.nexapos.retail.ui.components.isPortrait
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val numFmt = NumberFormat.getNumberInstance(Locale.US)

private fun rs(n: Int) = "Rs " + numFmt.format(n)

private data class FlyChip(val id: Long, val start: Offset, val target: Offset)

@Composable
fun PosSaleScreen(
    vm: SellingViewModel,
    onCharge: () -> Unit,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    var cat by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val lines = vm.workingLines

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var cartCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chips = remember { mutableStateListOf<FlyChip>() }
    var chipSeq by remember { mutableStateOf(0L) }

    val visible =
        vm.products.filter {
            (cat == "All" || it.cat == cat) &&
                (query.isBlank() || (it.name + " " + it.sku).contains(query, ignoreCase = true))
        }

    fun add(
        p: PosProduct,
        cardCoords: LayoutCoordinates?,
    ) {
        vm.addToCart(p)
        val root = rootCoords ?: return
        val cart = cartCoords ?: return
        val card = cardCoords ?: return
        if (!card.isAttached) return
        val start = root.localPositionOf(card, Offset(card.size.width / 2f, 18f))
        val target = root.localPositionOf(cart, Offset(cart.size.width / 2f, 36f))
        chips.add(FlyChip(chipSeq++, start, target))
    }

    val subtotal = vm.subtotal
    val vat = vm.vat
    val total = vm.total

    NavShell(
        active = "pos",
        onNav = onNav,
        outerModifier = Modifier.onGloballyPositioned { rootCoords = it },
        overlay = {
            chips.forEach { chip ->
                key(chip.id) {
                    FlyingChip(chip) { chips.removeAll { it.id == chip.id } }
                }
            }
        },
    ) {
        val portrait = isPortrait()
        ResponsiveSplit(
            portrait = portrait,
            primary = { primaryMod ->
                // Center / product area
                Column(primaryMod) {
                    AppBar(
                        title = "POS Sale",
                        subtitle = "Counter 01 · Shift opened 08:32",
                        right = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HoldMenuButton(vm)
                                SmallBtn(PosIcons.receipt, "Sales") { onNav("sales-list") }
                            }
                        },
                    )

                    // Customer + search row
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        CustomerCard(vm)
                        SearchField(query, { query = it }, Modifier.weight(1f))
                        SmallBtn(PosIcons.barcode, "Scan") {
                            com.nexapos.retail.data.barcode.BarcodeScanner.scan(ctx) { code ->
                                if (code.isNullOrBlank()) return@scan
                                val hit = vm.addByBarcode(code)
                                if (!hit) {
                                    android.widget.Toast.makeText(
                                        ctx,
                                        "No product matches $code — add it first or search by name.",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                    query = code
                                }
                            }
                        }
                        SmallBtn(PosIcons.plus, "New") { vm.startNewTicket() }
                    }

                    // Category chips
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        vm.categories.forEach { cc ->
                            val count = if (cc == "All") vm.products.size else vm.products.count { it.cat == cc }
                            CategoryChip(cc, cat == cc, count) { cat = cc }
                        }
                    }

                    // Product grid (4 columns, staggered reveal)
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        visible.chunked(4).forEachIndexed { rowIdx, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEachIndexed { colIdx, p ->
                                    ProductCard(
                                        p = p,
                                        index = rowIdx * 4 + colIdx,
                                        onAdd = { coords -> add(p, coords) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            },
            secondary = { secondaryMod ->
                // Ticket panel — side in landscape, bottom in portrait
                Column(
                    secondaryMod
                        .background(c.surface)
                        .border(width = 1.dp, color = c.hairline)
                        .onGloballyPositioned { cartCoords = it },
                ) {
                    // header
                    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Eyebrow("Current Ticket")
                                Text(
                                    vm.nextInvoiceNo,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = c.ink,
                                )
                            }
                            Badge("● Open", c.amberSoft, c.amberPress)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

                    // lines
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        lines.forEach { line ->
                            TicketLine(
                                line = line,
                                onDec = { vm.decrementLine(line.product.id) },
                                onInc = { vm.incrementLine(line.product.id) },
                                onRm = { vm.removeLine(line.product.id) },
                            )
                        }
                    }

                    // totals
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                    Column(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 18.dp, vertical = 14.dp)) {
                        TotalRow("Subtotal", rs(subtotal), false)
                        TotalRow("Discount", "— Rs 0", true)
                        TotalRow("VAT (15%, incl.)", rs(vat), true)
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "TOTAL",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.06.em,
                                color = c.muted,
                            )
                            CountUp(
                                total.toDouble(),
                                prefix = "Rs ",
                                decimals = 0,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = c.ink,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton("Clear", Modifier.weight(1f)) { vm.clearCart() }
                            SecondaryButton(
                                label = if (lines.isEmpty()) "Hold" else "Hold ticket",
                                modifier = Modifier.weight(1f),
                            ) { vm.holdCurrentTicket() }
                        }
                        Spacer(Modifier.height(8.dp))
                        ChargeButton(total, enabled = lines.isNotEmpty(), onClick = onCharge)
                    }
                }
            },
        )
    }
}

@Composable
private fun FlyingChip(
    chip: FlyChip,
    onDone: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(chip.id) {
        progress.animateTo(1f, tween(650, easing = LinearEasing))
        onDone()
    }
    val t = progress.value
    val pos = lerp(chip.start, chip.target, t)
    val scale = 1f - 0.5f * t
    val alpha = if (t < 0.7f) 1f else (1f - (t - 0.7f) / 0.3f)
    Box(
        Modifier
            .offset { IntOffset(pos.x.roundToInt() - 18, pos.y.roundToInt() - 18) }
            .size(36.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(PosTheme.colors.amber),
        contentAlignment = Alignment.Center,
    ) {
        Text("+1", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun ProductCard(
    p: PosProduct,
    index: Int,
    onAdd: (LayoutCoordinates?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val pop = remember { Animatable(1f) }
    var tapKey by remember { mutableStateOf(0) }
    LaunchedEffect(tapKey) {
        if (tapKey > 0) {
            pop.animateTo(1.05f, tween(120))
            pop.animateTo(1f, tween(130))
        }
    }
    // staggered reveal on first appearance
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 35L)
        reveal.animateTo(1f, tween(450))
    }
    val low = p.stock <= 6
    Column(
        modifier =
            modifier
                .onGloballyPositioned { coords = it }
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 8f
                }
                .clip(RoundedCornerShape(14.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                .clickable {
                    tapKey++
                    onAdd(coords)
                }
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.raised2)
                .border(1.dp, c.hairline2, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ProductThumb(imagePath = p.imagePath, kind = p.kind, size = 88.dp)
            if (low) {
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Badge("● ${p.stock} left", c.lowSoft, c.low)
                }
            }
        }
        Text(
            p.name,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = c.ink,
            modifier = Modifier.heightIn(min = 34.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(rs(p.price), fontFamily = JetBrainsMono, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(p.sku, fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = c.muted)
        }
    }
}

@Composable
private fun TicketLine(
    line: PosLine,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onRm: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.raised2)
                .border(1.dp, c.hairline2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { ProductTile(kind = line.product.kind, size = 40.dp) }
        Column(Modifier.weight(1f)) {
            Text(
                line.product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = c.ink,
            )
            Text(
                "${line.product.sku} · ${rs(line.product.price)}",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = c.muted,
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBtn(PosIcons.minus, onDec)
            Text(
                "${line.qty}",
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                modifier = Modifier.width(30.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepBtn(PosIcons.plus, onInc)
        }
        Text(
            rs(line.lineTotal),
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = c.ink,
            modifier = Modifier.width(78.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Box(Modifier.size(24.dp).clip(CircleShape).clickable { onRm() }, contentAlignment = Alignment.Center) {
            PosIcon(PosIcons.close, tint = c.muted, size = 15.dp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun StepBtn(
    paths: List<String>,
    onClick: () -> Unit,
) {
    Box(Modifier.size(width = 30.dp, height = 32.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        PosIcon(paths, tint = PosTheme.colors.ink, size = 14.dp)
    }
}

// ---------------------------------------------------------------------------
// Small reusable bits
// ---------------------------------------------------------------------------

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        letterSpacing = 0.14.em,
        fontWeight = FontWeight.SemiBold,
        color = PosTheme.colors.muted,
    )
}

@Composable
private fun Badge(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
) {
    Box(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg) }
}

private fun initialsOf(name: String): String =
    name.split(' ', '·', '-')
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "WI" }

@Composable
private fun CustomerCard(vm: SellingViewModel) {
    val c = PosTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddCustomerDialog(
            onDismiss = { showAdd = false },
            onSave = { n, ph, loc ->
                vm.addCustomer(n, ph, loc)
                showAdd = false
            },
        )
    }

    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
                .clickable { menuOpen = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .width(220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.amberSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(vm.customerName), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.amberPress)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "CUSTOMER",
                    fontSize = 10.sp,
                    letterSpacing = 0.06.em,
                    fontWeight = FontWeight.SemiBold,
                    color = c.muted,
                )
                Text(
                    vm.customerName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PosIcon(PosIcons.chevD, tint = c.ink, size = 14.dp)
        }

        androidx.compose.material3.DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Walk-in customer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                onClick = {
                    vm.selectCustomer(null)
                    menuOpen = false
                },
            )
            if (vm.customers.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider()
                vm.customers.forEach { party ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Column {
                                Text(party.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (party.phone.isNotBlank()) {
                                    Text(party.phone, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
                                }
                            }
                        },
                        onClick = {
                            vm.selectCustomer(party)
                            menuOpen = false
                        },
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider()
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("+  Add new customer…", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.amberPress) },
                onClick = {
                    menuOpen = false
                    showAdd = true
                },
            )
        }
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSave(name, phone, locality) },
                enabled = name.isNotBlank(),
            ) { Text("Save & select") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.nexapos.retail.ui.components.EditableField("Name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "Full name")
                com.nexapos.retail.ui.components.EditableField("Phone", phone, { phone = it }, Modifier.fillMaxWidth(), placeholder = "+230 …", mono = true)
                com.nexapos.retail.ui.components.EditableField("Locality", locality, { locality = it }, Modifier.fillMaxWidth(), placeholder = "Town")
            }
        },
    )
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosIcon(PosIcons.search, tint = c.ink, size = 16.dp)
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle =
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = HankenGrotesk,
                        fontSize = 14.sp,
                        color = c.ink,
                    ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.amber),
            )
            if (value.isEmpty()) Text("Search product or SKU…", fontSize = 14.sp, color = c.muted)
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    active: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (active) c.ink else c.raised)
            .border(1.dp, if (active) c.ink else c.hairline, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) c.surface else c.ink)
        Text(
            "$count",
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            color = (if (active) c.surface else c.ink).copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun HoldMenuButton(vm: SellingViewModel) {
    val c = PosTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    val cartHasLines = vm.workingLines.isNotEmpty()
    val held = vm.heldTickets
    val label = if (held.isEmpty()) "Hold" else "Hold (${held.size})"
    Box {
        Row(
            Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
                .clickable {
                    // No held tickets and no current cart → nothing to do, but still open
                    // the menu so the cashier sees the "Hold current" disabled state.
                    menuOpen = true
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosIcon(PosIcons.refresh, tint = c.ink, size = 14.dp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            if (held.isNotEmpty()) PosIcon(PosIcons.chevD, tint = c.muted, size = 12.dp)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        if (cartHasLines) "Hold current ticket" else "Hold current ticket (cart empty)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (cartHasLines) c.amberPress else c.muted,
                    )
                },
                enabled = cartHasLines,
                onClick = {
                    vm.holdCurrentTicket()
                    menuOpen = false
                },
            )
            if (held.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider()
                held.forEach { ticket ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    "Resume — ${ticket.label}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c.ink,
                                )
                                Text(
                                    "${ticket.itemCount} item${if (ticket.itemCount == 1) "" else "s"} · ${rs(ticket.total)}",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = c.muted,
                                )
                            }
                        },
                        onClick = {
                            vm.resumeHeldTicket(ticket.id)
                            menuOpen = false
                        },
                        trailingIcon = {
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).clickable {
                                    vm.discardHeldTicket(ticket.id)
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                PosIcon(PosIcons.close, tint = c.muted, size = 14.dp)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallBtn(
    paths: List<String>,
    label: String,
    onClick: () -> Unit = {},
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosIcon(paths, tint = c.ink, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink) }
}

@Composable
private fun ChargeButton(
    total: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) c.amber else c.hairline)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Charge ",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = androidx.compose.ui.graphics.Color.White,
        )
        CountUp(
            total.toDouble(),
            prefix = "Rs ",
            decimals = 0,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Spacer(Modifier.width(8.dp))
        PosIcon(PosIcons.arrowR, tint = androidx.compose.ui.graphics.Color.White, size = 20.dp)
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    muted: Boolean,
) {
    val c = PosTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = if (muted) c.muted else c.graphite)
        Text(
            value,
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (muted) c.muted else c.ink,
        )
    }
}
