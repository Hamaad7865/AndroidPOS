package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert
    suspend fun insertItems(items: List<PurchaseItem>)

    /** Raises (or lowers) a product's stock by [delta]. */
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id")
    suspend fun adjustStock(
        id: Long,
        delta: Int,
    )

    /**
     * Records a purchase + items and applies any received-stock increases in ONE
     * transaction, so a crash can't persist the PO with stock half-applied.
     * [stockDeltas] is empty for a pending/cancelled PO (records the order only).
     */
    @Transaction
    suspend fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
        stockDeltas: Map<Long, Int>,
    ): Long {
        val purchaseId = insertPurchase(purchase)
        insertItems(items.map { it.copy(purchaseId = purchaseId) })
        stockDeltas.forEach { (productId, delta) -> adjustStock(productId, delta) }
        return purchaseId
    }

    /**
     * Moves stock and flips the status in one transaction — so a status change
     * that crosses the "received" boundary can never leave stock and status out
     * of sync (which a re-run would then double-apply).
     */
    @Transaction
    suspend fun applyStatusChange(
        id: Long,
        status: String,
        stockDeltas: Map<Long, Int>,
    ) {
        stockDeltas.forEach { (productId, delta) -> adjustStock(productId, delta) }
        updateStatus(id, status)
    }

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Purchase?

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id")
    suspend fun itemsForPurchase(purchaseId: Long): List<PurchaseItem>

    @Query("UPDATE purchases SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: String,
    )

    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM purchases WHERE createdAt >= :since")
    fun observeTotalSince(since: Long): Flow<Long>
}
