package com.callbackdev.tempo.widget.agenda

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.tempo.core.domain.widget.AgendaFit
import com.callbackdev.tempo.core.domain.widget.AgendaForm
import com.callbackdev.tempo.core.domain.widget.AgendaPlan
import com.callbackdev.tempo.core.domain.widget.AgendaSpec
import com.callbackdev.tempo.core.domain.widget.CardAgenda
import com.callbackdev.tempo.core.domain.widget.CardFit
import com.callbackdev.tempo.core.domain.widget.CardFooter
import com.callbackdev.tempo.core.domain.widget.CardLine
import com.callbackdev.tempo.core.domain.widget.HeaderPlan
import com.callbackdev.tempo.core.domain.widget.RowStyle
import com.callbackdev.tempo.core.domain.widget.clockLineHeight
import com.callbackdev.tempo.core.domain.widget.fitLines
import com.callbackdev.tempo.core.domain.widget.lineHeight
import com.callbackdev.tempo.core.domain.widget.widthOf
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.widget.CardMessage
import com.callbackdev.tempo.widget.CardModels
import com.callbackdev.tempo.widget.CardText
import com.callbackdev.tempo.widget.ClockFace
import com.callbackdev.tempo.widget.ClockPatterns
import com.callbackdev.tempo.widget.MessageContent
import com.callbackdev.tempo.widget.R
import com.callbackdev.tempo.widget.TempoWidgetReceiver
import com.callbackdev.tempo.widget.TextWeight
import com.callbackdev.tempo.widget.WidgetCard
import com.callbackdev.tempo.widget.WidgetCardPadding
import com.callbackdev.tempo.widget.WidgetCardPaddingSnug
import com.callbackdev.tempo.widget.WidgetContent
import com.callbackdev.tempo.widget.WidgetIntents
import com.callbackdev.tempo.widget.WidgetModel
import com.callbackdev.tempo.widget.WidgetPalette
import com.callbackdev.tempo.widget.WidgetRefresh
import com.callbackdev.tempo.widget.WidgetSamples
import com.callbackdev.tempo.widget.balancedWidth
import com.callbackdev.tempo.widget.cardMessage
import com.callbackdev.tempo.widget.clockEm
import com.callbackdev.tempo.widget.clockViews
import com.callbackdev.tempo.widget.fontScale
import com.callbackdev.tempo.widget.headerTap
import com.callbackdev.tempo.widget.measureWidgetLines
import com.callbackdev.tempo.widget.rememberWidgetModel
import com.callbackdev.tempo.widget.textEm
import com.callbackdev.tempo.widget.widgetDressFor
import com.callbackdev.tempo.widget.widgetLocale
import java.time.format.DateTimeFormatter

/**
 * «Agenda» (PLANNING.md §7, VISION.md): the concept's widget in the family's dress. The clock and
 * the date are the system's `TextClock`; under them, or beside them, the rest of the day, as many
 * events as the card holds whole, then how many more. Never a scrolling list: a Glance `Column`,
 * its rows in groups, because Glance drops a container's eleventh child without a word.
 * `AgendaFit` (`:core:domain`) holds the forms and every number's reason.
 */
class AgendaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByTwo, WidgetSamples.FourByOne))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            AgendaWidgetContent(rememberWidgetModel(initial, loadedAt) { models.load() }, appWidgetId)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { AgendaWidgetContent(WidgetSamples.model()) }
    }
}

class AgendaWidgetReceiver : TempoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

/** The whole card for [model] at [LocalSize], shared with the settings preview and the tests. */
@Composable
internal fun AgendaWidgetContent(model: WidgetModel, appWidgetId: Int = 0) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val parts = AgendaParts(context, model, size, appWidgetId)
    WidgetCard(
        model,
        dress,
        paddingHorizontal = parts.plan.sidePadding.dp,
        paddingVertical = if (parts.plan.snug) WidgetCardPaddingSnug else WidgetCardPadding,
    ) { palette ->
        when (parts.plan.form) {
            AgendaForm.CLOCK -> Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(parts, palette)
            }

            AgendaForm.LINE -> Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = GlanceModifier.width(parts.headerWidth)) { Header(parts, palette) }
                Box(modifier = GlanceModifier.padding(start = AgendaFit.COLUMN_GAP.dp).defaultWeight()) {
                    Listing(parts, palette)
                }
            }

            AgendaForm.SIDE -> Row(modifier = GlanceModifier.fillMaxSize()) {
                Box(modifier = GlanceModifier.width(parts.headerWidth)) { Header(parts, palette) }
                Box(
                    modifier = GlanceModifier.padding(start = AgendaFit.COLUMN_GAP.dp).defaultWeight().fillMaxHeight(),
                ) {
                    Listing(parts, palette)
                }
            }

            AgendaForm.AGENDA -> Column(modifier = GlanceModifier.fillMaxSize()) {
                if (parts.plan.header != null) {
                    Header(parts, palette)
                    Spacer(modifier = GlanceModifier.height(AgendaFit.HEADER_GAP.dp))
                }
                Listing(parts, palette)
            }
        }
    }
}

