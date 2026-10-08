package com.callbackdev.tempo.widget

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.provider.AlarmClock
import android.widget.AnalogClock
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.widget.refresh.WidgetRefreshArming
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale

/**
 * The doors a card's touches open, its live clock, and the one alarm it leaves behind: what no
 * picture shows and the launcher alone would.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB")
class WidgetDoorsTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun installClock(packageName: String, system: Boolean) {
        val app = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
        }
        val activity = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
            applicationInfo = app
        }
        val info = ResolveInfo().apply { activityInfo = activity }
        val manager = shadowOf(context.packageManager)
        manager.addResolveInfoForIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS), info)
        manager.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
            ResolveInfo().apply { activityInfo = activity },
        )
    }

    @Test
    fun `the clock app is the one that shows the alarms, opened by its front door`() {
        installClock("com.example.clock", system = true)
        assertThat(ClockApp.packageName(context)).isEqualTo("com.example.clock")
        val open = ClockApp.open(context, "com.example.clock")
        assertThat(open?.component).isEqualTo(ComponentName("com.example.clock", "com.example.clock.Main"))
        assertThat(open?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK)).isNotEqualTo(0)
    }

    @Test
    fun `with no clock app the header falls back to Tempo, never to nothing`() {
        assertThat(ClockApp.packageName(context)).isNull()
        val doors = WidgetDoors(canOpenEvent = false, canOpenDay = false, clockPackage = null)
        val tap = WidgetIntents.header(context, HeaderTap.CLOCK, doors, Instant.EPOCH)
        assertThat(tap?.`package` ?: tap?.component?.packageName).isNotEqualTo("com.example.clock")
        assertThat(WidgetIntents.header(context, HeaderTap.CALENDAR, doors, Instant.EPOCH)?.action)
            .isNotEqualTo(Intent.ACTION_VIEW)
    }

    @Test
    fun `two occurrences of one event are two doors, each with its own times`() {
        fun standup(day: Long) = EventInstance(
            eventId = 42,
            calendarId = 1,
            title = "Standup",
            location = null,
            begin = Instant.parse("2026-10-07T07:30:00Z").plusSeconds(day * 86_400),
            end = Instant.parse("2026-10-07T07:45:00Z").plusSeconds(day * 86_400),
            allDay = false,
            color = null,
        )
        val doors = WidgetDoors(canOpenEvent = true, canOpenDay = true, clockPackage = null)
        val today = checkNotNull(WidgetIntents.event(context, EventTap.CALENDAR, doors, standup(0)))
        val tomorrow = checkNotNull(WidgetIntents.event(context, EventTap.CALENDAR, doors, standup(1)))
        assertThat(today.data).isEqualTo(tomorrow.data)
        assertThat(today.filterEquals(tomorrow)).isFalse()
    }

    @Test
    fun `the live clock is the system's TextClock, set with the locale's patterns`() {
        val patterns = ClockPatterns.time(Locale.UK, ClockFormat.H24)
        val views = clockViews(context, ClockFace.BOLD, patterns, 40f, Color.White, 1, null, frozenAt = null)
        val view = views.apply(context, FrameLayout(context))
        assertThat(view).isInstanceOf(TextClock::class.java)
        val clock = view as TextClock
        assertThat(clock.format24Hour.toString()).isEqualTo("HH:mm")
        assertThat(clock.format12Hour.toString()).isEqualTo("HH:mm")
        assertThat(clock.maxLines).isEqualTo(1)
    }

    @Test
    fun `the phone's own format leaves both patterns to the phone's switch`() {
        val patterns = ClockPatterns.time(Locale.US, ClockFormat.SYSTEM)
        assertThat(patterns.twelve).isEqualTo("h:mm a")
        assertThat(patterns.twentyFour).isEqualTo("HH:mm")
    }

    @Test
    fun `a frozen clock is plain text at the sample's time, for the previews and the pictures`() {
        val patterns = ClockPatterns.time(Locale.UK, ClockFormat.H24)
        val views = clockViews(context, ClockFace.BOLD, patterns, 40f, Color.White, 1, null, WidgetSamples.Now)
        val view = views.apply(context, FrameLayout(context))
        assertThat(view).isNotInstanceOf(TextClock::class.java)
        assertThat((view as TextView).text.toString()).isEqualTo("10:20")
    }

    @Test
    fun `the live dial is the system's AnalogClock in the card's ink, over the painted face`() {
        val palette = WidgetPalette(Color.White, Color.Gray, Color.Red, darkGround = true, markGround = Color.Black)
        val arcs = listOf(com.callbackdev.tempo.core.domain.widget.DialArc(600f, 60f, focus = true))
        val views = dialViews(context, 56f, arcs, palette, null, "Clock", frozenAt = null)
        val frame = views.apply(context, FrameLayout(context)) as FrameLayout
        assertThat(frame.contentDescription.toString()).isEqualTo("Clock")
        val hands = (0 until frame.childCount).map { frame.getChildAt(it) }.filterIsInstance<AnalogClock>().single()
        assertThat(hands.hourHandTintList?.defaultColor).isEqualTo(android.graphics.Color.WHITE)
        assertThat(hands.minuteHandTintList?.defaultColor).isEqualTo(android.graphics.Color.WHITE)
        val face = (0 until frame.childCount).map { frame.getChildAt(it) }.filterIsInstance<ImageView>().single()
        assertThat(face.drawable).isNotNull()
    }

    @Test
    fun `a frozen dial has no AnalogClock, the hands are painted at the sample's moment`() {
        val palette = WidgetPalette(Color.White, Color.Gray, Color.Red, darkGround = true, markGround = Color.Black)
        val views = dialViews(context, 56f, emptyList(), palette, null, "Clock", WidgetSamples.Now.toLocalTime())
        val frame = views.apply(context, FrameLayout(context)) as FrameLayout
        assertThat((0 until frame.childCount).map { frame.getChildAt(it) }.filterIsInstance<AnalogClock>()).isEmpty()
    }

    @Test
    fun `a card arms one inexact, non-wakeup alarm, at the next boundary`() {
        val arming = WidgetRefreshArming(context)
        val agenda = (WidgetSamples.model().content as WidgetContent.Ready).agenda
        arming.afterRender(agenda, WidgetSamples.Now.zone)
        val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val alarm = checkNotNull(alarms.peekNextScheduledAlarm())
        // Design review ends at 11:00, the sample's next boundary after 10:20.
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC)
        assertThat(alarm.triggerAtMs).isEqualTo(WidgetSamples.at("2026-10-07T11:00").toInstant().toEpochMilli())
        assertThat(alarms.scheduledAlarms).hasSize(1)
        arming.afterRender(agenda, WidgetSamples.Now.zone)
        assertThat(alarms.scheduledAlarms).hasSize(1)
        arming.disarm()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }
}
