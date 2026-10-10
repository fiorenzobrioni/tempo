package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import com.callbackdev.tempo.core.model.AppPalette

/** One dress: a scheme and its semantic colors, measured together and never mixed. */
@Immutable
internal data class TempoPalette(
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightColors: TempoColors,
    val darkColors: TempoColors,
) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme

    fun colors(dark: Boolean): TempoColors = if (dark) darkColors else lightColors
}

/** Warm paper and amber. */
private val Paper = TempoPalette(PaperLightScheme, PaperDarkScheme, PaperLightColors, PaperDarkColors)

/** Daylight white and azure: the default. */
private val Vivid = TempoPalette(VividLightScheme, VividDarkScheme, VividLightColors, VividDarkColors)

internal fun paletteFor(choice: AppPalette): TempoPalette = when (choice) {
    AppPalette.PAPER -> Paper
    AppPalette.VIVID -> Vivid
}
