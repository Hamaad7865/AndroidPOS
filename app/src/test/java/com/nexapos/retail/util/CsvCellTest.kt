package com.nexapos.retail.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCellTest {
    @Test
    fun `formula-triggering text is prefixed with a quote`() {
        assertEquals("'=cmd", csvCell("=cmd"))
        assertEquals("'@SUM(A1)", csvCell("@SUM(A1)"))
    }

    @Test
    fun `negative and signed numbers are left intact for numeric columns`() {
        assertEquals("-50", csvCell("-50"))
        assertEquals("+50", csvCell("+50"))
        assertEquals("1200", csvCell("1200"))
    }

    @Test
    fun `ordinary text is unchanged and commas still get quoted`() {
        assertEquals("Claw Hammer", csvCell("Claw Hammer"))
        assertEquals("\"Acme, Ltd\"", csvCell("Acme, Ltd"))
    }

    @Test
    fun `a formula that also needs quoting is both prefixed and quoted`() {
        // Leading '=' → prefixed with '; embedded comma → wrapped in quotes.
        assertEquals("\"'=1,2\"", csvCell("=1,2"))
    }
}
