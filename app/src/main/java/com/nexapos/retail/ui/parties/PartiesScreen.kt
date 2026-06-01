package com.nexapos.retail.ui.parties

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.ui.checkout.ReceiptOutput
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.BadgeKind
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.ResponsiveSplit
import com.nexapos.retail.ui.components.SearchField
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.StatusBadge
import com.nexapos.retail.ui.components.TabBar
import com.nexapos.retail.ui.components.isPortrait
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.sale.SaleDetailsDialog
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.data.entity.Party as PartyEntity
import com.nexapos.retail.data.entity.Sale as SaleEntity

private fun initials(name: String) =
    name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }

/**
 * Builds a plain-text account statement for [party], suitable for the share sheet
 * (WhatsApp, email, SMS, print-to-PDF). Customers get their lifetime spend and
 * recent receipts; suppliers get the balance we owe them.
 */
private fun buildPartyStatement(
    context: android.content.Context,
    party: PartyEntity,
    isCustomer: Boolean,
    lifetimeRupees: Int,
    balanceRupees: Int,
    recentSales: List<SaleEntity>,
): String {
    val biz = BusinessProfile.name(context)
    val now = java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", java.util.Locale.US).format(java.util.Date())
    val sb = StringBuilder()
    sb.append("$biz — Account statement\n")
    sb.append(party.name).append('\n')
    val contact = listOf(party.phone, party.locality).filter { it.isNotBlank() }.joinToString(" · ")
    if (contact.isNotBlank()) sb.append(contact).append('\n')
    sb.append("Type: ").append(if (isCustomer) "Customer" else "Supplier").append("\n\n")
    val balanceLabel = if (isCustomer) "Outstanding (owed to us)" else "Outstanding (we owe)"
    sb.append(balanceLabel).append(": ").append(if (balanceRupees > 0) rsStr(balanceRupees) else "Rs 0").append('\n')
    if (isCustomer) {
        val saleWord = if (recentSales.size == 1) "sale" else "sales"
        val lifetimeStr = if (lifetimeRupees > 0) rsStr(lifetimeRupees) else "Rs 0"
        sb.append("Lifetime purchases: ").append(lifetimeStr)
            .append(" across ").append(recentSales.size).append(' ').append(saleWord).append('\n')
        if (recentSales.isNotEmpty()) {
            sb.append("\nRecent activity:\n")
            val dayFmt = java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale.US)
            recentSales.take(MAX_RECENT_ROWS).forEach { sale ->
                sb.append("  ").append(sale.receiptNo).append("  ")
                    .append(dayFmt.format(java.util.Date(sale.createdAt))).append("  ")
                    .append(sale.paymentMethod).append("  ")
                    .append(rsStr((sale.totalCents / 100).toInt())).append('\n')
            }
        }
    }
    sb.append("\nGenerated ").append(now)
    return sb.toString()
}

