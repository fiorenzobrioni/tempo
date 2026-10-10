package com.callbackdev.tempo.feature.today

import android.app.AlarmManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.calendar.CalendarChanges
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.calendar.CalendarRead
import com.callbackdev.tempo.core.calendar.CalendarSource
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.clock.untilNextMinute
import com.callbackdev.tempo.core.domain.today.DaySentence
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
import kotlinx.coroutines.launch
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
 * - the agenda and its sentence built from the read at every tick, so "now" moves without a read.
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

    /** A read and the doors it found open, together: both change when the reader comes back. */
    private data class Snapshot(val read: CalendarRead, val doors: CalendarDoors)

    private val snapshots: Flow<Snapshot> = combine(
        ticks.map { it.toLocalDate() to it.zone }.distinctUntilChanged(),
        settings.map { it.horizonDays }.distinctUntilChanged(),
        merge(rereads.map { }, changes.flow()),
    ) { day, days, _ -> day to days }
        .mapLatest { (day, days) ->
            val now = ZonedDateTime.now(day.second)
            Snapshot(source.read(AgendaWindow.of(now.toInstant(), now.zone, days)), doors(now.toInstant()))
        }

    val state: StateFlow<TodayUiState?> = combine(ticks, settings, snapshots, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Back on the page: the permission, the calendars and the calendar app may all have changed. */
    fun reread() {
        rereads.value++
    }

    /** The permission was asked: remembered, so a later refusal can be told apart (§15). */
    fun onPermissionAnswered() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(askedCalendarPermission = true) }
            reread()
        }
    }

    private fun build(now: ZonedDateTime, settings: UserSettings, snapshot: Snapshot): TodayUiState {
        val content = when (val read = snapshot.read) {
            CalendarRead.NoPermission -> TodayContent.NoPermission

            is CalendarRead.Read -> when {
                read.calendars.isEmpty() -> TodayContent.NoCalendars

                read.calendars.none { CalendarChoices.isShown(it, settings.calendarChoices) } -> TodayContent.AllHidden

                else -> {
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
        )
    }

    /**
     * The phone's next alarm, as the system's clock app set it (no permission needed). Read at each
     * tick: one call into a system service a minute, while the page is visible.
     */
    private fun nextAlarm(): Instant? = context.getSystemService(AlarmManager::class.java)
        ?.nextAlarmClock
        ?.triggerTime
        ?.let(Instant::ofEpochMilli)

    private fun doors(now: Instant): CalendarDoors = CalendarDoors(
        canCreate = CalendarIntents.canOpen(context, CalendarIntents.insert(now)),
        canOpenEvent = CalendarIntents.canOpen(context, CalendarIntents.view(ProbeEvent)),
        canOpenDay = CalendarIntents.canOpen(context, CalendarIntents.openDay(now)),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

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
