package com.callbackdev.tempo.widget.words

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.tempo.core.domain.widget.AgendaFit
import com.callbackdev.tempo.core.domain.widget.DialArc
import com.callbackdev.tempo.core.domain.widget.DialArcs
import com.callbackdev.tempo.core.domain.widget.WordsDay
import com.callbackdev.tempo.core.domain.widget.WordsFit
import com.callbackdev.tempo.core.domain.widget.WordsFocus
import com.callbackdev.tempo.core.domain.widget.WordsForm
import com.callbackdev.tempo.core.domain.widget.WordsNote
import com.callbackdev.tempo.core.domain.widget.WordsPlan
import com.callbackdev.tempo.core.domain.widget.WordsSpec
import com.callbackdev.tempo.core.domain.widget.clockLineHeight
import com.callbackdev.tempo.core.domain.widget.quarterPoint
import com.callbackdev.tempo.core.domain.widget.spThatFits
import com.callbackdev.tempo.core.domain.widget.widthOf
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.widget.CardModels
import com.callbackdev.tempo.widget.CardText
import com.callbackdev.tempo.widget.ClockFace
import com.callbackdev.tempo.widget.ClockPatterns
import com.callbackdev.tempo.widget.MESSAGE_MAX_LINES
import com.callbackdev.tempo.widget.MESSAGE_SP
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
import com.callbackdev.tempo.widget.cardMessage
import com.callbackdev.tempo.widget.clockViews
import com.callbackdev.tempo.widget.dialViews
import com.callbackdev.tempo.widget.fontScale
import com.callbackdev.tempo.widget.headerTap
import com.callbackdev.tempo.widget.measureWidgetLines
import com.callbackdev.tempo.widget.rememberWidgetModel
import com.callbackdev.tempo.widget.textEm
import com.callbackdev.tempo.widget.widgetDressFor
import com.callbackdev.tempo.widget.widgetLocale
import java.time.format.DateTimeFormatter

/**
 * «In words» (Chiaro's «In parole», Passo's «In words»; PLANNING.md §7): a dial and the day in a few
 * words. The dial stands where Passo's ring stands, the system's hands over the focus's arc
 * (`WidgetDial.kt`); beside or under it, what comes next and when, the date, the day's note.
 * [WordsFit] holds the ranks, the forms and every number's reason.
 */
class WordsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByTwo, WidgetSamples.FourByOne))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            WordsWidgetContent(rememberWidgetModel(initial, loadedAt) { models.load() }, appWidgetId)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { WordsWidgetContent(WidgetSamples.model()) }
    }
}

class WordsWidgetReceiver : TempoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordsWidget()
}

/** The whole card for [model] at [LocalSize], shared with the settings preview and the tests. */
@Composable
internal fun WordsWidgetContent(model: WidgetModel, appWidgetId: Int = 0) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val parts = WordsParts(context, model, size, appWidgetId)
    WidgetCard(
        model,
        dress,
        paddingHorizontal = parts.plan.sidePadding.dp,
        paddingVertical = if (parts.plan.snug) WidgetCardPaddingSnug else WidgetCardPadding,
    ) { palette ->
        when (parts.plan.form) {
            WordsForm.CELL -> CellContent(parts, palette)
            WordsForm.ROW -> RowContent(parts, palette)
            WordsForm.TALL -> TallContent(parts, palette)
        }
    }
}

private class WordsParts(val context: Context, val model: WidgetModel, private val size: DpSize, appWidgetId: Int) {
    val text = CardText(context, model)
    val scale = fontScale(context)
    val look = model.look
    val message = cardMessage(context, model.content)
    private val agenda = (model.content as? WidgetContent.Ready)?.agenda
    val day: WordsDay? = agenda?.let { WordsDay.of(it, look.showAllDay, look.showDaysAhead) }
    val form: WordsForm = WordsFit.form(size.width.value, size.height.value)
    private val locale = context.widgetLocale()
    val innerWidth =
        (size.width.value - (if (form == WordsForm.CELL) AgendaFit.CELL_PADDING else AgendaFit.CARD_PADDING) * 2).dp

