package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.FreeGap
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FreeTimeTest {
    @Test
    fun `the gaps between events, never before the first or after the last`() {
        val result = agenda(
            timed(1, "2026-10-08T09:00", "2026-10-08T10:00"),
            timed(2, "2026-10-08T12:00", "2026-10-08T13:00"),
            timed(3, "2026-10-08T13:20", "2026-10-08T14:00"),
            now = at("2026-10-07T09:00"),
        )
        assertThat(result.day("2026-10-08").free).containsExactly(
            FreeGap(at("2026-10-08T10:00"), at("2026-10-08T12:00")),
            FreeGap(at("2026-10-08T13:00"), at("2026-10-08T13:20")),
        ).inOrder()
        assertThat(
            result.day("2026-10-08").freeRows,
        ).containsExactly(FreeGap(at("2026-10-08T10:00"), at("2026-10-08T12:00")))
    }

    @Test
    fun `today counts from now, and the time until the next event is free`() {
        val result = agenda(
            timed(1, "2026-10-07T09:00", "2026-10-07T10:00"),
            timed(2, "2026-10-07T12:00", "2026-10-07T13:00"),
            timed(3, "2026-10-07T15:00", "2026-10-07T16:00"),
            now = at("2026-10-07T12:30"),
        )
        // Under way at 12:30: the gap after it starts at its end; the morning's gap is gone.
        assertThat(
            result.day("2026-10-07").free,
        ).containsExactly(FreeGap(at("2026-10-07T13:00"), at("2026-10-07T15:00")))
        val early = agenda(timed(1, "2026-10-07T09:00", "2026-10-07T10:00"), now = at("2026-10-07T07:00"))
        assertThat(
            early.day("2026-10-07").free,
        ).containsExactly(FreeGap(at("2026-10-07T07:00"), at("2026-10-07T09:00")))
    }

    @Test
    fun `now inside a gap starts the gap at now`() {
        val result = agenda(
            timed(1, "2026-10-07T09:00", "2026-10-07T10:00"),
            timed(2, "2026-10-07T12:00", "2026-10-07T13:00"),
            now = at("2026-10-07T11:00"),
        )
        assertThat(
            result.day("2026-10-07").free,
        ).containsExactly(FreeGap(at("2026-10-07T11:00"), at("2026-10-07T12:00")))
    }

    @Test
    fun `overlapping events make one busy stretch`() {
        val result = agenda(
            timed(1, "2026-10-08T09:00", "2026-10-08T11:00"),
            timed(2, "2026-10-08T10:00", "2026-10-08T12:00"),
            timed(3, "2026-10-08T09:30", "2026-10-08T10:00"),
            timed(4, "2026-10-08T14:00", "2026-10-08T15:00"),
            now = at("2026-10-07T09:00"),
        )
        assertThat(
            result.day("2026-10-08").free,
        ).containsExactly(FreeGap(at("2026-10-08T12:00"), at("2026-10-08T14:00")))
    }

    @Test
    fun `free and declined events do not take the time`() {
        val result = agenda(
            timed(1, "2026-10-08T09:00", "2026-10-08T10:00"),
            timed(2, "2026-10-08T11:00", "2026-10-08T12:00", availability = Availability.FREE),
            timed(3, "2026-10-08T12:30", "2026-10-08T13:00", selfStatus = AttendeeStatus.DECLINED),
            timed(4, "2026-10-08T14:00", "2026-10-08T15:00"),
            now = at("2026-10-07T09:00"),
            filter = AgendaFilter(showDeclined = true),
        )
        assertThat(
            result.day("2026-10-08").free,
        ).containsExactly(FreeGap(at("2026-10-08T10:00"), at("2026-10-08T14:00")))
    }

    @Test
    fun `all-day events leave the day free`() {
        val result = agenda(
            allDay(1, "2026-10-08"),
            timed(2, "2026-10-08T09:00", "2026-10-08T10:00"),
            timed(3, "2026-10-08T11:00", "2026-10-08T12:00"),
            now = at("2026-10-07T09:00"),
        )
        assertThat(
            result.day("2026-10-08").free,
        ).containsExactly(FreeGap(at("2026-10-08T10:00"), at("2026-10-08T11:00")))
    }

    @Test
    fun `no events, no gaps`() {
        assertThat(FreeTime.gaps(emptyList(), from = at("2026-10-07T09:00"))).isEmpty()
    }
}
