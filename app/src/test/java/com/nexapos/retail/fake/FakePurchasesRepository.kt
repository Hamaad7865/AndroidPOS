package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.Purchase
import com.nexapos.retail.data.entity.PurchaseItem
import com.nexapos.retail.domain.repository.PurchasesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [PurchasesRepository] for unit tests, seeded with purchases + their items. */
class FakePurchasesRepository(
    seeded: List<Pair<Purchase, List<PurchaseItem>>> = emptyList(),
) : PurchasesRepository {
    private val purchases = MutableStateFlow(seeded.map { it.first })
    private val itemsByPurchaseId = seeded.associate { it.first.id to it.second }

    override fun observeRecent(): Flow<List<Purchase>> = purchases

    override fun observeTotalSince(since: Long): Flow<Long> =
        MutableStateFlow(purchases.value.filter { it.createdAt >= since }.sumOf { it.totalCents })

    override suspend fun getPurchase(id: Long): Purchase? = purchases.value.firstOrNull { it.id == id }

    override suspend fun itemsForPurchase(purchaseId: Long): List<PurchaseItem> = itemsByPurchaseId[purchaseId].orEmpty()

    override suspend fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
    ): Long {
        purchases.value = purchases.value + purchase
        return purchases.value.size.toLong()
    }

    override suspend fun updateStatus(
        purchaseId: Long,
        newStatus: String,
    ) {
        purchases.value = purchases.value.map { if (it.id == purchaseId) it.copy(status = newStatus) else it }
    }
}
