package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Chiaro's shape scale (its DESIGN.md §6). Chips and buttons are fully round at the call site. */
internal val TempoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** A group of rows or a card on a screen's list: Chiaro's 24dp group ground. */
val GroupShape = RoundedCornerShape(24.dp)

/** Chiaro's list rhythm: what a section header costs above and below. */
val SectionTop = 16.dp
val SectionBottom = 4.dp

/** The screen margin. */
val ScreenMargin = 16.dp
