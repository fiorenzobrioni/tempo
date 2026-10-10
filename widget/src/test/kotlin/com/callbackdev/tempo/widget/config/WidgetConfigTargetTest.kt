package com.callbackdev.tempo.widget.config

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.widget.WidgetKind
import com.callbackdev.tempo.widget.agenda.AgendaWidgetReceiver
import com.callbackdev.tempo.widget.words.WordsWidgetReceiver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The exported settings screen opens only for one of Tempo's own placed cards. */
@RunWith(AndroidJUnit4::class)
class WidgetConfigTargetTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    private fun bind(id: Int, provider: ComponentName) {
        manager.addBoundWidget(id, AppWidgetProviderInfo().apply { this.provider = provider })
    }

    private fun configure(id: Int?) = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
        if (id != null) putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    @Test
    fun `each of Tempo's cards opens as its own kind`() {
        bind(3, ComponentName(context, AgendaWidgetReceiver::class.java))
        bind(4, ComponentName(context, WordsWidgetReceiver::class.java))
        assertThat(configTarget(context, configure(3))).isEqualTo(3 to WidgetKind.AGENDA)
        assertThat(configTarget(context, configure(4))).isEqualTo(4 to WidgetKind.WORDS)
    }

    @Test
    fun `no id, an unknown id or another app's card opens nothing`() {
        bind(5, ComponentName("com.example.weather", "com.example.weather.WeatherWidget"))
        assertThat(configTarget(context, configure(null))).isNull()
        assertThat(configTarget(context, null)).isNull()
        assertThat(configTarget(context, configure(42))).isNull()
        assertThat(configTarget(context, configure(5))).isNull()
    }
}
