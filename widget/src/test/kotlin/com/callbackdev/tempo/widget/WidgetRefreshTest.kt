package com.callbackdev.tempo.widget

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/**
 * Whatever asks for a repaint stays until the cards have drawn it (the process is not left cached,
 * and frozen, with a card half drawn), and a new session starts from the card's last model.
 */
class WidgetRefreshTest {
    private val sample = WidgetSamples.model()

    @After
    fun forgetTheCards() = WidgetRefresh.forget(intArrayOf(CARD, OTHER))

    @Test
    fun `a repaint is waited for until every card has drawn its revision`() = runTest {
        val revision = WidgetRefresh.invalidate()
        val waiting = async { WidgetRefresh.awaitDrawn(listOf(CARD, OTHER), revision, timeoutMillis = 5_000) }
        WidgetRefresh.drawn(CARD, revision, sample)
        // An older revision does not count: that card has not drawn the change yet.
        WidgetRefresh.drawn(OTHER, revision - 1, sample)
        testScheduler.advanceTimeBy(1_000)
        assertThat(waiting.isCompleted).isFalse()
        WidgetRefresh.drawn(OTHER, revision, sample)
        assertThat(waiting.await()).isTrue()
    }

    @Test
    fun `a card that never draws is waited for no longer than the timeout`() = runTest {
        val revision = WidgetRefresh.invalidate()
        assertThat(WidgetRefresh.awaitDrawn(listOf(CARD), revision, timeoutMillis = 2_000)).isFalse()
    }

    @Test
    fun `a card's last model is kept for its next session, and forgotten with the card`() {
        WidgetRefresh.drawn(CARD, WidgetRefresh.revision.value, sample)
        assertThat(WidgetRefresh.lastModel(CARD)).isEqualTo(sample)
        WidgetRefresh.forget(intArrayOf(CARD))
        assertThat(WidgetRefresh.lastModel(CARD)).isNull()
    }

    private companion object {
        const val CARD = 9_001
        const val OTHER = 9_002
    }
}
