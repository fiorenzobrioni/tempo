package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.domain.calendar.markColor

/**
 * A calendar's colour as a mark (PLANNING.md §6): a dot, never the colour of a text or a ground.
 * Stepped in lightness by [markColor] until it reads at 3:1 on [ground], so a pale yellow
 * calendar is still a dot on a light card. With no colour, the page's quiet ink.
 */
@Composable
fun CalendarDot(argb: Int?, ground: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    val color = argb?.let { Color(markColor(it, ground.toArgb())) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.size(size).background(color, CircleShape))
}
