package com.callbackdev.tempo.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.ZoneCities
import com.callbackdev.tempo.core.domain.clock.SecondZone
import com.callbackdev.tempo.core.domain.clock.uses24Hour
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.callbackdev.tempo.core.designsystem.R as DesignR

/*
 * The card's clock and date: the system's `TextClock` (PLANNING.md §7), in a small layout placed
 * in the card through Glance's `AndroidRemoteViews`. The launcher draws it and the system moves it
 * every minute, with no work by Tempo, asleep or awake; a card's own repaints never touch it.
 */

/**
 * The patterns a clock is set with, as `TextClock` takes them: one for a phone on the 12-hour
 * clock and one for the 24-hour, both the locale's own (`DateFormat.getBestDateTimePattern`). The
 * phone's switch picks between them; a reader who chose a format on the card or in Tempo gets
 * that pattern in both.
 */
internal data class ClockPatterns(val twelve: String, val twentyFour: String) {
    /** The pattern this phone draws now: the samples draw it as text. */
    fun current(context: Context): String = if (DateFormat.is24HourFormat(context)) twentyFour else twelve

    /**
     * The same line broken after its first word, for a date over two lines ("Wednesday" over
     * "7 October", "Wed," over "7 Oct"), so a day's number stays with its month; null where the
     * pattern has no space to break at. The break is the pattern's first space outside a quoted
     * literal, which `TextClock` and `DateTimeFormatter` both print as it is.
     */
    fun brokenAfterFirstWord(): ClockPatterns? {
        val twelve = twelve.breakAtFirstSpace() ?: return null
        val twentyFour = twentyFour.breakAtFirstSpace() ?: return null
        return ClockPatterns(twelve, twentyFour)
    }

    /** The time, then [date]: the «In words» card's small line, one `TextClock` for both. */
    fun withDate(date: String): ClockPatterns = ClockPatterns("$twelve$SEPARATOR$date", "$twentyFour$SEPARATOR$date")

    /**
     * The time with [city] before it, as the second clock writes it ("New York 04:20", step 8.5):
     * one `TextClock` line, the city a quoted literal the system prints as it is.
     */
    fun inCity(resources: Resources, city: String): ClockPatterns {
        val literal = literal(city)
        return ClockPatterns(
            resources.getString(DesignR.string.second_clock, literal, twelve),
            resources.getString(DesignR.string.second_clock, literal, twentyFour),
        )
    }

    companion object {
        fun time(locale: Locale, format: ClockFormat): ClockPatterns {
            val twelve = DateFormat.getBestDateTimePattern(locale, "hm")
            val twentyFour = DateFormat.getBestDateTimePattern(locale, "Hm")
            return when (format) {
                ClockFormat.SYSTEM -> ClockPatterns(twelve, twentyFour)
                ClockFormat.H24 -> ClockPatterns(twentyFour, twentyFour)
                ClockFormat.H12 -> ClockPatterns(twelve, twelve)
            }
        }

        /** A date never depends on the clock's format: the same pattern twice. */
        fun date(locale: Locale, style: DateStyle): ClockPatterns =
            DateTimeText.datePattern(locale, style).let { ClockPatterns(it, it) }

        private const val SEPARATOR = " · "

        /**
         * [text] as a pattern's literal: quoted, its own apostrophes doubled ("L'Avana" is
         * `'L''Avana'`), so no letter of a city's name is read as a field of the time.
         */
        fun literal(text: String): String = "'" + text.replace("'", "''") + "'"
    }
}

private fun String.breakAtFirstSpace(): String? {
    var quoted = false
    forEachIndexed { i, c ->
        when {
            c == '\'' -> quoted = !quoted
            c == ' ' && !quoted -> return substring(0, i) + "\n" + substring(i + 1)
        }
    }
    return null
}

/** Whether a card's times are on the 24-hour clock: its own format, Tempo's, or the phone's. */
internal fun widgetUses24Hour(context: Context, format: ClockFormat): Boolean =
    format.uses24Hour(DateFormat.is24HourFormat(context))

