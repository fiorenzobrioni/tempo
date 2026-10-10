package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.tempo.core.model.AppFont
import com.callbackdev.tempo.core.model.AppPalette

/**
 * Tempo's theme: the design language of `docs/adr/0002-design-language.md`. The defaults are the
 * vivid dress, Google Sans, the app's own colors. [dynamicColor] takes Material's roles from the wallpaper instead; the
 * semantic colors ([TempoColors]) keep following the dress, because a warning must
 * not change color with a photo.
 */
@Composable
fun TempoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: AppPalette = AppPalette.VIVID,
    font: AppFont = AppFont.GOOGLE_SANS,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dress = paletteFor(palette)
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> dress.scheme(darkTheme)
    }
    CompositionLocalProvider(
        LocalTempoColors provides dress.colors(darkTheme),
        LocalTempoType provides tempoType(font),
        LocalReducedMotion provides rememberReducedMotion(context),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = tempoTypography(font),
            shapes = TempoShapes,
            content = content,
        )
    }
}

/** The design system's own values, next to [MaterialTheme]'s. */
object TempoTheme {
    val colors: TempoColors
        @Composable @ReadOnlyComposable
        get() = LocalTempoColors.current

    val type: TempoType
        @Composable @ReadOnlyComposable
        get() = LocalTempoType.current
}
