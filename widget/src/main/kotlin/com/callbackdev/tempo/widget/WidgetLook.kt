package com.callbackdev.tempo.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The widgets' file (PLANNING.md §5): not in the backup's allowlist, because a restore renews the
 * widget ids it is keyed by.
 */
internal val Context.widgetLookDataStore by preferencesDataStore(name = "widgets")

/**
 * What a card is painted on: a plain card in the app's light or dark surface, or whichever the
 * phone is in, or one of the six colours of [WidgetCardColor].
 */
enum class WidgetBackground { LIGHT, DARK, SYSTEM, COLOR }

/**
 * What a touch on the clock and the date opens (VISION.md, the widgets' settings): Tempo's Today,
 * the calendar app at today, or the phone's clock app, where its alarms are (owner, 8 Oct 2026:
 * the time is the clock's, and the clock app is what a reader reaches for from it).
 */
enum class HeaderTap { TEMPO, CALENDAR, CLOCK }

/** What a touch on an event opens: Tempo's Today, or that occurrence in the calendar app. */
enum class EventTap { TEMPO, CALENDAR }

/**
 * One card's look and content, chosen from the launcher's reconfigure flow and kept per widget,
 * so the same card can sit on a home screen twice, dressed twice. The dress default is a
 * solid blue card (`docs/adr/0003-widgets.md`), worn before anybody configures anything.
 *
 * @property clockFormat the card's own clock format; null follows Tempo's setting.
 * @property dateStyle the card's own date style; null follows Tempo's setting.
 * @property eventTap the calendar app by default: on Today a touch on an event opens it there too.
 * @property showAllDay can only take away: with Tempo's own switch off there are none to show.
 * @property showDaysAhead the days after today, in the room today leaves (owner, PLANNING.md §15).
 * @property cardColor kept while another background is picked, so coming back finds it.
 */
data class WidgetLook(
    val background: WidgetBackground = WidgetBackground.COLOR,
    val opacityPct: Int = DEFAULT_OPACITY,
    val cardColor: WidgetCardColor = WidgetCardColor.BLUE,
    val showClock: Boolean = true,
    val clockFormat: ClockFormat? = null,
    val showDate: Boolean = true,
    val dateStyle: DateStyle? = null,
    val headerTap: HeaderTap = HeaderTap.TEMPO,
    val eventTap: EventTap = EventTap.CALENDAR,
    val showAllDay: Boolean = true,
    val showDaysAhead: Boolean = true,
) {
    companion object {
        /** Solid: below [INK_TRUST_FLOOR_PCT] the ink has to ask the wallpaper, a different conversation. */
        const val DEFAULT_OPACITY: Int = 100
    }
}

/**
 * The looks, keyed by appWidgetId. Presentation only, so it lives with the widgets and not in
 * `:core:data`; a removed widget takes its look with it ([forget]).
 */
@Singleton
class WidgetLookStore internal constructor(private val dataStore: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.widgetLookDataStore)

    suspend fun lookFor(appWidgetId: Int): WidgetLook {
        val prefs = dataStore.data.first()
        val default = WidgetLook()
        val id = appWidgetId
        return WidgetLook(
            background = enumOf(prefs[key("bg", id)], default.background),
            opacityPct = (prefs[intKey("opacity", id)] ?: default.opacityPct).coerceIn(0, 100),
            cardColor = enumOf(prefs[key("card_color", id)], default.cardColor),
            showClock = prefs[boolKey("clock", id)] ?: default.showClock,
            clockFormat = prefs[key("clock_format", id)]?.let { name ->
                ClockFormat.entries.firstOrNull {
                    it.name ==
                        name
                }
            },
            showDate = prefs[boolKey("date", id)] ?: default.showDate,
            dateStyle = prefs[key("date_style", id)]?.let { name -> DateStyle.entries.firstOrNull { it.name == name } },
            headerTap = enumOf(prefs[key("header_tap", id)], default.headerTap),
            eventTap = enumOf(prefs[key("event_tap", id)], default.eventTap),
            showAllDay = prefs[boolKey("all_day", id)] ?: default.showAllDay,
            showDaysAhead = prefs[boolKey("days_ahead", id)] ?: default.showDaysAhead,
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        val id = appWidgetId
        dataStore.edit { prefs ->
            prefs[key("bg", id)] = look.background.name
            prefs[intKey("opacity", id)] = look.opacityPct.coerceIn(0, 100)
            prefs[key("card_color", id)] = look.cardColor.name
            prefs[boolKey("clock", id)] = look.showClock
            look.clockFormat?.let { prefs[key("clock_format", id)] = it.name } ?: prefs.remove(key("clock_format", id))
            prefs[boolKey("date", id)] = look.showDate
            look.dateStyle?.let { prefs[key("date_style", id)] = it.name } ?: prefs.remove(key("date_style", id))
            prefs[key("header_tap", id)] = look.headerTap.name
            prefs[key("event_tap", id)] = look.eventTap.name
            prefs[boolKey("all_day", id)] = look.showAllDay
            prefs[boolKey("days_ahead", id)] = look.showDaysAhead
        }
    }

    /** From the receivers' `onDeleted`: a removed widget leaves nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            val ids = appWidgetIds.map { "_$it" }
            prefs.asMap().keys.filter { key -> ids.any { key.name.endsWith(it) } }.forEach { prefs.remove(it) }
        }
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private fun key(name: String, id: Int) = stringPreferencesKey("${name}_$id")
    private fun intKey(name: String, id: Int) = intPreferencesKey("${name}_$id")
    private fun boolKey(name: String, id: Int) = booleanPreferencesKey("${name}_$id")
}
