package com.callbackdev.tempo.shell

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.callbackdev.tempo.R

/**
 * "New event" on the launcher's long press (VISION.md, System surfaces; owner, PLANNING.md §15).
 * It opens Tempo, which hands the new event to the calendar app at the next half hour: Back from
 * the calendar app lands on Today, where the new event then shows.
 *
 * Dynamic, not declared in XML: a static shortcut names its
 * package, and the debug build's is another (`.debug`). The launcher keeps it, with the words it
 * was given: published again when they are not the app's words any more (the reader changed the
 * app's language), and left alone otherwise.
 */
object NewEventShortcut {
    const val EXTRA_NEW_EVENT = "com.callbackdev.tempo.extra.NEW_EVENT"
    private const val ID = "new_event"

    fun publish(context: Context) {
        val short = context.getString(R.string.shortcut_new_event)
        val long = context.getString(R.string.shortcut_new_event_long)
        val published = ShortcutManagerCompat.getDynamicShortcuts(context).firstOrNull { it.id == ID }
        if (published?.shortLabel?.toString() == short && published?.longLabel?.toString() == long) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val shortcut = ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel(short)
            .setLongLabel(long)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_new_event))
            .setIntent(Intent(launch).setAction(Intent.ACTION_VIEW).putExtra(EXTRA_NEW_EVENT, true))
            .build()
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
        } catch (_: IllegalStateException) {
            // The launcher's rate limit: the next start publishes it.
        }
    }
}
