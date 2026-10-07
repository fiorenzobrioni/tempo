package com.callbackdev.tempo.core.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.tempo.core.domain.clock.uses24Hour
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/**
 * Times and dates in the reader's language and format, for every screen and card (CLAUDE.md:
 * the locale's own patterns, never a hard-coded order). Android's CLDR data picks the order, the
 * words and the separators for a skeleton; Tempo only picks the skeleton.
 */
object DateTimeText {
    fun time(moment: TemporalAccessor, locale: Locale, uses24Hour: Boolean): String =
        formatter(locale, if (uses24Hour) "Hm" else "hm").format(moment)

    /** A date in [style]; a long or medium one capitalised, as it starts a line of its own. */
    fun date(moment: TemporalAccessor, locale: Locale, style: DateStyle): String {
        val text = formatter(locale, skeleton(style)).format(moment)
        return if (style == DateStyle.NUMERIC) text else text.replaceFirstChar { it.titlecase(locale) }
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
}

/** Whether times are written on the 24-hour clock: the reader's [format], or the phone's switch. */
@Composable
fun uses24Hour(format: ClockFormat): Boolean = format.uses24Hour(DateFormat.is24HourFormat(LocalContext.current))
