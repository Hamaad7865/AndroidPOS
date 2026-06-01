package com.nexapos.retail.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Amber,
        onPrimary = Color.White,
        primaryContainer = AmberSoft,
        onPrimaryContainer = AmberPress,
        secondary = Graphite,
        onSecondary = Color.White,
        tertiary = Emerald,
        onTertiary = Color.White,
        background = Bg,
        onBackground = Ink,
        surface = Raised,
        onSurface = Ink,
        surfaceVariant = Raised2,
        onSurfaceVariant = Muted,
        outline = Hairline,
        outlineVariant = Hairline2,
        error = Crimson,
        onError = Color.White,
    )

/**
 * Full Workshop Precision token set, exposed to composables that need the exact
 * design values (the Material scheme only covers a subset). Mirrors tokens.css.
 */
@Immutable
data class PosColors(
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val raised2: Color,
    val ink: Color,
    val graphite: Color,
    val muted: Color,
    val hairline: Color,
    val hairline2: Color,
    val amber: Color,
    val amberPress: Color,
    val amberSoft: Color,
    val amberTint: Color,
    val emerald: Color,
    val emeraldSoft: Color,
    val crimson: Color,
    val crimsonSoft: Color,
    val low: Color,
    val lowSoft: Color,
    val ring: Color,
) {
    // Backwards-compatible aliases used by earlier screens.
    val success get() = emerald
    val warning get() = low
    val danger get() = crimson
    val ticketSurface get() = surface
    val mutedText get() = muted
    val moneyPositive get() = emerald
}

private val LightPosColors =
    PosColors(
        bg = Bg, surface = SurfaceToken, raised = Raised, raised2 = Raised2,
        ink = Ink, graphite = Graphite, muted = Muted, hairline = Hairline, hairline2 = Hairline2,
        amber = Amber, amberPress = AmberPress, amberSoft = AmberSoft, amberTint = AmberTint,
        emerald = Emerald, emeraldSoft = EmeraldSoft, crimson = Crimson, crimsonSoft = CrimsonSoft,
        low = Low, lowSoft = LowSoft, ring = Ring,
    )

val LocalPosColors = staticCompositionLocalOf { LightPosColors }

/** Accessor: `PosTheme.colors.amber`, with `.extra` kept as an alias for older code. */
object PosTheme {
    val colors: PosColors
        @Composable get() = LocalPosColors.current
    val extra: PosColors
        @Composable get() = LocalPosColors.current
}

@Composable
fun NexaPosTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPosColors provides LightPosColors) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
