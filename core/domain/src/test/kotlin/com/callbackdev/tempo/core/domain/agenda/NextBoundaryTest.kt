package com.callbackdev.tempo.core.domain.agenda

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NextBoundaryTest {
    @Test
    fun `the next start or end, whichever is first`() {
        val running = timed(1, "2026-10-07T09:30", "2026-10-07T10:30")
        val later = timed(2, "2026-10-07T10:15", "2026-10-07T11:00")
        val result = agenda(running, later, now = at("2026-10-07T10:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-10-07T10:15"))
    }

    @Test
    fun `with nothing left today, the next midnight`() {
        val tomorrow = timed(1, "2026-10-08T09:00", "2026-10-08T10:00")
        val result = agenda(tomorrow, now = at("2026-10-07T23:59"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-10-08T00:00"))
    }

    @Test
    fun `the end of a long event under way counts`() {
        val conference = timed(1, "2026-10-06T09:00", "2026-10-07T17:00")
        val result = agenda(conference, now = at("2026-10-07T16:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-10-07T17:00"))
    }

    @Test
    fun `a moment exactly now is not a boundary any more`() {
        val result = agenda(timed(1, "2026-10-07T10:00", "2026-10-07T11:00"), now = at("2026-10-07T10:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-10-07T11:00"))
    }

    @Test
    fun `with the dial on a home screen, an event coming onto its face is a boundary`() {
        // At 20:00 the face shows until 07:30: the 08:00 meeting comes onto it at 20:30, not at midnight.
        val tomorrow = timed(1, "2026-10-08T08:00", "2026-10-08T09:00")
        val result = agenda(tomorrow, now = at("2026-10-07T20:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-10-08T00:00"))
        assertThat(NextBoundary.after(result, Rome, dial = true)).isEqualTo(at("2026-10-07T20:31"))
    }

    @Test
    fun `an event already on the face adds no boundary`() {
        val running = timed(1, "2026-10-07T09:30", "2026-10-07T10:30")
        val later = timed(2, "2026-10-07T13:00", "2026-10-07T14:00")
        val result = agenda(running, later, now = at("2026-10-07T10:00"))
        assertThat(NextBoundary.after(result, Rome, dial = true)).isEqualTo(at("2026-10-07T10:30"))
    }

    @Test
    fun `the face is the wall clock, the night the clocks go back too`() {
        // 08:00 on 25 October is 12 h 29 min of real time after 20:31 the evening before, and 11 h 29 min
        // on the face: it comes onto the face at 20:31, as on any other evening.
        val sunday = timed(1, "2026-10-25T08:00", "2026-10-25T09:00")
        val result = agenda(sunday, now = at("2026-10-24T20:00"))
        assertThat(NextBoundary.after(result, Rome, dial = true)).isEqualTo(at("2026-10-24T20:31"))
    }

    @Test
    fun `the midnight after a 23-hour date`() {
        val result = agenda(now = at("2026-03-29T12:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-03-30T00:00"))
    }
}
