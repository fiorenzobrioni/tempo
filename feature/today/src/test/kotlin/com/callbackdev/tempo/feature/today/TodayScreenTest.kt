package com.callbackdev.tempo.feature.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.testing.assertAccessible
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

/** Phase 0's Today: the clock and the date, in the reader's format and language. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class TodayScreenTest {
    @get:Rule val compose = createComposeRule()

    private val moment = ZonedDateTime.of(2026, 10, 7, 21, 5, 0, 0, ZoneId.of("Europe/Rome"))

    @Test
    fun `the time and the date, on the 24-hour clock`() {
        compose.setContent { TempoTheme { TodayScreen(now = moment, uses24Hour = true) } }
        compose.onNodeWithTag(TodayTags.TIME).assertIsDisplayed().assertTextEquals("21:05")
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wednesday 7 October")
        compose.assertAccessible()
    }

    @Test
    fun `the 12-hour clock when the reader asks for it`() {
        compose.setContent { TempoTheme { TodayScreen(now = moment, uses24Hour = false) } }
        // The locale's own pattern, with its narrow no-break space before the day period (CLDR).
        compose.onNodeWithTag(TodayTags.TIME).assertTextEquals("9:05\u202Fpm")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the date in Italian, capitalised as a line of its own`() {
        compose.setContent { TempoTheme { TodayScreen(now = moment, uses24Hour = true) } }
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Mercoledì 7 ottobre")
    }
}
