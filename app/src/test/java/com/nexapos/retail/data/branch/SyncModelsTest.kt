package com.nexapos.retail.data.branch

import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.data.entity.Shift
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the Firestore wire format: every DTO must round-trip through its map
 * unchanged, entity mappers must preserve cents exactly, and numeric reads must
 * tolerate Firestore handing integers back as Long or Double.
 */
class SyncModelsTest {
    @Test
    fun `sale dto round-trips with exact odd cents`() {
        val dto =
            SaleDto(
                receiptNo = "S-00042",
                createdAt = 1_700_000_000_000L,
                customerName = "Priya",
                paymentMethod = "CASH",
                subtotalCents = 12_345L,
                taxCents = 1_611L,
                discountCents = 99L,
                totalCents = 12_246L,
                tenderedCents = 15_000L,
                changeCents = 2_754L,
                status = "COMPLETED",
                lines = listOf(SaleLineDto("Hammer", 3, 4_115L, 12_345L, 0L)),
            )
        assertEquals(dto, SaleDto.fromMap(dto.toMap()))
    }

    @Test
    fun `sale dto from entity preserves every money field`() {
        val sale =
            Sale(
                id = 7,
                receiptNo = "S-1",
                createdAt = 1_000L,
                subtotalCents = 9_950L,
                taxCents = 1_298L,
                discountCents = 50L,
                totalCents = 9_900L,
                paymentMethod = "CARD",
                amountTenderedCents = 9_900L,
                changeCents = 0L,
                customerName = "Walk-in",
            )
        val items =
            listOf(
                SaleItem(id = 1, saleId = 7, productId = 2, nameSnapshot = "Wrench", unitPriceCents = 4_975L, quantity = 2, lineTotalCents = 9_950L, discountCents = 50L),
            )
        val dto = SaleDto.from(sale, items)
        assertEquals(9_950L, dto.subtotalCents)
        assertEquals(1_298L, dto.taxCents)
        assertEquals(9_900L, dto.totalCents)
        assertEquals(9_900L, dto.tenderedCents)
        assertEquals(1, dto.lines.size)
        assertEquals(4_975L, dto.lines.first().unitPriceCents)
        assertEquals(50L, dto.lines.first().discountCents)
        assertEquals(dto, SaleDto.fromMap(dto.toMap()))
    }

    @Test
    fun `stock item from product round-trips`() {
        val p = Product(id = 5, name = "Paint 5L", barcode = "2001000000104", sku = "PNT-5", priceCents = 52_500L, costCents = 32_000L, stockQty = 35, lowStockThreshold = 6)
        val dto = StockItemDto.from(p, "Paint")
        assertEquals(52_500L, dto.priceCents)
        assertEquals(35, dto.stockQty)
        assertEquals("Paint", dto.category)
        assertEquals(dto, StockItemDto.fromMap(dto.toMap()))
    }

    @Test
    fun `stock chunk round-trips a page of items`() {
        val chunk =
            StockChunk(
                (1..3).map { StockItemDto(it.toLong(), "P$it", "SKU$it", null, it * 100L, it, 5, "Cat") },
            )
        assertEquals(chunk, StockChunk.fromMap(chunk.toMap()))
    }

    @Test
    fun `money txn from entity and round-trip`() {
        val t = MoneyTxn(id = 1, type = MoneyTxn.TYPE_EXPENSE, category = "Rent", description = "June", amountCents = 1_250_50L, account = "Till 01", createdAt = 99L)
        val dto = MoneyTxnDto.from(t)
        assertEquals(125_050L, dto.amountCents)
        assertEquals(dto, MoneyTxnDto.fromMap(dto.toMap()))
    }

    @Test
    fun `return from entity and round-trip`() {
        val r = SaleReturn(id = 1, code = "RET-0001", saleId = 7, receiptNo = "S-1", createdAt = 5L, totalCents = 4_975L, refundMethod = "CASH")
        val items = listOf(SaleReturnItem(id = 1, returnId = 1, productId = 2, nameSnapshot = "Wrench", unitPriceCents = 4_975L, quantity = 1, lineTotalCents = 4_975L))
        val dto = ReturnDto.from(r, items)
        assertEquals(4_975L, dto.totalCents)
        assertEquals(4_975L, dto.lines.first().lineTotalCents)
        assertEquals(dto, ReturnDto.fromMap(dto.toMap()))
    }

    @Test
    fun `closed shift over-short is counted minus expected`() {
        val over = ClosedShiftDto.from(Shift(staffId = 1, staffName = "Owner", openedAt = 1L, closedAt = 2L, openingFloatCents = 100_000L, declaredCashCents = 150_50L, expectedCashCents = 150_00L))
        assertEquals(50L, over.overShortCents)
        val short = over.copy(declaredCashCents = 149_00L, expectedCashCents = 150_00L)
        assertEquals(-100L, short.overShortCents)
        assertEquals(over, ClosedShiftDto.fromMap(over.toMap()))
    }

    @Test
    fun `branch summary round-trips including a null open shift`() {
        val s = BranchSummary(salesTodayCents = 50_000L, ticketsToday = 4, stockValueCents = 9_272_275L, itemCount = 14, lowStockCount = 1, openShiftStaff = null, openShiftSince = null)
        assertEquals(s, BranchSummary.fromMap(s.toMap()))
        val open = s.copy(openShiftStaff = "Priya", openShiftSince = 123L)
        assertEquals(open, BranchSummary.fromMap(open.toMap()))
    }

    @Test
    fun `day doc round-trips with all nested lists`() {
        val day =
            DayDoc(
                date = "2026-06-11",
                sales = listOf(SaleDto("S-1", 1L, "Walk-in", "CASH", 1_000L, 130L, 0L, 1_000L, 1_000L, 0L, "COMPLETED", listOf(SaleLineDto("X", 1, 1_000L, 1_000L, 0L)))),
                returns = listOf(ReturnDto("R-1", "S-1", 2L, 500L, "CASH", listOf(ReturnLineDto("X", 1, 500L, 500L)))),
                money = listOf(MoneyTxnDto("INCOME", "Misc", "x", 250L, "Till", 3L)),
                shifts = listOf(ClosedShiftDto("Owner", 1L, 9L, 100_000L, 150_000L, 150_000L)),
            )
        assertEquals(day, DayDoc.fromMap(day.toMap()))
    }

    @Test
    fun `numeric reads tolerate firestore returning long or double`() {
        // Firestore returns whole numbers as Long, but a value written as a Double
        // (or via the console) can come back as Double — both must read as exact cents.
        val asLong = mapOf("priceCents" to 1_250L, "stockQty" to 3L, "productId" to 5L, "name" to "X", "sku" to "S", "barcode" to null, "lowStockThreshold" to 5L, "category" to "C")
        val asDouble = asLong + mapOf("priceCents" to 1_250.0, "stockQty" to 3.0)
        assertEquals(1_250L, StockItemDto.fromMap(asLong).priceCents)
        assertEquals(1_250L, StockItemDto.fromMap(asDouble).priceCents)
        assertEquals(3, StockItemDto.fromMap(asDouble).stockQty)
    }
}
