package com.callbackdev.tempo.core.calendar

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.provider.AlarmClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant

/** The phone's clock app, the door of Today's alarm line and of a card's time, and the alarms it set. */
@RunWith(AndroidJUnit4::class)
class ClockAppTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    /** A clock app that shows the alarms, with a launcher entry unless [launchable] is false. */
    private fun install(packageName: String, system: Boolean, launchable: Boolean = true) {
        val app = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
        }
        val activity = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
            applicationInfo = app
        }
        val manager = shadowOf(context.packageManager)
        manager.addResolveInfoForIntent(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            ResolveInfo().apply {
                activityInfo =
                    activity
            },
        )
        if (launchable) {
            manager.addResolveInfoForIntent(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                ResolveInfo().apply { activityInfo = activity },
            )
        }
    }

    @Test
    fun `the clock app is the one that shows the alarms, opened by its front door`() {
        install("com.example.clock", system = true)
        assertThat(ClockApp.packageName(context)).isEqualTo("com.example.clock")
        val open = ClockApp.open(context, "com.example.clock")
        assertThat(open?.component).isEqualTo(ComponentName("com.example.clock", "com.example.clock.Main"))
        assertThat(open?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK)).isNotEqualTo(0)
    }

    @Test
    fun `with no default among several, the phone's own clock before one installed later`() {
        // With several and none chosen, Android answers with its own chooser, from "android".
        install("android", system = true, launchable = false)
        install("com.example.later", system = false)
        install("com.example.clock", system = true)
        assertThat(ClockApp.packageName(context)).isEqualTo("com.example.clock")
    }

    @Test
    fun `an app with no front door is not the clock app`() {
        install("com.example.hidden", system = true, launchable = false)
        assertThat(ClockApp.packageName(context)).isNull()
    }

    @Test
    fun `no clock app, no door`() {
        assertThat(ClockApp.packageName(context)).isNull()
    }

    private val tomorrowAt10 = Instant.parse("2026-10-31T09:00:00Z")

    /** An alarm clock set by [setter], as the system reports the next one; no show intent when null. */
    private fun nextAlarmBy(setter: String?) {
        val show = setter?.let {
            PendingIntent.getActivity(context, 0, Intent(it), PendingIntent.FLAG_IMMUTABLE)
                .also { intent -> shadowOf(intent).setCreatorPackage(setter) }
        }
        val ring = PendingIntent.getBroadcast(context, 1, Intent("ring"), PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)
            .setAlarmClock(AlarmManager.AlarmClockInfo(tomorrowAt10.toEpochMilli(), show), ring)
    }

    @Test
    fun `the next alarm is the one a clock app set`() {
        install("com.example.clock", system = true)
        nextAlarmBy("com.example.clock")
        assertThat(ClockApp.nextAlarm(context)).isEqualTo(tomorrowAt10)
    }

    @Test
    fun `a calendar app's reminder set as an alarm clock is not the reader's alarm`() {
        // The owner's phone: "Alarm tomorrow at 10:00" with no alarm in the clock app.
        install("com.example.clock", system = true)
        nextAlarmBy("com.example.calendar")
        assertThat(ClockApp.nextAlarm(context)).isNull()
    }

    @Test
    fun `an alarm clock that says nothing of its app, or none at all, is no alarm to show`() {
        install("com.example.clock", system = true)
        assertThat(ClockApp.nextAlarm(context)).isNull()
        nextAlarmBy(null)
        assertThat(ClockApp.nextAlarm(context)).isNull()
    }

    @Test
    fun `a second clock app's alarm is the reader's too, default or not`() {
        install("com.example.clock", system = true)
        install("com.example.sleep", system = false)
        nextAlarmBy("com.example.sleep")
        assertThat(ClockApp.nextAlarm(context)).isEqualTo(tomorrowAt10)
    }
}
