package com.callbackdev.tempo.feature.month

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarAccess
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.designsystem.components.AgendaAllDayRow
import com.callbackdev.tempo.core.designsystem.components.AgendaEventRow
import com.callbackdev.tempo.core.designsystem.components.AgendaRowEdge
import com.callbackdev.tempo.core.designsystem.components.CalendarPermissionCard
import com.callbackdev.tempo.core.designsystem.components.DayHeading
import com.callbackdev.tempo.core.designsystem.components.LocalGlowingEvent
import com.callbackdev.tempo.core.designsystem.components.LocalTimeColumn24
import com.callbackdev.tempo.core.designsystem.components.REVEAL_MILLIS
import com.callbackdev.tempo.core.designsystem.components.StatusCard
import com.callbackdev.tempo.core.designsystem.components.StatusTone
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.rememberAgendaText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.PageGutter
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.TwoPanes
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.designsystem.theme.tabular
import com.callbackdev.tempo.core.designsystem.theme.twoPanes
import com.callbackdev.tempo.core.domain.clock.newEventOn
import com.callbackdev.tempo.core.domain.month.MonthGrid
import com.callbackdev.tempo.core.domain.month.MonthPages
import com.callbackdev.tempo.core.domain.today.DaySummary
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.NewEvent
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.TimedEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import com.callbackdev.tempo.core.designsystem.R as DesignR

@Composable
fun MonthRoute(onBack: () -> Unit, onOpenSettings: () -> Unit, viewModel: MonthViewModel = hiltViewModel()) {
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
    // The calendar app opens on top of Tempo; what it changes is on this page when the reader is
    // back (CalendarChanges, PLANNING.md §4.5).
    val launch: (Intent) -> Unit = { intent -> runCatching { context.startActivity(intent) } }
    val current = state
    if (current == null) {
        Surface(Modifier.fillMaxSize()) { }
        return
    }
    MonthScreen(
        state = current,
        permission = permission,
        actions = MonthActions(
            back = onBack,
            showMonth = viewModel::show,
            openSettings = onOpenSettings,
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
            revealed = viewModel::onRevealed,
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class MonthActions(
    val back: () -> Unit = {},
    /** The month now on screen: its dates are read. */
    val showMonth: (YearMonth) -> Unit = {},
    val openSettings: () -> Unit = {},
    val askPermission: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val openEvent: (EventInstance) -> Unit = {},
    /** A new event from its start to its end, in the calendar app. */
    val newEvent: (begin: Instant, end: Instant) -> Unit = { _, _ -> },
    val openDay: (Instant) -> Unit = {},
    /** The event the reader made is being brought forward: the state need not say it again. */
    val revealed: () -> Unit = {},
)

/**
 * The month (VISION.md, The month; PLANNING.md §11 Phase 9): a whole month at a glance, the days
 * that hold something told by marks and never by titles, and under it the chosen day as Today
 * would list it, each event a touch from the calendar app. The page answers "which days are taken,
 * and what is on that one"; it stays a month, with no week and no day of its own, because Today is
 * the day.
 *
 * On top, the month's name: a touch on it goes to any month of any year, in two touches, and
 * "Today" stands beside it only while the page is somewhere else. The months turn under the
 * finger. A touch on a date chooses it; a long touch, or the button at the bottom corner, starts a
 * new event on it. Back from the calendar app, the event made is brought forward, its date chosen
 * and its row glowing once.
 *
 * On a large screen the month stands beside the day, each in its pane. Without the permission, or
 * a calendar, the month is drawn all the same and the day's place says why it is empty.
 *
 * @param initialMonth the month the page opens on: today's, save in a test.
 */
@Composable
fun MonthScreen(
    state: MonthUiState,
    permission: CalendarPermission,
    actions: MonthActions,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.from(state.today),
    initialDay: LocalDate = state.today,
) {
    val locale = LocalLocale.current.platformLocale
    val text = rememberAgendaText(state.now.zone, state.settings.clockFormat)
    val firstDay = remember(locale) { DateTimeText.firstDayOfWeek(locale) }
    val pager = rememberPagerState(initialPage = MonthPages.pageOf(initialMonth)) { MonthPages.COUNT }
    var selected by rememberSaveable(stateSaver = DateSaver) { mutableStateOf(initialDay) }
    var jumping by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reduced = reducedMotion()
    val today by rememberUpdatedState(state.today)
    val shown = MonthPages.monthOf(pager.currentPage)

    val turnTo: (YearMonth) -> Unit = { month -> scope.launch { pager.turnTo(MonthPages.pageOf(month), reduced) } }
    val choose: (LocalDate) -> Unit = { date ->
        selected = date
        if (YearMonth.from(date) != MonthPages.monthOf(pager.currentPage)) turnTo(YearMonth.from(date))
    }
    // A month come to rest is read, and holds the chosen day: today in its own month, else the
    // first, so the day under the grid is always one of the dates above it.
    val showMonth by rememberUpdatedState(actions.showMonth)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val month = MonthPages.monthOf(page)
            showMonth(month)
            if (YearMonth.from(selected) != month) {
                selected = if (month == YearMonth.from(today)) today else month.atDay(1)
            }
        }
    }

    val panes = twoPanes()
    val column = rememberLazyListState()
    val dayPane = rememberLazyListState()
    val list = if (panes == null) column else dayPane
    val glowing = revealNewEvent(state, actions, list, leading = if (panes == null) COLUMN_LEADING else 0, choose)

    val view = MonthView(
        state = state,
        permission = permission,
        actions = actions,
        text = text,
        pager = pager,
        firstDay = firstDay,
        shown = shown,
        selected = selected,
        choose = choose,
        turnBy = { months -> turnTo(MonthPages.monthOf(pager.currentPage + months)) },
        goToday = { choose(today) },
        jump = { jumping = true },
    )
    Surface(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTimeColumn24 provides text.uses24Hour, LocalGlowingEvent provides glowing) {
            if (panes == null) MonthPage(view, pageGutter(), column) else MonthPanes(view, panes, dayPane)
        }
    }
    if (jumping) {
        MonthJumpSheet(
            shown = shown,
            current = YearMonth.from(today),
            onPick = { month ->
                jumping = false
                turnTo(month)
            },
            onDismiss = { jumping = false },
        )
    }
}

