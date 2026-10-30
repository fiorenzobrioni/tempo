package com.callbackdev.tempo.core.calendar

import android.content.Context
import android.database.ContentObserver
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tick each time the calendar changes, for a screen that is showing it (PLANNING.md §4.5): a
 * `ContentObserver` on the whole provider, registered when the flow is collected and gone when
 * the collector stops, so nothing of it outlives the page (§9). A sync writes many rows in a
 * burst; the ticks are debounced so the page reads once per burst.
 *
 * The widgets do not use this: with no page on screen there is no collector, and they are told by
 * the content-URI-triggered work instead (§7).
 */
@Singleton
class CalendarChanges @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * The ticks, watched afresh each time [restarts] emits (a page's every return, a permission
     * just answered). Without `READ_CALENDAR` there is nothing to watch and [flow] ends at once;
     * granted later, from the page's own card or the system's settings, the next restart watches
     * again, so the page stays live without being left and reopened (review of 30 Oct 2026).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun flow(restarts: Flow<*>): Flow<Unit> = restarts.flatMapLatest { flow() }

    @OptIn(FlowPreview::class)
    fun flow(): Flow<Unit> = callbackFlow {
        // Android refuses an observer on the calendar without the permission; asked first, so
        // the answer is the same on every phone and in the tests.
        if (!CalendarAccess.isGranted(context)) {
            close()
            return@callbackFlow
        }
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        try {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {
            // Revoked in between: nothing to watch until the next restart.
            close()
            return@callbackFlow
        }
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate().debounce(DEBOUNCE_MILLIS)

    companion object {
        /** Long enough to join a sync's burst of writes, short enough to feel immediate. */
        const val DEBOUNCE_MILLIS = 300L
    }
}
