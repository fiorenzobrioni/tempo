package com.callbackdev.tempo.widget.words

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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.tempo.core.domain.widget.AgendaFit
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
import com.callbackdev.tempo.widget.fontScale
import com.callbackdev.tempo.widget.headerTap
import com.callbackdev.tempo.widget.measureWidgetLines
import com.callbackdev.tempo.widget.rememberWidgetModel
import com.callbackdev.tempo.widget.textEm
import com.callbackdev.tempo.widget.widgetDressFor
import com.callbackdev.tempo.widget.widgetLocale
import java.time.format.DateTimeFormatter

/**
 * «In words» (Chiaro's «In parole», Passo's «In words»; PLANNING.md §7): the day in type alone.
 * What comes next is the hero, its time and its title; the note under it says where the day
 * stands; the time and the date ride on top as a small `TextClock` line. [WordsFit] holds the ranks,
 * the forms and every number's reason.
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
    val snug = parts.plan.snug
    WidgetCard(
        model,
        dress,
        paddingHorizontal = parts.plan.sidePadding.dp,
        paddingVertical = if (snug) WidgetCardPaddingSnug else WidgetCardPadding,
    ) { palette ->
        when (parts.plan.form) {
            WordsForm.NEXT -> NextContent(parts, palette)
            WordsForm.LINE -> LineContent(parts, palette)
            WordsForm.STACK, WordsForm.PANEL -> TallContent(parts, palette)
        }
    }
}

private class WordsParts(val context: Context, val model: WidgetModel, private val size: DpSize, appWidgetId: Int) {
    val text = CardText(context, model)
    val scale = fontScale(context)
    val look = model.look
    val message = cardMessage(context, model.content)
    val day: WordsDay? = (model.content as? WidgetContent.Ready)?.let {
        WordsDay.of(it.agenda, look.showAllDay, look.showDaysAhead)
    }
    val form: WordsForm = WordsFit.form(size.width.value, size.height.value)
    private val cell = form == WordsForm.NEXT
    private val locale = context.widgetLocale()
    val innerWidth = (size.width.value - (if (cell) AgendaFit.CELL_PADDING else AgendaFit.CARD_PADDING) * 2).dp

    val hero: String = when {
        day != null -> if (cell) text.cellHero(day.focus) else text.hero(day.focus)
        message != null -> message.short
        else -> ""
    }
    val label: String? = day?.let { text.label(it.focus, cell) }
    val title: String? = day?.let { text.focusTitle(it.focus) }
    private val fullNote: String? = day?.let { text.note(it.note) } ?: message?.hint

    /**
     * The note where the panel's line of times follows it: the times say how many are left, so the
     * count is not said twice ("Then 3 more today." over "Then 13:00 · 15:00 · 18:30").
     */
    private val noteBeforeTimes: String? = when (day?.note) {
        is WordsNote.ThenMore -> null
        is WordsNote.FreeUntilThen -> context.getString(R.string.widget_note_free_until_bare)
        else -> fullNote
    }

    /**
     * The small line on top, in the order it is tried: the time and the reader's date, the time and
     * a shorter date, the time alone; the date alone where the reader hid the time.
     */
    private val dates = DateStyle.entries.drop(text.dateStyle.ordinal)

    val topPatterns: List<ClockPatterns> = run {
        val time = ClockPatterns.time(locale, look.clockFormat ?: model.settings.clockFormat)
        val datePatterns = dates.map { ClockPatterns.date(locale, it).twentyFour }
        when {
            look.showClock && look.showDate -> datePatterns.map { time.withDate(it) } + time
            look.showClock -> listOf(time)
            look.showDate -> datePatterns.map { ClockPatterns(it, it) }
            else -> emptyList()
        }
    }

    /** The patterns that carry the date: every one but the time alone. */
    private val topDated = if (look.showDate) dates.size else 0
    private val topEms = topPatterns.map { patterns ->
        val pattern = patterns.current(context)
        textEm(
            context,
            DateTimeFormatter.ofPattern(pattern, locale).format(model.now.withHour(22).withMinute(58)),
            TextWeight.REGULAR,
        )
    }

    private fun planFor(note: String?, then: Boolean) = WordsFit.plan(
        WordsSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            topEms = topEms,
            topDated = topDated,
            heroEm = textEm(context, hero, TextWeight.BOLD),
            label = label != null,
            title = title != null,
            titleLines =
            title?.let { measureWidgetLines(context, it, WordsFit.TITLE_SP, innerWidth, TextWeight.MEDIUM) } ?: 0,
            noteLines =
            note?.let { measureWidgetLines(context, it, WordsFit.NOTE_SP, innerWidth, TextWeight.REGULAR) } ?: 0,
            then = then,
        ),
    )

    private val hasTimes = form == WordsForm.PANEL && day?.then?.isNotEmpty() == true
    private val withTimes = if (hasTimes) planFor(noteBeforeTimes, then = true).takeIf { it.then } else null

    /** The panel with its line of times where it holds it; else the whole note, without them. */
    val plan: WordsPlan = withTimes ?: planFor(fullNote, then = false)
    val note: String? = if (withTimes != null) noteBeforeTimes else fullNote

    /** "Then 16:30 · 18:00": as many times as the line holds whole. */
    val then: String? = day?.then?.takeIf { plan.then && it.isNotEmpty() }?.let { events ->
        (events.size downTo 1).map { text.then(events, it) }
            .firstOrNull {
                widthOf(textEm(context, it, TextWeight.REGULAR), WordsFit.NOTE_SP, scale) <= innerWidth.value
            }
    }

    val headerIntent =
        headerTap(
            context,
            appWidgetId,
            WidgetIntents.header(context, look.headerTap, model.doors, model.now.toInstant()),
        )

    /** The title's lift beside the time in an inline row, so the two share a baseline. */
    val inlineBaseline: Dp
        get() = ((plan.heroSp - plan.titleSp) * DESCENT_EM * scale).coerceAtLeast(0f).dp

    /** What a message has of the card: its height, less the insets and the small line on top. */
    val messageRoom: Dp
        get() = (
            size.height.value - (if (plan.snug) AgendaFit.CARD_PADDING_SNUG else AgendaFit.CARD_PADDING) * 2 -
                (if (plan.topChoice >= 0) clockLineHeight(plan.topSp, scale) + WordsFit.TOP_GAP else 0f)
            ).dp

    /** A touch on the focus opens it as the reader chose; on "Free" or a message, Tempo. */
    val focusIntent = when (val focus = day?.focus) {
        is WordsFocus.UnderWay -> WidgetIntents.event(context, look.eventTap, model.doors, focus.event)
        is WordsFocus.Next -> WidgetIntents.event(context, look.eventTap, model.doors, focus.event)
        else -> WidgetIntents.tempo(context)
    }

    /** What TalkBack reads for the focus: its time, its title, and the label that dates it. */
    val focusDescription: String = listOfNotNull(
        label,
        if (cell) text.hero(day?.focus ?: WordsFocus.Free) else hero,
        title,
    )
        .joinToString(", ")
}

