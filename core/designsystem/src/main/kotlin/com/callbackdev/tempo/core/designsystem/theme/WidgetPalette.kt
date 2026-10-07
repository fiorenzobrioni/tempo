package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.callbackdev.tempo.core.model.AppPalette

/**
 * The card colours a home-screen widget can wear: **Chiaro's six, hex for hex**, as Passo's, so
 * a Tempo widget beside a Chiaro or a Passo one on the same home screen is the same piece of
 * furniture (PLANNING.md §7). Chiaro's DESIGN §2.6 carries the measurements, and they hold
 * here unchanged because the inks are the same: every one of the six is a dark ground under
 * white ink, white never under 7.8:1, the 75% quiet ink never under 5.2:1, the 85% one never
 * under 6.1:1.
 *
 * They are not roles and they do not follow the dress: the reader is choosing what colour a
 * card on their wallpaper is, which is a different act from the app's own colour. So this is a
 * table of hexes, and it lives in `theme/`, the one place hexes are allowed to.
 */
enum class WidgetCardColor { BLUE, AZURE, GREEN, TEAL, PLUM, CLAY }

/** The ground each colour paints, at full solidity; the reader's opacity thins it. */
fun widgetCardContainer(color: WidgetCardColor): Color = when (color) {
    WidgetCardColor.BLUE -> Color(0xFF0F3B6B)
    WidgetCardColor.AZURE -> Color(0xFF0F5580)
    WidgetCardColor.GREEN -> Color(0xFF17572E)
    WidgetCardColor.TEAL -> Color(0xFF0F5B5B)
    WidgetCardColor.PLUM -> Color(0xFF4A2C63)
    WidgetCardColor.CLAY -> Color(0xFF7A3320)
}

/**
 * One dress as a widget needs it: both schemes and both sets of semantic colours at once. A
 * widget cannot ask the theme which mode it is in (the launcher draws it, on a ground the app
 * does not control), so it resolves every colour itself against the ground it really has.
 */
data class WidgetDress(
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightColors: TempoColors,
    val darkColors: TempoColors,
) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme

    fun colors(dark: Boolean): TempoColors = if (dark) darkColors else lightColors
}

/**
 * [palette]'s dress; with [dynamicLight] and [dynamicDark] (the wallpaper's schemes, when the
 * reader turned wallpaper colours on) in place of its schemes. The semantic colours keep
 * following the dress, as in the app: a warning does not change colour with a photo.
 */
fun widgetDress(
    palette: AppPalette,
    dynamicLight: ColorScheme? = null,
    dynamicDark: ColorScheme? = null,
): WidgetDress {
    val dress = paletteFor(palette)
    return WidgetDress(
        lightScheme = dynamicLight ?: dress.lightScheme,
        darkScheme = dynamicDark ?: dress.darkScheme,
        lightColors = dress.lightColors,
        darkColors = dress.darkColors,
    )
}
