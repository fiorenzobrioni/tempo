package com.callbackdev.tempo

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.callbackdev.tempo.widget.WidgetPreviews
import com.callbackdev.tempo.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The application: Hilt's graph root. Nothing runs here on a timer: Tempo has no service and no
 * background work of its own (PLANNING.md §9). What starts is a listener on the settings, which
 * costs nothing until the reader changes one and repaints the widgets then, and the widget
 * picker's previews, published once per version. The widgets' refresh is armed by the cards
 * themselves, when there is one on a home screen.
 */
@HiltAndroidApp
class TempoApplication : Application() {
    @Inject lateinit var widgets: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        widgets.start()
        val version = runCatching {
            PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        }.getOrDefault(0L)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { WidgetPreviews.publishIfNeeded(this@TempoApplication, version) }
        }
    }
}
