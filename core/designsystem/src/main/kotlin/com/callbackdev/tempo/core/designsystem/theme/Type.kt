package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.model.AppFont

/*
 * Chiaro's type (its DESIGN.md §5), the same faces and the same scale, so the apps read as one
 * family. Both bundled faces are variable fonts under the SIL Open Font License 1.1
 * (`licenses/`), copied from Passo, which copied them from Chiaro, where
 * tools/import_google_sans.py cut Google Sans down to the scripts the family prints. Bundled, not fetched from a font provider: that would be a
 * runtime dependency on Play Services, and the app has no network anyway.
 */

// The variationSettings overload is still experimental. Without it Android would synthesise
// the weights by smearing one outline, which is what a variable font is bundled to avoid.
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Google Sans: its weight axis starts at 400, so a Light request lands on Regular. */
private val GoogleSansFamily = FontFamily(
    variable(R.font.google_sans_variable, 400),
    variable(R.font.google_sans_variable, 500),
    variable(R.font.google_sans_variable, 600),
    variable(R.font.google_sans_variable, 700),
)

private val InterFamily = FontFamily(
    variable(R.font.inter_variable, 300),
    variable(R.font.inter_variable, 400),
    variable(R.font.inter_variable, 500),
    variable(R.font.inter_variable, 600),
    variable(R.font.inter_variable, 700),
)

/**
 * The phone's own sans: a different font on every device, with whatever weights it has and
 * tabular figures it may not carry. The closest the app gets to the home-screen widget, which
 * RemoteViews always draws in the system face.
 */
private val SystemFamily = FontFamily.Default

internal fun familyFor(font: AppFont): FontFamily = when (font) {
    AppFont.GOOGLE_SANS -> GoogleSansFamily
    AppFont.INTER -> InterFamily
    AppFont.SYSTEM -> SystemFamily
}

/** Figures in a column, or updating live, are tabular: proportional digits make a number wobble. */
private const val TABULAR = "tnum"

/**
 * Material's scale in [family], with Chiaro's four adjustments. Every role is named, so no
 * line of the app falls back to the platform face by accident (`TypographyTest` counts them).
 */
internal fun typographyFor(family: FontFamily): Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(
            fontFamily = family,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleMedium = titleMedium.copy(
            fontFamily = family,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}

/**
 * The two roles Material does not have, in the reader's family.
 *
 * @property heroNumber the time, the thing the screen is for: bold, tracked in, tabular so the
 *   minutes do not wobble as they change (Chiaro's hero temperature, Passo's count: 64sp on a
 *   68sp line, −0.02em). Whether the clock wants a larger cut is Phase 3's to measure.
 * @property readingValue a value in a metric tile: light, tabular, 24sp on a 32sp line
 *   (Chiaro's tile reading).
 */
@Immutable
data class TempoType(val heroNumber: TextStyle, val readingValue: TextStyle)

private fun tempoTypeFor(family: FontFamily) = TempoType(
    heroNumber = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR,
    ),
    readingValue = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontFeatureSettings = TABULAR,
    ),
)

private val typographies = AppFont.entries.associateWith { typographyFor(familyFor(it)) }
private val tempoTypes = AppFont.entries.associateWith { tempoTypeFor(familyFor(it)) }

/** Built once per face and picked: the reader changes this about once. */
internal fun tempoTypography(font: AppFont): Typography = typographies.getValue(font)

internal fun tempoType(font: AppFont): TempoType = tempoTypes.getValue(font)

val LocalTempoType = staticCompositionLocalOf { tempoTypeFor(GoogleSansFamily) }

/** Any style, with tabular figures. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR)
