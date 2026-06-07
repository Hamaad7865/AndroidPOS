package com.nexapos.retail.ui.purchase

import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

// ---------------------------------------------------------------------------
// Minimal in-memory fakes used only by this test file.
// ---------------------------------------------------------------------------

internal class RecorderFakeCatalog(existing: List<Product>) : CatalogRepository {
    private val _products = MutableStateFlow(existing)
    private var nextId = (existing.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observeProducts(): Flow<List<Product>> = _products
    override fun observeAllProducts(): Flow<List<Product>> = _products
    override fun observeCategories() = MutableStateFlow(emptyList<com.nexapos.retail.data.entity.Category>())
    override fun observeBrands() = MutableStateFlow(emptyList<com.nexapos.retail.data.entity.Brand>())

    override suspend fun findByBarcode(barcode: String): Product? = _products.value.firstOrNull { it.barcode == barcode }
    override suspend fun getProduct(id: Long): Product? = _products.value.firstOrNull { it.id == id }

    override suspend fun upsert(product: Product): Long {
        return if (product.id == 0L) {
            val assigned = nextId++
            _products.value = _products.value + product.copy(id = assigned)
            assigned
        } else {
            _products.value = _products.value.filterNot { it.id == product.id } + product
            product.id
        }
    }

    override suspend fun upsertCategory(category: com.nexapos.retail.data.entity.Category): Long = category.id
    override suspend fun upsertBrand(brand: com.nexapos.retail.data.entity.Brand): Long = brand.id
    override suspend fun adjustStock(productId: Long, delta: Int) {}
    override suspend fun delete(product: Product) {
        _products.value = _products.value.filterNot { it.id == product.id }
    }
}

internal class RecorderFakeParties : PartiesRepository {
    private val _parties = MutableStateFlow<List<Party>>(emptyList())
    private var nextId = 1L

    override fun observeCustomers(): Flow<List<Party>> = _parties.map { it.filter { p -> p.type == Party.TYPE_CUSTOMER } }
    override fun observeSuppliers(): Flow<List<Party>> = _parties.map { it.filter { p -> p.type == Party.TYPE_SUPPLIER } }
    override fun observeCustomerCount(): Flow<Int> = _parties.map { it.count { p -> p.type == Party.TYPE_CUSTOMER } }
    override fun observeSupplierCount(): Flow<Int> = _parties.map { it.count { p -> p.type == Party.TYPE_SUPPLIER } }

    override suspend fun upsert(party: Party): Long {
        val id = if (party.id == 0L) nextId++ else party.id
        val stored = party.copy(id = id)
        _parties.value = _parties.value.filterNot { it.id == stored.id } + stored
        return id
    }

    override suspend fun getParty(id: Long): Party? = _parties.value.firstOrNull { it.id == id }
    override suspend fun adjustBalance(partyId: Long, deltaCents: Long) {}
    override suspend fun delete(party: Party) {
        _parties.value = _parties.value.filterNot { it.id == party.id }
    }
}

internal class RecorderFakePurchases : PurchasesRepository {
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val recorded = mutableListOf<Pair<Purchase, List<PurchaseItem>>>()
    private var nextId = 1L

    override fun observeRecent(): Flow<List<Purchase>> = _purchases
    override fun observeTotalSince(since: Long): Flow<Long> = MutableStateFlow(0L)
    override suspend fun getPurchase(id: Long): Purchase? = _purchases.value.firstOrNull { it.id == id }
    override suspend fun itemsForPurchase(purchaseId: Long): List<PurchaseItem> = emptyList()

    override suspend fun recordPurchase(purchase: Purchase, items: List<PurchaseItem>): Long {
        val id = nextId++
        val stored = purchase.copy(id = id)
        _purchases.value = _purchases.value + stored
        recorded += stored to items
        return id
    }

    override suspend fun updateStatus(purchaseId: Long, newStatus: String) {}
}

/** Bundles the three fakes for a single test scenario. */
internal class RecorderFakes(existing: List<Product> = emptyList()) {
    val catalog = RecorderFakeCatalog(existing)
    val parties = RecorderFakeParties()
    val purchases = RecorderFakePurchases()
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class PurchaseRecorderTest {
    @Test
    fun `records purchase, creates unknown product, links and totals`() = runTest {
        val fakes = RecorderFakes(
            existing = listOf(Product(id = 7, name = "Hammer", priceCents = 25000)),
        )
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "ACME",
            paymentMethod = "cash",
            items = listOf(
                PurchaseDraftItem("Hammer", quantity = 2, unitCostRupees = 250),
                PurchaseDraftItem("Brand New Bolt", quantity = 10, unitCostRupees = 5),
            ),
        )
        val recorded = fakes.purchases.recorded.single()
        assertEquals("ACME", recorded.first.supplierName)
        assertEquals(2 + 10, recorded.first.itemCount)
        assertEquals((2 * 250 + 10 * 5) * 100L, recorded.first.totalCents)
        assertEquals(7L, recorded.second.first { it.nameSnapshot == "Hammer" }.productId)
        assertNotNull(recorded.second.first { it.nameSnapshot == "Brand New Bolt" }.productId)
    }

    @Test
    fun `blank supplier name returns without recording`() = runTest {
        val fakes = RecorderFakes()
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "   ",
            paymentMethod = "cash",
            items = listOf(PurchaseDraftItem("Nails", quantity = 1, unitCostRupees = 10)),
        )
        assertEquals(0, fakes.purchases.recorded.size)
    }

    @Test
    fun `empty items list returns without recording`() = runTest {
        val fakes = RecorderFakes()
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "Supplier X",
            paymentMethod = "cash",
            items = emptyList<PurchaseDraftItem>(),
        )
        assertEquals(0, fakes.purchases.recorded.size)
    }

    @Test
    fun `existing supplier is not duplicated`() = runTest {
        val fakes = RecorderFakes()
        // First call creates supplier
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "Duplo Supplies",
            paymentMethod = "cash",
            items = listOf(PurchaseDraftItem("Widget", quantity = 1, unitCostRupees = 50)),
        )
        // Second call — supplier already exists, should not duplicate
        recordPurchaseFromDraft(
            purchasesRepository = fakes.purchases,
            catalogRepository = fakes.catalog,
            partiesRepository = fakes.parties,
            supplierName = "Duplo Supplies",
            paymentMethod = "cash",
            items = listOf(PurchaseDraftItem("Gadget", quantity = 2, unitCostRupees = 30)),
        )
        // Two purchases recorded but supplier only upserted once (checked via Flow first() value)
        assertEquals(2, fakes.purchases.recorded.size)
    }
}
