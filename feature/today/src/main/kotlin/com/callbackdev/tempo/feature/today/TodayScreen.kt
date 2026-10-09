package com.callbackdev.tempo.feature.today

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import com.callbackdev.tempo.core.designsystem.components.DayDial
import com.callbackdev.tempo.core.designsystem.components.StatusCard
import com.callbackdev.tempo.core.designsystem.components.StatusTone
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.rememberAgendaText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.GroupShape
import com.callbackdev.tempo.core.designsystem.theme.PageGutter
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.domain.clock.nextHalfHour
import com.callbackdev.tempo.core.domain.today.AgendaSection
import com.callbackdev.tempo.core.domain.today.AgendaSections
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.callbackdev.tempo.core.domain.today.DaySummary
import com.callbackdev.tempo.core.domain.today.Timeline
import com.callbackdev.tempo.core.domain.today.TimelineItem
import com.callbackdev.tempo.core.domain.widget.DialArc
import com.callbackdev.tempo.core.domain.widget.DialArcs
import com.callbackdev.tempo.core.domain.widget.WordsDay
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.FreeGap
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

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
 * Today (VISION.md, Today; PLANNING.md §11 Phase 3, reviewed on 9 Oct 2026, §15): the date and
 * the page's two doors on top, the time as the hero beside the day's clock face (the «In words»
 * card's dial, with the next twelve hours' events on its ring), the next alarm and the day in one
 * sentence, on Chiaro's and Passo's glow; then today's events, with the past folded and "now"
 * drawn as a line; then the days ahead, each on its own card: tomorrow in full, the rest of the
 * week a line an event, and a run of empty days said once. The new-event button stays out of the
 * way: a label at the top, its icon alone once scrolled.
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
            contentPadding = PaddingValues(bottom = navigationBar + 88.dp),
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

/** A state's card, or a day's, on the page's column. */
private fun LazyListScope.card(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.padding(gutter).padding(horizontal = ScreenMargin, vertical = 6.dp)) { content() }
    }
}

/** One of today's rows, on the page's column. */
private fun LazyListScope.row(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) { Box(Modifier.padding(gutter)) { content() } }
}

/**
 * The top of the page, on the family's glow (Passo's Today): the date (a touch opens the calendar
 * app on it) with the calendar's and the settings' buttons; the time, large, beside the day's
 * clock face; the next alarm; the day's sentence. The clock grows with the reader's text size up
 * to a cap, and shrinks to fit beside the dial: "9:10 PM" stays on one line (PLANNING.md §15).
 */
@Composable
private fun Hero(state: TodayUiState, text: AgendaText, gutter: PageGutter, actions: TodayActions) {
    val glow = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val locale = LocalLocale.current.platformLocale
    val now = state.now
    val content = state.content
    val openDay = actions.openDay.takeIf { state.doors.canOpenDay }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = ScreenMargin + 4.dp, end = 4.dp),
        ) {
            Text(
                text = DateTimeText.date(now, locale, state.settings.dateStyle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = openDay != null,
                        onClickLabel = stringResource(R.string.today_open_day),
                        role = Role.Button,
                    ) { openDay?.invoke(now.toInstant()) }
                    .heightIn(min = 48.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(end = 8.dp)
                    .testTag(TodayTags.DATE)
                    .semantics { heading() },
            )
            if (openDay != null && state.settings.showCalendarButton) {
                IconButton(onClick = { openDay(now.toInstant()) }, modifier = Modifier.testTag(TodayTags.CALENDAR)) {
                    Icon(TempoIcons.Calendar, contentDescription = stringResource(R.string.today_open_calendar))
                }
            }
            IconButton(onClick = actions.openSettings, modifier = Modifier.testTag(TodayTags.SETTINGS)) {
                Icon(TempoIcons.Settings, contentDescription = stringResource(R.string.today_settings))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(
                start = ScreenMargin + 4.dp,
                end = ScreenMargin + 4.dp,
                top = 4.dp,
            ),
        ) {
            Column(Modifier.weight(1f)) {
                Clock(now, locale, text.uses24Hour, Modifier.testTag(TodayTags.TIME))
                val alarm = state.alarm
                if (alarm != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp).testTag(TodayTags.ALARM),
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
            }
            Spacer(Modifier.width(16.dp))
            // The face works without the calendar, as the clock does: its ring is just empty.
            val arcs = if (content is TodayContent.Ready) {
                remember(content.agenda, state.settings.showAllDay) {
                    dialArcs(content, state.settings.showAllDay, now.zone)
                }
            } else {
                emptyList()
            }
            DayDial(
                faceMinutes = DialArcs.minuteOnFace(now.toInstant(), now.zone),
                arcs = arcs,
                description = stringResource(R.string.today_dial, DateTimeText.time(now, locale, text.uses24Hour)),
                modifier = Modifier.size(DIAL_SIZE).testTag(TodayTags.DIAL),
            )
        }
        if (content is TodayContent.Ready) {
            Text(
                text = text.sentence(content.sentence),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = ScreenMargin + 4.dp, end = ScreenMargin + 4.dp, top = 16.dp, bottom = 4.dp)
                    .testTag(TodayTags.SENTENCE),
            )
        }
    }
}

