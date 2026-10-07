package com.callbackdev.tempo.core.calendar

import android.Manifest
import android.app.Application
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.time.Instant

/** The provider's queries through the real ContentResolver path (PLANNING.md §4.1). */
@RunWith(AndroidJUnit4::class)
class CalendarSourceTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val source = CalendarSource(app)
    private val window = AgendaWindow(Instant.parse("2026-10-06T22:00:00Z"), Instant.parse("2026-10-14T22:00:00Z"))

    @Before
    fun setUp() {
        FakeCalendarProvider.reset()
        Robolectric.setupContentProvider(FakeCalendarProvider::class.java, CalendarContract.AUTHORITY)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
    }

    @After
    fun tearDown() = FakeCalendarProvider.reset()

    @Test
    fun `without the permission nothing is asked of the provider`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        assertThat(source.read(window)).isEqualTo(CalendarRead.NoPermission)
        assertThat(FakeCalendarProvider.queries).isEmpty()
    }

    @Test
    fun `a permission revoked during the read is a state, not a crash`() = runTest {
        FakeCalendarProvider.revoked = true
        assertThat(source.read(window)).isEqualTo(CalendarRead.NoPermission)
    }

    @Test
    fun `the instances are asked for the window, in the provider's own URI`() = runTest {
        source.read(window)
        val instancesUri = FakeCalendarProvider.queries.single { it.pathSegments.first() == "instances" }
        assertThat(instancesUri.pathSegments).containsExactly(
            "instances",
            "when",
            window.start.toEpochMilli().toString(),
            window.end.toEpochMilli().toString(),
        ).inOrder()
    }

    @Test
    fun `calendars and instances are read by column name`() = runTest {
        FakeCalendarProvider.calendars = listOf(
            mapOf(
                Calendars._ID to 3L,
                Calendars.CALENDAR_DISPLAY_NAME to "Family",
                Calendars.ACCOUNT_NAME to "reader@example.com",
                Calendars.ACCOUNT_TYPE to "com.google",
                Calendars.CALENDAR_COLOR to 0xFF0B8043.toInt(),
                Calendars.VISIBLE to 0,
                Calendars.IS_PRIMARY to 1,
            ),
        )
        FakeCalendarProvider.instances = listOf(
            mapOf(
                Instances.EVENT_ID to 42L,
                Instances.CALENDAR_ID to 3L,
                Instances.BEGIN to Instant.parse("2026-10-07T08:00:00Z").toEpochMilli(),
                Instances.END to Instant.parse("2026-10-07T09:00:00Z").toEpochMilli(),
                Instances.ALL_DAY to 0,
                Instances.TITLE to "Dentist",
                Instances.EVENT_LOCATION to "Via Roma 3",
                Instances.DISPLAY_COLOR to 0xFFD50000.toInt(),
                Instances.STATUS to Events.STATUS_CONFIRMED,
                Instances.SELF_ATTENDEE_STATUS to Attendees.ATTENDEE_STATUS_ACCEPTED,
                Instances.AVAILABILITY to Events.AVAILABILITY_BUSY,
                Instances.EVENT_TIMEZONE to "Europe/Rome",
            ),
        )
        val read = source.read(window) as CalendarRead.Read
        assertThat(read.calendars).containsExactly(
            CalendarInfo(
                3,
                "Family",
                "reader@example.com",
                "com.google",
                0xFF0B8043.toInt(),
                visibleInProvider = false,
                isPrimary = true,
            ),
        )
        val event = read.instances.single()
        assertThat(event.eventId).isEqualTo(42L)
        assertThat(event.calendarId).isEqualTo(3L)
        assertThat(event.title).isEqualTo("Dentist")
        assertThat(event.location).isEqualTo("Via Roma 3")
        assertThat(event.begin).isEqualTo(Instant.parse("2026-10-07T08:00:00Z"))
        assertThat(event.end).isEqualTo(Instant.parse("2026-10-07T09:00:00Z"))
        assertThat(event.allDay).isFalse()
        assertThat(event.color).isEqualTo(0xFFD50000.toInt())
        assertThat(event.selfStatus).isEqualTo(AttendeeStatus.ACCEPTED)
        assertThat(event.timeZone).isEqualTo("Europe/Rome")
    }

    @Test
    fun `an all-day event is read as stored, its midnights in UTC`() = runTest {
        FakeCalendarProvider.instances = listOf(
            instance(1, begin = "2026-10-07T00:00:00Z", end = "2026-10-08T00:00:00Z") + (Instances.ALL_DAY to 1),
        )
        val event = (source.read(window) as CalendarRead.Read).instances.single()
        assertThat(event.allDay).isTrue()
        assertThat(event.begin).isEqualTo(Instant.parse("2026-10-07T00:00:00Z"))
    }

    @Test
    fun `statuses, answers and availability keep their meaning`() = runTest {
        FakeCalendarProvider.instances = listOf(
            instance(1) + (Instances.STATUS to Events.STATUS_CANCELED),
            instance(2) + (Instances.SELF_ATTENDEE_STATUS to Attendees.ATTENDEE_STATUS_DECLINED),
            instance(3) + (Instances.AVAILABILITY to Events.AVAILABILITY_FREE),
            instance(4) + (Instances.STATUS to Events.STATUS_TENTATIVE),
        )
        val events = (source.read(window) as CalendarRead.Read).instances.associateBy { it.eventId }
        assertThat(events.getValue(1).status).isEqualTo(EventStatus.CANCELED)
        assertThat(events.getValue(2).selfStatus).isEqualTo(AttendeeStatus.DECLINED)
        assertThat(events.getValue(3).availability).isEqualTo(Availability.FREE)
        assertThat(events.getValue(4).status).isEqualTo(EventStatus.TENTATIVE)
    }

    @Test
    fun `a provider without some columns gives defaults, never a crash`() = runTest {
        FakeCalendarProvider.missing = setOf(
            Calendars.VISIBLE,
            Calendars.IS_PRIMARY,
            Calendars.CALENDAR_COLOR,
            Instances.DISPLAY_COLOR,
            Instances.STATUS,
            Instances.SELF_ATTENDEE_STATUS,
            Instances.AVAILABILITY,
            Instances.EVENT_TIMEZONE,
        )
        FakeCalendarProvider.calendars = listOf(mapOf(Calendars._ID to 1L, Calendars.ACCOUNT_NAME to "Phone"))
        FakeCalendarProvider.instances = listOf(instance(1))
        val read = source.read(window) as CalendarRead.Read
        val calendar = read.calendars.single()
        assertThat(calendar.name).isEqualTo("Phone")
        assertThat(calendar.visibleInProvider).isTrue()
        assertThat(calendar.color).isNull()
        val event = read.instances.single()
        assertThat(event.color).isNull()
        assertThat(event.status).isEqualTo(EventStatus.CONFIRMED)
        assertThat(event.selfStatus).isEqualTo(AttendeeStatus.NONE)
        assertThat(event.availability).isEqualTo(Availability.BUSY)
    }

    @Test
    fun `a blank title is no title, and a row without times is skipped`() = runTest {
        FakeCalendarProvider.instances = listOf(
            instance(1) + (Instances.TITLE to "  "),
            instance(2) + (Instances.BEGIN to null),
        )
        val events = (source.read(window) as CalendarRead.Read).instances
        assertThat(events.map { it.eventId }).containsExactly(1L)
        assertThat(events.single().title).isNull()
    }

    @Test
    fun `an end before the begin is read as a moment`() = runTest {
        FakeCalendarProvider.instances =
            listOf(instance(1, begin = "2026-10-07T10:00:00Z", end = "2026-10-07T09:00:00Z"))
        val event = (source.read(window) as CalendarRead.Read).instances.single()
        assertThat(event.end).isEqualTo(event.begin)
    }

    private fun instance(
        id: Long,
        begin: String = "2026-10-07T08:00:00Z",
        end: String = "2026-10-07T09:00:00Z",
    ): Map<String, Any?> = mapOf(
        Instances.EVENT_ID to id,
        Instances.CALENDAR_ID to 1L,
        Instances.BEGIN to Instant.parse(begin).toEpochMilli(),
        Instances.END to Instant.parse(end).toEpochMilli(),
        Instances.ALL_DAY to 0,
        Instances.TITLE to "Event $id",
    )
}
