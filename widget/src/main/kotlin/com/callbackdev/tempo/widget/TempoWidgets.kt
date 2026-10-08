package com.callbackdev.tempo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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

    /** «In words»: what comes next, large, and the day in a line (Chiaro's «In parole»). */
    WORDS,
}

/**
 * The widgets as one household: who is placed, and how to repaint them. Repaints go by the
 * system's own mapping of ids to providers, never by Glance's class bookkeeping (Chiaro saw every
 * widget repainted with the last-placed one's content that way), and always through
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
    suspend fun updateAll(context: Context) = update(context) { true }

    /** One card, with a fresh model: its settings screen's «apply now». */
    suspend fun updateOne(context: Context, appWidgetId: Int) = update(context) { it == appWidgetId }

    private suspend fun update(context: Context, which: (Int) -> Boolean) {
        WidgetRefresh.invalidate()
        val manager = AppWidgetManager.getInstance(context)
        val glance = GlanceAppWidgetManager(context)
        household.forEach { (receiver, widget) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).filter(which).forEach { id ->
                runCatching { widget().update(context, glance.getGlanceIdBy(id)) }
                    .onFailure { logWidgetFailure("Updating widget $id failed", it) }
            }
        }
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
        val pending = goAsync()
        Cleanup.launch {
            try {
                runCatching { app.widgetEntryPoint().looks().forget(appWidgetIds) }
            } finally {
                pending.finish()
            }
        }
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
