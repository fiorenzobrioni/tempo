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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarAccess
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.calendar.ClockApp
import com.callbackdev.tempo.core.designsystem.components.AgendaAllDayRow
import com.callbackdev.tempo.core.designsystem.components.AgendaBarWidth
import com.callbackdev.tempo.core.designsystem.components.AgendaEventRow
import com.callbackdev.tempo.core.designsystem.components.AgendaRowEdge
import com.callbackdev.tempo.core.designsystem.components.CalendarBar
import com.callbackdev.tempo.core.designsystem.components.CalendarDot
import com.callbackdev.tempo.core.designsystem.components.CalendarPermissionCard
import com.callbackdev.tempo.core.designsystem.components.DayDial
import com.callbackdev.tempo.core.designsystem.components.DayHeading
import com.callbackdev.tempo.core.designsystem.components.DayName
import com.callbackdev.tempo.core.designsystem.components.LocalGlowingEvent
import com.callbackdev.tempo.core.designsystem.components.LocalTimeColumn24
import com.callbackdev.tempo.core.designsystem.components.REVEAL_MILLIS
import com.callbackdev.tempo.core.designsystem.components.RevealItem
import com.callbackdev.tempo.core.designsystem.components.StatusCard
import com.callbackdev.tempo.core.designsystem.components.StatusTone
import com.callbackdev.tempo.core.designsystem.components.agendaStartTime
import com.callbackdev.tempo.core.designsystem.components.agendaTimeColumn
import com.callbackdev.tempo.core.designsystem.components.glows
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.TimeParts
import com.callbackdev.tempo.core.designsystem.format.rememberAgendaText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.GroupShape
import com.callbackdev.tempo.core.designsystem.theme.PageGutter
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.TwoPanes
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.designsystem.theme.tabular
import com.callbackdev.tempo.core.designsystem.theme.twoPanes
import com.callbackdev.tempo.core.domain.clock.SecondZone
import com.callbackdev.tempo.core.domain.clock.newEventIn
import com.callbackdev.tempo.core.domain.clock.nextHalfHour
import com.callbackdev.tempo.core.domain.today.AgendaSection
import com.callbackdev.tempo.core.domain.today.AgendaSections
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.callbackdev.tempo.core.domain.today.DaySummary
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.Timeline
import com.callbackdev.tempo.core.domain.today.TimelineItem
import com.callbackdev.tempo.core.domain.widget.DialArc
import com.callbackdev.tempo.core.domain.widget.DialArcs
import com.callbackdev.tempo.core.domain.widget.WordsDay
import com.callbackdev.tempo.core.model.Agenda
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale
import com.callbackdev.tempo.core.designsystem.R as DesignR

@Composable
fun TodayRoute(
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenMonth: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
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
            openMonth = onOpenMonth,
            askPermission = { ask.launch(CalendarAccess.PERMISSION) },
            openAppSettings = {
                val page = Uri.fromParts("package", context.packageName, null)
                launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, page))
            },
            openEvent = { launch(CalendarIntents.view(it)) },
            newEvent = { begin, end ->
                viewModel.onNewEventOpened()
                launch(CalendarIntents.insert(begin, end))
            },
            openDay = { launch(CalendarIntents.openDay(it)) },
            openClock = { current.doors.clockPackage?.let { ClockApp.open(context, it) }?.let(launch) },
            openGuide = {
                viewModel.onTipSeen()
                onOpenGuide()
            },
            dismissTip = viewModel::onTipSeen,
            revealed = viewModel::onRevealed,
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class TodayActions(
    val openSettings: () -> Unit = {},
    /** The month, Tempo's own page: from the button beside the settings'. */
    val openMonth: () -> Unit = {},
    val askPermission: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val openEvent: (EventInstance) -> Unit = {},
    /** A new event from its start to its end, in the calendar app. */
    val newEvent: (begin: Instant, end: Instant) -> Unit = { _, _ -> },
    val openDay: (Instant) -> Unit = {},
    /** The phone's clock app, by its front door: from the time, and from the alarm's line. */
    val openClock: () -> Unit = {},
    /** The guide, from the first day's tip: the tip is then seen. */
    val openGuide: () -> Unit = {},
    val dismissTip: () -> Unit = {},
    /** The event the reader made is being brought into view: the state need not say it again. */
    val revealed: () -> Unit = {},
)

