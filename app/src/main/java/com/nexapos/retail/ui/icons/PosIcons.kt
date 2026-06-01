@file:Suppress("ktlint:standard:max-line-length", "MaxLineLength")

package com.nexapos.retail.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * NexaPOS icon set — hand-drawn 1.6px stroke, rounded, no fill, 24x24.
 * Path data copied verbatim from the Claude Design handoff (shared.jsx `Icon`).
 * Strokes are solid black so Compose's `Icon(tint = …)` recolors them.
 */
private fun strokeIcon(
    vararg paths: String,
    sw: Float = 1.6f,
): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object PosIcons {
    val Home = strokeIcon("M3 11.5L12 4l9 7.5M5 10.5V20h14V10.5")
    val Cart =
        strokeIcon(
            "M3 4h2l2.5 12h11L21 7H7",
            "M9 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z",
            "M17 20.5a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4z",
        )
    val Chart = strokeIcon("M4 20V10", "M10 20V4", "M16 20v-7", "M22 20H2")
    val Report = strokeIcon("M6 3h9l5 5v13H6z", "M14 3v6h6", "M9 14h6M9 17h4")
    val Box = strokeIcon("M3 7l9-4 9 4-9 4-9-4z", "M3 7v10l9 4 9-4V7", "M12 11v10")
    val People =
        strokeIcon(
            "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z",
            "M2 21c0-3.5 3-6 7-6s7 2.5 7 6",
            "M17 11a3 3 0 1 0 0-6",
            "M16 21h6c0-2.5-1.8-4.7-4.5-5.4",
        )
    val Truck =
        strokeIcon(
            "M3 6h11v9H3z",
            "M14 9h4l3 3v3h-7",
            "M7 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
            "M17 18.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
        )
    val Wallet =
        strokeIcon("M3 7h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z", "M3 7V6a2 2 0 0 1 2-2h12", "M16 13h3")
    val Setting =
        strokeIcon(
            "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8L4.2 7a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    val Search = strokeIcon("M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z", "M21 21l-4.3-4.3")
    val Barcode = strokeIcon("M4 6v12", "M7 6v12", "M10 6v12", "M13 6v12", "M16 6v12", "M19 6v12")
    val Plus = strokeIcon("M12 5v14", "M5 12h14")
    val Minus = strokeIcon("M5 12h14")
    val Close = strokeIcon("M6 6l12 12", "M6 18L18 6")
    val Check = strokeIcon("M5 12l5 5L20 7")
    val ArrowR = strokeIcon("M5 12h14", "M13 6l6 6-6 6")
    val ChevR = strokeIcon("M9 6l6 6-6 6")
    val ChevD = strokeIcon("M6 9l6 6 6-6")
    val Bell = strokeIcon("M6 16V11a6 6 0 1 1 12 0v5l2 2H4z", "M10 21a2 2 0 1 0 4 0")
    val User = strokeIcon("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M4 21c0-4 4-7 8-7s8 3 8 7")
    val Trash = strokeIcon("M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
    val Print = strokeIcon("M7 8V3h10v5", "M5 8h14a2 2 0 0 1 2 2v6h-4v4H7v-4H3v-6a2 2 0 0 1 2-2z")
    val Share = strokeIcon("M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7", "M12 3v13", "M7 8l5-5 5 5")
    val Filter = strokeIcon("M3 5h18", "M6 12h12", "M10 19h4")
    val Download = strokeIcon("M12 3v13", "M7 12l5 5 5-5", "M5 21h14")
    val Sun =
        strokeIcon(
            "M12 4v2", "M12 18v2", "M4 12H2", "M22 12h-2", "M5 5l1.5 1.5", "M17.5 17.5L19 19",
            "M5 19l1.5-1.5", "M17.5 6.5L19 5", "M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
        )
    val Moon = strokeIcon("M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z")
    val Scan =
        strokeIcon(
            "M4 7V5a1 1 0 0 1 1-1h2",
            "M17 4h2a1 1 0 0 1 1 1v2",
            "M20 17v2a1 1 0 0 1-1 1h-2",
            "M7 20H5a1 1 0 0 1-1-1v-2",
            "M4 12h16",
        )
    val Card = strokeIcon("M3 7h18v11H3z", "M3 11h18", "M7 15h3")
    val Cash = strokeIcon("M2 7h20v10H2z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", "M5 9h2M17 15h2")
    val Mobile = strokeIcon("M7 3h10v18H7z", "M11 18h2")
    val Receipt = strokeIcon("M6 3h12v18l-3-2-3 2-3-2-3 2z", "M9 8h6M9 12h6M9 16h4")
    val More = strokeIcon("M5 12h.01", "M12 12h.01", "M19 12h.01", sw = 3f)
    val Menu = strokeIcon("M4 6h16", "M4 12h16", "M4 18h16")
    val Refresh = strokeIcon("M21 12a9 9 0 1 1-3-6.7L21 8", "M21 3v5h-5")
}
