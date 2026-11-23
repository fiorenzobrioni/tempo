package com.callbackdev.tempo.feature.month

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.calendar.CalendarChanges
import com.callbackdev.tempo.core.calendar.CalendarDoors
import com.callbackdev.tempo.core.calendar.CalendarRead
import com.callbackdev.tempo.core.calendar.CalendarSource
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarStanding
import com.callbackdev.tempo.core.domain.clock.untilNextMinute
import com.callbackdev.tempo.core.domain.month.MonthMarks
import com.callbackdev.tempo.core.domain.month.MonthPages
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.NewEvent
import com.callbackdev.tempo.core.domain.today.NewEvents
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * The month (PLANNING.md §11 Phase 9), live while visible and silent otherwise (§9), as Today is:
 *
 * - the provider read for the month on screen and the two beside it ([MonthPages.span]), so the
 *   page under the finger has its marks before it is turned to; read again when the reader turns
 *   to another month ([show]), when the calendar changes (`CalendarChanges`, bound to the page's
 *   collection), when the phone's zone changes, and when the reader comes back ([reread]);
 * - the dates built from the read at every minute's start, by the agenda's own builder, so a
 *   chosen day reads as it would on Today and "now" moves without a read; off the main thread: a
 *   quarter of a year is more dates than Today's week;
 * - back from the calendar app's new-event page, the event the reader made, to bring forward
 *   ([onNewEventOpened]: the dates' events kept in memory until then, nothing stored).
 *
 * Nothing runs once the page is gone: the flows stop five seconds after it (`WhileSubscribed`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val source: CalendarSource,
    changes: CalendarChanges,
) : ViewModel() {
    private val rereads = MutableStateFlow(0)

    /** The month on screen; null until the page says, and then today's. */
    private val shown = MutableStateFlow<YearMonth?>(null)

    /**
     * The events of the dates read when the reader opened the calendar app's new-event page, the
     * dates themselves ([span]: turned to another month, every event of it would look new), and
     * when they came back ([back], the first return since).
     */
    private data class Awaiting(val known: Set<EventKey>, val span: ClosedRange<LocalDate>, val back: Instant? = null)

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

    /** A read, the dates it was for, and the doors it found open. */
    private data class Snapshot(val span: ClosedRange<LocalDate>, val read: CalendarRead, val doors: CalendarDoors)

    private val snapshots: Flow<Snapshot> = combine(
        ticks.map { it.zone }.distinctUntilChanged(),
        shown,
        merge(rereads.map { }, changes.flow(restarts = rereads)),
    ) { zone, month, _ -> zone to month }
        .mapLatest { (zone, month) ->
            val now = ZonedDateTime.now(zone)
            val span = MonthPages.span(month ?: YearMonth.from(now))
            Snapshot(
                span = span,
                read = source.read(AgendaWindow.of(span.start, span.endInclusive, zone)),
                doors = CalendarDoors.of(context, now.toInstant()),
            )
        }

    val state: StateFlow<MonthUiState?> = combine(ticks, settings, snapshots, awaiting, ::build)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The reader turned to [month]: its dates, and its neighbours', are read. */
    fun show(month: YearMonth) {
        shown.value = month
    }

    /**
     * Back on the page: the permission, the calendars and the calendar app may all have changed.
     * The first return since a new event's page was opened starts the [REVEAL_WINDOW] in which the
     * event made is looked for; a later return ends it (the reader left that page without saving).
     */
    fun reread() {
        awaiting.update { it?.takeIf { awaiting -> awaiting.back == null }?.copy(back = Instant.now()) }
        rereads.value++
    }

    /** The calendar app's new-event page is opening: the dates' events, as they are, kept in memory. */
    fun onNewEventOpened() {
        val ready = state.value?.content as? MonthContent.Ready ?: return
        val dates = ready.days.keys
        if (dates.isEmpty()) return
        awaiting.value = Awaiting(NewEvents.keys(ready.days.values.toList()), dates.min()..dates.max())
    }

    /** The event made is on screen: nothing more to look for. */
    fun onRevealed() {
        awaiting.value = null
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
    ): MonthUiState {
        val content = when (val read = snapshot.read) {
            CalendarRead.NoPermission -> MonthContent.NoPermission

            is CalendarRead.Read -> when (CalendarChoices.standing(read.calendars, settings.calendarChoices)) {
                CalendarStanding.NONE -> MonthContent.NoCalendars

                CalendarStanding.ALL_HIDDEN -> MonthContent.AllHidden

                CalendarStanding.SOME_SHOWN -> {
                    val calendars = read.calendars.associateBy { it.id }
                    val days = AgendaBuilder.days(
                        instances = read.instances,
                        calendars = read.calendars,
                        filter = CalendarChoices.agendaFilter(settings, read.calendars),
                        now = now.toInstant(),
                        zone = now.zone,
                        from = snapshot.span.start,
                        to = snapshot.span.endInclusive,
                    )
                    MonthContent.Ready(
                        days = days.associateBy { it.date },
                        marks = MonthMarks.of(days, calendars, now.zone),
                        calendars = calendars,
                    )
                }
            }
        }
        return MonthUiState(
            now = now,
            settings = settings,
            content = content,
            doors = snapshot.doors,
            reveal = reveal(content, snapshot.span, awaiting),
        )
    }

    /**
     * The event the reader made, once they are back and for [REVEAL_WINDOW]: long enough for a
     * calendar app that writes the provider a moment after its page closes, short enough that an
     * event synced later from elsewhere does not move the page. Only over the dates held when
     * they left.
     */
    private fun reveal(content: MonthContent, span: ClosedRange<LocalDate>, awaiting: Awaiting?): NewEvent? {
        val back = awaiting?.back ?: return null
        if (awaiting.span != span || Instant.now().isAfter(back.plus(REVEAL_WINDOW))) return null
        val ready = content as? MonthContent.Ready ?: return null
        return NewEvents.first(ready.days.values.sortedBy { it.date }, awaiting.known)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val REVEAL_WINDOW: Duration = Duration.ofMinutes(1)
    }
}