/**
 * Today (VISION.md, Today; PLANNING.md §11 Phase 3, reviewed on 9 Oct and 10 Nov 2026, §15): the
 * date and the page's two doors on top, the time as the hero, beside it the day's clock face while
 * today has something left to put on its ring, the day in one sentence, the next alarm; then
 * today's events, with the past folded and "now" said once (the event under way on the page's one
 * ground, or a line while nothing is); then the days ahead, each under its name: tomorrow in
 * full, the rest of the week a line an event, and a run of empty days said once. The new-event
 * button is its icon, at the bottom corner; back from it, the event made is brought into view
 * and glows once. The first day, a tip under the hero says how to read the dial (step 8.6).
 *
 * On a large screen (a tablet, a foldable open at 840 dp and up; step 8.7) the page stands in two
 * panes, each scrolling on its own: the hero, with a larger dial, today's all-day events and the
 * tip on the leading side; the agenda on the trailing one, the new-event button over it.
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
    val panes = twoPanes()
    // Each layout keeps its own place, so a tablet turned between the two does not bring the
    // list to another item: the column's first items are the hero's, the agenda pane's are not.
    val column = rememberLazyListState()
    val heroPane = rememberLazyListState()
    val agendaPane = rememberLazyListState()
    val lists = when (panes) {
        null -> TodayLists(column, column, twoPanes = false)
        else -> TodayLists(heroPane, agendaPane, twoPanes = true)
    }
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    val glowing = revealNewEvent(state, actions, lists, showEarlier, openEarlier = { showEarlier = true })
    val onToggleEarlier = { showEarlier = !showEarlier }

    Surface(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTimeColumn24 provides text.uses24Hour, LocalGlowingEvent provides glowing) {
            if (panes == null) {
                TodayPage(state, permission, actions, text, pageGutter(), column, showEarlier, onToggleEarlier)
            } else {
                TodayPanes(state, permission, actions, text, panes, lists, showEarlier, onToggleEarlier)
            }
        }
    }
}

/**
 * The page's lists: on a phone one, [hero] and [agenda] the same, the hero and the tip before the
 * agenda; on a large screen the two panes', today's all-day events in the hero's.
 */
private class TodayLists(val hero: LazyListState, val agenda: LazyListState, val twoPanes: Boolean)

@Composable
private fun TodayPage(
    state: TodayUiState,
    permission: CalendarPermission,
    actions: TodayActions,
    text: AgendaText,
    gutter: PageGutter,
    list: LazyListState,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            // The list ends under the status bar: scrolled, the page's words went up behind the
            // system's clock and icons and were read through them (seen on the phone, 10 Nov 2026).
            modifier = Modifier.fillMaxSize().statusBarsPadding().testTag(TodayTags.LIST),
            // Room for the button over the last row, whatever the list ends with.
            contentPadding = PaddingValues(bottom = navigationBar + 88.dp),
        ) {
            item(key = "hero") { Hero(state, text, gutter, actions) }
            if (showsTip(state)) card("tip", gutter) { TipCard(actions) }
            agendaContent(state, permission, text, gutter, actions, showEarlier, onToggleEarlier, todayAllDay = true)
        }
        NewEventButton(state, actions, gutter, Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * Today on a large screen (step 8.7): the hero's pane, about two fifths of the page, and the
 * agenda's beside it, each scrolling on its own.
 */
@Composable
private fun TodayPanes(
    state: TodayUiState,
    permission: CalendarPermission,
    actions: TodayActions,
    text: AgendaText,
    panes: TwoPanes,
    lists: TodayLists,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dial = heroDial(panes.leading, text.uses24Hour)
    val leading = panes.leadingGutter
    val trailing = panes.trailingGutter
    // Both panes end under the status bar, as the page's one list does.
    Row(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            state = lists.hero,
            modifier = Modifier.width(leading.start + panes.leading).fillMaxHeight().testTag(TodayTags.HERO_PANE),
            contentPadding = PaddingValues(bottom = navigationBar + 16.dp),
        ) {
            item(key = "hero") { Hero(state, text, leading, actions, dial) }
            val content = state.content
            val allDay = (content as? TodayContent.Ready)?.let { todayAllDayItem(it.agenda) }
            if (content is TodayContent.Ready && allDay != null) {
                row(allDay.key, leading) {
                    AgendaAllDayRow(
                        allDay.day.allDay,
                        content.calendars,
                        text,
                        actions.openEvent.takeIf { state.doors.canOpenEvent },
                        Modifier.testTag(TodayTags.ALL_DAY),
                    )
                }
            }
            if (showsTip(state)) card("tip", leading) { TipCard(actions) }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            LazyColumn(
                state = lists.agenda,
                modifier = Modifier.fillMaxSize().testTag(TodayTags.LIST),
                // Room for the button over the last row; its first is level with the date's.
                contentPadding = PaddingValues(bottom = navigationBar + 88.dp),
            ) {
                agendaContent(
                    state,
                    permission,
                    text,
                    trailing,
                    actions,
                    showEarlier,
                    onToggleEarlier,
                    todayAllDay = false,
                )
            }
            NewEventButton(state, actions, trailing, Modifier.align(Alignment.BottomEnd))
        }
    }
}

/**
 * The agenda's place on the page: what the calendar says, or the state's card that says why it
 * cannot, and the word on a missing calendar app after it.
 *
 * @param todayAllDay whether today's all-day events are drawn here; on a large screen they stand
 *   in the hero's pane.
 */
