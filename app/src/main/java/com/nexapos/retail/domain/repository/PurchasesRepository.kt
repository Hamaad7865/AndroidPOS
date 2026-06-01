package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import kotlinx.coroutines.flow.Flow

/** Records and reports stock purchases. */
interface PurchasesRepository {
    fun observeRecent(): Flow<List<Purchase>>

    fun observeTotalSince(since: Long): Flow<Long>

    /** Loads a single purchase by id, or null if not found. */
    suspend fun getPurchase(id: Long): Purchase?

    /** Line items for a given purchase, ordered by id (insertion order). */
    suspend fun itemsForPurchase(purchaseId: Long): List<PurchaseItem>

    /**
     * Persists a purchase and its items. If [Purchase.status] is "received", stock
     * is raised for every catalog-matched line; otherwise no stock change occurs
     * yet (so a "pending" PO records the order without adjusting inventory).
     */
    suspend fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
    ): Long

    /**
     * Updates a purchase's status, adjusting stock when crossing the "received"
     * boundary: non-received → received adds stock for every line; received →
     * non-received reverses it. Other transitions are status-only.
     */
    suspend fun updateStatus(
        purchaseId: Long,
        newStatus: String,
    )
}
