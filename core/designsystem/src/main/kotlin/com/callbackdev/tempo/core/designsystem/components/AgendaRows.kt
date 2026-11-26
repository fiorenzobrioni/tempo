package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.GroupShape
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.designsystem.theme.tabular
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/*
 * A day's rows, as every page that lists a day draws them (Today's agenda, the month's chosen
 * day): one place, so an event never looks two ways in one app. Each is a plain function of what
 * it shows; the page gives the door a touch opens, or none where no calendar app takes it.
 */

/** What a day's rows keep from the page's sides: the hero's edge, so a page has one. */
val AgendaRowEdge = ScreenMargin + 4.dp

/** A calendar's bar ([CalendarBar]): what a row without one keeps in its place. */
val AgendaBarWidth = 4.dp

/** Whether the page writes times on the 24-hour clock, for [agendaTimeColumn]'s width. */
val LocalTimeColumn24 = staticCompositionLocalOf { true }

/** The event the reader just made, glowing on its row; null nearly always. */
val LocalGlowingEvent = compositionLocalOf<EventKey?> { null }

/** Whether [event]'s row is the one glowing. */
@Composable
@ReadOnlyComposable
fun glows(event: EventInstance): Boolean = LocalGlowingEvent.current == EventKey.of(event)

/**
 * The width of the times' column: enough for "10:00" on the 24-hour clock and "10:00 PM" on the
 * 12-hour one (the end's, whole; the start's day marker is set small), grown with the reader's
 * text size (at twice the size a fixed 56dp broke "10:00" in two), so every row's title starts on
 * one line.
 */
val agendaTimeColumn: Dp
    @Composable get() {
        val base = if (LocalTimeColumn24.current) 56.dp else 68.dp
        return base * LocalDensity.current.fontScale.coerceAtLeast(1f)
    }

/**
 * A day under its name: the name, the day in a few words under it, a touch on them opening the
 * calendar app on that day, and its rows after them. A run of empty days is its heading alone. No
 * ground of its own: on the calendar's side of a page the one ground is the event under way's.
 *
 * The name is a size above its rows' titles and parts the day from the one before by that alone:
 * at the rows' own size it needed a hairline over it to be a heading, and a page of days was a
 * page of lines (PLANNING.md §15, 26 Nov 2026). The air over it is the page's to give.
 */
