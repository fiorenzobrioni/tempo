package com.callbackdev.tempo.core.calendar

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant

/** The intents handed to the calendar app (PLANNING.md §4.7). */
@RunWith(AndroidJUnit4::class)
class CalendarIntentsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private val occurrence = EventInstance(
        eventId = 42,
        calendarId = 1,
        title = "Standup",
        location = null,
        begin = Instant.parse("2026-10-07T07:00:00Z"),
        end = Instant.parse("2026-10-07T07:30:00Z"),
        allDay = false,
        color = null,
    )

    @Test
    fun `an event opens at its own occurrence`() {
        val intent = CalendarIntents.view(occurrence)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("content://com.android.calendar/events/42")
        assertThat(
            intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0),
        ).isEqualTo(occurrence.begin.toEpochMilli())
        assertThat(
            intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0),
        ).isEqualTo(occurrence.end.toEpochMilli())
    }

    @Test
    fun `a new event starts where asked and lasts an hour`() {
        val begin = Instant.parse("2026-10-07T08:30:00Z")
        val intent = CalendarIntents.insert(begin)
        assertThat(intent.action).isEqualTo(Intent.ACTION_INSERT)
        assertThat(intent.data).isEqualTo(CalendarContract.Events.CONTENT_URI)
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0)).isEqualTo(begin.toEpochMilli())
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
            .isEqualTo(Instant.parse("2026-10-07T09:30:00Z").toEpochMilli())
    }

    @Test
    fun `a day opens through the provider's time URI`() {
        val moment = Instant.parse("2026-10-07T10:00:00Z")
        val intent = CalendarIntents.openDay(moment)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data.toString()).isEqualTo("content://com.android.calendar/time/${moment.toEpochMilli()}")
    }

    @Test
    fun `a button is drawn only if an app takes its intent`() {
        val intent = CalendarIntents.insert(Instant.parse("2026-10-07T08:30:00Z"))
        assertThat(CalendarIntents.canOpen(app, intent)).isFalse()
        val calendarApp = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "org.example.calendar"
                name = "org.example.calendar.EditEventActivity"
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(intent, calendarApp)
        assertThat(CalendarIntents.canOpen(app, intent)).isTrue()
    }
}
