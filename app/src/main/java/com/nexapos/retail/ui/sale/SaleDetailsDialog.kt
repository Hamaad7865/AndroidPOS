package com.nexapos.retail.ui.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.formatNum
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dlgDayFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

private fun rs(cents: Long): String = "Rs " + formatNum((cents / 100.0), 0)

/**
 * Read-only details view of a completed sale — receipt number, time, customer,
 * full line items and totals breakdown. Used by [SalesListScreen] and the
 * Parties detail panel; pass null for [sale] to hide.
 *
 * [items] is the list of `sale_items` rows; while it's still loading pass an
 * empty list and a placeholder is shown.
 */
@Composable
fun SaleDetailsDialog(
    sale: Sale?,
    items: List<SaleItem>,
    onDismiss: () -> Unit,
    onReturn: ((Sale) -> Unit)? = null,
) {
    if (sale == null) return
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                if (onReturn != null && sale.status.equals("COMPLETED", ignoreCase = true)) {
                    TextButton(onClick = { onReturn(sale) }) { Text("Return / refund") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = {
            Column {
                Text("Sale ${sale.receiptNo}", fontWeight = FontWeight.Bold)
                Text(
                    dlgDayFmt.format(Date(sale.createdAt)),
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = c.muted,
                    fontWeight = FontWeight.Medium,
                )
                if (sale.customerName.isNotBlank()) {
                    Text(
                        "Customer · ${sale.customerName}",
                        fontSize = 12.sp,
                        color = c.muted,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Eyebrow("Line items")
                if (items.isEmpty()) {
                    Text("Loading items…", fontSize = 12.sp, color = c.muted)
                } else {
                    items.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${item.quantity}×",
                                Modifier.width(36.dp),
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = c.muted,
                            )
                            Text(
                                item.nameSnapshot,
                                Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                rs(item.lineTotalCents),
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = c.ink,
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Eyebrow("Totals")
                SaleSumLine("Subtotal", rs(sale.subtotalCents))
                SaleSumLine("Discount", "− " + rs(sale.discountCents))
                SaleSumLine("VAT", rs(sale.taxCents))
                SaleSumLine("Total", rs(sale.totalCents), bold = true)
                SaleSumLine("Tendered (${sale.paymentMethod})", rs(sale.amountTenderedCents))
                SaleSumLine("Change", rs(sale.changeCents))
            }
        },
    )
}

@Composable
private fun SaleSumLine(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = if (bold) c.ink else c.muted, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
        Text(value, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, color = c.ink)
    }
}
