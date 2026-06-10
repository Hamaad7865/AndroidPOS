package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.VatType
import kotlin.math.roundToLong

private const val STANDARD_DIVISOR = 1.15

/**
 * Total VAT embedded in [lines]. Prices are VAT-inclusive, so Standard-rated lines
 * carry the 15% already inside their price; Exempt and Zero-rated carry none.
 * Returns 0 entirely when the business is not VAT-registered.
 */
fun vatOf(
    lines: List<PosLine>,
    vatRegistered: Boolean,
): Long {
    if (!vatRegistered) return 0L
    return lines.sumOf { line ->
        if (line.product.vatType == VatType.STANDARD) {
            line.lineTotalCents - (line.lineTotalCents / STANDARD_DIVISOR).roundToLong()
        } else {
            0L
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
    cartDiscountCents: Long,
    vatRegistered: Boolean,
): Long {
    if (!vatRegistered) return 0L
    val afterItems = lines.sumOf { it.netCents }
    if (afterItems <= 0L) return 0L
    val standardNet = lines.filter { it.product.vatType == VatType.STANDARD }.sumOf { it.netCents }
    val cartRatio = (afterItems - cartDiscountCents).coerceAtLeast(0L).toDouble() / afterItems
    val standardFinal = standardNet * cartRatio
    return (standardFinal - standardFinal / STANDARD_DIVISOR).roundToLong()
}
