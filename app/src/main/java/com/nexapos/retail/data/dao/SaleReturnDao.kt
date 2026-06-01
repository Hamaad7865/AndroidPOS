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

    /** Records a return and its line items atomically; returns the new return id. */
    @Transaction
    suspend fun recordReturn(
        saleReturn: SaleReturn,
        items: List<SaleReturnItem>,
    ): Long {
        val returnId = insertReturn(saleReturn)
        insertItems(items.map { it.copy(returnId = returnId) })
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
