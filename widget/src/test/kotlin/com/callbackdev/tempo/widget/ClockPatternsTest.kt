package com.callbackdev.tempo.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A date over two lines breaks after its first word, so a day's number stays with its month. */
class ClockPatternsTest {
    private fun broken(pattern: String) = ClockPatterns(pattern, pattern).brokenAfterFirstWord()?.twentyFour

    @Test
    fun `the break follows the first word`() {
        assertThat(broken("EEEE d MMMM")).isEqualTo("EEEE\nd MMMM")
        assertThat(broken("EEE, d MMM")).isEqualTo("EEE,\nd MMM")
        assertThat(broken("EEEE, MMMM d")).isEqualTo("EEEE,\nMMMM d")
    }

    @Test
    fun `a space inside a quoted literal is not a break`() {
        assertThat(broken("'a b' EEEE")).isEqualTo("'a b'\nEEEE")
        assertThat(broken("EEEE d 'de' MMMM")).isEqualTo("EEEE\nd 'de' MMMM")
    }

    @Test
    fun `a pattern of one word has no break`() {
        assertThat(broken("EEEE")).isNull()
    }
}
