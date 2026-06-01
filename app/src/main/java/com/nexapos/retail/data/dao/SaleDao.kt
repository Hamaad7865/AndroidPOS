package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertItems(items: List<SaleItem>)

    /**
     * Guarded stock decrement inside a checkout transaction.
     * Applies [delta] (negative for sale, positive for return) only when the result >= 0.
     * Returns 1 if the row was updated, 0 if stock would have gone negative (oversell guard).
     */
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id AND stockQty + :delta >= 0")
    suspend fun guardedStockAdjust(
        id: Long,
        delta: Int,
    ): Int

    /**
     * Bumps a party's credit balance by [deltaCents] inside a checkout transaction.
     * Positive values add to what the customer owes; negative values reduce it (payment).
     */
    @Query("UPDATE parties SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun bumpPartyBalance(
        id: Long,
        deltaCents: Long,
    )

    /**
     * Reads the highest numeric sequence already stored in receiptNo (format "S-NNNNN").
     * SUBSTR(receiptNo, 3) strips the "S-" prefix; CAST … AS INTEGER converts to a number.
     * Returns [startSeq] if the table is empty.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTR(receiptNo, 3) AS INTEGER)), :startSeq) FROM sales")
    suspend fun maxReceiptSeq(startSeq: Int): Int

    /**
     * Fully atomic checkout: derives the next invoice number from the DB, inserts the
     * sale row + line items, applies guarded stock decrements (rolls back on oversell),
     * and optionally bumps the credit customer's balance — all in one Room transaction.
     *
     * @param sale            Sale entity with receiptNo left blank (will be assigned here).
     * @param items           Line items with saleId = 0 (will be filled from the inserted sale id).
     * @param stockDeltas     Map of productId → delta (negative = decrement on sale).
     * @param creditCustomerId Party id to debit for a credit sale, or null.
     * @param creditDeltaCents Amount (cents) to add to the customer's balance for a credit sale.
     * @param invoiceStartSeq  Starting sequence for new installs (passed from the ViewModel constant).
     * @return The new sale's auto-generated id.
     */
    @Transaction
    suspend fun checkout(
        sale: Sale,
        items: List<SaleItem>,
        stockDeltas: Map<Long, Int>,
        creditCustomerId: Long?,
        creditDeltaCents: Long,
        invoiceStartSeq: Int,
    ): Long {
        val nextSeq = maxReceiptSeq(invoiceStartSeq) + 1
        val receiptNo = "S-%05d".format(nextSeq)
        val saleId = insertSale(sale.copy(receiptNo = receiptNo))
        insertItems(items.map { it.copy(saleId = saleId) })
        stockDeltas.forEach { (productId, delta) ->
            val rows = guardedStockAdjust(productId, delta)
            if (rows == 0) error("Oversell prevented: product $productId has insufficient stock")
        }
        if (creditCustomerId != null && creditDeltaCents > 0) {
            bumpPartyBalance(creditCustomerId, creditDeltaCents)
        }
        return saleId
    }

    /** Legacy helper kept for callers that only need sale + items (no stock/credit). */
    @Transaction
    suspend fun recordSale(
        sale: Sale,
        items: List<SaleItem>,
    ): Long {
        val saleId = insertSale(sale)
        insertItems(items.map { it.copy(saleId = saleId) })
        return saleId
    }

    @Query("SELECT * FROM sales ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id")
    suspend fun itemsForSale(saleId: Long): List<SaleItem>

    @Query(
        "SELECT * FROM sales WHERE customerId = :customerId AND status = 'COMPLETED' " +
            "ORDER BY createdAt DESC LIMIT :limit",
    )
    fun observeForCustomer(
        customerId: Long,
        limit: Int = 20,
    ): Flow<List<Sale>>

    @Query(
        "SELECT COALESCE(SUM(totalCents), 0) FROM sales " +
            "WHERE customerId = :customerId AND status = 'COMPLETED'",
    )
    fun observeLifetimeTotal(customerId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun count(): Int

    @Query(
        "SELECT COALESCE(SUM(totalCents), 0) FROM sales " +
            "WHERE status = 'COMPLETED' AND createdAt >= :since",
    )
    fun observeTotalSince(since: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM sales WHERE status = 'COMPLETED' AND createdAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}
