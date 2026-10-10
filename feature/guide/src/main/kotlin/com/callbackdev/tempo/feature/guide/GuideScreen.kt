package com.callbackdev.tempo.feature.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.components.DayDial
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.domain.widget.DialArc

@Composable
fun GuideRoute(onBack: () -> Unit) {
    GuideScreen(onBack = onBack)
}

/**
 * The guide, in Chiaro's shape as Passo's (VISION.md, Settings): where the events come from, what
 * Today and the widgets answer, who does the writing, and the things a screen cannot say out loud
 * (that an all-day event keeps its date in every zone, that a card says clock times because it is
 * redrawn at boundaries, that Tempo does nothing with the screen off). Re-openable from Settings:
 * a definition offered before the reader has met the thing does not stick.
 *
 * Chiaro's rules hold it in shape. It **never teaches a control**: it says what a part of the
 * screen is for, not which button to press. It says what Tempo does, and where the reader's
 * calendar app or Clock app does the rest (VISION.md asks for "what Tempo does not do and who does
 * it instead"), as a hand-over, never as an excuse. It teaches by showing the app's own dial, with
 * a caption saying it is an example, so no sample reads as the reader's day. Nothing on it depends
 * on the reader's settings, so it needs no view model: the example has no figures to format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TempoIcons.Back, contentDescription = stringResource(R.string.guide_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        GuideContent(Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun GuideContent(modifier: Modifier) {
    Column(
        modifier = modifier
            .testTag(GuideTags.CONTENT)
            .verticalScroll(rememberScrollState())
            .padding(pageGutter(sideInsets = false))
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        Chapter(TempoIcons.Calendar, stringResource(R.string.guide_source_title))
        Paragraph(stringResource(R.string.guide_source_p1))
        Feature(R.string.guide_source_choice_title, R.string.guide_source_choice_body)
        Feature(R.string.guide_source_all_day_title, R.string.guide_source_all_day_body)
        Feature(R.string.guide_source_declined_title, R.string.guide_source_declined_body)
        Feature(R.string.guide_source_work_title, R.string.guide_source_work_body)

        Chapter(TempoIcons.Clock, stringResource(R.string.guide_today_title))
        Paragraph(stringResource(R.string.guide_today_p1))
        DialSample()
        Caption(stringResource(R.string.guide_today_dial_caption))
        Feature(R.string.guide_today_dial_title, R.string.guide_today_dial_body)
        Feature(R.string.guide_today_timeline_title, R.string.guide_today_timeline_body)
        Feature(R.string.guide_today_free_title, R.string.guide_today_free_body)
        Feature(R.string.guide_today_ahead_title, R.string.guide_today_ahead_body)
        Feature(R.string.guide_today_alarm_title, R.string.guide_today_alarm_body)
        Feature(R.string.guide_today_live_title, R.string.guide_today_live_body)

        Chapter(TempoIcons.Plus, stringResource(R.string.guide_handover_title))
        Paragraph(stringResource(R.string.guide_handover_p1))
        Feature(R.string.guide_handover_event_title, R.string.guide_handover_event_body)
        Feature(R.string.guide_handover_new_title, R.string.guide_handover_new_body)
        Feature(R.string.guide_handover_day_title, R.string.guide_handover_day_body)
        Feature(R.string.guide_handover_reminders_title, R.string.guide_handover_reminders_body)

        Chapter(TempoIcons.Widgets, stringResource(R.string.guide_widgets_title))
        Feature(R.string.guide_widgets_two_title, R.string.guide_widgets_two_body)
        Feature(R.string.guide_widgets_fit_title, R.string.guide_widgets_fit_body)
        Feature(R.string.guide_widgets_times_title, R.string.guide_widgets_times_body)

        Chapter(TempoIcons.Battery, stringResource(R.string.guide_battery_title))
        Paragraph(stringResource(R.string.guide_battery_p1))

        Chapter(TempoIcons.Shield, stringResource(R.string.guide_data_title))
        Paragraph(stringResource(R.string.guide_data_p1))
        Paragraph(stringResource(R.string.guide_data_p2))
    }
}

// The prose kit: Chiaro's, value for value, as Passo's guide carries it.

@Composable
private fun Chapter(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        // Decoration: the title beside it says it all.
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Prose to be read, not scanned: bodyLarge. */
@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

/** One thing a part of the screen does: its name, then what it is for. */
@Composable
private fun Feature(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

/** What the sample above it stands for: always said, so no example reads as the reader's day. */
@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Today's dial, drawn by Today's own component: twenty past ten, a meeting under way until eleven
 * in the accent, lunch at one and a call at three quieter. It winds to its time as on Today.
 */
@Composable
private fun DialSample() {
    // The dial grows with the reader's text size, as far as the page allows, as Passo's ring does.
    val grow = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        DayDial(
            faceMinutes = SAMPLE_NOW,
            arcs = SAMPLE_ARCS,
            description = stringResource(R.string.guide_today_dial_description),
            modifier = Modifier.widthIn(max = 160.dp * grow).fillMaxWidth().testTag(GuideTags.DIAL),
        )
    }
}

object GuideTags {
    const val CONTENT = "guide_content"
    const val DIAL = "guide_dial"
}

/** Minutes from twelve o'clock on the face: 10:20. */
private const val SAMPLE_NOW = 620f

/** 10:00 to 11:00 under way, 13:00 to 14:00, 15:00 to 15:45. */
private val SAMPLE_ARCS = listOf(
    DialArc(from = 600f, sweep = 60f, focus = true),
    DialArc(from = 60f, sweep = 60f, focus = false),
    DialArc(from = 180f, sweep = 45f, focus = false),
)
