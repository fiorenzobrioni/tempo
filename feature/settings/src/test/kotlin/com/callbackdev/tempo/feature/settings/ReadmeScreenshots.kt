package com.callbackdev.tempo.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarGroups
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.UserSettings
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's picture of Settings' calendars (docs/screenshots). Run with
 * `./gradlew :feature:settings:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("tempo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun calendars() {
        val calendars = listOf(
            calendar(1, "Personal", "lucia@example.com", 0xFF0B8043.toInt(), primary = true),
            calendar(2, "Family", "lucia@example.com", 0xFFF4511E.toInt()),
            calendar(3, "Holidays in Italy", "lucia@example.com", 0xFF8E24AA.toInt()),
            calendar(4, "Work", "lucia@studio.example", 0xFF039BE5.toInt(), primary = true),
            calendar(5, "Team rota", "lucia@studio.example", 0xFFF6BF26.toInt()),
        )
        val settings = CalendarChoices.choose(UserSettings(), calendars[4], shown = false)
        compose.setContent {
            TempoTheme {
                SettingsScreen(
                    SettingsUiState(settings, CalendarsState.Listed(CalendarGroups.of(calendars)), "1.0.0"),
                    CalendarPermission.GRANTED,
                    onBack = {},
                    actions = SettingsActions(),
                )
            }
        }
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.calendar(5)))
        compose.waitForIdle()
        save("settings-calendars")
    }

    private fun calendar(id: Long, name: String, account: String, color: Int, primary: Boolean = false) = CalendarInfo(
        id = id,
        name = name,
        accountName = account,
        accountType = "com.google",
        color = color,
        visibleInProvider = true,
        isPrimary = primary,
    )

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
