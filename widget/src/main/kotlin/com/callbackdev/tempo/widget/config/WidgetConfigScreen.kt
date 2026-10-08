package com.callbackdev.tempo.widget.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.components.GroupDivider
import com.callbackdev.tempo.core.designsystem.components.GroupHeader
import com.callbackdev.tempo.core.designsystem.components.RadioDialog
import com.callbackdev.tempo.core.designsystem.components.SettingsGroup
import com.callbackdev.tempo.core.designsystem.components.SwitchRow
import com.callbackdev.tempo.core.designsystem.components.ValueRow
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.widgetCardContainer
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.widget.EventTap
import com.callbackdev.tempo.widget.HeaderTap
import com.callbackdev.tempo.widget.R
import com.callbackdev.tempo.widget.WidgetBackground
import com.callbackdev.tempo.widget.WidgetDoors
import com.callbackdev.tempo.widget.WidgetKind
import com.callbackdev.tempo.widget.WidgetLook
import com.callbackdev.tempo.widget.WidgetModel
import com.callbackdev.tempo.widget.isNight
import com.callbackdev.tempo.widget.widgetCardFill
import com.callbackdev.tempo.widget.widgetDressFor
import java.time.LocalDate
import kotlin.math.roundToInt

/** Tags for the tests. */
object WidgetConfigTags {
    const val LIST = "widget_config_list"
    const val HEADER_TAP = "widget_config_header_tap"
    const val EVENT_TAP = "widget_config_event_tap"
}

private enum class ConfigDialog { CLOCK_FORMAT, DATE_STYLE, HEADER_TAP, EVENT_TAP }

