package com.callbackdev.tempo

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.feature.onboarding.OnboardingTags
import com.callbackdev.tempo.feature.today.TodayTags
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The whole app, from a cold start: the real Application and its Hilt graph, the real activity,
 * the first run, then Today. The feature modules test their screens apart; this is the one test
 * that would see a start that breaks before any of them draws (a tablet's crash at launch, 7 Oct
 * 2026), on a phone and on a tablet, upright and on its side, in Italian.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppLaunchTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    /** The calendar's permission, granted before the activity starts for the tests that say so. */
    private val permission = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                if (description.methodName.contains("granted")) {
                    Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
                        .grantPermissions(Manifest.permission.READ_CALENDAR)
                }
                base.evaluate()
            }
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(permission).around(compose)

    private fun startsThroughTheFirstRun(granted: Boolean = false) {
        compose.waitForIdle()
        compose.onNodeWithTag(OnboardingTags.PRIMARY).performClick()
        compose.waitForIdle()
        // Granted, the calendar's page says so and its button moves on; otherwise "Not now".
        compose.onNodeWithTag(if (granted) OnboardingTags.PRIMARY else OnboardingTags.NOT_NOW).performClick()
        compose.waitForIdle()
        // The widgets' page, through the real graph (the launcher's pin support asked), then Done.
        compose.onNodeWithTag(OnboardingTags.PRIMARY).performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithTag(TodayTags.TIME).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `a phone on Android 14`() = startsThroughTheFirstRun()

    @Test
    @Config(sdk = [35], qualifiers = "en-rGB-w411dp-h891dp-night-xxhdpi")
    fun `a phone on Android 15, granted`() = startsThroughTheFirstRun(granted = true)

    @Test
    @Config(sdk = [34], qualifiers = "it-rIT-w1244dp-h778dp-land-night-xhdpi")
    fun `a tablet on its side`() = startsThroughTheFirstRun()

    @Test
    @Config(sdk = [35], qualifiers = "it-rIT-w778dp-h1244dp-port-xhdpi")
    fun `a tablet upright, granted`() = startsThroughTheFirstRun(granted = true)

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