/**
 * The page as its parts need it: what the calendar says, where the reader is (the month on
 * screen, the chosen day), and what a touch does.
 */
private class MonthView(
    val state: MonthUiState,
    val permission: CalendarPermission,
    val actions: MonthActions,
    val text: AgendaText,
    val pager: PagerState,
    val firstDay: DayOfWeek,
    val shown: YearMonth,
    val selected: LocalDate,
    val choose: (LocalDate) -> Unit,
    val turnBy: (Int) -> Unit,
    val goToday: () -> Unit,
    val jump: () -> Unit,
) {
    /** Whether the page is somewhere else than today: only then is "Today" a way back, and drawn. */
    val away: Boolean get() = shown != YearMonth.from(state.today) || selected != state.today

    /** A new event on [date], where a calendar app takes one: from the button, and a long touch on a date. */
    val newEvent: ((LocalDate) -> Unit)?
        get() = if (state.doors.canCreate) {
            { date ->
                choose(date)
                val begin = newEventOn(date, state.now.toInstant(), state.now.zone)
                actions.newEvent(begin, begin.plus(CalendarIntents.NEW_EVENT_LENGTH))
            }
        } else {
            null
        }

    val days: GridDays
        get() = GridDays(
            marks = (state.content as? MonthContent.Ready)?.marks,
            today = state.today,
            selected = selected,
            onChoose = choose,
            onNew = newEvent,
        )
}

/** Turns to [page]: under the eye when it is near, at once when it is years away or motion is reduced. */
private suspend fun PagerState.turnTo(page: Int, reduced: Boolean) {
    if (reduced || abs(page - currentPage) > NEAR_PAGES) scrollToPage(page) else animateScrollToPage(page)
}