/**
 * One widget's settings, reached from the launcher's reconfigure flow (the family's screen,
 * Chiaro's and Passo's groups in their order): the card at the top, at the sizes it can be given;
 * then what its time and date show, what its touches open, what it is painted on and what it
 * carries. Every choice is saved as it is made and repaints this one card; «Done» only closes the
 * door.
 *
 * A choice that cannot work on this phone is not offered: no clock app, no "opens your clock app"
 * (unless it was already chosen, so the screen says what the card does); no calendar app, no
 * calendar doors. A touch whose door is gone falls back to Tempo on the card itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    kind: WidgetKind,
    look: WidgetLook,
    model: WidgetModel?,
    placed: DpSize?,
    onLook: (WidgetLook) -> Unit,
    onOpacityDrag: (Int) -> Unit,
    onOpacityDone: () -> Unit,
    onDone: () -> Unit,
) {
    val gutter = pageGutter(sideInsets = false)
    val settings = model?.settings ?: UserSettings()
    val doors = model?.doors ?: WidgetDoors.None
    var dialog by rememberSaveable { mutableStateOf<ConfigDialog?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                windowInsets = TopAppBarDefaults.windowInsets.add(gutter.asInsets()),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(gutter)
                .padding(bottom = 24.dp)
                .testTag(WidgetConfigTags.LIST),
        ) {
            WidgetPreviewSection(kind, model?.copy(look = look), placed)

            GroupHeader(stringResource(R.string.widget_config_clock_group))
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_clock),
                    note = stringResource(R.string.widget_config_show_clock_note),
                    checked = look.showClock,
                    onChange = { onLook(look.copy(showClock = it)) },
                )
                if (look.showClock) {
                    GroupDivider()
                    ValueRow(
                        label = stringResource(R.string.widget_config_clock_format),
                        value = clockFormatLabel(look.clockFormat),
                        onClick = { dialog = ConfigDialog.CLOCK_FORMAT },
                    )
                }
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_date),
                    checked = look.showDate,
                    onChange = { onLook(look.copy(showDate = it)) },
                )
                if (look.showDate) {
                    GroupDivider()
                    ValueRow(
                        label = stringResource(R.string.widget_config_date_format),
                        value = dateStyleLabel(look.dateStyle),
                        onClick = { dialog = ConfigDialog.DATE_STYLE },
                    )
                }
            }

            GroupHeader(stringResource(R.string.widget_config_touches))
            SettingsGroup {
                if (look.showClock || look.showDate) {
                    ValueRow(
                        label = stringResource(R.string.widget_config_header_tap),
                        value = headerTapLabel(look.headerTap),
                        onClick = { dialog = ConfigDialog.HEADER_TAP },
                        modifier = Modifier.testTag(WidgetConfigTags.HEADER_TAP),
                    )
                    GroupDivider()
                }
                ValueRow(
                    label = stringResource(R.string.widget_config_event_tap),
                    value = eventTapLabel(look.eventTap),
                    onClick = { dialog = ConfigDialog.EVENT_TAP },
                    modifier = Modifier.testTag(WidgetConfigTags.EVENT_TAP),
                    enabled = doors.canOpenEvent || look.eventTap == EventTap.CALENDAR,
                )
            }

            GroupHeader(stringResource(R.string.widget_config_background))
            SettingsGroup {
                BackgroundChoices(look, settings, onLook)
                GroupDivider()
                OpacityRow(look.opacityPct, onOpacityDrag, onOpacityDone)
            }

            GroupHeader(stringResource(R.string.widget_config_content))
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.widget_config_all_day),
                    note = stringResource(
                        if (kind ==
                            WidgetKind.AGENDA
                        ) {
                            R.string.widget_config_all_day_note
                        } else {
                            R.string.widget_config_all_day_note_words
                        },
                    ),
                    checked = look.showAllDay,
                    onChange = { onLook(look.copy(showAllDay = it)) },
                )
                // With today alone on Tempo's horizon there are no days ahead to show: no switch.
                if (settings.horizonDays > 1) {
                    GroupDivider()
                    SwitchRow(
                        label = stringResource(R.string.widget_config_days_ahead),
                        note = stringResource(
                            if (kind == WidgetKind.AGENDA) {
                                R.string.widget_config_days_ahead_note
                            } else {
                                R.string.widget_config_days_ahead_note_words
                            },
                        ),
                        checked = look.showDaysAhead,
                        onChange = { onLook(look.copy(showDaysAhead = it)) },
                    )
                }
            }

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp),
            ) {
                Text(stringResource(R.string.widget_config_done))
            }
        }
    }

    when (dialog) {
        ConfigDialog.CLOCK_FORMAT -> RadioDialog(
            title = stringResource(R.string.widget_config_clock_format),
            options = listOf<ClockFormat?>(null, ClockFormat.SYSTEM, ClockFormat.H24, ClockFormat.H12)
                .map { it to clockFormatLabel(it) },
            selected = look.clockFormat,
            onSelect = {
                onLook(look.copy(clockFormat = it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        ConfigDialog.DATE_STYLE -> RadioDialog(
            title = stringResource(R.string.widget_config_date_format),
            options = (listOf<DateStyle?>(null) + DateStyle.entries).map { it to dateStyleLabel(it) },
            selected = look.dateStyle,
            onSelect = {
                onLook(look.copy(dateStyle = it))
                dialog = null
            },
            onDismiss = { dialog = null },
            explanation = stringResource(R.string.widget_config_date_format_note),
        )

        ConfigDialog.HEADER_TAP -> RadioDialog(
            title = stringResource(R.string.widget_config_header_tap),
            options = HeaderTap.entries
                .filter { tap ->
                    tap == look.headerTap || when (tap) {
                        HeaderTap.TEMPO -> true
                        HeaderTap.CALENDAR -> doors.canOpenDay
                        HeaderTap.CLOCK -> doors.clockPackage != null
                    }
                }
                .map { it to headerTapLabel(it) },
            selected = look.headerTap,
            onSelect = {
                onLook(look.copy(headerTap = it))
                dialog = null
            },
            onDismiss = { dialog = null },
            explanation = stringResource(R.string.widget_tap_clock_note).takeIf { doors.clockPackage != null },
        )

        ConfigDialog.EVENT_TAP -> RadioDialog(
            title = stringResource(R.string.widget_config_event_tap),
            options = EventTap.entries
                .filter { it == look.eventTap || it == EventTap.TEMPO || doors.canOpenEvent }
                .map { it to eventTapLabel(it) },
            selected = look.eventTap,
            onSelect = {
                onLook(look.copy(eventTap = it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@Composable
private fun clockFormatLabel(format: ClockFormat?): String = stringResource(
    when (format) {
        null -> R.string.widget_config_as_tempo
        ClockFormat.SYSTEM -> R.string.widget_config_as_phone
        ClockFormat.H24 -> R.string.widget_config_24
        ClockFormat.H12 -> R.string.widget_config_12
    },
)

/** A date style named by today's date in it, as Tempo's own Settings name them. */
@Composable
private fun dateStyleLabel(style: DateStyle?): String = if (style == null) {
    stringResource(R.string.widget_config_as_tempo)
} else {
    DateTimeText.date(LocalDate.now(), LocalLocale.current.platformLocale, style)
}