@Composable
fun DayHeading(
    title: String,
    summary: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val openLabel = stringResource(R.string.agenda_open_day)
    Column(modifier.fillMaxWidth()) {
        Column(
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
                .padding(start = AgendaRowEdge, end = AgendaRowEdge, top = 12.dp, bottom = 6.dp),
        ) {
            // Balanced, so a long name ("Da giovedì 12 novembre a sabato 14 novembre") breaks in
            // two halves and never leaves a word alone.
            DayName(
                name = title,
                opens = onOpen != null,
                style = MaterialTheme.typography.titleLarge.copy(lineBreak = LineBreak.Heading),
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/**
 * A day's name, a heading, with the chevron that says a touch on it opens the calendar app on that
 * day: one sign for one door, on a day of the agenda, on the month's chosen day, and on Today's
 * own date (owner, PLANNING.md §15, 23 Nov 2026). No chevron where no app takes the day.
 *
 * The chevron is set in the line, after the name's last word: beside the text it went to the
 * page's far side as soon as a long name took two lines. A screen reader hears the name alone;
 * the touch, and what it opens, are the caller's ([modifier]).
 */
@Composable
fun DayName(name: String, opens: Boolean, style: TextStyle, modifier: Modifier = Modifier) {
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = remember(name, opens) {
            buildAnnotatedString {
                append(name)
                if (opens) {
                    // Joined to the name's last word: where the name breaks, the chevron goes
                    // with that word and never stands on a line of its own.
                    append(WORD_JOINER)
                    appendInlineContent(CHEVRON)
                }
            }
        },
        style = style,
        inlineContent = mapOf(
            CHEVRON to InlineTextContent(
                Placeholder(1.5.em, 1.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                Icon(
                    TempoIcons.ChevronRight,
                    contentDescription = null,
                    tint = quiet,
                    modifier = Modifier.fillMaxSize().padding(start = 4.dp),
                )
            },
        ),
        modifier = modifier.clearAndSetSemantics {
            this.text = AnnotatedString(name)
            heading()
        },
    )
}

/** The chevron's place in a day's name ([DayName]). */
private const val CHEVRON = "chevron"

/** No line breaks on either side of it, and nothing drawn. */
private const val WORD_JOINER = '\u2060'

/**
 * The all-day events, over the timeline: a holiday is the day's frame, not one of its rows. Each
 * its calendar's dot and its title, with no ground: what they say is short, and a row of filled
 * shapes over the list was the loudest thing under the sentence.
 *
 * @param ground what the row stands on, for its dots: the page, or the hero's pane on a large
 *   screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendaAllDayRow(
    entries: List<AllDayEntry>,
    calendars: Map<Long, CalendarInfo>,
    text: AgendaText,
    openEvent: ((EventInstance) -> Unit)?,
    modifier: Modifier = Modifier,
    ground: Color = MaterialTheme.colorScheme.surface,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ALL_DAY_GAP - ALL_DAY_TOUCH * 2),
        // Each one's touch reaches a little past its words, so its dot stands on the page's edge.
        modifier = modifier.padding(horizontal = AgendaRowEdge - ALL_DAY_TOUCH),
    ) {
        entries.forEach { entry -> AllDayMark(entry, calendars[entry.event.calendarId], text, openEvent, ground) }
    }
}

/** Between one all-day event's words and the next one's dot. */
private val ALL_DAY_GAP = 20.dp

/** How far an all-day event's touch, and its glow, reach past its words on each side. */
private val ALL_DAY_TOUCH = 8.dp

@Composable
private fun AllDayMark(
    entry: AllDayEntry,
    calendar: CalendarInfo?,
    text: AgendaText,
    openEvent: ((EventInstance) -> Unit)?,
    ground: Color,
) {
    val event = entry.event
    val dayOf = if (entry.dayCount > 1) {
        stringResource(R.string.agenda_day_of, entry.dayNumber, entry.dayCount)
    } else {
        null
    }
    val description = listOfNotNull(
        stringResource(R.string.agenda_all_day),
        text.title(event),
        dayOf,
        calendar?.let { stringResource(R.string.a11y_in_calendar, it.name) },
    ).joinToString(", ")
    val openLabel = stringResource(R.string.a11y_open_event)
    val shape = MaterialTheme.shapes.medium
    RevealItem(glows(event), shape = shape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(shape)
                .then(
                    if (openEvent != null) {
                        Modifier.clickable(onClickLabel = openLabel) { openEvent(event) }
                    } else {
                        Modifier.semantics(mergeDescendants = true) { }
                    },
                )
                .clearAndSetSemantics { contentDescription = description }
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .padding(horizontal = ALL_DAY_TOUCH),
        ) {
            CalendarDot(event.color ?: calendar?.color, ground, size = 8.dp)
            Text(
                text = text.title(event),
                style = MaterialTheme.typography.labelLarge,
                // Two lines before it is cut: a birthday's name alone filled the first.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
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
}

/**
 * One event on a day's timeline: its times in a column (the start's day marker set small: "AM" ten
 * times down a column was its loudest word), its calendar's colour as a bar, its title, its
 * place, and what is true of it now. The event under way stands on the accent's ground, the one
 * place a page says "now" (as a tone of the page it was 1.1:1 from it, and now was the quietest
 * thing on the page: PLANNING.md §15, 26 Nov 2026), with how long is left; today's others are a
 * little heavier than another day's ([today]); past ones are quieter; a declined invitation is
 * struck through; an event written in a zone whose clock reads another time says its start there
 * ("08:00 in Lisbon"). TalkBack reads it as one sentence: "10:00 to 11:00, Dentist, Via Roma 3,
 * in Personal".
 *
 * @param modifier the row's own, on its touch target: a page's test tag.
 */
@Composable
fun AgendaEventRow(
    entry: TimedEntry,
    date: LocalDate,
    calendar: CalendarInfo?,
    text: AgendaText,
    now: Instant,
    openEvent: ((EventInstance) -> Unit)?,
    modifier: Modifier = Modifier,
    today: Boolean = false,
) {
    val event = entry.event
    val underWay = entry.state == EntryState.UNDER_WAY
    val quiet = entry.state == EntryState.PAST || event.selfStatus == AttendeeStatus.DECLINED
    val scheme = MaterialTheme.colorScheme
    val ground = if (underWay) scheme.primaryContainer else scheme.surface
    val titleStyle = when {
        underWay -> MaterialTheme.typography.titleMedium
        today && !quiet -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        else -> MaterialTheme.typography.bodyLarge
    }
    val ink = when {
        underWay -> scheme.onPrimaryContainer
        quiet -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val strike = if (event.selfStatus == AttendeeStatus.DECLINED) TextDecoration.LineThrough else null
    val description = describe(entry, date, calendar, text, now)
    val openLabel = stringResource(R.string.a11y_open_event)
    val notes = listOfNotNull(
        text.ownZone(event),
        startedNote(entry, date, text),
        endsNote(entry, date, text),
        stringResource(R.string.agenda_declined).takeIf { event.selfStatus == AttendeeStatus.DECLINED },
        stringResource(R.string.agenda_maybe).takeIf { event.selfStatus == AttendeeStatus.TENTATIVE },
        stringResource(R.string.agenda_not_answered).takeIf { event.awaitsAnswer },
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
        .then(modifier)
    val row: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                // On its ground the row keeps the page's edge: the ground stands 8dp from the sides.
                .padding(
                    horizontal = if (underWay) AgendaRowEdge - 8.dp else AgendaRowEdge,
                    vertical = if (underWay) 12.dp else 8.dp,
                )
                .clearAndSetSemantics { },
        ) {
            Column(Modifier.width(agendaTimeColumn)) {
                Text(
                    text = agendaStartTime(text, event.begin),
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = ink,
                )
                if (event.end != event.begin) {
                    Text(
                        text = text.time(event.end),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarBar(event.color ?: calendar?.color, ground)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = text.title(event),
                    style = titleStyle,
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
        RevealItem(glows(event), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), shape = GroupShape) {
            Surface(color = ground, shape = GroupShape) { Box(target) { row() } }
        }
    } else {
        // Kept from the page's edges as the event under way's ground is.
        RevealItem(glows(event), inset = 8.dp) { Box(target) { row() } }
    }
}

/**
 * A row's start with its day marker set small and quiet, where the reader's clock writes one: the
 * locale's own text, order and space, so "10:00 AM" and "오전 10:00" both keep theirs.
 */
@Composable
fun agendaStartTime(text: AgendaText, moment: Instant): AnnotatedString {
    val whole = text.time(moment)
    val marker = text.marker(moment) ?: return AnnotatedString(whole)
    val at = whole.indexOf(marker)
    if (at < 0) return AnnotatedString(whole)
    val small = SpanStyle(
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return buildAnnotatedString {
        append(whole)
        addStyle(small, at, at + marker.length)
    }
}

/**
 * How far the event under way has come, and how long is left: a bar in the accent on a track of
 * its own tone, thick enough to be read as a measure and not a rule, and the words beside it.
 * With the row's ground and the "now" line, a page's only accent: it means now, nothing else.
 */
@Composable
private fun UnderWayProgress(event: EventInstance, text: AgendaText, now: Instant) {
    val total = Duration.between(event.begin, event.end).toMillis().coerceAtLeast(1)
    val done = Duration.between(event.begin, now).toMillis().coerceIn(0, total)
    val fraction by animateFloatAsState(
        done.toFloat() / total,
        animationSpec = TempoMotion.spatial(reducedMotion()),
        label = "under-way",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = TRACK_ALPHA),
            drawStopIndicator = {},
            modifier = Modifier.weight(1f).height(6.dp),
        )
        Text(
            text = stringResource(R.string.agenda_left, text.duration(Duration.between(now, event.end))),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The accent as the track of the event under way's bar: there, and far quieter than what is done. */
private const val TRACK_ALPHA = 0.2f

/** The row's sentence for TalkBack. */
@Composable
private fun describe(
    entry: TimedEntry,
    date: LocalDate,
    calendar: CalendarInfo?,
    text: AgendaText,
    now: Instant,
): String {
    val event = entry.event
    val range = if (event.begin == event.end) {
        text.time(event.begin)
    } else {
        stringResource(R.string.a11y_range, text.time(event.begin), text.time(event.end))
    }
    return listOfNotNull(
        range,
        text.ownZone(event),
        text.title(event),
        event.location,
        calendar?.let { stringResource(R.string.a11y_in_calendar, it.name) },
        startedNote(entry, date, text),
        endsNote(entry, date, text),
        when (entry.state) {
            EntryState.UNDER_WAY ->
                stringResource(R.string.a11y_under_way, text.duration(Duration.between(now, event.end)))

            EntryState.PAST -> stringResource(R.string.a11y_over)

            EntryState.UPCOMING -> null
        },
        stringResource(R.string.a11y_declined).takeIf { event.selfStatus == AttendeeStatus.DECLINED },
        stringResource(R.string.a11y_maybe).takeIf { event.selfStatus == AttendeeStatus.TENTATIVE },
        stringResource(R.string.a11y_not_answered).takeIf { event.awaitsAnswer },
    ).joinToString(", ")
}

/**
 * On a date the event did not start on: "Started the day before", or the day it did ("Started
 * Monday"), never "the day before" for an event three days old.
 */
@Composable
private fun startedNote(entry: TimedEntry, date: LocalDate, text: AgendaText): String? {
    if (entry.startsOnDate) return null
    val started = text.dateOf(entry.event.begin)
    return if (started == date.minusDays(1)) {
        stringResource(R.string.agenda_started_before)
    } else {
        stringResource(R.string.agenda_started_on, text.otherDay(started, date))
    }
}

/** On a date the event does not end on: "Ends the next day", or the day it does ("Ends Friday"). */
@Composable
private fun endsNote(entry: TimedEntry, date: LocalDate, text: AgendaText): String? {
    if (entry.endsOnDate) return null
    val ends = text.endDate(entry.event.end)
    return if (ends == date.plusDays(1)) {
        stringResource(R.string.agenda_ends_after)
    } else {
        stringResource(R.string.agenda_ends_on, text.otherDay(ends, date))
    }
}
