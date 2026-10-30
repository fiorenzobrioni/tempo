package com.callbackdev.tempo.widget

import android.content.Context
import android.text.format.DateFormat
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.domain.widget.CardFooter
import com.callbackdev.tempo.core.domain.widget.CardLine
import com.callbackdev.tempo.core.domain.widget.CardSummary
import com.callbackdev.tempo.core.domain.widget.TodayNote
import com.callbackdev.tempo.core.domain.widget.WordsFocus
import com.callbackdev.tempo.core.domain.widget.WordsNote
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.callbackdev.tempo.core.designsystem.R as DesignR

/*
 * The words the two cards print. The times and the titles are Today's (`AgendaText`, one place
 * for the agenda's words), so the card and the screen never tell the day two ways; what is the
 * card's own (its counts, its notes) is said in clock times and counts, never "in 40 minutes",
 * which a card redrawn at the agenda's boundaries would leave wrong a minute later.
 */
internal class CardText(private val context: Context, model: WidgetModel) {
    private val resources = context.resources
    private val locale = context.widgetLocale()
    val uses24Hour: Boolean = widgetUses24Hour(context, model.look.clockFormat ?: model.settings.clockFormat)
    val agenda = AgendaText(resources, locale, model.now.zone, uses24Hour)
    private val today: LocalDate = model.now.toLocalDate()

    /** The date style a card's date is drawn in: its own, or Tempo's. */
    val dateStyle: DateStyle = model.look.dateStyle ?: model.settings.dateStyle

    fun title(event: EventInstance): String = agenda.title(event)

    /**
     * A row's time as a range: "10:00 – 11:00", "Until 01:30", "From 23:00", "All day", "Day 2 of 5".
     * Capitalised as the column's other words are: it begins its cell.
     */
    fun range(line: CardLine): String = when (line) {
        is CardLine.AllDay -> line.entries.singleOrNull()?.takeIf { it.dayCount > 1 }
            ?.let { context.getString(R.string.widget_day_of, it.dayNumber, it.dayCount) }
            ?: context.getString(R.string.widget_all_day)

        is CardLine.Timed -> timed(line, startOnly = false)

        is CardLine.Heading, is CardLine.Note -> ""
    }

    /** The same, where only the start fits: "10:00", "Until 11:00" for the one under way. */
    fun start(line: CardLine): String = when (line) {
        is CardLine.Timed -> timed(line, startOnly = true)
        else -> range(line)
    }

    /**
     * The same in fewer letters where there are fewer: "→ 11:00" for "Until 11:00", "→ Tomorrow ·
     * 06:30" for an end on a later date, as «In words» says them; any other time as it is. An
     * inline row prints it where the full form would widen the time column for every title
     * (`AgendaFit.timeColumn`).
     */
    fun short(line: CardLine, startOnly: Boolean): String {
        if (line is CardLine.Timed && line.saysUntil) {
            val end = line.entry.event.end
            val ends = agenda.endDate(end)
            val time = agenda.time(end)
            return context.getString(
                R.string.widget_until_short,
                if (ends <= line.date) time else context.getString(R.string.widget_when_day, heading(ends), time),
            )
        }
        return if (startOnly) start(line) else range(line)
    }

    /** An event under way, or one that began on an earlier date: its row says when it ends. */
    private val CardLine.Timed.saysUntil: Boolean
        get() = !entry.startsOnDate || entry.state == EntryState.UNDER_WAY

    private fun timed(line: CardLine.Timed, startOnly: Boolean): String {
        val event = line.entry.event
        return when {
            // An end on a later date says its day ("Until tomorrow at 06:00"): a bare time reads as the row's date's.
            line.saysUntil -> agenda.until(event.end, line.date)

            !line.entry.endsOnDate -> agenda.from(event.begin)

            startOnly -> agenda.time(event.begin)

            else -> agenda.range(event)
        }.replaceFirstChar { it.titlecase(locale) }
    }

    /** A day ahead's heading: "Tomorrow", else the weekday and the day ("Thursday 9"). */
    fun heading(date: LocalDate): String = if (date == today.plusDays(1)) {
        context.getString(R.string.widget_tomorrow)
    } else {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEd"), locale).format(date)
            .replaceFirstChar { it.titlecase(locale) }
    }

    fun note(note: TodayNote): String = resources.getString(
        when (note) {
            TodayNote.NOTHING_LEFT -> DesignR.string.sentence_done
            TodayNote.NOTHING_TODAY -> DesignR.string.sentence_free_day
        },
    )

    fun footer(footer: CardFooter): String = when (footer) {
        is CardFooter.MoreToday -> resources.getQuantityString(R.plurals.widget_more_today, footer.count, footer.count)
        is CardFooter.MoreLater -> resources.getQuantityString(R.plurals.widget_more_later, footer.count, footer.count)
    }

    /** A line's title: the event's, or a date's all-day events one after the other. */
    fun title(line: CardLine): String = when (line) {
        is CardLine.AllDay -> line.entries.joinToString(", ") { title(it.event) }
        is CardLine.Timed -> title(line.entry.event)
        is CardLine.Heading -> heading(line.date)
        is CardLine.Note -> note(line.note)
    }

    /** What TalkBack reads for a row: one sentence, as on Today. */
    fun describe(line: CardLine, calendars: Map<Long, CalendarInfo>): String = when (line) {
        is CardLine.AllDay -> listOf(range(line), title(line)).joinToString(", ")
        is CardLine.Timed -> describe(line.entry.event, calendars)
        is CardLine.Heading -> heading(line.date)
        is CardLine.Note -> note(line.note)
    }

