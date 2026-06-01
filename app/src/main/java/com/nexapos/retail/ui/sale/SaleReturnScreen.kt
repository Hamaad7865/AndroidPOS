package com.nexapos.retail.ui.sale

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun SaleReturnScreen(
    vm: SaleReturnViewModel,
    saleId: Long?,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current

    LaunchedEffect(saleId) { if (saleId != null) vm.load(saleId) }

    NavShell(active = "pos", onNav = onNav) {
        AppBar(
            title = "Return / Refund",
            subtitle =
                when {
                    saleId == null || vm.notFound -> "Sale not found"
                    vm.loading -> "Loading…"
                    else -> "Against ${vm.receiptNo} · ${vm.customerName}"
                },
            right = { SecBtn(null, "Cancel", onBack) },
        )

        if (saleId == null || vm.notFound) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PosIcon(PosIcons.refresh, tint = c.muted, size = 30.dp)
                Spacer(Modifier.height(10.dp))
                Text("Sale not found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
            }
            return@NavShell
        }
        if (vm.loading) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Loading sale…", fontSize = 13.sp, color = c.muted)
            }
            return@NavShell
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Items
                Card {
                    Eyebrow("Items to return")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Set how many of each line to return. Returned items are added back to stock.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    vm.lines.forEachIndexed { i, line ->
                        ReturnLineRow(
                            line = line,
                            qty = vm.chosen[i] ?: 0,
                            onInc = { vm.increment(i) },
                            onDec = { vm.decrement(i) },
                        )
                        if (i < vm.lines.size - 1) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
                        }
                    }
                }

                // Refund method
                Card {
                    Eyebrow("Refund method")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RefundChip(
                            label = "Cash refund",
                            sub = "Hand cash back",
                            selected = vm.refundMethod == "CASH",
                            enabled = true,
                            modifier = Modifier.weight(1f),
                        ) { vm.chooseRefundMethod("CASH") }
                        RefundChip(
                            label = "To credit",
                            sub = if (vm.canRefundToCredit) "Reduce ${vm.customerName}'s balance" else "Needs a customer",
                            selected = vm.refundMethod == "CREDIT",
                            enabled = vm.canRefundToCredit,
                            modifier = Modifier.weight(1f),
                        ) { vm.chooseRefundMethod("CREDIT") }
                    }
                }

                // Summary + confirm
                Card {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Eyebrow("Refund total")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                rsStr(vm.refundRupees),
                                fontFamily = JetBrainsMono,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = c.crimson,
                            )
                        }
                        Text(
                            "${vm.totalReturnUnits} unit${if (vm.totalReturnUnits == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    val label = if (vm.canRecord) "Confirm return · ${rsStr(vm.refundRupees)}" else "Pick at least one item"
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (vm.canRecord) c.amber else c.hairline)
                            .clickable(enabled = vm.canRecord) {
                                vm.record {
                                    android.widget.Toast.makeText(context, "Return recorded · stock updated", android.widget.Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vm.canRecord) androidx.compose.ui.graphics.Color.White else c.muted,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (vm.refundMethod == "CREDIT") {
                            "Refund reduces ${vm.customerName}'s outstanding balance."
                        } else {
                            "Cash refund — hand the amount back to the customer."
                        },
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReturnLineRow(
    line: ReturnLine,
    qty: Int,
    onInc: () -> Unit,
    onDec: () -> Unit,
) {
    val c = PosTheme.colors
    val exhausted = line.maxReturnable <= 0
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (exhausted) {
                    "Fully returned"
                } else {
                    "${rsStr(line.unitPriceRupees)} each · up to ${line.maxReturnable} returnable"
                },
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = c.muted,
            )
        }
        if (!exhausted) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBtn(PosIcons.minus, onDec)
                Text(
                    "$qty",
                    Modifier.width(30.dp),
                    fontFamily = JetBrainsMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.ink,
                    textAlign = TextAlign.Center,
                )
                StepBtn(PosIcons.plus, onInc)
            }
        }
    }
}

@Composable
private fun StepBtn(
    paths: List<String>,
    onClick: () -> Unit,
) {
    Box(Modifier.size(width = 32.dp, height = 34.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        PosIcon(paths, tint = PosTheme.colors.ink, size = 14.dp)
    }
}

@Composable
private fun RefundChip(
    label: String,
    sub: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.ink else c.raised)
            .border(1.5.dp, if (selected) c.ink else c.hairline, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color =
                if (!enabled) {
                    c.muted
                } else if (selected) {
                    c.surface
                } else {
                    c.ink
                },
        )
        Text(
            sub,
            fontSize = 11.sp,
            color = if (selected) c.surface.copy(alpha = 0.7f) else c.muted,
        )
    }
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
