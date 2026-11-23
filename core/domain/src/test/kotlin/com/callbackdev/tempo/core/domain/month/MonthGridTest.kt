package com.callbackdev.tempo.core.domain.month

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {
    private fun grid(month: String, first: DayOfWeek = DayOfWeek.MONDAY) = MonthGrid.of(YearMonth.parse(month), first)

    @Test
    fun `a month is whole weeks, from the week of its first date to the week of its last`() {
        // November 2026 starts on a Sunday and ends on a Monday: six weeks from Monday.
        val november = grid("2026-11")
        assertThat(november.weeks).hasSize(6)
        assertThat(november.first).isEqualTo(LocalDate.parse("2026-10-26"))
        assertThat(november.last).isEqualTo(LocalDate.parse("2026-12-06"))
        assertThat(november.weeks.all { it.size == 7 && it.first().dayOfWeek == DayOfWeek.MONDAY }).isTrue()
    }

    @Test
    fun `the same month from Sunday is one week shorter`() {
        val november = grid("2026-11", DayOfWeek.SUNDAY)
        assertThat(november.weeks).hasSize(5)
        assertThat(november.first).isEqualTo(LocalDate.parse("2026-11-01"))
        assertThat(november.last).isEqualTo(LocalDate.parse("2026-12-05"))
    }

    @Test
    fun `a February of four whole weeks is four rows and nothing else`() {
        // February 2027 runs from Monday the 1st to Sunday the 28th.
        val february = grid("2027-02")
        assertThat(february.weeks).hasSize(4)
        assertThat(february.first).isEqualTo(LocalDate.parse("2027-02-01"))
        assertThat(february.last).isEqualTo(LocalDate.parse("2027-02-28"))
    }

    @Test
    fun `no month takes more than six rows, whatever day the week starts on`() {
        for (first in DayOfWeek.entries) {
            var month = YearMonth.of(2024, 1)
            repeat(48) {
                val weeks = MonthGrid.of(month, first).weeks
                assertThat(weeks.size).isIn(4..MonthGrid.MOST_WEEKS)
                assertThat(weeks.flatten().zipWithNext().all { (a, b) -> b == a.plusDays(1) }).isTrue()
                month = month.plusMonths(1)
            }
        }
    }

    @Test
    fun `the grid knows its own dates from its neighbours'`() {
        val november = grid("2026-11")
        assertThat(LocalDate.parse("2026-11-30") in november).isTrue()
        assertThat(LocalDate.parse("2026-10-31") in november).isFalse()
        assertThat(LocalDate.parse("2026-12-01") in november).isFalse()
    }

    @Test
    fun `the heading follows the reader's first day`() {
        assertThat(MonthGrid.weekdays(DayOfWeek.SUNDAY).first()).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(MonthGrid.weekdays(DayOfWeek.SUNDAY).last()).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(MonthGrid.weekdays(DayOfWeek.MONDAY)).containsExactlyElementsIn(DayOfWeek.entries).inOrder()
    }

    @Test
    fun `a month and its page are each other's`() {
        val month = YearMonth.of(2026, 11)
        assertThat(MonthPages.monthOf(MonthPages.pageOf(month))).isEqualTo(month)
        assertThat(MonthPages.pageOf(month.plusMonths(1))).isEqualTo(MonthPages.pageOf(month) + 1)
        assertThat(MonthPages.monthOf(0)).isEqualTo(MonthPages.FIRST)
        assertThat(MonthPages.monthOf(MonthPages.COUNT - 1)).isEqualTo(MonthPages.LAST)
    }

    @Test
    fun `a month outside the row is its nearest end`() {
        assertThat(MonthPages.pageOf(YearMonth.of(1850, 6))).isEqualTo(0)
        assertThat(MonthPages.pageOf(YearMonth.of(2300, 6))).isEqualTo(MonthPages.COUNT - 1)
    }

    @Test
    fun `the dates read hold the grids of the month and of the two beside it`() {
        val month = YearMonth.of(2026, 11)
        val span = MonthPages.span(month)
        for (first in DayOfWeek.entries) {
            for (shown in listOf(month.minusMonths(1), month, month.plusMonths(1))) {
                val grid = MonthGrid.of(shown, first)
                assertThat(grid.first).isAtLeast(span.start)
                assertThat(grid.last).isAtMost(span.endInclusive)
            }
        }
    }
}