    /** A timed event as one sentence: its times, its title, where, and in which calendar. */
    fun describe(event: EventInstance, calendars: Map<Long, CalendarInfo>): String {
        val time = if (event.begin == event.end) {
            agenda.time(event.begin)
        } else {
            context.getString(R.string.widget_a11y_range, agenda.time(event.begin), agenda.time(event.end))
        }
        return listOfNotNull(
            time,
            title(event),
            event.location,
            calendars[event.calendarId]?.let { context.getString(R.string.widget_a11y_in_calendar, it.name) },
        ).joinToString(", ")
    }

    // --- «In words» -------------------------------------------------------------------------------

    fun focusTitle(focus: WordsFocus): String? = when (focus) {
        is WordsFocus.UnderWay -> title(focus.event)
        is WordsFocus.Next -> title(focus.event)
        WordsFocus.Free -> null
    }

    /**
     * The focus's time in words, the fullest first, as a card tries them: "Until 11:00",
     * "→ 11:00"; "15:00 – 16:00", "15:00"; "Tomorrow · 9:00 – 10:00", "Tomorrow · 9:00",
     * "Tomorrow". None for "Free". Clock times, never minutes from now (the card is redrawn at
     * boundaries). An end is never a bare time: under a title, "11:00" reads as when it starts;
     * nor, on a later date, a time without its day ("Until tomorrow at 06:00", "→ Tomorrow · 06:00",
     * "→ Tomorrow").
     */
    fun whenOptions(focus: WordsFocus): List<String> = when (focus) {
        is WordsFocus.UnderWay -> {
            val end = focus.event.end
            val ends = agenda.endDate(end)
            val full = agenda.until(end, today).replaceFirstChar { it.titlecase(locale) }
            if (ends <= today) {
                listOf(full, context.getString(R.string.widget_until_short, agenda.time(end)))
            } else {
                val day = heading(ends)
                listOf(
                    full,
                    context.getString(
                        R.string.widget_until_short,
                        context.getString(R.string.widget_when_day, day, agenda.time(end)),
                    ),
                    context.getString(R.string.widget_until_short, day),
                )
            }
        }

        is WordsFocus.Next -> {
            val times = listOf(agenda.range(focus.event), agenda.time(focus.event.begin)).distinct()
            if (focus.today) {
                times
            } else {
                val day = heading(focus.date)
                times.map { context.getString(R.string.widget_when_day, day, it) } + day
            }
        }

        WordsFocus.Free -> emptyList()
    }

    fun note(note: WordsNote): String = when (note) {
        is WordsNote.ThenMore -> if (note.count == 0) {
            context.getString(R.string.widget_note_last)
        } else {
            resources.getQuantityString(R.plurals.widget_note_then_more, note.count, note.count)
        }

        is WordsNote.FreeUntilThen -> if (note.after == 0) {
            context.getString(R.string.widget_note_free_until_last)
        } else {
            resources.getQuantityString(R.plurals.widget_note_free_until, note.after, note.after)
        }

        is WordsNote.DayDone -> {
            val first = note.allDay.firstOrNull()
            when {
                first == null -> note(if (note.hadEvents) TodayNote.NOTHING_LEFT else TodayNote.NOTHING_TODAY)

                note.allDay.size == 1 -> context.getString(R.string.widget_note_all_day, title(first.event))

                else -> resources.getQuantityString(
                    R.plurals.widget_note_all_day_more,
                    note.allDay.size - 1,
                    title(first.event),
                    note.allDay.size - 1,
                )
            }
        }
    }

    /**
     * The rest of today in a few words, a line each: "4 left today", "until 19:30"; an end on a
     * later date says its day ("until tomorrow at 06:30"), as the sentence's does.
     */
    fun summary(summary: CardSummary): List<String> = listOf(
        resources.getQuantityString(R.plurals.widget_summary_left, summary.left, summary.left),
        agenda.until(summary.until, today),
    )

    /** "Then 16:30 · 18:00", with as many times as [count]. */
    fun then(events: List<EventInstance>, count: Int): String =
        context.getString(R.string.widget_then, events.take(count).joinToString(" · ") { agenda.time(it.begin) })
}

/** What stands in the agenda's place, the hint of what a touch does, and the one word for a small card. */
internal data class CardMessage(val title: String, val hint: String, val short: String)

internal fun cardMessage(context: Context, content: WidgetContent): CardMessage? {
    val (title, hint, short) = when (content) {
        WidgetContent.NoPermission ->
            Triple(R.string.widget_permission_title, R.string.widget_permission_hint, R.string.widget_permission_short)

        WidgetContent.NoCalendars ->
            Triple(
                R.string.widget_no_calendars_title,
                R.string.widget_no_calendars_hint,
                R.string.widget_no_calendars_short,
            )

        WidgetContent.AllHidden ->
            Triple(R.string.widget_all_hidden_title, R.string.widget_all_hidden_hint, R.string.widget_all_hidden_short)

        WidgetContent.Unavailable ->
            Triple(
                R.string.widget_unavailable_title,
                R.string.widget_unavailable_hint,
                R.string.widget_unavailable_short,
            )

        is WidgetContent.Ready -> return null
    }
    return CardMessage(context.getString(title), context.getString(hint), context.getString(short))
}