/** The face a clock line is set in, live (a `TextClock`) or frozen at a sample's moment (a `TextView`). */
internal enum class ClockFace(val live: Int, val frozen: Int, val weight: TextWeight) {
    BOLD(R.layout.widget_clock_bold, R.layout.widget_clock_bold_frozen, TextWeight.BOLD),
    MEDIUM(R.layout.widget_clock_medium, R.layout.widget_clock_medium_frozen, TextWeight.MEDIUM),
    REGULAR(R.layout.widget_clock_regular, R.layout.widget_clock_regular_frozen, TextWeight.REGULAR),
}

/**
 * One clock line as `RemoteViews`: [patterns] in [face] at [sizeSp] and [color], over at most
 * [maxLines] lines; a touch on it sends [tap]. With [zone] it keeps that zone's time (the second
 * clock: `TextClock`'s own time zone, moved by the system as the first). With [frozenAt] it is the
 * same line drawn as text at that moment (the samples, the previews, the pictures), so it shows
 * the sample's time and not the moment a test happened to run.
 */
internal fun clockViews(
    context: Context,
    face: ClockFace,
    patterns: ClockPatterns,
    sizeSp: Float,
    color: Color,
    maxLines: Int,
    tap: PendingIntent?,
    frozenAt: ZonedDateTime?,
    zone: ZoneId? = null,
): RemoteViews {
    val views = RemoteViews(context.packageName, if (frozenAt == null) face.live else face.frozen)
    if (frozenAt == null) {
        views.setCharSequence(R.id.widget_clock, "setFormat12Hour", patterns.twelve)
        views.setCharSequence(R.id.widget_clock, "setFormat24Hour", patterns.twentyFour)
        if (zone != null) views.setString(R.id.widget_clock, "setTimeZone", zone.id)
    } else {
        val pattern = patterns.current(context)
        views.setTextViewText(
            R.id.widget_clock,
            DateTimeFormatter.ofPattern(pattern, context.widgetLocale())
                .format(zone?.let { frozenAt.withZoneSameInstant(it) } ?: frozenAt),
        )
    }
    views.setTextViewTextSize(R.id.widget_clock, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    views.setTextColor(R.id.widget_clock, color.toArgb())
    views.setInt(R.id.widget_clock, "setMaxLines", maxLines)
    if (tap != null) views.setOnClickPendingIntent(R.id.widget_clock, tap)
    return views
}

/**
 * A card's second clock (step 8.5): the zone it keeps and its line's patterns, the city before the
 * time as the card's own clock writes it.
 */
internal class SecondClock(val zone: ZoneId, val patterns: ClockPatterns)

/**
 * The second clock [model]'s card shows, with [time] as its own clock's patterns; null where the
 * reader did not turn it on for this card, chose no city in Tempo, or is in that city now.
 */
internal fun secondClock(context: Context, model: WidgetModel, time: ClockPatterns): SecondClock? {
    if (!model.look.showSecondZone) return null
    val zone = SecondZone.of(model.settings.secondZone, model.now.zone) ?: return null
    val city = ZoneCities.city(zone, context.widgetLocale()) ?: return null
    return SecondClock(zone, time.inCity(context.resources, city))
}

/**
 * The header's touch as a pending intent of the card's own, rather than Glance's action: Glance
 * gives an intent with no data a made-up one of its own, and the clock app's front door is opened
 * as the launcher opens it. Immutable, one per card and door.
 */
internal fun headerTap(context: Context, appWidgetId: Int, intent: Intent?): PendingIntent? = intent?.let {
    PendingIntent.getActivity(
        context,
        HEADER_REQUEST + appWidgetId,
        it,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private const val HEADER_REQUEST = 0x7E40_0000

/**
 * The width of a clock line's widest text, in ems of [face]: the pattern the phone draws now, at a
 * late, wide minute. Only that one: switching the phone between 12 and 24 hours sends
 * `ACTION_TIME_CHANGED`, which draws every card again (`WidgetSystemReceiver`).
 */
internal fun clockEm(context: Context, patterns: ClockPatterns, face: ClockFace, at: ZonedDateTime): Float {
    val formatter = DateTimeFormatter.ofPattern(patterns.current(context), context.widgetLocale())
    return WideMinutes.maxOf { (h, m) -> textEm(context, formatter.format(at.withHour(h).withMinute(m)), face.weight) }
}

/** Minutes whose digits are the widest a clock shows, in the morning and in the evening. */
private val WideMinutes = listOf(10 to 58, 20 to 48, 23 to 58)
