package com.callbackdev.tempo.core.calendar

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * A stand-in for the system Calendar Provider, registered under its real authority
 * (`com.android.calendar`) by the tests, so the queries go through the real `ContentResolver`
 * path: the URI, the projection, the cursor read by column name (PLANNING.md §11 Phase 1).
 *
 * Rows are maps of column to value; a column the test lists in [missing] is left out of the
 * cursor, as a maker's provider without it would.
 */
class FakeCalendarProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (revoked) throw SecurityException("Permission Denial: reading com.android.providers.calendar")
        queries += uri
        val columns = projection.orEmpty().filterNot { it in missing }
        val rows = when (uri.pathSegments.firstOrNull()) {
            "calendars" -> calendars
            "instances" -> instances
            else -> emptyList()
        }
        return MatrixCursor(columns.toTypedArray()).apply {
            rows.forEach { row -> addRow(columns.map { row[it] }) }
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Tempo never writes the calendar")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Tempo never writes the calendar")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Tempo never writes the calendar")

    companion object {
        var calendars: List<Map<String, Any?>> = emptyList()
        var instances: List<Map<String, Any?>> = emptyList()
        var missing: Set<String> = emptySet()
        var revoked = false
        val queries = mutableListOf<Uri>()

        fun reset() {
            calendars = emptyList()
            instances = emptyList()
            missing = emptySet()
            revoked = false
            queries.clear()
        }
    }
}
