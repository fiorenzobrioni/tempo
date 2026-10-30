package com.callbackdev.tempo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.callbackdev.tempo.core.domain.widget.CardAlarm
import com.callbackdev.tempo.core.domain.widget.CardLine
import com.callbackdev.tempo.core.domain.widget.WordsDay
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The cards' words for an event's end (a time today, its day as well on a later date) and the next alarm. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB")
class CardTextTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun event(id: Long, from: String, to: String, title: String) = EventInstance(
        eventId = id,
        calendarId = WidgetSamples.instances.first().calendarId,
        title = title,
        location = null,
        begin = WidgetSamples.at(from).toInstant(),
        end = WidgetSamples.at(to).toInstant(),
        allDay = false,
        color = null,
    )

    private fun words(now: String, vararg events: EventInstance): Pair<CardText, WidgetContent.Ready> {
        val model = WidgetSamples.model(
            look = WidgetLook(clockFormat = ClockFormat.H24),
            now = WidgetSamples.at(now),
            instances = events.toList(),
        )
        return CardText(context, model) to model.content as WidgetContent.Ready
    }

    @Test
    fun `an event under way until tomorrow says its day, on both cards`() {
        val train = event(20, "2026-10-07T22:30", "2026-10-08T06:30", "Night train")
        val (text, content) = words("2026-10-07T23:00", train)
        val focus = WordsDay.of(content.agenda, showAllDay = true, showDaysAhead = true).focus
        assertThat(text.whenOptions(focus))
            .containsExactly("Until tomorrow at 06:30", "→ Tomorrow · 06:30", "→ Tomorrow").inOrder()
        val today = content.agenda.days.first()
        assertThat(text.start(CardLine.Timed(today.timed.single(), today.date))).isEqualTo("Until tomorrow at 06:30")
    }

    @Test
    fun `an event under way until later today says its time alone`() {
        val review = event(21, "2026-10-07T10:00", "2026-10-07T11:00", "Design review")
        val (text, content) = words("2026-10-07T10:20", review)
        val focus = WordsDay.of(content.agenda, showAllDay = true, showDaysAhead = true).focus
        assertThat(text.whenOptions(focus)).containsExactly("Until 11:00", "→ 11:00").inOrder()
    }

    @Test
    fun `an inline row's short form says the same end in fewer letters`() {
        val review = event(21, "2026-10-07T10:00", "2026-10-07T11:00", "Design review")
        val train = event(20, "2026-10-07T22:30", "2026-10-08T06:30", "Night train")
        val (text, content) = words("2026-10-07T10:20", review)
        val today = content.agenda.days.first()
        val row = CardLine.Timed(today.timed.single(), today.date)
        assertThat(text.start(row)).isEqualTo("Until 11:00")
        assertThat(text.short(row, startOnly = true)).isEqualTo("→ 11:00")
        assertThat(text.short(row, startOnly = false)).isEqualTo("→ 11:00")
        val (late, night) = words("2026-10-07T23:00", train)
        val tonight = night.agenda.days.first()
        assertThat(late.short(CardLine.Timed(tonight.timed.single(), tonight.date), startOnly = true))
            .isEqualTo("→ Tomorrow · 06:30")
    }

    @Test
    fun `a row with no end to say keeps its time in the short form`() {
        val lunch = event(22, "2026-10-07T13:00", "2026-10-07T14:00", "Lunch")
        val (text, content) = words("2026-10-07T10:20", lunch)
        val today = content.agenda.days.first()
        val row = CardLine.Timed(today.timed.single(), today.date)
        assertThat(text.short(row, startOnly = true)).isEqualTo("13:00")
        assertThat(text.short(row, startOnly = false)).isEqualTo("13:00 – 14:00")
    }

    @Test
    fun `a night's end under the next day's heading is a bare time, long or short`() {
        // Thursday 22:30 to Friday 06:30, seen on Wednesday: Friday's line says when it ends.
        val train = event(23, "2026-10-08T22:30", "2026-10-09T06:30", "Night train")
        val (text, content) = words("2026-10-07T10:20", train)
        val friday = content.agenda.days.first { it.date.dayOfMonth == 9 }
        val row = CardLine.Timed(friday.timed.single(), friday.date)
        assertThat(text.start(row)).isEqualTo("Until 06:30")
        assertThat(text.short(row, startOnly = true)).isEqualTo("→ 06:30")
    }

    @Test
    fun `the alarm is a clock time, and tomorrow's says its day where there is room`() {
        val (text, _) = words("2026-10-07T22:00")
        val tomorrow = CardAlarm(WidgetSamples.at("2026-10-08T07:00").toInstant(), AlarmDay.Tomorrow)
        assertThat(text.alarm(tomorrow)).containsExactly("Tomorrow 07:00", "07:00").inOrder()
        assertThat(text.describe(tomorrow)).contains("tomorrow")
        val tonight = CardAlarm(WidgetSamples.at("2026-10-07T23:30").toInstant(), AlarmDay.Today)
        assertThat(text.alarm(tonight)).containsExactly("23:30")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, the alarm and the short end`() {
        val review = event(21, "2026-10-07T10:00", "2026-10-07T11:00", "Design review")
        val (text, content) = words("2026-10-07T10:20", review)
        val today = content.agenda.days.first()
        val row = CardLine.Timed(today.timed.single(), today.date)
        assertThat(text.start(row)).isEqualTo("Fino alle 11:00")
        assertThat(text.short(row, startOnly = true)).isEqualTo("→ 11:00")
        val tomorrow = CardAlarm(WidgetSamples.at("2026-10-08T07:00").toInstant(), AlarmDay.Tomorrow)
        assertThat(text.alarm(tomorrow)).containsExactly("Domani 07:00", "07:00").inOrder()
    }
}
