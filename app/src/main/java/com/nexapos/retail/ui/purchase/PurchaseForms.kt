@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.nexapos.retail.ui.purchase

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.FormSection
import com.nexapos.retail.ui.components.GhostBtn
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PickerField
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.SumRow
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Composable
fun AddPurchaseScreen(
    vm: PurchasesViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
    initialSupplierId: Long? = null,
) {
    val c = PosTheme.colors

    var supplierName by remember { mutableStateOf("") }
    var supplierContact by remember { mutableStateOf("") }
    var supplierLocality by remember { mutableStateOf("") }
    var expectedDelivery by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Bank transfer") }
    var notes by remember { mutableStateOf("") }
    var statusLabel by remember { mutableStateOf("Received") }
    var showAddItem by remember { mutableStateOf(false) }

    val items = remember { mutableStateListOf<PurchaseDraftItem>() }

    // When the user picks an existing supplier, auto-fill contact + locality.
    // Debounced: on every keystroke supplierName changes, but we only look up
    // once the value has been stable for 300 ms, preventing a coroutine per character.
    LaunchedEffect(Unit) {
        snapshotFlow { supplierName }
            .debounce(300L)
            .collectLatest { name ->
                val existing = vm.findSupplier(name)
                if (existing != null) {
                    supplierContact = existing.phone
                    supplierLocality = existing.locality
                }
            }
    }

    // Prefill from a supplier tapped in Parties → Suppliers → "New purchase".
    // We wait for the supplier list to load (it streams in async), fill the form
    // once, and guard with [prefilled] so a later list refresh can't clobber the
    // cashier's edits.
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(initialSupplierId, vm.suppliers) {
        if (!prefilled && initialSupplierId != null) {
            val s = vm.suppliers.firstOrNull { it.id == initialSupplierId }
            if (s != null) {
                supplierName = s.name
                supplierContact = s.phone
                supplierLocality = s.locality
                prefilled = true
            }
        }
    }

    val subtotal = items.sumOf { it.quantity * it.unitCostRupees }
    val canConfirm = supplierName.isNotBlank() && items.isNotEmpty()

    val ctx = androidx.compose.ui.platform.LocalContext.current

    fun confirm() {
        if (!canConfirm) return
        val draftItems = items.toList()
        val knownProductNames = vm.productNames.map { it.trim().lowercase() }.toSet()
        val newProductCount =
            draftItems.count { it.name.trim().lowercase() !in knownProductNames }
        vm.recordPurchase(
            supplierName = supplierName,
            paymentMethod = paymentMethod.lowercase().replace(' ', '_'),
            items = draftItems,
            status = statusLabel.lowercase(),
            expectedDelivery = expectedDelivery,
            notes = notes,
        )
        if (newProductCount > 0) {
            android.widget.Toast.makeText(
                ctx,
                "PO confirmed. $newProductCount new product${if (newProductCount == 1) "" else "s"} added to catalog — set sale prices in Products.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
        onBack()
    }

    if (showAddItem) {
        AddItemDialog(
            vm = vm,
            onDismiss = { showAddItem = false },
            onAdd = { draft ->
                items.add(draft)
                showAddItem = false
            },
        )
    }

    NavShell(active = "purchase", onNav = onNav) {
        AppBar(
            title = "New Purchase Order",
            subtitle = if (items.isEmpty()) "Pick a supplier, add items, confirm" else "${items.size} item${if (items.size == 1) "" else "s"} · ${rsStr(subtotal)}",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostBtn("Cancel", onBack)
                    PrimaryBtn(
                        PosIcons.check,
                        if (canConfirm) "Confirm PO" else "Fill supplier + items",
                    ) { if (canConfirm) confirm() }
                }
            },
        )
        Row(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FormSection("Supplier") {
                    PickerField(
                        label = "Supplier name *",
                        value = supplierName,
                        options = vm.suppliers.map { it.name },
                        onValueChange = { picked ->
                            supplierName = picked
                            // Picking from the list re-runs the LaunchedEffect above
                            // which back-fills contact + locality. For free-text
                            // (a new supplier) we clear the autofilled fields so the
                            // cashier can type fresh values.
                            if (vm.findSupplier(picked) == null) {
                                supplierContact = ""
                                supplierLocality = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Pick an existing supplier or type a new one",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(
                            "Contact",
                            supplierContact,
                            { supplierContact = it },
                            Modifier.weight(1f),
                            mono = true,
                            placeholder = "+230 …",
                        )
                        EditableField(
                            "Locality / address",
                            supplierLocality,
                            { supplierLocality = it },
                            Modifier.weight(1f),
                            placeholder = "Port-Louis",
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (vm.findSupplier(supplierName) != null) {
                            "Existing supplier — contact details auto-filled. Edits stay on this purchase only."
                        } else if (supplierName.isNotBlank()) {
                            "New supplier — they'll be added to Parties → Suppliers when you confirm."
                        } else {
                            "Tap the field to pick from your suppliers, or type a new name."
                        },
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
                FormSection("Items") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, c.hairline, RoundedCornerShape(10.dp))) {
                        Row(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ItemTh("PRODUCT", Modifier.weight(2f))
                            ItemTh("QTY", Modifier.width(60.dp), TextAlign.End)
                            ItemTh("COST/UNIT", Modifier.width(90.dp), TextAlign.End)
                            ItemTh("SUBTOTAL", Modifier.width(100.dp), TextAlign.End)
                            Spacer(Modifier.width(24.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                        if (items.isEmpty()) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "No items yet — tap Add item below.",
                                    fontSize = 12.sp,
                                    color = c.muted,
                                )
                            }
                        } else {
                            items.forEachIndexed { i, item ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(item.name, Modifier.weight(2f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                                    Text("${item.quantity}", Modifier.width(60.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End)
                                    Text(rsStr(item.unitCostRupees), Modifier.width(90.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.ink, textAlign = TextAlign.End)
                                    Text(rsStr(item.quantity * item.unitCostRupees), Modifier.width(100.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.End)
                                    Box(
                                        Modifier.size(24.dp).clip(CircleShape).clickable { items.removeAt(i) },
                                        contentAlignment = Alignment.Center,
                                    ) { PosIcon(PosIcons.trash, tint = c.muted, size = 14.dp) }
                                }
                                if (i < items.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SecBtn(PosIcons.plus, "Add item") { showAddItem = true }
                }
                FormSection("Notes & delivery") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PickerField(
                            label = "Status",
                            value = statusLabel,
                            options = listOf("Received", "Pending"),
                            onValueChange = { statusLabel = it },
                            modifier = Modifier.weight(1f),
                            allowFreeText = false,
                        )
                        PickerField(
                            label = "Payment method",
                            value = paymentMethod,
                            options = listOf("Cash", "Bank transfer", "Cheque", "Credit (30 days)", "Mobile"),
                            onValueChange = { paymentMethod = it },
                            modifier = Modifier.weight(1f),
                            allowFreeText = false,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (statusLabel.equals("Received", ignoreCase = true)) {
                            "Stock for every catalog item on this PO goes up the moment you confirm."
                        } else {
                            "PO is recorded but no stock changes yet. Mark it Received later from the detail page when the goods arrive."
                        },
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    EditableField(
                        "Expected delivery",
                        expectedDelivery,
                        { expectedDelivery = it },
                        Modifier.fillMaxWidth(),
                        placeholder = "Tomorrow, 02 Jun, …",
                    )
                    Spacer(Modifier.height(12.dp))
                    EditableField(
                        "Internal notes",
                        notes,
                        { notes = it },
                        Modifier.fillMaxWidth(),
                        placeholder = "e.g. urgent restock for weekend",
                        tall = true,
                    )
                }
            }
            // summary panel
            Column(
                Modifier
                    .width(360.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.raised)
                    .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                    .padding(18.dp),
            ) {
                Eyebrow("Order summary")
                Spacer(Modifier.height(6.dp))
                Text(
                    if (canConfirm) "Ready to confirm" else "Draft",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    color = if (canConfirm) c.emerald else c.muted,
                )
                Spacer(Modifier.height(14.dp))
                SumRow("Items", "${items.size} line${if (items.size == 1) "" else "s"}")
                Spacer(Modifier.height(6.dp))
                SumRow("Units", "${items.sumOf { it.quantity }}")
                Spacer(Modifier.height(6.dp))
                SumRow("Subtotal", rsStr(subtotal), mono = true)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontSize = 13.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
                    Text(
                        rsStr(subtotal),
                        fontFamily = JetBrainsMono,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.ink,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Confirming the PO raises the stock count for each line item that matches a product in your catalog. New names are recorded for reference but don't change stock.",
                    fontSize = 11.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(10.dp))
                WideBtn(
                    "Confirm purchase",
                    primary = canConfirm,
                    Modifier.fillMaxWidth(),
                    icon = PosIcons.check,
                    onClick = { if (canConfirm) confirm() },
                )
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    vm: PurchasesViewModel,
    onDismiss: () -> Unit,
    onAdd: (PurchaseDraftItem) -> Unit,
) {
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unitCost by remember { mutableStateOf("") }
    val c = PosTheme.colors

    // Auto-suggest cost when the cashier picks an existing product.
    // Debounced so every keystroke in the free-text field doesn't spawn a coroutine.
    LaunchedEffect(Unit) {
        snapshotFlow { product }
            .debounce(300L)
            .collectLatest { name ->
                val suggested = vm.suggestedCost(name)
                if (suggested > 0 && unitCost.isBlank()) unitCost = suggested.toString()
            }
    }

    val qtyN = qty.filter { it.isDigit() }.toIntOrNull() ?: 0
    val costN = unitCost.filter { it.isDigit() }.toIntOrNull() ?: 0
    val canSave = product.isNotBlank() && qtyN > 0 && costN > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onAdd(PurchaseDraftItem(product.trim(), qtyN, costN)) },
                enabled = canSave,
            ) { Text("Add to PO") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    label = "Product *",
                    value = product,
                    options = vm.productNames,
                    onValueChange = { product = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Type or pick a product",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditableField(
                        "Quantity *",
                        qty,
                        { qty = it },
                        Modifier.weight(1f),
                        mono = true,
                        number = true,
                        placeholder = "0",
                    )
                    EditableField(
                        "Cost / unit *",
                        unitCost,
                        { unitCost = it },
                        Modifier.weight(1f),
                        mono = true,
                        number = true,
                        right = "Rs",
                        placeholder = "0",
                    )
                }
                if (qtyN > 0 && costN > 0) {
                    Text(
                        "Line total: ${rsStr(qtyN * costN)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.emerald,
                    )
                }
                Text(
                    "Pick an existing product to raise its stock and prefill the catalog cost. Typing a brand-new name records it on the PO for reference only.",
                    fontSize = 11.sp,
                    color = c.muted,
                )
            }
        },
    )
}

@Composable
private fun ItemTh(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(text, modifier = modifier, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted, textAlign = align)
}

// ---------------------------------------------------------------------------
// Purchase detail
// ---------------------------------------------------------------------------

@Composable
fun PurchaseDetailScreen(
    vm: PurchaseDetailViewModel,
    purchaseId: Long?,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors

    LaunchedEffect(purchaseId) {
        if (purchaseId != null) vm.load(purchaseId)
    }

    val purchase = vm.purchase
    val items = vm.items

    NavShell(active = "purchase", onNav = onNav) {
        AppBar(
            title = purchase?.code ?: "Purchase",
            subtitle =
                when {
                    purchaseId == null -> "No purchase selected"
                    vm.loading -> "Loading…"
                    vm.notFound -> "Not found"
                    purchase != null -> "${purchase.itemCount} items · ${rsStr((purchase.totalCents / CENTS_PER_RUPEE).toInt())}"
                    else -> ""
                },
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(null, "Back", onBack)
                }
            },
        )
        if (purchaseId == null || vm.notFound) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PosIcon(PosIcons.truck, tint = c.muted, size = 32.dp)
                Spacer(Modifier.height(10.dp))
                Text("Purchase not found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The purchase may have been deleted, or the link is broken. Open it again from the Purchases list.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            }
            return@NavShell
        }
        if (purchase == null) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Loading purchase…", fontSize = 13.sp, color = c.muted)
            }
            return@NavShell
        }

        var showStatusDialog by remember { mutableStateOf(false) }
        if (showStatusDialog) {
            ChangeStatusDialog(
                current = purchase.status,
                onDismiss = { showStatusDialog = false },
                onPick = { picked ->
                    vm.changeStatus(picked)
                    showStatusDialog = false
                },
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailHeaderCard(purchase, onChangeStatus = { showStatusDialog = true })
            DetailItemsCard(items)
            DetailTotalsCard(purchase, items)
        }
    }
}

@Composable
private fun ChangeStatusDialog(
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val c = PosTheme.colors
    var picked by remember { mutableStateOf(current.lowercase()) }
    val options = listOf("received", "pending", "partial", "cancelled")
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onPick(picked) },
                enabled = !picked.equals(current, ignoreCase = true),
            ) { Text("Update status") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Change PO status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Currently: ${current.uppercase()}. Pick the new status — stock is adjusted automatically when crossing into or out of RECEIVED.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(4.dp))
                options.forEach { opt ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { picked = opt }.padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (picked == opt) c.amber else c.hairline, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (picked == opt) Box(Modifier.size(10.dp).clip(CircleShape).background(c.amber))
                        }
                        Column {
                            Text(opt.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(
                                statusBlurb(opt),
                                fontSize = 11.sp,
                                color = c.muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                val effect =
                    when {
                        current.equals("received", ignoreCase = true) && picked != "received" ->
                            "⚠ Stock will be REVERSED for every catalog line on this PO."
                        !current.equals("received", ignoreCase = true) && picked == "received" ->
                            "✓ Stock will be ADDED for every catalog line on this PO."
                        else -> "Stock won't change with this transition."
                    }
                Text(effect, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            }
        },
    )
}

private fun statusBlurb(status: String): String =
    when (status) {
        "received" -> "Goods arrived, stock raised."
        "pending" -> "Order placed, awaiting delivery."
        "partial" -> "Some lines received — informational; tracks per-line manually."
        "cancelled" -> "Order voided. Stock will be reversed if it was received."
        else -> ""
    }

@Composable
private fun DetailHeaderCard(
    purchase: com.nexapos.retail.data.entity.Purchase,
    onChangeStatus: () -> Unit,
) {
    val c = PosTheme.colors
    val dayFmt = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.US)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(c.amberSoft),
                contentAlignment = Alignment.Center,
            ) { PosIcon(PosIcons.truck, tint = c.amberPress, size = 22.dp) }
            Column(Modifier.weight(1f)) {
                Eyebrow("Purchase order")
                Spacer(Modifier.height(4.dp))
                Text(purchase.code, fontFamily = JetBrainsMono, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            }
            StatusPill(purchase.status)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Spacer(Modifier.height(16.dp))
        InfoRow("Supplier", purchase.supplierName)
        InfoRow("Created", dayFmt.format(java.util.Date(purchase.createdAt)))
        InfoRow("Payment", purchase.paymentMethod.replace('_', ' ').replaceFirstChar { it.uppercase() })
        InfoRow("Item units", "${purchase.itemCount}")
        if (purchase.expectedDelivery.isNotBlank()) InfoRow("Expected delivery", purchase.expectedDelivery)
        if (purchase.notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Eyebrow("Notes")
            Spacer(Modifier.height(4.dp))
            Text(purchase.notes, fontSize = 13.sp, color = c.ink)
        }
        Spacer(Modifier.height(16.dp))
        // Quick-action shortcuts for the most common transitions, plus a
        // general "Change status…" that opens the full picker.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!purchase.status.equals("received", ignoreCase = true) &&
                !purchase.status.equals("cancelled", ignoreCase = true)
            ) {
                SecBtn(PosIcons.check, "Mark received") { onChangeStatus() }
            }
            if (purchase.status.equals("pending", ignoreCase = true) ||
                purchase.status.equals("received", ignoreCase = true)
            ) {
                SecBtn(PosIcons.close, "Cancel PO") { onChangeStatus() }
            }
            SecBtn(PosIcons.refresh, "Change status…") { onChangeStatus() }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = c.muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun StatusPill(status: String) {
    val c = PosTheme.colors
    val (bg, fg) =
        when (status.lowercase()) {
            "received" -> c.emeraldSoft to c.emerald
            "partial" -> c.amberSoft to c.amberPress
            "cancelled" -> c.lowSoft to c.low
            else -> c.hairline2 to c.muted
        }
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            status.uppercase(),
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
private fun DetailItemsCard(items: List<com.nexapos.retail.data.entity.PurchaseItem>) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Eyebrow("Items received")
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text("No line items on this purchase.", fontSize = 13.sp, color = c.muted)
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemTh("PRODUCT", Modifier.weight(2f))
                ItemTh("QTY", Modifier.width(60.dp), TextAlign.End)
                ItemTh("COST/UNIT", Modifier.width(90.dp), TextAlign.End)
                ItemTh("SUBTOTAL", Modifier.width(110.dp), TextAlign.End)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            items.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(2f)) {
                        Text(
                            item.nameSnapshot,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.ink,
                        )
                        if (item.productId == null) {
                            Text(
                                "Not in current catalog",
                                fontSize = 11.sp,
                                color = c.muted,
                            )
                        }
                    }
                    Text(
                        "${item.quantity}",
                        Modifier.width(60.dp),
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        rsStr((item.unitCostCents / CENTS_PER_RUPEE).toInt()),
                        Modifier.width(90.dp),
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = c.graphite,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        rsStr((item.lineTotalCents / CENTS_PER_RUPEE).toInt()),
                        Modifier.width(110.dp),
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.ink,
                        textAlign = TextAlign.End,
                    )
                }
                if (i < items.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
            }
        }
    }
}

@Composable
private fun DetailTotalsCard(
    purchase: com.nexapos.retail.data.entity.Purchase,
    items: List<com.nexapos.retail.data.entity.PurchaseItem>,
) {
    val c = PosTheme.colors
    val subtotal = (items.sumOf { it.lineTotalCents } / CENTS_PER_RUPEE).toInt()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Eyebrow("Totals")
        Spacer(Modifier.height(12.dp))
        SumRow("Line items", "${items.size}")
        Spacer(Modifier.height(6.dp))
        SumRow("Subtotal", rsStr(subtotal), mono = true)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("TOTAL", fontSize = 13.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = c.muted)
            Text(
                rsStr((purchase.totalCents / CENTS_PER_RUPEE).toInt()),
                fontFamily = JetBrainsMono,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.ink,
            )
        }
    }
}

private const val CENTS_PER_RUPEE = 100L
