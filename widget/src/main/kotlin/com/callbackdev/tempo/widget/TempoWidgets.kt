package com.callbackdev.tempo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.callbackdev.tempo.widget.agenda.AgendaWidget
import com.callbackdev.tempo.widget.agenda.AgendaWidgetReceiver
import com.callbackdev.tempo.widget.refresh.WidgetRefreshArming
import com.callbackdev.tempo.widget.words.WordsWidget
import com.callbackdev.tempo.widget.words.WordsWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The two cards, as the settings screen and the previews need to tell them apart. */
enum class WidgetKind {
    /** «Agenda»: the clock and the date over the rest of the day. */
    AGENDA,

    /** «In words»: what comes next, large, and the day in a line. */
    WORDS,
}

/**
 * The widgets as one household: who is placed, and how to repaint them. Repaints go by the
 * system's own mapping of ids to providers, never by Glance's class bookkeeping (that way, every
 * widget was seen repainted with the last-placed one's content), and always through
 * [WidgetRefresh] first, because `update()` alone wakes a live session without reloading it.
 */
object TempoWidgets {
    private val household: List<Pair<Class<out GlanceAppWidgetReceiver>, () -> GlanceAppWidget>> = listOf(
        AgendaWidgetReceiver::class.java to { AgendaWidget() },
        WordsWidgetReceiver::class.java to { WordsWidget() },
    )

    /** The provider of each kind, for a pin request. */
    fun provider(context: Context, kind: WidgetKind): ComponentName = ComponentName(
        context,
        when (kind) {
            WidgetKind.AGENDA -> AgendaWidgetReceiver::class.java
            WidgetKind.WORDS -> WordsWidgetReceiver::class.java
        },
    )

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return household.any { (receiver, _) ->
            runCatching { manager.getAppWidgetIds(ComponentName(context, receiver)) }.getOrNull()?.isNotEmpty() == true
        }
    }

    /** Which card [appWidgetId] is; null for an id the host has not bound yet. */
    fun kindOf(context: Context, appWidgetId: Int): WidgetKind? =
        when (AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className) {
            AgendaWidgetReceiver::class.java.name -> WidgetKind.AGENDA
            WordsWidgetReceiver::class.java.name -> WidgetKind.WORDS
            else -> null
        }

    /** Every placed card, with a fresh model. */
    suspend fun updateAll(context: Context) {
        update(context) { true }
    }

    /** One card, with a fresh model: its settings screen's «apply now». */
    suspend fun updateOne(context: Context, appWidgetId: Int) {
        update(context) { it == appWidgetId }
    }

    /**
     * Every placed card, with a fresh model, and the caller kept until they are drawn (never
     * longer than [timeoutMillis]): the calendar's job and the receivers hold the process awake
     * for exactly the repaint they asked for ([WidgetRefresh]). [why] names it in the log.
     */
    suspend fun repaintAll(context: Context, why: String, timeoutMillis: Long) {
        val started = SystemClock.elapsedRealtime()
        val (revision, ids) = update(context) { true }
        val drawn = WidgetRefresh.awaitDrawn(ids, revision, timeoutMillis)
        val took = SystemClock.elapsedRealtime() - started
        logWidget(
            if (drawn) {
                "$why: ${ids.size} card(s) drawn in $took ms"
            } else {
                "$why: not every card drawn within $timeoutMillis ms"
            },
        )
    }

    /** The revision the cards will reload at, and the cards asked to. */
    private suspend fun update(context: Context, which: (Int) -> Boolean): Pair<Long, List<Int>> {
        val revision = WidgetRefresh.invalidate()
        val manager = AppWidgetManager.getInstance(context)
        val glance = GlanceAppWidgetManager(context)
        val updated = mutableListOf<Int>()
        household.forEach { (receiver, widget) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).filter(which).forEach { id ->
                runCatching { widget().update(context, glance.getGlanceIdBy(id)) }
                    .onSuccess { updated += id }
                    .onFailure { logWidgetFailure("Updating widget $id failed", it) }
            }
        }
        return revision to updated
    }
}

/** What Glance's classes, which Hilt does not build, need from the graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun loader(): WidgetModelLoader

    fun looks(): WidgetLookStore

    fun arming(): WidgetRefreshArming
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/**
 * Where one card's model comes from, for its `provideGlance`: the loader when the graph can hand
 * one over, and otherwise a card that says the calendar could not be read. Either way the card
 * reaches `provideContent`: Glance keeps its loading spinner on screen until it does.
 */
internal class CardModels(context: Context, private val appWidgetId: Int) {
    private val loader: WidgetModelLoader? = try {
        context.widgetEntryPoint().loader()
    } catch (e: Exception) {
        Log.e(WidgetModelLoader.TAG, "No model loader for widget $appWidgetId", e)
        null
    }

    suspend fun load(): WidgetModel = loader?.loadForCard(appWidgetId) ?: WidgetModel.unavailable()
}

/**
 * The receivers' shared duties: a removed card takes its look with it, and the last card gone
 * takes the refresh with it (the content-URI work and the boundary alarm), so nothing of Tempo
 * waits on a calendar no card shows.
 */
abstract class TempoWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext
        // Glance's own onDeleted has already held the broadcast open with goAsync, which hands its
        // pending result out once: here it is null, and finishing it crashed the process (a Galaxy
        // S24 Ultra, 8 Oct 2026). The look is forgotten in the time Glance's own work keeps the
        // broadcast open; if the process went first, a few bytes stay under an id never reused.
        WidgetRefresh.forget(appWidgetIds)
        val pending: PendingResult? = goAsync()
        Cleanup.launch {
            try {
                runCatching { app.widgetEntryPoint().looks().forget(appWidgetIds) }
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        logWidget("Widget $appWidgetId resized")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Called per provider: the other card may still be on a home screen.
        if (!TempoWidgets.hasWidgets(context)) {
            runCatching { context.widgetEntryPoint().arming().disarm() }
        }
    }

    private companion object {
        val Cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
