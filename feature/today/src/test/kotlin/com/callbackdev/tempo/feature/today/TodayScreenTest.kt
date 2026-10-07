package com.callbackdev.tempo.feature.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.testing.assertAccessible
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

/** Today as Phase 2 leaves it: the clock and the date, in the reader's formats and language. */
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
    fun `the date in the reader's style`() {
        compose.setContent {
            TempoTheme { TodayScreen(now = moment, uses24Hour = true, dateStyle = DateStyle.NUMERIC) }
        }
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("07/10/2026")
    }

    @Test
    fun `the gear opens Settings`() {
        var opened = false
        compose.setContent {
            TempoTheme { TodayScreen(now = moment, uses24Hour = true, onOpenSettings = { opened = true }) }
        }
        compose.onNodeWithTag(TodayTags.SETTINGS).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the date in Italian, capitalised as a line of its own`() {
        compose.setContent { TempoTheme { TodayScreen(now = moment, uses24Hour = true) } }
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Mercoledì 7 ottobre")
    }
}
