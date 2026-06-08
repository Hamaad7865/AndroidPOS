package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.VatType
import kotlin.math.roundToInt

private const val STANDARD_DIVISOR = 1.15

/**
 * Total VAT embedded in [lines]. Prices are VAT-inclusive, so Standard-rated lines
 * carry the 15% already inside their price; Exempt and Zero-rated carry none.
 * Returns 0 entirely when the business is not VAT-registered.
 */
fun vatOf(
    lines: List<PosLine>,
    vatRegistered: Boolean,
): Int {
    if (!vatRegistered) return 0
    return lines.sumOf { line ->
        if (line.product.vatType == VatType.STANDARD) {
            line.lineTotal - (line.lineTotal / STANDARD_DIVISOR).roundToInt()
        } else {
            0
        }
    }
}

/**
 * VAT embedded in the cart AFTER discounts. Standard-rated lines carry the 15% inside
 * their (item-discounted) net; the cart discount is spread proportionally across that net.
 * Returns 0 when the business is not VAT-registered.
 */
fun discountedVat(
    lines: List<PosLine>,
    cartDiscount: Int,
    vatRegistered: Boolean,
): Int {
    if (!vatRegistered) return 0
    val afterItems = lines.sumOf { it.net }
    if (afterItems <= 0) return 0
    val standardNet = lines.filter { it.product.vatType == VatType.STANDARD }.sumOf { it.net }
    val cartRatio = (afterItems - cartDiscount).coerceAtLeast(0).toDouble() / afterItems
    val standardFinal = standardNet * cartRatio
    return (standardFinal - standardFinal / STANDARD_DIVISOR).roundToInt()
}