/** A phone's page: the month, and the chosen day under it, in one column that scrolls as one. */
@Composable
private fun MonthPage(view: MonthView, gutter: PageGutter, list: LazyListState) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().statusBarsPadding().testTag(MonthTags.LIST),
            // Room for the button over the last row, whatever the day ends with.
            contentPadding = PaddingValues(bottom = navigationBar + 88.dp),
        ) {
            item(key = "top") { MonthTopBar(view, Modifier.padding(gutter)) }
            item(key = "grid") {
                Column(Modifier.padding(gutter)) {
                    Weekdays(view.firstDay)
                    MonthPager(view.pager, view.firstDay, view.days)
                }
            }
            dayPanel(view, gutter)
        }
        NewEventButton(view, gutter, Modifier.align(Alignment.BottomEnd))
    }
}

/** The page's items before the day's, in its one column: the top row and the grid. */
private const val COLUMN_LEADING = 2

/** On a large screen: the month in the leading pane, the chosen day beside it, the button over the day. */
@Composable
private fun MonthPanes(view: MonthView, panes: TwoPanes, list: LazyListState) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val leading = panes.leadingGutter
    val trailing = panes.trailingGutter
    Row(Modifier.fillMaxSize().statusBarsPadding()) {
        BoxWithConstraints(Modifier.width(leading.start + panes.leading).fillMaxHeight()) {
            // The month fills its pane as a wall calendar fills its page: six weeks share the
            // height the top rows leave, up to a size past which a date is only air.
            val least = dayCellHeight()
            val room = (maxHeight - PANE_TOP_ROWS - navigationBar) / MonthGrid.MOST_WEEKS
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(leading)
                    .padding(bottom = navigationBar + 16.dp)
                    .testTag(MonthTags.MONTH_PANE),
            ) {
                MonthTopBar(view)
                Weekdays(view.firstDay)
                MonthPager(view.pager, view.firstDay, view.days, cell = room.coerceIn(least, least * LARGEST_CELL))
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().testTag(MonthTags.LIST),
                // The day's name level with the month's; room for the button over the last row.
                contentPadding = PaddingValues(top = 2.dp, bottom = navigationBar + 88.dp),
            ) {
                dayPanel(view, trailing, underGrid = false)
            }
            NewEventButton(view, trailing, Modifier.align(Alignment.BottomEnd))
        }
    }
}

/**
 * The page's top row: the way back; the month and its year, a touch on them opening the page that
 * goes to any month; "Today", only while the page is elsewhere; and the two arrows to the months
 * beside, where the row has room for them at the reader's text size. The finger turns the months;
 * the arrows are for a pointer, and for whoever does not swipe.
 *
 * "Today" is an icon ([TodayIcon]): as a word it took the room the title needs, and away from the
 * current month, where the year matters, the title read "November 20…" on a phone (PLANNING.md
 * §15, 26 Nov 2026).
 */
@Composable
private fun MonthTopBar(view: MonthView, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val reduced = reducedMotion()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val arrows = maxWidth / LocalDensity.current.fontScale >= ARROWS_FROM
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = view.actions.back, modifier = Modifier.testTag(MonthTags.BACK)) {
                Icon(TempoIcons.Back, contentDescription = stringResource(R.string.month_back))
            }
            Box(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable(
                            onClickLabel = stringResource(R.string.month_choose),
                            role = Role.Button,
                            onClick = view.jump,
                        )
                        .semantics { heading() }
                        .heightIn(min = 48.dp)
                        .padding(start = 8.dp, end = 4.dp)
                        .testTag(MonthTags.TITLE),
                ) {
                    Text(
                        text = DateTimeText.month(view.shown, locale),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        TempoIcons.ChevronRight,
                        contentDescription = null,
                        tint = quiet,
                        modifier = Modifier.padding(start = 2.dp).size(20.dp).rotate(QUARTER_TURN),
                    )
                }
            }
            AnimatedVisibility(
                visible = view.away,
                enter = fadeIn(TempoMotion.effects(reduced)),
                exit = fadeOut(TempoMotion.effects(reduced)),
            ) {
                IconButton(onClick = view.goToday, modifier = Modifier.testTag(MonthTags.TODAY)) {
                    TodayIcon(view.state.today.dayOfMonth, stringResource(R.string.month_today))
                }
            }
            if (arrows) {
                val page = view.pager.currentPage
                IconButton(
                    onClick = { view.turnBy(-1) },
                    enabled = page > 0,
                    modifier = Modifier.testTag(MonthTags.PREVIOUS),
                ) {
                    Icon(TempoIcons.ChevronLeft, stringResource(R.string.month_previous), tint = quiet)
                }
                IconButton(
                    onClick = { view.turnBy(1) },
                    enabled = page < MonthPages.COUNT - 1,
                    modifier = Modifier.testTag(MonthTags.NEXT),
                ) {
                    Icon(TempoIcons.ChevronRight, stringResource(R.string.month_next), tint = quiet)
                }
            }
        }
    }
}

