package com.nexapos.retail.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.theme.PosTheme

/**
 * Adaptive navigation shell. In landscape (the design target on a counter tablet)
 * it renders the vertical [NavRail] on the left; in portrait it falls back to a
 * compact horizontal [BottomNav] strip so the screen content gets full width.
 *
 * Use it instead of the previous `Box { Row { NavRail; Column{...} } }` boilerplate.
 */
@Composable
fun NavShell(
    active: String,
    onNav: (String) -> Unit,
    outerModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PosTheme.colors
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .then(outerModifier),
    ) {
        if (portrait) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().systemBarsPadding(),
                    content = content,
                )
                BottomNav(active = active, onNav = onNav)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                NavRail(active = active, onNav = onNav)
                Column(
                    Modifier.weight(1f).fillMaxHeight().systemBarsPadding(),
                    content = content,
                )
            }
        }
        overlay()
    }
}

/** Compact horizontal nav bar used in portrait mode. */
@Composable
private fun BottomNav(
    active: String,
    onNav: (String) -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(width = 1.dp, color = c.hairline)
            // Padding goes INSIDE the background so the bar paints all the way
            // down to the screen edge in portrait, leaving gesture insets clear.
            .systemBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        navItems.forEach { item ->
            val on = active == item.id
            Column(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) c.ink else Color.Transparent)
                        .clickable { onNav(item.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PosIcon(item.paths, tint = if (on) c.surface else c.graphite, size = 18.dp)
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) c.surface else c.graphite,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // Signed-in staff badge — tap to sign out / lock the till.
        SessionSlot(onNav = onNav, compact = true)
        Spacer(Modifier.width(4.dp))
    }
}

/**
 * True when the device is in portrait orientation. Screens with 2-column
 * landscape layouts (POS ticket panel, Parties detail panel, AddProduct step
 * rail, etc.) read this and stack vertically when true.
 */
@Composable
fun isPortrait(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

/**
 * Renders two panels side-by-side in landscape (a 2-column tablet layout) and
 * stacked vertically in portrait. Each panel content receives a pre-computed
 * [Modifier] with the right weight/size for the current orientation, applied
 * inside the parent Row/Column scope (so [androidx.compose.foundation.layout.RowScope.weight]
 * and [androidx.compose.foundation.layout.ColumnScope.weight] both work).
 *
 * @param secondaryWidthDp width of the secondary panel in landscape (the side
 *     panel — ticket, detail, summary). Ignored in portrait, where the panel
 *     gets [portraitSecondaryWeight] of the available height.
 */
@Composable
fun ResponsiveSplit(
    portrait: Boolean,
    secondaryWidthDp: Int = 380,
    portraitSecondaryWeight: Float = 0.55f,
    primary: @Composable (Modifier) -> Unit,
    secondary: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Stack vertically when in portrait OR when the screen is too narrow to
        // fit the secondary panel plus a usable (~340dp) primary — so the layout
        // adapts to the actual width, not just orientation. This keeps small
        // landscape tablets from overflowing the two-column layout.
        val stacked = portrait || maxWidth < (secondaryWidthDp + MIN_PRIMARY_DP).dp
        if (stacked) {
            Column(Modifier.fillMaxSize()) {
                primary(Modifier.weight(1f).fillMaxWidth())
                secondary(Modifier.weight(portraitSecondaryWeight).fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                primary(Modifier.weight(1f).fillMaxHeight())
                secondary(Modifier.width(secondaryWidthDp.dp).fillMaxHeight())
            }
        }
    }
}

private const val MIN_PRIMARY_DP = 340
