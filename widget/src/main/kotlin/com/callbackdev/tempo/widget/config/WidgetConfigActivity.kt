package com.callbackdev.tempo.widget.config

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.model.AppFont
import com.callbackdev.tempo.core.model.AppPalette
import com.callbackdev.tempo.core.model.ThemeMode
import com.callbackdev.tempo.widget.TempoWidgets
import com.callbackdev.tempo.widget.WidgetKind
import com.callbackdev.tempo.widget.WidgetLook
import com.callbackdev.tempo.widget.WidgetLookStore
import com.callbackdev.tempo.widget.WidgetModel
import com.callbackdev.tempo.widget.WidgetModelLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The launcher's door into one widget's settings (`android:configure`, reconfigurable and
 * optional: a card is placed at once with the defaults, and a long press opens this). It wears
 * the app's own appearance: it is part of the app.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var looks: WidgetLookStore

    @Inject lateinit var loader: WidgetModelLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val target = configTarget(applicationContext, intent)
        if (target == null) {
            finish()
            return
        }
        val (appWidgetId, kind) = target
        // The host expects this result whether or not anything changes.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        val placed = placedWidgetSize(applicationContext, appWidgetId)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            var look by remember { mutableStateOf<WidgetLook?>(null) }
            var model by remember { mutableStateOf<WidgetModel?>(null) }
            LaunchedEffect(appWidgetId) {
                look = looks.lookFor(appWidgetId)
                model = runCatching { loader.load(appWidgetId) }.getOrNull()
            }

            fun save(next: WidgetLook) {
                look = next
                scope.launch {
                    looks.set(appWidgetId, next)
                    runCatching { TempoWidgets.updateOne(applicationContext, appWidgetId) }
                }
            }

            TempoTheme(
                darkTheme = when (settings?.theme) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
                },
                dynamicColor = settings?.dynamicColor ?: false,
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS,
            ) {
                // Until the stored look is read, nothing: a screen of defaults about to change
                // would show the reader switches in the wrong place.
                look?.let { current ->
                    WidgetConfigScreen(
                        kind = kind,
                        look = current,
                        model = model,
                        placed = placed,
                        onLook = ::save,
                        onOpacityDrag = { look = current.copy(opacityPct = it) },
                        onOpacityDone = { look?.let(::save) },
                        onDone = ::finish,
                    )
                }
            }
        }
    }
}

/**
 * The card a configure request names, when it is one of Tempo's placed cards. The screen is
 * exported, because the launcher opens it, so any app could open it with any id: one that is not
 * a Tempo card on a home screen (Android names its provider only to the provider and the host)
 * gets nothing, rather than a screen drawn from the reader's calendar that would write a look for
 * a card that does not exist.
 */
internal fun configTarget(context: Context, intent: Intent?): Pair<Int, WidgetKind>? {
    val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
    return TempoWidgets.kindOf(context, appWidgetId)?.let { appWidgetId to it }
}
