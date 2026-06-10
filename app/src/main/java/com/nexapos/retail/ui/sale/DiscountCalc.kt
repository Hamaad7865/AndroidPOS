package com.nexapos.retail.ui.sale

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Flat discount (cents) equal to [pct]% of [subtotalCents]. Percent is clamped to 0..100. */
fun percentToFlat(
    subtotalCents: Long,
    pct: Int,
): Long = (subtotalCents * pct.coerceIn(0, 100) / 100.0).roundToLong()

/** The whole-percent that a [flatCents] discount represents of [subtotalCents], clamped to 0..100. */
fun flatToPercent(
    subtotalCents: Long,
    flatCents: Long,
): Int = if (subtotalCents <= 0L) 0 else (flatCents * 100.0 / subtotalCents).roundToInt().coerceIn(0, 100)
