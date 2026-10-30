package com.callbackdev.tempo.core.calendar

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock

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
