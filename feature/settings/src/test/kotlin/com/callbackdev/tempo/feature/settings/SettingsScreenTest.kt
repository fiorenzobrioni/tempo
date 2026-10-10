package com.callbackdev.tempo.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarGroups
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings drawn from states (PLANNING.md §11 Phase 2), never from a store or a provider. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val family = calendar(1, "Family", "reader@example.com", color = 0xFF0B8043.toInt())
    private val birthdays = calendar(2, "Birthdays", "reader@example.com", color = 0xFFF6BF26.toInt(), visible = false)
    private val team = calendar(3, "Team", "work@example.com", color = null)
    private val listed = CalendarsState.Listed(CalendarGroups.of(listOf(family, birthdays, team)))

    private fun draw(
        settings: UserSettings = UserSettings(),
        calendars: CalendarsState? = listed,
        permission: CalendarPermission = CalendarPermission.GRANTED,
        actions: SettingsActions = SettingsActions(),
        widgets: WidgetsInfo = WidgetsInfo(),
    ) {
        compose.setContent {
            TempoTheme {
                SettingsScreen(SettingsUiState(settings, calendars, "0.1.0", widgets), permission, onBack = {
                }, actions = actions)
            }
        }
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `the guide's card comes first, and opens the guide`() {
        var opened = false
        draw(actions = SettingsActions(openGuide = { opened = true }))
        compose.onNodeWithTag(SettingsTags.GUIDE).assertIsDisplayed()
        compose.onNodeWithText("How Tempo works").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.GUIDE).performClick()
        assertThat(opened).isTrue()
        compose.assertAccessible()
    }

    @Test
    fun `the clock rows say the formats in use`() {
        draw(UserSettings(clockFormat = ClockFormat.H24))
        compose.onNodeWithText("24-hour", substring = true).assertIsDisplayed()
        compose.assertAccessible()
    }

    @Test
    fun `the widgets are handed to the launcher where it takes them`() {
        var pinned: TempoWidget? = null
        draw(actions = SettingsActions(pinWidget = { pinned = it }), widgets = WidgetsInfo(canPin = true))
        scrollTo(SettingsTags.PIN_WORDS)
        compose.onNodeWithTag(SettingsTags.PIN_WORDS).performClick()
        assertThat(pinned).isEqualTo(TempoWidget.WORDS)
        compose.assertAccessible()
    }

    @Test
    fun `a launcher that takes no request gets the way by hand`() {
        draw(widgets = WidgetsInfo(canPin = false))
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("choose Widgets", substring = true))
        compose.onNodeWithTag(SettingsTags.PIN_AGENDA).assertDoesNotExist()
    }

    @Test
    fun `a choice in a dialog is saved`() {
        var saved = UserSettings()
        draw(actions = SettingsActions(update = { saved = it(saved) }))
        compose.onNodeWithTag(SettingsTags.HORIZON).performClick()
        compose.onNodeWithText("Today and tomorrow").performClick()
        assertThat(saved.horizonDays).isEqualTo(2)
    }

    @Test
    fun `a switch is saved`() {
        var saved = UserSettings()
        draw(actions = SettingsActions(update = { saved = it(saved) }))
        scrollTo(SettingsTags.DECLINED)
        compose.onNodeWithTag(SettingsTags.DECLINED).performClick()
        assertThat(saved.showDeclined).isTrue()
    }

    @Test
    fun `Today's calendar button is on by default and one switch away`() {
        var saved = UserSettings()
        draw(actions = SettingsActions(update = { saved = it(saved) }))
        scrollTo(SettingsTags.CALENDAR_BUTTON)
        compose.onNodeWithTag(SettingsTags.CALENDAR_BUTTON).performClick()
        // One touch from the default turns it off: so the default was on.
        assertThat(saved.showCalendarButton).isFalse()
    }

    @Test
    fun `the calendars by account, each with its state, and how many are shown`() {
        draw()
        scrollTo(SettingsTags.CALENDARS_SUMMARY)
        compose.onNodeWithText("3 calendars on this phone, 2 shown", substring = true).assertIsDisplayed()
        scrollTo(SettingsTags.calendar(1))
        compose.onNodeWithTag(SettingsTags.calendar(1)).assertIsOn()
        scrollTo(SettingsTags.calendar(2))
        // Hidden in the calendar app, and no word from the reader: hidden here too.
        compose.onNodeWithTag(SettingsTags.calendar(2)).assertIsOff()
        compose.assertAccessible()
    }

    @Test
    fun `touching a calendar hides it`() {
        var hidden: Pair<CalendarInfo, Boolean>? = null
        draw(actions = SettingsActions(setCalendarShown = { calendar, shown -> hidden = calendar to shown }))
        scrollTo(SettingsTags.calendar(1))
        compose.onNodeWithTag(SettingsTags.calendar(1)).performClick()
        assertThat(hidden).isEqualTo(family to false)
    }

    @Test
    fun `the reader's word is what the switch shows`() {
        val chosen = CalendarChoices.choose(UserSettings(), birthdays, shown = true)
        draw(settings = chosen)
        scrollTo(SettingsTags.calendar(2))
        compose.onNodeWithTag(SettingsTags.calendar(2)).assertIsOn()
    }

    @Test
    fun `without the permission, the way to give it`() {
        var asked = false
        draw(
            calendars = CalendarsState.NoPermission,
            permission = CalendarPermission.ASKABLE,
            actions = SettingsActions(askPermission = { asked = true }),
        )
        scrollTo(SettingsTags.PERMISSION)
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
    }

    @Test
    fun `refused for good, the app's page in the system's settings`() {
        var opened = false
        draw(
            calendars = CalendarsState.NoPermission,
            permission = CalendarPermission.DENIED_FOR_GOOD,
            actions = SettingsActions(openAppSettings = { opened = true }),
        )
        scrollTo(SettingsTags.PERMISSION)
        compose.onNodeWithText("Open the app’s settings").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `no calendar on the phone says so`() {
        draw(calendars = CalendarsState.Listed(emptyList()))
        compose.onNodeWithTag(
            SettingsTags.LIST,
        ).performScrollToNode(hasText("no calendar on this phone", substring = true))
        compose.onNodeWithText("no calendar on this phone", substring = true).assertIsDisplayed()
    }

    @Test
    fun `nothing is drawn before the store answers`() {
        compose.setContent {
            TempoTheme { SettingsScreen(null, CalendarPermission.GRANTED, onBack = {}, actions = SettingsActions()) }
        }
        compose.onNodeWithTag(SettingsTags.LIST).assertDoesNotExist()
    }

    @Test
    fun `the whole page reads, screen by screen`() {
        draw()
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the whole page reads at twice the text size`() {
        draw(calendars = CalendarsState.NoPermission, permission = CalendarPermission.ASKABLE)
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `the whole page reads on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings-foldable")
    }

    private fun calendar(id: Long, name: String, account: String, color: Int?, visible: Boolean = true) = CalendarInfo(
        id = id,
        name = name,
        accountName = account,
        accountType = "com.google",
        color = color,
        visibleInProvider = visible,
        isPrimary = false,
    )
}
