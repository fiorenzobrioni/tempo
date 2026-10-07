package com.callbackdev.tempo.core.testing

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import java.io.File

/**
 * Walks a page from its top to its bottom a screenful at a time (four fifths of one, so nothing
 * falls between two), checking each screenful with [assertAccessible] and writing it to
 * `build/screenshots` as `name-1.png`, `name-2.png`… for a person to look at: the large-text and
 * wide-window passes (PLANNING.md §12) read the whole page, not its first screen.
 *
 * @param scrollable the page's scrolling list or column.
 */
fun ComposeContentTestRule.walkPage(scrollable: SemanticsMatcher, name: String, maxScreens: Int = 12) {
    for (screen in 1..maxScreens) {
        waitForIdle()
        assertAccessible()
        writeScreenshot("$name-$screen")
        val node = onNode(scrollable).fetchSemanticsNode()
        val range = node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) ?: return
        if (range.value() >= range.maxValue()) return
        val step = node.size.height * SCREENFUL
        onNode(scrollable).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, step) }
    }
}

/**
 * Writes what the screen looks like to `build/screenshots/[name].png`: not an assertion, and
 * skipped where the graphics runtime cannot draw.
 */
fun ComposeContentTestRule.writeScreenshot(name: String) {
    waitForIdle()
    runCatching {
        val bitmap = onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

private const val SCREENFUL = 0.8f
