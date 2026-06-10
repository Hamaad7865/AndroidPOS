package com.nexapos.retail.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftCalcTest {
    private fun inputs(
        float: Long = 0,
        cashKept: Long = 0,
        cashRefunds: Long = 0,
        income: Long = 0,
        expense: Long = 0,
    ) = ShiftCashInputs(
        openingFloatCents = float,
        cashSalesKeptCents = cashKept,
        cashRefundsCents = cashRefunds,
        manualIncomeCents = income,
        manualExpenseCents = expense,
    )

    @Test
    fun `empty shift expects exactly the float`() {
        assertEquals(200_000, ShiftCalc.expectedCashCents(inputs(float = 200_000)))
    }

    @Test
    fun `cash sales add to the drawer`() {
        assertEquals(
            200_000 + 25_000,
            ShiftCalc.expectedCashCents(inputs(float = 200_000, cashKept = 25_000)),
        )
    }

    @Test
    fun `cash refunds come out of the drawer`() {
        assertEquals(
            200_000 - 5_000,
            ShiftCalc.expectedCashCents(inputs(float = 200_000, cashRefunds = 5_000)),
        )
    }

    @Test
    fun `manual income and expenses move the drawer both ways`() {
        assertEquals(
            100_000 + 7_500 - 10_000,
            ShiftCalc.expectedCashCents(inputs(float = 100_000, income = 7_500, expense = 10_000)),
        )
    }

    @Test
    fun `the user scenario - float 1000 cash 250 refund 50 expense 100 equals 1100 rupees`() {
        // Card/Juice/credit sales never appear in cashSalesKeptCents, so they
        // can't skew the drawer expectation.
        val expected =
            ShiftCalc.expectedCashCents(
                inputs(float = 100_000, cashKept = 25_000, cashRefunds = 5_000, expense = 10_000),
            )
        assertEquals(110_000, expected)
    }

    @Test
    fun `over and short carry their sign`() {
        assertEquals(-1_000, ShiftCalc.overShortCents(declaredCents = 109_000, expectedCents = 110_000))
        assertEquals(2_500, ShiftCalc.overShortCents(declaredCents = 112_500, expectedCents = 110_000))
        assertEquals(0, ShiftCalc.overShortCents(declaredCents = 110_000, expectedCents = 110_000))
    }

    @Test
    fun `long math survives big drawers`() {
        val big = 9_000_000_000L // Rs 90 million in cents — beyond Int range
        assertEquals(big * 2, ShiftCalc.expectedCashCents(inputs(float = big, cashKept = big)))
    }
}
