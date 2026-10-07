package com.callbackdev.tempo.core.designsystem.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Chiaro's motion (its DESIGN.md §7): springs, not durations, and every one of them collapses
 * to a 100 ms fade when the reader asked the system for less motion. No animation ever gates
 * information: with motion off the same content is there at the same moment.
 */
object TempoMotion {
    /** Anything that moves or resizes. */
    fun <T> spatial(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Chips, toggles, small state. */
    fun <T> spatialFast(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = 0.9f, stiffness = 800f)

    /** Color, alpha: things that change without moving. */
    fun <T> effects(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    fun <T> fade(): FiniteAnimationSpec<T> = tween(REDUCED_MOTION_FADE_MILLIS)

    const val REDUCED_MOTION_FADE_MILLIS = 100

    /** A section opening: reduced motion keeps the fade and drops the measure. */
    fun enter(reduced: Boolean): EnterTransition =
        if (reduced) fadeIn(fade()) else fadeIn(spatial()) + expandVertically(spatial())

    fun exit(reduced: Boolean): ExitTransition =
        if (reduced) fadeOut(fade()) else fadeOut(spatial()) + shrinkVertically(spatial())
}

/**
 * Whether the reader asked for less motion. Android has no flag of its own: Remove animations
 * (Accessibility) and the animator duration scale (Developer options) both write
 * `ANIMATOR_DURATION_SCALE`, and zero is the answer the platform's own animators read.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

private fun reducedMotionOf(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Live: the toggle lives outside the app, and can change while it is open. */
@Composable
internal fun rememberReducedMotion(context: Context): Boolean {
    var reduced by remember(context) { mutableStateOf(reducedMotionOf(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = reducedMotionOf(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        reduced = reducedMotionOf(context)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

@Composable
@ReadOnlyComposable
fun reducedMotion(): Boolean = LocalReducedMotion.current
