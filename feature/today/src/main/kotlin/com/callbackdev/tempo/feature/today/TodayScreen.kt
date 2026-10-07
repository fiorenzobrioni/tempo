package com.callbackdev.tempo.feature.today

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarAccess
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.designsystem.components.CalendarBar
import com.callbackdev.tempo.core.designsystem.components.CalendarDot
import com.callbackdev.tempo.core.designsystem.components.CalendarPermissionCard
import com.callbackdev.tempo.core.designsystem.components.StatusCard
import com.callbackdev.tempo.core.designsystem.components.StatusTone
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.rememberAgendaText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.PageGutter
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.domain.clock.nextHalfHour
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.callbackdev.tempo.core.domain.today.DaySummary
import com.callbackdev.tempo.core.domain.today.Timeline
import com.callbackdev.tempo.core.domain.today.TimelineItem
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.FreeGap
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.Duration
import java.time.Instant

@Composable
fun TodayRoute(onOpenSettings: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val asked = state?.settings?.askedCalendarPermission ?: false
    var permission by remember { mutableStateOf(CalendarPermission.ASKABLE) }
    val refreshPermission = {
        permission = activity?.let { CalendarAccess.permission(it, asked) } ?: CalendarPermission.ASKABLE
    }
    // Back on the page: the permission, the calendars and the calendar app may all have changed.
    LifecycleResumeEffect(asked) {
        refreshPermission()
        viewModel.reread()
        onPauseOrDispose { }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionAnswered()
        refreshPermission()
    }
    // The calendar app opens on top of Tempo; whatever it does on Save or Back is its own, and
    // the change is on this page when the reader is back (CalendarChanges, PLANNING.md §4.5).
    val launch: (Intent) -> Unit = { intent -> runCatching { context.startActivity(intent) } }
    val current = state
    if (current == null) {
        Surface(Modifier.fillMaxSize()) { }
        return
    }
    TodayScreen(
        state = current,
        permission = permission,
        actions = TodayActions(
            openSettings = onOpenSettings,
            askPermission = { ask.launch(CalendarAccess.PERMISSION) },
            openAppSettings = {
                val page = Uri.fromParts("package", context.packageName, null)
                launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, page))
            },
            openEvent = { launch(CalendarIntents.view(it)) },
            newEvent = { launch(CalendarIntents.insert(it)) },
            openDay = { launch(CalendarIntents.openDay(it)) },
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class TodayActions(
    val openSettings: () -> Unit = {},
    val askPermission: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val openEvent: (EventInstance) -> Unit = {},
    val newEvent: (Instant) -> Unit = {},
    val openDay: (Instant) -> Unit = {},
)

/**
 * Today (VISION.md, Today; PLANNING.md §11 Phase 3): the time as the hero, the date, the next
 * alarm and the day in one sentence, on Chiaro's and Passo's glow; then the day's events, with the
 * past folded and "now" drawn as a line; then tomorrow, in full, and the rest of the week, brief.
 * The new-event button stays out of the way: a label at the top, its icon alone once scrolled.
 *
 * Every state says what is true: no permission, no calendar, every calendar hidden, no calendar
 * app to hand an event to. The page is drawn from [state] alone.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    permission: CalendarPermission,
    actions: TodayActions,
    modifier: Modifier = Modifier,
) {
    val text = rememberAgendaText(state.now.zone, state.settings.clockFormat)
    val gutter = pageGutter()
    val list = rememberLazyListState()
    val atTop by remember { derivedStateOf { list.firstVisibleItemIndex == 0 } }
    var showEarlier by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTimeColumn24 provides text.uses24Hour) {
            TodayPage(state, permission, actions, text, gutter, list, atTop, showEarlier, {
                showEarlier = !showEarlier
            })
        }
    }
}

@Composable
private fun TodayPage(
    state: TodayUiState,
    permission: CalendarPermission,
    actions: TodayActions,
    text: AgendaText,
    gutter: PageGutter,
    list: LazyListState,
    atTop: Boolean,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().testTag(TodayTags.LIST),
            // Room for the button over the last row, whatever the list ends with.
            contentPadding = PaddingValues(bottom = navigationBar + 96.dp),
        ) {
            item(key = "hero") { Hero(state, text, gutter, actions) }
            when (val content = state.content) {
                TodayContent.Loading -> Unit

                TodayContent.NoPermission -> card("permission", gutter) {
                    CalendarPermissionCard(permission, actions.askPermission, actions.openAppSettings)
                }

                TodayContent.NoCalendars -> card("no-calendars", gutter) {
                    StatusCard(
                        icon = TempoIcons.Calendar,
                        title = stringResource(R.string.today_no_calendars_title),
                        body = stringResource(R.string.today_no_calendars_body),
                        tone = StatusTone.NOTE,
                    )
                }

                TodayContent.AllHidden -> card("all-hidden", gutter) {
                    StatusCard(
                        icon = TempoIcons.Calendar,
                        title = stringResource(R.string.today_all_hidden_title),
                        body = stringResource(R.string.today_all_hidden_body),
                        tone = StatusTone.CHOICE,
                        action = stringResource(R.string.today_all_hidden_action),
                        onAction = actions.openSettings,
                    )
                }

                is TodayContent.Ready -> agenda(
                    content = content,
                    state = state,
                    text = text,
                    gutter = gutter,
                    actions = actions,
                    showEarlier = showEarlier,
                    onToggleEarlier = onToggleEarlier,
                )
            }
            if (state.content is TodayContent.Ready && !state.doors.canOpenEvent && !state.doors.canCreate) {
                card("no-app", gutter) {
                    StatusCard(
                        icon = TempoIcons.Info,
                        title = stringResource(R.string.today_no_app_title),
                        body = stringResource(R.string.today_no_app_body),
                        tone = StatusTone.NOTE,
                    )
                }
            }
        }
        if (state.doors.canCreate) {
            val newEventLabel = stringResource(R.string.today_new_event)
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.today_new_event)) },
                icon = { Icon(TempoIcons.Plus, contentDescription = null) },
                expanded = atTop,
                onClick = { actions.newEvent(nextHalfHour(state.now.toInstant(), state.now.zone)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(gutter)
                    .padding(ScreenMargin)
                    // Collapsed to its icon once the list scrolls: it still says what it does.
                    .semantics { contentDescription = newEventLabel }
                    .testTag(TodayTags.NEW_EVENT),
            )
        }
    }
}

private fun LazyListScope.card(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.padding(gutter).padding(horizontal = ScreenMargin, vertical = 8.dp)) { content() }
    }
}

/**
 * The time, large, on the family's glow (Passo's Today), the date the calendar app opens on, the
 * next alarm and the day's sentence. The hero grows with the reader's text size up to a cap: at
 * twice the size an unbounded clock would break "09:41" over two lines (PLANNING.md §15).
 */
@Composable
private fun Hero(state: TodayUiState, text: AgendaText, gutter: PageGutter, actions: TodayActions) {
    val glow = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val locale = LocalLocale.current.platformLocale
    val now = state.now
    // Through the density, so Android 14's non-linear text scaling is the one applied: the clock
    // grows with the reader's text size as the rest of the page does, up to its cap.
    val density = LocalDensity.current
    val clockSize = with(density) { minOf(CLOCK_SIZE.toDp(), CLOCK_CAP).toSp() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.75f),
                    0.55f to glow.copy(alpha = 0.18f),
                    1f to surface,
                ),
            )
            .statusBarsPadding()
            .padding(gutter)
            .padding(bottom = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(end = 4.dp)) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = actions.openSettings, modifier = Modifier.testTag(TodayTags.SETTINGS)) {
                Icon(TempoIcons.Settings, contentDescription = stringResource(R.string.today_settings))
            }
        }
        Text(
            text = DateTimeText.time(now, locale, text.uses24Hour),
            style = TempoTheme.type.heroNumber.copy(fontSize = clockSize, lineHeight = clockSize * 1.05f),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = ScreenMargin + 4.dp)
                .testTag(TodayTags.TIME)
                .semantics { heading() },
        )
        val dateText = DateTimeText.date(now, locale, state.settings.dateStyle)
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(
                    enabled = state.doors.canOpenDay,
                    onClickLabel = stringResource(R.string.today_open_day),
                    role = Role.Button,
                ) { actions.openDay(now.toInstant()) }
                .heightIn(min = 48.dp)
                .padding(horizontal = ScreenMargin + 4.dp, vertical = 12.dp)
                .testTag(TodayTags.DATE),
        )
        val alarm = state.alarm
        if (alarm != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp).testTag(TodayTags.ALARM),
            ) {
                Icon(
                    TempoIcons.Bell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = text.alarm(alarm, AlarmDay.of(alarm, now.toInstant(), now.zone)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val content = state.content
        if (content is TodayContent.Ready) {
            Text(
                text = text.sentence(content.sentence),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = ScreenMargin + 4.dp, end = ScreenMargin + 4.dp, top = 20.dp, bottom = 4.dp)
                    .testTag(TodayTags.SENTENCE),
            )
        }
    }
}