/** The dial's arcs, as the «In words» card draws them: the focus in the accent, the rest quiet. */
private fun dialArcs(content: TodayContent.Ready, showAllDay: Boolean, zone: ZoneId): List<DialArc> {
    val focus = WordsDay.of(content.agenda, showAllDay = showAllDay, showDaysAhead = true).focus
    return DialArcs.of(content.agenda, focus, zone, showDaysAhead = true)
}

/**
 * The time in the hero type: its figures as large as the column allows, each one rolling to its
 * next value as the minute changes (a fade under reduced motion), and the day marker set small
 * beside them on the 12-hour clock. Read as one time by a screen reader.
 */
@Composable
private fun Clock(now: ZonedDateTime, locale: Locale, uses24Hour: Boolean, modifier: Modifier = Modifier) {
    val parts = DateTimeText.timeParts(now, locale, uses24Hour)
    val spoken = DateTimeText.time(now, locale, uses24Hour)
    // Through the density, so Android 14's non-linear text scaling is the one applied: the clock
    // grows with the reader's text size as the rest of the page does, up to its cap.
    val density = LocalDensity.current
    val largest = with(density) { minOf(CLOCK_SIZE.toDp(), CLOCK_CAP).toSp() }
    val figuresStyle = TempoTheme.type.heroNumber
    val markerStyle = MaterialTheme.typography.titleLarge
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(modifier.semantics { text = AnnotatedString(spoken) }) {
        val markerWidth = parts.marker?.let { marker ->
            measurer.measure(AnnotatedString(marker), markerStyle, maxLines = 1, softWrap = false).size.width +
                with(density) { MARKER_GAP.roundToPx() }
        } ?: 0
        val room = (constraints.maxWidth - markerWidth).coerceAtLeast(1)
        // Each figure is laid out on its own and rounded up to a whole pixel, so the row is a
        // little wider than the string measured whole: counted, or the marker lost its last pixels.
        val wanted = measurer.measure(
            text = AnnotatedString(parts.figures),
            style = figuresStyle.copy(fontSize = largest),
            maxLines = 1,
            softWrap = false,
        ).size.width + parts.figures.length + with(density) { CLOCK_SLACK.roundToPx() }
        val shrunk = largest * (room.toFloat() / wanted)
        val size: TextUnit = when {
            wanted <= room -> largest
            shrunk.value < CLOCK_SMALLEST.value -> CLOCK_SMALLEST
            else -> shrunk
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.clearAndSetSemantics { }) {
            val marker: @Composable () -> Unit = {
                parts.marker?.let {
                    Text(
                        text = it,
                        style = markerStyle,
                        color = ink,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline().padding(horizontal = MARKER_GAP / 2),
                    )
                }
            }
            if (parts.markerFirst) marker()
            RollingFigures(
                figures = parts.figures,
                style = figuresStyle.copy(fontSize = size, lineHeight = size * CLOCK_LINE),
                color = ink,
                modifier = Modifier.alignByBaseline(),
            )
            if (!parts.markerFirst) marker()
        }
    }
}

