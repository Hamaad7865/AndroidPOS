package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nexapos.retail.data.entity.Brand
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Brand>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(brand: Brand): Long
}
