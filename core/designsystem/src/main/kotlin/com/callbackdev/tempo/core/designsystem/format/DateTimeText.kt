package com.callbackdev.tempo.core.designsystem.format

import android.icu.util.Calendar
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.tempo.core.domain.clock.uses24Hour
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import java.time.DayOfWeek
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

/**
 * Times and dates in the reader's language and format, for every screen and card (CLAUDE.md:
 * the locale's own patterns, never a hard-coded order). Android's CLDR data picks the order, the
 * words and the separators for a skeleton; Tempo only picks the skeleton.
 */
object DateTimeText {
    /**
     * A time, kept whole: a line never breaks between its figures and its day marker ("11:00 /
     * AM", seen in a balanced sentence). CLDR 42 already sets a no-break space there; older data,
     * and the tests' runtime, a plain one, which becomes the same narrow no-break space.
     */
    fun time(moment: TemporalAccessor, locale: Locale, uses24Hour: Boolean): String =
        formatter(locale, if (uses24Hour) "Hm" else "hm").format(moment).replace(' ', NARROW_NO_BREAK_SPACE)

    /**
     * The time as the hero clock sets it: its figures apart from the day marker ("10:20" and
     * "AM"; "10:20" alone on the 24-hour clock), so the marker can be set small beside figures that
     * fill the width. Each part keeps the locale's own pattern, and [TimeParts.markerFirst] says
     * where the locale writes the marker (before the time in Chinese and Korean).
     */
    fun timeParts(moment: TemporalAccessor, locale: Locale, uses24Hour: Boolean): TimeParts {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (uses24Hour) "Hm" else "hm")
        val marker = markerField(pattern) ?: return TimeParts(format(pattern, locale, moment), null, false)
        val figures = pattern.removeRange(marker).trim { it.isWhitespace() || it in PATTERN_SPACES }
        return TimeParts(
            figures = format(figures, locale, moment),
            marker = format(pattern.substring(marker), locale, moment),
            markerFirst = marker.first == 0,
        )
    }

    /** The day marker's field in [pattern] (`a`, or CLDR's day periods `b` and `B`), outside quoted text. */
    private fun markerField(pattern: String): IntRange? {
        var quoted = false
        var index = 0
        while (index < pattern.length) {
            val char = pattern[index]
            when {
                char == '\'' -> quoted = !quoted

                !quoted && char in MARKER_LETTERS -> {
                    var end = index
                    while (end + 1 < pattern.length && pattern[end + 1] == char) end++
                    return index..end
                }
            }
            index++
        }
        return null
    }

    private fun format(pattern: String, locale: Locale, moment: TemporalAccessor): String =
        DateTimeFormatter.ofPattern(pattern, locale).format(moment)

    /**
     * A date in [style]; a long or medium one capitalised when it [startsLine], left as the locale
     * writes it inside a sentence ("Da domani a mercoledì 14 ottobre").
     */
    fun date(moment: TemporalAccessor, locale: Locale, style: DateStyle, startsLine: Boolean = true): String {
        val text = formatter(locale, skeleton(style)).format(moment)
        return if (style == DateStyle.NUMERIC || !startsLine) text else text.replaceFirstChar { it.titlecase(locale) }
    }

    /** A month with its year, as it heads the month's page: "November 2026", "Novembre 2026". */
    fun month(month: YearMonth, locale: Locale): String =
        formatter(locale, "yMMMM").format(month.atDay(1)).replaceFirstChar { it.titlecase(locale) }

    /** A month's name alone, short, as one of twelve to choose from: "Nov", "Set". */
    fun monthShort(month: Month, locale: Locale): String =
        month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) }

    /** A weekday as the head of the month's column: its narrow name, "M", "T", "W". */
    fun weekdayNarrow(day: DayOfWeek, locale: Locale): String =
        day.getDisplayName(TextStyle.NARROW_STANDALONE, locale).uppercase(locale)

    /**
     * The day the reader's week starts on: the locale's own, or the one chosen in the phone's
     * regional preferences, which Android carries in the locale and ICU reads there.
     */
    fun firstDayOfWeek(locale: Locale): DayOfWeek {
        val first = Calendar.getInstance(locale).firstDayOfWeek
        // ICU counts from Sunday as 1; java.time from Monday as 1.
        return DayOfWeek.of((first + DAYS_FROM_SUNDAY) % DAYS_A_WEEK + 1)
    }

    /** The pattern a `TextClock` needs for [style] (Phase 4's widgets), the same as the app's. */
    fun datePattern(locale: Locale, style: DateStyle): String =
        DateFormat.getBestDateTimePattern(locale, skeleton(style))

    private fun skeleton(style: DateStyle) = when (style) {
        DateStyle.LONG -> "EEEEdMMMM"
        DateStyle.MEDIUM -> "EEEdMMM"
        DateStyle.NUMERIC -> "yMMdd"
    }

    private fun formatter(locale: Locale, skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

    private const val MARKER_LETTERS = "abB"

    /** The no-break spaces CLDR sets before a day marker ("10:20 AM" with U+202F since CLDR 42). */
    private const val NARROW_NO_BREAK_SPACE = '\u202F'

    private const val DAYS_A_WEEK = 7
    private const val DAYS_FROM_SUNDAY = 5

    private const val PATTERN_SPACES = "  "
}

/** A time in two parts: see [DateTimeText.timeParts]. */
data class TimeParts(val figures: String, val marker: String?, val markerFirst: Boolean)

/** Whether times are written on the 24-hour clock: the reader's [format], or the phone's switch. */
@Composable
fun uses24Hour(format: ClockFormat): Boolean = format.uses24Hour(DateFormat.is24HourFormat(LocalContext.current))
