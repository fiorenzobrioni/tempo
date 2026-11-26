package com.callbackdev.tempo.feature.month

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.calendar.CalendarDoors
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.NewEvent
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.EventInstance
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
import java.time.YearMonth

/** The month drawn from states (PLANNING.md §11 Phase 9: UI tests on states, not databases). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class MonthScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun draw(
        state: MonthUiState = MonthSample.state(),
        permission: CalendarPermission = CalendarPermission.GRANTED,
        actions: MonthActions = MonthActions(),
        dark: Boolean = false,
    ) {
        compose.setContent { TempoTheme(darkTheme = dark) { MonthScreen(state, permission, actions) } }
    }

    private fun day(iso: String) = compose.onNodeWithTag(MonthTags.day(MonthSample.date(iso)))

    private fun description(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text))

    @Test
    fun `the month, its name, and today chosen, with the day under it`() {
        draw()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("November 2026")).assertHasClickAction()
        day("2026-11-12").assertIsSelected().assert(description("Thursday 12 November, today, 7 events"))
        compose.onNode(hasText("Thursday 12 November") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithText("5 events, 09:30 to 19:30.").assertIsDisplayed()
        compose.onNodeWithTag(MonthTags.event(MonthSample.review.eventId))
            .assert(hasContentDescription("under way, 40 min left", substring = true))
        // "Today" is a way back: not drawn while the page is there.
        compose.onNodeWithTag(MonthTags.TODAY).assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("month")
    }

    @Test
    fun `a date says what it holds to a screen reader, since its marks say nothing aloud`() {
        draw()
        day("2026-11-02").assert(description("Monday 2 November, 2 events"))
        day("2026-11-03").assert(description("Tuesday 3 November, nothing planned"))
        day("2026-11-13").assert(description("Friday 13 November, 1 event"))
        // A span of days is counted on each of its dates.
        day("2026-11-21").assert(description("Saturday 21 November, 1 event"))
    }

    @Test
    fun `a touch on a date chooses it and lists its events`() {
        draw()
        day("2026-11-20").performClick()
        day("2026-11-20").assertIsSelected()
        compose.onNode(hasText("Friday 20 November") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithTag(MonthTags.ALL_DAY).assert(hasAnyDescendantDescription("All day, Florence, Day 1 of 3"))
        compose.onNodeWithTag(MonthTags.event(MonthSample.train.eventId)).assertIsDisplayed()
        // The day left is gone from under the month.
        compose.onNodeWithTag(MonthTags.event(MonthSample.review.eventId)).assertDoesNotExist()
        compose.onNodeWithTag(MonthTags.TODAY).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("month-day-chosen")
    }

    @Test
    fun `an empty day says so, and nothing else`() {
        draw()
        day("2026-11-03").performClick()
        compose.onNodeWithText("Tuesday 3 November").assertIsDisplayed()
        compose.onNodeWithText("Nothing planned.").assertIsDisplayed()
        compose.writeScreenshot("month-empty-day")
    }

    @Test
    fun `an event and the day's name are doors to the calendar app`() {
        var opened: EventInstance? = null
        var day: Instant? = null
        draw(actions = MonthActions(openEvent = { opened = it }, openDay = { day = it }))
        compose.onNodeWithTag(MonthTags.event(MonthSample.lunch.eventId)).performClick()
        assertThat(opened).isEqualTo(MonthSample.lunch)
        compose.onNode(hasText("Thursday 12 November") and hasClickAction()).performClick()
        assertThat(day).isEqualTo(MonthSample.at("2026-11-12T00:00").toInstant())
    }

    @Test
    fun `the button starts a new event on the chosen day`() {
        var created: Pair<Instant, Instant>? = null
        draw(actions = MonthActions(newEvent = { begin, end -> created = begin to end }))
        compose.onNodeWithContentDescription("New event on Thursday 12 November").performClick()
        // Today: the next half hour, as Today's own button.
        assertThat(created).isEqualTo(
            MonthSample.at("2026-11-12T10:30").toInstant() to MonthSample.at("2026-11-12T11:30").toInstant(),
        )
        day("2026-11-26").performClick()
        compose.onNodeWithContentDescription("New event on Thursday 26 November").performClick()
        // Another day: the same time of day.
        assertThat(created?.first).isEqualTo(MonthSample.at("2026-11-26T10:30").toInstant())
    }

    @Test
    fun `a long touch on a date starts a new event on it, and chooses it`() {
        var created: Instant? = null
        draw(actions = MonthActions(newEvent = { begin, _ -> created = begin }))
        day("2026-11-24").performTouchInput { longClick() }
        assertThat(created).isEqualTo(MonthSample.at("2026-11-24T10:30").toInstant())
        day("2026-11-24").assertIsSelected()
    }

    @Test
    fun `the arrows turn the months, and the month turned to is read`() {
        val read = mutableListOf<YearMonth>()
        draw(actions = MonthActions(showMonth = { read += it }))
        compose.onNodeWithTag(MonthTags.NEXT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("December 2026"))
        assertThat(read).containsExactly(YearMonth.of(2026, 11), YearMonth.of(2026, 12)).inOrder()
        // The day under the month is one of its own: the first.
        day("2026-12-01").assertIsSelected()
        compose.onNodeWithText("Tuesday 1 December").assertIsDisplayed()
        compose.onNodeWithTag(MonthTags.PREVIOUS).performClick()
        compose.onNodeWithTag(MonthTags.PREVIOUS).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("October 2026"))
    }

    @Test
    fun `Today brings the page back, and goes`() {
        draw()
        compose.onNodeWithTag(MonthTags.NEXT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.TODAY).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("November 2026"))
        day("2026-11-12").assertIsSelected()
        compose.onNodeWithTag(MonthTags.TODAY).assertDoesNotExist()
    }

    @Test
    fun `a date of the month beside turns the page to its month`() {
        draw()
        // November's last week runs to Sunday 6 December.
        compose.onNodeWithTag(MonthTags.beside(MonthSample.date("2026-12-03"))).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("December 2026"))
        day("2026-12-03").assertIsSelected()
        compose.onNodeWithText("Thursday 3 December").assertIsDisplayed()
    }

    @Test
    fun `the month's name opens the way to any month`() {
        val read = mutableListOf<YearMonth>()
        draw(actions = MonthActions(showMonth = { read += it }))
        compose.onNodeWithTag(MonthTags.TITLE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.JUMP_YEAR).assertTextEquals("2026")
        compose.onNodeWithTag(MonthTags.jump(11)).assertIsSelected().assert(description("November 2026"))
        compose.assertAccessible()
        compose.writeScreenshot("month-jump")
        compose.onNodeWithContentDescription("Next year").performClick()
        compose.onNodeWithContentDescription("Next year").performClick()
        compose.onNodeWithTag(MonthTags.JUMP_YEAR).assertTextEquals("2028")
        compose.onNodeWithTag(MonthTags.jump(3)).assert(description("March 2028")).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(MonthTags.JUMP).assertDoesNotExist()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("March 2028"))
        assertThat(read.last()).isEqualTo(YearMonth.of(2028, 3))
        day("2028-03-01").assertIsSelected()
    }

    @Test
    fun `back leaves the page`() {
        var left = false
        draw(actions = MonthActions(back = { left = true }))
        compose.onNodeWithContentDescription("Back").performClick()
        assertThat(left).isTrue()
    }

    @Test
    fun `the event the reader made is brought forward, its date chosen`() {
        val made = MonthSample.timed(90, "2026-11-24T16:00", "2026-11-24T17:00", "Haircut", MonthSample.personal)
        var revealed = false
        val state = MonthSample.state(instances = MonthSample.instances + made)
            .copy(reveal = NewEvent(EventKey.of(made), MonthSample.date("2026-11-24")))
        draw(state, actions = MonthActions(revealed = { revealed = true }))
        compose.waitForIdle()
        assertThat(revealed).isTrue()
        day("2026-11-24").assertIsSelected()
        compose.onNodeWithTag(MonthTags.event(made.eventId)).assertIsDisplayed()
    }

    @Test
    fun `without the permission the month is drawn all the same, and the day's place asks`() {
        var asked = false
        draw(
            state = MonthSample.state().copy(content = MonthContent.NoPermission),
            permission = CalendarPermission.ASKABLE,
            actions = MonthActions(askPermission = { asked = true }),
        )
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("November 2026"))
        day("2026-11-12").assert(description("Thursday 12 November, today"))
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
        compose.assertAccessible()
        compose.writeScreenshot("month-no-permission")
    }

    @Test
    fun `every calendar hidden sends the reader to choose them`() {
        var settings = false
        draw(
            state = MonthSample.state().copy(content = MonthContent.AllHidden),
            actions = MonthActions(openSettings = { settings = true }),
        )
        compose.onNodeWithText("Choose calendars").performClick()
        assertThat(settings).isTrue()
    }

    @Test
    fun `with no calendar app nothing is a door, and the page says why`() {
        draw(MonthSample.state(doors = CalendarDoors.None))
        compose.onNodeWithTag(MonthTags.NEW_EVENT).assertDoesNotExist()
        compose.onNodeWithTag(MonthTags.event(MonthSample.lunch.eventId)).assertHasNoClickAction()
        compose.onNodeWithTag(MonthTags.LIST).performScrollToNode(hasText("No calendar app on this phone"))
        compose.onNodeWithText("No calendar app on this phone").assertIsDisplayed()
        // A date is still chosen with a touch: that is the page's own.
        day("2026-11-20").assertHasClickAction()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun `the week starts where the reader's does`() {
        draw()
        // From Sunday, November 2026 begins its own first row: no date of October is on the page.
        compose.onNodeWithTag(MonthTags.beside(MonthSample.date("2026-10-31"))).assertDoesNotExist()
        day("2026-11-01").assertIsDisplayed()
        compose.writeScreenshot("month-sunday-first")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the month in Italian`() {
        draw()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("Novembre 2026"))
        day("2026-11-12").assert(description("Giovedì 12 novembre, oggi, 7 impegni"))
        compose.onNodeWithText("5 impegni, dalle 09:30 alle 19:30.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Nuovo evento giovedì 12 novembre").assertIsDisplayed()
        day("2026-11-03").performClick()
        compose.onNodeWithTag(MonthTags.TODAY).assert(hasContentDescription("Oggi"))
        compose.writeScreenshot("month-italian")
    }

    @Test
    fun `away from today the month's name is whole, beside the way back`() {
        awayInSeptember()
        assertWhole("September 2026")
        compose.onNodeWithTag(MonthTags.TODAY).assert(hasContentDescription("Today")).assertHasClickAction()
        compose.onNodeWithTag(MonthTags.PREVIOUS).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("month-away")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi")
    fun `the month's name is whole on a narrow phone`() {
        awayInSeptember()
        assertWhole("September 2026")
        compose.onNodeWithTag(MonthTags.TODAY).assertIsDisplayed()
        compose.assertAccessible()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the month's name is whole in Italian`() {
        awayInSeptember()
        assertWhole("Settembre 2026")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `the month's name is whole in Italian on a narrow phone`() {
        awayInSeptember()
        assertWhole("Settembre 2026")
    }

    /** The year's longest month, with a day other than today chosen: the way back is drawn. */
    private fun awayInSeptember() {
        val september = YearMonth.of(2026, 9)
        draw(MonthSample.state(now = MonthSample.at("2026-09-10T10:20"), shown = september))
        day("2026-09-03").performClick()
    }

    /** [name] is laid out on one line with nothing cut: no ellipsis, its last letter within the room it was given. */
    private fun assertWhole(name: String) {
        val node = compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag(MonthTags.TITLE)), useUnmergedTree = true)
            .fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isLineEllipsized(0)).isFalse()
        assertThat(layout.getLineRight(0)).isAtMost(layout.layoutInput.constraints.maxWidth.toFloat())
    }

    @Test
    fun `the reader's clock reaches the day's rows`() {
        draw(MonthSample.state(settings = MonthSample.settings.copy(clockFormat = ClockFormat.H12)))
        compose.onNodeWithTag(MonthTags.event(MonthSample.dentist.eventId))
            .assert(hasContentDescription("3:00 pm to 3:45 pm", substring = true))
    }

    @Test
    fun `the month in the dark`() {
        draw(dark = true)
        compose.assertAccessible()
        compose.writeScreenshot("month-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the whole page reads at twice the text size`() {
        draw()
        compose.onNodeWithTag(MonthTags.TITLE).assert(hasText("November 2026"))
        compose.walkPage(hasTestTag(MonthTags.LIST), "month-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w829dp-h690dp-xxhdpi")
    fun `the whole page reads on a foldable open under 840 dp, in its one column`() {
        draw()
        compose.onNodeWithTag(MonthTags.MONTH_PANE).assertDoesNotExist()
        compose.walkPage(hasTestTag(MonthTags.LIST), "month-foldable")
    }

    @Test
    @Config(qualifiers = "en-rGB-w891dp-h411dp-xxhdpi")
    fun `a phone on its side keeps the one column`() {
        draw()
        compose.onNodeWithTag(MonthTags.MONTH_PANE).assertDoesNotExist()
        compose.walkPage(hasTestTag(MonthTags.LIST), "month-phone-sideways")
    }

    @Test
    @Config(qualifiers = "en-rGB-w1280dp-h800dp-xhdpi")
    fun `on a large screen the month stands beside the day`() {
        draw()
        val inMonthPane = hasAnyAncestor(hasTestTag(MonthTags.MONTH_PANE))
        val inDayPane = hasAnyAncestor(hasTestTag(MonthTags.LIST))
        compose.onNode(hasTestTag(MonthTags.TITLE) and inMonthPane).assertIsDisplayed()
        compose.onNode(hasTestTag(MonthTags.day(MonthSample.date("2026-11-12"))) and inMonthPane).assertIsDisplayed()
        compose.onNode(hasTestTag(MonthTags.DAY) and inDayPane).assertIsDisplayed()
        compose.onNode(hasTestTag(MonthTags.event(MonthSample.review.eventId)) and inDayPane).assertIsDisplayed()
        day("2026-11-20").performClick()
        compose.onNode(hasTestTag(MonthTags.event(MonthSample.train.eventId)) and inDayPane).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("month-tablet")
    }

    private fun hasAnyDescendantDescription(text: String) = SemanticsMatcher("has a descendant read as «$text»") {
        fun reads(node: SemanticsNode): Boolean =
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { text in it } == true ||
                node.children.any(::reads)
        reads(it)
    }
}
