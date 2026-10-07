package com.callbackdev.tempo.feature.today

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.CalendarPermission
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of Today (docs/screenshots), drawn from [TodaySample]'s week. Run with
 * `./gradlew :feature:today:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("tempo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private fun show(state: TodayUiState, dark: Boolean = false) {
        compose.setContent {
            TempoTheme(darkTheme = dark) { TodayScreen(state, CalendarPermission.GRANTED, TodayActions()) }
        }
        compose.waitForIdle()
    }

    /** Mid-morning: a meeting under way, the morning folded, the free hours ahead. */
    @Test
    fun today() {
        show(TodaySample.state())
        save("today")
    }

    /** The evening, in the dark theme: today done, tomorrow's start in the sentence. */
    @Test
    fun todayEveningDark() {
        show(TodaySample.state(now = TodaySample.at("2026-10-07T21:10")), dark = true)
        save("today-evening-dark")
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
