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
 * Dynamic, as Passo's outings' shortcuts are, not declared in XML: a static shortcut names its
 * package, and the debug build's is another (`.debug`). Published once; the launcher keeps it.
 */
object NewEventShortcut {
    const val EXTRA_NEW_EVENT = "com.callbackdev.tempo.extra.NEW_EVENT"
    private const val ID = "new_event"

    fun publish(context: Context) {
        if (ShortcutManagerCompat.getDynamicShortcuts(context).any { it.id == ID }) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val shortcut = ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel(context.getString(R.string.shortcut_new_event))
            .setLongLabel(context.getString(R.string.shortcut_new_event_long))
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