private fun LazyListScope.agendaContent(
    state: TodayUiState,
    permission: CalendarPermission,
    text: AgendaText,
    gutter: PageGutter,
    actions: TodayActions,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
    todayAllDay: Boolean,
) {
    when (val content = state.content) {
        TodayContent.Loading -> Unit

        TodayContent.NoPermission -> card("permission", gutter) {
            CalendarPermissionCard(permission, actions.askPermission, actions.openAppSettings)
        }

        TodayContent.NoCalendars -> card("no-calendars", gutter) {
            StatusCard(
                icon = TempoIcons.Calendar,
                title = stringResource(DesignR.string.state_no_calendars_title),
                body = stringResource(DesignR.string.state_no_calendars_body),
                tone = StatusTone.NOTE,
            )
        }

        TodayContent.AllHidden -> card("all-hidden", gutter) {
            StatusCard(
                icon = TempoIcons.Calendar,
                title = stringResource(DesignR.string.state_all_hidden_title),
                body = stringResource(DesignR.string.state_all_hidden_body),
                tone = StatusTone.CHOICE,
                action = stringResource(DesignR.string.state_all_hidden_action),
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
            todayAllDay = todayAllDay,
        )
    }
    if (state.content is TodayContent.Ready && !state.doors.canOpenEvent && !state.doors.canCreate) {
        card("no-app", gutter) {
            StatusCard(
                icon = TempoIcons.Info,
                title = stringResource(DesignR.string.state_no_app_title),
                body = stringResource(DesignR.string.state_no_app_body),
                tone = StatusTone.NOTE,
            )
        }
    }
}

/**
 * The new-event button, at the page's bottom corner (over the agenda's pane on a large screen):
 * its icon alone. Its label stood beside it at the top of the page, over the list's last rows on
 * the first screen, for a button the reader learns the first day (PLANNING.md §15, 10 Nov 2026);
 * a screen reader still hears what it does.
 */
@Composable
private fun NewEventButton(
    state: TodayUiState,
    actions: TodayActions,
    gutter: PageGutter,
    modifier: Modifier = Modifier,
) {
    if (!state.doors.canCreate) return
    FloatingActionButton(
        onClick = {
            val begin = nextHalfHour(state.now.toInstant(), state.now.zone)
            actions.newEvent(begin, begin.plus(CalendarIntents.NEW_EVENT_LENGTH))
        },
        modifier = modifier
            .navigationBarsPadding()
            .padding(gutter)
            .padding(ScreenMargin)
            .testTag(TodayTags.NEW_EVENT),
    ) {
        Icon(TempoIcons.Plus, contentDescription = stringResource(DesignR.string.agenda_new_event))
    }
}

/** A state's card on the page's column. */
private fun LazyListScope.card(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.padding(gutter).padding(horizontal = ScreenMargin, vertical = 6.dp)) { content() }
    }
}

/** One of the agenda's items, on the page's column: a row of today's, or a day ahead. */
private fun LazyListScope.row(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) { Box(Modifier.padding(gutter)) { content() } }
}

/**
 * The first day's tip (step 8.6): what the dial says, and the way to the guide for the rest. Shown
 * until it is dismissed or followed ([TodayUiState.showsTip]), and only while the dial it explains
 * has an arc on it ([heroArcs]): on a day with nothing left, or with its ring still empty, it waits.
 */
private fun showsTip(state: TodayUiState): Boolean = state.showsTip && !heroArcs(state).isNullOrEmpty()

/** See [showsTip]. */
@Composable
private fun TipCard(actions: TodayActions) {
    StatusCard(
        icon = TempoIcons.Clock,
        title = stringResource(R.string.today_tip_title),
        body = stringResource(R.string.today_tip_body),
        tone = StatusTone.NOTE,
        action = stringResource(R.string.today_tip_open),
        onAction = actions.openGuide,
        dismiss = stringResource(R.string.today_tip_dismiss),
        onDismiss = actions.dismissTip,
        modifier = Modifier.testTag(TodayTags.TIP),
    )
}

/**
 * Back from the calendar app with the event the reader made ([TodayUiState.reveal], step 8.6): the
 * list brought to it where it is not in view already (at once under reduced motion), today's past
 * events unfolded if it is one of them, then its row glowing once. The reveal is taken from the
 * state at once ([TodayActions.revealed]), so it is done once. Returns the event glowing now.
 */
@Composable
private fun revealNewEvent(
    state: TodayUiState,
    actions: TodayActions,
    lists: TodayLists,
    showEarlier: Boolean,
    openEarlier: () -> Unit,
): EventKey? {
    var revealing by remember { mutableStateOf<EventKey?>(null) }
    var glowing by remember { mutableStateOf<EventKey?>(null) }
    val current by rememberUpdatedState(state)
    val layout by rememberUpdatedState(lists)
    val unfolded by rememberUpdatedState(showEarlier)
    val reduced = reducedMotion()
    val density = LocalDensity.current
    // A row is brought a margin below the list's top, never against it.
    val top = with(density) { REVEAL_MARGIN.roundToPx() }
    LaunchedEffect(state.reveal) {
        val reveal = state.reveal ?: return@LaunchedEffect
        revealing = reveal
        actions.revealed()
    }
    LaunchedEffect(revealing) {
        val target = revealing ?: return@LaunchedEffect
        try {
            val content = current.content as? TodayContent.Ready ?: return@LaunchedEffect
            val opens = !unfolded && agendaItems(content.agenda, showEarlier = false).any { it.folds(target) }
            if (opens) openEarlier()
            val (list, index) = placeOf(target, content.agenda, current, layout, showEarlier = unfolded || opens)
                ?: return@LaunchedEffect
            // The rows of the morning come in with the next layout.
            withTimeoutOrNull(REVEAL_MILLIS) { snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > index } }
            if (!list.shows(index, top)) {
                if (reduced) list.scrollToItem(index, -top) else list.animateScrollToItem(index, -top)
            }
            glowing = target
            delay(REVEAL_MILLIS)
        } finally {
            if (glowing == target) glowing = null
            if (revealing == target) revealing = null
        }
    }
    return glowing
}

