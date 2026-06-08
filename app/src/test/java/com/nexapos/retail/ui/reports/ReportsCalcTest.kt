package com.nexapos.retail.ui.reports

import com.nexapos.retail.data.entity.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsCalcTest {
    private fun item(
        productId: Long?,
        lineTotalCents: Long,
        discountCents: Long = 0,
        quantity: Int = 1,
    ) = SaleItem(
        saleId = 1,
        productId = productId,
        nameSnapshot = "x",
        unitPriceCents = if (quantity > 0) lineTotalCents / quantity else lineTotalCents,
        quantity = quantity,
        lineTotalCents = lineTotalCents,
        discountCents = discountCents,
    )

    @Test
    fun `subtracts each line's own discount from its revenue`() {
        val items = listOf(item(productId = 1, lineTotalCents = 20_000, discountCents = 2_000))
        // saleDiscountCents is the TOTAL discount; here it is only the per-line discount (no cart discount).
        val revenue = netRevenueByProduct(items, saleDiscountCents = 2_000)
        assertEquals(18_000L, revenue[1])
    }

    @Test
    fun `allocates the cart-level discount proportionally by net line value`() {
        val items =
            listOf(
                // net 18_000
                item(productId = 1, lineTotalCents = 20_000, discountCents = 2_000),
                // net 20_000
                item(productId = 2, lineTotalCents = 20_000, discountCents = 0),
            )
        // total discount 5_800 = 2_000 per-line + 3_800 cart; cart spreads 1_800 / 2_000 across the lines.
        val revenue = netRevenueByProduct(items, saleDiscountCents = 5_800)
        assertEquals(16_200L, revenue[1])
        assertEquals(18_000L, revenue[2])
    }

    @Test
    fun `aggregates multiple lines of the same product`() {
        val items =
            listOf(
                item(productId = 1, lineTotalCents = 10_000),
                item(productId = 1, lineTotalCents = 10_000),
            )
        val revenue = netRevenueByProduct(items, saleDiscountCents = 0)
        assertEquals(20_000L, revenue[1])
    }

    @Test
    fun `a non-catalog line dilutes the cart discount but earns no product revenue`() {
        val items =
            listOf(
                item(productId = 1, lineTotalCents = 20_000),
                item(productId = null, lineTotalCents = 20_000),
            )
        // cart discount 4_000 over a 40_000 net base → 2_000 lands on the catalog line.
        val revenue = netRevenueByProduct(items, saleDiscountCents = 4_000)
        assertEquals(18_000L, revenue[1])
        assertEquals(1, revenue.size) // the null-product line is never recorded
    }

    @Test
    fun `fully discounted lines yield zero revenue without dividing by zero`() {
        val items = listOf(item(productId = 1, lineTotalCents = 5_000, discountCents = 5_000))
        val revenue = netRevenueByProduct(items, saleDiscountCents = 5_000)
        assertEquals(0L, revenue[1])
    }

    @Test
    fun `cart-discount shares use floor division so revenue is never understated by rounding`() {
        val items =
            listOf(
                item(productId = 1, lineTotalCents = 10_000),
                item(productId = 2, lineTotalCents = 20_000),
            )
        // cart discount 1_000 over a 30_000 base: shares floor to 333 and 666 (sum 999, 1c residue dropped).
        val revenue = netRevenueByProduct(items, saleDiscountCents = 1_000)
        assertEquals(9_667L, revenue[1])
        assertEquals(19_334L, revenue[2])
    }
}
