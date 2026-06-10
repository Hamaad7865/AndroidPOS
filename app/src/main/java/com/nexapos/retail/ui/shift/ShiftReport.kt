package com.nexapos.retail.ui.shift

import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.ShiftSummary
import com.nexapos.retail.ui.reports.ReportData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tsFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

private const val CENTS_PER_RUPEE = 100L

private fun rsc(cents: Long): String = "Rs ${"%,d".format(cents / CENTS_PER_RUPEE)}"

/** Display label for a stored payment-method id (CASH, CARD, MOBILE, CREDIT). */
internal fun payMethodLabel(method: String): String =
    when (method.uppercase(Locale.US)) {
        "CASH" -> "Cash"
        "CARD" -> "Card"
        "MOBILE" -> "Juice (mobile)"
        "CREDIT" -> "Credit"
        else -> method.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
    }

/**
 * Shapes a shift summary into the report pipeline's [ReportData] — the same
 * structure every other report prints/exports through (CSV + PDF/print).
 * Pure function: unit-testable, no Android imports.
 */
internal fun shiftReportData(s: ShiftSummary): ReportData {
    val shift = s.shift
    val summary =
        buildList {
            add("Till opened" to "${tsFmt.format(Date(shift.openedAt))} by ${shift.staffName}")
            shift.closedAt?.let { add("Till closed" to tsFmt.format(Date(it))) }
            add("Sales" to "${s.salesCount} transaction${if (s.salesCount == 1) "" else "s"}")
            s.byMethod.sortedByDescending { it.cents }.forEach { m ->
                add("  ${payMethodLabel(m.paymentMethod)}" to "${m.count} × · ${rsc(m.cents)}")
            }
            add("Returns" to "${s.returnsCount} · ${rsc(s.returnsTotalCents)}")
            add("Cash in (manual)" to rsc(s.cashInCents))
            add("Cash out (manual)" to rsc(s.cashOutCents))
            add("Opening float" to rsc(shift.openingFloatCents))
            add("Expected cash in drawer" to rsc(s.expectedCashCents))
            shift.declaredCashCents?.let { add("Counted cash" to rsc(it)) }
            s.overShortCents?.let { os ->
                val label =
                    when {
                        os > 0 -> "OVER by ${rsc(os)}"
                        os < 0 -> "SHORT by ${rsc(-os)}"
                        else -> "Balanced — Rs 0"
                    }
                add("Over / short" to label)
            }
            if (shift.note.isNotBlank()) add("Note" to shift.note)
        }
    val rows =
        s.byMethod.sortedByDescending { it.cents }.map { m ->
            listOf(payMethodLabel(m.paymentMethod), m.count.toString(), rsc(m.cents))
        }
    return ReportData(
        title = "Shift Report",
        subtitle = "Till session · ${shift.staffName}",
        periodLabel =
            if (shift.status == Shift.STATUS_CLOSED) {
                "${tsFmt.format(Date(shift.openedAt))} — ${shift.closedAt?.let { tsFmt.format(Date(it)) }.orEmpty()}"
            } else {
                "Open since ${tsFmt.format(Date(shift.openedAt))}"
            },
        summary = summary,
        columns = listOf("Payment method", "Sales", "Amount"),
        rows = rows,
    )
}
