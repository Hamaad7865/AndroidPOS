package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.ProductUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [CatalogRepository] for unit tests. */
class FakeCatalogRepository(
    products: List<Product> = emptyList(),
    categories: List<Category> = emptyList(),
    brands: List<Brand> = emptyList(),
    private val usage: Map<Long, ProductUsage> = emptyMap(),
) : CatalogRepository {
    private val products = MutableStateFlow(products)
    private val categories = MutableStateFlow(categories)
    private val brands = MutableStateFlow(brands)

    /** Removal calls recorded for assertions. */
    val deletedIds = mutableListOf<Long>()
    val archivedIds = mutableListOf<Long>()

    override fun observeProducts(): Flow<List<Product>> = products

    override fun observeAllProducts(): Flow<List<Product>> = products

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observeBrands(): Flow<List<Brand>> = brands

    override suspend fun findByBarcode(barcode: String): Product? = products.value.firstOrNull { it.barcode == barcode }

    override suspend fun getProduct(id: Long): Product? = products.value.firstOrNull { it.id == id }

    override suspend fun upsert(product: Product): Long {
        products.value = products.value.filterNot { it.id == product.id } + product
        return product.id
    }

    override suspend fun upsertCategory(category: Category): Long {
        categories.value = categories.value.filterNot { it.id == category.id } + category
        return category.id
    }

    override suspend fun upsertBrand(brand: Brand): Long {
        brands.value = brands.value.filterNot { it.id == brand.id } + brand
        return brand.id
    }

    override suspend fun adjustStock(
        productId: Long,
        delta: Int,
    ) {
        products.value =
            products.value.map { if (it.id == productId) it.copy(stockQty = it.stockQty + delta) else it }
    }

    override suspend fun delete(product: Product) {
        deletedIds += product.id
        products.value = products.value.filterNot { it.id == product.id }
    }

    override suspend fun productUsage(productId: Long): ProductUsage = usage[productId] ?: ProductUsage(0, 0)

    override suspend fun archive(productId: Long) {
        archivedIds += productId
        products.value = products.value.map { if (it.id == productId) it.copy(isActive = false) else it }
    }
}
