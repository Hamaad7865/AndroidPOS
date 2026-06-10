package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nexapos.retail.data.entity.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    /** Every staff member, admins first, then alphabetical — for the management screen. */
    @Query("SELECT * FROM staff ORDER BY active DESC, role ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Staff>>

    /** Staff who can currently sign in. */
    @Query("SELECT * FROM staff WHERE active = 1")
    suspend fun active(): List<Staff>

    /** Every staff member, active or not — for PIN-uniqueness checks. */
    @Query("SELECT * FROM staff")
    suspend fun all(): List<Staff>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun byId(id: Long): Staff?

    @Insert
    suspend fun insert(staff: Staff): Long

    @Update
    suspend fun update(staff: Staff)
}
