package com.nexapos.retail.ui.labels

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.profile.LabelPrinterSettings
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.products.printProductLabels
import com.nexapos.retail.ui.sale.PosProduct
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

/**
 * Print barcode stickers on the thermal label printer: pick products, choose
 * copies per item ("= stock" prints one per unit on hand), then print with
 * progress and resume-on-failure. Items without a barcode can be bulk-assigned
 * in-store EAN-13s. The A4 sticker-sheet path remains as a fallback.
 */
@Composable
fun LabelPrintScreen(
    vm: LabelPrintViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var generatedNote by remember { mutableStateOf<String?>(null) }

    val visible =
        remember(vm.products, query) {
            if (query.isBlank()) {
                vm.products
            } else {
                vm.products.filter { (it.name + it.sku + (it.barcode ?: "")).contains(query, ignoreCase = true) }
            }
        }
    val missing = vm.missingBarcode()
    val configured = LabelPrinterSettings.configured(context)

    NavShell(active = "products", onNav = onNav) {
        AppBar(
            title = "Print labels",
            subtitle = "Name + SKU + barcode stickers · thermal printer",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(null, "Back", onBack)
                    SecBtn(PosIcons.print, "A4 sheet") {
                        val selected = vm.products.filter { (vm.copies[it.id] ?: 0) > 0 && !it.barcode.isNullOrBlank() }
                        if (selected.isNotEmpty()) {
                            printProductLabels(context, selected.map { it.toPosProduct() }, BusinessProfile.name(context))
                        }
                    }
                }
            },
        )
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) {
            EditableField("Search", query, { query = it }, Modifier.fillMaxWidth(), placeholder = "Name, SKU or barcode…")
            Spacer(Modifier.height(10.dp))

            // Bulk actions + running total.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecBtn(null, "1 each") { vm.oneEach(visible) }
                SecBtn(null, "= stock") { vm.stockEach(visible) }
                SecBtn(null, "Clear") { vm.clearSelection() }
                Spacer(Modifier.weight(1f))
                Text(
                    "${vm.selectedProducts} product${if (vm.selectedProducts == 1) "" else "s"} · ${vm.totalLabels} label${if (vm.totalLabels == 1) "" else "s"}",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.ink,
                )
            }
            Spacer(Modifier.height(10.dp))

            if (!configured) {
                NoticeRow("No label printer set up yet — tap to configure.", c.low) { onNav("label-printer-settings") }
                Spacer(Modifier.height(8.dp))
            }
            if (missing.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.lowSoft).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${missing.size} selected item${if (missing.size == 1) " has" else "s have"} no barcode and will be skipped.",
                        Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.low,
                    )
                    SecBtn(null, "Generate & save") {
                        vm.generateMissingBarcodes { n -> generatedNote = "Generated $n barcode${if (n == 1) "" else "s"}." }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            generatedNote?.let {
                Text(it, fontSize = 12.sp, fontFamily = JetBrainsMono, color = c.emerald)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(visible, key = { it.id }) { p ->
                    ProductRow(p, vm.copies[p.id] ?: 0, onDec = { vm.decrement(p.id) }, onInc = { vm.increment(p.id) })
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                }
            }

            // Print bar: progress / error / action.
            Spacer(Modifier.height(10.dp))
            if (vm.printing) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Printing… item ${vm.progressDone} of ${vm.progressTotal}",
                        Modifier.weight(1f),
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = c.ink,
                    )
                    SecBtn(null, "Cancel") { vm.cancelPrint() }
                }
            } else {
                vm.printError?.let { err ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(err, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.crimson)
                        if (vm.canRetry) SecBtn(null, "Retry from there") { vm.retry() }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (vm.printedOk) {
                    Text("Done — labels sent to the printer.", fontSize = 12.sp, fontFamily = JetBrainsMono, color = c.emerald)
                    Spacer(Modifier.height(8.dp))
                }
                val count = vm.totalLabels
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (count > 0 && configured) c.amber else c.hairline)
                        .clickable(enabled = count > 0 && configured) {
                            vm.startPrint(showPrice = LabelPrinterSettings.showPrice(context))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (count > 0) "Print $count label${if (count == 1) "" else "s"}" else "Pick items to print",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    p: Product,
    count: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    p.sku.ifBlank { null },
                    p.barcode ?: "no barcode",
                    "stock ${p.stockQty}",
                ).joinToString(" · "),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = if (p.barcode.isNullOrBlank()) c.low else c.muted,
            )
        }
        StepBtn("−", onDec)
        Text(
            if (count == 0) "·" else "$count",
            Modifier.width(40.dp),
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (count == 0) c.muted else c.ink,
            textAlign = TextAlign.Center,
        )
        StepBtn("+", onInc)
    }
}

@Composable
private fun StepBtn(
    label: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
    }
}

@Composable
private fun NoticeRow(
    message: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Minimal display-model bridge for the A4 sticker-sheet fallback. */
private fun Product.toPosProduct(): PosProduct =
    PosProduct(
        id = id.toString(),
        name = name,
        cat = "",
        priceCents = priceCents,
        sku = sku,
        stock = stockQty,
        kind = kind,
        barcode = barcode,
    )
