package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.GroupShape
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.SectionBottom
import com.callbackdev.tempo.core.designsystem.theme.SectionTop

/*
 * The settings kit (`docs/adr/0002-design-language.md`): a
 * header in the accent, rows that belong together on one rounded ground, hairlines between
 * them starting where the text starts, and one dialog shape for every multiple choice.
 */

/** A section's title, in the accent. */
@Composable
fun GroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, end = ScreenMargin, top = SectionTop, bottom = SectionBottom + 4.dp),
    )
}

/** Rows that belong together, on one rounded ground. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .clip(GroupShape),
    ) {
        Column { content() }
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
}

/**
 * A preference: what it is, what it says now, tap to change. With [trailing] a chevron says the
 * tap leaves the screen (a system page, a web page).
 */
@Composable
fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        if (icon != null) RowIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing) {
            Icon(
                imageVector = TempoIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A fact, with nothing to change. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A switch: the row is the touch target, the switch only shows the state. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    icon: ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onChange(!checked) }, role = Role.Switch)
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        if (icon != null) RowIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}

/** Every multiple-choice preference: a picker, never a free-text field for a value with a range. */
@Composable
fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    explanation: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                if (explanation != null) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, onClick = {
                                onSelect(value)
                            }, role = Role.RadioButton)
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
