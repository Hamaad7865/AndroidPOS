package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.SaleReturn
import com.nexapos.retail.data.entity.SaleReturnItem
import com.nexapos.retail.domain.repository.ReturnsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ReturnsRepository] for unit tests, seeded with returns + their items. */
class FakeReturnsRepository(
    seeded: List<Pair<SaleReturn, List<SaleReturnItem>>> = emptyList(),
) : ReturnsRepository {
    private val returns = MutableStateFlow(seeded.map { it.first })
    private val itemsByReturnId = seeded.associate { it.first.id to it.second }

    override fun observeRecent(): Flow<List<SaleReturn>> = returns

    override fun observeTotalSince(since: Long): Flow<Long> =
        MutableStateFlow(returns.value.filter { it.createdAt >= since }.sumOf { it.totalCents })

    override suspend fun count(): Int = returns.value.size

    override suspend fun itemsForReturn(returnId: Long): List<SaleReturnItem> = itemsByReturnId[returnId].orEmpty()

    override suspend fun returnedItemsForSale(saleId: Long): List<SaleReturnItem> = emptyList()

    override suspend fun recordReturn(
        saleReturn: SaleReturn,
        items: List<SaleReturnItem>,
    ): Long {
        returns.value = returns.value + saleReturn
        return returns.value.size.toLong()
    }
}