/**
 * Where the page draws [target]: its list, and its item there; null where it is not drawn. On a
 * large screen today's all-day events stand in the hero's pane, after the hero.
 */
private fun placeOf(
    target: EventKey,
    agenda: Agenda,
    state: TodayUiState,
    lists: TodayLists,
    showEarlier: Boolean,
): Pair<LazyListState, Int>? {
    val at = agendaItems(agenda, showEarlier, todayAllDay = !lists.twoPanes).indexOfFirst { it.holds(target) }
    return when {
        at >= 0 -> lists.agenda to (if (lists.twoPanes) 0 else leadingItems(state)) + at
        lists.twoPanes && todayAllDayItem(agenda)?.holds(target) == true -> lists.hero to 1
        else -> null
    }
}

/** The page's items before the agenda's, in its one column: the hero, and the tip while it is shown. */
private fun leadingItems(state: TodayUiState): Int = if (showsTip(state)) 2 else 1

/**
 * Whether the item at [index] is whole on screen, below [top] and above the room kept for the
 * new-event button.
 */
private fun LazyListState.shows(index: Int, top: Int): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return item.offset >= info.viewportStartOffset + top &&
        item.offset + item.size <= info.viewportEndOffset - info.afterContentPadding
}

private val REVEAL_MARGIN = 24.dp

/**
 * The top of the page: the date (a touch opens the calendar app on it, and its chevron says so) with the month's and the
 * settings' buttons; the time, large, and beside it the day's clock face while it has something
 * to show, the two one door to the phone's clock app ([TimeRow]); the day's sentence, its first
 * clause in ink and the rest quieter; then the next alarm and the second clock, under the two
 * things the page is for and never between them. The clock
 * grows with the reader's text size up to a cap, and shrinks to fit beside the dial: "9:10 PM"
 * stays on one line (PLANNING.md §15).
 *
 * @param dial the face's side: a phone's, or larger in the hero's pane on a large screen.
 */
@Composable
private fun Hero(
    state: TodayUiState,
    text: AgendaText,
    gutter: PageGutter,
    actions: TodayActions,
    dial: Dp = DIAL_SIZE,
) {
    val locale = LocalLocale.current.platformLocale
    val now = state.now
    val content = state.content
    val openDay = actions.openDay.takeIf { state.doors.canOpenDay }
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(gutter)
            // Room under the alarm line for its touch, which reaches past its words, clear of
            // the all-day events' under it; with no such line the list comes closer.
            .padding(bottom = if (state.alarm != null && state.doors.clockPackage != null) 16.dp else 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = HERO_EDGE, end = 4.dp),
        ) {
            // The date wears the chevron a day's name wears down the page: the same door, to the
            // calendar app on that day. Since the button beside it opens the month, the date is
            // the calendar app's one door up here, and has to say so (owner, PLANNING.md §15,
            // 23 Nov 2026).
            DayName(
                name = DateTimeText.date(now, locale, state.settings.dateStyle),
                opens = openDay != null,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = openDay != null,
                        onClickLabel = stringResource(DesignR.string.agenda_open_day),
                        role = Role.Button,
                    ) { openDay?.invoke(now.toInstant()) }
                    .heightIn(min = 48.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(end = 8.dp)
                    .testTag(TodayTags.DATE),
            )
            // The month is Tempo's own page: there with or without a calendar app to hand a day to.
            if (state.settings.showCalendarButton) {
                IconButton(onClick = actions.openMonth, modifier = Modifier.testTag(TodayTags.MONTH)) {
                    Icon(TempoIcons.Calendar, stringResource(R.string.today_open_month), tint = quiet)
                }
            }
            IconButton(onClick = actions.openSettings, modifier = Modifier.testTag(TodayTags.SETTINGS)) {
                Icon(TempoIcons.Settings, stringResource(R.string.today_settings), tint = quiet)
            }
        }
        TimeRow(state, text, actions, dial)
        if (content is TodayContent.Ready) {
            val sentence = text.sentenceText(content.sentence, now.toLocalDate())
            val ink = MaterialTheme.colorScheme.onSurface
            Text(
                text = remember(sentence, ink, quiet) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = ink, fontWeight = FontWeight.Medium)) { append(sentence.lead) }
                        append(sentence.rest)
                    }
                },
                // Balanced lines: the sentence never leaves its last word alone on a line. At the
                // title's size, its first clause told apart by weight and ink: a size larger, a
                // long sentence ("Oggi non resta altro; domani si comincia alle 10:00 con…") took
                // four lines and a sixth of the first screen (PLANNING.md §15, 10 Nov 2026).
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineBreak = LineBreak.Heading,
                ),
                color = quiet,
                modifier = Modifier
                    .padding(start = HERO_EDGE, end = HERO_EDGE, top = 12.dp)
                    .testTag(TodayTags.SENTENCE),
            )
        }
        // With no sentence between them (no calendar to read), the alarm's touch, which reaches
        // past its words, would run into the time's: the two doors are kept a finger apart.
        val underTime = if (content is TodayContent.Ready) 4.dp else 12.dp
        Column(Modifier.padding(start = HERO_EDGE, end = HERO_EDGE, top = underTime)) {
            val alarm = state.alarm
            if (alarm != null) {
                // A door to the clock app where there is one, with the line's own look: Compose
                // gives the touch its 48dp, and a 48dp row would push the list 20dp down (§15).
                val openClock = actions.openClock.takeIf { state.doors.clockPackage != null }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .then(
                            if (openClock != null) {
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.today_open_clock),
                                    role = Role.Button,
                                    onClick = openClock,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(vertical = 1.dp)
                        .padding(end = 6.dp)
                        .testTag(TodayTags.ALARM),
                ) {
                    Icon(TempoIcons.Bell, contentDescription = null, tint = quiet, modifier = Modifier.size(18.dp))
                    Text(
                        text = text.alarm(alarm, AlarmDay.of(alarm, now.toInstant(), now.zone)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = quiet,
                    )
                }
            }
            SecondClock(state, text)
        }
    }
}

