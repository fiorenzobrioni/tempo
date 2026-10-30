package com.callbackdev.tempo.core.calendar

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
import java.time.Instant

/**
 * The phone's clock app: the one that answers "show the alarms" (`AlarmClock.ACTION_SHOW_ALARMS`),
 * the reader's chosen one first. Opened by its launcher entry, not by that action: a clock app may
 * guard its alarm actions with `SET_ALARM`, a permission Tempo has no other use for, while its
 * front door is open to anyone, and lands where the reader left it. Seen through the manifest's
 * `<queries>`; nothing is asked of it. Today's alarm line and a card's time open it.
 */
object ClockApp {
    fun packageName(context: Context): String? {
        val manager = context.packageManager
        fun launchable(name: String) = manager.getLaunchIntentForPackage(name) != null
        val chosen = runCatching { manager.resolveActivity(Alarms, Flags) }.getOrNull()?.activityInfo?.packageName
        if (chosen != null && chosen != SYSTEM_RESOLVER && launchable(chosen)) return chosen
        // No default chosen among several: the phone's own clock before one installed later.
        return clockApps(context)
            .sortedByDescending { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.packageName }
            .firstOrNull { it != SYSTEM_RESOLVER && launchable(it) }
    }

    /**
     * The next alarm the reader set in a clock app, or null. The system's next alarm clock
     * (`AlarmManager.getNextAlarmClock`, no permission) is the next one *any* app set: a calendar
     * app may set its reminders as alarm clocks, so they ring on time in Doze, and Today and the
     * cards said "Alarm tomorrow at 10:00" for a reminder two days before Monday's event (owner's
     * phone, 30 Oct 2026, with no alarm in the clock app). So the alarm is kept only where the app
     * that set it is a clock app (it shows the alarms), read off its show intent's creator; one
     * with no show intent is nobody's to name. A reader's alarm behind another app's is said once
     * that one has rung: none is better than a wrong one.
     */
    fun nextAlarm(context: Context): Instant? {
        val next = runCatching { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock }.getOrNull()
            ?: return null
        val setter = next.showIntent?.creatorPackage ?: return null
        val clocks = clockApps(context).map { it.packageName }
        return Instant.ofEpochMilli(next.triggerTime).takeIf { setter != SYSTEM_RESOLVER && setter in clocks }
    }

    /** Every app that shows the alarms, the reader's default or not: a phone may keep two. */
    private fun clockApps(context: Context) =
        runCatching { context.packageManager.queryIntentActivities(Alarms, Flags) }.getOrDefault(emptyList())
            .map { it.activityInfo }

    private val Alarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    private val Flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())

    fun open(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The chooser Android stands in front of several apps with no default: not an app. */
    private const val SYSTEM_RESOLVER = "android"
}
