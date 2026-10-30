package com.callbackdev.tempo.shell

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** The launcher's "New event" says it in the app's language, whichever it is now. */
@RunWith(AndroidJUnit4::class)
class NewEventShortcutTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `the shortcut is published with the app's words`() {
        NewEventShortcut.publish(app)
        assertThat(labels()).containsExactly("New event" to "New event in your calendar")
    }

    @Test
    fun `a change of the app's language reaches the launcher`() {
        NewEventShortcut.publish(app)
        NewEventShortcut.publish(app.inLanguage(Locale.ITALIAN))
        assertThat(labels()).containsExactly("Nuovo evento" to "Nuovo evento nel tuo calendario")
    }

    private fun labels() = ShortcutManagerCompat.getDynamicShortcuts(app)
        .map { it.shortLabel.toString() to it.longLabel.toString() }

    private fun Context.inLanguage(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
