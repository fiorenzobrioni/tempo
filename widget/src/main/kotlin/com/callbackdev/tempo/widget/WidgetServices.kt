package com.callbackdev.tempo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.collection.intSetOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.data.widget.HomeScreenWidgets
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.widget.agenda.AgendaWidgetReceiver
import com.callbackdev.tempo.widget.words.WordsWidgetReceiver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's side of the refresh (PLANNING.md §7, "Settings changed"): a change of Tempo's own
 * settings repaints the cards at once (a format, the calendars, the appearance), and so does the
 * reader leaving the app ([refreshNow]), where they may have granted the calendar's permission or
 * hidden a calendar. Both only with a card on a home screen; neither runs on a timer.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** From the Application: listens to the settings for as long as the process lives, which costs nothing. */
    fun start() {
        scope.launch {
            settings.settings.distinctUntilChanged().drop(1).collect { refreshNow() }
        }
    }

    fun refreshNow() {
        scope.launch {
            if (TempoWidgets.hasWidgets(context)) runCatching { TempoWidgets.updateAll(context) }
        }
    }
}

@Singleton
class PinnedWidgets @Inject constructor(@ApplicationContext private val context: Context) : HomeScreenWidgets {
    private val manager: AppWidgetManager? get() = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()

    override fun canPin(): Boolean = manager?.isRequestPinAppWidgetSupported == true

    override fun pin(widget: TempoWidget): Boolean {
        val kind = when (widget) {
            TempoWidget.AGENDA -> WidgetKind.AGENDA
            TempoWidget.WORDS -> WidgetKind.WORDS
        }
        return runCatching { manager?.requestPinAppWidget(TempoWidgets.provider(context, kind), null, null) == true }
            .getOrDefault(false)
    }

    override fun placed(): Boolean = TempoWidgets.hasWidgets(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun homeScreenWidgets(pins: PinnedWidgets): HomeScreenWidgets
}

/**
 * The picker's generated previews (Android 15+): the real cards, drawn from [WidgetSamples] by
 * `providePreview`, published once per app version. The platform rate-limits the call, and a
 * preview only changes when the app does, so the version it was published for is remembered; a
 * refused call is tried again at the next start. Below Android 15 the static `previewLayout` is
 * what the picker shows.
 */
object WidgetPreviews {
    suspend fun publishIfNeeded(context: Context, appVersion: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val store = context.widgetLookDataStore
        if (store.data.first()[PublishedFor] == appVersion) return
        val manager = GlanceAppWidgetManager(context)
        val categories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        // Bounded: this runs at every app start until it succeeds, and nothing is worth a
        // coroutine left hanging on the launcher's side of a binder call.
        val results = listOf(AgendaWidgetReceiver::class, WordsWidgetReceiver::class).map { receiver ->
            runCatching {
                withTimeoutOrNull(PUBLISH_TIMEOUT_MILLIS) { manager.setWidgetPreviews(receiver, categories) }
            }
                .onFailure { Log.w(WidgetModelLoader.TAG, "Publishing the picker preview failed", it) }
                .getOrNull()
        }
        if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
            store.edit { it[PublishedFor] = appVersion }
        }
    }

    private val PublishedFor = longPreferencesKey("previews_published_for")
    private const val PUBLISH_TIMEOUT_MILLIS = 30_000L
}