@Composable
private fun headerTapLabel(tap: HeaderTap): String = stringResource(
    when (tap) {
        HeaderTap.TEMPO -> R.string.widget_tap_tempo
        HeaderTap.CALENDAR -> R.string.widget_tap_calendar_day
        HeaderTap.CLOCK -> R.string.widget_tap_clock
    },
)

@Composable
private fun eventTapLabel(tap: EventTap): String = stringResource(
    when (tap) {
        EventTap.TEMPO -> R.string.widget_tap_tempo
        EventTap.CALENDAR -> R.string.widget_tap_calendar_event
    },
)

/** What the background question offers, in Chiaro's order less its sky. Every [WidgetBackground] has its row. */
internal val WidgetBackgroundChoices: List<Pair<WidgetBackground, Int>> = listOf(
    WidgetBackground.LIGHT to R.string.widget_bg_light,
    WidgetBackground.DARK to R.string.widget_bg_dark,
    WidgetBackground.SYSTEM to R.string.widget_bg_system,
    WidgetBackground.COLOR to R.string.widget_bg_color,
)

/** Chiaro's six, in Chiaro's order. Every [WidgetCardColor] has its swatch. */
internal val WidgetCardColorChoices: List<Pair<WidgetCardColor, Int>> = listOf(
    WidgetCardColor.BLUE to R.string.widget_color_blue,
    WidgetCardColor.AZURE to R.string.widget_color_azure,
    WidgetCardColor.GREEN to R.string.widget_color_green,
    WidgetCardColor.TEAL to R.string.widget_color_teal,
    WidgetCardColor.PLUM to R.string.widget_color_plum,
    WidgetCardColor.CLAY to R.string.widget_color_clay,
)

/**
 * What kind of card, each row with a swatch of the ground it paints; and, once «A colour» is
 * picked, which colour, as a strip of swatches with the chosen one's name under it (a swatch is
 * never the only label).
 */
@Composable
private fun BackgroundChoices(look: WidgetLook, settings: UserSettings, onLook: (WidgetLook) -> Unit) {
    val context = LocalContext.current
    val dress = widgetDressFor(context, settings)
    val night = isNight(context)
    Column(Modifier.selectableGroup()) {
        WidgetBackgroundChoices.forEach { (background, label) ->
            ChoiceRow(
                label = stringResource(label),
                selected = look.background == background,
                onPick = { onLook(look.copy(background = background)) },
            ) {
                val swatch = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                val full = look.copy(background = background, opacityPct = 100)
                if (background == WidgetBackground.SYSTEM) {
                    // The phone's choice, drawn as both of its answers on a diagonal.
                    Canvas(swatch) {
                        drawRect(dress.lightScheme.surface)
                        drawPath(
                            Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            },
                            dress.darkScheme.surface,
                        )
                    }
                } else {
                    Box(swatch.background(widgetCardFill(full, dress, night)))
                }
            }
        }
    }
    if (look.background == WidgetBackground.COLOR) {
        ColorSwatches(look.cardColor) { onLook(look.copy(cardColor = it)) }
    }
}

@Composable
private fun ColorSwatches(selected: WidgetCardColor, onPick: (WidgetCardColor) -> Unit) {
    Column(modifier = Modifier.padding(start = 50.dp, end = 10.dp, bottom = 12.dp)) {
        // Each colour is a 48dp target around its 36dp disc (the chosen one's included), side by
        // side: the gaps between the discs are the targets' own. A narrow phone wraps the row.
        FlowRow(modifier = Modifier.selectableGroup()) {
            WidgetCardColorChoices.forEach { (color, label) ->
                val chosen = color == selected
                val name = stringResource(label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(selected = chosen, onClick = { onPick(color) }, role = Role.RadioButton)
                        .semantics { contentDescription = name },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (chosen) 2.dp else 0.dp,
                                color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(if (chosen) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .background(widgetCardContainer(color)),
                    ) {
                        if (chosen) {
                            // Every card colour is a dark ground under white ink.
                            Icon(
                                TempoIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(WidgetCardColorChoices.first { it.first == selected }.second),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onPick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onPick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        trailing?.invoke()
    }
}

/** How solid the card is: the name and the value on one line, the slider under them, in steps of 5%. */
@Composable
private fun OpacityRow(pct: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when (pct) {
                    100 -> stringResource(R.string.widget_opacity_full)
                    0 -> stringResource(R.string.widget_opacity_transparent)
                    else -> stringResource(R.string.widget_opacity_percent, pct)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pct.toFloat(),
            onValueChange = { raw -> onChange((raw / 5f).roundToInt() * 5) },
            onValueChangeFinished = onDone,
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}
