package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleReturnDao {
    @Insert
    suspend fun insertReturn(saleReturn: SaleReturn): Long

    @Insert
    suspend fun insertItems(items: List<SaleReturnItem>)

    /** Restocks a returned line. Positive [delta] adds the quantity back. */
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id")
    suspend fun restock(
        id: Long,
        delta: Int,
    )

    /** Reduces a credit customer's balance by a cash-equivalent refund. */
    @Query("UPDATE parties SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun bumpPartyBalance(
        id: Long,
        deltaCents: Long,
    )

    /**
     * Records a return + its items, restocks every returned line, and (for a
     * credit refund) reduces the customer's balance — all in ONE transaction, so
     * a crash mid-way can never leave stock or balances half-adjusted.
     *
     * @param stockDeltas      productId → quantity to add back to stock.
     * @param creditCustomerId customer to credit on a CREDIT refund, else null.
     * @param creditRefundCents amount to REDUCE the customer's balance by (>0).
     */
    @Transaction
    suspend fun recordReturn(
        saleReturn: SaleReturn,
        items: List<SaleReturnItem>,
        stockDeltas: Map<Long, Int>,
        creditCustomerId: Long?,
        creditRefundCents: Long,
    ): Long {
        val returnId = insertReturn(saleReturn)
        insertItems(items.map { it.copy(returnId = returnId) })
        stockDeltas.forEach { (productId, delta) -> restock(productId, delta) }
        if (creditCustomerId != null && creditRefundCents > 0) {
            bumpPartyBalance(creditCustomerId, -creditRefundCents)
        }
        return returnId
    }

    @Query("SELECT * FROM sale_returns ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SaleReturn>>

    @Query("SELECT COUNT(*) FROM sale_returns")
    suspend fun count(): Int

    @Query("SELECT * FROM sale_return_items WHERE returnId = :returnId ORDER BY id")
    suspend fun itemsForReturn(returnId: Long): List<SaleReturnItem>

    /** All return-item rows belonging to returns against [saleId] — used to cap re-returns. */
    @Query(
        "SELECT * FROM sale_return_items WHERE returnId IN " +
            "(SELECT id FROM sale_returns WHERE saleId = :saleId)",
    )
    suspend fun returnedItemsForSale(saleId: Long): List<SaleReturnItem>

    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM sale_returns WHERE createdAt >= :since")
    fun observeTotalSince(since: Long): Flow<Long>
}