@Composable
fun PartiesScreen(
    vm: PartiesViewModel,
    onNav: (String) -> Unit,
    onNewSale: () -> Unit,
    onNewPurchase: (Long) -> Unit,
) {
    val c = PosTheme.colors
    var tab by remember { mutableStateOf("customers") }
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val source = if (tab == "customers") vm.customers else vm.suppliers
    val filtered =
        remember(source, query) {
            source.filter { party -> query.isBlank() || party.name.contains(query, true) || party.phone.contains(query) }
        }
    val selected =
        remember(source, selectedId) {
            source.firstOrNull { party -> party.id == selectedId }
        }

    // Switch tabs → clear the right-panel selection so we don't keep stale data
    // from another tab visible.
    androidx.compose.runtime.LaunchedEffect(tab) {
        selectedId = null
        vm.selectParty(null)
    }

    SaleDetailsDialog(
        sale = vm.detailSale,
        items = vm.detailItems,
        onDismiss = { vm.closeSaleDetails() },
    )

    if (showAdd) {
        AddPartyDialog(
            isSupplier = tab == "suppliers",
            onDismiss = { showAdd = false },
            onSave = { partyName, phone, locality, supplier ->
                vm.addParty(partyName, phone, locality, supplier)
                showAdd = false
            },
        )
    }

    NavShell(active = "parties", onNav = onNav) {
        AppBar(
            title = "Parties",
            subtitle = "${vm.customerCount} customers · ${vm.supplierCount} suppliers · ${rsStr(vm.customerDue)} due",
            right = { PrimaryBtn(PosIcons.plus, "Add party") { showAdd = true } },
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TabBar(
                listOf(
                    "customers" to "Customers · ${vm.customerCount}",
                    "suppliers" to "Suppliers · ${vm.supplierCount}",
                ),
                tab,
            ) { tab = it }
            SearchField(query, { query = it }, "Search name, phone…", Modifier.width(340.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(22.dp)) {
            val portrait = isPortrait()
            ResponsiveSplit(
                portrait = portrait,
                secondaryWidthDp = 360,
                primary = { mod ->
                    // list
                    Column(
                        mod
                            .padding(end = if (portrait) 0.dp else 7.dp, bottom = if (portrait) 7.dp else 0.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.raised)
                            .border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Spacer(Modifier.width(36.dp))
                            Th(if (tab == "suppliers") "SUPPLIER" else "CUSTOMER", Modifier.weight(1f))
                            Th("BALANCE", Modifier.width(96.dp), TextAlign.End)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                        if (filtered.isEmpty()) {
                            EmptyPartiesState(tab == "suppliers", Modifier.weight(1f).fillMaxWidth())
                        } else {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                filtered.forEach { p ->
                                    PartyRow(
                                        p = p,
                                        selected = p.id == selectedId,
                                        onClick = {
                                            selectedId = p.id
                                            vm.selectParty(p)
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                secondary = { mod ->
                    DetailPanel(
                        selected = selected,
                        lifetimeRupees = vm.selectedLifetimeRupees,
                        recentSales = vm.selectedRecentSales,
                        onSaleClick = { sale -> vm.openSaleDetails(sale) },
                        modifier = mod.padding(start = if (portrait) 0.dp else 7.dp, top = if (portrait) 7.dp else 0.dp),
                        onNewSale = onNewSale,
                        onNewPurchase = onNewPurchase,
                    )
                },
            )
        }
    }
}

@Composable
private fun Th(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(text, modifier = modifier, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted, textAlign = align)
}

@Composable
private fun PartyRow(
    p: PartyEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    val balanceRupees = (p.balanceCents / 100).toInt()
    // Phone + locality become a compact subtitle so the row fits any tablet width.
    val sub = listOf(p.phone, p.locality).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "No contact" }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) c.amberTint else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (selected) c.amberSoft else c.raised2).border(1.dp, c.hairline2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(p.name), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) c.amberPress else c.graphite)
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontFamily = JetBrainsMono, fontSize = 11.5.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
            if (balanceRupees > 0) StatusBadge(rsStr(balanceRupees), BadgeKind.DUE) else Text("Rs 0", fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.muted)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun EmptyPartiesState(
    isSuppliers: Boolean,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    Column(
        modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PosIcon(PosIcons.people, tint = c.muted, size = 32.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            if (isSuppliers) "No suppliers yet" else "No customers yet",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = c.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap Add party (top right) to create your first one.",
            fontSize = 12.sp,
            color = c.muted,
        )
    }
}

@Composable
private fun DetailPanel(
    selected: PartyEntity?,
    lifetimeRupees: Int,
    recentSales: List<com.nexapos.retail.data.entity.Sale>,
    onSaleClick: (com.nexapos.retail.data.entity.Sale) -> Unit,
    modifier: Modifier,
    onNewSale: () -> Unit,
    onNewPurchase: (Long) -> Unit,
) {
    val c = PosTheme.colors
    if (selected == null) {
        Column(
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(c.raised)
                .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PosIcon(PosIcons.people, tint = c.muted, size = 28.dp)
            Spacer(Modifier.height(10.dp))
            Text("No party selected", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a row on the left to see details and recent activity.",
                fontSize = 12.sp,
                color = c.muted,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val balanceRupees = (selected.balanceCents / 100).toInt()
    val isCustomer = selected.type == PartyEntity.TYPE_CUSTOMER
    val context = LocalContext.current
    // Tapping Statement opens an in-app preview first, then offers Share. This way
    // the button always shows something — on locked-down POS tablets a bare
    // ACTION_SEND share sheet can resolve to zero apps and silently do nothing.
    var showStatement by remember(selected.id) { mutableStateOf(false) }
    if (showStatement) {
        val statementBody = buildPartyStatement(context, selected, isCustomer, lifetimeRupees, balanceRupees, recentSales)
        PartyStatementDialog(
            partyName = selected.name,
            body = statementBody,
            onShare = {
                ReceiptOutput.shareText(context, "Statement — ${selected.name}", statementBody)
                showStatement = false
            },
            onDismiss = { showStatement = false },
        )
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(c.amberSoft).border(1.dp, c.hairline, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(initials(selected.name), fontFamily = JetBrainsMono, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c.amberPress)
            }
            Column(Modifier.weight(1f)) {
                Text(selected.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
                val sub =
                    listOf(selected.phone, selected.locality).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" }
                Text(sub, fontFamily = JetBrainsMono, fontSize = 11.5.sp, color = c.muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                label = "Lifetime",
                value =
                    if (lifetimeRupees > 0) {
                        rsStr(lifetimeRupees)
                    } else if (isCustomer) {
                        "Rs 0"
                    } else {
                        "—"
                    },
                sub =
                    if (!isCustomer) {
                        "supplier — sales N/A"
                    } else if (lifetimeRupees > 0) {
                        "${recentSales.size} sale${if (recentSales.size == 1) "" else "s"}"
                    } else {
                        "no sales yet"
                    },
                valueColor = if (lifetimeRupees > 0) c.emerald else c.muted,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Outstanding",
                value = if (balanceRupees > 0) rsStr(balanceRupees) else "Rs 0",
                sub = if (balanceRupees > 0) "amount due" else "no balance",
                valueColor = if (balanceRupees > 0) c.crimson else c.emerald,
                modifier = Modifier.weight(1f),
            )
        }
        Column {
            Eyebrow("Recent")
            Spacer(Modifier.height(8.dp))
            if (!isCustomer) {
                Text(
                    "Sales history is for customers. This party is a supplier.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            } else if (recentSales.isEmpty()) {
                Text(
                    "Recent sales linked to this customer will appear here once they buy something.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            } else {
                val dayFmt = java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale.US)
                recentSales.take(MAX_RECENT_ROWS).forEach { sale ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSaleClick(sale) }.padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                sale.receiptNo,
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = c.ink,
                            )
                            Text(
                                dayFmt.format(java.util.Date(sale.createdAt)),
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = c.muted,
                            )
                        }
                        Text(
                            sale.paymentMethod,
                            fontSize = 11.sp,
                            color = c.muted,
                        )
                        Text(
                            rsStr((sale.totalCents / 100).toInt()),
                            fontFamily = JetBrainsMono,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.ink,
                        )
                        Text("›", fontSize = 18.sp, color = c.muted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                }
                if (recentSales.size > MAX_RECENT_ROWS) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "+ ${recentSales.size - MAX_RECENT_ROWS} more — open Sales to see all.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecBtn(PosIcons.share, "Statement") { showStatement = true }
            Box(Modifier.weight(1f)) { }
            // A supplier can't be sold to — they're someone we buy from. Offer a
            // purchase order instead; customers get the sale action.
            if (isCustomer) {
                PrimaryBtn(PosIcons.cart, "New sale", onNewSale)
            } else {
                PrimaryBtn(PosIcons.truck, "New purchase") { onNewPurchase(selected.id) }
            }
        }
    }
}

/**
 * In-app preview of a party's account statement. Renders the plain-text [body]
 * (the same text used for sharing) in a scrollable monospace block, with a
 * Share action that hands off to the system share sheet. Showing it in-app first
 * guarantees a visible result on every device, including POS tablets where no
 * app handles a plain-text share intent.
 */
@Composable
private fun PartyStatementDialog(
    partyName: String,
    body: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = {
            Column {
                Text("Statement", fontWeight = FontWeight.Bold)
                Text(
                    partyName,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = c.muted,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    body,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = c.ink,
                )
            }
        },
    )
}

private const val MAX_RECENT_ROWS = 5

@Composable
private fun MiniStat(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(c.raised2).border(1.dp, c.hairline2, RoundedCornerShape(10.dp)).padding(12.dp)) {
        Eyebrow(label)
        Spacer(Modifier.height(2.dp))
        Text(value, fontFamily = JetBrainsMono, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(sub, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted)
    }
}

@Composable
private fun AddPartyDialog(
    isSupplier: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(name, phone, locality, isSupplier) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (isSupplier) "Add supplier" else "Add customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditableField("Name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "Full name")
                EditableField("Phone", phone, { phone = it }, Modifier.fillMaxWidth(), placeholder = "+230 …", mono = true)
                EditableField("Locality", locality, { locality = it }, Modifier.fillMaxWidth(), placeholder = "Town")
            }
        },
    )
}
