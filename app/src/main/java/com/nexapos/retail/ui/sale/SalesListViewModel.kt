package com.nexapos.retail.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.launch

/**
 * Backs the Sales-list screen. Exposes the most recent completed sales plus an
 * on-demand fetch of line items for the "details" dialog.
 */
class SalesListViewModel(
    private val salesRepository: SalesRepository,
) : ViewModel() {
    var sales by mutableStateOf<List<Sale>>(emptyList())
        private set

    var detailItems by mutableStateOf<List<SaleItem>>(emptyList())
        private set

    var detailSale by mutableStateOf<Sale?>(null)
        private set

    init {
        viewModelScope.launch {
            salesRepository.observeRecent().collect { sales = it }
        }
    }

    /** Loads the line items for [sale] into [detailItems] and opens the details dialog. */
    fun openDetails(sale: Sale) {
        detailSale = sale
        detailItems = emptyList()
        viewModelScope.launch {
            detailItems = salesRepository.itemsForSale(sale.id)
        }
    }

    fun closeDetails() {
        detailSale = null
        detailItems = emptyList()
    }

    val totalRupees: Int get() = (sales.sumOf { it.totalCents } / CENTS_PER_RUPEE).toInt()

    val itemsCount: Int get() = sales.size

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
