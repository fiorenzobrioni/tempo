package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The rest of today in a few words, beside «Agenda»'s list: what is left, and when it ends. */
class CardSummaryTest {
    private val run = timed(1, "2026-10-07T07:30", "2026-10-07T08:15")
    private val review = timed(2, "2026-10-07T10:00", "2026-10-07T11:00")
    private val lunch = timed(3, "2026-10-07T13:00", "2026-10-07T14:00")
    private val swim = timed(4, "2026-10-07T18:30", "2026-10-07T19:30")

    @Test
    fun `what is left today counts the event under way, and ends with the last one`() {
        val day = agenda(run, review, lunch, swim, now = at("2026-10-07T10:20"))
        assertThat(CardSummary.of(day)).isEqualTo(CardSummary(3, at("2026-10-07T19:30")))
    }

    @Test
    fun `an event over is not left, and the last end is the latest, not the last to start`() {
        // A long afternoon that outlasts the swim ends the day at 20:00.
        val workshop = timed(5, "2026-10-07T14:00", "2026-10-07T20:00")
        val day = agenda(run, review, workshop, swim, now = at("2026-10-07T15:00"))
        assertThat(CardSummary.of(day)).isEqualTo(CardSummary(2, at("2026-10-07T20:00")))
    }

    @Test
    fun `an event running past midnight ends the day on its own date`() {
        val train = timed(6, "2026-10-07T22:30", "2026-10-08T06:30")
        val day = agenda(train, now = at("2026-10-07T23:00"))
        assertThat(CardSummary.of(day)).isEqualTo(CardSummary(1, at("2026-10-08T06:30")))
    }

    @Test
    fun `nothing left, or nothing timed, is none, since the list says it already`() {
        assertThat(CardSummary.of(agenda(run, review, now = at("2026-10-07T21:00")))).isNull()
        assertThat(CardSummary.of(agenda(allDay(7, "2026-10-07"), now = at("2026-10-07T10:20")))).isNull()
        assertThat(CardSummary.of(agenda(now = at("2026-10-07T10:20")))).isNull()
    }

    @Test
    fun `tomorrow's events are not today's`() {
        val tomorrow = timed(8, "2026-10-08T09:00", "2026-10-08T10:00")
        assertThat(CardSummary.of(agenda(tomorrow, now = at("2026-10-07T10:20")))).isNull()
    }
}
