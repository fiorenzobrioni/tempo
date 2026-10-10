package com.callbackdev.tempo.core.testing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import com.callbackdev.tempo.core.designsystem.components.DENSE_TARGETS_TAG
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt

/**
 * The checks of Android's Accessibility Scanner that the semantics tree can answer, run on what
 * the screen shows right now (PLANNING.md §12):
 *
 * - **Touch target**: every control a finger can reach has at least 48 by 48dp of its own. Compose
 *   already widens a smaller control's touch area to 48dp (and tells TalkBack and the Scanner
 *   so), which makes a small control a problem only where that widened area runs into another
 *   control's: there a finger, and the Scanner, find less than 48dp.
 * - **Label**: every control says what it is to TalkBack, with a text, a description or a value.
 *
 * Contrast is the theme's, pinned by `ContrastTest` in `:core:designsystem`. A control cut by the
 * edge of a scrolling list is measured when it is scrolled into view, as the Scanner does.
 *
 * Targets packed on purpose are exempt from the size, and only from it: a chart's bars, a
 * month's days and Material's clock dial, marked with [DENSE_TARGETS_TAG]. A 7-day row cannot
 * give each day 48dp on a phone; each is still its own labelled node, reached one by one by
 * swiping in TalkBack.
 */
fun SemanticsNodeInteractionsProvider.assertAccessible() {
    val problems = mutableListOf<String>()
    // Every window: the page, and a dialog or a menu over it.
    for (root in onAllNodes(isRoot()).fetchSemanticsNodes()) {
        val controls = mutableListOf<Control>()
        fun visit(node: SemanticsNode, dense: Boolean, ancestors: List<Int>) {
            val inDense = dense || node.config.getOrNull(SemanticsProperties.TestTag) == DENSE_TARGETS_TAG
            controlOf(node, inDense, ancestors, order = controls.size)?.let { controls += it }
            node.children.forEach { visit(it, inDense, ancestors + node.id) }
        }
        visit(root, dense = false, ancestors = emptyList())
        problems += problemsOf(controls)
    }
    assertWithMessage("Accessibility problems on screen").that(problems).isEmpty()
}

/** A control on screen: what it reads as, and the area a finger has for it, in pixels. */
private class Control(
    val id: Int,
    val ancestors: List<Int>,
    val label: String,
    val area: Rect,
    val bounds: Rect,
    val order: Int,
    val pxPerDp: Float,
    val measured: Boolean,
) {
    /** Neither holds the other: a card around its own buttons is not a collision. */
    fun apartFrom(other: Control) = id != other.id && id !in other.ancestors && other.id !in ancestors
}

private fun controlOf(node: SemanticsNode, dense: Boolean, ancestors: List<Int>, order: Int): Control? {
    val config = node.config
    if (SemanticsActions.OnClick !in config || SemanticsProperties.Disabled in config) return null
    if (SemanticsProperties.HideFromAccessibility in config) return null
    val bounds = node.boundsInRoot
    if (bounds.width <= 0f || bounds.height <= 0f) return null
    // Cut by a list's edge: measured once scrolled into view, not here, and only its visible
    // part stands next to the others (a row scrolled under a top bar is not beside its buttons).
    val clipped = bounds.width < node.size.width - 1 || bounds.height < node.size.height - 1
    return Control(
        id = node.id,
        ancestors = ancestors,
        label = labelOf(node),
        area = if (clipped) bounds else node.touchBoundsInRoot,
        bounds = bounds,
        order = order,
        pxPerDp = node.layoutInfo.density.density,
        measured = !dense && !clipped,
    )
}

/**
 * Where two touch areas overlap, a touch goes to the nearer control (Compose's hit test), so each
 * keeps half the overlap: what is left of [control]'s area once its neighbours have taken theirs.
 *
 * A control drawn over another (a floating action button over a list's row: their drawn bounds
 * overlap, not only their touch areas) takes the whole overlap, since it is on top, and the one
 * under it keeps the side that leaves a finger the most room; the one on top loses nothing to it.
 * Added in Phase 3: halving such an overlap read a 48dp row as 26dp.
 */
private fun shareOf(control: Control, controls: List<Control>): Rect {
    val area = control.area
    var left = area.left
    var top = area.top
    var right = area.right
    var bottom = area.bottom
    for (other in controls) {
        if (!other.apartFrom(control) || !other.area.overlaps(area)) continue
        val drawnOver = other.bounds.overlaps(control.bounds)
        if (drawnOver && other.order < control.order) continue
        val overlap = Rect(left, top, right, bottom).intersect(other.area)
        if (overlap.width <= 0f || overlap.height <= 0f) continue
        if (drawnOver) {
            // Under a floating control: keep the larger of the four sides it leaves free.
            val sides = listOf(
                Rect(left, top, overlap.left, bottom),
                Rect(overlap.right, top, right, bottom),
                Rect(left, top, right, overlap.top),
                Rect(left, overlap.bottom, right, bottom),
            ).filter { it.width > 0f && it.height > 0f }
            val best = sides.maxByOrNull { minOf(it.width, it.height) } ?: Rect(left, top, left, top)
            left = best.left
            top = best.top
            right = best.right
            bottom = best.bottom
            continue
        }
        if (overlap.width <= overlap.height) {
            if (other.area.center.x > area.center.x) right -= overlap.width / 2 else left += overlap.width / 2
        } else {
            if (other.area.center.y > area.center.y) bottom -= overlap.height / 2 else top += overlap.height / 2
        }
    }
    return Rect(left, top, right, bottom)
}

private fun problemsOf(controls: List<Control>): List<String> {
    val problems = mutableListOf<String>()
    for (control in controls) {
        if (control.label.isBlank()) problems += "A control with nothing to read at ${control.area}"
        if (!control.measured) continue
        val share = shareOf(control, controls)
        val width = share.width / control.pxPerDp
        val height = share.height / control.pxPerDp
        if (width < MIN_TARGET_DP || height < MIN_TARGET_DP) {
            problems += "«${control.label}»: ${width.roundToInt()} by ${height.roundToInt()}dp for a finger"
        }
    }
    return problems
}

private fun labelOf(node: SemanticsNode): String {
    val config = node.config
    return listOfNotNull(
        config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
        config.getOrNull(SemanticsProperties.EditableText)?.text,
        config.getOrNull(SemanticsProperties.StateDescription),
    ).joinToString(" ").trim()
}

/** Material's minimum, less half a dp for rounding at the test's density. */
private const val MIN_TARGET_DP = 47.5f
