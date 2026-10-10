package com.callbackdev.tempo.feature.guide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.callbackdev.tempo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The guide (PLANNING.md §11 Phase 5): every chapter, its example, both languages, every reader. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class GuideScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(dark: Boolean = false, onBack: () -> Unit = {}) {
        compose.setContent { TempoTheme(darkTheme = dark) { GuideScreen(onBack = onBack) } }
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun `the guide opens on what Tempo does, and goes back`() {
        var back = false
        show(onBack = { back = true })

        compose.onNodeWithText("The guide").assertIsDisplayed()
        compose.onNodeWithText("Tempo shows the time", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Where your events come from").assertIsDisplayed()
        compose.assertAccessible()
        compose.onNodeWithContentDescription("Back").performClick()
        assertThat(back).isTrue()
    }

    @Test
    fun `every chapter is there, each with what it answers`() {
        show()
        // One line each chapter alone has.
        listOf(
            "Work calendars",
            "Free time",
            "Your calendar app does the writing",
            "Alarms and reminders",
            "Never a list to scroll",
            "Clock times, not countdowns",
            "nothing on a timer",
            "it sends nothing, to anyone",
        ).forEachIndexed { index, line ->
            scrollTo(line)
            compose.onNodeWithText(line, substring = true).assertIsDisplayed()
            compose.writeScreenshot("guide-${index + 1}")
        }
    }

    @Test
    fun `the example is Today's own dial, said to be one`() {
        show()
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.DIAL))
        compose.onNodeWithContentDescription("An example clock face", substring = true).assertIsDisplayed()
        scrollTo("An example: twenty past ten")
        compose.onNodeWithText("An example: twenty past ten", substring = true).assertIsDisplayed()
        compose.writeScreenshot("guide-dial")
    }

    @Test
    fun `the guide reads in the dark`() {
        show(dark = true)
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.DIAL))
        compose.writeScreenshot("guide-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the guide speaks Italian`() {
        show()
        compose.onNodeWithText("La guida").assertIsDisplayed()
        compose.onNodeWithText("Da dove vengono i tuoi impegni").assertIsDisplayed()
        scrollTo("A scrivere è la tua app calendario")
        compose.onNodeWithText("A scrivere è la tua app calendario").assertIsDisplayed()
        compose.writeScreenshot("guide-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, the guide still reads`() {
        show()
        compose.walkPage(hasTestTag(GuideTags.CONTENT), "guide-large-text", maxScreens = 40)
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xhdpi")
    fun `on an open foldable the guide is a column in the middle`() {
        show()
        compose.walkPage(hasTestTag(GuideTags.CONTENT), "guide-foldable", maxScreens = 2)
        compose.onNode(hasContentDescription("Back")).assertIsDisplayed()
    }
}
