package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AgendaDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One stretch of Today's page below the hero. */
sealed interface AgendaSection {
    /** Today's timeline, whole, whatever it holds: its emptiness is the hero's sentence. */
    data class Today(val day: AgendaDay) : AgendaSection

    /** Tomorrow, drawn as today is: its all-day chips and every event with its times. */
    data class Tomorrow(val day: AgendaDay) : AgendaSection

    /** A day after tomorrow, one line an event. */
    data class Later(val day: AgendaDay) : AgendaSection

    /**
     * One or more days in a row with nothing on them, [from] [to] inclusive, said once: five
     * empty headings in a row pushed the first real event of the week below the fold (owner,
     * PLANNING.md §15, 9 Oct 2026).
     *
     * @property startsTomorrow whether [from] is tomorrow, so the row can say "Tomorrow".
     */
    data class Nothing(val from: LocalDate, val to: LocalDate, val startsTomorrow: Boolean) : AgendaSection {
        val isOneDay: Boolean get() = from == to

        /** How many days the run holds, both ends counted. */
        val days: Int get() = ChronoUnit.DAYS.between(from, to).toInt() + 1
    }
}

/**
 * The agenda's days as the page lays them out (VISION.md, Today; PLANNING.md §6): today on top,
 * tomorrow in full, the days after it compact, and every run of empty days folded into one row.
 */
object AgendaSections {
    fun of(agenda: Agenda): List<AgendaSection> {
        val days = agenda.days
        if (days.isEmpty()) return emptyList()
        val sections = mutableListOf<AgendaSection>(AgendaSection.Today(days.first()))
        var run: AgendaSection.Nothing? = null
        days.drop(1).forEachIndexed { index, day ->
            if (day.isEmpty) {
                run = run?.copy(to = day.date) ?: AgendaSection.Nothing(day.date, day.date, startsTomorrow = index == 0)
                return@forEachIndexed
            }
            run?.let { sections += it }
            run = null
            sections += if (index == 0) AgendaSection.Tomorrow(day) else AgendaSection.Later(day)
        }
        run?.let { sections += it }
        return sections
    }
}
