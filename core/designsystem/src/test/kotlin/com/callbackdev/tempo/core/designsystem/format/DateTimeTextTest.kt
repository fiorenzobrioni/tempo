package com.callbackdev.tempo.core.designsystem.format

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.model.DateStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** The hero clock's parts and the dates, as Android's CLDR data writes them for the app's languages. */
@RunWith(AndroidJUnit4::class)
class DateTimeTextTest {
    private val tenTwenty: LocalTime = LocalTime.of(10, 20)
    private val evening: LocalTime = LocalTime.of(21, 5)

    @Test
    fun `the 24-hour clock has figures alone`() {
        val parts = DateTimeText.timeParts(tenTwenty, Locale.UK, uses24Hour = true)
        assertThat(parts).isEqualTo(TimeParts("10:20", null, markerFirst = false))
        assertThat(DateTimeText.timeParts(evening, Locale.ITALY, uses24Hour = true).figures).isEqualTo("21:05")
    }

    @Test
    fun `the 12-hour clock sets its marker apart, with no space left on the figures`() {
        val parts = DateTimeText.timeParts(evening, Locale.US, uses24Hour = false)
        assertThat(parts.figures).isEqualTo("9:05")
        assertThat(parts.marker).isEqualTo("PM")
        assertThat(parts.markerFirst).isFalse()
        // The two parts, joined, are the time the locale writes.
        val whole = DateTimeText.time(evening, Locale.US, uses24Hour = false)
        assertThat(whole.filterNot { it.isWhitespace() }).isEqualTo(parts.figures + parts.marker)
    }

    @Test
    fun `a locale that writes the marker first says so`() {
        val parts = DateTimeText.timeParts(tenTwenty, Locale.KOREA, uses24Hour = false)
        assertThat(parts.marker).isNotNull()
        assertThat(parts.markerFirst).isTrue()
        assertThat(parts.figures).isEqualTo("10:20")
    }

    @Test
    fun `a date is capitalised as it starts a line, a numeric one left alone`() {
        val date = LocalDate.of(2026, 10, 7)
        assertThat(DateTimeText.date(date, Locale.ITALY, DateStyle.LONG)).isEqualTo("Mercoledì 7 ottobre")
        assertThat(DateTimeText.date(date, Locale.UK, DateStyle.MEDIUM)).isEqualTo("Wed, 7 Oct")
        assertThat(DateTimeText.date(date, Locale.UK, DateStyle.NUMERIC)).isEqualTo("07/10/2026")
    }

    @Test
    fun `a date inside a sentence keeps the locale's own case`() {
        val date = LocalDate.of(2026, 10, 14)
        assertThat(DateTimeText.date(date, Locale.ITALY, DateStyle.LONG, startsLine = false))
            .isEqualTo("mercoledì 14 ottobre")
        assertThat(DateTimeText.date(date, Locale.UK, DateStyle.LONG, startsLine = false))
            .isEqualTo("Wednesday 14 October")
    }
}
