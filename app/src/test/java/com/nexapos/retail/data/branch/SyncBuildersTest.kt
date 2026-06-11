package com.nexapos.retail.data.branch

import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.Shift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SyncBuildersTest {
    private val utc = ZoneId.of("UTC")

    private fun product(
        id: Long,
        priceCents: Long,
        stock: Int,
        low: Int = 5,
        cat: Long? = null,
    ) = Product(id = id, name = "P$id", priceCents = priceCents, stockQty = stock, lowStockThreshold = low, categoryId = cat)

    @Test
    fun `summary computes stock value, counts and low-stock exactly`() {
        val products = listOf(product(1, 1_250L, 4, low = 5), product(2, 10_000L, 0, low = 5), product(3, 500L, 100))
        val s = SyncBuilders.summary(salesTodayCents = 42_550L, ticketsToday = 3, products = products, openShift = null)
        assertEquals(42_550L, s.salesTodayCents)
        assertEquals(3, s.ticketsToday)
        // 1250*4 + 10000*0 + 500*100 = 5000 + 0 + 50000 = 55000
        assertEquals(55_000L, s.stockValueCents)
        assertEquals(3, s.itemCount)
        // product 1 (4<=5) and product 2 (0<=5) are low; product 3 (100) is not
        assertEquals(2, s.lowStockCount)
        assertEquals(null, s.openShiftStaff)
    }

    @Test
    fun `summary carries the open shift`() {
        val shift = Shift(staffId = 9, staffName = "Priya", openedAt = 1_234L, openingFloatCents = 100_000L)
        val s = SyncBuilders.summary(0L, 0, emptyList(), shift)
        assertEquals("Priya", s.openShiftStaff)
        assertEquals(1_234L, s.openShiftSince)
    }

    @Test
    fun `stock chunks split at the chunk size and preserve cents`() {
        val n = SyncBuilders.STOCK_CHUNK_SIZE + 100
        val products = (1..n).map { product(it.toLong(), it * 10L, it) }
        val chunks = SyncBuilders.stockChunks(products) { "Cat" }
        assertEquals(2, chunks.size)
        assertEquals(SyncBuilders.STOCK_CHUNK_SIZE, chunks[0].items.size)
        assertEquals(100, chunks[1].items.size)
        assertEquals(10L, chunks[0].items.first().priceCents)
        assertEquals("Cat", chunks[0].items.first().category)
    }

    @Test
    fun `day doc assembles sales, money and shifts`() {
        val sale = Sale(id = 1, receiptNo = "S-1", createdAt = 5L, subtotalCents = 1_000L, totalCents = 1_000L, paymentMethod = "CASH")
        val items = listOf(SaleItem(id = 1, saleId = 1, productId = null, nameSnapshot = "X", unitPriceCents = 1_000L, quantity = 1, lineTotalCents = 1_000L))
        val txn = MoneyTxn(id = 1, type = MoneyTxn.TYPE_INCOME, amountCents = 250L, createdAt = 6L)
        val shift = Shift(id = 1, staffId = 1, staffName = "Owner", openedAt = 1L, closedAt = 9L, openingFloatCents = 100_000L, declaredCashCents = 100_500L, expectedCashCents = 100_000L, status = Shift.STATUS_CLOSED)
        val day = SyncBuilders.dayDoc("2026-06-11", listOf(sale to items), emptyList(), listOf(txn), listOf(shift))
        assertEquals("2026-06-11", day.date)
        assertEquals(1_000L, day.sales.single().totalCents)
        assertEquals(250L, day.money.single().amountCents)
        assertEquals(500L, day.shifts.single().overShortCents)
        // full round-trip through the wire format
        assertEquals(day, DayDoc.fromMap(day.toMap()))
    }

    @Test
    fun `date key and start-of-day are consistent in a fixed zone`() {
        val now = 1_700_000_000_000L // 2023-11-14T22:13:20Z
        assertEquals("2023-11-14", SyncBuilders.dateKey(now, utc))
        val start = SyncBuilders.startOfDay(now, utc)
        assertTrue(start <= now)
        assertTrue(now - start < 24L * 60 * 60 * 1000)
        assertEquals("2023-11-14", SyncBuilders.dateKey(start, utc))
    }

    @Test
    fun `branch paths are well-formed firestore document paths`() {
        assertEquals("businesses/U/branches/A/state/summary", BranchPaths.summary("U", "A"))
        assertEquals("businesses/U/branches/A/state/stock-0", BranchPaths.stockChunk("U", "A", 0))
        assertEquals("businesses/U/branches/A/days/2026-06-11", BranchPaths.day("U", "A", "2026-06-11"))
    }
}
