package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DaySentenceTest {
    private val standup = timed(1, "2026-10-07T09:30", "2026-10-07T10:30", title = "Standup")
    private val review = timed(2, "2026-10-07T10:00", "2026-10-07T10:15", title = "Review")
    private val dentist = timed(3, "2026-10-07T15:00", "2026-10-07T16:00", title = "Dentist")
    private val gym = timed(4, "2026-10-07T18:00", "2026-10-07T19:00", title = "Gym")
    private val tomorrow = timed(5, "2026-10-08T09:00", "2026-10-08T10:00", title = "Call")

    @Test
    fun `under way, the one that ends first, and how many are left`() {
        val result = DaySentence.today(agenda(standup, review, dentist, gym, now = at("2026-10-07T10:05")))
        assertThat(result).isEqualTo(TodaySentence.UnderWay(review, laterToday = 2))
    }

    @Test
    fun `within the hour, the minutes until it`() {
        val result = DaySentence.today(agenda(dentist, gym, now = at("2026-10-07T14:20")))
        assertThat(result).isEqualTo(TodaySentence.Soon(dentist, minutes = 40, laterToday = 1))
    }

    @Test
    fun `seconds round up, never to zero`() {
        val result = DaySentence.today(agenda(dentist, now = at("2026-10-07T14:59:10")))
        assertThat(result).isEqualTo(TodaySentence.Soon(dentist, minutes = 1, laterToday = 0))
    }

    @Test
    fun `further away, free until it`() {
        val result = DaySentence.today(agenda(standup, dentist, gym, now = at("2026-10-07T11:00")))
        assertThat(result).isEqualTo(TodaySentence.FreeUntil(dentist, laterToday = 1))
    }

    @Test
    fun `today done, tomorrow's first event`() {
        val result = DaySentence.today(agenda(dentist, tomorrow, now = at("2026-10-07T20:00")))
        assertThat(result).isEqualTo(TodaySentence.AllDone(Tomorrow.StartsWith(tomorrow)))
    }

    @Test
    fun `a free day, and a free tomorrow`() {
        val result = DaySentence.today(agenda(allDay(9, "2026-10-07"), now = at("2026-10-07T08:00")))
        assertThat(result).isEqualTo(TodaySentence.FreeDay(Tomorrow.Free))
    }

    @Test
    fun `with today alone on the horizon, tomorrow is not guessed at`() {
        val result = DaySentence.today(agenda(tomorrow, now = at("2026-10-07T20:00"), days = 1))
        assertThat(result).isEqualTo(TodaySentence.FreeDay(Tomorrow.NotShown))
    }

    @Test
    fun `tomorrow starts with its own first event, not one carried over from tonight`() {
        val lateShow = timed(6, "2026-10-07T23:00", "2026-10-08T01:00", title = "Late show")
        val result = DaySentence.today(agenda(lateShow, tomorrow, now = at("2026-10-07T23:30")))
        assertThat(result).isEqualTo(TodaySentence.UnderWay(lateShow, laterToday = 0))
        val later = DaySentence.today(agenda(dentist, lateShow, tomorrow, now = at("2026-10-07T16:30")))
        assertThat(later).isEqualTo(TodaySentence.FreeUntil(lateShow, laterToday = 0))
    }

    @Test
    fun `a day's summary counts, and spans first start to last end`() {
        val summary = DaySummary.of(
            agenda(standup, dentist, gym, allDay(9, "2026-10-07"), now = at("2026-10-06T12:00")).days[1],
        )
        assertThat(summary.timedCount).isEqualTo(3)
        assertThat(summary.allDayCount).isEqualTo(1)
        assertThat(summary.first).isEqualTo(at("2026-10-07T09:30"))
        assertThat(summary.lastEnd).isEqualTo(at("2026-10-07T19:00"))
    }

    @Test
    fun `an empty day's summary is empty`() {
        assertThat(DaySummary.of(agenda(now = at("2026-10-07T12:00")).days[1]).isEmpty).isTrue()
    }
}
