package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.EventInstance
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The zone an event was written in, where saying it helps (PLANNING.md §4.2, step 8.5): a flight
 * booked in Lisbon reads "09:00 in Lisbon" beside the phone's 10:00. The event is still placed by
 * its instants in the phone's zone; this only adds a note.
 */
object EventZone {
    /**
     * The event's own zone, where its clock reads another time than the phone's: a timed event whose
     * zone names a place ([place]) and has another offset than [phoneZone] at the event's start.
     * Paris beside Rome says nothing, and neither does London beside Lisbon, one clock all year; a
     * zone that matches the phone's in one season only is said in the other.
     */
    fun differs(event: EventInstance, phoneZone: ZoneId): ZoneId? {
        if (event.allDay) return null
        val zone = event.timeZone?.let(::place) ?: return null
        val at = event.begin
        return zone.takeIf { it.rules.getOffset(at) != phoneZone.rules.getOffset(at) }
    }

    /**
     * [id] as a zone that names a place, or null: UTC, GMT, a bare offset and the `Etc/` zones name
     * none, so there is no city to say; nor does an id this phone's time-zone data does not know.
     */
    fun place(id: String): ZoneId? {
        val zone = runCatching { ZoneId.of(id.trim()) }.getOrNull() ?: return null
        if (zone is ZoneOffset) return null
        val name = zone.id
        return zone.takeIf { '/' in name && !name.startsWith(NO_PLACE) }
    }

    /** The tz database's zones without a place: fixed offsets from Greenwich, and UTC by other names. */
    private const val NO_PLACE = "Etc/"
}
