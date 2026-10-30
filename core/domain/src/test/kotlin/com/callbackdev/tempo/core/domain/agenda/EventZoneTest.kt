package com.callbackdev.tempo.core.domain.agenda

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Month
import java.time.ZoneId

/** An event's own zone is said only where its clock reads another time than the phone's (§4.2, step 8.5). */
class EventZoneTest {
    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val london = ZoneId.of("Europe/London")

    /** A one-hour event at [local] in [zone], written in the zone [id]. */
    private fun writtenIn(id: String?, local: String = "2026-10-12T09:00", zone: ZoneId = Rome) =
        timed(1, local, local.replaceAfter('T', "23:00"), zone).copy(timeZone = id)

    @Test
    fun `a flight booked in Lisbon, read in Rome, says Lisbon`() {
        assertThat(EventZone.differs(writtenIn("Europe/Lisbon"), Rome)).isEqualTo(lisbon)
    }

    @Test
    fun `Paris beside Rome says nothing, one clock`() {
        assertThat(EventZone.differs(writtenIn("Europe/Paris", "2026-01-12T09:00"), Rome)).isNull()
        assertThat(EventZone.differs(writtenIn("Europe/Paris", "2026-07-12T09:00"), Rome)).isNull()
    }

    @Test
    fun `the phone's own zone says nothing`() {
        assertThat(EventZone.differs(writtenIn("Europe/Rome"), Rome)).isNull()
    }

    @Test
    fun `London beside Lisbon says nothing, all year`() {
        Month.entries.forEach { month ->
            val local = "2026-%02d-15T09:00".format(month.value)
            assertThat(EventZone.differs(writtenIn("Europe/London", local, london), lisbon)).isNull()
        }
    }

    @Test
    fun `a zone with Rome's offset in one season only is said in the other`() {
        // Lagos keeps UTC+1 all year: Rome's winter clock, an hour behind its summer one.
        assertThat(EventZone.differs(writtenIn("Africa/Lagos", "2026-01-12T09:00"), Rome)).isNull()
        assertThat(EventZone.differs(writtenIn("Africa/Lagos", "2026-07-12T09:00"), Rome))
            .isEqualTo(ZoneId.of("Africa/Lagos"))
    }

    @Test
    fun `the offsets are compared at the event's start, not now`() {
        // The night Rome's clocks go back (25 Oct 2026): Lagos matches Rome from 03:00, not before.
        assertThat(EventZone.differs(writtenIn("Africa/Lagos", "2026-10-25T01:00"), Rome)).isNotNull()
        assertThat(EventZone.differs(writtenIn("Africa/Lagos", "2026-10-25T04:00"), Rome)).isNull()
    }

    @Test
    fun `an all-day event never says a zone`() {
        val holiday = allDay(1, "2026-10-12").copy(timeZone = "America/New_York")
        assertThat(EventZone.differs(holiday, Rome)).isNull()
    }

    @Test
    fun `UTC, GMT, offsets and the Etc zones name no place`() {
        listOf("UTC", "GMT", "Z", "+02:00", "UTC+05:00", "Etc/UTC", "Etc/GMT+5").forEach { id ->
            assertThat(EventZone.differs(writtenIn(id), Rome)).isNull()
        }
    }

    @Test
    fun `no zone, a blank one or an unknown one says nothing`() {
        listOf(null, "", "  ", "Mars/Olympus_Mons", "Pacific Standard Time").forEach { id ->
            assertThat(EventZone.differs(writtenIn(id), Rome)).isNull()
        }
    }

    @Test
    fun `an old name for a zone is still its place`() {
        assertThat(EventZone.differs(writtenIn("US/Eastern"), Rome)).isEqualTo(ZoneId.of("US/Eastern"))
    }
}
