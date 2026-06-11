package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.BrandDao
import com.nexapos.retail.data.dao.CategoryDao
import com.nexapos.retail.data.dao.ProductDao
import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.domain.repository.ProductUsage
import kotlinx.coroutines.flow.Flow

/** Room-backed [CatalogRepository]. */
class RoomCatalogRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
) : CatalogRepository {
    override fun observeProducts(): Flow<List<Product>> = productDao.observeActive()

    override fun observeAllProducts(): Flow<List<Product>> = productDao.observeAll()

    override fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    override fun observeBrands(): Flow<List<Brand>> = brandDao.observeAll()

    override suspend fun findByBarcode(barcode: String): Product? = productDao.findByBarcode(barcode)

    override suspend fun getProduct(id: Long): Product? = productDao.getById(id)

    override suspend fun upsert(product: Product): Long = productDao.upsert(product)

    override suspend fun upsertCategory(category: Category): Long = categoryDao.insert(category)

    override suspend fun upsertBrand(brand: Brand): Long = brandDao.insert(brand)

    override suspend fun adjustStock(
        productId: Long,
        delta: Int,
    ) = productDao.adjustStock(productId, delta)

    override suspend fun delete(product: Product) = productDao.delete(product)

    override suspend fun productUsage(productId: Long): ProductUsage =
        ProductUsage(
            sales = productDao.saleLineCount(productId),
            purchases = productDao.purchaseLineCount(productId),
        )

    override suspend fun archive(productId: Long) = productDao.setArchived(productId)
}
