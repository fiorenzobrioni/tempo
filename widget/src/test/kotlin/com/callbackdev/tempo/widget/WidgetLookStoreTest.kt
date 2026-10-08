package com.callbackdev.tempo.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WidgetLookStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        WidgetLookStore(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(folder.root, "widgets.preferences_pb")
            },
        )
    }

    @After
    fun close() = scope.cancel()

    @Test
    fun `a card nobody configured wears the family's defaults and follows Tempo's formats`() = runTest {
        val look = store.lookFor(7)
        assertThat(look).isEqualTo(WidgetLook())
        assertThat(look.background).isEqualTo(WidgetBackground.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.BLUE)
        assertThat(look.clockFormat).isNull()
        assertThat(look.dateStyle).isNull()
        assertThat(look.headerTap).isEqualTo(HeaderTap.TEMPO)
        assertThat(look.eventTap).isEqualTo(EventTap.CALENDAR)
    }

    @Test
    fun `each card keeps its own look, its clock app included`() = runTest {
        val clay = WidgetLook(
            cardColor = WidgetCardColor.CLAY,
            opacityPct = 40,
            clockFormat = ClockFormat.H12,
            headerTap = HeaderTap.CLOCK,
            showDaysAhead = false,
        )
        val light = WidgetLook(
            background = WidgetBackground.LIGHT,
            showClock = false,
            dateStyle = DateStyle.NUMERIC,
            eventTap = EventTap.TEMPO,
        )
        store.set(1, clay)
        store.set(2, light)
        assertThat(store.lookFor(1)).isEqualTo(clay)
        assertThat(store.lookFor(2)).isEqualTo(light)
    }

    @Test
    fun `a format set back to Tempo's is forgotten, not kept`() = runTest {
        store.set(5, WidgetLook(clockFormat = ClockFormat.H24))
        store.set(5, WidgetLook())
        assertThat(store.lookFor(5).clockFormat).isNull()
    }

    @Test
    fun `a removed card leaves nothing behind, and only its own`() = runTest {
        store.set(3, WidgetLook(showAllDay = false))
        store.set(13, WidgetLook(showAllDay = false))
        store.forget(intArrayOf(3))
        assertThat(store.lookFor(3)).isEqualTo(WidgetLook())
        assertThat(store.lookFor(13).showAllDay).isFalse()
    }

    @Test
    fun `an opacity out of range is clamped`() = runTest {
        store.set(4, WidgetLook(opacityPct = 140))
        assertThat(store.lookFor(4).opacityPct).isEqualTo(100)
    }
}
