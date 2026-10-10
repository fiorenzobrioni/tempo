package com.callbackdev.tempo.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.callbackdev.tempo.core.model.AppFont
import com.callbackdev.tempo.core.model.AppPalette
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.ThemeMode
import com.callbackdev.tempo.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reader's settings in DataStore (PLANNING.md §5): only what differs
 * from the default is stored, so a default improved in a later version reaches everyone who has
 * not moved away from it; a value that cannot be read back (out of range, an enum from a newer
 * build) reads as its default.
 */
@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings> = dataStore.data
        // An unreadable file is not a reason to stop: the defaults stand in.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)
        .distinctUntilChanged()

    /** Applies [transform] to the stored settings atomically; returns the settings now stored. */
    suspend fun update(transform: (UserSettings) -> UserSettings): UserSettings {
        var updated = UserSettings()
        dataStore.edit { prefs ->
            val old = read(prefs)
            val new = transform(old).sanitized()
            write(prefs, old, new)
            updated = new
        }
        return updated
    }

    private fun read(prefs: Preferences): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            theme = prefs[Keys.THEME].toEnumOrNull<ThemeMode>() ?: defaults.theme,
            palette = prefs[Keys.PALETTE].toEnumOrNull<AppPalette>() ?: defaults.palette,
            font = prefs[Keys.FONT].toEnumOrNull<AppFont>() ?: defaults.font,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            clockFormat = prefs[Keys.CLOCK_FORMAT].toEnumOrNull<ClockFormat>() ?: defaults.clockFormat,
            dateStyle = prefs[Keys.DATE_STYLE].toEnumOrNull<DateStyle>() ?: defaults.dateStyle,
            horizonDays = prefs[Keys.HORIZON_DAYS] ?: defaults.horizonDays,
            calendarChoices = prefs[Keys.CALENDAR_CHOICES].orEmpty().mapNotNull(CalendarChoiceCodec::decode)
                .sortedBy { it.calendarId },
            showDeclined = prefs[Keys.SHOW_DECLINED] ?: defaults.showDeclined,
            showAllDay = prefs[Keys.SHOW_ALL_DAY] ?: defaults.showAllDay,
            showNextAlarm = prefs[Keys.SHOW_NEXT_ALARM] ?: defaults.showNextAlarm,
            showCalendarButton = prefs[Keys.SHOW_CALENDAR_BUTTON] ?: defaults.showCalendarButton,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            askedCalendarPermission = prefs[Keys.ASKED_CALENDAR_PERMISSION] ?: defaults.askedCalendarPermission,
        ).sanitized()
    }

    private fun write(prefs: MutablePreferences, old: UserSettings, new: UserSettings) {
        val d = UserSettings()
        prefs.write(Keys.THEME, old.theme.name, new.theme.name, d.theme.name)
        prefs.write(Keys.PALETTE, old.palette.name, new.palette.name, d.palette.name)
        prefs.write(Keys.FONT, old.font.name, new.font.name, d.font.name)
        prefs.write(Keys.DYNAMIC_COLOR, old.dynamicColor, new.dynamicColor, d.dynamicColor)
        prefs.write(Keys.CLOCK_FORMAT, old.clockFormat.name, new.clockFormat.name, d.clockFormat.name)
        prefs.write(Keys.DATE_STYLE, old.dateStyle.name, new.dateStyle.name, d.dateStyle.name)
        prefs.write(Keys.HORIZON_DAYS, old.horizonDays, new.horizonDays, d.horizonDays)
        prefs.write(
            Keys.CALENDAR_CHOICES,
            old.calendarChoices.map(CalendarChoiceCodec::encode).toSet(),
            new.calendarChoices.map(CalendarChoiceCodec::encode).toSet(),
            emptySet(),
        )
        prefs.write(Keys.SHOW_DECLINED, old.showDeclined, new.showDeclined, d.showDeclined)
        prefs.write(Keys.SHOW_ALL_DAY, old.showAllDay, new.showAllDay, d.showAllDay)
        prefs.write(Keys.SHOW_NEXT_ALARM, old.showNextAlarm, new.showNextAlarm, d.showNextAlarm)
        prefs.write(Keys.SHOW_CALENDAR_BUTTON, old.showCalendarButton, new.showCalendarButton, d.showCalendarButton)
        prefs.write(Keys.ONBOARDING_COMPLETED, old.onboardingCompleted, new.onboardingCompleted, d.onboardingCompleted)
        prefs.write(
            Keys.ASKED_CALENDAR_PERMISSION,
            old.askedCalendarPermission,
            new.askedCalendarPermission,
            d.askedCalendarPermission,
        )
    }

    /** Writes only a field that changed; back at its default, the key goes. */
    private fun <T : Any> MutablePreferences.write(key: Preferences.Key<T>, old: T, new: T, default: T) {
        if (old == new) return
        if (new == default) remove(key) else set(key, new)
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val DATE_STYLE = stringPreferencesKey("date_style")
        val HORIZON_DAYS = intPreferencesKey("horizon_days")
        val CALENDAR_CHOICES = stringSetPreferencesKey("calendar_choices")
        val SHOW_DECLINED = booleanPreferencesKey("show_declined")
        val SHOW_ALL_DAY = booleanPreferencesKey("show_all_day")
        val SHOW_NEXT_ALARM = booleanPreferencesKey("show_next_alarm")
        val SHOW_CALENDAR_BUTTON = booleanPreferencesKey("show_calendar_button")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ASKED_CALENDAR_PERMISSION = booleanPreferencesKey("asked_calendar_permission")
    }

    companion object {
        /** The DataStore file's name, under the app's files directory: the backup's one file. */
        const val FILE_NAME = "settings"
    }
}

/** These settings with every value the app would not accept replaced by its default. */
internal fun UserSettings.sanitized(): UserSettings = copy(
    horizonDays = horizonDays.takeIf { it in UserSettings.HORIZON_CHOICES } ?: UserSettings.DEFAULT_HORIZON_DAYS,
)

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