/**
 * The way back to today: a calendar's page with today's number on it, in the accent. The number
 * is part of the drawing, so it keeps the icon's size whatever the reader's text size; a screen
 * reader hears [label] alone.
 */
@Composable
private fun TodayIcon(day: Int, label: String) {
    val locale = LocalLocale.current.platformLocale
    val accent = MaterialTheme.colorScheme.primary
    val figures = with(LocalDensity.current) { TODAY_FIGURES.toSp() }
    Box(Modifier.size(24.dp).clearAndSetSemantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(TempoIcons.CalendarPage, contentDescription = null, tint = accent)
        Text(
            text = String.format(locale, "%d", day),
            style = MaterialTheme.typography.labelSmall.tabular().copy(
                fontSize = figures,
                lineHeight = figures,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = accent,
            maxLines = 1,
            softWrap = false,
            // The page's middle, under its rings: a little below the icon's own.
            modifier = Modifier.offset(y = 1.5.dp),
        )
    }
}

/** The height of the number on [TodayIcon]'s page: two figures fit between its sides. */
private val TODAY_FIGURES = 9.dp

/**
 * The chosen day, under the month: its name and the day in a few words (a touch opens the calendar
 * app on it), its all-day events, its events in order, each as Today draws it and a touch from the
 * calendar app. Or the state's card that says why the calendar gives nothing.
 *
 * @param underGrid whether the day stands under the month and not beside it: a hairline then
 *   parts the two, the page's own (a day's name draws none).
 */
private fun LazyListScope.dayPanel(view: MonthView, gutter: PageGutter, underGrid: Boolean = true) {
    val state = view.state
    val actions = view.actions
    when (val content = state.content) {
        MonthContent.Loading -> Unit

        MonthContent.NoPermission -> card("permission", gutter) {
            CalendarPermissionCard(view.permission, actions.askPermission, actions.openAppSettings)
        }

        MonthContent.NoCalendars -> card("no-calendars", gutter) {
            StatusCard(
                icon = TempoIcons.Calendar,
                title = stringResource(DesignR.string.state_no_calendars_title),
                body = stringResource(DesignR.string.state_no_calendars_body),
                tone = StatusTone.NOTE,
            )
        }

        MonthContent.AllHidden -> card("all-hidden", gutter) {
            StatusCard(
                icon = TempoIcons.Calendar,
                title = stringResource(DesignR.string.state_all_hidden_title),
                body = stringResource(DesignR.string.state_all_hidden_body),
                tone = StatusTone.CHOICE,
                action = stringResource(DesignR.string.state_all_hidden_action),
                onAction = actions.openSettings,
            )
        }

        is MonthContent.Ready -> {
            val date = view.selected
            val day = content.days[date]
            val now = state.now.toInstant()
            val openEvent = actions.openEvent.takeIf { state.doors.canOpenEvent }
            val openDay = actions.openDay.takeIf { state.doors.canOpenDay }
            panelItems(date, day).forEach { item ->
                row(item.key, gutter) {
                    when (item) {
                        is PanelItem.Heading -> Column {
                            if (underGrid) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = AgendaRowEdge, end = AgendaRowEdge, top = 8.dp),
                                )
                            }
                            DayHeading(
                                title = view.text.date(date, state.settings.dateStyle),
                                // A date not read yet (the page is on its way to a far month): its name alone.
                                summary = day?.let { view.text.summary(DaySummary.of(it)) }.orEmpty(),
                                onOpen = openDay?.let { open ->
                                    { open(date.atStartOfDay(state.now.zone).toInstant()) }
                                },
                                modifier = Modifier.testTag(MonthTags.DAY),
                            )
                        }

                        is PanelItem.AllDay -> AgendaAllDayRow(
                            item.day.allDay,
                            content.calendars,
                            view.text,
                            openEvent,
                            Modifier.testTag(MonthTags.ALL_DAY),
                        )

                        is PanelItem.Event -> AgendaEventRow(
                            entry = item.entry,
                            date = date,
                            calendar = content.calendars[item.entry.event.calendarId],
                            text = view.text,
                            now = now,
                            openEvent = openEvent,
                            modifier = Modifier.testTag(MonthTags.event(item.entry.event.eventId)),
                            today = date == state.today,
                        )
                    }
                }
            }
        }
    }
    if (state.content is MonthContent.Ready && !state.doors.canOpenEvent && !state.doors.canCreate) {
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

/** The chosen day's part of the list, item by item and in order: what [dayPanel] draws, and where a reveal finds its event. */
private sealed interface PanelItem {
    val key: String

    data class Heading(val date: LocalDate) : PanelItem {
        override val key get() = "day-$date"
    }

    data class AllDay(val day: AgendaDay) : PanelItem {
        override val key get() = "all-day-${day.date}"
    }

    data class Event(val entry: TimedEntry, val date: LocalDate) : PanelItem {
        override val key get() = "event-$date-${entry.event.eventId}-${entry.event.begin}"
    }
}

private fun panelItems(date: LocalDate, day: AgendaDay?): List<PanelItem> = buildList {
    add(PanelItem.Heading(date))
    if (day == null) return@buildList
    if (day.allDay.isNotEmpty()) add(PanelItem.AllDay(day))
    day.timed.forEach { add(PanelItem.Event(it, date)) }
}

/** A state's card on the page's column. */
private fun LazyListScope.card(key: String, gutter: PageGutter, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.padding(gutter).padding(horizontal = ScreenMargin, vertical = 6.dp)) { content() }
    }
}

