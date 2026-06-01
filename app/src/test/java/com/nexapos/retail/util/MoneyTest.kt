package com.nexapos.retail.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsCentsWithSymbolAndThousands() {
        assertEquals("Rs 1,250.00", Money.format(125_000))
        assertEquals("Rs 0.05", Money.format(5))
        assertEquals("Rs 0.00", Money.format(0))
    }

    @Test
    fun parsesMajorUnitsToCents() {
        assertEquals(1_250L, Money.parseToCents("12.50"))
        assertEquals(1_000L, Money.parseToCents("10"))
        assertEquals(100_000L, Money.parseToCents("1,000")) // commas stripped -> Rs 1,000.00
    }

    @Test
    fun parseReturnsNullForInvalidInput() {
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents(""))
    }
}
