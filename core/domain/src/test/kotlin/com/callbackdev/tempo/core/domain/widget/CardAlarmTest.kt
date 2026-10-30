package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.today.AlarmDay
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId

/** The next alarm on a card: within a day, said in clock times, and the moments it changes by itself. */
class CardAlarmTest {
    @Test
    fun `an alarm within a day is on the card, with its day`() {
        val now = at("2026-10-07T22:00")
        assertThat(
            CardAlarm.of(at("2026-10-07T23:30"), now, Rome),
        ).isEqualTo(CardAlarm(at("2026-10-07T23:30"), AlarmDay.Today))
        assertThat(
            CardAlarm.of(at("2026-10-08T07:00"), now, Rome),
        ).isEqualTo(CardAlarm(at("2026-10-08T07:00"), AlarmDay.Tomorrow))
    }

    @Test
    fun `an alarm further than a day is noise on a card, and none is none`() {
        val now = at("2026-10-07T10:20")
        assertThat(CardAlarm.of(at("2026-10-08T10:20"), now, Rome)).isNotNull()
        assertThat(CardAlarm.of(at("2026-10-08T10:21"), now, Rome)).isNull()
        assertThat(CardAlarm.of(at("2026-10-12T07:00"), now, Rome)).isNull()
        assertThat(CardAlarm.of(null, now, Rome)).isNull()
    }

    @Test
    fun `an alarm that has rung is not on the card`() {
        val now = at("2026-10-08T07:00")
        assertThat(CardAlarm.of(at("2026-10-08T07:00"), now, Rome)).isNull()
        assertThat(CardAlarm.of(at("2026-10-08T06:59"), now, Rome)).isNull()
    }

    @Test
    fun `across the night the clocks go back, a day is still twenty-four hours`() {
        // Rome, 25 October 2026: 22:00 to 07:00 next morning is ten hours, and tomorrow's.
        val now = at("2026-10-24T22:00")
        val alarm = checkNotNull(CardAlarm.of(at("2026-10-25T07:00"), now, Rome))
        assertThat(alarm.day).isEqualTo(AlarmDay.Tomorrow)
        assertThat(CardAlarm.of(at("2026-10-25T21:30"), now, Rome)).isNull()
    }

    @Test
    fun `tomorrow's alarm changes at midnight, then leaves when it rings`() {
        val alarm = at("2026-10-08T07:00")
        assertThat(CardAlarm.changes(alarm, at("2026-10-07T22:00"), Rome))
            .containsExactly(at("2026-10-08T00:00"), alarm)
        assertThat(CardAlarm.changes(alarm, at("2026-10-08T01:00"), Rome)).containsExactly(alarm)
    }

    @Test
    fun `an alarm further than a day comes onto the card a day before it rings`() {
        val alarm = at("2026-10-12T07:00")
        assertThat(CardAlarm.changes(alarm, at("2026-10-07T10:20"), Rome)).containsExactly(at("2026-10-11T07:00"))
    }

    @Test
    fun `no alarm, or one that has rung, changes nothing`() {
        val now = at("2026-10-08T08:00")
        assertThat(CardAlarm.changes(null, now, Rome)).isEmpty()
        assertThat(CardAlarm.changes(at("2026-10-08T07:00"), now, Rome)).isEmpty()
    }

    @Test
    fun `midnight is the phone's own`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val alarm = at("2026-10-08T07:00")
        // 22:00 in Rome is 05:00 in Tokyo, where the alarm (14:00 there) is today's: no midnight to wait for.
        assertThat(CardAlarm.changes(alarm, at("2026-10-07T22:00"), tokyo)).containsExactly(alarm)
    }
}
