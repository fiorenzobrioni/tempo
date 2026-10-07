package com.callbackdev.tempo.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.tempo.core.model.AppPalette
import com.callbackdev.tempo.core.model.CalendarChoice
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.ThemeMode
import com.callbackdev.tempo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The settings file: what is kept, what survives a restart, what a bad value reads as (§5). */
class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var file: File

    /** A repository on its own DataStore, as a fresh process would open it; [block] runs, then it closes. */
    private suspend fun TestScope.withRepository(block: suspend (SettingsRepository) -> Unit) {
        if (!::file.isInitialized) file = File(folder.root, "settings.preferences_pb")
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job),
            produceFile = { file },
        )
        block(SettingsRepository(store))
        job.cancelAndJoin()
    }

    @Test
    fun `a fresh file reads as the defaults`() = runTest {
        withRepository { assertThat(it.settings.first()).isEqualTo(UserSettings()) }
    }

    @Test
    fun `every setting survives a restart`() = runTest {
        val choice = CalendarChoice(7, "com.google", "reader@example.com", "Family | home", shown = false)
        val chosen = UserSettings(
            theme = ThemeMode.DARK,
            palette = AppPalette.PAPER,
            dynamicColor = true,
            clockFormat = ClockFormat.H12,
            dateStyle = DateStyle.NUMERIC,
            horizonDays = 2,
            calendarChoices = listOf(choice),
            showDeclined = true,
            showAllDay = false,
            showNextAlarm = false,
            onboardingCompleted = true,
            askedCalendarPermission = true,
        )
        withRepository { repository -> repository.update { chosen } }
        withRepository { repository -> assertThat(repository.settings.first()).isEqualTo(chosen) }
    }

    @Test
    fun `a setting back at its default leaves no key behind`() = runTest {
        withRepository { repository ->
            repository.update { it.copy(clockFormat = ClockFormat.H24) }
            repository.update { it.copy(clockFormat = ClockFormat.SYSTEM) }
        }
        val raw = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }).data.first()
        assertThat(raw.asMap()).isEmpty()
    }

    @Test
    fun `values the app would not accept read as defaults`() = runTest {
        val job = Job()
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job),
            produceFile = { File(folder.root, "settings.preferences_pb").also { file = it } },
        ).edit {
            it[intPreferencesKey("horizon_days")] = 5
            it[stringPreferencesKey("clock_format")] = "H36"
        }
        job.cancelAndJoin()
        withRepository { repository ->
            val settings = repository.settings.first()
            assertThat(settings.horizonDays).isEqualTo(UserSettings.DEFAULT_HORIZON_DAYS)
            assertThat(settings.clockFormat).isEqualTo(ClockFormat.SYSTEM)
        }
    }

    @Test
    fun `an update is applied to what is stored, not to a stale copy`() = runTest {
        withRepository { repository ->
            repository.update { it.copy(showDeclined = true) }
            val after = repository.update { it.copy(showAllDay = false) }
            assertThat(after.showDeclined).isTrue()
            assertThat(after.showAllDay).isFalse()
        }
    }
}
