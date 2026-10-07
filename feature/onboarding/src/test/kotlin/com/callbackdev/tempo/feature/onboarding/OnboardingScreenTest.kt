package com.callbackdev.tempo.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
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
    ) {
        compose.setContent { TempoTheme { OnboardingScreen(step, permission, actions) } }
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
    fun `the calendar's page asks, and lets the reader through without it`() {
        var asked = false
        var finished = false
        draw(
            OnboardingStep.CALENDAR,
            CalendarPermission.ASKABLE,
            OnboardingActions(ask = { asked = true }, finish = { finished = true }),
        )
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
        compose.onNodeWithTag(OnboardingTags.NOT_NOW).performClick()
        assertThat(finished).isTrue()
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
    fun `granted, it says so and finishes`() {
        var finished = false
        draw(OnboardingStep.CALENDAR, CalendarPermission.GRANTED, OnboardingActions(finish = { finished = true }))
        compose.onNodeWithTag(OnboardingTags.GRANTED).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.NOT_NOW).assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        assertThat(finished).isTrue()
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the welcome reads at twice the text size`() {
        draw(OnboardingStep.WELCOME, CalendarPermission.ASKABLE)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `the calendar's page on an open foldable`() {
        draw(OnboardingStep.CALENDAR, CalendarPermission.ASKABLE)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-foldable")
    }
}
