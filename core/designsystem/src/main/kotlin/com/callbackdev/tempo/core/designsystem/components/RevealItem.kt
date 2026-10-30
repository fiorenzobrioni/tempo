package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import kotlinx.coroutines.delay

/**
 * A row brought into view, glowing once while [revealed]: the accent at a low alpha over it, as a
 * touch's state layer is, up in a moment and gone within [REVEAL_MILLIS]; under reduced motion a
 * plain highlight for as long, then none. Over the content rather than behind it, so a row on its
 * own ground (the event under way, a chip) glows as the others do. Read in the draw phase: the glow
 * recomposes nothing.
 *
 * @param inset how far the glow keeps from the item's sides, so a row that runs to the page's
 *   edge glows as a card would.
 */
@Composable
fun RevealItem(
    revealed: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    inset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val reduced = reducedMotion()
    val glow = remember { Animatable(0f) }
    LaunchedEffect(revealed, reduced) {
        if (!revealed) {
            glow.snapTo(0f)
            return@LaunchedEffect
        }
        if (reduced) {
            glow.snapTo(1f)
            delay(REVEAL_MILLIS)
        } else {
            glow.animateTo(1f, TempoMotion.effects())
            delay(HOLD_MILLIS)
            glow.animateTo(0f, tween(FADE_MILLIS.toInt(), easing = LinearOutSlowInEasing))
        }
        glow.snapTo(0f)
    }
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier.drawWithContent {
            drawContent()
            val strength = glow.value
            if (strength > 0f) {
                inset(horizontal = inset.toPx(), vertical = 0f) {
                    drawOutline(shape.createOutline(size, layoutDirection, this), color, alpha = GLOW_ALPHA * strength)
                }
            }
        },
    ) { content() }
}

/** How long a reveal glows, from its rise to its last trace. */
const val REVEAL_MILLIS = 1_200L

private const val HOLD_MILLIS = 250L
private const val FADE_MILLIS = 700L

/** A dragged item's state layer in Material 3: seen at once, light enough that the row reads through it. */
private const val GLOW_ALPHA = 0.16f
