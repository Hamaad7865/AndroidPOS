package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the product catalog. Presentation code depends on this
 * abstraction (not Room) so it can be faked in tests and re-implemented for a
 * future synced data source. See ADR-005 in docs/ARCHITECTURE.md.
 */
interface CatalogRepository {
    /** Active products, ordered by name. Emits again whenever the catalog changes. */
    fun observeProducts(): Flow<List<Product>>

    /** All products including inactive ones, for the management screen. */
    fun observeAllProducts(): Flow<List<Product>>

    /** Categories ordered by their configured sort order. */
    fun observeCategories(): Flow<List<Category>>

    /** Brands ordered by their configured sort order. */
    fun observeBrands(): Flow<List<Brand>>

    /** Finds a single active/inactive product by exact barcode, or null. */
    suspend fun findByBarcode(barcode: String): Product?

    /** Loads a single product by id, or null. */
    suspend fun getProduct(id: Long): Product?

    /** Inserts or replaces a product; returns its row id. */
    suspend fun upsert(product: Product): Long

    /** Inserts or replaces a category; returns its row id. */
    suspend fun upsertCategory(category: Category): Long

    /** Inserts or replaces a brand; returns its row id. */
    suspend fun upsertBrand(brand: Brand): Long

    /** Adjusts a product's stock by [delta] (negative decrements). */
    suspend fun adjustStock(
        productId: Long,
        delta: Int,
    )

    /** Permanently removes a product. */
    suspend fun delete(product: Product)

    /** Counts how many sales / purchases reference [productId] (for the delete warning). */
    suspend fun productUsage(productId: Long): ProductUsage

    /** Soft-deletes a product: hides it from the catalog/POS but keeps the row + history. */
    suspend fun archive(productId: Long)
}

/** How many historical records reference a product — drives the delete-vs-archive warning. */
data class ProductUsage(
    val sales: Int,
    val purchases: Int,
) {
    val isUsed: Boolean get() = sales > 0 || purchases > 0
}
