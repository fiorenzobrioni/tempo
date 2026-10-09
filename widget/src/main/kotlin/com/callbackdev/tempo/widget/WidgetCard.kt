package com.callbackdev.tempo.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import androidx.annotation.StyleRes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.tempo.core.designsystem.theme.WidgetDress
import com.callbackdev.tempo.core.designsystem.theme.widgetCardContainer
import com.callbackdev.tempo.core.designsystem.theme.widgetDress
import com.callbackdev.tempo.core.domain.calendar.markColor
import com.callbackdev.tempo.core.domain.widget.FIT_SLACK
import com.callbackdev.tempo.core.domain.widget.lineHeight
import com.callbackdev.tempo.core.domain.widget.widthOf
import com.callbackdev.tempo.core.model.UserSettings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The widgets' side of the design system: Chiaro's card, carried over through Passo, so a Tempo
 * card beside a Chiaro or a Passo one on a home screen is the same piece of furniture. The same
 * 24 dp corner, the same insets, the same grounds, the same inks; the system face, because a
 * launcher draws `RemoteViews` in it whatever the app's own setting is.
 */

/** The dress a card writes in: the reader's palette, or the wallpaper's schemes when they asked. */
fun widgetDressFor(context: Context, settings: UserSettings): WidgetDress = if (settings.dynamicColor) {
    widgetDress(settings.palette, dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
} else {
    widgetDress(settings.palette)
}

/**
 * The inks a card draws with, resolved once for the ground it really has.
 *
 * @property markGround what a calendar's colour is stepped against to keep 3:1 (PLANNING.md §6):
 *   the card's own ground when it is solid, the darkest or lightest a wallpaper behind a
 *   see-through card can be for the ink chosen when it is not.
 * @property accent the dial's focus, Passo's ring's progress: white on a colour, the scheme's
 *   primary on the light and dark grounds.
 * @property track the dial's ring under its arcs (Passo's `TRACK_ALPHA`).
 */
data class WidgetPalette(
    val primary: Color,
    val secondary: Color,
    val attention: Color,
    val darkGround: Boolean,
    val markGround: Color,
    val accent: Color = primary,
    val track: Color = primary.copy(alpha = TRACK_ALPHA),
) {
    val primaryInk: ColorProvider get() = ColorProvider(primary)
    val secondaryInk: ColorProvider get() = ColorProvider(secondary)
    val attentionInk: ColorProvider get() = ColorProvider(attention)

    /** A calendar's colour as a mark on this card: data, never a role, stepped to 3:1. */
    fun mark(argb: Int?): Color = Color(markColor(argb ?: secondary.toArgb(), markGround.toArgb()))
}

/** [widgetInk]'s answer, dressed in the colours it names. */
fun widgetPalette(context: Context, look: WidgetLook, dress: WidgetDress): WidgetPalette {
    val night = isNight(context)
    val delegates = look.background == WidgetBackground.SYSTEM || look.background == WidgetBackground.COLOR
    val bright = look.opacityPct < INK_TRUST_FLOOR_PCT && delegates && wallpaperWantsDarkInk(context)
    val ink = widgetInk(look.background, look.opacityPct, night, bright)
    val solid = look.opacityPct >= INK_TRUST_FLOOR_PCT
    val ground = cardGround(look, dress, night)
    return when (ink) {
        WidgetInk.OVER_COLOR -> WidgetPalette(
            primary = Color.White,
            secondary = Color.White.copy(alpha = QUIET_ALPHA),
            attention = Color.White.copy(alpha = ATTENTION_ALPHA),
            darkGround = true,
            markGround = if (solid) ground else Color.Black,
            accent = Color.White,
            track = Color.White.copy(alpha = TRACK_ALPHA),
        )

        WidgetInk.ON_LIGHT, WidgetInk.ON_DARK -> {
            val dark = ink.darkGround
            val scheme = dress.scheme(dark)
            WidgetPalette(
                primary = scheme.onSurface,
                secondary = scheme.onSurfaceVariant,
                attention = dress.colors(dark).attention,
                darkGround = dark,
                markGround = if (solid) {
                    ground
                } else if (dark) {
                    Color.Black
                } else {
                    Color.White
                },
                accent = scheme.primary,
                track = scheme.onSurface.copy(alpha = TRACK_ALPHA),
            )
        }
    }
}

/** The card's ground at full strength: what [widgetCardFill] thins by the reader's opacity. */
private fun cardGround(look: WidgetLook, dress: WidgetDress, night: Boolean): Color = when (look.background) {
    WidgetBackground.LIGHT -> dress.lightScheme.surface
    WidgetBackground.DARK -> dress.darkScheme.surface
    WidgetBackground.SYSTEM -> dress.scheme(night).surface
    WidgetBackground.COLOR -> widgetCardContainer(look.cardColor)
}

/** The card's fill at the reader's opacity: the ground thins, the ink never does. */
fun widgetCardFill(look: WidgetLook, dress: WidgetDress, night: Boolean): Color =
    cardGround(look, dress, night).copy(alpha = look.opacityPct.coerceIn(0, 100) / 100f)

/**
 * The phone's night mode at render time. Glance's day/night providers are resolved by the
 * launcher, and a host that flips the card without the words leaves dark ink on a dark card
 * (Chiaro, device report of 3 Sep 2026): every colour here is resolved against one answer.
 */
fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Whether the wallpaper behind a see-through card can carry dark text, by the system's own
 * account ([WallpaperColors.HINT_SUPPORTS_DARK_TEXT]): dark ink only when the system says the
 * ground is bright, light ink whenever it says nothing.
 */
fun wallpaperWantsDarkInk(context: Context): Boolean {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
    val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        ?: runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_LOCK) }.getOrNull()
        ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}