/** Each figure in its own place, so only the one that changed rolls up to its next value. */
@Composable
private fun RollingFigures(figures: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    Row(modifier) {
        figures.forEachIndexed { index, figure ->
            AnimatedContent(
                targetState = figure,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(TempoMotion.fade()) togetherWith fadeOut(TempoMotion.fade())
                    } else {
                        (
                            slideInVertically(TempoMotion.spatial()) {
                                it / 2
                            } + fadeIn(TempoMotion.effects())
                            ) togetherWith
                            (slideOutVertically(TempoMotion.spatial()) { -it / 2 } + fadeOut(TempoMotion.effects()))
                    }
                },
                label = "figure-$index",
            ) { shown ->
                Text(text = shown.toString(), style = style, color = color, maxLines = 1, softWrap = false)
            }
        }
    }
}

private val CLOCK_SIZE = 80.sp

/** The clock's largest size on screen: five characters still fit a phone's width on one line. */
private val CLOCK_CAP = 104.dp

/** Shrunk to fit beside the dial, never below this: still the largest thing on the page. */
private val CLOCK_SMALLEST = 36.sp

private const val CLOCK_LINE = 1.05f
private val CLOCK_SLACK = 4.dp
private val MARKER_GAP = 8.dp

/** The dial's side: Passo's ring in its tall card, the «In words» card's dial on a 2×2. */
private val DIAL_SIZE = 112.dp

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
    val zone = state.now.zone
    val doors = state.doors
    val openEvent = actions.openEvent.takeIf { doors.canOpenEvent }
    val newEvent = actions.newEvent.takeIf { doors.canCreate }
    val openDay: ((LocalDate) -> Unit)? = if (doors.canOpenDay) {
        { date -> actions.openDay(date.atStartOfDay(zone).toInstant()) }
    } else {
        null
    }
    val style = state.settings.dateStyle
    AgendaSections.of(content.agenda).forEach { section ->
        when (section) {
            is AgendaSection.Today -> {
                val day = section.day
                if (day.allDay.isNotEmpty()) {
                    row("all-day-${day.date}", gutter) {
                        AllDayRow(
                            day.allDay,
                            content.calendars,
                            text,
                            ScreenMargin,
                            openEvent,
                            Modifier.testTag(TodayTags.ALL_DAY),
                        )
                    }
                }
                Timeline.of(day, isToday = true, showEarlier = showEarlier).forEach { item ->
                    timelineItem(item, day, content.calendars, text, now, gutter, openEvent, newEvent, onToggleEarlier)
                }
            }

            is AgendaSection.Tomorrow -> card("day-${section.day.date}", gutter) {
                val day = section.day
                DayCard(
                    title = stringResource(R.string.today_tomorrow),
                    summary = text.summary(DaySummary.of(day)),
                    onOpen = openDay?.let { { it(day.date) } },
                    modifier = Modifier.testTag(TodayTags.day(day.date)),
                ) {
                    if (day.allDay.isNotEmpty()) AllDayRow(day.allDay, content.calendars, text, CARD_EDGE, openEvent)
                    Timeline.of(day, isToday = false).forEach { item ->
                        if (item is TimelineItem.Event) {
                            EventRow(
                                item.entry,
                                content.calendars[item.entry.event.calendarId],
                                text,
                                now,
                                CARD_EDGE,
                                openEvent,
                            )
                        }
                    }
                }
            }

            is AgendaSection.Later -> card("day-${section.day.date}", gutter) {
                val day = section.day
                DayCard(
                    title = text.date(day.date, style),
                    summary = text.summary(DaySummary.of(day)),
                    onOpen = openDay?.let { { it(day.date) } },
                    modifier = Modifier.testTag(TodayTags.day(day.date)),
                ) {
                    day.allDay.forEach { entry -> CompactRow(entry.event, null, content.calendars, text, openEvent) }
                    day.timed.forEach { entry -> CompactRow(entry.event, entry, content.calendars, text, openEvent) }
                }
            }

            is AgendaSection.Nothing -> card("nothing-${section.from}", gutter) {
                val first = if (section.startsTomorrow) {
                    stringResource(
                        R.string.today_tomorrow,
                    )
                } else {
                    text.date(section.from, style)
                }
                DayCard(
                    title = if (section.isOneDay) first else text.range(first, text.date(section.to, style)),
                    summary = text.nothingPlanned(),
                    onOpen = openDay?.let { { it(section.from) } },
                    modifier = Modifier.testTag(TodayTags.day(section.from)),
                ) { }
            }
        }
    }
}

