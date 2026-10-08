package com.callbackdev.tempo.widget

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant

/**
 * The phone's clock app: the one that answers "show the alarms" (`AlarmClock.ACTION_SHOW_ALARMS`),
 * the reader's chosen one first. Opened by its launcher entry, not by that action: a clock app may
 * guard its alarm actions with `SET_ALARM`, a permission Tempo has no other use for, while its
 * front door is open to anyone, and lands where the reader left it. Seen through the manifest's
 * `<queries>`; nothing is asked of it.
 */
object ClockApp {
    fun packageName(context: Context): String? {
        val manager = context.packageManager
        val alarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        fun launchable(name: String) = manager.getLaunchIntentForPackage(name) != null
        val chosen = runCatching { manager.resolveActivity(alarms, flags) }.getOrNull()?.activityInfo?.packageName
        if (chosen != null && chosen != SYSTEM_RESOLVER && launchable(chosen)) return chosen
        // No default chosen among several: the phone's own clock before one installed later.
        return runCatching { manager.queryIntentActivities(alarms, flags) }.getOrDefault(emptyList())
            .map { it.activityInfo }
            .sortedByDescending { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.packageName }
            .firstOrNull { it != SYSTEM_RESOLVER && launchable(it) }
    }

    fun open(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The chooser Android stands in front of several apps with no default: not an app. */
    private const val SYSTEM_RESOLVER = "android"
}

/**
 * Where each of a card's touches goes, with Tempo as the fallback whenever the chosen door is not
 * there (an uninstalled clock app, no calendar app): a touch never does nothing.
 */
internal object WidgetIntents {
    /** Tempo's own front door: the launcher's intent brings back the app's task, never a second one. */
    fun tempo(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)

    fun header(context: Context, tap: HeaderTap, doors: WidgetDoors, now: Instant): Intent? = when (tap) {
        HeaderTap.TEMPO -> null
        HeaderTap.CALENDAR -> CalendarIntents.openDay(now).takeIf { doors.canOpenDay }?.newTask()
        HeaderTap.CLOCK -> doors.clockPackage?.let { ClockApp.open(context, it) }
    } ?: tempo(context)

    /**
     * One occurrence in the calendar app. Its identifier tells two occurrences of one event apart
     * (a daily stand-up today and tomorrow): they share the event's URI, and a pending intent
     * matched on it alone would carry the last one's times for both.
     */
    fun event(context: Context, tap: EventTap, doors: WidgetDoors, event: EventInstance): Intent? =
        if (tap == EventTap.CALENDAR && doors.canOpenEvent) {
            CalendarIntents.view(
                event,
            ).newTask().setIdentifier("tempo-event-${event.eventId}-${event.begin.toEpochMilli()}")
        } else {
            tempo(context)
        }

    private fun Intent.newTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
