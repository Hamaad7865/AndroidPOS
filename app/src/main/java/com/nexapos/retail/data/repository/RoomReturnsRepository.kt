package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.SaleReturnDao
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.domain.repository.ReturnsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [ReturnsRepository]. The return insert, restock and credit-balance
 * adjustment all run in a single [SaleReturnDao.recordReturn] transaction.
 */
class RoomReturnsRepository(
    private val saleReturnDao: SaleReturnDao,
) : ReturnsRepository {
    override fun observeRecent(limit: Int): Flow<List<SaleReturn>> = saleReturnDao.observeRecent(limit)

    override fun observeTotalSince(since: Long): Flow<Long> = saleReturnDao.observeTotalSince(since)

    override suspend fun count(): Int = saleReturnDao.count()

    override suspend fun itemsForReturn(returnId: Long): List<SaleReturnItem> =
        saleReturnDao.itemsForReturn(returnId)

    override suspend fun returnedItemsForSale(saleId: Long): List<SaleReturnItem> =
        saleReturnDao.returnedItemsForSale(saleId)

    override suspend fun recordReturn(
        saleReturn: SaleReturn,
        items: List<SaleReturnItem>,
    ): Long {
        // Total quantity to restock per product (handles duplicate lines).
        val stockDeltas =
            items.mapNotNull { item -> item.productId?.let { id -> id to item.quantity } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, qtys) -> qtys.sum() }
        // Credit refund → the customer owes less. Cash refund leaves the balance alone.
        val isCredit =
            saleReturn.refundMethod.equals(REFUND_CREDIT, ignoreCase = true) && saleReturn.customerId != null
        return saleReturnDao.recordReturn(
            saleReturn = saleReturn,
            items = items,
            stockDeltas = stockDeltas,
            creditCustomerId = if (isCredit) saleReturn.customerId else null,
            creditRefundCents = if (isCredit) saleReturn.totalCents else 0L,
        )
    }

    private companion object {
        const val REFUND_CREDIT = "CREDIT"
    }
}
