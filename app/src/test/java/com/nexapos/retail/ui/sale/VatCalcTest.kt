package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.VatType
import org.junit.Assert.assertEquals
import org.junit.Test

class VatCalcTest {
    private fun product(
        price: Int,
        type: VatType,
    ) = PosProduct(id = "1", name = "x", cat = "c", price = price, sku = "s", stock = 0, kind = "generic", vatType = type)

    private fun line(
        price: Int,
        qty: Int,
        type: VatType,
    ) = PosLine(product(price, type), qty)

    @Test
    fun `standard line carries embedded 15 percent`() {
        // 1150 inclusive → VAT portion = 1150 - 1150/1.15 = 150
        assertEquals(150, vatOf(listOf(line(1150, 1, VatType.STANDARD)), vatRegistered = true))
    }

    @Test
    fun `exempt and zero rated carry no vat`() {
        val lines = listOf(line(1150, 1, VatType.EXEMPT), line(2300, 1, VatType.ZERO_RATED))
        assertEquals(0, vatOf(lines, vatRegistered = true))
    }

    @Test
    fun `mixed cart sums only the standard lines`() {
        val lines = listOf(line(1150, 1, VatType.STANDARD), line(500, 1, VatType.EXEMPT))
        assertEquals(150, vatOf(lines, vatRegistered = true))
    }

    @Test
    fun `not vat registered yields zero`() {
        assertEquals(0, vatOf(listOf(line(1150, 2, VatType.STANDARD)), vatRegistered = false))
    }
}
