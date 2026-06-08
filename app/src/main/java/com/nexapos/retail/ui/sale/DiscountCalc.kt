package com.nexapos.retail.ui.sale

import kotlin.math.roundToInt

/** Flat-rupee discount equal to [pct]% of [subtotal]. Percent is clamped to 0..100. */
fun percentToFlat(
    subtotal: Int,
    pct: Int,
): Int = (subtotal * pct.coerceIn(0, 100) / 100.0).roundToInt()
