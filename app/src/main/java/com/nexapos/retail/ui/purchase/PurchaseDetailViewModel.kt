package com.nexapos.retail.ui.purchase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.launch

/**
 * Backs the Purchase Detail screen. Loads a single [Purchase] + its line items
 * on demand. [purchase] is null while loading; [notFound] becomes true if the
 * id doesn't match anything (purchase was deleted, bad nav arg, etc.).
 */
class PurchaseDetailViewModel(
    private val purchasesRepository: PurchasesRepository,
) : ViewModel() {
    var purchase by mutableStateOf<Purchase?>(null)
        private set

    var items by mutableStateOf<List<PurchaseItem>>(emptyList())
        private set

    var loading by mutableStateOf(true)
        private set

    var notFound by mutableStateOf(false)
        private set

    /** Reloads the purchase + items for the given [purchaseId]. */
    fun load(purchaseId: Long) {
        loading = true
        notFound = false
        viewModelScope.launch {
            val p = purchasesRepository.getPurchase(purchaseId)
            if (p == null) {
                notFound = true
                loading = false
                return@launch
            }
            purchase = p
            items = purchasesRepository.itemsForPurchase(purchaseId)
            loading = false
        }
    }

    /**
     * Changes the purchase status. The repository handles the stock-adjustment
     * side-effects (raising stock when crossing into "received", reversing it
     * when crossing out). Reloads on completion so the UI reflects the change.
     */
    fun changeStatus(newStatus: String) {
        val current = purchase ?: return
        if (current.status.equals(newStatus, ignoreCase = true)) return
        viewModelScope.launch {
            purchasesRepository.updateStatus(current.id, newStatus)
            // Refresh the local copy + items (stock may have moved; the row is the
            // same but we want the new status to show immediately).
            val refreshed = purchasesRepository.getPurchase(current.id)
            if (refreshed != null) purchase = refreshed
        }
    }
}
