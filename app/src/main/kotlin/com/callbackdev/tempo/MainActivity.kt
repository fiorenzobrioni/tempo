package com.callbackdev.tempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.feature.today.TodayRoute
import dagger.hilt.android.AndroidEntryPoint

/**
 * The one activity, edge to edge. Phase 0 shows Today's clock in the default dress (Chiaro's
 * vivid one, Google Sans, the phone's light or dark); the reader's appearance (theme, palette,
 * typeface) is read from the settings in Phase 2, and the shell (onboarding, Today, Settings, on
 * Navigation 3, as Passo's) arrives with Phase 3 (PLANNING.md §11).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TempoTheme {
                TodayRoute()
            }
        }
    }
}
