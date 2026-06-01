package com.nexapos.retail.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.PartiesRepository
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/** Supplies the dashboard's headline KPI numbers from live data. */
class DashboardViewModel(
    salesRepository: SalesRepository,
    catalogRepository: CatalogRepository,
    partiesRepository: PartiesRepository,
) : ViewModel() {
    var todaySalesCents by mutableStateOf(0L)
        private set

    var todayCount by mutableStateOf(0)
        private set

    var weekSalesCents by mutableStateOf(0L)
        private set

    var categoryCount by mutableStateOf(0)
        private set

    var supplierCount by mutableStateOf(0)
        private set

    /** Most recent completed sales — drives the Live activity card. */
    var recentSales by mutableStateOf<List<Sale>>(emptyList())
        private set

    // Derived properties are computed once when the products flow emits and stored in
    // Compose state so each composable snapshot reads a stable value, not a fresh
    // O(n) accumulation on every recompose.
    var stockValue: Int by mutableStateOf(0)
        private set

    var itemCount: Int by mutableStateOf(0)
        private set

    var lowStockCount: Int by mutableStateOf(0)
        private set

    var lowStockItems: List<Product> by mutableStateOf(emptyList())
        private set

    init {
        val dayStart = startOfDay()
        val weekStart = dayStart - SEVEN_DAYS_MS
        viewModelScope.launch { salesRepository.observeTotalSince(dayStart).collect { todaySalesCents = it } }
        viewModelScope.launch { salesRepository.observeCountSince(dayStart).collect { todayCount = it } }
        viewModelScope.launch { salesRepository.observeTotalSince(weekStart).collect { weekSalesCents = it } }
        viewModelScope.launch { salesRepository.observeRecent().collect { recentSales = it } }
        viewModelScope.launch {
            catalogRepository.observeAllProducts().collect { list ->
                // Recompute all derived values in one pass so they're updated
                // atomically; composables that read them get a single recompose.
                val low = list.filter { it.stockQty <= it.lowStockThreshold }
                itemCount = list.size
                lowStockCount = low.size
                lowStockItems = low
                stockValue =
                    list.sumOf { it.priceCents * it.stockQty }
                        .let { (it / CENTS_PER_RUPEE).toInt() }
            }
        }
        viewModelScope.launch { catalogRepository.observeCategories().collect { categoryCount = it.size } }
        viewModelScope.launch { partiesRepository.observeSupplierCount().collect { supplierCount = it } }
    }

    val todaySales: Int get() = (todaySalesCents / CENTS_PER_RUPEE).toInt()
    val salesWeek: Int get() = (weekSalesCents / CENTS_PER_RUPEE).toInt()

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
