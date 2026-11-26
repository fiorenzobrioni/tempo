package com.callbackdev.tempo.core.designsystem.components

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.designsystem.theme.tabular
import com.callbackdev.tempo.core.domain.calendar.blend
import com.callbackdev.tempo.core.domain.calendar.markAlpha
import com.callbackdev.tempo.core.domain.calendar.markColor
import com.callbackdev.tempo.core.domain.month.DayBar
import com.callbackdev.tempo.core.domain.month.DayMarks
import com.callbackdev.tempo.core.domain.month.MonthGrid
import com.callbackdev.tempo.core.domain.month.MonthMarks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * A month as every page that shows one draws it (PLANNING.md §11, step 10.4): the week's days, and
 * the month's weeks of dates, each with its marks. What a page does with it, turning the months
 * under the finger, listing a chosen day, is the page's own.
 */

/** The week's days over the grid's columns, in the reader's order: quiet, and not read aloud (each date says its own). */
@Composable
fun MonthWeekdays(firstDay: DayOfWeek, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    Row(modifier.fillMaxWidth().padding(horizontal = MonthGridEdge).clearAndSetSemantics { }) {
        MonthGrid.weekdays(firstDay).forEach { day ->
            Text(
                text = DateTimeText.weekdayNarrow(day, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
        }
    }
}

/**
 * What the grid needs of the page: the dates' marks, which date is today and which is chosen,
 * and what a touch does.
 *
 * @property marks null where the calendar gives none (no permission, no calendar): the grid is
 *   drawn all the same, a month is a month.
 * @property chooseLabel what a touch on a date does on this page, as a screen reader says it.
 * @property onNew a new event on a date, from a long touch on it; null where the page has none,
 *   or no app takes it.
 */
class MonthDays(
    val marks: Map<LocalDate, DayMarks>?,
    val today: LocalDate,
    val selected: LocalDate,
    val onChoose: (LocalDate) -> Unit,
    val chooseLabel: String,
    val onNew: ((LocalDate) -> Unit)?,
)

/**
 * A month's weeks, a row each, [MonthGrid.weeks]: its own dates and, quieter, the dates of the
 * months beside that complete its first and last week. As wide as it is given; the page keeps it
 * [MonthGridEdge] from its sides.
 *
 * @param cell a date's height: what its number and its marks take ([monthCellHeight]), or more
 *   where the page has the room to give (a large screen's pane).
 * @param labels what a screen reader says of a date; a page that draws several months at a time
 *   makes them once for all.
 */
@Composable
fun MonthWeeks(
    grid: MonthGrid,
    days: MonthDays,
    modifier: Modifier = Modifier,
    cell: Dp = monthCellHeight(),
    labels: MonthDayLabels = rememberMonthDayLabels(),
) {
    Column(modifier.fillMaxWidth().testTag(DENSE_TARGETS_TAG)) {
        grid.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().height(cell)) {
                week.forEach { date ->
                    val inMonth = date in grid
                    DayCell(
                        date = date,
                        look = DayLook(
                            inMonth = inMonth,
                            today = date == days.today,
                            selected = date == days.selected,
                            faded = !inMonth || date < days.today,
                        ),
                        marks = days.marks?.get(date),
                        labels = labels,
                        onClick = { days.onChoose(date) },
                        onClickLabel = days.chooseLabel,
                        onLongClick = days.onNew?.let { new -> { new(date) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(if (inMonth) MonthCellTags.day(date) else MonthCellTags.beside(date)),
                    )
                }
            }
        }
    }
}

/**
 * How a date stands in its cell: whether it is of the month on its page, today, the chosen one, and
 * whether its marks are drawn quieter (a date already gone, or of the month beside).
 */
private data class DayLook(val inMonth: Boolean, val today: Boolean, val selected: Boolean, val faded: Boolean)

/**
 * One date: its number, and under it what it holds, as marks (`MonthMarks`): a dot an event with a
 * time, a bar a span of days, running on into the cell beside. Today's number stands on the accent,
 * which here too means now and nothing else; the chosen date's on a quiet disc, and a ring round
 * today's when today is the chosen one.
 *
 * A touch chooses the date, and what that does is the page's ([onClickLabel]); a long touch, where
 * the page has one, starts a new event on it. Read by a screen reader as its date and what it holds
 * ("Thursday 26 November, 3 events"), since the marks say nothing aloud.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    look: DayLook,
    marks: DayMarks?,
    labels: MonthDayLabels,
    onClick: () -> Unit,
    onClickLabel: String,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val chosen by animateFloatAsState(
        targetValue = if (look.selected) 1f else 0f,
        animationSpec = TempoMotion.spatialFast(reducedMotion()),
        label = "chosen",
    )
    val disc = dayDisc()
    val surface = scheme.surface.toArgb()
    val quiet = scheme.onSurfaceVariant
    val inks = remember(marks, surface, quiet, look.faded) { MarkInks.of(marks, surface, quiet, look.faded) }
    val haptics = LocalHapticFeedback.current
    val number = when {
        look.today -> scheme.onPrimary
        look.selected -> scheme.onSecondaryContainer
        look.inMonth -> scheme.onSurface
        else -> quiet
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .drawBehind {
                val center = Offset(size.width / 2f, (CELL_TOP + disc / 2).toPx())
                val radius = disc.toPx() / 2f
                if (look.today) {
                    drawCircle(scheme.primary, radius, center)
                    if (chosen > 0f) {
                        drawCircle(
                            color = scheme.primary,
                            radius = radius + RING_GAP.toPx(),
                            center = center,
                            alpha = chosen,
                            style = Stroke(RING.toPx()),
                        )
                    }
                } else if (chosen > 0f) {
                    drawCircle(scheme.secondaryContainer, radius * (SHRUNK + (1 - SHRUNK) * chosen), center, chosen)
                }
                if (inks != null) drawMarks(inks, top = CELL_TOP + disc + MARKS_GAP, quiet = quiet)
            }
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClickLabel = onClickLabel,
                role = Role.Button,
                onLongClickLabel = onLongClick?.let { stringResource(R.string.agenda_new_event) },
                onLongClick = onLongClick?.let { new ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        new()
                    }
                },
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = labels.of(date, look.today, marks)
                selected = look.selected
            },
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge.tabular(),
            fontWeight = if (look.today) {
                FontWeight.SemiBold
            } else if (look.inMonth) {
                FontWeight.Medium
            } else {
                null
            },
            color = number,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = CELL_TOP).height(disc).wrapContentHeight(Alignment.CenterVertically),
        )
    }
}

/**
 * A date's marks as inks: each a calendar's colour stepped to 3:1 on the page ([markColor]), and,
 * where the date is gone or of the month beside, as quiet as that ratio allows ([markAlpha]). A
 * past event looks past here too.
 */
private class MarkInks(val dots: List<Color>, val more: Boolean, val lanes: List<Pair<DayBar, Color>?>) {
    companion object {
        fun of(marks: DayMarks?, surface: Int, quiet: Color, faded: Boolean): MarkInks? {
            if (marks == null || marks.isEmpty) return null
            // A quiet ink is mixed with the page's, not drawn through: a bar's round end over its
            // own line would show twice as strong.
            fun ink(argb: Int?): Color {
                if (argb == null) return if (faded) Color(blend(quiet.toArgb(), surface, QUIET_ALPHA)) else quiet
                val mark = markColor(argb, surface)
                return Color(if (faded) blend(mark, surface, markAlpha(mark, surface, QUIET_ALPHA)) else mark)
            }
            return MarkInks(
                dots = marks.dots.map(::ink),
                more = marks.more,
                lanes = marks.lanes.map { bar -> bar?.let { it to ink(it.color) } },
            )
        }
    }
}

/**
 * The marks, from [top] down: the dots in a row under the number, centred, with a small plus after
 * them where the date holds more than they show; then the bars, a lane each, to the cell's edges
 * where the span goes on and rounded short of them where it starts or ends.
 */
private fun DrawScope.drawMarks(inks: MarkInks, top: Dp, quiet: Color) {
    val dot = DOT.toPx()
    val gap = DOT_GAP.toPx()
    val plus = if (inks.more) PLUS.toPx() else 0f
    val count = inks.dots.size
    val width = count * dot + (count - 1).coerceAtLeast(0) * gap + (if (inks.more && count > 0) gap else 0f) + plus
    val y = top.toPx() + dot / 2f
    var x = (size.width - width) / 2f
    inks.dots.forEach { color ->
        drawCircle(color, dot / 2f, Offset(x + dot / 2f, y))
        x += dot + gap
    }
    if (inks.more) {
        val stroke = PLUS_STROKE.toPx()
        val middle = x + plus / 2f
        drawLine(quiet, Offset(x, y), Offset(x + plus, y), stroke, StrokeCap.Round)
        drawLine(quiet, Offset(middle, y - plus / 2f), Offset(middle, y + plus / 2f), stroke, StrokeCap.Round)
    }
    val bar = BAR.toPx()
    val inset = BAR_INSET.toPx()
    inks.lanes.forEachIndexed { lane, entry ->
        val (span, color) = entry ?: return@forEachIndexed
        val line = (top + DOT + MARKS_GAP + (BAR + BAR_GAP) * lane).toPx() + bar / 2f
        // The week runs the other way in a right-to-left language, and a span with it.
        val rtl = layoutDirection == LayoutDirection.Rtl
        val startsAtLeft = if (rtl) span.endsHere else span.startsHere
        val endsAtRight = if (rtl) span.startsHere else span.endsHere
        val from = if (startsAtLeft) inset + bar / 2f else 0f
        val to = if (endsAtRight) size.width - inset - bar / 2f else size.width
        // Round where the span stops; square, and to the edge, where it goes on next door.
        drawLine(color, Offset(from, line), Offset(to, line), bar, StrokeCap.Butt)
        if (startsAtLeft) drawCircle(color, bar / 2f, Offset(from, line))
        if (endsAtRight) drawCircle(color, bar / 2f, Offset(to, line))
    }
}

/**
 * What a screen reader says of a date: its name in full, that it is today, and how much it holds.
 * Made once for a grid and asked only when the semantics are read: a hundred and more cells are
 * composed at a time, and a date's name costs a format.
 */
class MonthDayLabels internal constructor(private val resources: Resources, private val locale: Locale) {
    private val names = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)

    fun of(date: LocalDate, today: Boolean, marks: DayMarks?): String {
        val name = names.format(date).replaceFirstChar { it.titlecase(locale) }
        val named = if (today) resources.getString(R.string.month_day_today, name) else name
        return when {
            marks == null -> named
            marks.isEmpty -> resources.getString(R.string.month_day_nothing, named)
            else -> resources.getQuantityString(R.plurals.month_day_events, marks.events, named, marks.events)
        }
    }
}

@Composable
fun rememberMonthDayLabels(): MonthDayLabels {
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    return remember(resources, locale) { MonthDayLabels(resources, locale) }
}

/**
 * The disc a date's number stands on: a finger's glance at the usual text size, and grown with the
 * reader's so two figures at twice the size still stand inside it.
 */
@Composable
private fun dayDisc(): Dp {
    val figures = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toDp() }
    return maxOf(DISC, figures * DISC_PER_FIGURE)
}

