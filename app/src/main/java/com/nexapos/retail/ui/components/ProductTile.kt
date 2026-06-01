package com.nexapos.retail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexapos.retail.data.media.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val palettes: Map<String, List<Color>> =
    mapOf(
        "sprayer" to listOf(Color(0xFF1F4D8A), Color(0xFFE8C341), Color(0xFF0E2E55)),
        "drill" to listOf(Color(0xFFC7401A), Color(0xFF1A1714), Color(0xFFF4ECDD)),
        "wrench" to listOf(Color(0xFF3A332A), Color(0xFFC7BAA0), Color(0xFF14110C)),
        "saw" to listOf(Color(0xFFE8651D), Color(0xFF1A1714), Color(0xFFC7BAA0)),
        "scrubber" to listOf(Color(0xFF3F8B5E), Color(0xFFFBE5D2), Color(0xFF1F4D26)),
        "paint" to listOf(Color(0xFFD03B3B), Color(0xFFF4ECDD), Color(0xFF1A1714)),
        "hammer" to listOf(Color(0xFF7B3D14), Color(0xFF1A1714), Color(0xFFE8C341)),
        "pipe" to listOf(Color(0xFF2A3A45), Color(0xFFC7BAA0), Color(0xFF1A1714)),
        "generic" to listOf(Color(0xFF3B342A), Color(0xFFD9D1C2), Color(0xFFE8651D)),
    )

// Path-based tile shapes, kept as data so they can be parsed once and cached.
private val tilePaths: Map<String, List<String>> =
    mapOf(
        "wrench" to listOf("M14 22 L40 48 L48 40 L22 14 Z"),
        "saw" to
            listOf(
                "M10 50 L70 50 L66 56 L62 50 L58 56 L54 50 L50 56 L46 50 L42 56 L38 50 " +
                    "L34 56 L30 50 L26 56 L22 50 L18 56 L14 50 Z",
            ),
    )

/** Geometric, slightly textured product placeholder (80×80 art), matching shared.jsx. */
@Composable
fun ProductTile(
    kind: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
) {
    val p = palettes[kind] ?: palettes.getValue("generic")
    // Parse the kind's path shapes once (not per frame).
    val parsed = remember(kind) { (tilePaths[kind] ?: emptyList()).map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 80f
        scale(s, s, pivot = Offset.Zero) {
            // gradient background (p[1] -> p[0] @ 25%)
            drawRect(
                brush = Brush.verticalGradient(listOf(p[1], p[0].copy(alpha = 0.25f))),
                size = Size(80f, 80f),
            )
            // faint construction grid
            var g = 8f
            while (g < 80f) {
                drawLine(Color(0x0D000000), Offset(g, 0f), Offset(g, 80f), 1f)
                drawLine(Color(0x0D000000), Offset(0f, g), Offset(80f, g), 1f)
                g += 8f
            }
            when (kind) {
                "sprayer" -> {
                    rr(p[0], 22f, 20f, 22f, 40f, 3f)
                    rr(p[2], 26f, 14f, 14f, 8f, 2f)
                    rr(p[1], 44f, 28f, 14f, 3f, 0f)
                    drawCircle(p[1], 3f, Offset(33f, 32f))
                }
                "drill" -> {
                    rr(p[0], 14f, 34f, 36f, 14f, 3f)
                    rr(p[1], 22f, 48f, 14f, 18f, 3f)
                    rr(p[2], 50f, 38f, 18f, 6f, 1f)
                    drawCircle(p[0], 3f, Offset(68f, 41f))
                }
                "wrench" -> {
                    drawPath(parsed[0], color = p[0])
                    drawCircle(p[0], 6f, Offset(18f, 18f), style = Stroke(4f))
                    drawCircle(p[0], 6f, Offset(58f, 58f), style = Stroke(4f))
                }
                "saw" -> {
                    drawPath(parsed[0], color = p[0])
                    rr(p[1], 6f, 46f, 68f, 6f, 0f)
                    rr(p[2], 2f, 38f, 14f, 14f, 2f)
                }
                "scrubber" -> {
                    rr(p[0], 14f, 20f, 52f, 16f, 4f)
                    rr(p[1], 16f, 36f, 48f, 20f, 2f)
                    repeat(8) { i -> drawLine(p[2], Offset(20f + i * 5.5f, 38f), Offset(20f + i * 5.5f, 54f), 1.5f) }
                }
                "paint" -> {
                    rr(p[0], 22f, 20f, 36f, 44f, 3f)
                    rr(p[2], 20f, 16f, 40f, 6f, 1f)
                    rr(p[2], 34f, 6f, 6f, 14f, 0f)
                }
                "hammer" -> {
                    rr(p[1], 20f, 14f, 40f, 14f, 2f)
                    rr(p[0], 24f, 28f, 6f, 44f, 0f)
                }
                "pipe" -> {
                    rr(p[0], 10f, 32f, 60f, 16f, 0f)
                    rr(p[1], 6f, 28f, 10f, 24f, 0f)
                    rr(p[1], 64f, 28f, 10f, 24f, 0f)
                }
                else -> {
                    rr(p[0].copy(alpha = 0.9f), 20f, 20f, 40f, 40f, 0f)
                    rr(p[2], 28f, 28f, 24f, 24f, 0f)
                }
            }
        }
    }
}

private fun DrawScope.rr(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: Float,
) {
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(r, r))
}

/**
 * Shows a product's real photo (from [ImageStore]) when [imagePath] is set,
 * loading it off the main thread. Falls back to the generated [ProductTile]
 * artwork while loading or when there's no photo.
 */
@Composable
fun ProductThumb(
    imagePath: String?,
    kind: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
) {
    if (imagePath.isNullOrBlank()) {
        ProductTile(kind = kind, modifier = modifier, size = size)
        return
    }
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = imagePath) {
        value = withContext(Dispatchers.IO) { ImageStore.load(context, imagePath) }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = modifier.size(size).clip(RoundedCornerShape(7.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        ProductTile(kind = kind, modifier = modifier, size = size)
    }
}
