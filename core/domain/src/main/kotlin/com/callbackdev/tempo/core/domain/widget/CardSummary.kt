package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.EntryState
import java.time.Instant

/**
 * The rest of today in a few words, for the leading column of «Agenda»'s wide card under the
 * date (PLANNING.md §11, 8.2): how many timed events are left today, the one under way among
 * them, and when the last of them ends: "4 left today", "until 19:30". It stands where the next
 * alarm was planned: the alarm's change did not reach a card in the background reliably on the
 * owner's phone, and a card must not say what it cannot keep true (owner, 30 Oct 2026). These are
 * counts and clock times the card is redrawn at anyway (an event starts or ends, the calendar
 * changes), so they never go stale.
 *
 * None when nothing timed is left today: the list beside it shows it already, with its note
 * ("Nothing left today.") or tomorrow's heading after today's all-day line; and the note's words
 * are wider than the four-by-two's column, which would have to drop them anyway.
 *
 * @property left the timed events of today not over yet, the ones under way included.
 * @property until the last end among them; on a later date for an event that runs past midnight.
 */
data class CardSummary(val left: Int, val until: Instant) {
    companion object {
        fun of(agenda: Agenda): CardSummary? {
            val left = agenda.days.firstOrNull()?.timed.orEmpty().filter { it.state != EntryState.PAST }
            if (left.isEmpty()) return null
            return CardSummary(left.size, left.maxOf { it.event.end })
        }
    }
}
