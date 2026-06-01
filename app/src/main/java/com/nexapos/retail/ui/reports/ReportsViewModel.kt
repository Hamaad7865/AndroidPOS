package com.nexapos.retail.ui.reports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.MoneyRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import com.nexapos.retail.domain.repository.ReturnsRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val CENTS_PER_RUPEE = 100L

/** A unified day-book row — one of the four transaction kinds, with an amount in rupees. */
sealed class DayBookEntry {
    abstract val createdAt: Long
    abstract val rupees: Int
    abstract val ref: String
    abstract val description: String

    data class SaleEntry(val sale: Sale) : DayBookEntry() {
        override val createdAt get() = sale.createdAt
        override val rupees get() = (sale.totalCents / CENTS_PER_RUPEE).toInt()
        override val ref get() = sale.receiptNo
        override val description get() = "Sale · ${sale.paymentMethod.lowercase()}"
    }

    data class IncomeEntry(val txn: MoneyTxn) : DayBookEntry() {
        override val createdAt get() = txn.createdAt
        override val rupees get() = (txn.amountCents / CENTS_PER_RUPEE).toInt()
        override val ref get() = txn.code.ifEmpty { "I-${txn.id}" }
        override val description get() = (txn.description.ifBlank { txn.category }).ifBlank { "Income" }
    }

    data class ExpenseEntry(val txn: MoneyTxn) : DayBookEntry() {
        override val createdAt get() = txn.createdAt
        override val rupees get() = (txn.amountCents / CENTS_PER_RUPEE).toInt()
        override val ref get() = txn.code.ifEmpty { "E-${txn.id}" }
        override val description get() = (txn.description.ifBlank { txn.category }).ifBlank { "Expense" }
    }

    data class PurchaseEntry(val purchase: Purchase) : DayBookEntry() {
        override val createdAt get() = purchase.createdAt
        override val rupees get() = (purchase.totalCents / CENTS_PER_RUPEE).toInt()
        override val ref get() = purchase.code
        override val description get() = "Purchase · ${purchase.supplierName}"
    }

    data class ReturnEntry(val ret: SaleReturn) : DayBookEntry() {
        override val createdAt get() = ret.createdAt
        override val rupees get() = (ret.totalCents / CENTS_PER_RUPEE).toInt()
        override val ref get() = ret.code
        override val description get() = "Return · ${ret.receiptNo}"
    }
}

/** Aggregated sales/cost stats for one product. */
data class ProductActivity(
    val productId: Long,
    val name: String,
    val sku: String,
    /** Total quantity sold across all sales. */
    val qtySold: Int,
    /** Total revenue from those sales (whole rupees). */
    val revenueRupees: Int,
    /** Total quantity purchased across all purchases. */
    val qtyPurchased: Int,
    /** Total cost of those purchases (whole rupees). */
    val purchaseCostRupees: Int,
)

/**
 * Backs the full Reports suite. Loads every data source once and exposes a
 * collection of getters / aggregations for each individual report screen.
 */
