package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.SaleDao
import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.flow.Flow

/** Room-backed [SalesRepository]. Atomicity is provided by [SaleDao]. */
class RoomSalesRepository(private val saleDao: SaleDao) : SalesRepository {
    override fun observeRecent(limit: Int): Flow<List<Sale>> = saleDao.observeRecent(limit)

    override fun observeTotalSince(since: Long): Flow<Long> = saleDao.observeTotalSince(since)

    override fun observeCountSince(since: Long): Flow<Int> = saleDao.observeCountSince(since)

    override suspend fun count(): Int = saleDao.count()

    override suspend fun getSale(id: Long): Sale? = saleDao.getById(id)

    override suspend fun itemsForSale(saleId: Long): List<SaleItem> = saleDao.itemsForSale(saleId)

    override fun observeForCustomer(customerId: Long): Flow<List<Sale>> =
        saleDao.observeForCustomer(customerId)

    override fun observeLifetimeTotal(customerId: Long): Flow<Long> =
        saleDao.observeLifetimeTotal(customerId)

    override suspend fun recordSale(
        sale: Sale,
        items: List<SaleItem>,
    ): Long = saleDao.recordSale(sale, items)

    override suspend fun checkout(
        sale: Sale,
        items: List<SaleItem>,
        stockDeltas: Map<Long, Int>,
        creditCustomerId: Long?,
        creditDeltaCents: Long,
        invoiceStartSeq: Int,
    ): Long = saleDao.checkout(sale, items, stockDeltas, creditCustomerId, creditDeltaCents, invoiceStartSeq)
}
