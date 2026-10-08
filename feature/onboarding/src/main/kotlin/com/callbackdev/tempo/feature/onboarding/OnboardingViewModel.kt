package com.callbackdev.tempo.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tempo.core.data.settings.SettingsRepository
import com.callbackdev.tempo.core.data.widget.HomeScreenWidgets
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The first run's two writes (the permission was asked, the first run is over), and its one
 * request to the launcher: a widget, placed where the reader says in the launcher's own dialog.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val widgets: HomeScreenWidgets,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun permissionAnswered() {
        viewModelScope.launch { repository.update { it.copy(askedCalendarPermission = true) } }
    }

    fun widgetOffer(): WidgetOffer = WidgetOffer(canPin = widgets.canPin(), placed = widgets.placed())

    fun pin(widget: TempoWidget) {
        widgets.pin(widget)
    }

    /** The shell turns to Today as soon as this is stored. */
    fun finish() {
        viewModelScope.launch { repository.update { it.copy(onboardingCompleted = true) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
