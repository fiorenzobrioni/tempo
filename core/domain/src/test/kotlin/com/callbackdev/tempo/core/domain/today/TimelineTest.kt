package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimelineTest {
    private val early = timed(1, "2026-10-07T08:00", "2026-10-07T08:30", title = "Early")
    private val standup = timed(2, "2026-10-07T09:30", "2026-10-07T10:00", title = "Standup")
    private val review = timed(3, "2026-10-07T11:00", "2026-10-07T11:30", title = "Review")
    private val dentist = timed(4, "2026-10-07T15:00", "2026-10-07T16:00", title = "Dentist")

    private fun kinds(items: List<TimelineItem>) = items.map {
        when (it) {
            is TimelineItem.Event -> it.entry.event.title.orEmpty()
            is TimelineItem.Free -> "free"
            is TimelineItem.Earlier -> "earlier ${it.entries.size}"
            TimelineItem.Now -> "now"
        }
    }

    @Test
    fun `the past folds, now draws the line, gaps of an hour sit between events`() {
        val day = agenda(early, standup, review, dentist, now = at("2026-10-07T10:30")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true)))
            .containsExactly("earlier 2", "now", "Review", "free", "Dentist").inOrder()
    }

    @Test
    fun `opened, the past is its own rows`() {
        val day = agenda(early, standup, review, now = at("2026-10-07T10:30")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true, showEarlier = true)))
            .containsExactly("earlier 2", "Early", "Standup", "now", "Review").inOrder()
    }

    @Test
    fun `one past event stays a row`() {
        val day = agenda(early, review, now = at("2026-10-07T10:30")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true))).containsExactly("Early", "now", "Review").inOrder()
    }

    @Test
    fun `on today every gap of an hour or more is a row`() {
        // 08:30 to 09:30 and 10:00 to 11:00 are an hour each: rows, as the 11:30 to 15:00 gap.
        val day = agenda(early, standup, review, dentist, now = at("2026-10-07T07:00")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true)))
            .containsExactly("now", "free", "Early", "free", "Standup", "free", "Review", "free", "Dentist").inOrder()
    }

    @Test
    fun `a gap from now comes right under the line`() {
        val day = agenda(dentist, now = at("2026-10-07T12:00")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true))).containsExactly("now", "free", "Dentist").inOrder()
    }

    @Test
    fun `an event under way follows the line`() {
        val day = agenda(early, review, now = at("2026-10-07T11:10")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true))).containsExactly("Early", "now", "Review").inOrder()
    }

    @Test
    fun `with nothing ahead there is no line to draw`() {
        val day = agenda(early, now = at("2026-10-07T20:00")).days.first()
        assertThat(kinds(Timeline.of(day, isToday = true))).containsExactly("Early")
    }

    @Test
    fun `another day has neither line nor fold, nor free rows`() {
        val day = agenda(early, standup, review, dentist, now = at("2026-10-06T12:00")).days[1]
        assertThat(kinds(Timeline.of(day, isToday = false)))
            .containsExactly("Early", "Standup", "Review", "Dentist").inOrder()
    }
}
