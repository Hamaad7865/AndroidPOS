package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import kotlinx.coroutines.flow.Flow

/** Records and reports sales returns / refunds. */
interface ReturnsRepository {
    fun observeRecent(limit: Int = 50): Flow<List<SaleReturn>>

    fun observeTotalSince(since: Long): Flow<Long>

    /** Total returns ever recorded — used to seed the return code sequence. */
    suspend fun count(): Int

    suspend fun itemsForReturn(returnId: Long): List<SaleReturnItem>

    /** Items already returned against a given sale (to cap re-returns per line). */
    suspend fun returnedItemsForSale(saleId: Long): List<SaleReturnItem>

    /**
     * Persists a return and its items atomically, then **restocks** each returned
     * line. When [SaleReturn.refundMethod] is CREDIT and a customer is attached,
     * the customer's outstanding balance is reduced by the refund total (cash
     * refunds don't touch the balance). Returns the new return id.
     */
    suspend fun recordReturn(
        saleReturn: SaleReturn,
        items: List<SaleReturnItem>,
    ): Long
}
