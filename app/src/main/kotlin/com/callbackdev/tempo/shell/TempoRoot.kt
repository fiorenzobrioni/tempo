package com.callbackdev.tempo.shell

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.feature.guide.GuideRoute
import com.callbackdev.tempo.feature.onboarding.OnboardingRoute
import com.callbackdev.tempo.feature.settings.SettingsRoute
import com.callbackdev.tempo.feature.today.TodayRoute
import kotlinx.serialization.Serializable

/*
 * The shell's destinations as Navigation 3 keys: on one back stack, so
 * back is previewed under the finger (predictive back) and the stack survives a rotation and
 * process death. The onboarding stands before them (it is not a page to go back to).
 */

@Serializable
data object TodayKey : NavKey

/** Settings, from Today's gear. */
@Serializable
data object SettingsKey : NavKey

/** The guide, from the card at the top of Settings (Phase 5). */
@Serializable
data object GuideKey : NavKey

/**
 * The shell (PLANNING.md §11 Phases 2 and 3): the first run until it is done, then the pages.
 * The transitions: a short slide with a fade, the 300 ms that predictive back seeks,
 * and a plain fade under reduced motion.
 */
@Composable
fun TempoRoot(settings: UserSettings) {
    if (!settings.onboardingCompleted) {
        OnboardingRoute()
        return
    }
    MainPages()
}

@Composable
private fun MainPages() {
    val backStack = rememberNavBackStack(TodayKey)
    val reduced = reducedMotion()
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            transitionSpec = { forward(reduced) },
            popTransitionSpec = { backward(reduced) },
            predictivePopTransitionSpec = { backward(reduced) },
            entryProvider = entryProvider<NavKey> {
                entry<TodayKey> { TodayRoute(onOpenSettings = { backStack.add(SettingsKey) }) }
                entry<SettingsKey> { SettingsRoute(onBack = goBack, onOpenGuide = { backStack.add(GuideKey) }) }
                entry<GuideKey> { GuideRoute(onBack = goBack) }
            },
        )
    }
}

private const val NAV_MILLIS = 300
private const val SLIDE_DIVISOR = 6

private fun forward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(TempoMotion.fade()) togetherWith fadeOut(TempoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR })
}

private fun backward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(TempoMotion.fade()) togetherWith fadeOut(TempoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR })
}
