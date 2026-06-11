package com.nexapos.retail.ui.products

import com.nexapos.retail.MainDispatcherRule
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.domain.repository.ProductUsage
import com.nexapos.retail.fake.FakeCatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun product(id: Long) = Product(id = id, name = "Hammer", priceCents = 10_000)

    @Test
    fun `hard delete removes the product row`() =
        runTest {
            val repo = FakeCatalogRepository(products = listOf(product(1)))
            val vm = CatalogViewModel(repo)
            var done = false
            vm.deleteProduct(1, hard = true) { done = true }
            assertTrue(done)
            assertEquals(listOf(1L), repo.deletedIds)
            assertTrue(repo.archivedIds.isEmpty())
            assertEquals(null, repo.getProduct(1))
        }

    @Test
    fun `archive hides the product but keeps the row`() =
        runTest {
            val repo = FakeCatalogRepository(products = listOf(product(1)))
            val vm = CatalogViewModel(repo)
            var done = false
            vm.deleteProduct(1, hard = false) { done = true }
            assertTrue(done)
            assertEquals(listOf(1L), repo.archivedIds)
            assertTrue(repo.deletedIds.isEmpty())
            assertEquals(false, repo.getProduct(1)?.isActive)
        }

    @Test
    fun `usage reports linked sales and purchases`() =
        runTest {
            val repo = FakeCatalogRepository(usage = mapOf(1L to ProductUsage(sales = 3, purchases = 1)))
            val vm = CatalogViewModel(repo)
            val u = vm.productUsage(1)
            assertEquals(3, u.sales)
            assertEquals(1, u.purchases)
            assertTrue(u.isUsed)
        }
}
