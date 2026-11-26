package com.callbackdev.tempo.feature.today

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.calendar.CalendarDoors
import com.callbackdev.tempo.core.designsystem.components.COVER_TAG
import com.callbackdev.tempo.core.designsystem.theme.LocalReducedMotion
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.callbackdev.tempo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import kotlin.math.abs

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
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20")
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wednesday 7 October")
        compose.onNodeWithText("Alarm tomorrow at 07:00").assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.SENTENCE).assertTextEquals("Design review until 11:00, then 3 more today.")
        compose.assertAccessible()
    }

    @Test
    fun `the reader's formats reach the clock and the times`() {
        draw(
            TodaySample.state(
                settings = TodaySample.settings.copy(clockFormat = ClockFormat.H12, dateStyle = DateStyle.MEDIUM),
            ),
        )
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20 am")
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wed, 7 Oct")
    }

    @Test
    fun `the alarm line opens the clock app`() {
        var opened = false
        draw(actions = TodayActions(openClock = { opened = true }))
        compose.onNodeWithTag(TodayTags.ALARM)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("opens the clock app") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open the clock app"
                },
            )
            .assert(hasText("Alarm tomorrow at 07:00"))
            .performClick()
        assertThat(opened).isTrue()
        compose.assertAccessible()
    }

    @Test
    fun `with no clock app, the alarm line is no button`() {
        draw(TodaySample.state(doors = TodaySample.allDoors.copy(clockPackage = null)))
        compose.onNodeWithTag(TodayTags.ALARM).assertIsDisplayed().assertHasNoClickAction()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the alarm line's door in Italian`() {
        draw()
        compose.onNodeWithTag(TodayTags.ALARM).assert(
            SemanticsMatcher("apre l'orologio") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Apri l’orologio"
            },
        )
    }

    @Test
    fun `no alarm line when the reader hid it or none is set`() {
        draw(TodaySample.state(alarm = null))
        compose.onNodeWithTag(TodayTags.ALARM).assertDoesNotExist()
    }

    @Test
    fun `a second clock under the alarm, in the reader's city`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(secondZone = "America/New_York")))
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assertIsDisplayed().assertHasNoClickAction()
            .assert(hasText("New York 04:20"))
        compose.assertAccessible()
        compose.writeScreenshot("today-second-clock")
    }

    @Test
    fun `the second clock says its date where it is not the phone's`() {
        val tokyo = TodaySample.settings.copy(secondZone = "Asia/Tokyo")
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T21:10"), settings = tokyo))
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assert(hasText("Tokyo 04:10 · tomorrow"))
    }

    @Test
    fun `the second clock says yesterday in the early morning`() {
        val newYork = TodaySample.settings.copy(secondZone = "America/New_York")
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T01:30"), settings = newYork))
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assert(hasText("New York 19:30 · yesterday"))
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the second clock in Italian`() {
        val tokyo = TodaySample.settings.copy(secondZone = "Asia/Tokyo")
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T21:10"), settings = tokyo))
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assert(hasText("Tokyo 04:10 · domani"))
    }

    @Test
    fun `no second clock when none is chosen`() {
        draw()
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assertDoesNotExist()
    }

    @Test
    fun `no second clock in its own city`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(secondZone = "Europe/Rome")))
        compose.onNodeWithTag(TodayTags.SECOND_CLOCK).assertDoesNotExist()
    }

    @Test
    fun `an event written where the clock reads another time says its start there`() {
        val call = TodaySample.call.copy(timeZone = "Europe/Lisbon")
        draw(TodaySample.state(instances = TodaySample.instances - TodaySample.call + call))
        scrollTo(TodayTags.event(call.eventId))
        compose.onNodeWithTag(TodayTags.event(call.eventId))
            .assertIsDisplayed()
            .assert(
                hasContentDescription(
                    "09:00 to 09:30, 08:00 in Lisbon, Call with Lisbon, Google Meet",
                    substring = true,
                ),
            )
        compose.assertAccessible()
        compose.writeScreenshot("today-event-zone")
    }

    @Test
    fun `an event written on the phone's clock says no zone`() {
        val call = TodaySample.call.copy(timeZone = "Europe/Paris")
        draw(TodaySample.state(instances = TodaySample.instances - TodaySample.call + call))
        scrollTo(TodayTags.event(call.eventId))
        compose.onNodeWithTag(TodayTags.event(call.eventId))
            .assert(hasContentDescription("09:00 to 09:30, Call with Lisbon", substring = true))
        compose.onNode(hasContentDescription("in Paris", substring = true)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `an event's own zone in Italian`() {
        val call = TodaySample.call.copy(timeZone = "Europe/Lisbon")
        draw(TodaySample.state(instances = TodaySample.instances - TodaySample.call + call))
        scrollTo(TodayTags.event(call.eventId))
        compose.onNodeWithTag(TodayTags.event(call.eventId))
            .assert(hasContentDescription("dalle 09:00 alle 09:30, 08:00 ora di Lisbona", substring = true))
    }

    @Test
    fun `the morning folds, and the event under way is now, with no line over it`() {
        draw()
        compose.onNodeWithText("2 events earlier today").assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(TodaySample.run.eventId)).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.NOW).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.event(TodaySample.review.eventId))
            .assert(hasContentDescription("under way, 40 min left", substring = true))
        compose.onNodeWithTag(TodayTags.EARLIER).performClick()
        compose.onNodeWithTag(TodayTags.event(TodaySample.run.eventId)).assertIsDisplayed()
    }

    @Test
    fun `with nothing under way, a line marks now`() {
        // 12:00: the review is over, lunch is an hour away.
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T12:00")))
        compose.onNodeWithTag(TodayTags.NOW).assertIsDisplayed()
        val now = compose.onNodeWithTag(TodayTags.NOW).fetchSemanticsNode().boundsInRoot
        val lunch = compose.onNodeWithTag(TodayTags.event(TodaySample.lunch.eventId)).fetchSemanticsNode().boundsInRoot
        assertThat(now.bottom).isAtMost(lunch.top)
        compose.assertAccessible()
        compose.writeScreenshot("today-free-now")
    }

    @Test
    fun `today has one free row, the next one`() {
        // At 10:20 the day is free from 11:00 for 2 h, from 14:00 for 1 h and from 15:45 for
        // 2 h 45 min: the first is the row, the others are not drawn.
        draw()
        scrollTo(TodayTags.event(TodaySample.swimming.eventId))
        compose.onAllNodesWithTag(TodayTags.FREE).assertCountEquals(1)
        compose.onNodeWithTag(TodayTags.FREE).assert(hasContentDescription("Free from 11:00, for 2 h"))
        compose.onNode(hasContentDescription("Free from 14:00", substring = true)).assertDoesNotExist()
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
    fun `touching an event, a free gap and the date hand them to the calendar app`() {
        var opened: EventInstance? = null
        var created: Instant? = null
        var day: Instant? = null
        draw(
            actions = TodayActions(
                openEvent = { opened = it },
                newEvent = { begin, _ -> created = begin },
                openDay = { day = it },
            ),
        )
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
        assertThat(day).isEqualTo(TodaySample.at("2026-10-07T10:20").toInstant())
    }

    @Test
    @Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
    fun `beside the dial the day marker is whole, on a narrower phone too`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(clockFormat = ClockFormat.H12)))
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20\u202FAM")
        // The marker keeps 4 dp of air after it. Cut short for want of room, its last letter
        // ends in ink at the very edge of its own box; whole, it ends in the page's ground.
        val clock = compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).captureToImage().asAndroidBitmap()
        val ground = clock.getPixel(0, 0)
        val air = (4 * compose.density.density).toInt()
        val lastOfMarker = clock.width - 1 - air
        val edge = (0 until clock.height).map { clock.getPixel(lastOfMarker, it) }
        assertThat(edge.all { it == ground }).isTrue()
    }

    @Test
    fun `the date is read as the date alone, its chevron a sign for the eye`() {
        draw()
        // The chevron stands in the line: a screen reader hears the date, and what a touch opens.
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Wednesday 7 October").assertHasClickAction()
        compose.assertAccessible()
        compose.writeScreenshot("today-date-door")
    }

    @Test
    fun `the button beside the settings opens the month`() {
        var opened = false
        draw(actions = TodayActions(openMonth = { opened = true }))
        compose.onNodeWithContentDescription("Open the month").assertHasClickAction()
        compose.onNodeWithTag(TodayTags.MONTH).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `the time is one door to the clock app, its figures and its face together`() {
        var opened = 0
        draw(actions = TodayActions(openClock = { opened++ }))
        compose.onNodeWithTag(TodayTags.TIME_ROW)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("opens the clock app") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open the clock app"
                },
            )
            // Read once: the time, then what the face shows.
            .assert(hasText("10:20"))
            .assert(hasContentDescription("Clock face at 10:20", substring = true))
            .performClick()
        assertThat(opened).isEqualTo(1)
        compose.assertAccessible()
    }

    @Test
    fun `the time stays a door in the evening, when the face is gone`() {
        var opened = false
        draw(
            TodaySample.state(now = TodaySample.at("2026-10-07T21:10")),
            actions = TodayActions(openClock = { opened = true }),
        )
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.TIME_ROW).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `the time is no door when the reader turned it off, or the phone has no clock app`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(timeOpensClock = false)))
        compose.onNodeWithTag(TodayTags.TIME_ROW).assertHasNoClickAction()
        compose.onNodeWithTag(TodayTags.TIME).assertTextEquals("10:20")
    }

    @Test
    fun `with no clock app, the time is no door`() {
        draw(TodaySample.state(doors = TodaySample.allDoors.copy(clockPackage = null)))
        compose.onNodeWithTag(TodayTags.TIME_ROW).assertHasNoClickAction()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the time's door in Italian`() {
        draw()
        compose.onNodeWithTag(TodayTags.TIME_ROW).assert(
            SemanticsMatcher("apre l'orologio") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Apri l’orologio"
            },
        )
    }

    @Test
    fun `the gap open now has no time of its own, and its new event starts at the next half hour`() {
        var created: Pair<Instant, Instant>? = null
        draw(
            TodaySample.state(now = TodaySample.at("2026-10-07T08:20:41")),
            actions = TodayActions(newEvent = { begin, end -> created = begin to end }),
        )
        val gap = hasContentDescription("Free from now, for 1 h 9 min")
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(gap)
        compose.onNode(gap).performClick()
        assertThat(created).isEqualTo(
            TodaySample.at("2026-10-07T08:30").toInstant() to TodaySample.at("2026-10-07T09:30").toInstant(),
        )
    }

    @Test
    fun `an end on a later date says its day, in the sentence and on the row`() {
        val train = event(20, "2026-10-07T22:30", "2026-10-08T06:30", "Night train")
        val trip = event(21, "2026-10-07T21:00", "2026-10-10T17:00", "Trip to Florence")
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T23:00"), instances = listOf(train, trip)))
        compose.onNodeWithTag(TodayTags.SENTENCE)
            .assertTextEquals("Night train until tomorrow at 06:30, the last today.")
        // The train is on tomorrow's card too ("Started the day before"); today's row comes first.
        compose.onAllNodesWithTag(TodayTags.event(train.eventId)).onFirst()
            .assert(hasContentDescription("Ends the next day", substring = true))
        compose.onNodeWithTag(TodayTags.event(trip.eventId))
            .assert(hasContentDescription("Ends Saturday", substring = true))
    }

    @Test
    fun `an invitation not answered yet says so`() {
        val invite = event(22, "2026-10-07T16:30", "2026-10-07T17:00", "Budget sync")
            .copy(selfStatus = AttendeeStatus.INVITED, isOrganizer = false)
        draw(TodaySample.state(instances = TodaySample.instances + invite))
        scrollTo(TodayTags.event(invite.eventId))
        compose.onNodeWithTag(TodayTags.event(invite.eventId))
            .assert(hasContentDescription("not answered yet", substring = true))
    }

    @Test
    fun `with no calendar app the date is no door, and the month is still Tempo's own`() {
        draw(TodaySample.state(doors = CalendarDoors.None))
        compose.onNodeWithTag(TodayTags.MONTH).assertHasClickAction()
        compose.onNodeWithTag(TodayTags.DATE).assertIsNotEnabled()
    }

    @Test
    fun `a day ahead opens the calendar app on that day`() {
        var day: Instant? = null
        draw(actions = TodayActions(openDay = { day = it }))
        val friday = TodayTags.day(java.time.LocalDate.parse("2026-10-09"))
        scrollTo(friday)
        compose.onNode(hasText("Friday 9 October") and hasClickAction()).performClick()
        assertThat(day).isEqualTo(TodaySample.at("2026-10-09T00:00").toInstant())
    }

    @Test
    fun `the empty days of the week are said once, not a heading each`() {
        draw()
        val run = TodayTags.day(java.time.LocalDate.parse("2026-10-12"))
        scrollTo(run)
        val inRun = hasAnyAncestor(hasTestTag(run))
        compose.onNode(inRun and hasText("From Monday 12 October to Tuesday 13 October")).assertIsDisplayed()
        compose.onNode(inRun and hasText("2 days with nothing planned.")).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.day(java.time.LocalDate.parse("2026-10-13"))).assertDoesNotExist()
        compose.assertAccessible()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian a long run's name keeps to itself, and a later day's all-day events need no label`() {
        draw(dark = true)
        val run = TodayTags.day(java.time.LocalDate.parse("2026-10-12"))
        scrollTo(run)
        compose.onNode(hasAnyAncestor(hasTestTag(run)) and hasText("Da lunedì 12 ottobre a martedì 13 ottobre"))
            .assertIsDisplayed()
        // Said by the day's summary, not beside each one, where it broke in two.
        compose.onAllNodesWithText("Tutto il giorno").assertCountEquals(0)
        compose.onNode(hasContentDescription("Tutto il giorno, Florence, Giorno 2 di 2", substring = true))
            .assertExists()
        compose.assertAccessible()
        compose.writeScreenshot("today-days-italian")
    }

    @Test
    fun `an empty week ahead starts at tomorrow`() {
        draw(TodaySample.state(instances = emptyList()))
        val run = TodayTags.day(java.time.LocalDate.parse("2026-10-08"))
        scrollTo(run)
        compose.onNode(
            hasAnyAncestor(hasTestTag(run)) and hasText("From tomorrow to Tuesday 13 October"),
        ).assertIsDisplayed()
    }

    @Test
    fun `the clock face says what it shows`() {
        draw()
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true)
            .assert(hasContentDescription("Clock face at 10:20, with what is left of today on its ring"))
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
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertIsDisplayed()
        // No face with no calendar to put on its ring: the figures say the time alone.
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertDoesNotExist()
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
    fun `with nothing left today the face is not drawn, and the time stands alone`() {
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T21:00")))
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("today-evening")
    }

    @Test
    fun `early in the day the face stands, its ring empty until the afternoon comes onto it`() {
        // At 02:00 the window closes at 13:30: the 15:00 dentist and the 18:30 swim are beyond it.
        // The face stays, as the day has events left; its ring is empty and says why.
        draw(
            TodaySample.state(
                now = TodaySample.at("2026-10-07T02:00"),
                instances = listOf(TodaySample.dentist, TodaySample.swimming),
            ),
        )
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assert(
            hasContentDescription(
                "Clock face at 02:00; today’s next event is more than eleven hours away and not yet on its ring",
            ),
        )
        compose.assertAccessible()
        compose.writeScreenshot("today-early-empty-ring")
    }

    @Test
    fun `an event cut at the window's end is drawn`() {
        // At 00:55 an event from 10:00 to 22:00 reaches past the window: its arc fades at 12:25.
        val long = TodaySample.dentist.copy(
            begin = TodaySample.at("2026-10-07T10:00").toInstant(),
            end = TodaySample.at("2026-10-07T22:00").toInstant(),
        )
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T00:55"), instances = listOf(long)))
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true)
            .assert(hasContentDescription("Clock face at 00:55, with what is left of today on its ring"))
        compose.writeScreenshot("today-cut-arc")
    }

    @Test
    fun `the sentence comes before the alarm, and the alarm before the day's events`() {
        draw()
        val sentence = compose.onNodeWithTag(TodayTags.SENTENCE).fetchSemanticsNode().boundsInRoot
        val alarm = compose.onNodeWithTag(TodayTags.ALARM).fetchSemanticsNode().boundsInRoot
        val allDay = compose.onNodeWithTag(TodayTags.ALL_DAY).fetchSemanticsNode().boundsInRoot
        assertThat(sentence.bottom).isAtMost(alarm.top)
        assertThat(alarm.bottom).isAtMost(allDay.top)
    }

    @Test
    fun `the new-event button is its icon, and says what it does`() {
        draw()
        compose.onNodeWithText("New event").assertDoesNotExist()
        compose.onNodeWithContentDescription("New event").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the day in Italian`() {
        draw()
        compose.onNodeWithTag(TodayTags.DATE).assertTextEquals("Mercoledì 7 ottobre")
        compose.onNodeWithTag(TodayTags.SENTENCE).assertTextEquals("Design review fino alle 11:00, poi altri 3 oggi.")
    }

    // --- The first day's tip (step 8.6) --------------------------------------------------------

    private val firstDay = TodaySample.settings.copy(todayTipSeen = false)

    @Test
    fun `the first day's tip stands under the hero, before the day's events`() {
        draw(TodaySample.state(settings = firstDay))
        compose.onNodeWithTag(TodayTags.TIP).assertIsDisplayed()
        compose.onNodeWithText("How to read the dial").assertIsDisplayed()
        val tip = compose.onNodeWithTag(TodayTags.TIP).fetchSemanticsNode().boundsInRoot
        val sentence = compose.onNodeWithTag(TodayTags.SENTENCE).fetchSemanticsNode().boundsInRoot
        val allDay = compose.onNodeWithTag(TodayTags.ALL_DAY).fetchSemanticsNode().boundsInRoot
        assertThat(tip.top).isAtLeast(sentence.bottom)
        assertThat(tip.bottom).isAtMost(allDay.top)
        compose.assertAccessible()
        compose.writeScreenshot("today-tip")
    }

    @Test
    fun `the tip, dismissed, is gone`() {
        var settings by mutableStateOf(firstDay)
        compose.setContent {
            TempoTheme {
                TodayScreen(
                    TodaySample.state(settings = settings),
                    CalendarPermission.GRANTED,
                    TodayActions(dismissTip = { settings = settings.copy(todayTipSeen = true) }),
                )
            }
        }
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.ALL_DAY).assertIsDisplayed()
    }

    @Test
    fun `the tip opens the guide`() {
        var guide = false
        draw(TodaySample.state(settings = firstDay), actions = TodayActions(openGuide = { guide = true }))
        compose.onNodeWithText("Open the guide").performClick()
        assertThat(guide).isTrue()
    }

    @Test
    fun `the tip is never beside a state's card`() {
        draw(TodaySample.state(settings = firstDay).copy(content = TodayContent.NoPermission))
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
    }

    @Test
    fun `the tip waits for the dial it explains`() {
        draw(TodaySample.state(now = TodaySample.at("2026-10-07T21:00"), settings = firstDay))
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
    }

    @Test
    fun `the tip waits for an arc on the dial`() {
        draw(
            TodaySample.state(
                now = TodaySample.at("2026-10-07T02:00"),
                instances = listOf(TodaySample.dentist),
                settings = firstDay,
            ),
        )
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
    }

    @Test
    fun `the tip waits for the agenda`() {
        draw(TodaySample.state(settings = firstDay).copy(content = TodayContent.Loading))
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
    }

    @Test
    fun `once seen, the tip is not there again`() {
        draw()
        compose.onNodeWithTag(TodayTags.TIP).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the tip in Italian`() {
        draw(TodaySample.state(settings = firstDay))
        compose.onNodeWithText("Come leggere il quadrante").assertIsDisplayed()
        compose.onNodeWithText("Apri la guida").assertIsDisplayed()
        compose.onNodeWithText("Ho capito").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the tip reads at twice the text size`() {
        draw(TodaySample.state(settings = firstDay))
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-tip-large-text", maxScreens = 2)
    }

    // --- The new event brought into view (step 8.6) -----------------------------------------

    /**
     * Today as it was when the reader left for the calendar app; the function returned brings them
     * back with the event they made on the page.
     */
    private fun leftForNewEvent(reduced: Boolean = false, onRevealed: () -> Unit = {}): (EventInstance) -> Unit {
        var state by mutableStateOf(TodaySample.state())
        compose.setContent {
            TempoTheme {
                CompositionLocalProvider(LocalReducedMotion provides reduced) {
                    TodayScreen(state, CalendarPermission.GRANTED, TodayActions(revealed = onRevealed))
                }
            }
        }
        compose.waitForIdle()
        return { made ->
            state = TodaySample.state(instances = TodaySample.instances + made).copy(reveal = EventKey.of(made))
        }
    }

    @Test
    fun `back from the calendar app, the event made is brought into view and glows once`() {
        var revealed = 0
        val tea = event(30, "2026-10-11T16:00", "2026-10-11T17:00", "Tea with Anna")
        val sunday = TodayTags.day(java.time.LocalDate.parse("2026-10-11"))
        val back = leftForNewEvent(onRevealed = { revealed++ })
        compose.onNodeWithTag(sunday).assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        back(tea)
        // The paused clock runs frames, not the main thread's apply notification: sent by hand.
        Snapshot.sendApplyNotifications()
        // A frame for the new state, then the scroll and the glow at its height.
        compose.mainClock.advanceTimeBy(16)
        compose.mainClock.advanceTimeBy(400)
        compose.writeScreenshot("today-reveal")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag(sunday).assertIsDisplayed()
        compose.onNode(hasContentDescription("Tea with Anna", substring = true)).assertIsDisplayed()
        assertThat(revealed).isEqualTo(1)
    }

    @Test
    fun `an event made in view is not scrolled to`() {
        val coffee = event(31, "2026-10-07T11:00", "2026-10-07T11:30", "Coffee with Marta")
        leftForNewEvent()(coffee)
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(coffee.eventId)).assertIsDisplayed()
    }

    @Test
    fun `an event made this morning unfolds the morning`() {
        val breakfast = event(32, "2026-10-07T08:30", "2026-10-07T09:00", "Breakfast meeting")
        leftForNewEvent()(breakfast)
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.event(breakfast.eventId)).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(TodaySample.run.eventId)).assertExists()
    }

    @Test
    fun `under reduced motion the list jumps to the event made`() {
        val tea = event(30, "2026-10-11T16:00", "2026-10-11T17:00", "Tea with Anna")
        leftForNewEvent(reduced = true)(tea)
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.day(java.time.LocalDate.parse("2026-10-11"))).assertIsDisplayed()
    }

    @Test
    fun `an event synced from elsewhere moves nothing`() {
        var state by mutableStateOf(TodaySample.state())
        compose.setContent { TempoTheme { TodayScreen(state, CalendarPermission.GRANTED, TodayActions()) } }
        val synced = event(33, "2026-10-11T16:00", "2026-10-11T17:00", "Synced")
        state = TodaySample.state(instances = TodaySample.instances + synced)
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertIsDisplayed()
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
    @Config(qualifiers = "en-rGB-w829dp-h690dp-xxhdpi")
    fun `the whole page reads on a foldable open under 840 dp, in its one column`() {
        draw()
        compose.onNodeWithTag(TodayTags.HERO_PANE).assertDoesNotExist()
        compose.onNode(
            hasTestTag(TodayTags.TIME) and hasAnyAncestor(hasTestTag(TodayTags.LIST)),
            useUnmergedTree = true,
        )
            .assertExists()
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-foldable")
    }

    // --- Two grounds: the clock's and the calendar's (step 10.2) --------------------------------

    /** Today drawn, and the scheme it was drawn in. */
    private fun drawn(dark: Boolean, state: TodayUiState = TodaySample.state()): ColorScheme {
        lateinit var scheme: ColorScheme
        compose.setContent {
            TempoTheme(darkTheme = dark) {
                scheme = MaterialTheme.colorScheme
                TodayScreen(state, CalendarPermission.GRANTED, TodayActions())
            }
        }
        compose.waitForIdle()
        return scheme
    }

    /** The colour drawn at a point of the page, in dp from the root's corner. */
    private fun Bitmap.at(x: Float, y: Float): Int {
        val density = compose.density.density
        return getPixel((x * density).toInt(), (y * density).toInt())
    }

    private fun assertGround(what: String, pixel: Int, ground: Color) {
        val want = ground.toArgb()
        val near = listOf(16, 8, 0).all { abs(((pixel shr it) and 0xFF) - ((want shr it) and 0xFF)) <= 2 }
        assertWithMessage("$what is #%08X, not #%08X".format(pixel, want)).that(near).isTrue()
    }

    /** A node's bounds in dp. */
    private fun dp(tag: String): Rect {
        val density = compose.density.density
        val px = bounds(tag)
        return Rect(px.left / density, px.top / density, px.right / density, px.bottom / density)
    }

    private fun twoGrounds(dark: Boolean) {
        val scheme = drawn(dark)
        val page = compose.onRoot().captureToImage().asAndroidBitmap()
        val earlier = dp(TodayTags.EARLIER)
        val now = dp(TodayTags.event(TodaySample.review.eventId))
        val button = dp(TodayTags.NEW_EVENT)
        // The clock's ground from the screen's top corner, square there.
        assertGround("the hero's corner", page.at(1f, 1f), scheme.surfaceContainer)
        assertGround("beside the date", page.at(4f, dp(TodayTags.DATE).center.y), scheme.surfaceContainer)
        // The calendar's is the page.
        assertGround("beside the morning's row", page.at(4f, earlier.center.y), scheme.surface)
        // Now's is the accent's: inside its corner, over its words.
        assertGround("the event under way", page.at(now.left + 20f, now.top + 4f), scheme.primaryContainer)
        // The button's is the scheme's second colour: beside its icon.
        assertGround("the button", page.at(button.left + 8f, button.center.y), scheme.secondaryContainer)
    }

    @Test
    fun `the clock stands on its ground, now on the accent's, and the button wears the second colour`() {
        twoGrounds(dark = false)
        compose.assertAccessible()
    }

    @Test
    fun `the two grounds in the dark`() {
        twoGrounds(dark = true)
        compose.assertAccessible()
    }

    @Test
    fun `the dial's face is the page, on the clock's ground`() {
        val scheme = drawn(dark = false)
        val page = compose.onRoot().captureToImage().asAndroidBitmap()
        val dial = dp(TodayTags.DIAL)
        // Under the centre, half way to the ring: at 10:20 clear of both hands.
        assertGround("the face", page.at(dial.center.x, dial.center.y + dial.height / 4), scheme.surface)
        // The square's corner is outside the disc.
        assertGround("around the face", page.at(dial.left + 1f, dial.top + 1f), scheme.surfaceContainer)
    }

    /**
     * The acceptance of step 10.2: the clock's ground and a day's larger name cost the first
     * screen no event. At 411 × 891 dp, before the step, it held the event under way, the rest of
     * today and tomorrow's first event; it still does.
     */
    @Test
    fun `the first screen holds the events it held before the page had two grounds`() {
        draw()
        val list = bounds(TodayTags.LIST)
        val first = listOf(TodaySample.review, TodaySample.lunch, TodaySample.dentist, TodaySample.swimming) +
            TodaySample.call
        for (event in first) {
            val row = compose.onNodeWithTag(TodayTags.event(event.eventId), useUnmergedTree = true)
                .fetchSemanticsNode()
            // Whole: what is on screen of it is all of it.
            assertWithMessage(event.title).that(row.boundsInRoot.height).isEqualTo(row.size.height.toFloat())
            assertWithMessage(event.title).that(row.boundsInRoot.bottom).isAtMost(list.bottom)
        }
    }

    @Test
    fun `a day ahead stands under its name, a size above its rows, and no line parts the days`() {
        draw()
        val friday = TodayTags.day(java.time.LocalDate.parse("2026-10-09"))
        scrollTo(friday)
        compose.onNode(hasAnyAncestor(hasTestTag(friday)) and hasText("Friday 9 October")).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("today-days-ahead")
    }

    /** A fortnight with one event in its second week: the run after it has the year's longest name. */
    private val longRun: TodayUiState
        get() = TodaySample.state(
            settings = TodaySample.settings.copy(horizonDays = 14),
            instances = TodaySample.instances + event(40, "2026-10-13T18:00", "2026-10-13T19:00", "Cinema"),
        )

    private fun longNameReads(name: String) {
        draw(longRun)
        val run = TodayTags.day(java.time.LocalDate.parse("2026-10-14"))
        scrollTo(run)
        compose.onNode(hasAnyAncestor(hasTestTag(run)) and hasText("Da mercoledì 14 ottobre a martedì 20 ottobre"))
            .assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot(name)
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian a long run's name reads at its larger size on a narrow phone`() {
        longNameReads("today-long-name-narrow")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `in Italian a long run's name reads at twice the text size`() {
        longNameReads("today-long-name-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen the whole hero's pane is the clock's ground, the agenda's the page`() {
        val scheme = drawn(dark = false, TodaySample.state(settings = firstDay))
        val page = compose.onRoot().captureToImage().asAndroidBitmap()
        val hero = dp(TodayTags.HERO_PANE)
        val agenda = dp(TodayTags.LIST)
        val tip = dp(TodayTags.TIP)
        assertGround("the pane's top", page.at(hero.left + 1f, hero.top + 1f), scheme.surfaceContainer)
        assertGround("the pane's foot", page.at(hero.right - 2f, hero.bottom - 2f), scheme.surfaceContainer)
        assertGround("the agenda's pane", page.at(agenda.left + 2f, agenda.bottom - 2f), scheme.surface)
        // A note on the clock's ground is drawn on the page's surface, a step from its own it was lost.
        assertGround("the tip", page.at(tip.left + 20f, tip.top + 4f), scheme.surface)
    }

    // --- The compact bar (step 10.3) ------------------------------------------------------------

    /** Far enough down the page that the hero has left it. */
    private val sunday = TodayTags.day(java.time.LocalDate.parse("2026-10-11"))

    /** The page moved by [px] under a finger, down the list or, below zero, back up. */
    private fun scrollBy(px: Float) {
        compose.onNodeWithTag(TodayTags.LIST).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        compose.waitForIdle()
    }

    @Test
    fun `once the hero has gone up, a bar keeps the date, the time and the page's two doors`() {
        draw()
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.BAR_DATE).assertTextEquals("Wed, 7 Oct").assertHasClickAction()
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:20").assertHasClickAction()
        compose.onNodeWithTag(TodayTags.BAR_MONTH).assert(hasContentDescription("Open the month"))
        compose.onNodeWithTag(TodayTags.BAR_SETTINGS).assert(hasContentDescription("Settings"))
        // One line, as high as its buttons, at the screen's top.
        val bar = dp(TodayTags.BAR)
        assertThat(bar.top).isEqualTo(0f)
        assertThat(bar.height).isEqualTo(48f)
        compose.assertAccessible()
        compose.writeScreenshot("today-bar")
        // Back at the top the hero says it all, and the bar is gone.
        scrollTo(TodayTags.DATE)
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.DATE).assertIsDisplayed()
    }

    @Test
    fun `the bar comes as the hero's foot goes up under its place, and goes as it comes back`() {
        draw()
        val density = compose.density.density
        // The hero's ground ends 4 dp over the list's next item; the bar's place is the top 48 dp.
        val foot = bounds(TodayTags.ALL_DAY).top - 4 * density
        val under = foot - 48 * density
        scrollBy(under - 2 * density)
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
        scrollBy(4 * density)
        compose.onNodeWithTag(TodayTags.BAR).assertIsDisplayed()
        // No row of the agenda was under it as it came: the first one starts below it.
        assertThat(bounds(TodayTags.ALL_DAY).top).isAtLeast(bounds(TodayTags.BAR).bottom)
        scrollBy(-4 * density)
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
    }

    @Test
    fun `the bar's date and time are the hero's doors, and its buttons the page's own`() {
        var day: Instant? = null
        var clock = 0
        var month = 0
        var settings = 0
        draw(
            actions = TodayActions(
                openDay = { day = it },
                openClock = { clock++ },
                openMonth = { month++ },
                openSettings = { settings++ },
            ),
        )
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_DATE).performClick()
        assertThat(day).isEqualTo(TodaySample.at("2026-10-07T10:20").toInstant())
        compose.onNodeWithTag(TodayTags.BAR_TIME)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("opens the clock app") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open the clock app"
                },
            )
            .performClick()
        assertThat(clock).isEqualTo(1)
        compose.onNodeWithTag(TodayTags.BAR_MONTH).performClick()
        assertThat(month).isEqualTo(1)
        compose.onNodeWithTag(TodayTags.BAR_SETTINGS).performClick()
        assertThat(settings).isEqualTo(1)
    }

    @Test
    fun `a touch on the bar never reaches the row under it`() {
        var opened: EventInstance? = null
        var day: Instant? = null
        // No clock app: the time is plain words, with a row of the agenda under them.
        draw(
            TodaySample.state(doors = TodaySample.allDoors.copy(clockPackage = null)),
            actions = TodayActions(openEvent = { opened = it }, openDay = { day = it }),
        )
        // The event under way brought half under the bar, the bar's middle over it.
        val review = TodayTags.event(TodaySample.review.eventId)
        scrollBy(bounds(review).top + 10 * compose.density.density)
        val bar = bounds(TodayTags.BAR_TIME)
        val row = bounds(review)
        assertThat(row.top).isLessThan(bar.center.y)
        assertThat(row.bottom).isGreaterThan(bar.center.y)
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertHasNoClickAction().performClick()
        assertThat(opened).isNull()
        assertThat(day).isNull()
    }

    @Test
    fun `with no calendar app and no clock app the bar says the date and the time, and opens nothing`() {
        draw(TodaySample.state(doors = CalendarDoors.None))
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_DATE).assertTextEquals("Wed, 7 Oct").assertIsNotEnabled()
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:20").assertHasNoClickAction()
        compose.onNodeWithTag(TodayTags.BAR_MONTH).assertHasClickAction()
        compose.assertAccessible()
        compose.writeScreenshot("today-bar-no-doors")
    }

    @Test
    fun `the bar's time is no door where the reader turned the hero's off`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(timeOpensClock = false)))
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:20").assertHasNoClickAction()
    }

    @Test
    fun `the bar's time moves with the page's minute`() {
        var state by mutableStateOf(TodaySample.state())
        compose.setContent { TempoTheme { TodayScreen(state, CalendarPermission.GRANTED, TodayActions()) } }
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:20")
        state = TodaySample.state(now = TodaySample.at("2026-10-07T10:21"))
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:21")
    }

    @Test
    fun `the bar says the date in the reader's style, a long one in its short names`() {
        val twelve = TodaySample.settings.copy(dateStyle = DateStyle.NUMERIC, clockFormat = ClockFormat.H12)
        draw(TodaySample.state(settings = twelve))
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_DATE).assertTextEquals("07/10/2026")
        compose.onNodeWithTag(TodayTags.BAR_TIME).assertTextEquals("10:20 am")
        compose.assertAccessible()
        compose.writeScreenshot("today-bar-twelve-hour")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the bar in Italian`() {
        draw()
        scrollTo(sunday)
        compose.onNodeWithTag(TodayTags.BAR_DATE).assertTextEquals("Mer 7 ott")
        compose.onNodeWithTag(TodayTags.BAR_TIME).assert(
            SemanticsMatcher("apre l'orologio") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Apri l’orologio"
            },
        )
        compose.onNodeWithTag(TodayTags.BAR_MONTH).assert(hasContentDescription("Apri il mese"))
    }

    private fun barGround(dark: Boolean) {
        val scheme = drawn(dark)
        scrollTo(sunday)
        val page = compose.onRoot().captureToImage().asAndroidBitmap()
        val bar = dp(TodayTags.BAR)
        // Between the date and the time, and at the screen's top corner, square as the hero's.
        assertGround("the bar", page.at(dp(TodayTags.BAR_TIME).left - 4f, bar.center.y), scheme.surfaceContainer)
        assertGround("the bar's corner", page.at(1f, 1f), scheme.surfaceContainer)
        // The page under it: the agenda's ground, as before the scroll.
        assertGround("under the bar", page.at(4f, bar.bottom + 60f), scheme.surface)
    }

    @Test
    fun `the bar stands on the clock's ground`() {
        barGround(dark = false)
    }

    @Test
    fun `the bar's ground in the dark`() {
        barGround(dark = true)
        compose.writeScreenshot("today-bar-dark")
    }

    @Test
    fun `a screen reader hears the bar before the list`() {
        draw()
        scrollTo(sunday)
        // Its own group, ahead of everything else on the page whatever stands as high.
        val bar = compose.onNode(hasTestTag(COVER_TAG)).fetchSemanticsNode().config
        assertThat(bar.getOrNull(SemanticsProperties.IsTraversalGroup)).isTrue()
        assertThat(bar.getOrNull(SemanticsProperties.TraversalIndex)).isEqualTo(-1f)
        val list = compose.onNodeWithTag(TodayTags.LIST).fetchSemanticsNode().config
        assertThat(list.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f).isGreaterThan(-1f)
    }

    @Test
    fun `the event made is brought under the bar, never behind it`() {
        // A fortnight with a week of events in it, so the list has the room to bring tomorrow up.
        val fortnight = TodaySample.settings.copy(horizonDays = 14)
        val week = (12..16).map { event(40L + it, "2026-10-${it}T10:00", "2026-10-${it}T11:00", "Workshop") }
        val visit = event(35, "2026-10-08T14:00", "2026-10-08T15:00", "Visit to the studio")
        val thursday = TodayTags.day(java.time.LocalDate.parse("2026-10-08"))
        var state by mutableStateOf(TodaySample.state(settings = fortnight, instances = TodaySample.instances + week))
        compose.setContent { TempoTheme { TodayScreen(state, CalendarPermission.GRANTED, TodayActions()) } }
        compose.waitForIdle()
        state = TodaySample.state(settings = fortnight, instances = TodaySample.instances + week + visit)
            .copy(reveal = EventKey.of(visit))
        compose.waitForIdle()
        compose.onNodeWithTag(TodayTags.BAR).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.event(visit.eventId)).assertIsDisplayed()
        // Tomorrow a margin under the bar, with the air a day's name keeps over it: the bar's
        // height is counted, or the name would stand behind the bar.
        val density = compose.density.density
        val under = bounds(thursday).top - bounds(TodayTags.BAR).bottom
        assertThat(under).isAtLeast(24 * density - 1)
        assertThat(under).isAtMost(29 * density)
    }

    /** What the bar's ground is drawn as 80 ms after the scroll that brings it, and the scheme's. */
    private fun barSoonAfter(reduced: Boolean): Pair<Int, ColorScheme> {
        lateinit var scheme: ColorScheme
        compose.setContent {
            TempoTheme {
                scheme = MaterialTheme.colorScheme
                CompositionLocalProvider(LocalReducedMotion provides reduced) {
                    TodayScreen(TodaySample.state(), CalendarPermission.GRANTED, TodayActions())
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        // By its index, in one call: with the clock paused, a scroll that looks for a node waits
        // for a layout no frame will make. Any item past the first rows has the hero gone.
        compose.onNodeWithTag(TodayTags.LIST).performSemanticsAction(SemanticsActions.ScrollToIndex) { it(6) }
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(80)
        val page = compose.onRoot().captureToImage().asAndroidBitmap()
        val pixel = page.at(dp(TodayTags.BAR_TIME).left - 4f, dp(TodayTags.BAR).center.y)
        compose.mainClock.autoAdvance = true
        return pixel to scheme
    }

    @Test
    fun `under reduced motion the bar is there at once, with no fade`() {
        val (pixel, scheme) = barSoonAfter(reduced = true)
        assertGround("the bar, at once", pixel, scheme.surfaceContainer)
    }

    @Test
    fun `the bar fades in`() {
        val (pixel, scheme) = barSoonAfter(reduced = false)
        // Still nearer the page under it than its own ground.
        assertThat(pixel).isNotEqualTo(scheme.surfaceContainer.toArgb())
    }

    // --- Today on a large screen: two panes (step 8.7) ------------------------------------------

    private val inHeroPane = hasAnyAncestor(hasTestTag(TodayTags.HERO_PANE))
    private val inAgendaPane = hasAnyAncestor(hasTestTag(TodayTags.LIST))

    private fun bounds(tag: String) =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    /** Both panes walked, each from its top to its bottom. */
    private fun walkPanes(name: String) {
        compose.walkPage(hasTestTag(TodayTags.HERO_PANE), "$name-hero")
        compose.walkPage(hasTestTag(TodayTags.LIST), "$name-agenda")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `a tablet on its side, the hero beside the agenda and the dial at its largest`() {
        draw()
        compose.onNodeWithTag(TodayTags.HERO_PANE).assertWidthIsEqualTo(512.dp)
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertWidthIsEqualTo(200.dp)
        // The clock is never shrunk for the face.
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20")
        for (tag in listOf(TodayTags.DATE, TodayTags.TIME, TodayTags.DIAL, TodayTags.ALARM, TodayTags.SENTENCE)) {
            compose.onNode(hasTestTag(tag) and inHeroPane, useUnmergedTree = true).assertIsDisplayed()
        }
        compose.onNode(hasTestTag(TodayTags.ALL_DAY) and inHeroPane).assertIsDisplayed()
        compose.onNode(hasTestTag(TodayTags.ALL_DAY) and inAgendaPane).assertDoesNotExist()
        compose.onNode(hasTestTag(TodayTags.event(TodaySample.review.eventId)) and inAgendaPane).assertIsDisplayed()
        // Side by side, the agenda's first row level with the date's, and the button over the agenda.
        val hero = bounds(TodayTags.HERO_PANE)
        val agenda = bounds(TodayTags.LIST)
        assertThat(agenda.left).isAtLeast(hero.right)
        assertThat(bounds(TodayTags.EARLIER).top).isEqualTo(bounds(TodayTags.DATE).top)
        assertThat(bounds(TodayTags.NEW_EVENT).left).isAtLeast(agenda.left)
        walkPanes("today-tablet")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `the two panes scroll each on its own`() {
        draw()
        val time = bounds(TodayTags.TIME)
        compose.onNodeWithTag(TodayTags.EARLIER).assertIsDisplayed()
        scrollTo(TodayTags.day(java.time.LocalDate.parse("2026-10-11")))
        // The morning's row has gone up out of the agenda's pane; the clock has not moved.
        assertThat(compose.onAllNodesWithTag(TodayTags.EARLIER).fetchSemanticsNodes().none { it.layoutInfo.isPlaced })
            .isTrue()
        assertThat(bounds(TodayTags.TIME)).isEqualTo(time)
        // And no bar says it twice: the compact bar is the one column's.
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-rGB-w900dp-h1200dp-xhdpi")
    fun `a large tablet upright in two panes, the hero a phone's width at least`() {
        draw()
        compose.onNodeWithTag(TodayTags.HERO_PANE).assertWidthIsEqualTo(400.dp)
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20")
        val dial = bounds(TodayTags.DIAL).width / compose.density.density
        assertThat(dial).isAtLeast(112f)
        assertThat(dial).isLessThan(200f)
        walkPanes("today-tablet-upright")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `the whole page reads on an open foldable 840 dp wide, in two panes`() {
        draw()
        compose.onNodeWithTag(TodayTags.HERO_PANE).assertWidthIsEqualTo(400.dp)
        walkPanes("today-foldable-panes")
    }

    @Test
    @Config(qualifiers = "en-rGB-w891dp-h411dp-xxhdpi")
    fun `a phone on its side keeps its one column`() {
        draw()
        compose.onNodeWithTag(TodayTags.HERO_PANE).assertDoesNotExist()
        compose.onNodeWithTag(TodayTags.DIAL, useUnmergedTree = true).assertWidthIsEqualTo(112.dp)
        compose.walkPage(hasTestTag(TodayTags.LIST), "today-phone-sideways", maxScreens = 3)
        // Down the page the bar is there, and one line of it: on a screen 411 dp high it must
        // not take the page.
        compose.onNodeWithTag(TodayTags.BAR).assertIsDisplayed()
        assertThat(dp(TodayTags.BAR).height).isEqualTo(48f)
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi", fontScale = 2f)
    fun `two panes at twice the text size`() {
        draw()
        walkPanes("today-tablet-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `two panes in the dark, on the twelve-hour clock`() {
        draw(TodaySample.state(settings = TodaySample.settings.copy(clockFormat = ClockFormat.H12)), dark = true)
        compose.onNodeWithTag(TodayTags.TIME, useUnmergedTree = true).assertTextEquals("10:20\u202Fam")
        compose.assertAccessible()
        compose.writeScreenshot("today-tablet-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen the first day's tip stands in the hero's pane`() {
        draw(TodaySample.state(settings = firstDay))
        compose.onNode(hasTestTag(TodayTags.TIP) and inHeroPane).assertIsDisplayed()
        assertThat(bounds(TodayTags.TIP).top).isAtLeast(bounds(TodayTags.ALL_DAY).bottom)
        compose.assertAccessible()
        compose.writeScreenshot("today-tablet-tip")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen a state's card stands in the agenda's pane`() {
        var asked = false
        draw(
            state = TodaySample.state().copy(content = TodayContent.NoPermission),
            permission = CalendarPermission.ASKABLE,
            actions = TodayActions(askPermission = { asked = true }),
        )
        compose.onNode(hasTestTag(TodayTags.TIME) and inHeroPane, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasTestTag(TodayTags.DIAL), useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("Allow") and inAgendaPane).performClick()
        assertThat(asked).isTrue()
        compose.assertAccessible()
        compose.writeScreenshot("today-tablet-permission")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen the event made is brought into view in the agenda's pane`() {
        var revealed = 0
        val tea = event(30, "2026-10-11T16:00", "2026-10-11T17:00", "Tea with Anna")
        val sunday = TodayTags.day(java.time.LocalDate.parse("2026-10-11"))
        val back = leftForNewEvent(onRevealed = { revealed++ })
        compose.onNodeWithTag(sunday).assertDoesNotExist()
        val time = bounds(TodayTags.TIME)
        back(tea)
        compose.waitForIdle()
        compose.onNode(hasTestTag(sunday) and inAgendaPane).assertIsDisplayed()
        // Whole, above the button's room, and the hero where it was.
        assertThat(bounds(sunday).bottom).isAtMost(bounds(TodayTags.LIST).bottom)
        assertThat(bounds(TodayTags.TIME)).isEqualTo(time)
        assertThat(revealed).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen an all-day event made today glows in the hero's pane`() {
        val holiday = TodaySample.birthday.copy(eventId = 34, title = "Day off")
        val back = leftForNewEvent()
        compose.mainClock.autoAdvance = false
        back(holiday)
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(16)
        compose.mainClock.advanceTimeBy(400)
        compose.writeScreenshot("today-tablet-reveal")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNode(hasContentDescription("Day off", substring = true) and inHeroPane).assertIsDisplayed()
    }

    /** An event of the Personal calendar, from and to local times in Rome. */
    private fun event(id: Long, from: String, to: String, title: String) = EventInstance(
        eventId = id,
        calendarId = TodaySample.personal.id,
        title = title,
        location = null,
        begin = TodaySample.at(from).toInstant(),
        end = TodaySample.at(to).toInstant(),
        allDay = false,
        color = null,
    )
}
