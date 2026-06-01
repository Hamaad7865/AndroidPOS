package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nexapos.retail.data.entity.Party
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE type = :type AND isActive = 1 ORDER BY name")
    fun observeByType(type: String): Flow<List<Party>>

    @Query("SELECT COUNT(*) FROM parties WHERE type = :type AND isActive = 1")
    fun observeCountByType(type: String): Flow<Int>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Party?

    /** Atomically adjusts a party's balance (positive = they owe more). */
    @Query("UPDATE parties SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun adjustBalance(
        id: Long,
        deltaCents: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(party: Party): Long

    @Delete
    suspend fun delete(party: Party)
}
