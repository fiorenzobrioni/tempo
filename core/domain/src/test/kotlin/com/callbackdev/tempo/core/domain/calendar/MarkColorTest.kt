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
    fun `a translucent colour comes out opaque`() {
        val mark = markColor(0x401A237E, white)
        assertThat(mark ushr 24).isEqualTo(0xFF)
    }
}
