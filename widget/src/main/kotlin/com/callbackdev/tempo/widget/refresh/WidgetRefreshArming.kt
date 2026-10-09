package com.callbackdev.tempo.widget.refresh

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.callbackdev.tempo.core.domain.agenda.NextBoundary
import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.widget.TempoWidgets
import com.callbackdev.tempo.widget.logWidgetFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/*
 * How the cards stay fresh (PLANNING.md §7, §9): pushed, never polled. Four ways in, nothing else:
 *
 * - the calendar changed, told by the provider itself: its `PROVIDER_CHANGED` broadcast, about a
 *   second after an app's edit (half a minute after a sync's), which the provider sends to
 *   receivers in the manifest too (`FLAG_RECEIVER_INCLUDE_BACKGROUND`, PLANNING.md §15, 9 Oct
 *   2026). The fast way: nothing waits on the job scheduler;
 * - the calendar changed, seen by a one-shot WorkManager job with a content-URI trigger on the
 *   provider: the safety net, for a maker's provider that does not send the broadcast. It costs
 *   nothing while nothing changes, and the next one is armed as soon as one starts (a content
 *   trigger fires once by design);
 * - the agenda changes by itself (an event starts or ends, a day begins): one inexact,
 *   non-wakeup alarm at the next boundary, delivered when the phone is next awake, which is when
 *   someone can see the card;
 * - the clock, the zone or the language changed, or the app was updated: the exempt broadcasts.
 *
 * Whatever asked stays until the cards are drawn (`TempoWidgets.repaintAll`): Glance draws in a
 * worker of Tempo's process that no job holds awake, and a process left cached may be frozen
 * with the card half drawn. The cards' clocks need none of it: they are the system's `TextClock`.
 */

/** Arms and disarms the two things a card leaves behind it: the calendar's trigger and the boundary alarm. */
@Singleton
class WidgetRefreshArming @Inject constructor(@ApplicationContext private val context: Context) {
    /** After a card is drawn: the calendar's trigger, and the alarm at [agenda]'s next boundary. */
    fun afterRender(agenda: Agenda?, zone: ZoneId) {
        Arming.launch { armCalendarTrigger(context) }
        if (agenda != null) armBoundary(NextBoundary.after(agenda, zone))
    }

    /** The last card is gone: nothing of Tempo waits on a calendar no card shows. */
    fun disarm() {
        runCatching {
            val work = WorkManager.getInstance(context)
            work.cancelAllWorkByTag(CALENDAR_TRIGGER)
            work.cancelUniqueWork(LEGACY_CALENDAR_WORK)
        }
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
        /** The tag of the calendar's trigger: one waiting at a time ([armCalendarTrigger]). */
        internal const val CALENDAR_TRIGGER = "tempo-widget-calendar-trigger"

        /** The single unique work of the first version, appended after each run; cancelled where found. */
        private const val LEGACY_CALENDAR_WORK = "tempo-widget-calendar-changed"

        /** How long a job or a receiver stays for the cards it asked to repaint. */
        internal const val JOB_DRAW_TIMEOUT_MILLIS = 20_000L
        internal const val RECEIVER_DRAW_TIMEOUT_MILLIS = 8_000L

        private val ArmLock = Mutex()
        private val Arming = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /**
         * The calendar's trigger: exactly one waiting. Tagged work rather than one unique name, so
         * the job that is running can arm the next before it repaints (a unique name would have
         * to wait behind it, and a change during its run went unseen until the next boundary).
         * The lock keeps two cards drawn at once from arming two. With [replaceWaiting], the one
         * waiting is dropped first: the provider's broadcast has told of the change it would fire
         * for, and a second repaint of the same change is work for nothing.
         *
         * A sync writes many rows at once: the job waits for the provider to be quiet for [QUIET]
         * (and never longer than [LONGEST] after the first change), so a sync is one repaint.
         */
        internal suspend fun armCalendarTrigger(context: Context, replaceWaiting: Boolean = false) {
            try {
                ArmLock.withLock {
                    val work = WorkManager.getInstance(context)
                    val waiting = work.getWorkInfosByTagFlow(CALENDAR_TRIGGER).first()
                        .filter { it.state == WorkInfo.State.ENQUEUED }
                    if (waiting.isNotEmpty() && !replaceWaiting) return@withLock
                    waiting.forEach { work.cancelWorkById(it.id).await() }
                    work.cancelUniqueWork(LEGACY_CALENDAR_WORK)
                    work.enqueue(calendarTrigger()).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logWidgetFailure("Arming the calendar's trigger failed", e)
            }
        }

        private fun calendarTrigger(): OneTimeWorkRequest = OneTimeWorkRequest.Builder(CalendarChangeWorker::class.java)
            .addTag(CALENDAR_TRIGGER)
            .setConstraints(
                Constraints.Builder()
                    .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
                    .setTriggerContentUpdateDelay(QUIET)
                    .setTriggerContentMaxDelay(LONGEST)
                    .build(),
            )
            .build()

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
 * The calendar changed: the next trigger armed first, so a change during this run is seen, then
 * every card drawn again, and the job kept until they are. With no card left, it arms nothing.
 */
class CalendarChangeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TempoWidgets.hasWidgets(applicationContext)) return Result.success()
        WidgetRefreshArming.armCalendarTrigger(applicationContext)
        TempoWidgets.repaintAll(
            applicationContext,
            "Calendar changed (content trigger)",
            WidgetRefreshArming.JOB_DRAW_TIMEOUT_MILLIS,
        )
        return Result.success()
    }
}

/**
 * The provider's own word that the calendar changed (`PROVIDER_CHANGED` on
 * `content://com.android.calendar`). The waiting trigger is replaced before the repaint, so the
 * change is drawn once and a change after it is still seen.
 */
class CalendarChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PROVIDER_CHANGED) return
        repaint(context, "Calendar changed (broadcast)") {
            WidgetRefreshArming.armCalendarTrigger(it, replaceWaiting = true)
        }
    }
}

/** The boundary alarm: the agenda changed by itself (an event began or ended, a day began). */
class BoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = repaint(context, "Boundary")
}

/**
 * The exempt broadcasts (PLANNING.md §7): the clock set, the zone changed (a trip moves the
 * days), the language changed, the app updated. Received from the manifest, delivered by the
 * system only.
 */
class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action in Actions) repaint(context, action.orEmpty().substringAfterLast('.'))
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

/**
 * Every card drawn again within the receiver's own time (`goAsync`), never a service: [before]
 * first, then the repaint, and the broadcast held open until the cards are drawn.
 */
private fun BroadcastReceiver.repaint(context: Context, why: String, before: (suspend (Context) -> Unit)? = null) {
    val app = context.applicationContext
    if (!TempoWidgets.hasWidgets(app)) return
    val pending = goAsync()
    Repaints.launch {
        try {
            before?.invoke(app)
            TempoWidgets.repaintAll(app, why, WidgetRefreshArming.RECEIVER_DRAW_TIMEOUT_MILLIS)
        } finally {
            pending.finish()
        }
    }
}

private val Repaints = CoroutineScope(SupervisorJob() + Dispatchers.Default)
