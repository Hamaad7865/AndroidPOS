package com.nexapos.retail.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nexapos.retail.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND categoryId = :categoryId ORDER BY name")
    fun observeByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    /** Atomically adjusts stock by [delta] (negative to decrement on a sale). */
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id")
    suspend fun adjustStock(
        id: Long,
        delta: Int,
    )

    /**
     * Guarded stock adjustment: applies [delta] only when the result would be >= 0.
     * Returns 1 if the update succeeded, 0 if it would have caused oversell.
     * Use a negative [delta] when decrementing on a sale.
     */
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id AND stockQty + :delta >= 0")
    suspend fun adjustStockGuarded(
        id: Long,
        delta: Int,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
