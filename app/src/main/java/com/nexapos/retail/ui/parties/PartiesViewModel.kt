package com.nexapos.retail.ui.parties

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PartiesViewModel(
    private val partiesRepository: PartiesRepository,
    private val salesRepository: SalesRepository,
) : ViewModel() {
    var customers by mutableStateOf<List<Party>>(emptyList())
        private set

    var suppliers by mutableStateOf<List<Party>>(emptyList())
        private set

    /** Lifetime total spent (whole rupees) by the currently selected customer. */
    var selectedLifetimeRupees by mutableStateOf(0)
        private set

    /** Most recent sales for the currently selected customer (newest first). */
    var selectedRecentSales by mutableStateOf<List<Sale>>(emptyList())
        private set

    /** Sale clicked from the Recent list — drives the read-only details dialog. */
    var detailSale by mutableStateOf<Sale?>(null)
        private set

    /** Items for [detailSale], loaded on demand. */
    var detailItems by mutableStateOf<List<com.nexapos.retail.data.entity.SaleItem>>(emptyList())
        private set

    private var selectedCustomerJob: Job? = null

    init {
        viewModelScope.launch { partiesRepository.observeCustomers().collect { customers = it } }
        viewModelScope.launch { partiesRepository.observeSuppliers().collect { suppliers = it } }
    }

    val customerCount: Int get() = customers.size
    val supplierCount: Int get() = suppliers.size

    /** Total outstanding balance across customers, in whole rupees. */
    val customerDue: Int get() = (customers.sumOf { it.balanceCents } / CENTS_PER_RUPEE).toInt()

    /**
     * Switches which party's history is being observed. Cancels the previous
     * observation so we never have two flows feeding the same state. Pass null
     * (or a non-customer) to clear.
     */
    fun selectParty(party: Party?) {
        selectedCustomerJob?.cancel()
        selectedLifetimeRupees = 0
        selectedRecentSales = emptyList()
        if (party == null || party.type != Party.TYPE_CUSTOMER) return
        selectedCustomerJob =
            viewModelScope.launch {
                launch {
                    salesRepository.observeLifetimeTotal(party.id).collect { cents ->
                        selectedLifetimeRupees = (cents / CENTS_PER_RUPEE).toInt()
                    }
                }
                launch {
                    salesRepository.observeForCustomer(party.id).collect { selectedRecentSales = it }
                }
            }
    }

    /** Opens the read-only details dialog for [sale] and loads its line items. */
    fun openSaleDetails(sale: Sale) {
        detailSale = sale
        detailItems = emptyList()
        viewModelScope.launch {
            detailItems = salesRepository.itemsForSale(sale.id)
        }
    }

    fun closeSaleDetails() {
        detailSale = null
        detailItems = emptyList()
    }

    fun addParty(
        name: String,
        phone: String,
        locality: String,
        isSupplier: Boolean,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            partiesRepository.upsert(
                Party(
                    name = name.trim(),
                    phone = phone.trim(),
                    locality = locality.trim(),
                    type = if (isSupplier) Party.TYPE_SUPPLIER else Party.TYPE_CUSTOMER,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
