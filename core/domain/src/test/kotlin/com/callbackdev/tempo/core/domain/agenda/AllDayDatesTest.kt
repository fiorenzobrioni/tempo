package com.callbackdev.tempo.core.domain.agenda

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AllDayDatesTest {
    @Test
    fun `one date, and five`() {
        assertThat(
            allDay(1, "2026-10-07").allDayDates(),
        ).isEqualTo(LocalDate.parse("2026-10-07")..LocalDate.parse("2026-10-07"))
        assertThat(allDay(1, "2026-10-06", dates = 5).allDayDates())
            .isEqualTo(LocalDate.parse("2026-10-06")..LocalDate.parse("2026-10-10"))
    }

    @Test
    fun `a malformed event with no length covers its first date`() {
        val broken = allDay(1, "2026-10-07").let { it.copy(end = it.begin) }
        assertThat(broken.allDayDates()).isEqualTo(LocalDate.parse("2026-10-07")..LocalDate.parse("2026-10-07"))
    }
}
