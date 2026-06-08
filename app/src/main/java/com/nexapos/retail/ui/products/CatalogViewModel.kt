package com.nexapos.retail.ui.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product
import com.nexapos.retail.domain.repository.CatalogRepository
import com.nexapos.retail.ui.sale.ALL_CATEGORY
import com.nexapos.retail.ui.sale.PosProduct
import com.nexapos.retail.ui.sale.toFilterLabels
import com.nexapos.retail.ui.sale.toPosProducts
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Summary shown after a CSV import finishes. */
data class ImportResult(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val messages: List<String>,
    val fatalError: String? = null,
) {
    val ok: Boolean get() = fatalError == null
}

/**
 * Backs the product-management screen. Exposes the full catalog (including
 * inactive products) as display models, the category + brand lists, and
 * persists new products.
 */
class CatalogViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {
    var products by mutableStateOf<List<PosProduct>>(emptyList())
        private set

    var categories by mutableStateOf(listOf(ALL_CATEGORY))
        private set

    var brands by mutableStateOf<List<String>>(emptyList())
        private set

    private var categoryEntities: List<Category> = emptyList()
    private var brandEntities: List<Brand> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.observeAllProducts(),
                catalogRepository.observeCategories(),
                catalogRepository.observeBrands(),
            ) { prods, cats, brs -> Triple(prods, cats, brs) }
                .collect { (prods, cats, brs) ->
                    categoryEntities = cats
                    brandEntities = brs
                    products = prods.toPosProducts(cats, brs)
                    categories = cats.toFilterLabels()
                    brands = brs.map { it.name }
                }
        }
    }

    /** Total retail value of stock on hand, in whole rupees. */
    val stockValue: Int get() = products.sumOf { it.price * it.stock }

    /** Loads a single product by id — used when opening a row to edit. */
    suspend fun loadProduct(id: Long): Product? = catalogRepository.getProduct(id)

    /**
     * Saves a product. When [id] is null this inserts a new row; when set it
     * updates the existing row in place (keeps the same DB id). [imagePath]
     * being null leaves the existing photo untouched on edits. New category
     * and brand names are auto-created the first time they're used.
     */
    @Suppress("LongParameterList")
    fun saveProduct(
        id: Long?,
        name: String,
        sku: String,
        barcode: String?,
        priceRupees: Int,
        costRupees: Int,
        categoryName: String,
        brandName: String,
        stock: Int,
        lowStockThreshold: Int,
        unit: String,
        model: String,
        rack: String,
        shelf: String,
        vatType: String,
        kind: String,
        imagePath: String?,
    ) {
        if (name.isBlank() || priceRupees <= 0) return
        val trimmedCat = categoryName.trim()
        val trimmedBrand = brandName.trim()
        viewModelScope.launch {
            val categoryId =
                resolveLookupId(
                    name = trimmedCat,
                    existing = categoryEntities.firstOrNull { it.name.equals(trimmedCat, ignoreCase = true) }?.id,
                ) { catalogRepository.upsertCategory(Category(name = trimmedCat)) }
            val brandId =
                resolveLookupId(
                    name = trimmedBrand,
                    existing = brandEntities.firstOrNull { it.name.equals(trimmedBrand, ignoreCase = true) }?.id,
                ) { catalogRepository.upsertBrand(Brand(name = trimmedBrand)) }
            val previous = id?.let { catalogRepository.getProduct(it) }
            val priceCents = priceRupees * CENTS_PER_RUPEE
            val costCents = costRupees * CENTS_PER_RUPEE
            val base = previous ?: Product(name = "", priceCents = 0)
            catalogRepository.upsert(
                base.copy(
                    id = id ?: 0,
                    name = name.trim(),
                    sku = sku.trim(),
                    barcode = barcode?.trim()?.takeIf { it.isNotEmpty() },
                    priceCents = priceCents,
                    costCents = costCents,
                    stockQty = stock,
                    lowStockThreshold = lowStockThreshold,
                    categoryId = categoryId,
                    brandId = brandId,
                    unit = unit.trim().ifEmpty { "pcs" },
                    model = model.trim(),
                    rack = rack.trim(),
                    shelf = shelf.trim(),
                    vatType = vatType,
                    taxRatePercent = com.nexapos.retail.data.entity.VatType.from(vatType).ratePercent,
                    taxInclusive = true,
                    kind = kind,
                    imagePath = imagePath ?: previous?.imagePath,
                ),
            )
        }
    }

    /**
     * Bulk-imports products from a CSV (see [parseProductsCsv]). Existing products
     * are matched by barcode → SKU → name and updated in place; the rest are
     * inserted. New categories are auto-created. Calls [onResult] on the main
     * thread when finished.
     */
    fun importProductsFromCsv(
        csvText: String,
        onResult: (ImportResult) -> Unit,
    ) {
        viewModelScope.launch {
            val parsed = parseProductsCsv(csvText)
            if (parsed.rows.isEmpty()) {
                onResult(ImportResult(0, 0, parsed.skipped, parsed.errors, parsed.fatalError ?: "Nothing to import."))
                return@launch
            }
            // Snapshot the catalog once; keep it updated as we go so duplicate rows
            // in the same file update rather than collide on the unique barcode index.
            val working = catalogRepository.observeAllProducts().first().toMutableList()
            // Lowercased category name → id, seeded with what already exists.
            val catIds = HashMap<String, Long>()
            categoryEntities.forEach { catIds[it.name.lowercase()] = it.id }

            var imported = 0
            var updated = 0
            for (r in parsed.rows) {
                val categoryId =
                    if (r.category.isBlank()) {
                        null
                    } else {
                        catIds[r.category.lowercase()] ?: run {
                            val newId = catalogRepository.upsertCategory(Category(name = r.category.trim()))
                            catIds[r.category.lowercase()] = newId
                            newId
                        }
                    }
                val bc = r.barcode?.trim()?.takeIf { it.isNotEmpty() }
                val match =
                    working.firstOrNull { bc != null && it.barcode == bc }
                        ?: working.firstOrNull { r.sku.isNotBlank() && it.sku.equals(r.sku.trim(), ignoreCase = true) }
                        ?: working.firstOrNull { r.sku.isBlank() && bc == null && it.name.equals(r.name.trim(), ignoreCase = true) }
                val base = match ?: Product(name = "", priceCents = 0)
                val toSave =
                    base.copy(
                        id = match?.id ?: 0,
                        name = r.name.trim(),
                        sku = r.sku.trim(),
                        barcode = bc,
                        priceCents = r.priceRupees * CENTS_PER_RUPEE,
                        costCents = r.costRupees * CENTS_PER_RUPEE,
                        stockQty = r.stock,
                        categoryId = categoryId ?: base.categoryId,
                        isActive = true,
                    )
                val savedId = catalogRepository.upsert(toSave)
                val saved = toSave.copy(id = savedId)
                if (match != null) {
                    updated++
                    val idx = working.indexOfFirst { it.id == match.id }
                    if (idx >= 0) working[idx] = saved else working += saved
                } else {
                    imported++
                    working += saved
                }
            }
            onResult(ImportResult(imported, updated, parsed.skipped, parsed.errors, null))
        }
    }

    private suspend fun resolveLookupId(
        name: String,
        existing: Long?,
        create: suspend () -> Long,
    ): Long? =
        when {
            existing != null -> existing
            name.isNotEmpty() -> create()
            else -> null
        }

    private companion object {
        const val CENTS_PER_RUPEE = 100L
    }
}
