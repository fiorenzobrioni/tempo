package com.callbackdev.tempo.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.callbackdev.tempo.core.designsystem.theme.TempoColors
import com.callbackdev.tempo.core.designsystem.theme.paletteFor
import com.callbackdev.tempo.core.model.AppPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every ink the app sets text in reads on every ground it stands on, in both dresses and both
 * themes: WCAG's 4.5:1 for text, the Accessibility Scanner's threshold (Passo's ContrastTest,
 * carried over). The generated schemes are Chiaro's and never hand-edited, so this pins the pairs
 * the screens use, and the colors Tempo adds. A screen that sets text on a new pair adds it here.
 */
class ContrastTest {
    private val dresses = AppPalette.entries.flatMap { palette ->
        listOf(false, true).map { dark ->
            val dress = paletteFor(palette)
            Dress("$palette ${if (dark) "dark" else "light"}", dress.scheme(dark), dress.colors(dark))
        }
    }

    private class Dress(val name: String, val scheme: ColorScheme, val colors: TempoColors)

    @Test
    fun `text reads on every ground it is set on`() {
        for (dress in dresses) {
            val s = dress.scheme
            val grounds = listOf(
                "surface" to s.surface,
                "surfaceContainerLow" to s.surfaceContainerLow,
                "surfaceContainer" to s.surfaceContainer,
                "surfaceContainerHigh" to s.surfaceContainerHigh,
                "surfaceContainerHighest" to s.surfaceContainerHighest,
            )
            for ((ground, color) in grounds) {
                expect(dress, "onSurface on $ground", s.onSurface, color)
                expect(dress, "onSurfaceVariant on $ground", s.onSurfaceVariant, color)
                expect(dress, "primary on $ground", s.primary, color)
            }
            expect(dress, "onPrimary on primary", s.onPrimary, s.primary)
            expect(dress, "onPrimaryContainer on primaryContainer", s.onPrimaryContainer, s.primaryContainer)
            expect(dress, "onSecondaryContainer on secondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
            expect(dress, "onErrorContainer on errorContainer", s.onErrorContainer, s.errorContainer)
            expect(dress, "error on surface", s.error, s.surface)
            expect(dress, "inverseOnSurface on inverseSurface", s.inverseOnSurface, s.inverseSurface)
        }
    }

    @Test
    fun `the attention ink reads on the page`() {
        for (dress in dresses) {
            val s = dress.scheme
            expect(dress, "attention on surface", dress.colors.attention, s.surface)
            expect(dress, "attention on surfaceContainerLow", dress.colors.attention, s.surfaceContainerLow)
        }
    }

    private fun expect(dress: Dress, what: String, ink: Color, ground: Color) {
        val ratio = contrast(ink.compositeOver(ground), ground)
        assertWithMessage("${dress.name}: $what is ${"%.2f".format(ratio)}:1").that(ratio).isAtLeast(TEXT_CONTRAST)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TEXT_CONTRAST = 4.5
    }
}
