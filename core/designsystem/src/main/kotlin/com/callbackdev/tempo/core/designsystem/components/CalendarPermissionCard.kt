package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.callbackdev.tempo.core.designsystem.R
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.model.CalendarPermission

/**
 * Where the calendar would be, without the permission (Today, Settings): what Tempo would read
 * and why, and the one way forward that works now. Asked again while Android allows it; refused
 * for good, the app's own page in the system's settings, the only place left to change it.
 */
@Composable
fun CalendarPermissionCard(
    permission: CalendarPermission,
    onAsk: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forGood = permission == CalendarPermission.DENIED_FOR_GOOD
    StatusCard(
        icon = TempoIcons.Calendar,
        title = stringResource(R.string.permission_title),
        body = stringResource(if (forGood) R.string.permission_denied_body else R.string.permission_body),
        tone = StatusTone.CHOICE,
        action = stringResource(if (forGood) R.string.permission_open_settings else R.string.permission_allow),
        onAction = if (forGood) onOpenAppSettings else onAsk,
        modifier = modifier,
    )
}
