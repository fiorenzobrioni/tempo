package com.callbackdev.tempo.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.tempo.core.designsystem.components.dialMark
import com.callbackdev.tempo.core.designsystem.theme.TempoColors
import com.callbackdev.tempo.core.designsystem.theme.paletteFor
import com.callbackdev.tempo.core.domain.calendar.MIN_CONTRAST
import com.callbackdev.tempo.core.domain.calendar.blend
import com.callbackdev.tempo.core.domain.calendar.markColor
import com.callbackdev.tempo.core.model.AppPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every ink the app sets text in reads on every ground it stands on, in both dresses and both
 * themes: WCAG's 4.5:1 for text, the Accessibility Scanner's threshold. The generated schemes
 * are never hand-edited, so this pins the pairs
 * the screens use, and the colors Tempo adds. A screen that sets text on a new pair adds it here.
 *
 * And every mark, a calendar's colour drawn as a bar, a dot or an arc, reads at 3:1 on the grounds
 * a mark stands on: the ratio for a graphic that carries meaning, which is all a calendar's
 * colour ever is here.
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

    /**
     * Today's two grounds (PLANNING.md §11 step 10.2): the clock's, under the hero, and now's,
     * under the event under way. The date, the time and the sentence's lead on the first, the
     * quiet ink of the sentence and the alarm, and the accent; the title, the quiet words (the
     * place, the end, the notes) and the accent of "40 min left" on the second.
     */
    @Test
    fun `text reads on Today's two grounds, the clock's and now's`() {
        for (dress in dresses) {
            val s = dress.scheme
            expect(dress, "onSurface on the clock's ground", s.onSurface, s.surfaceContainer)
            expect(dress, "onSurfaceVariant on the clock's ground", s.onSurfaceVariant, s.surfaceContainer)
            expect(dress, "primary on the clock's ground", s.primary, s.surfaceContainer)
            expect(dress, "onPrimaryContainer on now's ground", s.onPrimaryContainer, s.primaryContainer)
            expect(dress, "onSurfaceVariant on now's ground", s.onSurfaceVariant, s.primaryContainer)
            expect(dress, "primary on now's ground", s.primary, s.primaryContainer)
        }
    }

    @Test
    fun `a calendar's mark reads on every ground a mark stands on`() {
        for (dress in dresses) {
            val s = dress.scheme
            val grounds = listOf(
                "the page" to s.surface,
                "the clock's ground" to s.surfaceContainer,
                "now's ground" to s.primaryContainer,
            )
            for ((name, ground) in grounds) {
                // A row whose calendar has no colour wears the outline's, stepped as a colour is.
                for (color in CALENDAR_COLOURS + s.outline.toArgb()) {
                    val mark = Color(markColor(color, ground.toArgb()))
                    expectMark(dress, "${hex(color)} on $name", mark, ground)
                }
            }
        }
    }

    /** The dial's ring is at its face's rim: an arc has the face on one side, the hero's ground on the other. */
    @Test
    fun `the dial's quiet arcs read against the clock's ground and against the face`() {
        for (dress in dresses) {
            val s = dress.scheme
            val ground = s.surfaceContainer.toArgb()
            val face = s.surface.toArgb()
            // An event with no colour known is drawn in the page's ink, as quiet.
            for (color in CALENDAR_COLOURS + s.onSurface.toArgb()) {
                val ink = dialMark(color, ground, face)
                // As the canvas draws it, and as the alpha was found: channel by channel, rounded.
                val drawn = Color(blend(ink.copy(alpha = 1f).toArgb(), face, ink.alpha))
                expectMark(dress, "the arc ${hex(color)} against the clock's ground", drawn, s.surfaceContainer)
                expectMark(dress, "the arc ${hex(color)} against the face", drawn, s.surface)
            }
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

    private fun expectMark(dress: Dress, what: String, mark: Color, ground: Color) {
        val ratio = contrast(mark, ground)
        assertWithMessage("${dress.name}: $what is ${"%.2f".format(ratio)}:1").that(ratio).isAtLeast(MIN_CONTRAST)
    }

    private fun hex(argb: Int) = "#%06X".format(argb and 0xFFFFFF)

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TEXT_CONTRAST = 4.5

        /**
         * The colours a calendar app offers for a calendar or an event, from the darkest to the
         * palest, with the two no ground can be asked to carry as they are.
         */
        val CALENDAR_COLOURS = listOf(
            0xFFD50000, 0xFFE67C73, 0xFFF4511E, 0xFFF6BF26, 0xFF33B679, 0xFF0B8043,
            0xFF039BE5, 0xFF3F51B5, 0xFF7986CB, 0xFF8E24AA, 0xFF616161, 0xFFFFFFFF, 0xFF000000,
        ).map { it.toInt() }
    }
}
