package com.callbackdev.tempo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.widget.agenda.AgendaWidgetContent
import com.callbackdev.tempo.widget.words.WordsWidgetContent
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Nothing a card prints is cut (VISION.md, "Fits, never scrolls"), at every reference grant, in
 * English and Italian, at three text sizes, through the day, in the reader's choices and the
 * states, with every line drawn 5% wider than measured ([STRETCH]), as a launcher on another face
 * may draw it (Passo's Samsung, 5 Oct 2026). A line is whole, or in fewer words, or not there; and
 * nothing is laid out below the card's edge. The one thing a card may cut is an event's title,
 * ellipsised on its one line (PLANNING.md §7): the time and the mark say which event it is.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetFitTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val titles = WidgetSamples.instances.mapNotNull { it.title }

    /** A title, or a date's all-day titles one after the other. */
    private fun isTitle(text: String) = titles.any { text.startsWith(it.take(TITLE_PREFIX)) }

    private val models = listOf(
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T07:00")),
        WidgetSamples.model(),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T14:20")),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T21:10")),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-11T23:00")),
        WidgetSamples.model(look = WidgetLook(clockFormat = ClockFormat.H12, dateStyle = DateStyle.NUMERIC)),
        WidgetSamples.model(look = WidgetLook(showClock = false)),
        WidgetSamples.model(look = WidgetLook(showDate = false, showAllDay = false, showDaysAhead = false)),
        WidgetSamples.model(look = WidgetLook(showClock = false, showDate = false)),
        WidgetSamples.model(content = WidgetContent.NoPermission),
        WidgetSamples.model(content = WidgetContent.NoCalendars),
        WidgetSamples.model(content = WidgetContent.Unavailable),
    )

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.OneByTwo,
        Grants.TwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.FourByThree,
    )

    private fun cuts(scale: Float): List<String> {
        RuntimeEnvironment.setFontScale(scale)
        val found = mutableListOf<String>()
        models.forEachIndexed { index, model ->
            sizes.forEach { size ->
                val at = "#$index ${size.width.value.toInt()}x${size.height.value.toInt()} @$scale"
                cutTexts(context, size, STRETCH, ::isTitle) { AgendaWidgetContent(model) }.forEach {
                    found +=
                        "agenda $at: $it"
                }
                cutTexts(context, size, STRETCH, ::isTitle) { WordsWidgetContent(model) }.forEach {
                    found +=
                        "words $at: $it"
                }
            }
        }
        return found
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
    fun `nothing is cut in English, on a wider face`() {
        val found = cuts(1f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `nothing is cut in Italian, on a wider face`() {
        val found = cuts(1f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xhdpi")
    fun `nothing is cut in American English, on the twelve-hour clock, at larger text`() {
        val found = cuts(1.15f) + cuts(1.3f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `nothing is cut in Italian, at larger text`() {
        val found = cuts(1.15f) + cuts(1.3f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    private companion object {
        const val STRETCH = 1.05f
        const val TITLE_PREFIX = 4
    }
}
