package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest a page's column grows, its margins included: Material's bottom sheet width, a
 * phone's column and a little more. Below it (every phone held upright, a folded foldable) a
 * page is laid out exactly as before; above it (a foldable open, a phone on its side) the
 * column stays this wide, centered, so the clock, the agenda and a line of text keep a phone's
 * proportions instead of stretching across the inner screen.
 */
val PageMaxWidth = 640.dp

/**
 * What a page leaves free on each side so its column is centered and clear of the display
 * cutout and a side navigation bar. The side's background (the window, a bar)
 * still reaches the edge: only the content moves in.
 */
@Immutable
data class PageGutter(val start: Dp, val end: Dp) {
    /** A list's content padding: the gutter, plus its own [top] and [bottom]. */
    fun contentPadding(top: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues =
        PaddingValues(start = start, top = top, end = end, bottom = bottom)

    /**
     * The gutter as insets, for a top app bar that keeps its ground across the window and moves
     * its title and actions over the column. Left and right: meant for the gutter a `Scaffold`
     * page takes (`sideInsets = false`), the same on both sides.
     */
    fun asInsets(): WindowInsets = WindowInsets(left = start, right = end)

    companion object {
        val None = PageGutter(0.dp, 0.dp)

        /**
         * The arithmetic, apart for its tests: the insets first, then whatever the window has
         * beyond [PageMaxWidth] split between the two sides.
         */
        fun of(windowWidth: Dp, insetStart: Dp, insetEnd: Dp): PageGutter {
            val spare = (windowWidth - insetStart - insetEnd - PageMaxWidth).coerceAtLeast(0.dp) / 2
            return PageGutter(insetStart + spare, insetEnd + spare)
        }
    }
}

/**
 * The gutter of a page that fills the window's width, as every page does.
 *
 * @param sideInsets false for a page whose `Scaffold` already pads its content for the cutout
 *   and a side navigation bar, so they are not counted twice.
 */
@Composable
fun pageGutter(sideInsets: Boolean = true): PageGutter {
    val width = LocalWindowInfo.current.containerDpSize.width
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val start = insets.calculateStartPadding(direction)
    val end = insets.calculateEndPadding(direction)
    val gutter = PageGutter.of(width, start, end)
    return if (sideInsets) gutter else PageGutter(gutter.start - start, gutter.end - end)
}

/** Pads a row or a column that is not a list by the page's [gutter]. */
fun Modifier.padding(gutter: PageGutter): Modifier = padding(start = gutter.start, end = gutter.end)