/**
 * One of the day's items, on the page's column. A day chosen after another fades in where the
 * other fades out, and nothing moves under reduced motion.
 */
private fun LazyListScope.row(key: String, gutter: PageGutter, content: @Composable LazyItemScope.() -> Unit) {
    item(key = key) {
        val reduced = reducedMotion()
        Box(
            Modifier
                .animateItem(
                    fadeInSpec = TempoMotion.effects(reduced),
                    placementSpec = if (reduced) null else TempoMotion.spatial(),
                    fadeOutSpec = TempoMotion.effects(reduced),
                )
                .padding(gutter),
        ) { content() }
    }
}

/**
 * The new-event button, at the page's bottom corner (over the day's pane on a large screen): a new
 * event on the chosen day. Not drawn where no calendar app takes one. In the scheme's second
 * colour, as Today's: the colour the chosen day wears in the grid.
 */
@Composable
private fun NewEventButton(view: MonthView, gutter: PageGutter, modifier: Modifier = Modifier) {
    val new = view.newEvent ?: return
    val locale = LocalLocale.current.platformLocale
    val date = DateTimeText.date(view.selected, locale, DateStyle.LONG, startsLine = false)
    FloatingActionButton(
        onClick = { new(view.selected) },
        modifier = modifier
            .navigationBarsPadding()
            .padding(gutter)
            .padding(ScreenMargin)
            .testTag(MonthTags.NEW_EVENT),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(TempoIcons.Plus, contentDescription = stringResource(R.string.month_new_event_on, date))
    }
}

