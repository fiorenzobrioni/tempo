package com.callbackdev.tempo.core.calendar

import android.Manifest
import android.app.Application
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The page's tick when the calendar changes, and nothing left behind (PLANNING.md §4.5, §9). */
@RunWith(AndroidJUnit4::class)
class CalendarChangesTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val changes = CalendarChanges(app)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
    }

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

    @Test
    fun `without the permission nothing is watched`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        changes.flow().test {
            awaitComplete()
        }
        assertThat(observers()).isEmpty()
    }

    @Test
    fun `a watch that could not start starts when the page asks again, the permission granted`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CALENDAR)
        val restarts = MutableStateFlow(0)
        changes.flow(restarts).test {
            assertThat(observers()).isEmpty()
            // The reader allows it from the page's card: the page reads again, and watches again.
            shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
            restarts.value++
            assertThat(observers()).hasSize(1)
            app.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
            assertThat(awaitItem()).isEqualTo(Unit)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(observers()).isEmpty()
    }

    @Test
    fun `each restart leaves one observer, never two`() = runTest {
        val restarts = MutableStateFlow(0)
        changes.flow(restarts).test {
            repeat(3) { restarts.value++ }
            assertThat(observers()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun observers() = shadowOf(app.contentResolver).getContentObservers(CalendarContract.CONTENT_URI)
}
