package com.nexapos.retail.ui.reports

import com.nexapos.retail.MainDispatcherRule
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.fake.FakeCatalogRepository
import com.nexapos.retail.fake.FakeMoneyRepository
import com.nexapos.retail.fake.FakePartiesRepository
import com.nexapos.retail.fake.FakePurchasesRepository
import com.nexapos.retail.fake.FakeReturnsRepository
import com.nexapos.retail.fake.FakeSalesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val products =
        listOf(
            Product(id = 1, name = "Wrench", sku = "WRN", priceCents = 10_000),
            Product(id = 2, name = "Hammer", sku = "HMR", priceCents = 20_000),
        )

    // Unconfined dispatcher (see MainDispatcherRule) runs the VM's init + ensureSaleItemsLoaded
    // coroutines eagerly, so seeded data is visible synchronously after construction.
    private fun vm(sales: List<Pair<Sale, List<SaleItem>>>): ReportsViewModel =
        ReportsViewModel(
            salesRepository = FakeSalesRepository(seeded = sales),
            moneyRepository = FakeMoneyRepository(),
            purchasesRepository = FakePurchasesRepository(),
            catalogRepository = FakeCatalogRepository(products = products),
            partiesRepository = FakePartiesRepository(),
            returnsRepository = FakeReturnsRepository(),
        )

    @Test
    fun `productActivity reports revenue net of per-line and cart discounts`() =
        runTest {
            val sale =
                Sale(
                    id = 1,
                    receiptNo = "S-00001",
                    createdAt = 1_000L,
                    subtotalCents = 40_000,
                    // total discount = 2_000 per-line + 3_800 cart
                    discountCents = 5_800,
                    totalCents = 34_200,
                    paymentMethod = "CASH",
                )
            val items =
                listOf(
                    SaleItem(
                        saleId = 1,
                        productId = 1,
                        nameSnapshot = "Wrench",
                        unitPriceCents = 10_000,
                        quantity = 2,
                        lineTotalCents = 20_000,
                        discountCents = 2_000,
                    ),
                    SaleItem(
                        saleId = 1,
                        productId = 2,
                        nameSnapshot = "Hammer",
                        unitPriceCents = 20_000,
                        quantity = 1,
                        lineTotalCents = 20_000,
                        discountCents = 0,
                    ),
                )
            val model = vm(listOf(sale to items))
            model.ensureSaleItemsLoaded()

            val activity = model.productActivity().associateBy { it.productId }
            // Wrench: net 18_000 − cart share 1_800 = 16_200c = Rs 162 (gross would be Rs 200).
            assertEquals(162, activity.getValue(1).revenueRupees)
            assertEquals(2, activity.getValue(1).qtySold)
            // Hammer: net 20_000 − cart share 2_000 = 18_000c = Rs 180 (gross would be Rs 200).
            assertEquals(180, activity.getValue(2).revenueRupees)
            assertEquals(1, activity.getValue(2).qtySold)
        }

    @Test
    fun `productActivity revenue equals gross when there are no discounts`() =
        runTest {
            val sale =
                Sale(
                    id = 1,
                    receiptNo = "S-00001",
                    createdAt = 1_000L,
                    subtotalCents = 30_000,
                    discountCents = 0,
                    totalCents = 30_000,
                    paymentMethod = "CASH",
                )
            val items =
                listOf(
                    SaleItem(
                        saleId = 1,
                        productId = 1,
                        nameSnapshot = "Wrench",
                        unitPriceCents = 10_000,
                        quantity = 1,
                        lineTotalCents = 10_000,
                    ),
                    SaleItem(
                        saleId = 1,
                        productId = 2,
                        nameSnapshot = "Hammer",
                        unitPriceCents = 20_000,
                        quantity = 1,
                        lineTotalCents = 20_000,
                    ),
                )
            val model = vm(listOf(sale to items))
            model.ensureSaleItemsLoaded()

            val activity = model.productActivity().associateBy { it.productId }
            assertEquals(100, activity.getValue(1).revenueRupees)
            assertEquals(200, activity.getValue(2).revenueRupees)
        }
}