/** Everything a form needs, measured once: the plan, the lines, what fits, and the words. */
private class AgendaParts(val context: Context, val model: WidgetModel, size: DpSize, val appWidgetId: Int) {
    val text = CardText(context, model)
    val scale = fontScale(context)
    val look = model.look
    val message: CardMessage? = cardMessage(context, model.content)
    val ready = model.content as? WidgetContent.Ready
    val card: CardAgenda? = ready?.let { CardAgenda.of(it.agenda, look.showAllDay, look.showDaysAhead) }

    private val locale = context.widgetLocale()
    val timePatterns = ClockPatterns.time(locale, look.clockFormat ?: model.settings.clockFormat)

    /** The reader's date style first, then the shorter ones: a date is shortened before it is shrunk. */
    val dateStyles: List<DateStyle> = DateStyle.entries.drop(text.dateStyle.ordinal)
    private val dateEms = dateStyles.map { style ->
        val pattern = ClockPatterns.date(locale, style).twentyFour
        textEm(context, DateTimeFormatter.ofPattern(pattern, locale).format(model.now), ClockFace.MEDIUM.weight)
    }

    private val lines: List<CardLine> = card?.lines.orEmpty()
    private val rangeEm =
        lines.filter { it.isEvent }.maxOfOrNull { textEm(context, text.range(it), TextWeight.MEDIUM) } ?: 0f
    private val startEm =
        lines.filter { it.isEvent }.maxOfOrNull { textEm(context, text.start(it), TextWeight.MEDIUM) } ?: 0f

    val plan: AgendaPlan = AgendaFit.plan(
        AgendaSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            clock = look.showClock,
            date = look.showDate,
            clockEm = clockEm(context, timePatterns, ClockFace.BOLD, model.now),
            dateEms = dateEms,
            rangeEm = rangeEm,
            startEm = startEm,
        ),
    )

    val headerWidth: Dp get() = (plan.header?.width ?: 0f).dp

    /** A two-line date's width: balanced, so the two lines are even and no word stands alone. */
    fun dateWidth(header: HeaderPlan): Dp {
        val style = dateStyles[header.dateChoice]
        val date = DateTimeFormatter.ofPattern(ClockPatterns.date(locale, style).twentyFour, locale).format(model.now)
        return balancedWidth(context, date, header.dateSp, header.width.dp, TextWeight.MEDIUM, header.dateLines)
    }
    val listWidth: Dp get() = plan.listWidth.dp

    /** The note's lines at the list's width, two at most. */
    fun noteLines(line: CardLine.Note): Int = measureWidgetLines(
        context,
        text.note(line.note),
        AgendaFit.NOTE_SP,
        listWidth,
        TextWeight.REGULAR,
    ).coerceIn(1, AgendaFit.NOTE_MAX_LINES)

    val fit: CardFit? = card?.let { agenda ->
        fitLines(
            agenda = agenda,
            heights = { line ->
                AgendaFit.height(
                    line,
                    plan.rowStyle,
                    scale,
                    (line as? CardLine.Note)?.let(::noteLines) ?: 1,
                )
            },
            room = plan.listRoom,
            footerHeight = AgendaFit.footerHeight(scale),
        )
    }

    val timeColumn: Dp get() = AgendaFit.timeColumn(plan.rowStyle, rangeEm, startEm, scale).dp

    val headerIntent =
        headerTap(
            context,
            appWidgetId,
            WidgetIntents.header(context, look.headerTap, model.doors, model.now.toInstant()),
        )

    private fun fits(text: String, sizeSp: Float, weight: TextWeight, width: Dp): Boolean =
        widthOf(textEm(context, text, weight), sizeSp, scale) <= width.value

    /**
     * A stacked row's time line: the range where the column holds it, its start where only that
     * fits, and none on a column too narrow for either (a headerless one-cell card): the title
     * still says which event it is, and a time cut in half would say a wrong one.
     */
    fun stackedTime(line: CardLine): String? {
        val room = listWidth - (AgendaFit.MARK_WIDTH + AgendaFit.MARK_GAP).dp
        return listOf(text.range(line), text.start(line)).firstOrNull {
            fits(it, AgendaFit.TIME_SP, TextWeight.MEDIUM, room)
        }
    }

    /** The count of what did not fit, in as many words as the list's width holds: "2 more today", "2 more", "+2". */
    fun footer(footer: CardFooter): String {
        val count = when (footer) {
            is CardFooter.MoreToday -> footer.count
            is CardFooter.MoreLater -> footer.count
        }
        val short = context.resources.getQuantityString(R.plurals.widget_more_short, count, count)
        return listOf(text.footer(footer), short)
            .firstOrNull { fits(it, AgendaFit.FOOTER_SP, TextWeight.MEDIUM, listWidth) }
            ?: context.getString(R.string.widget_more_count, count)
    }
}

