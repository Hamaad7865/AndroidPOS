package com.nexapos.retail.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.profile.DrawerSettings
import com.nexapos.retail.domain.ShiftSummary
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.Eyebrow
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.reports.ExportButtons
import com.nexapos.retail.ui.session.currentStaff
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val openedFmt = SimpleDateFormat("HH:mm", Locale.US)
private const val CENTS_PER_RUPEE = 100L

internal fun centsToRupees(cents: Long): Int = (cents / CENTS_PER_RUPEE).toInt()

/**
 * The till-shift hub. Three states: no shift (open form with float entry),
 * shift open (live summary + close), just closed (frozen report + print).
 */
@Composable
fun ShiftScreen(
    vm: ShiftViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val shift by vm.openShift.collectAsState()
    val justClosed = vm.justClosed
    var showClose by remember { mutableStateOf(false) }

    if (showClose) {
        val summary = vm.liveSummary
        if (summary != null) {
            ShiftCloseDialog(
                expectedRupees = centsToRupees(summary.expectedCashCents),
                onDismiss = { showClose = false },
                onConfirm = { counted, note ->
                    vm.close(counted, note)
                    showClose = false
                },
            )
        }
    }

    NavShell(active = "money", onNav = onNav) {
        AppBar(
            title = "Shift / Till",
            subtitle =
                when {
                    justClosed != null -> "Shift closed — report below"
                    shift != null -> "Open since ${openedFmt.format(Date(shift!!.openedAt))} · ${shift!!.staffName}"
                    else -> "No shift open"
                },
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(null, "History") { onNav("shift-history") }
                    SecBtn(null, "‹ Back", onBack)
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                vm.error?.let { msg ->
                    Text(msg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.crimson)
                }
                when {
                    justClosed != null -> ClosedReport(justClosed) { vm.dismissJustClosed() }
                    shift == null -> OpenShiftCard(vm)
                    else -> LiveShiftBody(vm, onClose = { showClose = true })
                }
            }
        }
    }
}

/** State 1 — no shift: enter the float and open the till. */
@Composable
private fun OpenShiftCard(vm: ShiftViewModel) {
    val c = PosTheme.colors
    val staff = currentStaff()
    var floatRupees by remember { mutableStateOf("") }
    ShiftCard {
        Eyebrow("Open the till")
        Spacer(Modifier.height(6.dp))
        Text(
            if (staff != null) "Opening as ${staff.name}." else "Sign in first to open a shift.",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.ink,
        )
        Text(
            "Count the cash you're putting in the drawer (the float). Every sale, return and " +
                "money entry from now until close is tallied on this shift's report.",
            fontSize = 12.sp,
            color = c.muted,
        )
        Spacer(Modifier.height(10.dp))
        EditableField(
            "Opening float (Rs)",
            floatRupees,
            { floatRupees = it },
            Modifier.fillMaxWidth(),
            mono = true,
            number = true,
            placeholder = "e.g. 2000",
        )
        Spacer(Modifier.height(12.dp))
        WideBtn("Open shift", primary = true, Modifier.fillMaxWidth()) {
            if (staff != null) vm.open(floatRupees.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0)
        }
    }
}

/** State 2 — open shift: live tallies + actions. */
@Composable
private fun LiveShiftBody(
    vm: ShiftViewModel,
    onClose: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = vm.liveSummary

    if (summary == null) {
        ShiftCard { Text("Loading shift…", fontSize = 13.sp, color = c.muted) }
        return
    }
    SummaryCards(summary)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (DrawerSettings.isConfigured(context)) {
            WideBtn("Open drawer", primary = false, Modifier.weight(1f)) {
                val kicker = (context.applicationContext as PosApplication).container.drawerKicker
                scope.launch { kicker.kickNow(KickReason.MANUAL) }
            }
        }
        WideBtn("Close shift · count drawer", primary = true, Modifier.weight(2f)) { onClose() }
    }
    Text(
        "Expected cash assumes credit down-payments and bank-account money entries don't go " +
            "through the drawer — record till cash movements as Money income/expense.",
        fontSize = 11.sp,
        color = c.muted,
    )
}

/** State 3 — just closed: frozen report + export/print. */
@Composable
private fun ClosedReport(
    summary: ShiftSummary,
    onDone: () -> Unit,
) {
    SummaryCards(summary)
    ExportButtons(shiftReportData(summary))
    WideBtn("Done", primary = true, Modifier.fillMaxWidth()) { onDone() }
}

/** The shared tally cards: per-method totals, returns, cash movements, drawer. */
@Composable
internal fun SummaryCards(summary: ShiftSummary) {
    val c = PosTheme.colors
    val shift = summary.shift
    ShiftCard {
        Eyebrow("Sales · ${summary.salesCount} transactions")
        Spacer(Modifier.height(6.dp))
        if (summary.byMethod.isEmpty()) {
            Text("No sales yet this shift.", fontSize = 13.sp, color = c.muted)
        } else {
            summary.byMethod.sortedByDescending { it.cents }.forEach { m ->
                SumLine("${payMethodLabel(m.paymentMethod)} · ${m.count}×", rsStr(centsToRupees(m.cents)))
            }
        }
        if (summary.returnsCount > 0) {
            Spacer(Modifier.height(4.dp))
            SumLine(
                "Returns · ${summary.returnsCount}×",
                "− ${rsStr(centsToRupees(summary.returnsTotalCents))}",
                color = c.crimson,
            )
        }
    }
    ShiftCard {
        Eyebrow("Cash drawer")
        Spacer(Modifier.height(6.dp))
        SumLine("Opening float", rsStr(centsToRupees(shift.openingFloatCents)))
        SumLine("Cash in (manual)", "+ ${rsStr(centsToRupees(summary.cashInCents))}")
        SumLine("Cash out (manual)", "− ${rsStr(centsToRupees(summary.cashOutCents))}")
        Spacer(Modifier.height(4.dp))
        SumLine("Expected cash in drawer", rsStr(centsToRupees(summary.expectedCashCents)), bold = true)
        shift.declaredCashCents?.let { declared ->
            SumLine("Counted cash", rsStr(centsToRupees(declared)), bold = true)
        }
        summary.overShortCents?.let { os ->
            val label =
                when {
                    os > 0 -> "OVER by ${rsStr(centsToRupees(os))}"
                    os < 0 -> "SHORT by ${rsStr(centsToRupees(-os))}"
                    else -> "Balanced"
                }
            SumLine("Over / short", label, bold = true, color = if (os < 0) c.crimson else c.emerald)
        }
    }
}

@Composable
internal fun ShiftCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
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
internal fun SumLine(
    label: String,
    value: String,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color? = null,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color ?: c.ink,
        )
        Text(
            value,
            fontFamily = JetBrainsMono,
            fontSize = 13.5.sp,
            letterSpacing = (-0.01).em,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = color ?: c.ink,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}
