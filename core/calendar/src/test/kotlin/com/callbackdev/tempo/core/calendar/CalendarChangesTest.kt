package com.callbackdev.tempo.core.calendar

import android.app.Application
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The page's tick when the calendar changes, and nothing left behind (PLANNING.md §4.5, §9). */
@RunWith(AndroidJUnit4::class)
class CalendarChangesTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val changes = CalendarChanges(app)

    @Test
    fun `a change anywhere in the calendar ticks once per burst`() = runTest {
        changes.flow().test {
            repeat(5) { app.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null) }
            assertThat(awaitItem()).isEqualTo(Unit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the observer is gone when the collector stops`() = runTest {
        changes.flow().test {
            assertThat(observers()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(observers()).isEmpty()
    }

    private fun observers() = shadowOf(app.contentResolver).getContentObservers(CalendarContract.CONTENT_URI)
}
