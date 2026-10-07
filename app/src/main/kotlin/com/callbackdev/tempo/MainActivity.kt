package com.callbackdev.tempo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.domain.clock.nextHalfHour
import com.callbackdev.tempo.core.model.ThemeMode
import com.callbackdev.tempo.shell.NewEventShortcut
import com.callbackdev.tempo.shell.TempoRoot
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * The one activity, edge to edge. It wears the reader's appearance (theme, palette, typeface,
 * wallpaper colours), hands the pages to [TempoRoot], and answers the launcher's "New event"
 * ([NewEventShortcut]) by handing a new event to the calendar app, over Today.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NewEventShortcut.publish(this)
        if (savedInstanceState == null) newEventIfAsked(intent)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (settings?.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            // The bars' ink follows the applied theme, not the system's: a reader who forces
            // light on a dark phone would otherwise get white icons on a white page.
            val view = LocalView.current
            DisposableEffect(dark) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
                onDispose { }
            }
            val current = settings
            if (current == null) {
                // One bare frame until the settings are read: flashing the wrong theme is worse.
                TempoTheme(darkTheme = dark) { Surface(Modifier.fillMaxSize()) { } }
            } else {
                TempoTheme(
                    darkTheme = dark,
                    dynamicColor = current.dynamicColor,
                    palette = current.palette,
                    font = current.font,
                ) {
                    TempoRoot(current)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newEventIfAsked(intent)
    }

    /** The shortcut's new event, once per touch, if a calendar app takes it; otherwise Today alone. */
    private fun newEventIfAsked(intent: Intent?) {
        if (intent?.getBooleanExtra(NewEventShortcut.EXTRA_NEW_EVENT, false) != true) return
        intent.removeExtra(NewEventShortcut.EXTRA_NEW_EVENT)
        val insert = CalendarIntents.insert(nextHalfHour(Instant.now(), ZoneId.systemDefault()))
        if (CalendarIntents.canOpen(this, insert)) runCatching { startActivity(insert) }
    }
}