/**
 * The time, the page's hero: its figures and, beside them, the day's clock face. The face is drawn
 * while today has an event left, even before it comes onto the ring: the hero keeps its shape
 * through the day, and does not say "nothing" beside an agenda with an afternoon in it. The
 * figures stand alone once the day is done, and with no calendar to read (PLANNING.md §15, 10 and
 * 11 Nov 2026).
 *
 * The two are one door to the phone's clock app (owner, PLANNING.md §15, 23 Nov 2026): the date
 * above them opens the calendar, the time opens the clock. On the whole row and not on the face
 * alone, which is gone in the evening, when an alarm is set. One touch target, read once by a
 * screen reader; no door where the phone has no clock app, or the reader turned it off.
 */
@Composable
private fun TimeRow(state: TodayUiState, text: AgendaText, actions: TodayActions, dial: Dp) {
    val locale = LocalLocale.current.platformLocale
    val now = state.now
    val arcs = remember(state.content, state.settings.showAllDay, now.zone) { heroArcs(state) }
    val openClock = actions.openClock.takeIf { state.settings.timeOpensClock && state.doors.clockPackage != null }
    val openLabel = stringResource(R.string.today_open_clock)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // The touch reaches a little past the figures and the face, which keep the hero's edge.
            .padding(start = HERO_EDGE - TIME_TOUCH, end = HERO_EDGE - TIME_TOUCH, top = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (openClock != null) {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = openClock)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = TIME_TOUCH)
            .testTag(TodayTags.TIME_ROW),
    ) {
        Box(Modifier.weight(1f)) { Clock(now, locale, text.uses24Hour, Modifier.testTag(TodayTags.TIME)) }
        if (arcs != null) {
            val time = DateTimeText.time(now, locale, text.uses24Hour)
            Spacer(Modifier.width(DIAL_GAP))
            DayDial(
                faceMinutes = DialArcs.minuteOnFace(now.toInstant(), now.zone),
                arcs = arcs,
                description = if (arcs.isEmpty()) {
                    stringResource(R.string.today_dial_later, time)
                } else {
                    stringResource(R.string.today_dial, time)
                },
                modifier = Modifier.size(dial).testTag(TodayTags.DIAL),
            )
        }
    }
}

/** How far the time's touch reaches past its figures and its face on each side. */
private val TIME_TOUCH = 8.dp

/**
 * The second clock (step 8.5): the reader's city and its time, and its date where it is not the
 * phone's ("Tokyo 06:20 · tomorrow"), under the alarm in the alarm's look; moved by the page's
 * minute ticker with the clock above it. Not drawn where it would repeat that clock (the reader is
 * in that city).
 */
@Composable
private fun SecondClock(state: TodayUiState, text: AgendaText) {
    val zone = SecondZone.of(state.settings.secondZone, state.now.zone) ?: return
    val line = text.secondClock(state.now.toInstant(), zone) ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(vertical = 1.dp)
            .semantics(mergeDescendants = true) { }
            .testTag(TodayTags.SECOND_CLOCK),
    ) {
        Icon(
            TempoIcons.Globe,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The dial's arcs, as the «At a glance» card places them: the focus in the accent, the rest quiet, in
 * their calendars' colours, as the list's bars under it. Null, and no face, with no timed event
 * left today or no agenda to read; empty, and a face with an empty ring, while today's events are
 * all still beyond its twelve hours (early in the day, an afternoon's).
 */
private fun heroArcs(state: TodayUiState): List<DialArc>? {
    val content = state.content as? TodayContent.Ready ?: return null
    if (DialArcs.today(content.agenda).isEmpty()) return null
    val focus = WordsDay.of(content.agenda, showAllDay = state.settings.showAllDay, showDaysAhead = true).focus
    return DialArcs.of(content.agenda, focus, state.now.zone, calendars = content.calendars)
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
    val density = LocalDensity.current
    val styles = clockStyles()
    val largest = styles.figures.fontSize
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(modifier.semantics { text = AnnotatedString(spoken) }) {
        val widths = measurer.clockWidths(parts, styles, density)
        val room = (constraints.maxWidth - widths.marker).coerceAtLeast(1)
        val wanted = widths.figures
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
                        style = styles.marker,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline().padding(horizontal = MARKER_GAP / 2),
                    )
                }
            }
            if (parts.markerFirst) marker()
            RollingFigures(
                figures = parts.figures,
                style = styles.figures.copy(fontSize = size, lineHeight = size * CLOCK_LINE),
                color = ink,
                modifier = Modifier.alignByBaseline(),
            )
            if (!parts.markerFirst) marker()
        }
    }
}

