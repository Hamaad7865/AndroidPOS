package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.PartyDao
import com.nexapos.retail.data.dao.ProductDao
import com.nexapos.retail.data.dao.SaleReturnDao
import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.domain.repository.ReturnsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed [ReturnsRepository]. The return + items insert is atomic; stock is
 * then raised per returned line, and a credit refund reduces the customer's
 * balance.
 */
class RoomReturnsRepository(
    private val saleReturnDao: SaleReturnDao,
    private val productDao: ProductDao,
    private val partyDao: PartyDao,
) : ReturnsRepository {
    override fun observeRecent(): Flow<List<SaleReturn>> = saleReturnDao.observeRecent()

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
        val returnId = saleReturnDao.recordReturn(saleReturn, items)
        // Restock every returned line that maps to a known product.
        items.forEach { item ->
            item.productId?.let { productId -> productDao.adjustStock(productId, item.quantity) }
        }
        // Credit refund → the customer owes less. Cash refund leaves the balance alone.
        if (saleReturn.refundMethod.equals(REFUND_CREDIT, ignoreCase = true) && saleReturn.customerId != null) {
            partyDao.adjustBalance(saleReturn.customerId, -saleReturn.totalCents)
        }
        return returnId
    }

    private companion object {
        const val REFUND_CREDIT = "CREDIT"
    }
}
