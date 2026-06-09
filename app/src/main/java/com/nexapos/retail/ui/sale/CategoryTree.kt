@file:Suppress("MatchingDeclarationName") // groups MainCat with its tree-building helpers

package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Category

/** A main category together with its sub-categories. */
data class MainCat(
    val id: Long,
    val name: String,
    val subs: List<Category>,
)

/** Groups flat categories into mains (parentId == null), each carrying its subs, both sorted. */
fun buildCategoryTree(all: List<Category>): List<MainCat> {
    val subsByParent = all.filter { it.parentId != null }.groupBy { it.parentId }
    return all.filter { it.parentId == null }
        .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        .map { main ->
            MainCat(
                id = main.id,
                name = main.name,
                subs = (subsByParent[main.id] ?: emptyList()).sortedWith(compareBy({ it.sortOrder }, { it.name })),
            )
        }
}

/** The main-category id for a leaf category (itself when it is already a main). */
fun mainIdOf(leaf: Category): Long = leaf.parentId ?: leaf.id

/**
 * Drill-down filter. [main]/[sub] are the selected names (null = no filter at that level);
 * [productMain]/[productLeaf] describe the product's category.
 */
fun matchesCategory(
    productMain: String,
    productLeaf: String,
    main: String?,
    sub: String?,
): Boolean = main == null || (productMain == main && (sub == null || productLeaf == sub))

/** Product category label: "Main · Sub" when it has a distinct sub, else just the name. */
fun categoryLabel(
    mainCat: String,
    leaf: String,
): String = if (mainCat.isNotEmpty() && leaf != mainCat) "$mainCat · $leaf" else leaf