/**
 * The card every widget lives in: the 24 dp corner, the ground at the reader's opacity, the insets
 * for what sits against each edge, and Tempo's door for a touch anywhere the card has no other.
 */
@Composable
internal fun WidgetCard(
    model: WidgetModel,
    dress: WidgetDress,
    paddingHorizontal: Dp = WidgetCardPadding,
    paddingVertical: Dp = WidgetCardPadding,
    content: @Composable (WidgetPalette) -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(model.look, dress) { widgetPalette(context, model.look, dress) }
    val tap = WidgetIntents.tempo(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetCorner)
            .then(if (tap != null) GlanceModifier.clickable(actionStartActivity(tap)) else GlanceModifier),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(widgetCardFill(model.look, dress, isNight(context)))),
        ) {}
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        ) {
            content(palette)
        }
    }
}

/** A model and the [WidgetRefresh] revision it was read at ([STALE]: read again at once). */
internal data class LoadedModel(val model: WidgetModel, val revision: Long) {
    companion object {
        const val STALE = -1L
    }
}

/**
 * What a new session draws first. The card's last model where this process still holds it, so
 * the session reaches `provideContent` at once (a resize is drawn without waiting on the
 * calendar) and reads after it; else a read now, before anything is drawn.
 */
internal suspend fun firstModel(appWidgetId: Int, models: CardModels): LoadedModel {
    WidgetRefresh.lastModel(appWidgetId)?.let { return LoadedModel(it, LoadedModel.STALE) }
    // Read before the load, so only a change after it reloads.
    val revision = WidgetRefresh.revision.value
    return LoadedModel(models.load(), revision)
}

/**
 * The model this widget draws, re-read inside the composition whenever [WidgetRefresh] ticks.
 * [first] is what `provideGlance` started from, so the first frame costs nothing more and only a
 * real change reloads. Each model drawn is reported, with its revision ([WidgetRefresh.drawn]).
 */
@Composable
internal fun rememberWidgetModel(
    appWidgetId: Int,
    first: LoadedModel,
    reload: suspend () -> WidgetModel,
): WidgetModel {
    val revision by WidgetRefresh.revision.collectAsState()
    val loaded by produceState(first, revision) {
        if (revision != value.revision) value = LoadedModel(reload(), revision)
    }
    SideEffect { WidgetRefresh.drawn(appWidgetId, loaded.revision, loaded.model) }
    return loaded.model
}

/**
 * One message on the card, where the agenda would be: what is true, and what a touch does about it,
 * as [room] holds them whole. The sentence and its hint where both fit; the sentence alone where
 * the hint does not; the message's one word where not even the sentence does. Never cut.
 */
@Composable
internal fun MessageContent(message: CardMessage, palette: WidgetPalette, width: Dp, room: Dp) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val titleLines = measureWidgetLines(context, message.title, MESSAGE_SP, width, TextWeight.MEDIUM)
    val hintLines = measureWidgetLines(context, message.hint, HINT_SP, width, TextWeight.REGULAR)
    val titleHeight = lineHeight(MESSAGE_SP, scale) * titleLines
    val hintHeight = lineHeight(HINT_SP, scale) * hintLines
    val (title, lines, hint) = when {
        titleLines <= MESSAGE_MAX_LINES && hintLines <= MESSAGE_MAX_LINES && titleHeight + hintHeight <= room.value ->
            Triple(message.title, titleLines, true)

        titleLines <= MESSAGE_MAX_LINES && titleHeight <= room.value -> Triple(message.title, titleLines, false)

        else -> Triple(
            message.short,
            measureWidgetLines(
                context,
                message.short,
                MESSAGE_SP,
                width,
                TextWeight.MEDIUM,
            ).coerceIn(1, MESSAGE_MAX_LINES),
            false,
        )
    }
    Column(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(
            text = title,
            style = TextStyle(color = palette.primaryInk, fontSize = MESSAGE_SP.sp, fontWeight = FontWeight.Medium),
            maxLines = lines,
        )
        if (hint) {
            Text(
                text = message.hint,
                style = TextStyle(color = palette.secondaryInk, fontSize = HINT_SP.sp, fontWeight = FontWeight.Normal),
                maxLines = hintLines,
            )
        }
    }
}

/** The locale a card formats in, off the context Glance composes with. */
internal fun Context.widgetLocale(): Locale = resources.configuration.locales[0]

