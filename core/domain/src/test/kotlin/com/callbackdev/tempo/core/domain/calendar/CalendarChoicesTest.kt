package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.calendar
import com.callbackdev.tempo.core.domain.agenda.day
import com.callbackdev.tempo.core.domain.agenda.timed
import com.callbackdev.tempo.core.model.CalendarChoice
import com.callbackdev.tempo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalendarChoicesTest {
    @Test
    fun `with no word from the reader, the calendar app decides`() {
        assertThat(CalendarChoices.isShown(calendar(1, visible = true), emptyList())).isTrue()
        assertThat(CalendarChoices.isShown(calendar(2, visible = false), emptyList())).isFalse()
    }

    @Test
    fun `the reader's word wins over the calendar app's`() {
        val hiddenInApp = calendar(2, visible = false)
        val settings = CalendarChoices.choose(UserSettings(), hiddenInApp, shown = true)
        assertThat(CalendarChoices.isShown(hiddenInApp, settings.calendarChoices)).isTrue()
    }

    @Test
    fun `choosing again replaces the choice, never doubles it`() {
        val family = calendar(1)
        val once = CalendarChoices.choose(UserSettings(), family, shown = false)
        val twice = CalendarChoices.choose(once, family, shown = true)
        assertThat(twice.calendarChoices).hasSize(1)
        assertThat(twice.calendarChoices.single().shown).isTrue()
    }

    @Test
    fun `a calendar renamed in its app keeps the reader's choice`() {
        val settings = CalendarChoices.choose(UserSettings(), calendar(1), shown = false)
        val renamed = calendar(1).copy(name = "Family, renamed")
        assertThat(CalendarChoices.isShown(renamed, settings.calendarChoices)).isFalse()
    }

    @Test
    fun `a restored phone finds the choice by account and name, under a new id`() {
        val settings = CalendarChoices.choose(UserSettings(), calendar(1), shown = false)
        val restored = calendar(1).copy(id = 57)
        assertThat(CalendarChoices.isShown(restored, settings.calendarChoices)).isFalse()
    }

    @Test
    fun `the same id in another account is another calendar`() {
        val choice = CalendarChoice(1, "com.google", "work@example.com", "Calendar 1", shown = false)
        assertThat(CalendarChoices.choiceFor(calendar(1), listOf(choice))).isNull()
    }

    @Test
    fun `the agenda's filter follows the choices of the calendars the phone has`() {
        val calendars = listOf(calendar(1), calendar(2, visible = false), calendar(3))
        var settings = UserSettings(showDeclined = true, showAllDay = false)
        settings = CalendarChoices.choose(settings, calendars[0], shown = false)
        settings = CalendarChoices.choose(settings, calendars[1], shown = true)
        val filter = CalendarChoices.agendaFilter(settings, calendars)
        assertThat(filter.hiddenCalendars).containsExactly(1L)
        assertThat(filter.shownCalendars).containsExactly(2L)
        assertThat(filter.showDeclined).isTrue()
        assertThat(filter.showAllDay).isFalse()
    }

    @Test
    fun `hiding a calendar takes its events off the agenda at once`() {
        val calendars = listOf(calendar(1), calendar(2))
        val events = arrayOf(
            timed(1, "2026-10-07T10:00", "2026-10-07T11:00", calendarId = 1),
            timed(2, "2026-10-07T12:00", "2026-10-07T13:00", calendarId = 2),
        )
        val settings = CalendarChoices.choose(UserSettings(), calendars[1], shown = false)
        val result = agenda(
            *events,
            now = at("2026-10-07T09:00"),
            calendars = calendars,
            filter = CalendarChoices.agendaFilter(settings, calendars),
        )
        assertThat(result.day("2026-10-07").timed.map { it.event.calendarId }).containsExactly(1L)
    }
}
