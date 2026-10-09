package com.callbackdev.tempo.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.calendar.CalendarRead
import com.callbackdev.tempo.core.calendar.CalendarSource
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.widget.refresh.WidgetRefreshArming
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a card knows when it draws: the moment, the reader's settings and this card's look, and
 * what the calendar says, read at render time from the provider as Today reads it (the same
 * `AgendaBuilder`, so the card and the screen never tell two stories about one day).
 *
 * @property frozenClock the samples' drawing of the time as plain text at [now], so a preview and
 *   a picture show the sample's time; a placed card always draws the system's live `TextClock`.
 */
data class WidgetModel(
    val look: WidgetLook,
    val settings: UserSettings,
    val now: ZonedDateTime,
    val content: WidgetContent,
    val doors: WidgetDoors,
    val frozenClock: Boolean = false,
) {
    companion object {
        /** A card that says it could not read the calendar, in the default dress. */
        fun unavailable(): WidgetModel = WidgetModel(
            look = WidgetLook(),
            settings = UserSettings(),
            now = ZonedDateTime.now(),
            content = WidgetContent.Unavailable,
            doors = WidgetDoors.None,
        )
    }
}

/** What the agenda's place on a card holds: the day, or the one thing that stands in for it. */
sealed interface WidgetContent {
    data class Ready(val agenda: Agenda, val calendars: Map<Long, CalendarInfo>) : WidgetContent

    /** `READ_CALENDAR` is not granted: the clock still works, the rest says so (VISION.md). */
    data object NoPermission : WidgetContent

    data object NoCalendars : WidgetContent

    data object AllHidden : WidgetContent

    /** The read failed or did not finish in time: said, not waited on. */
    data object Unavailable : WidgetContent
}

/**
 * The doors a card's touches can open (PLANNING.md §4.7): a touch whose intent nothing takes falls
 * back to Tempo, never to nothing.
 *
 * @property clockPackage the phone's clock app (`ClockApp`), or null when there is none.
 */
data class WidgetDoors(val canOpenEvent: Boolean, val canOpenDay: Boolean, val clockPackage: String?) {
    companion object {
        val None = WidgetDoors(canOpenEvent = false, canOpenDay = false, clockPackage = null)
    }
}

/**
 * Reads a [WidgetModel], and arms what keeps the card fresh once it is drawn: the calendar's
 * content-URI trigger and the alarm at the next boundary ([WidgetRefreshArming], PLANNING.md §7).
 */
@Singleton
class WidgetModelLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val source: CalendarSource,
    private val looks: WidgetLookStore,
    private val arming: WidgetRefreshArming,
) {
    suspend fun load(appWidgetId: Int): WidgetModel {
        val settings = settings.settings.first()
        val look = looks.lookFor(appWidgetId)
        val now = ZonedDateTime.now()
        val read = source.read(AgendaWindow.of(now.toInstant(), now.zone, settings.horizonDays))
        val content = when (read) {
            CalendarRead.NoPermission -> WidgetContent.NoPermission

            is CalendarRead.Read -> when {
                read.calendars.isEmpty() -> WidgetContent.NoCalendars

                read.calendars.none { CalendarChoices.isShown(it, settings.calendarChoices) } -> WidgetContent.AllHidden

                else -> WidgetContent.Ready(
                    agenda = AgendaBuilder.build(
                        instances = read.instances,
                        calendars = read.calendars,
                        filter = CalendarChoices.agendaFilter(settings, read.calendars),
                        now = now.toInstant(),
                        zone = now.zone,
                        days = settings.horizonDays,
                    ),
                    calendars = read.calendars.associateBy { it.id },
                )
            }
        }
        arming.afterRender((content as? WidgetContent.Ready)?.agenda, now.zone)
        return WidgetModel(look, settings, now, content, doors(now.toInstant()))
    }

    /**
     * [load] for a card waiting to be drawn: never longer than [LOAD_TIMEOUT_MILLIS] and never an
     * exception, because Glance shows its loading spinner until `provideContent` is reached and a
     * card stuck on it says nothing to anybody (Passo's device report, 25 Sep 2026).
     */
    suspend fun loadForCard(appWidgetId: Int): WidgetModel {
        val started = SystemClock.elapsedRealtime()
        val model = guardedLoad(LOAD_TIMEOUT_MILLIS, "widget $appWidgetId") { load(appWidgetId) }
        logWidget("Widget $appWidgetId read in ${SystemClock.elapsedRealtime() - started} ms")
        return model
    }

    private fun doors(now: Instant) = WidgetDoors(
        canOpenEvent = CalendarIntents.canOpen(context, CalendarIntents.view(ProbeEvent)),
        canOpenDay = CalendarIntents.canOpen(context, CalendarIntents.openDay(now)),
        clockPackage = ClockApp.packageName(context),
    )

    companion object {
        /** The log tag of everything the widgets report: `adb logcat -s TempoWidget`. */
        const val TAG: String = "TempoWidget"

        /** A read is two queries on the phone's own storage; ten seconds is something wrong. */
        const val LOAD_TIMEOUT_MILLIS: Long = 10_000L

        /** Any event: only the kind of intent matters to whether an app takes it. */
        private val ProbeEvent = EventInstance(
            eventId = 1,
            calendarId = 1,
            title = null,
            location = null,
            begin = Instant.EPOCH,
            end = Instant.EPOCH,
            allDay = false,
            color = null,
        )
    }
}

/**
 * [load], bounded by [timeoutMillis] and never throwing but for cancellation: a failure is logged
 * as [what] and becomes [WidgetModel.unavailable]. Split off the loader so a test can pin it.
 */
internal suspend fun guardedLoad(timeoutMillis: Long, what: String, load: suspend () -> WidgetModel): WidgetModel =
    try {
        withTimeout(timeoutMillis) { load() }
    } catch (e: TimeoutCancellationException) {
        logWidgetFailure("Reading the calendar for $what took over $timeoutMillis ms", e)
        WidgetModel.unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logWidgetFailure("Reading the calendar for $what failed", e)
        WidgetModel.unavailable()
    }

/** The log line of a failed read; quiet on a JVM without Android's log, where the tests run. */
internal fun logWidgetFailure(message: String, error: Throwable) {
    runCatching { Log.w(WidgetModelLoader.TAG, message, error) }
}

/**
 * The refresh's timeline, one line a step (what asked, how long the read and the drawing took):
 * `adb logcat -s TempoWidget` tells on a phone where a late card waited. A few lines per calendar
 * change, nothing while nothing changes.
 */
internal fun logWidget(message: String) {
    runCatching { Log.i(WidgetModelLoader.TAG, message) }
}
