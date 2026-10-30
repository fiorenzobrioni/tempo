package com.callbackdev.tempo.core.calendar

import android.app.Application
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

/** The phone's clock app, the door of Today's alarm line and of a card's time. */
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
}
