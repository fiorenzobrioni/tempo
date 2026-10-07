package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.domain.calendar.markColor

/**
 * A calendar's colour as the bar at the start of an event's row: as tall as the row, 4dp wide,
 * stepped to 3:1 on [ground] like [CalendarDot]. A mark, never the colour of the event's text.
 */
@Composable
fun CalendarBar(argb: Int?, ground: Color, modifier: Modifier = Modifier) {
    val color = argb?.let { Color(markColor(it, ground.toArgb())) } ?: MaterialTheme.colorScheme.outline
    Box(modifier.width(4.dp).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
}