/** The clock's two styles: its figures at the largest size they take, and its day marker. */
private class ClockStyles(val figures: TextStyle, val marker: TextStyle)

@Composable
private fun clockStyles(): ClockStyles {
    // Through the density, so Android 14's non-linear text scaling is the one applied: the clock
    // grows with the reader's text size as the rest of the page does, up to its cap.
    val largest = with(LocalDensity.current) { minOf(CLOCK_SIZE.toDp(), CLOCK_CAP).toSp() }
    return ClockStyles(TempoTheme.type.heroNumber.copy(fontSize = largest), MaterialTheme.typography.titleLarge)
}

/** What the clock takes at its largest, in pixels: its figures, and its day marker with its gap (none on the 24-hour clock). */
private class ClockWidths(val figures: Int, val marker: Int)

private fun TextMeasurer.clockWidths(parts: TimeParts, styles: ClockStyles, density: Density): ClockWidths {
    val marker = parts.marker?.let {
        measure(AnnotatedString(it), styles.marker, maxLines = 1, softWrap = false).size.width +
            with(density) { MARKER_GAP.roundToPx() }
    } ?: 0
    // Each figure is laid out on its own and rounded up to a whole pixel, so the row is a
    // little wider than the string measured whole: counted, or the marker lost its last pixels.
    val figures = measure(AnnotatedString(parts.figures), styles.figures, maxLines = 1, softWrap = false).size.width +
        parts.figures.length + with(density) { CLOCK_SLACK.roundToPx() }
    return ClockWidths(figures, marker)
}

/**
 * The dial in the hero's pane on a large screen (step 8.7): the room the hero's row leaves beside
 * the clock at its largest, from a phone's [DIAL_SIZE] up to [DIAL_LARGEST], so the time is never
 * shrunk for the face. The clock is measured at the widest it reads in the reader's format (two
 * figures of hours, either day marker), so the face keeps its size through the day.
 *
 * @param pane the hero's pane, its gutter aside.
 */
@Composable
private fun heroDial(pane: Dp, uses24Hour: Boolean): Dp {
    val locale = LocalLocale.current.platformLocale
    val density = LocalDensity.current
    val styles = clockStyles()
    val measurer = rememberTextMeasurer()
    val clock = WIDEST_TIMES.maxOf { time ->
        val widths = measurer.clockWidths(DateTimeText.timeParts(time, locale, uses24Hour), styles, density)
        widths.figures + widths.marker
    }
    val row = pane - HERO_EDGE * 2
    return (row - DIAL_GAP - with(density) { clock.toDp() }).coerceIn(DIAL_SIZE, DIAL_LARGEST)
}

/** Two figures of hours, the widest minutes' place on a tabular face, and both day markers. */
private val WIDEST_TIMES = listOf(LocalTime.of(10, 58), LocalTime.of(22, 58))

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

/** The dial's side: the «At a glance» card's dial on a 2×2. */
private val DIAL_SIZE = 112.dp

/** The dial's largest, in the hero's pane on a large screen (step 8.7). */
private val DIAL_LARGEST = 200.dp

/** Between the clock's column and the dial. */
private val DIAL_GAP = 16.dp

/** What the hero's rows keep from its sides. */
private val HERO_EDGE = AgendaRowEdge

/**
 * The agenda's part of the list, item by item and in order: what [agenda] draws, and where a
 * reveal finds its event (step 8.6). Today is a row an item; a day ahead is one item.
 */
private sealed interface AgendaItem {
    val key: String

    /** Whether [event] is drawn in this item. */
    fun holds(event: EventKey): Boolean

    /** Whether [event] is among today's past events folded into this item. */
    fun folds(event: EventKey): Boolean = false

    data class AllDay(val day: AgendaDay) : AgendaItem {
        override val key get() = "all-day-${day.date}"

        override fun holds(event: EventKey) = day.allDay.any { EventKey.of(it.event) == event }
    }

    data class Line(val item: TimelineItem, val day: AgendaDay) : AgendaItem {
        override val key
            get() = when (item) {
                is TimelineItem.Event -> "event-${day.date}-${item.entry.event.eventId}-${item.entry.event.begin}"

                // Keyed by its end: the gap open now starts at now, which moves every minute.
                is TimelineItem.Free -> "free-${day.date}-${item.gap.end}"

                is TimelineItem.Earlier -> "earlier-${day.date}"

                TimelineItem.Now -> "now"
            }

        override fun holds(event: EventKey) = item is TimelineItem.Event && EventKey.of(item.entry.event) == event

        override fun folds(event: EventKey) =
            item is TimelineItem.Earlier && !item.open && item.entries.any { EventKey.of(it.event) == event }
    }

