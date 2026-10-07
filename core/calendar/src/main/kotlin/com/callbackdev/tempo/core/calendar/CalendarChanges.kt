package com.callbackdev.tempo.core.calendar

import android.content.Context
import android.database.ContentObserver
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
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
    @OptIn(FlowPreview::class)
    fun flow(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        try {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {
            // No permission to watch: nothing will change that the page could show.
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
