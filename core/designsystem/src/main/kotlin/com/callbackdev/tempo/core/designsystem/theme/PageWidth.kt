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
 * proportions instead of stretching across the inner screen. Today alone, on a large screen,
 * stands in [TwoPanes] instead.
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

/**
 * The narrowest window that takes a page in two panes: Material's expanded width. Under it (a
 * phone upright, a foldable open under 840 dp) a page is its one column, centred.
 */
val TwoPaneMinWidth = 840.dp

/**
 * The shortest window that takes two panes: Material's medium height. A phone on its side is
 * as wide as a tablet (891 dp) and as short as a phone is narrow: it keeps the one column.
 */
val TwoPaneMinHeight = 480.dp

/** The widest two panes grow together, the page's column twice; past it they stand centred. */
val TwoPaneMaxWidth = PageMaxWidth * 2

/**
 * The leading pane's share of the two, and its narrowest: a phone's page, so the hero there is
 * never smaller than on a phone.
 */
const val LEADING_PANE_SHARE = 0.4f
val LeadingPaneMinWidth = 400.dp

/**
 * A page in two panes side by side (Today on a large screen, PLANNING.md §11 step 8.7): what the
 * page leaves free on each side, as [PageGutter] does for a column, and the two panes' widths
 * between them.
 */
@Immutable
data class TwoPanes(val gutter: PageGutter, val leading: Dp, val trailing: Dp) {
    /** The leading pane's own gutter: the page's on its outer side, none where the panes meet. */
    val leadingGutter: PageGutter get() = PageGutter(gutter.start, 0.dp)

    /** The trailing pane's own gutter. */
    val trailingGutter: PageGutter get() = PageGutter(0.dp, gutter.end)

    companion object {
        /**
         * The arithmetic, apart for its tests: null where the window is narrower than
         * [TwoPaneMinWidth] or shorter than [TwoPaneMinHeight]; else the insets first, the two panes
         * up to [TwoPaneMaxWidth] between them, the rest split between the two sides, and the
         * leading pane [LEADING_PANE_SHARE] of the two, never under [LeadingPaneMinWidth].
         */
        fun of(windowWidth: Dp, windowHeight: Dp, insetStart: Dp, insetEnd: Dp): TwoPanes? {
            if (windowWidth < TwoPaneMinWidth || windowHeight < TwoPaneMinHeight) return null
            val room = (windowWidth - insetStart - insetEnd).coerceAtLeast(0.dp)
            val width = room.coerceAtMost(TwoPaneMaxWidth)
            val spare = (room - width) / 2
            val leading = (width * LEADING_PANE_SHARE).coerceAtLeast(LeadingPaneMinWidth).coerceAtMost(width)
            return TwoPanes(PageGutter(insetStart + spare, insetEnd + spare), leading, width - leading)
        }
    }
}

/**
 * Two panes where the window is large enough for them (a tablet, a foldable open at 840 dp and
 * up), else null: the page is then its one column, with [pageGutter].
 */
@Composable
fun twoPanes(): TwoPanes? {
    val window = LocalWindowInfo.current.containerDpSize
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    return TwoPanes.of(
        windowWidth = window.width,
        windowHeight = window.height,
        insetStart = insets.calculateStartPadding(direction),
        insetEnd = insets.calculateEndPadding(direction),
    )
}
