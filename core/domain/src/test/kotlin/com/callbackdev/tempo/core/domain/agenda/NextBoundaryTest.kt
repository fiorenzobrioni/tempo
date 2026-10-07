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
    fun `the midnight after a 23-hour date`() {
        val result = agenda(now = at("2026-03-29T12:00"))
        assertThat(NextBoundary.after(result, Rome)).isEqualTo(at("2026-03-30T00:00"))
    }
}
