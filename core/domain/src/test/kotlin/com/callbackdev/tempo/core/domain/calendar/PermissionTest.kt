package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.model.CalendarPermission
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionTest {
    @Test
    fun `granted is granted, whatever came before`() {
        assertThat(calendarPermission(granted = true, askedBefore = true, showRationale = false))
            .isEqualTo(CalendarPermission.GRANTED)
    }

    @Test
    fun `never asked, or refused once, can be asked`() {
        assertThat(calendarPermission(granted = false, askedBefore = false, showRationale = false))
            .isEqualTo(CalendarPermission.ASKABLE)
        assertThat(calendarPermission(granted = false, askedBefore = true, showRationale = true))
            .isEqualTo(CalendarPermission.ASKABLE)
    }

    @Test
    fun `asked, refused and no rationale offered is for good`() {
        assertThat(calendarPermission(granted = false, askedBefore = true, showRationale = false))
            .isEqualTo(CalendarPermission.DENIED_FOR_GOOD)
    }
}
