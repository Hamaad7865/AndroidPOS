package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.MoneyTxn
import kotlinx.coroutines.flow.Flow

/** Read/write access to manual cash-book entries (income & expenses). */
interface MoneyRepository {
    fun observeIncome(): Flow<List<MoneyTxn>>

    fun observeExpenses(): Flow<List<MoneyTxn>>

    fun observeRecent(): Flow<List<MoneyTxn>>

    fun observeSumSince(
        type: String,
        since: Long,
    ): Flow<Long>

    suspend fun add(txn: MoneyTxn): Long
}
