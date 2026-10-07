package com.callbackdev.tempo.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.calendar.CalendarChanges
import com.callbackdev.tempo.core.calendar.CalendarSource
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.domain.calendar.AccountCalendars
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarGroups
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The calendars section: the phone's calendars by account, or why there is no list. */
sealed interface CalendarsState {
    data class Listed(val accounts: List<AccountCalendars>) : CalendarsState

    data object NoPermission : CalendarsState
}

/** What Settings shows. [calendars] is null until the first read: the section waits for it. */
data class SettingsUiState(val settings: UserSettings, val calendars: CalendarsState?, val version: String)

/**
 * Settings (PLANNING.md §11 Phase 2). The settings come from DataStore, the calendars from the
 * provider, read again when the calendar changes while the page is open, and when the reader
 * comes back to it (the permission may have changed in the system's settings meanwhile).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: SettingsRepository,
    private val source: CalendarSource,
    changes: CalendarChanges,
) : ViewModel() {
    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private val rereads = MutableStateFlow(0)

    private val calendars: Flow<CalendarsState?> = merge(rereads.map { }, changes.flow()).map {
        source.readCalendars()?.let { CalendarsState.Listed(CalendarGroups.of(it)) } ?: CalendarsState.NoPermission
    }

    val state: StateFlow<SettingsUiState?> =
        combine(repository.settings, merge(flowOf(null), calendars)) { settings, calendars ->
            SettingsUiState(settings, calendars, version)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun setCalendarShown(calendar: CalendarInfo, shown: Boolean) = update {
        CalendarChoices.choose(it, calendar, shown)
    }

    /** Reads the calendars again: back on the page, or after the permission's question. */
    fun rereadCalendars() {
        rereads.value++
    }

    /** The permission was asked: remembered, so a later refusal can be told apart (§15), and the list read again. */
    fun onPermissionAnswered() {
        viewModelScope.launch {
            repository.update { it.copy(askedCalendarPermission = true) }
            rereadCalendars()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