// --- The header --------------------------------------------------------------------------------

/** The clock over the date, both the system's `TextClock`, each where the reader keeps it. */
@Composable
private fun Header(parts: AgendaParts, palette: WidgetPalette) {
    val header: HeaderPlan = parts.plan.header ?: return
    val context = parts.context
    val frozen = parts.model.now.takeIf { parts.model.frozenClock }
    val centred = parts.plan.form == AgendaForm.CLOCK
    Column(horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
        if (header.clockSp > 0f) {
            AndroidRemoteViews(
                remoteViews = clockViews(
                    context,
                    ClockFace.BOLD,
                    parts.timePatterns,
                    header.clockSp,
                    palette.primary,
                    1,
                    parts.headerIntent,
                    frozen,
                ),
                // Its own height, measured: a container Glance sizes alone may take its column's. Its
                // own width too beside a two-line date, whose balanced width would otherwise be the
                // column's, and the clock's container takes the column's width.
                modifier = GlanceModifier
                    .height(clockLineHeight(header.clockSp, parts.scale).dp)
                    .then(if (header.dateLines > 1) GlanceModifier.width(header.width.dp) else GlanceModifier),
            )
        }
        if (header.dateChoice >= 0) {
            val patterns = ClockPatterns.date(context.widgetLocale(), parts.dateStyles[header.dateChoice])
            AndroidRemoteViews(
                remoteViews = clockViews(
                    context,
                    ClockFace.MEDIUM,
                    patterns,
                    header.dateSp,
                    if (header.clockSp > 0f) palette.secondary else palette.primary,
                    header.dateLines,
                    parts.headerIntent,
                    frozen,
                ),
                modifier = GlanceModifier
                    .height((clockLineHeight(header.dateSp, parts.scale) * header.dateLines).dp)
                    .then(if (header.dateLines > 1) GlanceModifier.width(parts.dateWidth(header)) else GlanceModifier),
            )
        }
    }
}

// --- The list ----------------------------------------------------------------------------------

/**
 * The lines that fit, in groups of [GROUP] (Glance drops the eleventh child of a container without
 * a word, so no container here holds more than ten), then the count of what did not fit; or, with
 * no day to list, the one message that stands in its place.
 */
@Composable
private fun Listing(parts: AgendaParts, palette: WidgetPalette) {
    val message = parts.message
    if (message != null) {
        MessageContent(message, palette, parts.listWidth, parts.plan.listRoom.dp)
        return
    }
    val card = parts.card ?: return
    val fit = parts.fit ?: return
    val shown = card.lines.take(minOf(fit.shown, MAX_LINES))
    val centred = parts.plan.form == AgendaForm.LINE
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = if (centred) Alignment.CenterVertically else Alignment.Top,
    ) {
        shown.chunked(GROUP).forEach { group ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                group.forEach { line -> Line(parts, palette, line) }
            }
        }
        fit.footer?.let { footer ->
            Text(
                text = parts.footer(footer),
                style = TextStyle(
                    color = palette.secondaryInk,
                    fontSize = AgendaFit.FOOTER_SP.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = AgendaFit.FOOTER_TOP.dp),
            )
        }
    }
}

