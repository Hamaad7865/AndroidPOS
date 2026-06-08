package com.nexapos.retail.ui.sale

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscountCalcTest {
    @Test
    fun `ten percent of 1000 is 100`() {
        assertEquals(100, percentToFlat(1000, 10))
    }

    @Test
    fun `rounds to the nearest rupee`() {
        // 1455 * 7% = 101.85 → 102
        assertEquals(102, percentToFlat(1455, 7))
    }

    @Test
    fun `percent above 100 is clamped`() {
        assertEquals(1000, percentToFlat(1000, 150))
    }

    @Test
    fun `negative percent clamps to zero`() {
        assertEquals(0, percentToFlat(1000, -5))
    }
}
