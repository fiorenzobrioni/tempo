package com.callbackdev.tempo.core.calendar

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Read fresh each time: a revocation from the system's settings is seen at once (§4.6). */
@RunWith(AndroidJUnit4::class)
class CalendarAccessTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `granted, revoked, granted again`() {
        shadowOf(app).grantPermissions(CalendarAccess.PERMISSION)
        assertThat(CalendarAccess.isGranted(app)).isTrue()
        shadowOf(app).denyPermissions(CalendarAccess.PERMISSION)
        assertThat(CalendarAccess.isGranted(app)).isFalse()
        shadowOf(app).grantPermissions(CalendarAccess.PERMISSION)
        assertThat(CalendarAccess.isGranted(app)).isTrue()
    }
}