private val CLOCK_SIZE = 80.sp

/** The clock's largest size on screen: five characters still fit a phone's width on one line. */
private val CLOCK_CAP = 104.dp

private fun LazyListScope.agenda(
    content: TodayContent.Ready,
    state: TodayUiState,
    text: AgendaText,
    gutter: PageGutter,
    actions: TodayActions,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
) {
    val now = state.now.toInstant()
    val doors = state.doors
    val openEvent = actions.openEvent.takeIf { doors.canOpenEvent }
    val newEvent = actions.newEvent.takeIf { doors.canCreate }
    content.agenda.days.forEachIndexed { index, day ->
        when (index) {
            0 -> {
                allDayItem(day, content.calendars, text, gutter, openEvent)
                Timeline.of(day, isToday = true, showEarlier = showEarlier).forEach { row ->
                    timelineItem(row, day, content.calendars, text, now, gutter, openEvent, newEvent, onToggleEarlier)
                }
            }

            1 -> {
                item(key = "head-${day.date}") {
                    DayHeader(stringResource(R.string.today_tomorrow), text.summary(DaySummary.of(day)), gutter)
                }
                allDayItem(day, content.calendars, text, gutter, openEvent)
                Timeline.of(day, isToday = false).forEach { row ->
                    timelineItem(row, day, content.calendars, text, now, gutter, openEvent, newEvent, onToggleEarlier)
                }
            }

            else -> {
                item(key = "head-${day.date}") {
                    val locale = LocalLocale.current.platformLocale
                    val title = DateTimeText.date(day.date, locale, DateStyle.LONG)
                    DayHeader(title, text.summary(DaySummary.of(day)), gutter)
                }
                day.allDay.forEach { entry ->
                    item(key = "compact-all-${day.date}-${entry.event.eventId}-${entry.event.begin}") {
                        CompactRow(entry.event, null, content.calendars, text, gutter, openEvent)
                    }
                }
                day.timed.forEach { entry ->
                    item(key = "compact-${day.date}-${entry.event.eventId}-${entry.event.begin}") {
                        CompactRow(entry.event, entry, content.calendars, text, gutter, openEvent)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.allDayItem(
    day: AgendaDay,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
) {
    if (day.allDay.isEmpty()) return
    item(key = "all-day-${day.date}") { AllDayRow(day.allDay, calendars, text, gutter, openEvent) }
}

private fun LazyListScope.timelineItem(
    row: TimelineItem,
    day: AgendaDay,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    now: Instant,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
    newEvent: ((Instant) -> Unit)?,
    onToggleEarlier: () -> Unit,
) {
    when (row) {
        is TimelineItem.Event -> item(key = "event-${day.date}-${row.entry.event.eventId}-${row.entry.event.begin}") {
            EventRow(row.entry, calendars[row.entry.event.calendarId], text, now, gutter, openEvent)
        }

        is TimelineItem.Free -> item(key = "free-${day.date}-${row.gap.start}") {
            FreeRow(row.gap, text, gutter, newEvent)
        }

        is TimelineItem.Earlier -> item(key = "earlier-${day.date}") {
            EarlierRow(row.entries.size, showEarlier = row.open, gutter = gutter, onToggle = onToggleEarlier)
        }

        TimelineItem.Now -> item(key = "now") { NowRow(gutter) }
    }
}

/** A day's heading: "Tomorrow", or its date, with the day in a few words under it. */
@Composable
private fun DayHeader(title: String, summary: String, gutter: PageGutter) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(gutter)
            .padding(start = ScreenMargin + 4.dp, end = ScreenMargin, top = 28.dp, bottom = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The all-day events, as chips over the timeline: a holiday is the day's frame, not one of its rows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllDayRow(
    entries: List<AllDayEntry>,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(gutter)
            .padding(horizontal = ScreenMargin, vertical = 8.dp)
            .testTag(TodayTags.ALL_DAY),
    ) {
        entries.forEach { entry -> AllDayChip(entry, calendars[entry.event.calendarId], text, openEvent) }
    }
}

@Composable
private fun AllDayChip(
    entry: AllDayEntry,
    calendar: CalendarInfo?,
    text: AgendaText,
    openEvent: ((EventInstance) -> Unit)?,
) {
    val event = entry.event
    val ground = MaterialTheme.colorScheme.surfaceContainerHigh
    val dayOf = if (entry.dayCount > 1) stringResource(R.string.today_day_of, entry.dayNumber, entry.dayCount) else null
    val description = listOfNotNull(
        stringResource(R.string.today_all_day),
        text.title(event),
        dayOf,
        calendar?.let { stringResource(R.string.a11y_in_calendar, it.name) },
    ).joinToString(", ")
    val chip: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).clearAndSetSemantics { },
        ) {
            CalendarDot(event.color ?: calendar?.color, ground, size = 10.dp)
            Text(
                text = text.title(event),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dayOf != null) {
                Text(
                    text = dayOf,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (openEvent != null) {
        Surface(
            onClick = { openEvent(event) },
            color = ground,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.semantics { contentDescription = description },
        ) { chip() }
    } else {
        Surface(
            color = ground,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        ) { chip() }
    }
}

/**
 * One event on the timeline: its times in a column, its calendar's colour as a bar, its title, its
 * place, and what is true of it now. The event under way stands on its own ground with how long
 * is left; past ones are quieter; a declined invitation is struck through. TalkBack reads it as
 * one sentence: "10:00 to 11:00, Dentist, Via Roma 3, in Personal".
 */
@Composable
private fun EventRow(
    entry: TimedEntry,
    calendar: CalendarInfo?,
    text: AgendaText,
    now: Instant,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
) {
    val event = entry.event
    val underWay = entry.state == EntryState.UNDER_WAY
    val quiet = entry.state == EntryState.PAST || event.selfStatus == AttendeeStatus.DECLINED
    val ground = if (underWay) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
    val ink = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val strike = if (event.selfStatus == AttendeeStatus.DECLINED) TextDecoration.LineThrough else null
    val description = describe(entry, calendar, text, now)
    val openLabel = stringResource(R.string.a11y_open_event)
    val notes = listOfNotNull(
        stringResource(R.string.today_started_before).takeIf { !entry.startsOnDate },
        stringResource(R.string.today_ends_after).takeIf { !entry.endsOnDate },
        stringResource(R.string.today_declined).takeIf { event.selfStatus == AttendeeStatus.DECLINED },
        stringResource(R.string.today_maybe).takeIf { event.selfStatus == AttendeeStatus.TENTATIVE },
    )
    val target = Modifier
        .fillMaxWidth()
        .then(
            if (openEvent != null) {
                Modifier.clickable(onClickLabel = openLabel) { openEvent(event) }
            } else {
                Modifier.semantics(mergeDescendants = true) { }
            },
        )
        .semantics { contentDescription = description }
        .testTag(TodayTags.event(event.eventId))
    val row: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = if (underWay) 12.dp else ScreenMargin + 4.dp, vertical = 12.dp)
                .clearAndSetSemantics { },
        ) {
            Column(Modifier.width(timeColumn)) {
                Text(
                    text = text.time(event.begin),
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                )
                if (event.end != event.begin) {
                    Text(
                        text = text.time(event.end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarBar(event.color ?: calendar?.color, ground)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = text.title(event),
                    style = if (underWay) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = ink,
                    textDecoration = strike,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                event.location?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (notes.isNotEmpty()) {
                    Text(
                        text = notes.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (underWay) UnderWayProgress(event, text, now)
            }
        }
    }
    if (underWay) {
        Surface(
            color = ground,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(gutter).padding(horizontal = 8.dp, vertical = 4.dp),
        ) { Box(target) { row() } }
    } else {
        Box(Modifier.padding(gutter).then(target)) { row() }
    }
}

/** How far the event under way has come, and how long is left: the bar in the accent, the words beside it. */
@Composable
private fun UnderWayProgress(event: EventInstance, text: AgendaText, now: Instant) {
    val total = Duration.between(event.begin, event.end).toMillis().coerceAtLeast(1)
    val done = Duration.between(event.begin, now).toMillis().coerceIn(0, total)
    val fraction by animateFloatAsState(done.toFloat() / total, label = "under-way")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
            modifier = Modifier.weight(1f).height(4.dp),
        )
        Text(
            text = stringResource(R.string.today_left, text.duration(Duration.between(now, event.end))),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The row's sentence for TalkBack (PLANNING.md §11 Phase 3, acceptance). */
@Composable
private fun describe(entry: TimedEntry, calendar: CalendarInfo?, text: AgendaText, now: Instant): String {
    val event = entry.event
    val range = if (event.begin == event.end) {
        text.time(event.begin)
    } else {
        stringResource(R.string.a11y_range, text.time(event.begin), text.time(event.end))
    }
    return listOfNotNull(
        range,
        text.title(event),
        event.location,
        calendar?.let { stringResource(R.string.a11y_in_calendar, it.name) },
        stringResource(R.string.today_started_before).takeIf { !entry.startsOnDate },
        stringResource(R.string.today_ends_after).takeIf { !entry.endsOnDate },
        when (entry.state) {
            EntryState.UNDER_WAY ->
                stringResource(R.string.a11y_under_way, text.duration(Duration.between(now, event.end)))

            EntryState.PAST -> stringResource(R.string.a11y_over)

            EntryState.UPCOMING -> null
        },
        stringResource(R.string.a11y_declined).takeIf { event.selfStatus == AttendeeStatus.DECLINED },
        stringResource(R.string.a11y_maybe).takeIf { event.selfStatus == AttendeeStatus.TENTATIVE },
    ).joinToString(", ")
}

/**
 * A gap of an hour or more: said, and, where a calendar app can take it, the way to fill it (a new
 * event that starts when the gap does).
 */
@Composable
private fun FreeRow(gap: FreeGap, text: AgendaText, gutter: PageGutter, newEvent: ((Instant) -> Unit)?) {
    val length = text.duration(gap.duration)
    val description = stringResource(R.string.a11y_free, text.time(gap.start), length)
    val newLabel = stringResource(R.string.today_new_event)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(gutter)
            .fillMaxWidth()
            .then(
                if (newEvent != null) {
                    Modifier.clickable(onClickLabel = newLabel) { newEvent(gap.start) }
                } else {
                    Modifier.semantics(mergeDescendants = true) { }
                },
            )
            .semantics { contentDescription = description }
            .heightIn(min = 48.dp)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 6.dp)
            .testTag(TodayTags.FREE),
    ) {
        Text(
            text = text.time(gap.start),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(timeColumn).clearAndSetSemantics { },
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.today_free_for, length),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).clearAndSetSemantics { },
        )
        if (newEvent != null) {
            Icon(
                TempoIcons.Plus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The line between the day so far and what is ahead: the accent, a dot, the word. */
@Composable
private fun NowRow(gutter: PageGutter) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(gutter)
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 6.dp)
            .testTag(TodayTags.NOW),
    ) {
        Text(
            text = stringResource(R.string.today_now),
            style = MaterialTheme.typography.labelLarge,
            color = primary,
            modifier = Modifier.width(timeColumn),
        )
        Box(Modifier.size(8.dp).background(primary, CircleShape))
        HorizontalDivider(thickness = 2.dp, color = primary, modifier = Modifier.weight(1f).clearAndSetSemantics { })
    }
}

/** Today's events already over, folded into one row; a touch opens them, another folds them back. */
@Composable
private fun EarlierRow(count: Int, showEarlier: Boolean, gutter: PageGutter, onToggle: () -> Unit) {
    val label = pluralStringResource(R.plurals.today_earlier, count, count)
    val action = stringResource(if (showEarlier) R.string.today_hide else R.string.today_show)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(gutter)
            .fillMaxWidth()
            .clickable(onClickLabel = action, onClick = onToggle)
            .semantics { stateDescription = action }
            .heightIn(min = 48.dp)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 6.dp)
            .testTag(TodayTags.EARLIER),
    ) {
        Spacer(Modifier.width(timeColumn))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            TempoIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(if (showEarlier) 270f else 90f),
        )
    }
}

/** A day after tomorrow, one line an event: the time (or "All day"), the calendar's dot, the title. */
@Composable
private fun CompactRow(
    event: EventInstance,
    entry: TimedEntry?,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
) {
    val calendar = calendars[event.calendarId]
    val time = when {
        entry == null -> stringResource(R.string.today_all_day)
        !entry.startsOnDate -> text.until(event.end)
        else -> text.time(event.begin)
    }
    val inCalendar = calendar?.let { stringResource(R.string.a11y_in_calendar, it.name) }
    val description = listOfNotNull(time, text.title(event), inCalendar).joinToString(", ")
    val openLabel = stringResource(R.string.a11y_open_event)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(gutter)
            .fillMaxWidth()
            .then(
                if (openEvent != null) {
                    Modifier.clickable(onClickLabel = openLabel) { openEvent(event) }
                } else {
                    Modifier.semantics(mergeDescendants = true) { }
                },
            )
            .semantics { contentDescription = description }
            .heightIn(min = 48.dp)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.width(timeColumn).clearAndSetSemantics { },
        )
        CalendarDot(event.color ?: calendar?.color, MaterialTheme.colorScheme.surface, size = 10.dp)
        Text(
            text = text.title(event),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clearAndSetSemantics { },
        )
    }
}

/**
 * The width of the times' column: enough for "10:00" on the 24-hour clock and "10:00 PM" on the
 * 12-hour one, grown with the reader's text size (at twice the size a fixed 56dp broke "10:00" in
 * two, Phase 3's screenshots), so every row's title starts on one line.
 */
private val timeColumn: Dp
    @Composable get() {
        val base = if (LocalTimeColumn24.current) 56.dp else 80.dp
        return base * LocalDensity.current.fontScale.coerceAtLeast(1f)
    }

/** Whether the page writes times on the 24-hour clock, for [timeColumn]'s width. */
private val LocalTimeColumn24 = staticCompositionLocalOf { true }

/** Hooks for the UI tests. */
object TodayTags {
    const val LIST = "today_list"
    const val TIME = "today_time"
    const val DATE = "today_date"
    const val ALARM = "today_alarm"
    const val SENTENCE = "today_sentence"
    const val SETTINGS = "today_settings"
    const val NEW_EVENT = "today_new_event"
    const val ALL_DAY = "today_all_day"
    const val FREE = "today_free"
    const val NOW = "today_now"
    const val EARLIER = "today_earlier"

    fun event(id: Long) = "today_event_$id"
}
