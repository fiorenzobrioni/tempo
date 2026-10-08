package com.callbackdev.tempo.widget.config

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.core.testing.assertAccessible
import com.callbackdev.tempo.core.testing.walkPage
import com.callbackdev.tempo.widget.HeaderTap
import com.callbackdev.tempo.widget.WidgetBackground
import com.callbackdev.tempo.widget.WidgetDoors
import com.callbackdev.tempo.widget.WidgetKind
import com.callbackdev.tempo.widget.WidgetLook
import com.callbackdev.tempo.widget.WidgetSamples
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h1600dp-xhdpi")
class WidgetConfigScreenTest {
    @get:Rule val compose = createComposeRule()

    private var look by mutableStateOf(WidgetLook())

    private val withClock = WidgetDoors(canOpenEvent = true, canOpenDay = true, clockPackage = "com.example.clock")

    private fun show(
        kind: WidgetKind,
        doors: WidgetDoors = withClock,
        settings: UserSettings = UserSettings(onboardingCompleted = true),
    ) {
        compose.setContent {
            TempoTheme {
                WidgetConfigScreen(
                    kind = kind,
                    look = look,
                    model = WidgetSamples.model(doors = doors, settings = settings),
                    placed = null,
                    onLook = { look = it },
                    onOpacityDrag = { look = look.copy(opacityPct = it) },
                    onOpacityDone = {},
                    onDone = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the time's touch can open the phone's clock app`() {
        show(WidgetKind.AGENDA)
        scrollToTag(WidgetConfigTags.HEADER_TAP)
        compose.onNodeWithTag(WidgetConfigTags.HEADER_TAP).performClick()
        compose.onNodeWithText("Your alarms, timers and stopwatch", substring = true).assertExists()
        compose.onNodeWithText("Opens your clock app").performClick()
        assertThat(look.headerTap).isEqualTo(HeaderTap.CLOCK)
        compose.onNodeWithText("Opens your clock app").assertExists()
        save("widget-config-agenda")
    }

    @Test
    fun `with no clock app and no calendar app, only Tempo is offered for the time`() {
        show(WidgetKind.AGENDA, doors = WidgetDoors.None)
        scrollToTag(WidgetConfigTags.HEADER_TAP)
        compose.onNodeWithTag(WidgetConfigTags.HEADER_TAP).performClick()
        compose.onNodeWithText("Opens your clock app").assertDoesNotExist()
        compose.onNodeWithText("Opens your calendar at today").assertDoesNotExist()
    }

    @Test
    fun `a card's own clock format is kept, and Tempo's is the default`() {
        show(WidgetKind.WORDS)
        compose.onAllNodesWithText("As in Tempo’s settings").onFirst().assertExists()
        compose.onNodeWithText("Time format").performClick()
        compose.onNodeWithText("12-hour").performClick()
        assertThat(look.clockFormat).isEqualTo(ClockFormat.H12)
    }

    @Test
    fun `with the time hidden its format is not offered, and with both hidden the header's touch neither`() {
        look = WidgetLook(showClock = false, showDate = false)
        show(WidgetKind.AGENDA)
        compose.onNodeWithText("Time format").assertDoesNotExist()
        compose.onNodeWithTag(WidgetConfigTags.HEADER_TAP).assertDoesNotExist()
    }

    @Test
    fun `with today alone on Tempo's horizon there is no days-ahead switch`() {
        show(WidgetKind.AGENDA, settings = UserSettings(horizonDays = 1))
        compose.onNodeWithTag(WidgetConfigTags.LIST).performScrollToNode(hasText("All-day events"))
        compose.onNodeWithText("The days ahead").assertDoesNotExist()
    }

    @Test
    fun `the colours appear once a colour is the ground, and a pick is kept`() {
        look = WidgetLook(background = WidgetBackground.LIGHT)
        show(WidgetKind.WORDS)
        compose.onNodeWithTag(WidgetConfigTags.LIST).performScrollToNode(hasText("A colour"))
        compose.onNodeWithContentDescription("Terracotta").assertDoesNotExist()
        compose.onNodeWithText("A colour").performClick()
        compose.onNodeWithContentDescription("Terracotta").performClick()
        assertThat(look.background).isEqualTo(WidgetBackground.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.CLAY)
        save("widget-config-words")
    }

    @Test
    fun `every ground and every colour has its row`() {
        assertThat(WidgetBackgroundChoices.map { it.first }).containsExactlyElementsIn(WidgetBackground.entries)
        assertThat(WidgetCardColorChoices.map { it.first }).containsExactlyElementsIn(WidgetCardColor.entries)
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi", fontScale = 2f)
    fun `the page reads at twice the text size`() {
        show(WidgetKind.AGENDA)
        compose.walkPage(hasTestTag(WidgetConfigTags.LIST), "widget-config-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xhdpi")
    fun `the page on an open foldable`() {
        show(WidgetKind.WORDS)
        compose.walkPage(hasTestTag(WidgetConfigTags.LIST), "widget-config-foldable")
    }

    private fun scrollToTag(tag: String) {
        compose.onNodeWithTag(WidgetConfigTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    private fun save(name: String) {
        compose.assertAccessible()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