    data class Day(val section: AgendaSection) : AgendaItem {
        override val key
            get() = when (section) {
                is AgendaSection.Tomorrow -> "day-${section.day.date}"
                is AgendaSection.Later -> "day-${section.day.date}"
                is AgendaSection.Nothing -> "nothing-${section.from}"
                is AgendaSection.Today -> "today-${section.day.date}"
            }

        override fun holds(event: EventKey): Boolean {
            val day = when (section) {
                is AgendaSection.Tomorrow -> section.day
                is AgendaSection.Later -> section.day
                is AgendaSection.Nothing, is AgendaSection.Today -> return false
            }
            return day.allDay.any { EventKey.of(it.event) == event } || day.timed.any { EventKey.of(it.event) == event }
        }
    }
}

/** @param todayAllDay whether today's all-day events are among them: not where the hero's pane holds them. */
private fun agendaItems(agenda: Agenda, showEarlier: Boolean, todayAllDay: Boolean = true): List<AgendaItem> =
    AgendaSections.of(agenda).flatMap { section ->
        if (section is AgendaSection.Today) {
            val day = section.day
            listOfNotNull(AgendaItem.AllDay(day).takeIf { todayAllDay && day.allDay.isNotEmpty() }) +
                Timeline.of(day, isToday = true, showEarlier = showEarlier).map { AgendaItem.Line(it, day) }
        } else {
            listOf(AgendaItem.Day(section))
        }
    }

/** Today's all-day events as their item, where today has any: the hero's pane's on a large screen. */
private fun todayAllDayItem(agenda: Agenda): AgendaItem.AllDay? =
    agenda.days.firstOrNull()?.takeIf { it.allDay.isNotEmpty() }?.let { AgendaItem.AllDay(it) }

private fun LazyListScope.agenda(
    content: TodayContent.Ready,
    state: TodayUiState,
    text: AgendaText,
    gutter: PageGutter,
    actions: TodayActions,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
    todayAllDay: Boolean,
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
    agendaItems(content.agenda, showEarlier, todayAllDay).forEach { item ->
        when (item) {
            is AgendaItem.AllDay -> row(item.key, gutter) {
                AgendaAllDayRow(
                    item.day.allDay,
                    content.calendars,
                    text,
                    openEvent,
                    Modifier.testTag(TodayTags.ALL_DAY),
                )
            }

            is AgendaItem.Line -> row(item.key, gutter) {
                TimelineLine(item.item, item.day, content.calendars, text, now, openEvent, newEvent, onToggleEarlier)
            }

            is AgendaItem.Day -> row(item.key, gutter) {
                DaySection(item.section, content.calendars, text, now, style, openEvent, openDay)
            }
        }
    }
}

@Composable
private fun TimelineLine(
    item: TimelineItem,
    day: AgendaDay,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    now: Instant,
    openEvent: ((EventInstance) -> Unit)?,
    newEvent: ((Instant, Instant) -> Unit)?,
    onToggleEarlier: () -> Unit,
) {
    when (item) {
        is TimelineItem.Event -> AgendaEventRow(
            item.entry,
            day.date,
            calendars[item.entry.event.calendarId],
            text,
            now,
            openEvent,
            Modifier.testTag(TodayTags.event(item.entry.event.eventId)),
            today = true,
        )

        is TimelineItem.Free -> FreeRow(item.gap, text, now, newEvent)

        is TimelineItem.Earlier -> EarlierRow(item.entries.size, showEarlier = item.open, onToggle = onToggleEarlier)

        TimelineItem.Now -> NowRow()
    }
}

/** A day after today under its name: tomorrow in full, a later day a line an event, a run of empty days its heading. */
@Composable
private fun DaySection(
    section: AgendaSection,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    now: Instant,
    style: DateStyle,
    openEvent: ((EventInstance) -> Unit)?,
    openDay: ((LocalDate) -> Unit)?,
) {
    when (section) {
        // Today is drawn row by row, never as a section.
        is AgendaSection.Today -> Unit

        is AgendaSection.Tomorrow -> {
            val day = section.day
            DayHeading(
                title = stringResource(R.string.today_tomorrow),
                summary = text.summary(DaySummary.of(day)),
                onOpen = openDay?.let { { it(day.date) } },
                modifier = Modifier.testTag(TodayTags.day(day.date)),
            ) {
                if (day.allDay.isNotEmpty()) AgendaAllDayRow(day.allDay, calendars, text, openEvent)
                Timeline.of(day, isToday = false).forEach { item ->
                    if (item is TimelineItem.Event) {
                        val event = item.entry.event
                        AgendaEventRow(
                            item.entry,
                            day.date,
                            calendars[event.calendarId],
                            text,
                            now,
                            openEvent,
                            Modifier.testTag(TodayTags.event(event.eventId)),
                        )
                    }
                }
            }
        }

        is AgendaSection.Later -> {
            val day = section.day
            DayHeading(
                title = text.date(day.date, style),
                summary = text.summary(DaySummary.of(day)),
                onOpen = openDay?.let { { it(day.date) } },
                modifier = Modifier.testTag(TodayTags.day(day.date)),
            ) {
                // As today's and tomorrow's: "All day" beside each one broke in two in the times'
                // column ("Tutto il / giorno"), and the day's summary has said it already.
                if (day.allDay.isNotEmpty()) AgendaAllDayRow(day.allDay, calendars, text, openEvent)
                day.timed.forEach { entry -> CompactRow(entry, calendars, text, openEvent) }
            }
        }

        is AgendaSection.Nothing -> {
            val title = when {
                !section.isOneDay -> text.span(section.from, section.to, style, section.startsTomorrow)
                section.startsTomorrow -> stringResource(R.string.today_tomorrow)
                else -> text.date(section.from, style)
            }
            DayHeading(
                title = title,
                summary = text.nothingPlanned(section.days),
                onOpen = openDay?.let { { it(section.from) } },
                modifier = Modifier.testTag(TodayTags.day(section.from)),
            ) { }
        }
    }
}