private fun LazyListScope.timelineItem(
    item: TimelineItem,
    day: AgendaDay,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    now: Instant,
    gutter: PageGutter,
    openEvent: ((EventInstance) -> Unit)?,
    newEvent: ((Instant) -> Unit)?,
    onToggleEarlier: () -> Unit,
) {
    when (item) {
        is TimelineItem.Event -> row(
            "event-${day.date}-${item.entry.event.eventId}-${item.entry.event.begin}",
            gutter,
        ) {
            EventRow(item.entry, calendars[item.entry.event.calendarId], text, now, ScreenMargin + 4.dp, openEvent)
        }

        is TimelineItem.Free -> row("free-${day.date}-${item.gap.start}", gutter) { FreeRow(item.gap, text, newEvent) }

        is TimelineItem.Earlier -> row("earlier-${day.date}", gutter) {
            EarlierRow(item.entries.size, showEarlier = item.open, onToggle = onToggleEarlier)
        }

        TimelineItem.Now -> row("now", gutter) { NowRow() }
    }
}

/**
 * A day ahead on its own ground (Chiaro's group, Passo's cards): its name and the day in a few
 * words on one line, a touch on them opening the calendar app on that day, and its rows under
 * them. A run of empty days is a card of its heading alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(
    title: String,
    summary: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val openLabel = stringResource(R.string.today_open_day)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(bottom = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onOpen != null) {
                            Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
                        } else {
                            Modifier
                        },
                    )
                    .semantics(mergeDescendants = true) { }
                    .heightIn(min = 48.dp)
                    .padding(start = CARD_EDGE, end = 12.dp, top = 10.dp, bottom = 6.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics {
                            heading()
                        },
                    )
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onOpen != null) {
                    Icon(
                        TempoIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp).size(20.dp),
                    )
                }
            }
            content()
        }
    }
}

/** What a card's rows keep from its edge. */
private val CARD_EDGE = 16.dp

/** The all-day events, as chips over the timeline: a holiday is the day's frame, not one of its rows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllDayRow(
    entries: List<AllDayEntry>,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    edge: Dp,
    openEvent: ((EventInstance) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = edge, vertical = 6.dp),
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
    edge: Dp,
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
        .heightIn(min = 48.dp)
        .testTag(TodayTags.event(event.eventId))
    val row: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = if (underWay) 12.dp else edge, vertical = 8.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) { Box(target) { row() } }
    } else {
        Box(target) { row() }
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
private fun FreeRow(gap: FreeGap, text: AgendaText, newEvent: ((Instant) -> Unit)?) {
    val length = text.duration(gap.duration)
    val description = stringResource(R.string.a11y_free, text.time(gap.start), length)
    val newLabel = stringResource(R.string.today_new_event)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
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
private fun NowRow() {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
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
private fun EarlierRow(count: Int, showEarlier: Boolean, onToggle: () -> Unit) {
    val label = pluralStringResource(R.plurals.today_earlier, count, count)
    val action = stringResource(if (showEarlier) R.string.today_hide else R.string.today_show)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
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
            .padding(horizontal = CARD_EDGE, vertical = 4.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.width(timeColumn).clearAndSetSemantics { },
        )
        CalendarDot(event.color ?: calendar?.color, MaterialTheme.colorScheme.surfaceContainerLow, size = 10.dp)
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
    const val DIAL = "today_dial"
    const val ALARM = "today_alarm"
    const val SENTENCE = "today_sentence"
    const val SETTINGS = "today_settings"
    const val CALENDAR = "today_calendar"
    const val NEW_EVENT = "today_new_event"
    const val ALL_DAY = "today_all_day"
    const val FREE = "today_free"
    const val NOW = "today_now"
    const val EARLIER = "today_earlier"

    fun event(id: Long) = "today_event_$id"

    fun day(date: LocalDate) = "today_day_$date"
}