    /** What comes next, or "Free" when nothing does. */
    val title: String = day?.let { text.focusTitle(it.focus) ?: context.getString(R.string.widget_free) } ?: ""
    private val fullNote: String? = day?.let { text.note(it.note) } ?: message?.hint

    /**
     * When, the fullest first. "Free" has no time: on a row and a cell, whose note has no line of
     * its own, the note stands in its place ("Nothing left today.").
     */
    val whens: List<String> = day?.let { day ->
        text.whenOptions(day.focus).ifEmpty { if (form == WordsForm.TALL) emptyList() else listOfNotNull(fullNote) }
    } ?: emptyList()

    /**
     * The note where the line of times follows it: the times say how many are left, so the count
     * is not said twice ("Then 3 more today." over "Then 13:00 · 15:00 · 18:30").
     */
    private val noteBeforeTimes: String? = when (day?.note) {
        is WordsNote.ThenMore -> null
        is WordsNote.FreeUntilThen -> context.getString(R.string.widget_note_free_until_bare)
        else -> fullNote
    }

    /** The date in the reader's style, then the shorter ones: a date is shortened before it is lost. */
    val datePatterns: List<ClockPatterns> = if (look.showDate) {
        DateStyle.entries.drop(text.dateStyle.ordinal).map { ClockPatterns.date(locale, it) }
    } else {
        emptyList()
    }
    private val dateTexts = datePatterns.map {
        DateTimeFormatter.ofPattern(it.current(context), locale).format(model.now)
    }

    /** A date over two lines: the wider of its two halves, at the break that makes them most even. */
    private fun twoLineEm(date: String): Float {
        val words = date.split(' ')
        if (words.size < 2) return textEm(context, date, TextWeight.REGULAR)
        return (1 until words.size).minOf { cut ->
            maxOf(
                textEm(context, words.take(cut).joinToString(" "), TextWeight.REGULAR),
                textEm(context, words.drop(cut).joinToString(" "), TextWeight.REGULAR),
            )
        }
    }

    private fun planFor(note: String?, then: Boolean) = WordsFit.plan(
        WordsSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            dial = look.showClock,
            dateEms = dateTexts.map { textEm(context, it, TextWeight.REGULAR) },
            dateTwoLineEms = dateTexts.map { twoLineEm(it) },
            titleEm = textEm(context, title, TextWeight.MEDIUM),
            titleLines = measureWidgetLines(context, title, WordsFit.TALL_TITLE_SP, innerWidth, TextWeight.MEDIUM),
            whenEms = whens.map { textEm(context, it, TextWeight.REGULAR) },
            noteLines =
            note?.let { measureWidgetLines(context, it, WordsFit.NOTE_SP, innerWidth, TextWeight.REGULAR) } ?: 0,
            then = then,
        ),
    )

    private val hasTimes = form == WordsForm.TALL && day?.then?.isNotEmpty() == true
    private val withTimes = if (hasTimes) planFor(noteBeforeTimes, then = true).takeIf { it.then } else null

    /** The tall card with its line of times where it holds it; else the whole note, without them. */
    val plan: WordsPlan = withTimes ?: planFor(fullNote, then = false)
    val note: String? = if (withTimes != null) noteBeforeTimes else fullNote
    val whenText: String? = whens.getOrNull(plan.whenChoice)

    /** "Then 16:30 · 18:00": as many times as the line holds whole. */
    val then: String? = day?.then?.takeIf { plan.then && it.isNotEmpty() }?.let { events ->
        (events.size downTo 1).map { text.then(events, it) }
            .firstOrNull {
                widthOf(textEm(context, it, TextWeight.REGULAR), WordsFit.NOTE_SP, scale) <= innerWidth.value
            }
    }

    /** The day's arcs on the dial: the focus and what else is still to come in its twelve hours. */
    val arcs: List<DialArc> = if (agenda != null && day != null) {
        DialArcs.of(agenda, day.focus, model.now.zone, look.showDaysAhead)
    } else {
        emptyList()
    }

    val headerIntent =
        headerTap(
            context,
            appWidgetId,
            WidgetIntents.header(context, look.headerTap, model.doors, model.now.toInstant()),
        )

    /** A touch on the focus opens it as the reader chose; on "Free" or a message, Tempo. */
    val focusIntent = when (val focus = day?.focus) {
        is WordsFocus.UnderWay -> WidgetIntents.event(context, look.eventTap, model.doors, focus.event)
        is WordsFocus.Next -> WidgetIntents.event(context, look.eventTap, model.doors, focus.event)
        else -> WidgetIntents.tempo(context)
    }

    /** What TalkBack reads for the focus: its title and when, the fullest way. */
    val focusDescription: String = listOfNotNull(title, whens.firstOrNull()).joinToString(", ")

    /** What a message has of a tall card: its height, less the insets and the dial or the date over it. */
    val messageRoom: Dp
        get() = (
            size.height.value - AgendaFit.CARD_PADDING * 2 - when {
                plan.dial > 0f -> plan.dial + WordsFit.DIAL_BOTTOM_GAP
                plan.dateChoice >= 0 -> clockLineHeight(plan.dateSp, scale) * plan.dateLines + WordsFit.TOP_GAP
                else -> 0f
            }
            ).dp
}

