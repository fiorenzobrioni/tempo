package com.callbackdev.tempo.shell

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.R
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The launch's ground is written by hand in the app's resources (a window's theme is read before
 * any code runs) and must be the page's: the default dress's surface, by day and at night. A
 * scheme drawn again with another surface would bring back the flash between the launch and the
 * first frame, and nothing else would say so.
 */
@RunWith(AndroidJUnit4::class)
class LaunchGroundTest {
    @get:Rule val compose = createComposeRule()

    private fun isThePages() {
        var surface = Color.Unspecified
        // The theme as a reader who changed nothing has it: the default dress, the system's day or night.
        compose.setContent { TempoTheme { surface = MaterialTheme.colorScheme.surface } }
        compose.waitForIdle()
        val launch = ApplicationProvider.getApplicationContext<Application>().getColor(R.color.launch_ground)
        assertWithMessage("launch_ground is #%08X, the page's surface #%08X".format(launch, surface.toArgb()))
            .that(launch)
            .isEqualTo(surface.toArgb())
    }

    @Test
    fun `the launch's ground is the page's by day`() = isThePages()

    @Test
    @Config(qualifiers = "night")
    fun `the launch's ground is the page's at night`() = isThePages()
}
