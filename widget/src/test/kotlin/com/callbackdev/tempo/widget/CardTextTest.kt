package com.callbackdev.tempo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.domain.widget.CardLine
import com.callbackdev.tempo.core.domain.widget.WordsDay
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The cards' words for an event's end: a time today, its day as well on a later date. */
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
}
