package com.nexapos.retail.ui.shift

import com.nexapos.retail.data.dao.PayMethodTotal
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.domain.ShiftSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftReportTest {
    private val closedShift =
        Shift(
            id = 7,
            staffId = 1,
            staffName = "Priya",
            openedAt = 1_700_000_000_000,
            closedAt = 1_700_030_000_000,
            openingFloatCents = 100_000,
            declaredCashCents = 109_000,
            expectedCashCents = 110_000,
            status = Shift.STATUS_CLOSED,
        )

    private val summary =
        ShiftSummary(
            shift = closedShift,
            salesCount = 3,
            byMethod =
                listOf(
                    PayMethodTotal("CASH", 2, 25_000),
                    PayMethodTotal("MOBILE", 1, 30_000),
                ),
            returnsCount = 1,
            returnsTotalCents = 5_000,
            cashInCents = 0,
            cashOutCents = 10_000,
            expectedCashCents = 110_000,
            overShortCents = -1_000,
        )

    @Test
    fun `report carries per-method labels and the shortage`() {
        val data = shiftReportData(summary)
        assertEquals("Shift Report", data.title)
        val summaryMap = data.summary.toMap()
        assertTrue(summaryMap.keys.any { it.contains("Cash") })
        assertTrue(summaryMap.keys.any { it.contains("Juice") }) // MOBILE renders as Juice
        assertEquals("SHORT by Rs 10", summaryMap["Over / short"])
        assertEquals("Rs 1,100", summaryMap["Expected cash in drawer"])
        assertEquals("Rs 1,090", summaryMap["Counted cash"])
        // Table body: one row per method with the bigger one first.
        assertEquals(listOf("Juice (mobile)", "1", "Rs 300"), data.rows.first())
    }

    @Test
    fun `payment method labels cover the till's ids`() {
        assertEquals("Cash", payMethodLabel("CASH"))
        assertEquals("Card", payMethodLabel("CARD"))
        assertEquals("Juice (mobile)", payMethodLabel("MOBILE"))
        assertEquals("Credit", payMethodLabel("CREDIT"))
        assertEquals("Cheque", payMethodLabel("CHEQUE"))
    }
}