class ReportsViewModel(
    salesRepository: SalesRepository,
    moneyRepository: MoneyRepository,
    purchasesRepository: PurchasesRepository,
    private val catalogRepository: CatalogRepository,
    partiesRepository: PartiesRepository,
    returnsRepository: ReturnsRepository,
) : ViewModel() {
    // --- Raw data feeds -------------------------------------------------

    var sales by mutableStateOf<List<Sale>>(emptyList())
        private set

    var purchases by mutableStateOf<List<Purchase>>(emptyList())
        private set

    var money by mutableStateOf<List<MoneyTxn>>(emptyList())
        private set

    var returns by mutableStateOf<List<SaleReturn>>(emptyList())
        private set

    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    var customers by mutableStateOf<List<Party>>(emptyList())
        private set

    /** The merged Day Book — sales + money + purchases sorted newest first. */
    var entries by mutableStateOf<List<DayBookEntry>>(emptyList())
        private set

    /** Sale items keyed by saleId, loaded lazily for COGS-dependent reports. */
    var saleItemsBySaleId by mutableStateOf<Map<Long, List<SaleItem>>>(emptyMap())
        private set

    /** Purchase items keyed by purchaseId, loaded lazily for product histories. */
    var purchaseItemsByPurchaseId by mutableStateOf<Map<Long, List<PurchaseItem>>>(emptyMap())
        private set

    private val salesRepo = salesRepository
    private val purchasesRepo = purchasesRepository

    init {
        viewModelScope.launch { salesRepository.observeRecent().collect { sales = it } }
        viewModelScope.launch { purchasesRepository.observeRecent().collect { purchases = it } }
        viewModelScope.launch { moneyRepository.observeRecent().collect { money = it } }
        viewModelScope.launch { catalogRepository.observeAllProducts().collect { products = it } }
        viewModelScope.launch { partiesRepository.observeCustomers().collect { customers = it } }
        viewModelScope.launch { returnsRepository.observeRecent().collect { returns = it } }

        // Day Book stream — combine all four sources.
        viewModelScope.launch {
            combine(
                salesRepository.observeRecent(),
                moneyRepository.observeRecent(),
                purchasesRepository.observeRecent(),
                returnsRepository.observeRecent(),
            ) { s, m, p, r ->
                val merged = mutableListOf<DayBookEntry>()
                s.forEach { merged += DayBookEntry.SaleEntry(it) }
                m.forEach { t ->
                    when (t.type) {
                        MoneyTxn.TYPE_INCOME -> merged += DayBookEntry.IncomeEntry(t)
                        MoneyTxn.TYPE_EXPENSE -> merged += DayBookEntry.ExpenseEntry(t)
                    }
                }
                p.forEach { merged += DayBookEntry.PurchaseEntry(it) }
                r.forEach { merged += DayBookEntry.ReturnEntry(it) }
                merged.sortedByDescending { it.createdAt }
            }.collect { entries = it }
        }
    }

    fun returnsInRange(
        from: Long,
        to: Long,
    ): List<SaleReturn> = returns.filter { it.createdAt in from until to }

    /**
     * Loads the sale items for every visible sale on-demand. Reports that need
     * line-level data (Bill-wise profit, Product sale history, COGS for P&L)
     * call this before reading [saleItemsBySaleId].
     */
    fun ensureSaleItemsLoaded() {
        val needed = sales.map { it.id } - saleItemsBySaleId.keys
        if (needed.isEmpty()) return
        viewModelScope.launch {
            val merged = saleItemsBySaleId.toMutableMap()
            needed.forEach { id -> merged[id] = salesRepo.itemsForSale(id) }
            saleItemsBySaleId = merged
        }
    }

    /** Same idea for purchase items. */
    fun ensurePurchaseItemsLoaded() {
        val needed = purchases.map { it.id } - purchaseItemsByPurchaseId.keys
        if (needed.isEmpty()) return
        viewModelScope.launch {
            val merged = purchaseItemsByPurchaseId.toMutableMap()
            needed.forEach { id -> merged[id] = purchasesRepo.itemsForPurchase(id) }
            purchaseItemsByPurchaseId = merged
        }
    }

    /** Sum of `net` flow over all entries (sales + income − expenses − purchases). */
    val netRupees: Int
        get() =
            entries.sumOf { e ->
                when (e) {
                    is DayBookEntry.SaleEntry, is DayBookEntry.IncomeEntry -> e.rupees
                    is DayBookEntry.ExpenseEntry, is DayBookEntry.PurchaseEntry, is DayBookEntry.ReturnEntry -> -e.rupees
                }
            }

    // --- Filter helpers -------------------------------------------------

    fun salesInRange(
        from: Long,
        to: Long,
    ): List<Sale> = sales.filter { it.createdAt in from until to }

    fun purchasesInRange(
        from: Long,
        to: Long,
    ): List<Purchase> = purchases.filter { it.createdAt in from until to }

    fun moneyInRange(
        from: Long,
        to: Long,
        type: String,
    ): List<MoneyTxn> = money.filter { it.type == type && it.createdAt in from until to }

    // --- Aggregations ---------------------------------------------------

    /** Customers with a positive balance (money owed to the shop). */
    val customersDue: List<Party> get() = customers.filter { it.balanceCents > 0 }.sortedByDescending { it.balanceCents }

    /** Income aggregated by category (rupees). */
    fun incomeByCategory(
        from: Long,
        to: Long,
    ): List<Pair<String, Int>> =
        moneyInRange(from, to, MoneyTxn.TYPE_INCOME)
            .groupBy { it.category.ifBlank { "Uncategorised" } }
            .map { (cat, list) -> cat to (list.sumOf { it.amountCents } / CENTS_PER_RUPEE).toInt() }
            .sortedByDescending { it.second }

    /** Computes per-product activity once items are loaded. */
    fun productActivity(): List<ProductActivity> {
        val costByProductId = products.associate { it.id to it.costCents }
        val sold = mutableMapOf<Long, Pair<Int, Long>>() // productId → (qty, revenueCents)
        saleItemsBySaleId.values.flatten().forEach { item ->
            val pid = item.productId ?: return@forEach
            val cur = sold[pid] ?: (0 to 0L)
            sold[pid] = (cur.first + item.quantity) to (cur.second + item.lineTotalCents)
        }
        val purchased = mutableMapOf<Long, Pair<Int, Long>>()
        purchaseItemsByPurchaseId.values.flatten().forEach { item ->
            val pid = item.productId ?: return@forEach
            val cur = purchased[pid] ?: (0 to 0L)
            purchased[pid] = (cur.first + item.quantity) to (cur.second + item.lineTotalCents)
        }
        return products.map { p ->
            val s = sold[p.id] ?: (0 to 0L)
            val q = purchased[p.id] ?: (0 to 0L)
            ProductActivity(
                productId = p.id,
                name = p.name,
                sku = p.sku,
                qtySold = s.first,
                revenueRupees = (s.second / CENTS_PER_RUPEE).toInt(),
                qtyPurchased = q.first,
                purchaseCostRupees = (q.second / CENTS_PER_RUPEE).toInt(),
            )
        }
    }

    /** Cost of goods sold for the given sales (uses each item's current product cost). */
    fun cogsRupees(salesSubset: List<Sale>): Int {
        val costByProductId = products.associate { it.id to it.costCents }
        val ids = salesSubset.map { it.id }.toSet()
        val items = saleItemsBySaleId.filterKeys { it in ids }.values.flatten()
        return (
            items.sumOf { item ->
                val unit = item.productId?.let { costByProductId[it] } ?: 0L
                unit * item.quantity
            } / CENTS_PER_RUPEE
        ).toInt()
    }
}
