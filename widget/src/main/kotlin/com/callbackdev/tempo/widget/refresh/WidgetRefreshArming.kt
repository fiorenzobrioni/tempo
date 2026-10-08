package com.callbackdev.tempo.widget.refresh

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callbackdev.tempo.core.domain.agenda.NextBoundary
import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.widget.TempoWidgets
import com.callbackdev.tempo.widget.logWidgetFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/*
 * How the cards stay fresh (PLANNING.md §7, §9): pushed, never polled. Three ways in, nothing else:
 *
 * - the calendar changed: a one-shot WorkManager job with a content-URI trigger on the Calendar
 *   Provider, which costs nothing while nothing changes, and arms the next one after it runs
 *   (a content trigger fires once by design; `PROVIDER_CHANGED` cannot be received from the
 *   manifest since Android 8);
 * - the agenda changes by itself (an event starts or ends, a day begins): one inexact,
 *   non-wakeup alarm at the next boundary, delivered when the phone is next awake, which is when
 *   someone can see the card;
 * - the clock, the zone or the language changed, or the app was updated: the exempt broadcasts.
 *
 * The cards' clocks need none of it: they are the system's `TextClock`.
 */

/** Arms and disarms the two things a card leaves behind it: the calendar's trigger and the boundary alarm. */
@Singleton
class WidgetRefreshArming @Inject constructor(@ApplicationContext private val context: Context) {
    /** After a card is drawn: the calendar's trigger, and the alarm at [agenda]'s next boundary. */
    fun afterRender(agenda: Agenda?, zone: ZoneId) {
        armCalendarTrigger(context, ExistingWorkPolicy.KEEP)
        if (agenda != null) armBoundary(NextBoundary.after(agenda, zone))
    }

    /** The last card is gone: nothing of Tempo waits on a calendar no card shows. */
    fun disarm() {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(CALENDAR_WORK) }
        context.getSystemService(AlarmManager::class.java)?.cancel(boundaryIntent(context))
    }

    /**
     * `AlarmManager.set` with `RTC`: inexact and non-wakeup since Android 4.4, so it needs no
     * permission and never wakes the phone. One pending intent, so a new boundary replaces the old.
     */
    private fun armBoundary(at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarms.set(AlarmManager.RTC, at.toEpochMilli(), boundaryIntent(context)) }
            .onFailure { logWidgetFailure("Arming the boundary alarm failed", it) }
    }

    companion object {
        internal const val CALENDAR_WORK = "tempo-widget-calendar-changed"

        /**
         * The calendar's trigger. A sync writes many rows at once: the job waits for the provider to
         * be quiet for [QUIET] (and never longer than [LONGEST] after the first change), so a sync
         * is one repaint, well within the minute the vision promises (VISION.md, Success criteria).
         */
        internal fun armCalendarTrigger(context: Context, policy: ExistingWorkPolicy) {
            val request = OneTimeWorkRequest.Builder(CalendarChangeWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
                        .setTriggerContentUpdateDelay(QUIET)
                        .setTriggerContentMaxDelay(LONGEST)
                        .build(),
                )
                .build()
            runCatching { WorkManager.getInstance(context).enqueueUniqueWork(CALENDAR_WORK, policy, request) }
                .onFailure { logWidgetFailure("Arming the calendar's trigger failed", it) }
        }

        private fun boundaryIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, BoundaryReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private val QUIET: Duration = Duration.ofSeconds(3)
        private val LONGEST: Duration = Duration.ofSeconds(20)
    }
}

/**
 * The calendar changed: every card is drawn again, and the next trigger armed after this one
 * (appended, so it waits for this run to end rather than cancelling it). With no card left, it
 * arms nothing.
 */
class CalendarChangeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TempoWidgets.hasWidgets(applicationContext)) return Result.success()
        TempoWidgets.updateAll(applicationContext)
        WidgetRefreshArming.armCalendarTrigger(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}

/** The boundary alarm: the agenda changed by itself (an event began or ended, a day began). */
class BoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = repaint(context)
}

/**
 * The exempt broadcasts (PLANNING.md §7): the clock set, the zone changed (a trip moves the
 * days), the language changed, the app updated. Received from the manifest, delivered by the
 * system only.
 */
class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in Actions) repaint(context)
    }

    private companion object {
        val Actions = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/** Every card drawn again, within the receiver's own time (`goAsync`), never a service. */
private fun BroadcastReceiver.repaint(context: Context) {
    val app = context.applicationContext
    if (!TempoWidgets.hasWidgets(app)) return
    val pending = goAsync()
    Repaints.launch {
        try {
            TempoWidgets.updateAll(app)
        } finally {
            pending.finish()
        }
    }
}

private val Repaints = CoroutineScope(SupervisorJob() + Dispatchers.Default)
