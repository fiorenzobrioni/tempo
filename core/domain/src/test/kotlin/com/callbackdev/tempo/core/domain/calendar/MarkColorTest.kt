package com.callbackdev.tempo.core.domain.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkColorTest {
    private val white = 0xFFFCF9F3.toInt()
    private val night = 0xFF141218.toInt()

    @Test
    fun `a colour that already reads is kept`() {
        val navy = 0xFF1A237E.toInt()
        assertThat(markColor(navy, white)).isEqualTo(navy)
    }

    @Test
    fun `a pale yellow is darkened on a light card, just enough`() {
        val banana = 0xFFF6BF26.toInt()
        val mark = markColor(banana, white)
        assertThat(contrast(mark, white)).isAtLeast(MIN_CONTRAST)
        assertThat(contrast(mark, white)).isLessThan(MIN_CONTRAST + 0.6)
        assertThat(luminance(mark)).isLessThan(luminance(banana))
    }

    @Test
    fun `a dark colour is lightened on a dark card`() {
        val navy = 0xFF1A237E.toInt()
        val mark = markColor(navy, night)
        assertThat(contrast(mark, night)).isAtLeast(MIN_CONTRAST)
        assertThat(luminance(mark)).isGreaterThan(luminance(navy))
    }

    @Test
    fun `a mark that reads strongly fades as far as asked`() {
        val black = 0xFF000000.toInt()
        assertThat(markAlpha(black, white, 0.45f)).isEqualTo(0.45f)
    }

    @Test
    fun `a faded mark keeps three to one, and fades no less than it must`() {
        val green = markColor(0xFF0B8043.toInt(), white)
        val alpha = markAlpha(green, white, 0.45f)
        assertThat(alpha).isIn(com.google.common.collect.Range.openClosed(0.45f, 1f))
        assertThat(contrast(blend(green, white, alpha), white)).isAtLeast(MIN_CONTRAST)
        assertThat(contrast(blend(green, white, alpha - 0.05f), white)).isLessThan(MIN_CONTRAST)
    }

    @Test
    fun `a mark lifted to three to one on a dark page barely fades`() {
        val mark = markColor(0xFF0B8043.toInt(), night)
        val alpha = markAlpha(mark, night, 0.45f)
        assertThat(alpha).isAtLeast(0.8f)
        assertThat(contrast(blend(mark, night, alpha), night)).isAtLeast(MIN_CONTRAST)
    }

    @Test
    fun `a mark drawn over a lighter face keeps three to one against the ground beside it`() {
        // The dial on the hero's ground: its face is the page, lighter, and a mark fades more
        // over it than over the ground it must still read against.
        val ground = 0xFFE6EFF9.toInt()
        val face = 0xFFF6FAFF.toInt()
        val green = markColor(0xFF0B8043.toInt(), ground)
        val alpha = markAlpha(green, ground, 0.45f, over = face)
        assertThat(alpha).isAtLeast(markAlpha(green, ground, 0.45f))
        assertThat(contrast(blend(green, face, alpha), ground)).isAtLeast(MIN_CONTRAST)
        assertThat(contrast(blend(green, face, alpha), face)).isAtLeast(MIN_CONTRAST)
        assertThat(contrast(blend(green, face, alpha - 0.05f), ground)).isLessThan(MIN_CONTRAST)
    }

    @Test
    fun `a mark drawn over a darker face keeps three to one against the ground beside it`() {
        val ground = 0xFF192028.toInt()
        val face = 0xFF0D141B.toInt()
        val navy = markColor(0xFF1A237E.toInt(), ground)
        val alpha = markAlpha(navy, ground, 0.45f, over = face)
        assertThat(contrast(blend(navy, face, alpha), ground)).isAtLeast(MIN_CONTRAST)
        assertThat(contrast(blend(navy, face, alpha), face)).isAtLeast(MIN_CONTRAST)
    }

    @Test
    fun `a blend is the ground at nothing and the colour at everything`() {
        val green = 0xFF0B8043.toInt()
        assertThat(blend(green, white, 0f)).isEqualTo(white)
        assertThat(blend(green, white, 1f)).isEqualTo(green)
    }

    @Test
    fun `a translucent colour comes out opaque`() {
        val mark = markColor(0x401A237E, white)
        assertThat(mark ushr 24).isEqualTo(0xFF)
    }
}
