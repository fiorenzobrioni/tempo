package com.callbackdev.tempo.widget

import android.content.Context
import android.text.format.DateFormat
import com.callbackdev.tempo.core.designsystem.format.AgendaText
import com.callbackdev.tempo.core.domain.widget.CardFooter
import com.callbackdev.tempo.core.domain.widget.CardLine
import com.callbackdev.tempo.core.domain.widget.TodayNote
import com.callbackdev.tempo.core.domain.widget.WordsFocus
import com.callbackdev.tempo.core.domain.widget.WordsNote
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.TimedEntry
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

    /** A row's time as a range: "10:00 – 11:00", "until 01:30", "from 23:00", "All day", "Day 2 of 5". */
    fun range(line: CardLine): String = when (line) {
        is CardLine.AllDay -> line.entries.singleOrNull()?.takeIf { it.dayCount > 1 }
            ?.let { context.getString(R.string.widget_day_of, it.dayNumber, it.dayCount) }
            ?: context.getString(R.string.widget_all_day)

        is CardLine.Timed -> timed(line.entry, startOnly = false)

        is CardLine.Heading, is CardLine.Note -> ""
    }

    /** The same, where only the start fits: "10:00", "until 11:00" for the one under way. */
    fun start(line: CardLine): String = when (line) {
        is CardLine.Timed -> timed(line.entry, startOnly = true)
        else -> range(line)
    }

    private fun timed(entry: TimedEntry, startOnly: Boolean): String {
        val event = entry.event
        return when {
            !entry.startsOnDate -> agenda.until(event.end)
            entry.state == EntryState.UNDER_WAY -> agenda.until(event.end)
            !entry.endsOnDate -> agenda.from(event.begin)
            startOnly -> agenda.time(event.begin)
            else -> agenda.range(event)
        }
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
    fun describe(line: CardLine, calendars: Map<Long, CalendarInfo>): String {
        val (event, time) = when (line) {
            is CardLine.AllDay -> return listOf(range(line), title(line)).joinToString(", ")

            is CardLine.Timed -> line.entry.event to if (line.entry.event.begin == line.entry.event.end) {
                agenda.time(line.entry.event.begin)
            } else {
                context.getString(
                    R.string.widget_a11y_range,
                    agenda.time(line.entry.event.begin),
                    agenda.time(line.entry.event.end),
                )
            }

            is CardLine.Heading -> return heading(line.date)

            is CardLine.Note -> return note(line.note)
        }
        return listOfNotNull(
            time,
            title(event),
            event.location,
            calendars[event.calendarId]?.let { context.getString(R.string.widget_a11y_in_calendar, it.name) },
        ).joinToString(", ")
    }

    // --- «In words» -------------------------------------------------------------------------------

    /** The focus large: "15:00", "Until 11:00", "Free". */
    fun hero(focus: WordsFocus): String = when (focus) {
        is WordsFocus.UnderWay -> agenda.until(focus.event.end).replaceFirstChar { it.titlecase(locale) }
        is WordsFocus.Next -> agenda.time(focus.event.begin)
        WordsFocus.Free -> context.getString(R.string.widget_free)
    }

    /** The one-cell card's split of the same: the word over the time ("Until", "11:00"). */
    fun cellHero(focus: WordsFocus): String = when (focus) {
        is WordsFocus.UnderWay -> agenda.time(focus.event.end)
        else -> hero(focus)
    }

    /** The label over the focus: a later day's name; on the one-cell card, also "Until" and "Next". */
    fun label(focus: WordsFocus, cell: Boolean): String? = when (focus) {
        is WordsFocus.UnderWay -> if (cell) context.getString(R.string.widget_label_until) else null

        is WordsFocus.Next -> if (!focus.today) {
            heading(focus.date)
        } else if (cell) {
            context.getString(R.string.widget_label_next)
        } else {
            null
        }

        WordsFocus.Free -> null
    }

    fun focusTitle(focus: WordsFocus): String? = when (focus) {
        is WordsFocus.UnderWay -> title(focus.event)
        is WordsFocus.Next -> title(focus.event)
        WordsFocus.Free -> null
    }

    /**
     * The focus's time in words, the fullest first, as a card tries them: "Until 11:00", "11:00";
     * "15:00 – 16:00", "15:00"; "Tomorrow · 9:00 – 10:00", "Tomorrow · 9:00", "Tomorrow". None
     * for "Free". Clock times, never minutes from now (the card is redrawn at boundaries).
     */
    fun whenOptions(focus: WordsFocus): List<String> = when (focus) {
        is WordsFocus.UnderWay -> listOf(
            agenda.until(focus.event.end).replaceFirstChar { it.titlecase(locale) },
            agenda.time(focus.event.end),
        )

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
