package com.callbackdev.tempo.feature.today

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.CalendarPermission
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today under a keyboard: a row takes the focus only where no finger has put the window in touch
 * mode, which is set before the window is made, so these tests stand apart from the page's others.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class TodayKeyboardTest {
    private val compose = createComposeRule()

    private val keyboard = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
                try {
                    base.evaluate()
                } finally {
                    InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
                }
            }
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(keyboard).around(compose)

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun focus(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    /**
     * The list runs to the screen's top, under the compact bar: a row the focus lands on going up
     * the page is brought under the bar's foot, where the list alone would stop it at the screen's
     * edge, behind the bar.
     */
    @Test
    fun `a row the focus lands on is brought out from under the bar`() {
        compose.setContent {
            TempoTheme { TodayScreen(TodaySample.state(), CalendarPermission.GRANTED, TodayActions()) }
        }
        val review = TodayTags.event(TodaySample.review.eventId)
        // The event under way half under the bar.
        val up = bounds(review).top + 10 * compose.density.density
        compose.onNodeWithTag(TodayTags.LIST).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, up) }
        compose.waitForIdle()
        val bar = bounds(TodayTags.BAR)
        assertThat(bounds(review).top).isLessThan(bar.bottom)
        focus(review)
        val row = compose.onNodeWithTag(review).fetchSemanticsNode()
        assertThat(row.boundsInRoot.top).isAtLeast(bounds(TodayTags.BAR).bottom)
        assertThat(row.boundsInRoot.height).isEqualTo(row.size.height.toFloat())
    }

    /** At the page's top there is no bar: the focus on a row in view moves nothing. */
    @Test
    fun `with the hero in view the focus on a row moves nothing`() {
        compose.setContent {
            TempoTheme { TodayScreen(TodaySample.state(), CalendarPermission.GRANTED, TodayActions()) }
        }
        val review = TodayTags.event(TodaySample.review.eventId)
        val before = bounds(review)
        focus(review)
        assertThat(bounds(review)).isEqualTo(before)
        compose.onNodeWithTag(TodayTags.BAR).assertDoesNotExist()
    }
}
