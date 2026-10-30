package com.callbackdev.tempo.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Where a page stands in two panes, and how wide each is (PLANNING.md §11 step 8.7), at the
 * windows of the phones, foldables and tablets it was decided for.
 */
class PageWidthTest {
    private fun panes(width: Dp, height: Dp, insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp) =
        TwoPanes.of(width, height, insetStart, insetEnd)

    @Test
    fun `a phone keeps its one column, upright and on its side`() {
        assertThat(panes(411.dp, 891.dp)).isNull()
        // As wide as a tablet, and too short for two panes side by side.
        assertThat(panes(891.dp, 411.dp)).isNull()
    }

    @Test
    fun `a foldable open under 840 dp keeps its one column`() {
        assertThat(panes(690.dp, 829.dp)).isNull()
        assertThat(panes(829.dp, 690.dp)).isNull()
    }

    @Test
    fun `two panes from Material's expanded width and medium height`() {
        assertThat(panes(840.dp, 480.dp)).isNotNull()
        assertThat(panes(839.dp, 800.dp)).isNull()
        assertThat(panes(1280.dp, 479.dp)).isNull()
    }

    @Test
    fun `a tablet on its side, two fifths and the rest`() {
        assertThat(panes(1280.dp, 800.dp)).isEqualTo(TwoPanes(PageGutter.None, 512.dp, 768.dp))
    }

    @Test
    fun `the hero's pane is never narrower than a phone's page`() {
        // A foldable open at 841 dp, and a large tablet upright.
        assertThat(panes(841.dp, 701.dp)).isEqualTo(TwoPanes(PageGutter.None, 400.dp, 441.dp))
        assertThat(panes(900.dp, 1200.dp)).isEqualTo(TwoPanes(PageGutter.None, 400.dp, 500.dp))
        assertThat(panes(1100.dp, 800.dp)).isEqualTo(TwoPanes(PageGutter.None, 440.dp, 660.dp))
    }

    @Test
    fun `past twice the column the two panes stand centred`() {
        assertThat(panes(1480.dp, 924.dp)).isEqualTo(TwoPanes(PageGutter(100.dp, 100.dp), 512.dp, 768.dp))
    }

    @Test
    fun `a cutout or a side bar is kept clear on its own side`() {
        val panes = panes(1280.dp, 800.dp, insetStart = 40.dp)
        assertThat(panes?.gutter).isEqualTo(PageGutter(40.dp, 0.dp))
        assertThat(panes?.leading).isEqualTo(496.dp)
        assertThat(panes?.trailing).isEqualTo(744.dp)
        assertThat(panes?.leadingGutter).isEqualTo(PageGutter(40.dp, 0.dp))
        assertThat(panes?.trailingGutter).isEqualTo(PageGutter(0.dp, 0.dp))
    }
}
