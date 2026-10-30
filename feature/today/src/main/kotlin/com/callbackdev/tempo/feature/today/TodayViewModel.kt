package com.callbackdev.tempo.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.calendar.CalendarChanges
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.calendar.CalendarRead
import com.callbackdev.tempo.core.calendar.CalendarSource
import com.callbackdev.tempo.core.calendar.ClockApp
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarStanding
import com.callbackdev.tempo.core.domain.clock.untilNextMinute
import com.callbackdev.tempo.core.domain.today.DaySentence
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.NewEvents
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Today (PLANNING.md §11 Phase 3), live while visible and silent otherwise (§9):
 *
 * - a minute ticker, on each minute's start, that runs only while the page collects (the flows
 *   stop five seconds after the page goes: `WhileSubscribed`);
 * - the provider read again when the date or the zone changes (midnight, a trip), when the horizon
 *   changes, when the calendar changes (`CalendarChanges`, bound to the same collection) and when
 *   the reader comes back to the page ([reread]: the permission may have changed meanwhile);
 * - the agenda and its sentence built from the read at every tick, so "now" moves without a read;
 * - back from the calendar app's new-event page, the event the reader made, to bring into view
 *   ([onNewEventOpened]: the page's events kept in memory until then, nothing stored).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val source: CalendarSource,
    changes: CalendarChanges,
) : ViewModel() {
    private val rereads = MutableStateFlow(0)

    /**
     * The page's events when the reader opened the calendar app's new-event page, and when they
     * came back ([back], the first return since): what tells the event they made (step 8.6).
     */
    private data class Awaiting(val known: Set<EventKey>, val back: Instant? = null)

    private val awaiting = MutableStateFlow<Awaiting?>(null)

    private val ticks: Flow<ZonedDateTime> = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(untilNextMinute(Instant.now()).toMillis())
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private val settings: Flow<UserSettings> =
        settingsRepository.settings.shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            replay = 1,
        )

    /**
     * A read and the doors it found open, together: both change when the reader comes back (a
     * calendar or a clock app installed meanwhile).
     */
    private data class Snapshot(val read: CalendarRead, val doors: CalendarDoors)

    private val snapshots: Flow<Snapshot> = combine(
        ticks.map { it.toLocalDate() to it.zone }.distinctUntilChanged(),
        settings.map { it.horizonDays }.distinctUntilChanged(),
        merge(rereads.map { }, changes.flow(restarts = rereads)),
    ) { day, days, _ -> day to days }
        .mapLatest { (day, days) ->
            val now = ZonedDateTime.now(day.second)
            Snapshot(source.read(AgendaWindow.of(now.toInstant(), now.zone, days)), doors(now.toInstant()))
        }

    val state: StateFlow<TodayUiState?> = combine(ticks, settings, snapshots, awaiting, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /**
     * Back on the page: the permission, the calendars and the calendar app may all have changed.
     * The first return since a new event's page was opened starts the [REVEAL_WINDOW] in which the
     * event made is looked for; a later return ends it (the reader left that page without saving).
     */
    fun reread() {
        awaiting.update { it?.takeIf { awaiting -> awaiting.back == null }?.copy(back = Instant.now()) }
        rereads.value++
    }

    /** The calendar app's new-event page is opening: the page's events, as they are, kept in memory. */
    fun onNewEventOpened() {
        val ready = state.value?.content as? TodayContent.Ready ?: return
        awaiting.value = Awaiting(NewEvents.keys(ready.agenda))
    }

    /** The event made is in view: nothing more to look for. */
    fun onRevealed() {
        awaiting.value = null
    }

    /** The first day's tip, dismissed or followed to the guide: not shown again. */
    fun onTipSeen() {
        viewModelScope.launch { settingsRepository.update { it.copy(todayTipSeen = true) } }
    }

    /** The permission was asked: remembered, so a later refusal can be told apart (§15). */
    fun onPermissionAnswered() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(askedCalendarPermission = true) }
            reread()
        }
    }

    private fun build(
        now: ZonedDateTime,
        settings: UserSettings,
        snapshot: Snapshot,
        awaiting: Awaiting?,
    ): TodayUiState {
        val content = when (val read = snapshot.read) {
            CalendarRead.NoPermission -> TodayContent.NoPermission

            is CalendarRead.Read -> when (CalendarChoices.standing(read.calendars, settings.calendarChoices)) {
                CalendarStanding.NONE -> TodayContent.NoCalendars

                CalendarStanding.ALL_HIDDEN -> TodayContent.AllHidden

                CalendarStanding.SOME_SHOWN -> {
                    val agenda = AgendaBuilder.build(
                        instances = read.instances,
                        calendars = read.calendars,
                        filter = CalendarChoices.agendaFilter(settings, read.calendars),
                        now = now.toInstant(),
                        zone = now.zone,
                        days = settings.horizonDays,
                    )
                    TodayContent.Ready(agenda, DaySentence.today(agenda), read.calendars.associateBy { it.id })
                }
            }
        }
        return TodayUiState(
            now = now,
            settings = settings,
            alarm = if (settings.showNextAlarm) nextAlarm() else null,
            content = content,
            doors = snapshot.doors,
            reveal = reveal(content, awaiting),
        )
    }

    /**
     * The event the reader made, once they are back and for [REVEAL_WINDOW]: long enough for a
     * calendar app that writes the provider a moment after its page closes, short enough that an
     * event synced later from elsewhere does not move the list.
     */
    private fun reveal(content: TodayContent, awaiting: Awaiting?): EventKey? {
        val back = awaiting?.back ?: return null
        if (Instant.now().isAfter(back.plus(REVEAL_WINDOW))) return null
        val ready = content as? TodayContent.Ready ?: return null
        return NewEvents.first(ready.agenda, awaiting.known)
    }

    /**
     * The next alarm the reader set in a clock app ([ClockApp.nextAlarm]: a calendar app's reminder
     * set as an alarm clock is not one). Read at each tick: two calls into system services a minute,
     * while the page is visible.
     */
    private fun nextAlarm(): Instant? = ClockApp.nextAlarm(context)

    private fun doors(now: Instant): CalendarDoors = CalendarDoors(
        canCreate = CalendarIntents.canOpen(context, CalendarIntents.insert(now)),
        canOpenEvent = CalendarIntents.canOpen(context, CalendarIntents.view(ProbeEvent)),
        canOpenDay = CalendarIntents.canOpen(context, CalendarIntents.openDay(now)),
        clockPackage = ClockApp.packageName(context),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val REVEAL_WINDOW: Duration = Duration.ofMinutes(1)

        /** Any event: only the kind of intent matters to whether an app takes it. */
        val ProbeEvent = EventInstance(
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
