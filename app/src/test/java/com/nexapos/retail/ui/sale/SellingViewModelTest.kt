package com.nexapos.retail.ui.sale

import com.nexapos.retail.MainDispatcherRule
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Shift
import com.nexapos.retail.data.entity.Staff
import com.nexapos.retail.data.entity.StaffRole
import com.nexapos.retail.data.security.StaffSession
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.fake.FakeCatalogRepository
import com.nexapos.retail.fake.FakeDrawerKicker
import com.nexapos.retail.fake.FakePartiesRepository
import com.nexapos.retail.fake.FakeSalesRepository
import com.nexapos.retail.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SellingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalog =
        FakeCatalogRepository(
            products =
                listOf(
                    Product(id = 1, name = "Wrench", sku = "WRN-T17", priceCents = 18_500, stockQty = 48, categoryId = 1, kind = "wrench"),
                    Product(id = 2, name = "Hammer", sku = "HMR-16", priceCents = 32_000, stockQty = 22, categoryId = 1, kind = "hammer"),
                ),
            categories = listOf(Category(id = 1, name = "Tools", sortOrder = 1)),
        )
    private val sales = FakeSalesRepository()
    private val parties = FakePartiesRepository()
    private val drawer = FakeDrawerKicker()
    private val shifts = FakeShiftRepository()
    private val session = StaffSession()

    // The dispatcher rule is unconfined, so the VM's init coroutines run eagerly on construction.
    private fun vm() = SellingViewModel(catalog, sales, parties, drawer, shifts, session)

    @Test
    fun `catalog is mapped to cents display products`() =
        runTest {
            val model = vm()
            assertEquals(2, model.products.size)
            val wrench = model.products.first { it.sku == "WRN-T17" }
            assertEquals(18_500, wrench.priceCents)
            assertEquals("Tools", wrench.cat)
            assertEquals(listOf("Tools"), model.categoryTree.map { it.name })
        }

    @Test
    fun `price override changes the line total and persisted unit price`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.setLinePrice(wrench.id, 200 * 100L)
            assertEquals(200 * 100L, model.subtotalCents)
            model.beginCheckout()
            model.complete()
            val (_, items) = sales.recorded.first()
            assertEquals(200 * 100L, items.first().unitPriceCents)
            assertEquals(200 * 100L, items.first().lineTotalCents)
        }

    @Test
    fun `sale note is persisted on the sale`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.saleNote = "Deliver tomorrow AM"
            model.beginCheckout()
            model.complete()
            val (sale, _) = sales.recorded.first()
            assertEquals("Deliver tomorrow AM", sale.note)
            // the note must reset after the sale so it can't leak onto the next ticket
            assertEquals("", model.saleNote)
        }

    @Test
    fun `totals use VAT-inclusive pricing — VAT is extracted not added`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.addToCart(wrench)
            val hammer = model.products.first { it.sku == "HMR-16" }
            model.addToCart(hammer)
            // subtotal = 185*2 + 320 = 690
            // vat (inclusive) = 690 - round(690 / 1.15) = 690 - round(600.0) = 690 - 600 = 90
            // total = subtotal - discount + shipping = 690 - 0 + 0 = 690 (VAT NOT added again)
            assertEquals(690 * 100L, model.subtotalCents)
            assertEquals(90 * 100L, model.vatCents)
            assertEquals(690 * 100L, model.totalCents)
        }

    @Test
    fun `complete persists the sale with correct cents values`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.addToCart(wrench)
            model.beginCheckout()
            model.complete()

            assertEquals(1, sales.recorded.size)
            val (sale, items) = sales.recorded.first()
            // subtotal = 185*2 = 370 rupees -> 37_000 cents (VAT-inclusive; total same as subtotal)
            assertEquals(37_000, sale.subtotalCents)
            // total cents = subtotal - discount + shipping = 370 rupees -> 37_000 cents
            assertEquals(37_000, sale.totalCents)
            assertEquals(1, items.size)
            assertEquals(2, items.first().quantity)
            assertNotNull(model.lastSale)
            assertTrue(model.workingLines.isEmpty())
            assertNull(model.saleError)
            // Stock decrement is handled atomically inside SaleDao.checkout() in production.
            // The FakeSalesRepository captures the stock delta via the checkout() call;
            // no separate catalogRepository.adjustStock is needed in tests.
        }

    @Test
    fun `complete is a no-op on an empty cart`() =
        runTest {
            val model = vm()
            model.complete()
            assertNull(model.lastSale)
            assertEquals(0, sales.recorded.size)
        }

    @Test
    fun `complete sets saleError when repository throws`() =
        runTest {
            val failingSales = FakeSalesRepository(failOnRecord = true)
            val model = SellingViewModel(catalog, failingSales, parties, drawer, shifts, session)
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout()
            model.complete()

            // lastSale is set optimistically before the coroutine runs.
            assertNotNull(model.lastSale)
            assertNotNull(model.saleError)
        }

    @Test
    fun `cash sale kicks the drawer exactly once after checkout succeeds`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout() // default payment method is cash
            model.complete()
            assertEquals(listOf(KickReason.CASH_SALE), drawer.kicks)
        }

    @Test
    fun `card sale never kicks the drawer`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout()
            model.setPaymentType("card")
            model.complete()
            assertEquals(1, sales.recorded.size)
            assertTrue(drawer.kicks.isEmpty())
        }

    @Test
    fun `failed checkout does not kick the drawer`() =
        runTest {
            val failingSales = FakeSalesRepository(failOnRecord = true)
            val localDrawer = FakeDrawerKicker()
            val model = SellingViewModel(catalog, failingSales, parties, localDrawer, shifts, session)
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout()
            model.complete()
            assertNotNull(model.saleError)
            assertTrue(localDrawer.kicks.isEmpty())
        }

    @Test
    fun `sale is stamped with the signed-in staff and open shift`() =
        runTest {
            val openShifts =
                FakeShiftRepository(
                    open = Shift(id = 42, staffId = 9, staffName = "Priya", openedAt = 0L, openingFloatCents = 100_000),
                )
            session.login(
                Staff(id = 9, name = "Priya", pinHash = "h", pinSalt = "s", role = StaffRole.CASHIER.name, createdAt = 0L),
            )
            val model = SellingViewModel(catalog, sales, parties, drawer, openShifts, session)
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout()
            model.complete()
            val (sale, _) = sales.recorded.first()
            assertEquals(9L, sale.staffId)
            assertEquals(42L, sale.shiftId)
        }

    @Test
    fun `sale with no open shift and no session keeps null stamps`() =
        runTest {
            session.logout()
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench)
            model.beginCheckout()
            model.complete()
            val (sale, _) = sales.recorded.first()
            assertNull(sale.staffId)
            assertNull(sale.shiftId)
        }

    @Test
    fun `underpaid cash sale does not complete`() =
        runTest {
            val model = vm()
            val wrench = model.products.first { it.sku == "WRN-T17" }
            model.addToCart(wrench) // total = Rs 185
            model.beginCheckout()
            model.receivedCents = 100 * 100L // Rs 100, deliberately less than Rs 185
            // canComplete must be false due to isFullyTendered being false
            assertTrue(!model.canComplete)
            model.complete()
            // No sale should be recorded
            assertEquals(0, sales.recorded.size)
        }
}
