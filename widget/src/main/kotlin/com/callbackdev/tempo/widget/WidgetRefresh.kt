package com.callbackdev.tempo.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The tick the widgets' compositions listen to when something they draw has changed.
 *
 * Glance runs `provideGlance` once per session and keeps the composition alive for about
 * forty-five seconds; inside that window `update()` only wakes the composition that is already
 * there, so a model read before `provideContent` would stay as it was. Chiaro learned it on a
 * device (its `WidgetRefresh`, 4 Sep 2026) and AndroidX says as much: observe the data inside the
 * composition. So every repaint bumps this first, and [rememberWidgetModel] reloads on it.
 *
 * In memory on purpose: a session cannot outlive the process that runs it.
 */
internal object WidgetRefresh {
    private val ticks = MutableStateFlow(0L)

    val revision: StateFlow<Long> = ticks.asStateFlow()

    fun invalidate() {
        ticks.update { it + 1 }
    }
}
