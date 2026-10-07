package com.callbackdev.tempo.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.uses24Hour
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.domain.clock.untilNextMinute
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Today, as Phase 2 leaves it: the clock and the date in the reader's formats, and the way to
 * Settings. The agenda, the day's sentence and the new-event button arrive with Phase 3
 * (PLANNING.md §11), on the calendar engine of Phase 1. The time is read once a minute, on the
 * minute, and only while the page is started: no ticker survives the screen going off (§9).
 */
@Composable
fun TodayRoute(
    clockFormat: ClockFormat = ClockFormat.SYSTEM,
    dateStyle: DateStyle = DateStyle.LONG,
    onOpenSettings: () -> Unit = {},
) {
    val now by rememberMinuteClock()
    TodayScreen(now = now, uses24Hour = uses24Hour(clockFormat), dateStyle = dateStyle, onOpenSettings = onOpenSettings)
}

/** The page itself, drawn from a moment rather than a clock, so its tests can pin one. */
@Composable
fun TodayScreen(
    now: ZonedDateTime,
    uses24Hour: Boolean,
    dateStyle: DateStyle = DateStyle.LONG,
    onOpenSettings: () -> Unit = {},
) {
    val locale = LocalLocale.current.platformLocale
    Surface(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(pageGutter(sideInsets = false)),
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).testTag(TodayTags.SETTINGS),
            ) {
                Icon(TempoIcons.Settings, contentDescription = stringResource(R.string.today_settings))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center).padding(ScreenMargin),
            ) {
                Text(
                    text = DateTimeText.time(now, locale, uses24Hour),
                    style = TempoTheme.type.heroNumber,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(TodayTags.TIME).semantics { heading() },
                )
                Text(
                    text = DateTimeText.date(now, locale, dateStyle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TodayTags.DATE),
                )
            }
        }
    }
}

object TodayTags {
    const val TIME = "today_time"
    const val DATE = "today_date"
    const val SETTINGS = "today_settings"
}

/** The current moment, refreshed on each minute's start while the page is at least started. */
@Composable
private fun rememberMinuteClock(): State<ZonedDateTime> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(ZonedDateTime.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = ZonedDateTime.now()
                delay(untilNextMinute(Instant.now()).toMillis())
            }
        }
    }
}
