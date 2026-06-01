package com.nexapos.retail.ui.settings

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.profile.ReceiptSettings
import com.nexapos.retail.ui.checkout.ReceiptOutput
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.sale.PosLine
import com.nexapos.retail.ui.sale.PosProduct
import com.nexapos.retail.ui.sale.SaleSnapshot
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun PrintingSettingsScreen(
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    var paper by remember { mutableStateOf(ReceiptSettings.paper(context)) }
    var footer by remember { mutableStateOf(ReceiptSettings.footerNote(context)) }
    var saved by remember { mutableStateOf(false) }

    fun persist() {
        ReceiptSettings.setPaper(context, paper)
        ReceiptSettings.setFooterNote(context, footer)
        saved = true
    }

    NavShell(active = "settings", onNav = onNav) {
        AppBar(
            title = "Printing & receipt",
            subtitle = "Configure paper size, footer and test your printer",
            right = { SecBtn(null, "Back", onBack) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // How printing works
                Card {
                    Eyebrow("How printing works")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "NexaPOS prints through Android's built-in print system. It works with any " +
                            "printer that has an Android print plugin installed — most Wi-Fi / network printers, " +
                            "Mopria-compatible printers, and many thermal receipt printers via their vendor app. " +
                            "“Save as PDF” is always available even with no printer connected.",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }

                // Paper width
                Card {
                    Eyebrow("Receipt paper")
                    Spacer(Modifier.height(10.dp))
                    ReceiptSettings.Paper.entries.forEach { opt ->
                        RadioRow(label = opt.label, selected = paper == opt) {
                            paper = opt
                            saved = false
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "58 mm and 80 mm are the common thermal roll widths. Pick A4 if you print full-page invoices.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }

                // Footer note
                Card {
                    Eyebrow("Receipt footer")
                    Spacer(Modifier.height(10.dp))
                    EditableField(
                        "Footer note",
                        footer,
                        {
                            footer = it
                            saved = false
                        },
                        Modifier.fillMaxWidth(),
                        placeholder = ReceiptSettings.DEFAULT_FOOTER,
                        tall = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Printed at the bottom of every receipt — return policy, thank-you message, opening hours, etc.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WideBtn("Test print", primary = false, Modifier.weight(1f), icon = PosIcons.print) {
                        persist()
                        ReceiptOutput.print(context, sampleSale())
                    }
                    WideBtn(if (saved) "Saved ✓" else "Save", primary = true, Modifier.weight(1f), icon = PosIcons.check) {
                        persist()
                    }
                }
                Text(
                    "Test print opens the print dialog with a sample receipt so you can confirm the paper size and pick your printer.",
                    fontSize = 11.sp,
                    color = c.muted,
                )
            }
        }
    }
}

/** A throwaway sale used only for the Test print preview. */
private fun sampleSale(): SaleSnapshot {
    val lines =
        listOf(
            PosLine(PosProduct(id = "0", name = "Sample Hammer", cat = "Tools", price = 250, sku = "HMR-1", stock = 0, kind = "generic"), 2),
            PosLine(PosProduct(id = "0", name = "Sample Paint 5L", cat = "Paint", price = 600, sku = "PNT-5", stock = 0, kind = "generic"), 1),
        )
    val subtotal = lines.sumOf { it.lineTotal }
    return SaleSnapshot(
        lines = lines,
        subtotal = subtotal,
        discount = 0,
        vat = (subtotal * 0.15).toInt(),
        shipping = 0,
        total = subtotal,
        received = subtotal,
        change = 0,
        pay = "cash",
        invoiceNo = "TEST-0001",
        createdAt = System.currentTimeMillis(),
        customerName = "Walk-in",
    )
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
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (selected) c.amber else c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.amber))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.ink)
    }
}
