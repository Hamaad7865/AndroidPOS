package com.nexapos.retail.ui.sale

import kotlin.math.roundToInt

/** Flat-rupee discount equal to [pct]% of [subtotal]. Percent is clamped to 0..100. */
fun percentToFlat(
    subtotal: Int,
    pct: Int,
): Int = (subtotal.toLong() * pct.coerceIn(0, 100) / 100.0).roundToInt()

/** The whole-percent that a [flat] discount represents of [subtotal], rounded and clamped to 0..100. */
fun flatToPercent(
    subtotal: Int,
    flat: Int,
): Int = if (subtotal <= 0) 0 else (flat * 100.0 / subtotal).roundToInt().coerceIn(0, 100)
