package com.callbackdev.tempo.core.calendar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.callbackdev.tempo.core.domain.calendar.calendarPermission
import com.callbackdev.tempo.core.model.CalendarPermission

/**
 * Tempo's standing with `READ_CALENDAR`, the only calendar permission it holds. Read fresh every
 * time: the reader can revoke it from the system's settings while the app is open, and grant it
 * there while the app is in the background (PLANNING.md §4.6).
 */
object CalendarAccess {
    const val PERMISSION = Manifest.permission.READ_CALENDAR

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * Granted, askable, or refused for good: only an activity can tell the last two apart, with
     * whether the question was ever asked, which the settings remember ([askedBefore]).
     */
    fun permission(activity: Activity, askedBefore: Boolean): CalendarPermission = calendarPermission(
        granted = isGranted(activity),
        askedBefore = askedBefore,
        showRationale = activity.shouldShowRequestPermissionRationale(PERMISSION),
    )
}