// --- The ranks --------------------------------------------------------------------------------

@Composable
private fun Dial(parts: WordsParts, palette: WidgetPalette) {
    val side = parts.plan.dial
    if (side <= 0f) return
    AndroidRemoteViews(
        remoteViews = dialViews(
            parts.context,
            side,
            parts.arcs,
            palette,
            parts.headerIntent,
            parts.context.getString(R.string.widget_dial_desc),
            parts.model.now.toLocalTime().takeIf { parts.model.frozenClock },
        ),
        modifier = GlanceModifier.size(side.dp),
    )
}

/** The date, a `TextClock` so it turns at midnight; at the far edge of a row, aligned to it. */
@Composable
private fun DateLine(parts: WordsParts, palette: WidgetPalette, end: Boolean) {
    val plan = parts.plan
    if (plan.dateChoice < 0) return
    val views = clockViews(
        parts.context,
        ClockFace.REGULAR,
        parts.datePatterns[plan.dateChoice],
        plan.dateSp,
        palette.secondary,
        plan.dateLines,
        parts.headerIntent,
        parts.model.now.takeIf { parts.model.frozenClock },
    )
    if (end) {
        views.setViewLayoutWidth(R.id.widget_clock, MATCH_PARENT.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        views.setInt(R.id.widget_clock, "setGravity", Gravity.END or Gravity.TOP)
    }
    AndroidRemoteViews(
        remoteViews = views,
        modifier = GlanceModifier
            .width(plan.dateWidth.dp)
            .height((clockLineHeight(plan.dateSp, parts.scale) * plan.dateLines).dp),
    )
}

@Composable
private fun Title(parts: WordsParts, palette: WidgetPalette, center: Boolean = false) {
    if (parts.plan.titleLines <= 0 || parts.title.isEmpty()) return
    Text(
        text = parts.title,
        style = TextStyle(
            color = palette.primaryInk,
            fontSize = parts.plan.titleSp.sp,
            fontWeight = FontWeight.Medium,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
        ),
        maxLines = parts.plan.titleLines,
    )
}

@Composable
private fun When(parts: WordsParts, palette: WidgetPalette, center: Boolean = false) {
    val time = parts.whenText ?: return
    val bold = parts.plan.whenBold
    Text(
        text = time,
        style = TextStyle(
            color = if (bold) palette.primaryInk else palette.secondaryInk,
            fontSize = parts.plan.whenSp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
        ),
        maxLines = 1,
    )
}

@Composable
private fun Note(parts: WordsParts, palette: WidgetPalette) {
    val note = parts.note ?: return
    if (parts.plan.noteLines <= 0) return
    Text(
        text = note,
        style = TextStyle(color = palette.secondaryInk, fontSize = WordsFit.NOTE_SP.sp, fontWeight = FontWeight.Normal),
        maxLines = parts.plan.noteLines,
        modifier = GlanceModifier.padding(top = WordsFit.NOTE_GAP.dp),
    )
}

/** The focus as one door: the title and when, one sentence to a screen reader. */
private fun focusModifier(parts: WordsParts): GlanceModifier = GlanceModifier
    .then(parts.focusIntent?.let { GlanceModifier.clickable(actionStartActivity(it)) } ?: GlanceModifier)
    .semantics { contentDescription = parts.focusDescription }

// --- The forms ---------------------------------------------------------------------------------

/**
 * One cell: the dial, and the focus's time under it where the cell is tall; without the dial, the
 * time large in its place. A message's one word where there is no day.
 */
@Composable
private fun CellContent(parts: WordsParts, palette: WidgetPalette) {
    val message = parts.message
    if (message != null) {
        // Never broken inside a word: the size its longest word fits whole ("No / permission").
        val widest = message.short.split(' ').maxOf { textEm(parts.context, it, TextWeight.BOLD) }
        val size = quarterPoint(minOf(MESSAGE_SP, spThatFits(parts.innerWidth.value, widest, parts.scale)))
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message.short,
                style = TextStyle(color = palette.attentionInk, fontSize = size.sp, fontWeight = FontWeight.Bold),
                maxLines = MESSAGE_MAX_LINES,
            )
        }
        return
    }
    val dial = parts.plan.dial > 0f
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (dial) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Dial(parts, palette)
        if (parts.plan.whenChoice >= 0 || (!dial && parts.plan.titleLines > 0)) {
            Column(
                modifier = GlanceModifier.then(focusModifier(parts))
                    .padding(top = if (dial) WordsFit.CELL_GAP.dp else 0.dp),
                horizontalAlignment = if (dial) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                When(parts, palette, center = dial)
                Title(parts, palette, center = dial)
            }
        }
    }
}

