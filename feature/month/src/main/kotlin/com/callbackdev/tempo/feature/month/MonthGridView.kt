package com.callbackdev.tempo.feature.month

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.callbackdev.tempo.core.designsystem.components.MonthDays
import com.callbackdev.tempo.core.designsystem.components.MonthGridEdge
import com.callbackdev.tempo.core.designsystem.components.MonthWeeks
import com.callbackdev.tempo.core.designsystem.components.monthCellHeight
import com.callbackdev.tempo.core.designsystem.components.rememberMonthDayLabels
import com.callbackdev.tempo.core.domain.month.MonthGrid
import com.callbackdev.tempo.core.domain.month.MonthPages
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.sign

/**
 * The months, a page each, turned under the finger; a month's own drawing, its weeks of dates and
 * their marks, is the one every page that shows a month shares ([MonthWeeks]). A page is as tall
 * as its month's weeks (four, five or six rows), and between two months of different heights the
 * grid's own height follows the finger, so what stands under it slides with the page instead of
 * jumping when it lands. Read in the layout: turning a page composes nothing.
 *
 * A date of the month beside is on that month's page too, and three pages are composed at a time:
 * the two have a tag each (`MonthCellTags`).
 *
 * @param cell a date's height: what its number and its marks take, or more where the page has
 *   the room to give (a large screen's pane).
 */
@Composable
internal fun MonthPager(
    pager: PagerState,
    firstDay: DayOfWeek,
    days: MonthDays,
    modifier: Modifier = Modifier,
    cell: Dp = monthCellHeight(),
) {
    val rows = remember(firstDay) { mutableMapOf<Int, Int>() }
    fun rowsOf(page: Int) = rows.getOrPut(page) { MonthGrid.of(MonthPages.monthOf(page), firstDay).weeks.size }
    val labels = rememberMonthDayLabels()
    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top,
        key = { it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MonthGridEdge)
            .layout { measurable, constraints ->
                val offset = pager.currentPageOffsetFraction
                val here = rowsOf(pager.currentPage)
                val beside = (pager.currentPage + offset.sign.toInt()).coerceIn(0, MonthPages.COUNT - 1)
                val height = (cell * (here + (rowsOf(beside) - here) * abs(offset))).roundToPx()
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .testTag(MonthTags.GRID),
    ) { page ->
        val grid = remember(page, firstDay) { MonthGrid.of(MonthPages.monthOf(page), firstDay) }
        MonthWeeks(grid, days, cell = cell, labels = labels)
    }
}
