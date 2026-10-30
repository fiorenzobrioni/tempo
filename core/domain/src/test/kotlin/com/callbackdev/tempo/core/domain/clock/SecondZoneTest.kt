package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.domain.agenda.NewYork
import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.Tokyo
import com.callbackdev.tempo.core.domain.agenda.at
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId

/** The second clock: which zone it shows, and which day it is there (step 8.5). */
class SecondZoneTest {
    @Test
    fun `a chosen city is its zone`() {
        assertThat(SecondZone.of("America/New_York", Rome)).isEqualTo(NewYork)
    }

    @Test
    fun `none chosen, or an id this phone does not know, is off`() {
        assertThat(SecondZone.of(null, Rome)).isNull()
        assertThat(SecondZone.of("Mars/Olympus_Mons", Rome)).isNull()
        assertThat(SecondZone.of("UTC", Rome)).isNull()
    }

    @Test
    fun `the phone's own zone is not a second clock, under any of its names`() {
        assertThat(SecondZone.of("America/New_York", NewYork)).isNull()
        assertThat(SecondZone.of("Asia/Calcutta", ZoneId.of("Asia/Kolkata"))).isNull()
    }

    @Test
    fun `a city on the same clock is still the reader's choice`() {
        assertThat(SecondZone.of("Europe/Paris", Rome)).isEqualTo(ZoneId.of("Europe/Paris"))
    }

    @Test
    fun `tomorrow, yesterday or the same date`() {
        assertThat(SecondZone.daysAhead(at("2026-10-12T23:30"), Rome, Tokyo)).isEqualTo(1)
        assertThat(SecondZone.daysAhead(at("2026-10-12T02:00"), Rome, NewYork)).isEqualTo(-1)
        assertThat(SecondZone.daysAhead(at("2026-10-12T12:00"), Rome, NewYork)).isEqualTo(0)
    }
}