// --- The ranks --------------------------------------------------------------------------------

@Composable
private fun TopLine(parts: WordsParts, palette: WidgetPalette) {
    val choice = parts.plan.topChoice
    if (choice < 0) return
    AndroidRemoteViews(
        remoteViews = clockViews(
            parts.context,
            ClockFace.REGULAR,
            parts.topPatterns[choice],
            parts.plan.topSp,
            palette.secondary,
            1,
            parts.headerIntent,
            parts.model.now.takeIf { parts.model.frozenClock },
        ),
        modifier = GlanceModifier.height(clockLineHeight(parts.plan.topSp, parts.scale).dp),
    )
}

/** The label over the focus, a size smaller where the card is narrower than the word ("Tomorrow" on one cell). */
@Composable
private fun Label(parts: WordsParts, palette: WidgetPalette) {
    val label = parts.label ?: return
    if (!parts.plan.label) return
    val fits = spThatFits(parts.innerWidth.value, textEm(parts.context, label, TextWeight.MEDIUM), parts.scale)
    Text(
        text = label,
        style = TextStyle(
            color = palette.secondaryInk,
            fontSize = quarterPoint(minOf(WordsFit.LABEL_SP, fits)).sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )
}

@Composable
private fun Hero(parts: WordsParts, palette: WidgetPalette) {
    Text(
        text = parts.hero,
        style = TextStyle(
            color = if (parts.message != null) palette.attentionInk else palette.primaryInk,
            fontSize = parts.plan.heroSp.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
    )
}

@Composable
private fun Title(parts: WordsParts, palette: WidgetPalette, modifier: GlanceModifier = GlanceModifier) {
    val title = parts.title ?: return
    if (parts.plan.titleLines <= 0) return
    Text(
        text = title,
        style = TextStyle(color = palette.primaryInk, fontSize = parts.plan.titleSp.sp, fontWeight = FontWeight.Medium),
        maxLines = parts.plan.titleLines,
        modifier = modifier,
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

/** The focus as one door: the label, the time and the title, one sentence to a screen reader. */
private fun focusModifier(parts: WordsParts): GlanceModifier = GlanceModifier
    .then(parts.focusIntent?.let { GlanceModifier.clickable(actionStartActivity(it)) } ?: GlanceModifier)
    .semantics { contentDescription = parts.focusDescription }

// --- The forms ---------------------------------------------------------------------------------

/** One cell: the label, the time, the title where it fits; a message's one word where there is no day. */
@Composable
private fun NextContent(parts: WordsParts, palette: WidgetPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize().then(focusModifier(parts)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(parts, palette)
        Hero(parts, palette)
        Title(parts, palette)
    }
}

/** One row: the small line on top; the time and the title on one line, or the title under the time. */
@Composable
private fun LineContent(parts: WordsParts, palette: WidgetPalette) {
    if (parts.message != null) {
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            TopLine(parts, palette)
            MessageContent(parts.message, palette, parts.innerWidth, parts.messageRoom)
        }
        return
    }
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        TopLine(parts, palette)
        Column(modifier = GlanceModifier.fillMaxWidth().then(focusModifier(parts))) {
            Label(parts, palette)
            if (parts.plan.titleInline) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Hero(parts, palette)
                    Title(
                        parts,
                        palette,
                        GlanceModifier.padding(
                            start = WordsFit.INLINE_GAP.dp,
                            bottom = parts.inlineBaseline,
                        ).defaultWeight(),
                    )
                }
            } else {
                Hero(parts, palette)
                Title(parts, palette)
            }
        }
    }
}

/** Two rows and up: the small line on top, the air, then the focus and the note on the bottom edge. */
@Composable
private fun TallContent(parts: WordsParts, palette: WidgetPalette) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        TopLine(parts, palette)
        Spacer(modifier = GlanceModifier.defaultWeight())
        if (parts.message != null) {
            MessageContent(parts.message, palette, parts.innerWidth, parts.messageRoom)
            return@Column
        }
        Column(modifier = GlanceModifier.fillMaxWidth().then(focusModifier(parts))) {
            Label(parts, palette)
            Hero(parts, palette)
            Title(parts, palette)
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

/**
 * Roboto's line box under the baseline (555 of 2048 units): a line keeps the face's descent under
 * its baseline in proportion to its size, so the large time stands higher than a smaller title
 * bottom-aligned with it (Chiaro's `textPanelBaselineLift`, Passo's `baselineLift`).
 */
private const val DESCENT_EM = 0.271f
