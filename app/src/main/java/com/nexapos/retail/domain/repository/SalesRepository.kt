package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

/**
 * Records and reports completed sales. See ADR-005 in docs/ARCHITECTURE.md.
 */
interface SalesRepository {
    /** Most recent sales, newest first. [limit] caps the row count (default 50). */
    fun observeRecent(limit: Int = 50): Flow<List<Sale>>

    /** Sum of completed-sale totals (minor units) since [since] epoch millis. */
    fun observeTotalSince(since: Long): Flow<Long>

    /** Count of completed sales since [since] epoch millis. */
    fun observeCountSince(since: Long): Flow<Int>

    /** Total number of sales ever recorded (used to seed the invoice sequence). */
    suspend fun count(): Int

    /** Loads a single sale by id, or null. */
    suspend fun getSale(id: Long): Sale?

    /** Line items for a given sale, ordered by id (insertion order). */
    suspend fun itemsForSale(saleId: Long): List<SaleItem>

    /** Most-recent completed sales for a single customer, newest first. */
    fun observeForCustomer(customerId: Long): Flow<List<Sale>>

    /** Sum (minor units) of all completed sales for the given customer. */
    fun observeLifetimeTotal(customerId: Long): Flow<Long>

    /**
     * Persists a sale and its line items atomically and returns the new sale id.
     * Implementations must guarantee all-or-nothing semantics.
     */
    suspend fun recordSale(
        sale: Sale,
        items: List<SaleItem>,
    ): Long

    /**
     * Full checkout transaction: assigns the next invoice number from the DB, inserts the
     * sale + items, applies guarded stock decrements (throws on oversell), and bumps the
     * credit customer's balance — all atomically.
     *
     * @param sale             Sale entity; [Sale.receiptNo] will be replaced inside the txn.
     * @param items            Line items with saleId = 0 (filled automatically).
     * @param stockDeltas      Map of productId → delta (negative = decrement).
     * @param creditCustomerId Party id for credit sales, or null.
     * @param creditDeltaCents Cents to add to the customer's balance (0 for non-credit).
     * @param invoiceStartSeq  Fallback starting sequence when the sales table is empty.
     * @return The new sale's auto-generated id.
     */
    suspend fun checkout(
        sale: Sale,
        items: List<SaleItem>,
        stockDeltas: Map<Long, Int>,
        creditCustomerId: Long?,
        creditDeltaCents: Long,
        invoiceStartSeq: Int,
    ): Long
}
