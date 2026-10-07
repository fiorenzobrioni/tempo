package com.callbackdev.tempo.core.data.settings

import com.callbackdev.tempo.core.model.CalendarChoice
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * One calendar choice as one string of a DataStore string set: a version, then the fields, each
 * URL-encoded so an account or a calendar name with a `|` in it cannot break the line. A line
 * that does not read back (a newer version's, a damaged one) is dropped: the calendar then
 * follows the calendar app, which is the default and never hides an event by mistake.
 */
internal object CalendarChoiceCodec {
    private const val VERSION = "1"
    private const val SEPARATOR = "|"

    fun encode(choice: CalendarChoice): String = listOf(
        VERSION,
        if (choice.shown) "shown" else "hidden",
        choice.calendarId.toString(),
        choice.accountType.encoded(),
        choice.accountName.encoded(),
        choice.name.encoded(),
    ).joinToString(SEPARATOR)

    fun decode(line: String): CalendarChoice? {
        val parts = line.split(SEPARATOR)
        if (parts.size != 6 || parts[0] != VERSION) return null
        val shown = when (parts[1]) {
            "shown" -> true
            "hidden" -> false
            else -> return null
        }
        return runCatching {
            CalendarChoice(
                calendarId = parts[2].toLong(),
                accountType = parts[3].decoded(),
                accountName = parts[4].decoded(),
                name = parts[5].decoded(),
                shown = shown,
            )
        }.getOrNull()
    }

    private fun String.encoded() = URLEncoder.encode(this, Charsets.UTF_8)

    private fun String.decoded() = URLDecoder.decode(this, Charsets.UTF_8)
}
