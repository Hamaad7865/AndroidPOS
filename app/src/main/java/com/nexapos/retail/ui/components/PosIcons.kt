package com.nexapos.retail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexapos.retail.ui.theme.Amber
import com.nexapos.retail.ui.theme.Ink

/**
 * The prototype's iconography: hand-drawn 1.6px stroke, rounded caps/joins, no fill,
 * on a 24×24 grid. We render the exact same SVG path data via [PathParser] so the
 * icons match pixel-for-pixel.
 */
object PosIcons {
    val home = listOf("M3 11.5L12 4l9 7.5M5 10.5V20h14V10.5")
    val cart =
        listOf(
            "M3 4h2l2.5 12h11L21 7H7",
            "M9 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z",
            "M17 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z",
        )
    val chart = listOf("M4 20V10", "M10 20V4", "M16 20v-7", "M22 20H2")
    val report = listOf("M6 3h9l5 5v13H6z", "M14 3v6h6", "M9 14h6M9 17h4")
    val box = listOf("M3 7l9-4 9 4-9 4-9-4z", "M3 7v10l9 4 9-4V7", "M12 11v10")
    val people =
        listOf(
            "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z",
            "M2 21c0-3.5 3-6 7-6s7 2.5 7 6",
            "M17 11a3 3 0 1 0 0-6",
            "M16 21h6c0-2.5-1.8-4.7-4.5-5.4",
        )
    val truck =
        listOf(
            "M3 6h11v9H3z",
            "M14 9h4l3 3v3h-7",
            "M7 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
            "M17 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
        )
    val wallet =
        listOf("M3 7h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z", "M3 7V6a2 2 0 0 1 2-2h12", "M16 13h3")
    val setting =
        listOf(
            "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8L4.2 7a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    val search = listOf("M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z", "M21 21l-4.3-4.3")
    val barcode = listOf("M4 6v12", "M7 6v12", "M10 6v12", "M13 6v12", "M16 6v12", "M19 6v12")
    val scan =
        listOf(
            "M3 7V5a2 2 0 0 1 2-2h2",
            "M21 7V5a2 2 0 0 0-2-2h-2",
            "M3 17v2a2 2 0 0 0 2 2h2",
            "M21 17v2a2 2 0 0 1-2 2h-2",
            "M7 12h10",
        )
    val plus = listOf("M12 5v14", "M5 12h14")
    val minus = listOf("M5 12h14")
    val close = listOf("M6 6l12 12", "M6 18L18 6")
    val check = listOf("M5 12l5 5L20 7")
    val arrowR = listOf("M5 12h14", "M13 6l6 6-6 6")
    val chevD = listOf("M6 9l6 6 6-6")
    val chevR = listOf("M9 6l6 6-6 6")
    val receipt = listOf("M6 3h12v18l-3-2-3 2-3-2-3 2z", "M9 8h6M9 12h6M9 16h4")
    val refresh = listOf("M21 12a9 9 0 1 1-3-6.7L21 8", "M21 3v5h-5")
    val cash = listOf("M2 7h20v10H2z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", "M5 9h2M17 15h2")
    val card = listOf("M3 7h18v11H3z", "M3 11h18", "M7 15h3")
    val mobile = listOf("M7 3h10v18H7z", "M11 18h2")
    val print = listOf("M7 8V3h10v5", "M5 8h14a2 2 0 0 1 2 2v6h-4v4H7v-4H3v-6a2 2 0 0 1 2-2z")
    val share = listOf("M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7", "M12 3v13", "M7 8l5-5 5 5")
    val download = listOf("M12 3v13", "M7 12l5 5 5-5", "M5 21h14")
    val upload = listOf("M12 16V4", "M7 9l5-5 5 5", "M5 20h14")
    val trash = listOf("M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
    val arrowUp = listOf("M7 14l5-5 5 5")
    val arrowDn = listOf("M7 10l5 5 5-5")
    val bell = listOf("M6 16V11a6 6 0 1 1 12 0v5l2 2H4z", "M10 21a2 2 0 1 0 4 0")
    val filter = listOf("M3 5h18", "M6 12h12", "M10 19h4")
    val user = listOf("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M4 21c0-4 4-6 8-6s8 2 8 6")
    val star = listOf("M12 3l2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z")
    val sun = listOf("M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M12 2v2", "M12 20v2", "M2 12h2", "M20 12h2", "M5 5l1.4 1.4", "M17.6 17.6L19 19", "M19 5l-1.4 1.4", "M6.4 17.6L5 19")
    val moon = listOf("M20 14a8 8 0 1 1-9-11 6 6 0 0 0 9 11z")
}

@Composable
fun PosIcon(
    paths: List<String>,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Float = 1.6f,
) {
    // Parse SVG path data once per icon (not every frame) to avoid main-thread jank.
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            parsed.forEach { p ->
                drawPath(p, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** The NexaPOS mark — rounded ink square with an amber "N" and bone receipt lines. */
@Composable
fun NexaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val bone = Color(0xFFF4ECDD)
    val bg = remember { PathParser().parsePathString("M9 2h14a7 7 0 0 1 7 7v14a7 7 0 0 1-7 7H9a7 7 0 0 1-7-7V9a7 7 0 0 1 7-7z").toPath() }
    val mark = remember { PathParser().parsePathString("M9 22V10l7 8V10").toPath() }
    val lines = remember { PathParser().parsePathString("M19 14h4M19 18h4M19 22h2").toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 32f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(bg, color = Ink)
            drawPath(mark, color = Amber, style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(lines, color = bone, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
        }
    }
}