/**
 * Back from the calendar app with the event the reader made ([MonthUiState.reveal]): its date
 * chosen, its month turned to if it is another, the list brought to its row where the row is not
 * in view, and the row glowing once. Taken from the state at once ([MonthActions.revealed]), so it
 * is done once. Returns the event glowing now.
 *
 * @param leading the list's items before the day's.
 */
@Composable
private fun revealNewEvent(
    state: MonthUiState,
    actions: MonthActions,
    list: LazyListState,
    leading: Int,
    choose: (LocalDate) -> Unit,
): EventKey? {
    var revealing by remember { mutableStateOf<NewEvent?>(null) }
    var glowing by remember { mutableStateOf<EventKey?>(null) }
    val current by rememberUpdatedState(state)
    val reduced = reducedMotion()
    LaunchedEffect(state.reveal) {
        val reveal = state.reveal ?: return@LaunchedEffect
        revealing = reveal
        actions.revealed()
    }
    LaunchedEffect(revealing) {
        val target = revealing ?: return@LaunchedEffect
        try {
            choose(target.date)
            val day = (current.content as? MonthContent.Ready)?.days?.get(target.date)
            val at = panelItems(target.date, day).indexOfFirst { it.holds(target.key) }
            if (at >= 0) {
                val index = leading + at
                // The day's rows come in with the next layout.
                withTimeoutOrNull(REVEAL_MILLIS) {
                    snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > index }
                }
                if (list.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                    if (reduced) list.scrollToItem(index) else list.animateScrollToItem(index)
                }
            }
            glowing = target.key
            delay(REVEAL_MILLIS)
        } finally {
            if (glowing == target.key) glowing = null
            if (revealing == target) revealing = null
        }
    }
    return glowing
}

private fun PanelItem.holds(event: EventKey): Boolean = when (this) {
    is PanelItem.Heading -> false
    is PanelItem.AllDay -> day.allDay.any { EventKey.of(it.event) == event }
    is PanelItem.Event -> EventKey.of(entry.event) == event
}

/** The chosen day across a rotation: a date is its day's count. */
private val DateSaver = Saver<LocalDate, Long>(save = { it.toEpochDay() }, restore = { LocalDate.ofEpochDay(it) })

/** A month this near is turned to under the eye; a farther one at once, without a year of pages flying by. */
private const val NEAR_PAGES = 2

/** What stands over the grid in its pane, and the air under it: the top row, the week's days. */
private val PANE_TOP_ROWS = 112.dp

/** How far a date's cell grows in a large screen's pane, in cells. */
private const val LARGEST_CELL = 1.75f

/** The narrowest top row, at the usual text size, that holds the arrows beside the month's name and "Today". */
private val ARROWS_FROM = 380.dp

private const val QUARTER_TURN = 90f

/** Hooks for the UI tests. */
object MonthTags {
    /** The list that holds the chosen day: the page's one, or the day's pane on a large screen. */
    const val LIST = "month_list"

    /** The month's pane, on a large screen only. */
    const val MONTH_PANE = "month_pane"
    const val GRID = "month_grid"
    const val BACK = "month_back"
    const val TITLE = "month_title"
    const val TODAY = "month_today"
    const val PREVIOUS = "month_previous"
    const val NEXT = "month_next"
    const val NEW_EVENT = "month_new_event"

    /** The chosen day's name, over its events. */
    const val DAY = "month_day"
    const val ALL_DAY = "month_all_day"
    const val JUMP = "month_jump"
    const val JUMP_YEAR = "month_jump_year"

    /** A date in its own month's grid. */
    fun day(date: LocalDate) = "month_day_$date"

    /** A date drawn in the grid of the month beside its own. */
    fun beside(date: LocalDate) = "month_beside_$date"

    fun event(id: Long) = "month_event_$id"

    /** A month to go to, in the page that jumps. */
    fun jump(month: Int) = "month_jump_$month"
}
