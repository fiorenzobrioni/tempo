package com.callbackdev.tempo.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.calendar.CalendarChanges
import com.callbackdev.tempo.core.calendar.CalendarDoors
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
import com.callbackdev.tempo.core.domain.today.TodayMonth
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
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
 * - where the page shows the current month beside the clock, on a large screen ([showMonth]), that
 *   month's dates asked for in the same read and its marks built with the agenda: one query for
 *   the two, on the same observer and the same ticker, and on a phone the read is the agenda's
 *   alone (step 10.4);
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

    /** Whether the page shows the current month, as it does in two panes: only then is it read. */
    private val showsMonth = MutableStateFlow(false)

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
     *
     * @property month the month whose dates the read holds besides the agenda's; null where the
     *   page shows none.
     */
    private data class Snapshot(val read: CalendarRead, val month: YearMonth?, val doors: CalendarDoors)

    private val snapshots: Flow<Snapshot> = combine(
        ticks.map { it.toLocalDate() to it.zone }.distinctUntilChanged(),
        settings.map { it.horizonDays }.distinctUntilChanged(),
        showsMonth,
        merge(rereads.map { }, changes.flow(restarts = rereads)),
    ) { day, days, withMonth, _ -> Triple(day.second, days, withMonth) }
        .mapLatest { (zone, days, withMonth) ->
            val now = ZonedDateTime.now(zone)
            val agenda = AgendaWindow.of(now.toInstant(), zone, days)
            val month = if (withMonth) YearMonth.from(now) else null
            Snapshot(
                read = source.read(month?.let { TodayMonth.window(agenda, it, zone) } ?: agenda),
                month = month,
                doors = CalendarDoors.of(context, now.toInstant()),
            )
        }

    // Built off the main thread: at each minute, and with the month's marks on a large screen.
    val state: StateFlow<TodayUiState?> = combine(ticks, settings, snapshots, awaiting, ::build)
        .flowOn(Dispatchers.Default)
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

    /**
     * Whether the page shows the current month beside the clock: said by the page, which knows
     * its panes. Its dates are read from then on, with the agenda's, and no longer once it says
     * otherwise (a tablet turned, a foldable closed).
     */
    fun showMonth(shown: Boolean) {
        showsMonth.value = shown
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
                    val filter = CalendarChoices.agendaFilter(settings, read.calendars)
                    // The read is wider than the agenda where it holds the month too: the builder
                    // trims each to its own dates.
                    val agenda = AgendaBuilder.build(
                        instances = read.instances,
                        calendars = read.calendars,
                        filter = filter,
                        now = now.toInstant(),
                        zone = now.zone,
                        days = settings.horizonDays,
                    )
                    TodayContent.Ready(
                        agenda = agenda,
                        sentence = DaySentence.today(agenda),
                        calendars = read.calendars.associateBy { it.id },
                        month = snapshot.month?.let { month ->
                            TodayMonth.of(month, read.instances, read.calendars, filter, now.toInstant(), now.zone)
                        },
                    )
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val REVEAL_WINDOW: Duration = Duration.ofMinutes(1)
    }
}
