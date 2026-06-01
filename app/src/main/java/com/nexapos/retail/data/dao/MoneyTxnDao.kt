package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nexapos.retail.data.entity.MoneyTxn
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyTxnDao {
    @Query("SELECT * FROM money_txns WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<MoneyTxn>>

    @Query("SELECT * FROM money_txns ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<MoneyTxn>>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM money_txns " +
            "WHERE type = :type AND createdAt >= :since",
    )
    fun observeSumSince(
        type: String,
        since: Long,
    ): Flow<Long>

    @Insert
    suspend fun insert(txn: MoneyTxn): Long
}
