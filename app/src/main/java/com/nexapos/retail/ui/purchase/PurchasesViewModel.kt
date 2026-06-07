package com.nexapos.retail.ui.purchase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/** One line a cashier is about to purchase (whole-rupee unit cost). */
data class PurchaseDraftItem(
    val name: String,
    val quantity: Int,
    val unitCostRupees: Int,
)

class PurchasesViewModel(
    private val purchasesRepository: PurchasesRepository,
    private val catalogRepository: CatalogRepository,
    private val partiesRepository: PartiesRepository,
) : ViewModel() {
    var purchases by mutableStateOf<List<Purchase>>(emptyList())
        private set

    var monthTotalCents by mutableStateOf(0L)
        private set

    /** Active suppliers — drives the Supplier dropdown on the new-purchase form. */
    var suppliers by mutableStateOf<List<Party>>(emptyList())
        private set

    /** Product names — drives the item-picker dropdown on the new-purchase form. */
    var productNames by mutableStateOf<List<String>>(emptyList())
        private set

    private var products: List<Product> = emptyList()

    init {
        viewModelScope.launch { purchasesRepository.observeRecent().collect { purchases = it } }
        viewModelScope.launch { purchasesRepository.observeTotalSince(startOfMonth()).collect { monthTotalCents = it } }
        viewModelScope.launch {
            catalogRepository.observeAllProducts().collect {
                products = it
                productNames = it.map { p -> p.name }
            }
        }
        viewModelScope.launch { partiesRepository.observeSuppliers().collect { suppliers = it } }
    }

    val orderCount: Int get() = purchases.size
    val monthTotal: Int get() = (monthTotalCents / CENTS_PER_RUPEE).toInt()

    /** Looks up a supplier by name (case-insensitive). Returns null on miss. */
    fun findSupplier(name: String): Party? =
        suppliers.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    /** Suggests a unit cost for a product name (the catalog cost) — falls back to 0. */
    fun suggestedCost(productName: String): Int {
        val match = products.firstOrNull { it.name.equals(productName.trim(), ignoreCase = true) }
        return ((match?.costCents ?: 0L) / CENTS_PER_RUPEE).toInt()
    }

    /** Adds a new supplier on the fly and selects it. Returns the saved party. */
    suspend fun addSupplier(
        name: String,
        phone: String = "",
        locality: String = "",
    ): Party {
        val trimmed = name.trim()
        val id =
            partiesRepository.upsert(
                Party(
                    name = trimmed,
                    phone = phone.trim(),
                    locality = locality.trim(),
                    type = Party.TYPE_SUPPLIER,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        return Party(
            id = id,
            name = trimmed,
            phone = phone.trim(),
            locality = locality.trim(),
            type = Party.TYPE_SUPPLIER,
        )
    }

    /**
     * Records a purchase and raises stock for every line that matches a known
     * product by name **when [status] is "received"**. Pending POs persist the
     * order without changing stock. Returns immediately; persistence runs in the
     * background.
     */
    fun recordPurchase(
        supplierName: String,
        paymentMethod: String,
        items: List<PurchaseDraftItem>,
        status: String = "received",
        expectedDelivery: String = "",
        notes: String = "",
    ) {
        if (supplierName.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            recordPurchaseFromDraft(
                purchasesRepository,
                catalogRepository,
                partiesRepository,
                supplierName,
                paymentMethod,
                items,
                status,
                expectedDelivery,
                notes,
            )
        }
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
