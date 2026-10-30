package com.callbackdev.tempo.core.designsystem.format

import android.content.res.Resources
import android.icu.text.Collator
import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import com.callbackdev.tempo.core.designsystem.R
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/**
 * Time zones as a reader knows them, by their city (step 8.5): "Lisbon", "Lisbona", "New York", as
 * the locale's own data writes them (`TimeZoneNames.getExemplarLocationName`, CLDR's exemplar
 * cities), never a zone's id ("America/New_York") or its long name.
 */
object ZoneCities {
    /** [zone]'s city in [locale], or null for a zone that names none. */
    fun city(zone: ZoneId, locale: Locale): String? {
        val canonical = TimeZone.getCanonicalID(zone.id) ?: return null
        return TimeZoneNames.getInstance(locale).getExemplarLocationName(canonical)?.takeIf { it.isNotBlank() }
    }

    /** [zone]'s country in [locale] ("United States", "Stati Uniti"), or null for a zone with none. */
    fun country(zone: ZoneId, locale: Locale): String? = TimeZone.getCanonicalID(zone.id)?.let { regionOf(it, locale) }

    /**
     * The second clock's choices: every city of the phone's time-zone data
     * (`SystemTimeZoneType.CANONICAL_LOCATION`, one per place, its old names folded into it), in
     * [locale]'s order, each with its country and its offset at [now]. The phone's own zone is
     * not offered: its clock is already the first.
     */
    fun all(resources: Resources, locale: Locale, phone: ZoneId, now: Instant): List<ZoneCity> {
        val names = TimeZoneNames.getInstance(locale)
        val own = TimeZone.getCanonicalID(phone.id)
        val collator = Collator.getInstance(locale)
        return TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null)
            .asSequence()
            .filter { it != own }
            .mapNotNull { id ->
                val zone = runCatching { ZoneId.of(id) }.getOrNull() ?: return@mapNotNull null
                val city = names.getExemplarLocationName(id)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ZoneCity(id, city, regionOf(id, locale), offset(resources, zone, now))
            }
            .sortedWith { a, b -> collator.compare(a.city, b.city).takeIf { it != 0 } ?: a.id.compareTo(b.id) }
            .toList()
    }

    /** "UTC+1", "UTC−3", "UTC+5:30"; "UTC" where the zone is on Greenwich's clock at [now]. */
    fun offset(resources: Resources, zone: ZoneId, now: Instant): String {
        val seconds = zone.rules.getOffset(now).totalSeconds
        val minutes = abs(seconds) / 60
        val figures = if (minutes % 60 == 0) "${minutes / 60}" else "%d:%02d".format(minutes / 60, minutes % 60)
        val sign = if (seconds < 0) MINUS else "+"
        return resources.getString(R.string.zone_offset, if (seconds == 0) "" else sign + figures)
    }

    /** The country of the system zone [id], in [locale]; none for a zone of no country, or one ICU does not know. */
    private fun regionOf(id: String, locale: Locale): String? = runCatching { TimeZone.getRegion(id) }.getOrNull()
        ?.takeIf { it.isNotBlank() && it != WORLD }
        ?.let { runCatching { Locale.Builder().setRegion(it).build().getDisplayCountry(locale) }.getOrNull() }
        ?.takeIf { it.isNotBlank() }

    /** The typographic minus, as the locale writes an offset ("UTC−3"), read as "minus". */
    private const val MINUS = "\u2212"

    /** ICU's region for a zone with no country. */
    private const val WORLD = "001"
}

/**
 * One choice of the second clock: its zone's [id] (what is stored), its [city], its [country]
 * where it has one, and its [offset] from UTC now.
 */
data class ZoneCity(val id: String, val city: String, val country: String?, val offset: String) {
    private val searchable = listOfNotNull(city, country).map(::folded)

    /** Whether [query] is part of the city's or the country's name, accents and case aside ("zurich", "Zürich"). */
    fun matches(query: String): Boolean {
        val wanted = folded(query.trim())
        return wanted.isEmpty() || searchable.any { wanted in it }
    }
}

/** [text] without its accents, in lower case: what a search compares. */
private fun folded(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING, "").lowercase(Locale.ROOT)

private val COMBINING = Regex("\\p{Mn}+")
