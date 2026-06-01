package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.ProductDao
import com.nexapos.retail.data.dao.PurchaseDao
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [PurchasesRepository]. The purchase + items insert is atomic;
 * stock is then increased per line through [ProductDao.adjustStock] — but only
 * when the purchase is marked "received".
 */
class RoomPurchasesRepository(
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
) : PurchasesRepository {
    override fun observeRecent(): Flow<List<Purchase>> = purchaseDao.observeRecent()

    override fun observeTotalSince(since: Long): Flow<Long> = purchaseDao.observeTotalSince(since)

    override suspend fun getPurchase(id: Long): Purchase? = purchaseDao.getById(id)

    override suspend fun itemsForPurchase(purchaseId: Long): List<PurchaseItem> =
        purchaseDao.itemsForPurchase(purchaseId)

    override suspend fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
    ): Long {
        val purchaseId = purchaseDao.recordPurchase(purchase, items)
        // Only raise stock if the purchase is marked received on creation.
        // Pending/partial/cancelled PO records the order without changing stock.
        if (purchase.status.equals(STATUS_RECEIVED, ignoreCase = true)) {
            items.forEach { item ->
                item.productId?.let { productId -> productDao.adjustStock(productId, item.quantity) }
            }
        }
        return purchaseId
    }

    override suspend fun updateStatus(
        purchaseId: Long,
        newStatus: String,
    ) {
        val existing = purchaseDao.getById(purchaseId) ?: return
        val before = existing.status.lowercase()
        val after = newStatus.lowercase()
        if (before == after) return

        val wasReceived = before == STATUS_RECEIVED
        val isReceived = after == STATUS_RECEIVED

        // Stock-adjustment delta: +qty when crossing into received, −qty when
        // crossing out of received. Other transitions don't move stock.
        val direction =
            when {
                !wasReceived && isReceived -> +1
                wasReceived && !isReceived -> -1
                else -> 0
            }
        if (direction != 0) {
            val items = purchaseDao.itemsForPurchase(purchaseId)
            items.forEach { item ->
                item.productId?.let { productId ->
                    productDao.adjustStock(productId, item.quantity * direction)
                }
            }
        }
        purchaseDao.updateStatus(purchaseId, after)
    }

    private companion object {
        const val STATUS_RECEIVED = "received"
    }
}