/** One row, Passo's: the dial; the title over its time; the date at the far edge. */
@Composable
private fun RowContent(parts: WordsParts, palette: WidgetPalette) {
    val dial = parts.plan.dial > 0f
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Dial(parts, palette)
        val words = parts.innerWidth - (if (dial) (parts.plan.dial + WordsFit.DIAL_GAP).dp else 0.dp)
        Column(
            modifier = GlanceModifier
                .padding(start = if (dial) WordsFit.DIAL_GAP.dp else 0.dp)
                .defaultWeight()
                .then(if (parts.message == null) focusModifier(parts) else GlanceModifier),
        ) {
            val message = parts.message
            if (message != null) {
                MessageContent(
                    message,
                    palette,
                    words,
                    (
                        LocalSize.current.height.value -
                            AgendaFit.CARD_PADDING_SNUG * 2
                        ).dp,
                )
            } else {
                Title(parts, palette)
                When(parts, palette)
            }
        }
        if (parts.message == null && parts.plan.dateChoice >= 0) {
            Column(
                modifier = GlanceModifier.padding(start = WordsFit.DATE_GAP.dp),
                horizontalAlignment = Alignment.End,
            ) {
                DateLine(parts, palette, end = true)
            }
        }
    }
}

/**
 * Two rows and up, Passo's tall card: the dial in the top trailing corner and the date beside it;
 * the title, when and the note hanging from the bottom leading corner.
 */
@Composable
private fun TallContent(parts: WordsParts, palette: WidgetPalette) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Dial(parts, palette)
        Column(modifier = GlanceModifier.fillMaxSize()) {
            DateLine(parts, palette, end = false)
            Spacer(modifier = GlanceModifier.defaultWeight())
            val message = parts.message
            if (message != null) {
                MessageContent(message, palette, parts.innerWidth, parts.messageRoom)
                return@Column
            }
            Column(modifier = GlanceModifier.fillMaxWidth().then(focusModifier(parts))) {
                Title(parts, palette)
                When(parts, palette)
            }
            Note(parts, palette)
            parts.then?.let { then ->
                Text(
                    text = then,
                    style = TextStyle(
                        color = palette.secondaryInk,
                        fontSize = WordsFit.NOTE_SP.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
