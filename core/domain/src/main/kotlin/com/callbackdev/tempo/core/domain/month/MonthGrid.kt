package com.callbackdev.tempo.core.domain.month

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * A month as the page draws it (PLANNING.md §11 Phase 9): whole weeks, from the week its first
 * date falls in to the week of its last, so the grid has four, five or six rows and no hole in
 * any of them. The dates of the months beside it that complete a week are drawn quieter, and a
 * span of days that crosses the month's end is seen whole.
 *
 * @property weeks the rows, each of seven dates from the reader's first day of the week.
 */
data class MonthGrid(val month: YearMonth, val weeks: List<List<LocalDate>>) {
    val first: LocalDate get() = weeks.first().first()

    val last: LocalDate get() = weeks.last().last()

    operator fun contains(date: LocalDate): Boolean = YearMonth.from(date) == month

    companion object {
        const val DAYS_A_WEEK = 7

        /** The most rows a month takes: thirty-one days that start on a week's last day. */
        const val MOST_WEEKS = 6

        fun of(month: YearMonth, firstDayOfWeek: DayOfWeek): MonthGrid {
            val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6)))
            val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
            val weeks = (0 until days / DAYS_A_WEEK).map { week ->
                (0 until DAYS_A_WEEK).map { day -> start.plusDays((week * DAYS_A_WEEK + day).toLong()) }
            }
            return MonthGrid(month, weeks)
        }

        /**
         * The dates to read for [month] alone: every date its grid can hold, whatever day the
         * week starts on (up to six of the month before, and six of the month after).
         */
        fun span(month: YearMonth): ClosedRange<LocalDate> =
            month.atDay(1).minusDays(DAYS_A_WEEK - 1L)..month.atEndOfMonth().plusDays(DAYS_A_WEEK - 1L)

        /** The week's days in the reader's order, for the grid's heading. */
        fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
            (0 until DAYS_A_WEEK).map { firstDayOfWeek.plus(it.toLong()) }
    }
}

/**
 * The months as pages of one long row, so the page turns from one to the next under the finger
 * and a far month is a page number away: two centuries, from January 1900 to December 2099. A
 * calendar's events lie well inside them, and a pager wants a count.
 */
object MonthPages {
    val FIRST: YearMonth = YearMonth.of(1900, 1)
    val LAST: YearMonth = YearMonth.of(2099, 12)
    val COUNT: Int = ChronoUnit.MONTHS.between(FIRST, LAST).toInt() + 1

    /** The page of [month]; a month outside the row is its nearest end. */
    fun pageOf(month: YearMonth): Int = ChronoUnit.MONTHS.between(FIRST, month).toInt().coerceIn(0, COUNT - 1)

    fun monthOf(page: Int): YearMonth = FIRST.plusMonths(page.coerceIn(0, COUNT - 1).toLong())

    /**
     * The dates to read for the page on [month]: its own grid and the grids of the months beside
     * it ([MonthGrid.span]), so the page under the finger has its marks before it is turned to.
     */
    fun span(month: YearMonth): ClosedRange<LocalDate> =
        MonthGrid.span(month.minusMonths(1)).start..MonthGrid.span(month.plusMonths(1)).endInclusive

    /** The years the row holds, for the page that jumps to a month. */
    val YEARS: IntRange = FIRST.year..LAST.year
}
