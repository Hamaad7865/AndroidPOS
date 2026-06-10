package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.VatType
import org.junit.Assert.assertEquals
import org.junit.Test

class VatCalcTest {
    private fun product(
        priceCents: Long,
        type: VatType,
    ) = PosProduct(id = "1", name = "x", cat = "c", priceCents = priceCents, sku = "s", stock = 0, kind = "generic", vatType = type)

    private fun line(
        priceCents: Long,
        qty: Int,
        type: VatType,
    ) = PosLine(product(priceCents, type), qty)

    @Test
    fun `standard line carries embedded 15 percent`() {
        // 1150 inclusive → VAT portion = 1150 - 1150/1.15 = 150
        assertEquals(150L, vatOf(listOf(line(1150, 1, VatType.STANDARD)), vatRegistered = true))
    }

    @Test
    fun `exempt and zero rated carry no vat`() {
        val lines = listOf(line(1150, 1, VatType.EXEMPT), line(2300, 1, VatType.ZERO_RATED))
        assertEquals(0L, vatOf(lines, vatRegistered = true))
    }

    @Test
    fun `mixed cart sums only the standard lines`() {
        val lines = listOf(line(1150, 1, VatType.STANDARD), line(500, 1, VatType.EXEMPT))
        assertEquals(150L, vatOf(lines, vatRegistered = true))
    }

    @Test
    fun `not vat registered yields zero`() {
        assertEquals(0L, vatOf(listOf(line(1150, 2, VatType.STANDARD)), vatRegistered = false))
    }

    @Test
    fun `discountedVat with no discount equals plain inclusive vat`() {
        assertEquals(150L, discountedVat(listOf(line(1150, 1, VatType.STANDARD)), cartDiscountCents = 0L, vatRegistered = true))
    }

    @Test
    fun `item discount lowers the vat`() {
        // net = 1150 - 150 = 1000 → vat = 1000 - 1000/1.15 = 130
        val lines = listOf(PosLine(product(1150, VatType.STANDARD), 1, discountCents = 150L))
        assertEquals(130L, discountedVat(lines, cartDiscountCents = 0L, vatRegistered = true))
    }

    @Test
    fun `cart discount lowers the vat proportionally`() {
        // afterItems = 1150, cart 150 → final 1000 → vat = 130
        assertEquals(130L, discountedVat(listOf(line(1150, 1, VatType.STANDARD)), cartDiscountCents = 150L, vatRegistered = true))
    }

    @Test
    fun `discountedVat is zero when not registered`() {
        assertEquals(0L, discountedVat(listOf(line(1150, 1, VatType.STANDARD)), cartDiscountCents = 0L, vatRegistered = false))
    }
}
