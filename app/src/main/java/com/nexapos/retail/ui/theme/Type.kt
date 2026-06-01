package com.nexapos.retail.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexapos.retail.R

@OptIn(ExperimentalTextApi::class)
private fun hanken(weight: Int) =
    Font(
        R.font.hanken_grotesk,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: Int) =
    Font(
        R.font.jetbrains_mono,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

/** Display + UI typeface. */
val HankenGrotesk =
    FontFamily(
        hanken(400),
        hanken(500),
        hanken(600),
        hanken(700),
        hanken(800),
    )

/** Monospaced, tabular face used for money, quantities and invoice numbers. */
val JetBrainsMono =
    FontFamily(
        mono(400),
        mono(500),
        mono(700),
    )

val AppTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.25).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
            ),
    )

@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int) =
    Font(
        R.font.fraunces,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

/** Accent serif (handoff --serif) used sparingly for editorial headings. */
val Fraunces = FontFamily(fraunces(500), fraunces(600), fraunces(700))

/** Tabular money/number styles built on JetBrains Mono. */
object MoneyStyles {
    val hero =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
        )
    val large =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        )
    val medium =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    val small =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
}
