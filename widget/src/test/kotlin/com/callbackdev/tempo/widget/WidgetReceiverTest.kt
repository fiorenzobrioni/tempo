package com.callbackdev.tempo.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.widget.agenda.AgendaWidgetReceiver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.Collections

/**
 * A card removed from the home screen, as the system delivers it: Glance's own `onDeleted` holds
 * the broadcast open with `goAsync`, which hands its pending result out once, so the receiver's
 * cleanup must not finish one it never got (a Galaxy S24 Ultra, 8 Oct 2026: the process crashed
 * on a null `PendingResult` a moment after a card was removed).
 */
@RunWith(AndroidJUnit4::class)
class WidgetReceiverTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val uncaught = Collections.synchronizedList(mutableListOf<Throwable>())
    private var previous: Thread.UncaughtExceptionHandler? = null

    @Before
    fun catchUncaught() {
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
    }

    @After
    fun restore() {
        Thread.setDefaultUncaughtExceptionHandler(previous)
    }

    @Test
    fun `a removed card is cleaned up without finishing a pending result twice`() {
        val deleted = Intent(AppWidgetManager.ACTION_APPWIDGET_DELETED)
            .setComponent(ComponentName(context, AgendaWidgetReceiver::class.java))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(7))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 7)
        context.sendBroadcast(deleted)
        shadowOf(Looper.getMainLooper()).idle()
        // The cleanup runs on its own dispatcher: give it the time it takes, then look for a crash.
        val until = System.currentTimeMillis() + SETTLE_MS
        while (System.currentTimeMillis() < until && uncaught.isEmpty()) {
            Thread.sleep(POLL_MS)
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertThat(uncaught.map { "$it" }).isEmpty()
    }

    private companion object {
        const val SETTLE_MS = 2_000L
        const val POLL_MS = 50L
    }
}