/** A cell's height: the disc, the dots under it, the bars' lanes, and the air between them. */
@Composable
fun monthCellHeight(): Dp =
    CELL_TOP + dayDisc() + MARKS_GAP + DOT + MARKS_GAP + BAR * MonthMarks.LANES + BAR_GAP * (MonthMarks.LANES - 1) +
        CELL_BOTTOM

/** What the grid keeps from the page's sides: its first column's numbers stand near the page's edge. */
val MonthGridEdge = 8.dp

/** Hooks for the UI tests: a date's cell, wherever a month is drawn. */
object MonthCellTags {
    /** A date in its own month's grid. */
    fun day(date: LocalDate) = "month_day_$date"

    /** A date drawn in the grid of the month beside its own. */
    fun beside(date: LocalDate) = "month_beside_$date"
}

private val CELL_TOP = 3.dp
private val CELL_BOTTOM = 4.dp
private val DISC = 32.dp
private const val DISC_PER_FIGURE = 1.55f
private val RING = 1.5.dp
private val RING_GAP = 3.dp

/** The chosen date's disc grows to its size from a little under it. */
private const val SHRUNK = 0.8f
private val MARKS_GAP = 3.dp
private val DOT = 5.dp
private val DOT_GAP = 3.dp
private val PLUS = 5.dp
private val PLUS_STROKE = 1.2.dp
private val BAR = 3.dp
private val BAR_GAP = 2.dp

/** How far a bar's rounded end keeps from the cell's edge, so two spans end to end read as two. */
private val BAR_INSET = 5.dp

/** How quiet the marks of a date gone, or of the month beside, are drawn, where 3:1 allows it. */
private const val QUIET_ALPHA = 0.45f
