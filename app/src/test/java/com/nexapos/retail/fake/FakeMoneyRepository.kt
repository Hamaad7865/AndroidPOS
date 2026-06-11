package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.domain.repository.MoneyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [MoneyRepository] for unit tests. */
class FakeMoneyRepository(initial: List<MoneyTxn> = emptyList()) : MoneyRepository {
    private val txns = MutableStateFlow(initial)

    override fun observeIncome(): Flow<List<MoneyTxn>> =
        txns.map { list -> list.filter { it.type == MoneyTxn.TYPE_INCOME } }

    override fun observeExpenses(): Flow<List<MoneyTxn>> =
        txns.map { list -> list.filter { it.type == MoneyTxn.TYPE_EXPENSE } }

    override fun observeRecent(limit: Int): Flow<List<MoneyTxn>> = txns

    override fun observeSumSince(
        type: String,
        since: Long,
    ): Flow<Long> =
        txns.map { list -> list.filter { it.type == type && it.createdAt >= since }.sumOf { it.amountCents } }

    override suspend fun add(txn: MoneyTxn): Long {
        txns.value = txns.value + txn
        return txns.value.size.toLong()
    }
}
