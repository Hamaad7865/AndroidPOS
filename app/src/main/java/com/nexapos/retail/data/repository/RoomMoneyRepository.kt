package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.MoneyTxnDao
import com.nexapos.retail.data.entity.MoneyTxn
import com.nexapos.retail.domain.repository.MoneyRepository
import kotlinx.coroutines.flow.Flow

/** Room-backed [MoneyRepository]. */
class RoomMoneyRepository(private val moneyTxnDao: MoneyTxnDao) : MoneyRepository {
    override fun observeIncome(): Flow<List<MoneyTxn>> = moneyTxnDao.observeByType(MoneyTxn.TYPE_INCOME)

    override fun observeExpenses(): Flow<List<MoneyTxn>> = moneyTxnDao.observeByType(MoneyTxn.TYPE_EXPENSE)

    override fun observeRecent(limit: Int): Flow<List<MoneyTxn>> = moneyTxnDao.observeRecent(limit)

    override fun observeSumSince(
        type: String,
        since: Long,
    ): Flow<Long> = moneyTxnDao.observeSumSince(type, since)

    override suspend fun add(txn: MoneyTxn): Long = moneyTxnDao.insert(txn)
}
