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

    /** Records a purchase and its line items atomically; returns the new purchase id. */
    @Transaction
    suspend fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
    ): Long {
        val purchaseId = insertPurchase(purchase)
        insertItems(items.map { it.copy(purchaseId = purchaseId) })
        return purchaseId
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