@Composable
private fun Line(parts: AgendaParts, palette: WidgetPalette, line: CardLine) {
    when (line) {
        is CardLine.Heading -> Text(
            text = parts.text.heading(line.date),
            style = TextStyle(
                color = palette.secondaryInk,
                fontSize = AgendaFit.HEADING_SP.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            modifier = GlanceModifier.padding(top = AgendaFit.HEADING_TOP.dp, bottom = AgendaFit.ROW_GAP.dp),
        )

        is CardLine.Note -> Text(
            text = parts.text.note(line.note),
            style = TextStyle(
                color = palette.secondaryInk,
                fontSize = AgendaFit.NOTE_SP.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = parts.noteLines(line),
            modifier = GlanceModifier.padding(bottom = AgendaFit.ROW_GAP.dp),
        )

        is CardLine.AllDay, is CardLine.Timed -> EventRow(parts, palette, line)
    }
}

/**
 * An event: the calendar's colour as a mark (data, never a role: it never paints the words), the
 * time, the title on one line. The one under way says when it ends, in the strong ink; a touch
 * opens what the reader chose for this card, and the row reads as one sentence.
 */
@Composable
private fun EventRow(parts: AgendaParts, palette: WidgetPalette, line: CardLine) {
    val context = parts.context
    // A date's all-day events share the first one's mark; together, a touch opens Tempo's day.
    val event = when (line) {
        is CardLine.AllDay -> line.entries.first().event
        is CardLine.Timed -> line.entry.event
        else -> return
    }
    val single = line !is CardLine.AllDay || line.entries.size == 1
    val underWay = line is CardLine.Timed && line.entry.state == EntryState.UNDER_WAY
    val calendars = (parts.model.content as? WidgetContent.Ready)?.calendars.orEmpty()
    val mark = palette.mark(event.color ?: calendars[event.calendarId]?.color)
    val tap = if (single) {
        WidgetIntents.event(
            context,
            parts.look.eventTap,
            parts.model.doors,
            event,
        )
    } else {
        WidgetIntents.tempo(context)
    }
    val description = parts.text.describe(line, calendars)
    val style = parts.plan.rowStyle
    val timeText = when (style) {
        RowStyle.INLINE_START -> parts.text.start(line)
        RowStyle.INLINE_RANGE -> parts.text.range(line)
        RowStyle.STACKED -> parts.stackedTime(line)
    }
    val timeStyle = TextStyle(
        color = if (underWay) palette.primaryInk else palette.secondaryInk,
        fontSize = AgendaFit.TIME_SP.sp,
        fontWeight = if (underWay) FontWeight.Medium else FontWeight.Normal,
    )
    val titleStyle =
        TextStyle(color = palette.primaryInk, fontSize = AgendaFit.TITLE_SP.sp, fontWeight = FontWeight.Medium)
    val scale = parts.scale
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = AgendaFit.ROW_GAP.dp)
            .then(if (tap != null) GlanceModifier.clickable(actionStartActivity(tap)) else GlanceModifier)
            .semantics { contentDescription = description },
    ) {
        val markHeight = if (style == RowStyle.STACKED) {
            lineHeight(AgendaFit.TIME_SP, scale) + lineHeight(AgendaFit.TITLE_SP, scale) - MARK_INSET * 2
        } else {
            lineHeight(AgendaFit.TITLE_SP, scale) - MARK_INSET * 2
        }
        Box(
            modifier = GlanceModifier
                .width(AgendaFit.MARK_WIDTH.dp)
                .height(markHeight.dp)
                .cornerRadius(MARK_CORNER)
                .background(ColorProvider(mark)),
        ) {}
        if (style == RowStyle.STACKED) {
            Column(modifier = GlanceModifier.padding(start = AgendaFit.MARK_GAP.dp).defaultWeight()) {
                if (timeText != null) Text(text = timeText, style = timeStyle, maxLines = 1)
                Text(text = parts.text.title(line), style = titleStyle, maxLines = 1)
            }
        } else {
            Text(
                text = timeText.orEmpty(),
                style = timeStyle,
                maxLines = 1,
                modifier = GlanceModifier.padding(
                    start = AgendaFit.MARK_GAP.dp,
                ).width(parts.timeColumn + AgendaFit.MARK_GAP.dp),
            )
            Text(
                text = parts.text.title(line),
                style = titleStyle,
                maxLines = 1,
                modifier = GlanceModifier.padding(start = AgendaFit.TIME_GAP.dp).defaultWeight(),
            )
        }
    }
}

/** Rows per group: five, so a list and its footer never pass Glance's ten children. */
private const val GROUP = 5

/** A card never lists more than this, whatever its height: eight groups and the footer. */
private const val MAX_LINES = 40

private const val MARK_INSET = 2f
private val MARK_CORNER = 2.dp
