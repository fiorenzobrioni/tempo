package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors Material has no role for, per dress and per theme, never flipped: a dark value is
 * chosen for dark. Taken from Chiaro, as Passo takes them, so a meaning keeps its color across
 * the family; a family is added here with the phase that needs it, not before.
 *
 * - **attention**: an ink for "this is not live right now" (the calendar permission missing, a
 *   widget that cannot read the calendar): Chiaro's freshness ink, which says "this data is
 *   old" in the same amber, 7:1 or better on either surface. Passo's paused widget wears it too.
 *
 * The reader's calendar colours are not here and never will be: they are data, chosen in the
 * calendar app with no thought for this app's grounds, so they mark an event (a dot, a bar) and
 * never paint its text or its ground (PLANNING.md §6).
 */
@Immutable
data class TempoColors(val attention: Color)

internal val PaperLightColors = TempoColors(attention = Color(0xFF7A5200))

internal val PaperDarkColors = TempoColors(attention = Color(0xFFFFBC27))

internal val VividLightColors = TempoColors(attention = Color(0xFF7A5200))

internal val VividDarkColors = TempoColors(attention = Color(0xFFFFBC27))

val LocalTempoColors = staticCompositionLocalOf { VividLightColors }
