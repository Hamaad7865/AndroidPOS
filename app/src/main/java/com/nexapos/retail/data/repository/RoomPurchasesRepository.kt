package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.PurchaseDao
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [PurchasesRepository]. Insert + received-stock changes happen in a
 * single transaction (see [PurchaseDao]); stock only moves when the purchase is
 * "received".
 */
class RoomPurchasesRepository(
    private val purchaseDao: PurchaseDao,
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
        // Raise stock only when received on creation; pending/cancelled records
        // the order without moving stock.
        val deltas =
            if (purchase.status.equals(STATUS_RECEIVED, ignoreCase = true)) {
                stockDeltas(items, direction = 1)
            } else {
                emptyMap()
            }
        return purchaseDao.recordPurchase(purchase, items, deltas)
    }

    override suspend fun updateStatus(
        purchaseId: Long,
        newStatus: String,
    ) {
        val existing = purchaseDao.getById(purchaseId) ?: return
        val before = existing.status.lowercase()
        val after = newStatus.lowercase()
        if (before == after) return

        // +qty when crossing into received, −qty when crossing out; otherwise no move.
        val direction =
            when {
                before != STATUS_RECEIVED && after == STATUS_RECEIVED -> +1
                before == STATUS_RECEIVED && after != STATUS_RECEIVED -> -1
                else -> 0
            }
        val deltas = if (direction != 0) stockDeltas(purchaseDao.itemsForPurchase(purchaseId), direction) else emptyMap()
        purchaseDao.applyStatusChange(purchaseId, after, deltas)
    }

    /** productId → signed quantity to move, summed across duplicate lines. */
    private fun stockDeltas(
        items: List<PurchaseItem>,
        direction: Int,
    ): Map<Long, Int> =
        items.mapNotNull { item -> item.productId?.let { id -> id to item.quantity * direction } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, qtys) -> qtys.sum() }

    private companion object {
        const val STATUS_RECEIVED = "received"
    }
}
