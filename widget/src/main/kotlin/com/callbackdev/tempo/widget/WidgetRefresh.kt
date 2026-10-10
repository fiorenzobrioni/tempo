package com.callbackdev.tempo.widget

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * The tick the widgets' compositions listen to when something they draw has changed, and what
 * each card last drew.
 *
 * Glance runs `provideGlance` once per session and keeps the composition alive for about
 * forty-five seconds; inside that window `update()` only wakes the composition that is already
 * there, so a model read before `provideContent` would stay as it was. A device showed it
 * (4 Sep 2026) and AndroidX says as much: observe the data inside the
 * composition. So every repaint bumps this first, and [rememberWidgetModel] reloads on it.
 *
 * Each composition reports the revision it has drawn ([drawn]), so whatever asked for a repaint
 * (the calendar's job, a receiver) can stay until the cards are drawn ([awaitDrawn]). Glance
 * composes in a `SessionWorker` that WorkManager starts inside Tempo's process, with no job of
 * its own bound yet: once the job or the broadcast that asked has ended, Android may freeze the
 * cached process with the card half drawn, and thaw it only when the system gets to that worker's
 * job, half a minute later (owner's report, 9 Oct 2026: after an edit in the calendar the card
 * "fell asleep", deaf even to a resize). Staying the few hundred milliseconds a repaint takes
 * keeps the process awake for exactly the work it does.
 *
 * In memory on purpose: a session cannot outlive the process that runs it.
 */
internal object WidgetRefresh {
    private val ticks = MutableStateFlow(0L)

    val revision: StateFlow<Long> = ticks.asStateFlow()

    /** The newest revision each card has drawn, by `appWidgetId`. */
    private val drawnRevisions = MutableStateFlow<Map<Int, Long>>(emptyMap())

    /** The model each card last drew, so a new session draws at once and reads after ([firstModel]). */
    private val lastModels = ConcurrentHashMap<Int, WidgetModel>()

    /** A new revision, which every live composition reloads on; the revision, for [awaitDrawn]. */
    fun invalidate(): Long = ticks.updateAndGet { it + 1 }

    /** [appWidgetId]'s composition has drawn [model], read at [revision]. */
    fun drawn(appWidgetId: Int, revision: Long, model: WidgetModel) {
        lastModels[appWidgetId] = model
        drawnRevisions.update { drawn ->
            if ((drawn[appWidgetId] ?: Long.MIN_VALUE) >= revision) drawn else drawn + (appWidgetId to revision)
        }
    }

    fun lastModel(appWidgetId: Int): WidgetModel? = lastModels[appWidgetId]

    /** Removed cards: nothing of theirs is kept. */
    fun forget(appWidgetIds: IntArray) {
        appWidgetIds.forEach { lastModels.remove(it) }
        drawnRevisions.update { drawn -> drawn - appWidgetIds.toSet() }
    }

    /**
     * Until every card in [appWidgetIds] has drawn [revision] or later, then [DRAW_GRACE_MILLIS]
     * for Glance to hand the views to the launcher (it does so right after the composition
     * settles); never longer than [timeoutMillis]. Whether they all made it, for the log.
     */
    suspend fun awaitDrawn(appWidgetIds: Collection<Int>, revision: Long, timeoutMillis: Long): Boolean {
        val all = withTimeoutOrNull(timeoutMillis) {
            drawnRevisions.first { drawn -> appWidgetIds.all { (drawn[it] ?: Long.MIN_VALUE) >= revision } }
        } != null
        if (all) delay(DRAW_GRACE_MILLIS)
        return all
    }

    private const val DRAW_GRACE_MILLIS = 500L
}
