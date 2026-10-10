package com.callbackdev.tempo.feature.onboarding

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.callbackdev.tempo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The first run, drawn from its step and the permission's state (PLANNING.md §11 Phase 3). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun draw(
        step: OnboardingStep,
        permission: CalendarPermission,
        actions: OnboardingActions = OnboardingActions(),
        widgets: WidgetOffer = WidgetOffer(),
    ) {
        compose.setContent { TempoTheme { OnboardingScreen(step, permission, actions, widgets = widgets) } }
    }

    @Test
    fun `the welcome says what Tempo is and moves on`() {
        var next = false
        draw(OnboardingStep.WELCOME, CalendarPermission.ASKABLE, OnboardingActions(next = { next = true }))
        compose.onNodeWithText("The time, and what comes next.").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.MARK).assertIsDisplayed()
        compose.assertAccessible()
        compose.onNodeWithTag(OnboardingTags.PRIMARY).performClick()
        assertThat(next).isTrue()
    }

    @Test
    fun `the welcome says where the run stands, as the other pages do`() {
        draw(OnboardingStep.WELCOME, CalendarPermission.ASKABLE)
        compose.onNodeWithContentDescription("Step 1 of 3").assertIsDisplayed()
    }

    // A phone upright as a reader holds it: 384 x 832 dp (a Galaxy S24's), less its status bar and
    // gesture line, which Robolectric does not draw. In Italian, the longer of the two languages.
    @Test
    @Config(qualifiers = "it-w384dp-h770dp-xxhdpi")
    fun `the welcome fits a phone without scrolling`() {
        draw(OnboardingStep.WELCOME, CalendarPermission.ASKABLE)
        compose.waitForIdle()
        compose.writeScreenshot("onboarding-welcome-it")
        val page = compose.onNodeWithTag(OnboardingTags.PAGE).fetchSemanticsNode()
        val range = page.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        assertThat(range?.maxValue?.invoke() ?: 0f).isEqualTo(0f)
    }

    @Test
    @Config(qualifiers = "en-rGB-w384dp-h770dp-night-xxhdpi")
    fun `the welcome's mark stands on the dark page`() {
        compose.setContent {
            TempoTheme(darkTheme = true) {
                OnboardingScreen(OnboardingStep.WELCOME, CalendarPermission.ASKABLE, OnboardingActions())
            }
        }
        compose.waitForIdle()
        compose.writeScreenshot("onboarding-welcome-dark")
    }

    @Test
    fun `the calendar's page asks, and lets the reader through without it`() {
        var asked = false
        var next = false
        draw(
            OnboardingStep.CALENDAR,
            CalendarPermission.ASKABLE,
            OnboardingActions(ask = { asked = true }, next = { next = true }),
        )
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
        compose.onNodeWithTag(OnboardingTags.NOT_NOW).performClick()
        assertThat(next).isTrue()
        compose.assertAccessible()
    }

    @Test
    fun `refused for good, the system's page instead of a question that would not show`() {
        var opened = false
        draw(
            OnboardingStep.CALENDAR,
            CalendarPermission.DENIED_FOR_GOOD,
            OnboardingActions(openAppSettings = {
                opened =
                    true
            }),
        )
        compose.onNodeWithText("Open the app’s settings").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `granted, it says so and moves on`() {
        var next = false
        draw(OnboardingStep.CALENDAR, CalendarPermission.GRANTED, OnboardingActions(next = { next = true }))
        compose.onNodeWithTag(OnboardingTags.GRANTED).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.NOT_NOW).assertDoesNotExist()
        compose.onNodeWithText("Next").performClick()
        assertThat(next).isTrue()
    }

    @Test
    fun `the widget's page offers the pair to the launcher, and finishes`() {
        var pinned: TempoWidget? = null
        var finished = false
        draw(
            OnboardingStep.WIDGET,
            CalendarPermission.GRANTED,
            OnboardingActions(pin = { pinned = it }, finish = { finished = true }),
            WidgetOffer(canPin = true),
        )
        compose.onNodeWithTag(OnboardingTags.pin(TempoWidget.WORDS)).performClick()
        assertThat(pinned).isEqualTo(TempoWidget.WORDS)
        compose.onNodeWithTag(OnboardingTags.NOT_NOW).assertDoesNotExist()
        compose.assertAccessible()
        compose.onNodeWithText("Done").performClick()
        assertThat(finished).isTrue()
    }

    @Test
    fun `a launcher that places no widget on request gets the way by hand`() {
        draw(OnboardingStep.WIDGET, CalendarPermission.ASKABLE)
        compose.onNodeWithTag(OnboardingTags.pin(TempoWidget.AGENDA)).assertDoesNotExist()
        compose.onNodeWithText("To add one", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a card already placed is said, with how to change it`() {
        draw(OnboardingStep.WIDGET, CalendarPermission.GRANTED, widgets = WidgetOffer(canPin = true, placed = true))
        compose.onNodeWithTag(OnboardingTags.PLACED).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the welcome reads at twice the text size`() {
        draw(OnboardingStep.WELCOME, CalendarPermission.ASKABLE)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the widget's page reads at twice the text size`() {
        draw(OnboardingStep.WIDGET, CalendarPermission.GRANTED, widgets = WidgetOffer(canPin = true))
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-widget-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `the calendar's page on an open foldable`() {
        draw(OnboardingStep.CALENDAR, CalendarPermission.ASKABLE)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-foldable")
    }
}
