package com.callbackdev.tempo.feature.today

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Today drawn from states (PLANNING.md §11 Phase 3: UI tests on states, not databases). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class TodayScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun draw(
        state: TodayUiState = TodaySample.state(),
        permission: CalendarPermission = CalendarPermission.GRANTED,
        actions: TodayActions = TodayActions(),
        dark: Boolean = false,
    ) {
        compose.setContent { TempoTheme(darkTheme = dark) { TodayScreen(state, permission, actions) } }
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `the clock, the date, the alarm and the day in one sentence`() {
        draw()
        compose.onNodeWithTag(TodayTags.TIME).assertTextEquals("10:20")
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wednesday 7 October")
        compose.onNodeWithText("Alarm tomorrow at 07:00").assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.SENTENCE).assertTextEquals("Design review until 11:00, then 3 more today.")
        compose.assertAccessible()
    }

    @Test
    fun `the reader's formats reach the clock and the times`() {
        draw(TodaySample.state(settings = UserSettings(clockFormat = ClockFormat.H12, dateStyle = DateStyle.MEDIUM)))
        compose.onNodeWithTag(TodayTags.TIME).assertTextEquals("10:20 am")
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wed, 7 Oct")
    }

    @Test
    fun `no alarm line when the reader hid it or none is set`() {
        draw(TodaySample.state(alarm = null))
        compose.onNodeWithTag(TodayTags.ALARM).assertDoesNotExist()
    }

    @Test
    fun `the morning folds, now draws the line, the event under way leads`() {
        draw()
        compose.onNodeWithText("2 events earlier today").assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(TodaySample.run.eventId)).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.NOW).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(TodaySample.review.eventId))
            .assert(hasContentDescription("under way, 40 min left", substring = true))
        compose.onNodeWithTag(TodayTags.EARLIER).performClick()
        compose.onNodeWithTag(TodayTags.event(TodaySample.run.eventId)).assertIsDisplayed()
    }

    @Test
    fun `TalkBack reads an event as one sentence`() {
        draw()
        scrollTo(TodayTags.event(TodaySample.dentist.eventId))
        compose.onNodeWithTag(TodayTags.event(TodaySample.dentist.eventId)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("15:00 to 15:45, Dentist, Via Roma 3, in Personal"),
            ),
        )
    }

    @Test
    fun `touching an event, a free gap, the date and the button hand them to the calendar app`() {
        var opened: EventInstance? = null
        var created: Instant? = null
        var day: Instant? = null
        draw(actions = TodayActions(openEvent = { opened = it }, newEvent = { created = it }, openDay = { day = it }))
        compose.onNodeWithTag(TodayTags.event(TodaySample.review.eventId)).performClick()
        assertThat(opened).isEqualTo(TodaySample.review)
        compose.onNodeWithTag(TodayTags.NEW_EVENT).performClick()
        assertThat(created).isEqualTo(TodaySample.at("2026-10-07T10:30").toInstant())
        val gap = hasContentDescription("Free from 11:00, for 2 h")
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(gap)
        compose.onNode(gap).performClick()
        assertThat(created).isEqualTo(TodaySample.at("2026-10-07T11:00").toInstant())
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(TodayTags.DATE))
        compose.onNodeWithTag(TodayTags.DATE).performClick()
        assertThat(day).isNotNull()
    }

    @Test
    fun `tomorrow in full, the days after briefly`() {
        draw()
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasText("Tomorrow"))
        compose.onNodeWithText("3 events, 09:00 to 18:00.").assertIsDisplayed()
        val train = hasContentDescription("Train to Florence", substring = true)
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(train)
        compose.onNode(train).assertIsDisplayed()
    }

    @Test
    fun `without the permission, the clock stays and the way to give it`() {
        var asked = false
        draw(
            state = TodaySample.state().copy(content = TodayContent.NoPermission),
            permission = CalendarPermission.ASKABLE,
            actions = TodayActions(askPermission = { asked = true }),
        )
        compose.onNodeWithTag(TodayTags.TIME).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.SENTENCE).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasText("Allow"))
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
        compose.assertAccessible()
    }

    @Test
    fun `every calendar hidden, the way back to them`() {
        var settings = false
        draw(
            state = TodaySample.state().copy(content = TodayContent.AllHidden),
            actions = TodayActions(openSettings = { settings = true }),
        )
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasText("Choose calendars"))
        compose.onNodeWithText("Choose calendars").performClick()
        assertThat(settings).isTrue()
    }

    @Test
    fun `no calendar on the phone says so`() {
        draw(TodaySample.state().copy(content = TodayContent.NoCalendars))
        compose.onNodeWithText("There is no calendar on this phone yet").assertIsDisplayed()
    }

    @Test
    fun `no calendar app, no button that would do nothing, and a word on why`() {
        draw(TodaySample.state(doors = CalendarDoors.None))
        compose.onNodeWithTag(TodayTags.NEW_EVENT).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasText("No calendar app on this phone"))
        compose.onNodeWithText("No calendar app on this phone").assertIsDisplayed()
    }

    @Test
    fun `an empty day says so before anything else`() {
        draw(TodaySample.state(instances = emptyList()))
        compose.onNodeWithTag(TodayTags.SENTENCE).assertTextEquals("Nothing on the calendar today or tomorrow.")
    }

    @Test
    fun `the evening points at tomorrow`() {
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T21:00")))
        compose.onNodeWithTag(TodayTags.SENTENCE)
            .assertTextEquals("Nothing left today; tomorrow starts at 09:00 with Call with Lisbon.")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the day in Italian`() {
        draw()
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Mercoledì 7 ottobre")
        compose.onNodeWithTag(TodayTags.SENTENCE).assertTextEquals("Design review fino alle 11:00, poi altri 3 oggi.")
    }

    @Test
    fun `the whole page reads, screen by screen`() {
        draw()
        compose.walkPage(hasTestTag(TodayTags.LIST), "today")
    }

    @Test
    fun `the whole page reads in the dark`() {
        draw(dark = true)
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the whole page reads at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `the whole page reads on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-foldable")
    }
}
