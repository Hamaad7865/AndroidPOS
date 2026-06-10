package com.nexapos.retail.domain

import com.nexapos.retail.data.dao.PayMethodTotal
import com.nexapos.retail.data.entity.Shift

/** Drawer-cash movements of one shift, all in cents. Pure — unit-tested on the JVM. */
data class ShiftCashInputs(
    val openingFloatCents: Long,
    /** SUM(tendered − change) over COMPLETED CASH sales in the shift. */
    val cashSalesKeptCents: Long,
    /** SUM(total) over CASH-refund returns in the shift. */
    val cashRefundsCents: Long,
    /** SUM(amount) over manual INCOME entries in the shift. */
    val manualIncomeCents: Long,
    /** SUM(amount) over manual EXPENSE entries in the shift. */
    val manualExpenseCents: Long,
)

/** Everything the shift screen and the printed close report show. */
data class ShiftSummary(
    val shift: Shift,
    val salesCount: Int,
    val byMethod: List<PayMethodTotal>,
    val returnsCount: Int,
    val returnsTotalCents: Long,
    val cashInCents: Long,
    val cashOutCents: Long,
    val expectedCashCents: Long,
    /** declared − expected; null while the shift is open (nothing counted yet). */
    val overShortCents: Long?,
)

object ShiftCalc {
    /**
     * What should be in the drawer right now:
     * float + cash kept from sales − cash refunds + manual income − manual expenses.
     * Known limits (shown as fine print in the UI): credit-sale down-payments are
     * not counted, and manual entries against bank accounts skew the figure —
     * MoneyTxn has no cash/bank flag yet.
     */
    fun expectedCashCents(inputs: ShiftCashInputs): Long =
        inputs.openingFloatCents +
            inputs.cashSalesKeptCents -
            inputs.cashRefundsCents +
            inputs.manualIncomeCents -
            inputs.manualExpenseCents

    /** Positive = drawer is over, negative = short. */
    fun overShortCents(
        declaredCents: Long,
        expectedCents: Long,
    ): Long = declaredCents - expectedCents
}
