package com.nexapos.retail.ui.reports

import com.nexapos.retail.data.entity.SaleItem

/**
 * Per-product **net** revenue (minor units) for the line items of a single sale.
 *
 * Each line starts from its gross [SaleItem.lineTotalCents] minus its own
 * [SaleItem.discountCents] (the per-line discount). [saleDiscountCents] holds the
 * sale's *total* discount (cart-level + every line's), so the cart-level portion is
 * `saleDiscountCents − Σ line discounts`; it is spread across the lines in proportion
 * to each line's net value — mirroring how the till applies a cart discount to the
 * post-item-discount subtotal.
 *
 * Lines with no catalog product (`productId == null`) earn no product revenue but
 * still dilute the cart-discount denominator, so catalog products receive only their
 * fair share. Cart-discount shares use floor division; any sub-cent remainder is
 * dropped (immaterial to the whole-rupee report and never understates revenue).
 */
internal fun netRevenueByProduct(
    items: List<SaleItem>,
    saleDiscountCents: Long,
): Map<Long, Long> {
    val lineDiscountTotal = items.sumOf { it.discountCents }
    val cartDiscount = (saleDiscountCents - lineDiscountTotal).coerceAtLeast(0L)
    val netBase = items.sumOf { lineNetCents(it) }

    val byProduct = mutableMapOf<Long, Long>()
    items.forEach { item ->
        val pid = item.productId ?: return@forEach
        val net = lineNetCents(item)
        val cartShare = if (netBase > 0L) cartDiscount * net / netBase else 0L
        byProduct[pid] = (byProduct[pid] ?: 0L) + (net - cartShare).coerceAtLeast(0L)
    }
    return byProduct
}

/** A line's value after its own per-line discount, never negative. */
private fun lineNetCents(item: SaleItem): Long = (item.lineTotalCents - item.discountCents).coerceAtLeast(0L)
