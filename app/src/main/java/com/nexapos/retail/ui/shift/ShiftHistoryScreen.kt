package com.nexapos.retail.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.reports.ExportButtons
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val rowFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

/**
 * Past till sessions. Admins see every shift; cashiers only their own
 * (filtered in the ViewModel via StaffPolicy). Tapping a row opens its frozen
 * summary with the same print/export actions as the close screen.
 */
@Composable
fun ShiftHistoryScreen(
    vm: ShiftViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    LaunchedEffect(Unit) { vm.loadHistory() }
    val detail = vm.historyDetail

    NavShell(active = "money", onNav = onNav) {
        AppBar(
            title = if (detail == null) "Shift history" else "Shift report",
            subtitle =
                if (detail == null) {
                    "Past till sessions · tap one to reprint its report"
                } else {
                    "${detail.shift.staffName} · ${rowFmt.format(Date(detail.shift.openedAt))}"
                },
            right = {
                SecBtn(null, "‹ Back") {
                    if (detail != null) vm.clearDetail() else onBack()
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (detail != null) {
                    ShiftDetailBody(vm, detail)
                } else {
                    HistoryList(vm)
                }
            }
        }
    }
}

@Composable
private fun ShiftDetailBody(
    vm: ShiftViewModel,
    detail: com.nexapos.retail.domain.ShiftSummary,
) {
    SummaryCards(detail)
    ExportButtons(shiftReportData(detail))
    WideBtn("Back to history", primary = false, Modifier.fillMaxWidth()) { vm.clearDetail() }
}

@Composable
private fun HistoryList(vm: ShiftViewModel) {
    val c = PosTheme.colors
    if (vm.history.isEmpty()) {
        ShiftCard {
            Text(
                "No shifts yet. Open the till from the Shift screen and the sessions will show up here.",
                fontSize = 13.sp,
                color = c.muted,
            )
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
    ) {
        vm.history.forEachIndexed { i, shift ->
            HistoryRow(shift) { vm.loadDetail(shift.id) }
            if (i < vm.history.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
        }
    }
}

@Composable
private fun HistoryRow(
    shift: Shift,
    onOpen: () -> Unit,
) {
    val c = PosTheme.colors
    val open = shift.status == Shift.STATUS_OPEN
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shift.staffName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
                if (open) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(c.amberSoft).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("OPEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em, color = c.amberPress)
                    }
                }
            }
            Text(
                rowFmt.format(Date(shift.openedAt)) +
                    (shift.closedAt?.let { " — ${rowFmt.format(Date(it))}" } ?: ""),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = c.muted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Float ${rsStr(shift.openingFloatCents)}",
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                color = c.muted,
            )
            val declared = shift.declaredCashCents
            val expected = shift.expectedCashCents
            if (declared != null && expected != null) {
                val os = declared - expected
                val label =
                    when {
                        os > 0L -> "Over ${rsStr(os)}"
                        os < 0L -> "Short ${rsStr(-os)}"
                        else -> "Balanced"
                    }
                Text(
                    label,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (os < 0) c.crimson else c.emerald,
                )
            }
        }
        Text("›", fontSize = 18.sp, color = c.muted)
    }
}
