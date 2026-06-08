package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.Sale
import com.nexapos.retail.data.entity.SaleItem
import com.nexapos.retail.domain.repository.SalesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SalesRepository] for unit tests, with an optional forced failure. */
class FakeSalesRepository(
    var failOnRecord: Boolean = false,
    seeded: List<Pair<Sale, List<SaleItem>>> = emptyList(),
) : SalesRepository {
    val recorded = seeded.toMutableList()

    override fun observeRecent(): Flow<List<Sale>> = MutableStateFlow(recorded.map { it.first })

    override fun observeTotalSince(since: Long): Flow<Long> = MutableStateFlow(0L)

    override fun observeCountSince(since: Long): Flow<Int> = MutableStateFlow(0)

    override suspend fun count(): Int = recorded.size

    override suspend fun getSale(id: Long): Sale? = recorded.getOrNull((id - 1).toInt())?.first

    override suspend fun itemsForSale(saleId: Long): List<SaleItem> =
        recorded.firstOrNull { it.first.id == saleId }?.second
            ?: recorded.getOrNull((saleId - 1).toInt())?.second.orEmpty()

    override fun observeForCustomer(customerId: Long): Flow<List<Sale>> =
        MutableStateFlow(recorded.map { it.first }.filter { it.customerId == customerId })

    override fun observeLifetimeTotal(customerId: Long): Flow<Long> =
        MutableStateFlow(
            recorded.map { it.first }.filter { it.customerId == customerId }.sumOf { it.totalCents },
        )

    override suspend fun recordSale(
        sale: Sale,
        items: List<SaleItem>,
    ): Long {
        if (failOnRecord) error("Simulated database failure")
        recorded += sale to items
        return recorded.size.toLong()
    }

    override suspend fun checkout(
        sale: Sale,
        items: List<SaleItem>,
        stockDeltas: Map<Long, Int>,
        creditCustomerId: Long?,
        creditDeltaCents: Long,
        invoiceStartSeq: Int,
    ): Long {
        if (failOnRecord) error("Simulated database failure")
        val nextSeq = (recorded.size) + invoiceStartSeq + 1
        val receiptNo = "S-%05d".format(nextSeq)
        recorded += sale.copy(receiptNo = receiptNo) to items
        return recorded.size.toLong()
    }
}