internal fun fontScale(context: Context): Float = context.resources.configuration.fontScale

/**
 * The width one line of text really takes, measured with the face, size and weight the launcher
 * will draw it in (Chiaro's `measureWidgetText`): Glance cannot measure, this process can.
 */
internal fun measureWidgetText(
    context: Context,
    text: String,
    sizeSp: Float,
    weight: TextWeight = TextWeight.REGULAR,
): Dp {
    val metrics = context.resources.displayMetrics
    val paint = widgetPaint(context, weight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics))
    return (paint.measureText(text) * FACE_MARGIN / metrics.density).dp
}

/**
 * How many lines [text] takes at [width], laid out as a `TextView` would lay it out: measured,
 * where a budget that reserved the most a sentence could take would leave the rest as air.
 */
internal fun measureWidgetLines(context: Context, text: String, sizeSp: Float, width: Dp, weight: TextWeight): Int {
    val metrics = context.resources.displayMetrics
    val widthPx = ((width.value - FIT_SLACK) * metrics.density / FACE_MARGIN).toInt()
    if (widthPx <= 0 || text.isEmpty()) return if (text.isEmpty()) 0 else Int.MAX_VALUE
    val paint = widgetPaint(context, weight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics))
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
        .setIncludePad(true)
        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .build()
        .lineCount
}

/**
 * The narrowest width at which [text] still takes the lines it takes at [width]: a date set at
 * that width breaks into lines of even length rather than a full line and an orphan
 * ("Wednesday, October / 7"). What `TextView`'s balanced break strategy would do, which a
 * `RemoteViews` cannot ask for (Passo's `balancedWidth`). [width] itself when the text fits one
 * line or more than [maxLines].
 */
internal fun balancedWidth(
    context: Context,
    text: String,
    sizeSp: Float,
    width: Dp,
    weight: TextWeight,
    maxLines: Int,
): Dp {
    val lines = measureWidgetLines(context, text, sizeSp, width, weight)
    if (lines < 2 || lines > maxLines) return width
    var low = width / 2
    var high = width
    while (high - low > 1.dp) {
        val mid = (low + high) / 2
        if (measureWidgetLines(context, text, sizeSp, mid, weight) == lines) high = mid else low = mid
    }
    return minOf(width, high + FIT_SLACK.dp)
}

/**
 * How wide a clock line set with [pattern] is at [at], over the lines the pattern breaks it into
 * (a date broken after its first word, `ClockPatterns.brokenAfterFirstWord`): its widest line.
 */
internal fun linesWidth(context: Context, pattern: String, at: ZonedDateTime, sizeSp: Float, weight: TextWeight): Dp {
    val text = DateTimeFormatter.ofPattern(pattern, context.widgetLocale()).format(at)
    val em = text.split('\n').maxOf { textEm(context, it, weight) }
    return widthOf(em, sizeSp, fontScale(context)).dp
}

/**
 * The three weights a card sets text in, each with the appearance Glance sets it in
 * (`text_faces.xml`): its weight, in the theme's device-default family.
 */
internal enum class TextWeight(@StyleRes val appearance: Int) {
    REGULAR(R.style.WidgetTextFace_Regular),
    MEDIUM(R.style.WidgetTextFace_Medium),
    BOLD(R.style.WidgetTextFace_Bold),
}

/**
 * A paint in the face the launcher will draw [weight] in, at [sizePx]: the very span Glance puts
 * on a weighted `Text`. Measured in plain sans-serif instead, a Samsung's SamsungOne came out a
 * tenth wider than budgeted (Passo, 5 Oct 2026).
 */
internal fun widgetPaint(context: Context, weight: TextWeight, sizePx: Float): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        TextAppearanceSpan(context, weight.appearance).updateMeasureState(this)
        textSize = sizePx
    }

/**
 * The width of [text] in ems of the system face at [weight]: independent of the density and of
 * the reader's font scale, which the arithmetic applies itself (`:core:domain`'s `AgendaFit`).
 */
internal fun textEm(context: Context, text: String, weight: TextWeight): Float =
    widgetPaint(context, weight, EM_PROBE_PX).measureText(text) * FACE_MARGIN / EM_PROBE_PX

/**
 * How much wider than measured a card budgets every line: measured in the launcher's own face,
 * but a launcher may still set a line a little wider (its hinting, a face substituted for a
 * digit). `WidgetFitTest` draws every form 5% wider than measured and finds nothing cut.
 */
internal const val FACE_MARGIN = 1.06f

private const val EM_PROBE_PX = 100f

/** Chiaro's card: the corner, the words' inset, and the snug inset of a one-row card. */
internal val WidgetCorner = 24.dp
internal val WidgetCardPadding = 14.dp
internal val WidgetCardPaddingSnug = 6.dp

internal const val MESSAGE_SP = 14f
internal const val HINT_SP = 12f
internal const val MESSAGE_MAX_LINES = 3

private const val QUIET_ALPHA = 0.75f
private const val ATTENTION_ALPHA = 0.85f
private const val TRACK_ALPHA = 0.2f
