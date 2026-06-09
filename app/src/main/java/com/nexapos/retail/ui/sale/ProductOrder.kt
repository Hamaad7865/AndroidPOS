package com.nexapos.retail.ui.sale

/**
 * Orders [products] by each product's position in [savedOrder] (ids not listed fall to the
 * end), breaking ties alphabetically by name. Empty [savedOrder] yields a pure A–Z order.
 */
fun orderProducts(
    products: List<PosProduct>,
    savedOrder: List<String>,
): List<PosProduct> {
    val rank = savedOrder.withIndex().associate { (i, id) -> id to i }
    return products.sortedWith(compareBy({ rank[it.id] ?: Int.MAX_VALUE }, { it.name.lowercase() }))
}
