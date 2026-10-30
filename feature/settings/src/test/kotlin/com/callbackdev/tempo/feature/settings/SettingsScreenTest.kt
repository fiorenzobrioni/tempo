package com.callbackdev.tempo.feature.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import com.callbackdev.tempo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId

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
    fun `the days ahead reach two weeks`() {
        var saved = UserSettings()
        draw(actions = SettingsActions(update = { saved = it(saved) }))
        compose.onNodeWithTag(SettingsTags.HORIZON).assertTextContains("A week", substring = true)
        compose.onNodeWithTag(SettingsTags.HORIZON).performClick()
        listOf("Today only", "Today and tomorrow").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        // The row's value and the dialog's choice.
        compose.onAllNodesWithText("A week").assertCountEquals(2)
        compose.onNodeWithText("Two weeks").performClick()
        assertThat(saved.horizonDays).isEqualTo(14)
    }

    @Test
    fun `two weeks are said as such`() {
        draw(UserSettings(horizonDays = 14))
        compose.onNodeWithTag(SettingsTags.HORIZON).assertTextContains("Two weeks", substring = true)
        compose.assertAccessible()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian, two weeks`() {
        draw(UserSettings(horizonDays = 14))
        compose.onNodeWithTag(SettingsTags.HORIZON).assertTextContains("Due settimane", substring = true)
    }

    @Test
    fun `the second clock is off until a city is chosen`() {
        draw()
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).assertTextContains("Off")
    }

    @Test
    fun `the chosen city is said with its country`() {
        draw(UserSettings(secondZone = "America/New_York"))
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).assertTextContains("New York, United States")
    }

    @Test
    fun `a city is found by a part of its name, accents aside, and chosen`() {
        var saved = UserSettings()
        draw(actions = SettingsActions(update = { saved = it(saved) }))
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("sao pa")
        compose.onNodeWithTag(SettingsTags.zone("America/Sao_Paulo")).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.ZONE_NONE).assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("settings-zone-search")
        compose.onNodeWithTag(SettingsTags.zone("America/Sao_Paulo")).performClick()
        assertThat(saved.secondZone).isEqualTo("America/Sao_Paulo")
        compose.onNodeWithTag(SettingsTags.ZONE_LIST).assertDoesNotExist()
    }

    @Test
    fun `a city is found by its country, and a search that finds none says so`() {
        draw()
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("japan")
        compose.onNodeWithTag(SettingsTags.zone("Asia/Tokyo")).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextClearance()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("Atlantis")
        compose.onNodeWithText("No city matches “Atlantis”.").assertIsDisplayed()
    }

    @Test
    fun `the second clock is turned off from the top of the list`() {
        var saved = UserSettings(secondZone = "Asia/Tokyo")
        draw(settings = saved, actions = SettingsActions(update = { saved = it(saved) }))
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.onNodeWithTag(SettingsTags.ZONE_LIST).performScrollToNode(hasTestTag(SettingsTags.ZONE_NONE))
        compose.onNodeWithTag(SettingsTags.ZONE_NONE).performClick()
        assertThat(saved.secondZone).isNull()
    }

    @Test
    fun `the list opens on the chosen city`() {
        draw(UserSettings(secondZone = "Asia/Tokyo"))
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.onNodeWithTag(SettingsTags.zone("Asia/Tokyo")).assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun `the phone's own zone is not offered, every city says its country and its offset`() {
        compose.setContent {
            TempoTheme {
                SecondZonePage(
                    selected = null,
                    onPick = {},
                    onBack = {},
                    phone = ZoneId.of("Europe/Rome"),
                    now = Instant.parse("2026-01-12T10:00:00Z"),
                )
            }
        }
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("ro")
        compose.onNodeWithTag(SettingsTags.zone("Europe/Rome")).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextClearance()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("lisbon")
        compose.onNodeWithTag(SettingsTags.zone("Europe/Lisbon")).assertTextContains("Portugal · UTC")
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextClearance()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("kolkata")
        // Stored by ICU's canonical name, which java.time knows too.
        compose.onNodeWithTag(SettingsTags.zone("Asia/Calcutta")).assertTextContains("India · UTC+5:30")
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextClearance()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("new york")
        compose.onNodeWithTag(SettingsTags.zone("America/New_York")).assertTextContains("United States · UTC\u22125")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian, the cities are the language's own`() {
        draw()
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.onNodeWithTag(SettingsTags.ZONE_SEARCH).performTextInput("lisbona")
        compose.onNodeWithTag(SettingsTags.zone("Europe/Lisbon")).assertTextContains("Lisbona")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the list of cities reads at twice the text size`() {
        draw(UserSettings(secondZone = "Europe/Lisbon"))
        compose.onNodeWithTag(SettingsTags.SECOND_ZONE).performClick()
        compose.walkPage(hasTestTag(SettingsTags.ZONE_LIST), "settings-zones-large-text", maxScreens = 3)
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
