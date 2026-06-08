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
