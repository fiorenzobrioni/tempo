package com.callbackdev.tempo.feature.today

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.domain.clock.untilNextMinute
import com.callbackdev.tempo.core.domain.clock.uses24Hour
import com.callbackdev.tempo.core.model.ClockFormat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Today, as Phase 0 leaves it: the clock and the date, nothing that is not real yet. The agenda,
 * the day's sentence and the new-event button arrive with Phase 3 (PLANNING.md §11), on the
 * calendar engine of Phase 1. The time is read once a minute, on the minute, and only while the
 * page is started: no ticker survives the screen going off (PLANNING.md §9).
 */
@Composable
fun TodayRoute(clockFormat: ClockFormat = ClockFormat.SYSTEM) {
    val now by rememberMinuteClock()
    val context = LocalContext.current
    TodayScreen(now = now, uses24Hour = clockFormat.uses24Hour(DateFormat.is24HourFormat(context)))
}

/** The page itself, drawn from a moment rather than a clock, so its tests can pin one. */
@Composable
fun TodayScreen(now: ZonedDateTime, uses24Hour: Boolean) {
    val locale = LocalLocale.current.platformLocale
    val time = DateTimeFormatter.ofPattern(bestPattern(locale, if (uses24Hour) "Hm" else "hm"), locale).format(now)
    val date = DateTimeFormatter.ofPattern(bestPattern(locale, "EEEEdMMMM"), locale).format(now)
    Surface(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(pageGutter(sideInsets = false))
                .padding(ScreenMargin),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = time,
                    style = TempoTheme.type.heroNumber,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(TodayTags.TIME).semantics { heading() },
                )
                Text(
                    text = date.replaceFirstChar { it.titlecase(locale) },
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
}

/** The locale's own order and words for [skeleton] (Android's CLDR data, as every clock app). */
private fun bestPattern(locale: Locale, skeleton: String): String = DateFormat.getBestDateTimePattern(locale, skeleton)

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
