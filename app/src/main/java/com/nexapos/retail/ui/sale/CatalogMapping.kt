package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Brand
import com.nexapos.retail.data.entity.Category
import com.nexapos.retail.data.entity.Product

/** Filter label shown first in every category chip row. */
const val ALL_CATEGORY = "All"

private const val CENTS_PER_RUPEE = 100L
private const val UNCATEGORISED = "Other"

/** Maps Room [Product]s to the lightweight display model used across the UI. */
fun List<Product>.toPosProducts(
    categories: List<Category>,
    brands: List<Brand> = emptyList(),
): List<PosProduct> {
    val catById = categories.associate { it.id to it.name }
    val brandById = brands.associate { it.id to it.name }
    return map { product ->
        PosProduct(
            id = product.id.toString(),
            name = product.name,
            cat = catById[product.categoryId] ?: UNCATEGORISED,
            price = (product.priceCents / CENTS_PER_RUPEE).toInt(),
            sku = product.sku,
            stock = product.stockQty,
            kind = product.kind,
            barcode = product.barcode,
            cost = (product.costCents / CENTS_PER_RUPEE).toInt(),
            brand = brandById[product.brandId].orEmpty(),
            model = product.model,
            unit = product.unit,
            rack = product.rack,
            shelf = product.shelf,
            lowStockThreshold = product.lowStockThreshold,
            taxRatePercent = product.taxRatePercent,
            taxInclusive = product.taxInclusive,
            imagePath = product.imagePath,
        )
    }
}

/** Category chip labels, always starting with [ALL_CATEGORY]. */
fun List<Category>.toFilterLabels(): List<String> = listOf(ALL_CATEGORY) + map { it.name }