/**
 * A gap of an hour or more: said, and, where a calendar app can take it, the way to fill it (a new
 * event that starts when the gap does, or at the next half hour in the gap open now). It writes no
 * time of its own: it starts where the row above it ends, or at the "now" line, and that time is
 * already there; a screen reader, which hears the row alone, is told it.
 */
@Composable
private fun FreeRow(gap: FreeGap, text: AgendaText, now: Instant, newEvent: ((Instant, Instant) -> Unit)?) {
    val length = text.duration(gap.duration)
    val opensNow = gap.start <= now
    val description = if (opensNow) {
        stringResource(R.string.a11y_free_now, length)
    } else {
        stringResource(R.string.a11y_free, text.time(gap.start), length)
    }
    val slot = newEventIn(gap, now, text.zone, CalendarIntents.NEW_EVENT_LENGTH)
    val newLabel = stringResource(DesignR.string.agenda_new_event)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (newEvent != null) {
                    Modifier.clickable(onClickLabel = newLabel) { newEvent(slot.start, slot.end) }
                } else {
                    Modifier.semantics(mergeDescendants = true) { }
                },
            )
            .semantics { contentDescription = description }
            .heightIn(min = 48.dp)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 6.dp)
            .testTag(TodayTags.FREE),
    ) {
        Spacer(Modifier.width(agendaTimeColumn))
        Spacer(Modifier.width(AgendaBarWidth))
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
                // Quiet: the accent on this page means now.
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = Modifier.width(agendaTimeColumn),
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
        Spacer(Modifier.width(agendaTimeColumn))
        Spacer(Modifier.width(AgendaBarWidth))
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

/**
 * A timed event of a day after tomorrow, briefly: its start, its calendar's dot, its title. One
 * begun the day before has no start on this date: it says when it ends under its title, where
 * the words have room ("fino alle 06:30" broke in two in the times' column).
 */
@Composable
private fun CompactRow(
    entry: TimedEntry,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    openEvent: ((EventInstance) -> Unit)?,
) {
    val event = entry.event
    val calendar = calendars[event.calendarId]
    val until = if (entry.startsOnDate) null else text.until(event.end)
    val inCalendar = calendar?.let { stringResource(DesignR.string.a11y_in_calendar, it.name) }
    val description = listOfNotNull(until ?: text.time(event.begin), text.title(event), inCalendar).joinToString(", ")
    val openLabel = stringResource(DesignR.string.a11y_open_event)
    RevealItem(glows(event), inset = 8.dp) {
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
                .clearAndSetSemantics { contentDescription = description }
                .heightIn(min = 48.dp)
                .padding(horizontal = AgendaRowEdge, vertical = 4.dp),
        ) {
            Text(
                text = if (until == null) agendaStartTime(text, event.begin) else AnnotatedString(""),
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(agendaTimeColumn),
            )
            // In the bar's place and no wider to the row, so every day's titles start on one line.
            Box(Modifier.width(AgendaBarWidth), contentAlignment = Alignment.Center) {
                CalendarDot(
                    event.color ?: calendar?.color,
                    MaterialTheme.colorScheme.surface,
                    Modifier.wrapContentSize(unbounded = true),
                    size = 8.dp,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = text.title(event),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (until != null) {
                    Text(
                        text = until,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Hooks for the UI tests. */
object TodayTags {
    /** The list that holds the agenda: the page's one, or the agenda's pane on a large screen. */
    const val LIST = "today_list"

    /** The hero's pane, on a large screen only. */
    const val HERO_PANE = "today_hero_pane"
    const val TIME = "today_time"

    /** The time's row, the figures and the face: the door to the clock app. */
    const val TIME_ROW = "today_time_row"
    const val DATE = "today_date"
    const val DIAL = "today_dial"
    const val ALARM = "today_alarm"
    const val SECOND_CLOCK = "today_second_clock"
    const val SENTENCE = "today_sentence"
    const val SETTINGS = "today_settings"
    const val MONTH = "today_month"
    const val NEW_EVENT = "today_new_event"
    const val ALL_DAY = "today_all_day"
    const val FREE = "today_free"
    const val NOW = "today_now"
    const val EARLIER = "today_earlier"
    const val TIP = "today_tip"

    fun event(id: Long) = "today_event_$id"

    fun day(date: LocalDate) = "today_day_$date"
}
