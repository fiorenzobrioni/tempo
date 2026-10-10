package com.callbackdev.tempo.core.designsystem.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.callbackdev.tempo.core.domain.today.DaySummary
import com.callbackdev.tempo.core.domain.today.TodaySentence
import com.callbackdev.tempo.core.domain.today.Tomorrow
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The agenda's words, for Today and for the widgets (Phase 4) alike: one place, so the two never
 * tell the day in two ways. Every word is a resource in both languages; the order of a date and a
 * time is the locale's ([DateTimeText]). Plain functions over [Resources], so a Glance card can
 * call them as well as a composable.
 */
class AgendaText(
    private val resources: Resources,
    private val locale: Locale,
    private val zone: ZoneId,
    val uses24Hour: Boolean,
) {
    fun time(moment: Instant): String = DateTimeText.time(moment.atZone(zone), locale, uses24Hour)

    fun title(event: EventInstance): String = event.title ?: resources.getString(R.string.event_no_title)

    /** "10:00 – 11:00"; a moment alone when the event has no length. */
    fun range(event: EventInstance): String = if (event.begin ==
        event.end
    ) {
        time(event.begin)
    } else {
        resources.getString(R.string.time_range, time(event.begin), time(event.end))
    }

    fun from(moment: Instant): String = resources.getString(R.string.time_from, time(moment))

    fun until(moment: Instant): String = resources.getString(R.string.time_until, time(moment))

    /** "45 min", "2 h", "1 h 30 min", rounded to the minute. */
    fun duration(length: Duration): String {
        val minutes = length.toMinutes().coerceAtLeast(0)
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> resources.getString(R.string.duration_m, m)
            m == 0L -> resources.getString(R.string.duration_h, h)
            else -> resources.getString(R.string.duration_hm, h, m)
        }
    }

    /** The day in one sentence (PLANNING.md §6), ending with its full stop. */
    fun sentence(sentence: TodaySentence): String = when (sentence) {
        is TodaySentence.UnderWay ->
            resources.getString(R.string.sentence_under_way, title(sentence.event), time(sentence.event.end)) +
                more(sentence.laterToday)

        is TodaySentence.Soon ->
            resources.getQuantityString(
                R.plurals.sentence_soon,
                sentence.minutes,
                title(sentence.event),
                sentence.minutes,
            ) +
                more(sentence.laterToday)

        is TodaySentence.FreeUntil ->
            resources.getString(R.string.sentence_free_until, time(sentence.event.begin), title(sentence.event)) +
                andMore(sentence.laterToday)

        is TodaySentence.AllDone -> when (val tomorrow = sentence.tomorrow) {
            is Tomorrow.StartsWith ->
                resources.getString(R.string.sentence_done_tomorrow, time(tomorrow.event.begin), title(tomorrow.event))

            Tomorrow.Free -> resources.getString(R.string.sentence_done_tomorrow_free)

            Tomorrow.NotShown -> resources.getString(R.string.sentence_done)
        }

        is TodaySentence.FreeDay -> when (val tomorrow = sentence.tomorrow) {
            is Tomorrow.StartsWith ->
                resources.getString(
                    R.string.sentence_free_day_tomorrow,
                    time(tomorrow.event.begin),
                    title(tomorrow.event),
                )

            Tomorrow.Free -> resources.getString(R.string.sentence_free_day_tomorrow_free)

            Tomorrow.NotShown -> resources.getString(R.string.sentence_free_day)
        }
    }

    /** ", then 2 more today." or ", the last today." */
    private fun more(count: Int): String = if (count > 0) {
        resources.getQuantityString(R.plurals.sentence_then_more, count, count)
    } else {
        resources.getString(R.string.sentence_last)
    }

    /** After "Free until 15:00, then Dentist": ", and 2 more after it." or the full stop. */
    private fun andMore(count: Int): String = if (count > 0) {
        resources.getQuantityString(R.plurals.sentence_and_more, count, count)
    } else {
        resources.getString(R.string.sentence_end)
    }

    /** A day after today in a few words: "3 events, 9:00 to 18:00." or "Nothing planned." */
    fun summary(summary: DaySummary): String {
        val first = summary.first
        val lastEnd = summary.lastEnd
        return when {
            summary.timedCount > 0 && first != null && lastEnd != null && summary.firstEventStartsOnDate ->
                resources.getQuantityString(
                    R.plurals.summary_events,
                    summary.timedCount,
                    summary.timedCount,
                    time(first),
                    time(lastEnd),
                )

            summary.timedCount > 0 -> resources.getQuantityString(
                R.plurals.summary_events_count,
                summary.timedCount,
                summary.timedCount,
            )

            summary.allDayCount > 0 -> resources.getQuantityString(
                R.plurals.summary_all_day,
                summary.allDayCount,
                summary.allDayCount,
            )

            else -> resources.getString(R.string.summary_nothing)
        }
    }

    /** A date in [style], capitalised as it starts a line. */
    fun date(date: LocalDate, style: DateStyle): String = DateTimeText.date(date, locale, style)

    /**
     * Two days as a span, in words: "From Monday 12 October to Tuesday 13 October", "From tomorrow
     * to Friday 9 October". Not two names around a dash, which read as one day named twice
     * ("Tomorrow – Friday 9 October" said tomorrow was Friday; owner, PLANNING.md §15, 10 Oct 2026).
     */
    fun span(from: LocalDate, to: LocalDate, style: DateStyle, fromTomorrow: Boolean): String {
        val end = DateTimeText.date(to, locale, style, startsLine = false)
        return if (fromTomorrow) {
            resources.getString(R.string.date_span_tomorrow, end)
        } else {
            resources.getString(R.string.date_span, DateTimeText.date(from, locale, style, startsLine = false), end)
        }
    }

    /** A run of empty days in a few words: "Nothing planned." for one, "4 days with nothing planned." */
    fun nothingPlanned(days: Int): String = if (days > 1) {
        resources.getQuantityString(R.plurals.summary_nothing_days, days, days)
    } else {
        nothingPlanned()
    }

    /** "Nothing planned.", a day's summary with nothing in it. */
    fun nothingPlanned(): String = resources.getString(R.string.summary_nothing)

    /** "Alarm tomorrow at 7:00". */
    fun alarm(at: Instant, day: AlarmDay): String = when (day) {
        AlarmDay.Today -> resources.getString(R.string.alarm_today, time(at))

        AlarmDay.Tomorrow -> resources.getString(R.string.alarm_tomorrow, time(at))

        is AlarmDay.On -> resources.getString(
            R.string.alarm_on,
            day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            time(at),
        )
    }
}

/** The agenda's words for the current composition: its resources, its locale, the reader's clock. */
@Composable
fun rememberAgendaText(zone: ZoneId, clockFormat: ClockFormat): AgendaText {
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    val uses24 = uses24Hour(clockFormat)
    return remember(resources, locale, zone, uses24) { AgendaText(resources, locale, zone, uses24) }
}
