package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.theme.GroupShape

/** How much a status card asks of the reader. */
enum class StatusTone {
    /** Something is not working and needs them. */
    PROBLEM,

    /** A state they chose, one tap from undoing. */
    CHOICE,

    /** A fact worth knowing once. */
    NOTE,
}

/**
 * A state of the app stated as a card: a mark, what is going on, what to do, and the button that
 * does it. Never a toast: a toast is gone before it is read (Chiaro §8.2).
 */
@Composable
fun StatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    dismiss: String? = null,
    onDismiss: () -> Unit = {},
) {
    val (container, content) = when (tone) {
        StatusTone.PROBLEM -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer

        StatusTone.CHOICE ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer

        StatusTone.NOTE -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
    Surface(color = container, contentColor = content, shape = GroupShape, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (action != null) 8.dp else 16.dp,
            ),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = BODY_ALPHA))
                }
            }
            if (action != null || dismiss != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    if (dismiss != null) {
                        TextButton(onClick = onDismiss) { Text(dismiss, color = content) }
                    }
                    // Filled: a tonal button wears the card's own ground on a CHOICE card and reads
                    // as plain text (Phase 2's screenshots).
                    if (action != null) {
                        Button(onClick = onAction) { Text(action) }
                    }
                }
            }
        }
    }
}

private const val BODY_ALPHA = 0.86f
