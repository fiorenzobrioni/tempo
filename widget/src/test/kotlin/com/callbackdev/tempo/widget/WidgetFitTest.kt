package com.callbackdev.tempo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.UserSettings
import com.callbackdev.tempo.widget.agenda.AgendaWidgetContent
import com.callbackdev.tempo.widget.words.WordsWidgetContent
import com.google.common.truth.Truth.assertThat
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
 * may draw it (a Samsung, 5 Oct 2026). A line is whole, or in fewer words, or not there; and
 * nothing is laid out below the card's edge. The one thing a card may cut is an event's title,
 * ellipsised on its one line (PLANNING.md §7): the time and the mark say which event it is.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetFitTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Under way at 23:00 and over at 06:30: an end on a later date says its day, and must fit too. */
    private val nightTrain = WidgetSamples.instances.first().copy(
        eventId = 99,
        title = "Night train",
        begin = WidgetSamples.at("2026-10-07T22:30").toInstant(),
        end = WidgetSamples.at("2026-10-08T06:30").toInstant(),
    )

    private val titles = (WidgetSamples.instances + nightTrain).mapNotNull { it.title }

    /** A title, or a date's all-day titles one after the other. */
    private fun isTitle(text: String) = titles.any { text.startsWith(it.take(TITLE_PREFIX)) }

    private val models = listOf(
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T07:00")),
        WidgetSamples.model(),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T14:20")),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T21:10")),
        WidgetSamples.model(
            now = WidgetSamples.at("2026-10-07T23:00"),
            instances = WidgetSamples.instances + nightTrain,
        ),
        WidgetSamples.model(now = WidgetSamples.at("2026-10-11T23:00")),
        // The whole day left to say under the date: six events, from the run at 07:30.
        WidgetSamples.model(now = WidgetSamples.at("2026-10-07T05:00")),
        WidgetSamples.model(look = WidgetLook(clockFormat = ClockFormat.H12, dateStyle = DateStyle.NUMERIC)),
        WidgetSamples.model(look = WidgetLook(showClock = false)),
        WidgetSamples.model(look = WidgetLook(showClock = false, dateStyle = DateStyle.NUMERIC)),
        WidgetSamples.model(look = WidgetLook(showDate = false, showAllDay = false, showDaysAhead = false)),
        WidgetSamples.model(look = WidgetLook(showClock = false, showDate = false)),
        // The second clock (step 8.5): a long city, on the twelve-hour clock, without the dial or the date.
        WidgetSamples.model(look = WidgetLook(showSecondZone = true), settings = second("Asia/Saigon")),
        WidgetSamples.model(
            look = WidgetLook(showSecondZone = true, clockFormat = ClockFormat.H12),
            settings = second("America/Los_Angeles"),
        ),
        WidgetSamples.model(
            look = WidgetLook(showSecondZone = true, showClock = false),
            settings = second("Asia/Tokyo"),
        ),
        WidgetSamples.model(
            look = WidgetLook(showSecondZone = true, showDate = false),
            settings = second("Asia/Tokyo"),
        ),
        WidgetSamples.model(content = WidgetContent.NoPermission),
        WidgetSamples.model(content = WidgetContent.NoCalendars),
        WidgetSamples.model(content = WidgetContent.Unavailable),
    )

    private fun second(zone: String) = UserSettings(onboardingCompleted = true, secondZone = zone)

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.OneByTwo,
        Grants.TwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.TwoByThree,
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

    /**
     * The README's four-by-two at 10:20: every event of the rest of today with its title whole.
     * Before the arrow and the all-day line across the list, English cut "Lunch with Gi…" and
     * "Swimming les…" for a column as wide as "Until 11:00", and Italian stacked every row for
     * "Tutto il giorno" and listed three events (review of 30 Oct 2026).
     */
    private fun assertFourByTwoWhole() {
        val shown = shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(WidgetSamples.model()) }
        assertThat(shown).containsAtLeast("Design review", "Lunch with Giulia", "Dentist", "Swimming lesson")
        assertThat(shown).contains("→ 11:00")
        val cut = cutTexts(context, Grants.FourByTwo, 1f) { AgendaWidgetContent(WidgetSamples.model()) }
        assertWithMessage(cut.joinToString("\n")).that(cut).isEmpty()
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
    fun `the four-by-two lists the rest of today, every title whole, in English`() = assertFourByTwoWhole()

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `the four-by-two lists the rest of today, every title whole, in Italian`() {
        assertFourByTwoWhole()
        val shown = shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(WidgetSamples.model()) }
        assertThat(shown).containsAtLeast("Tutto il giorno", "Ancora 4 oggi", "fino alle 19:30")
    }

    /**
     * «In words» three rows high: the next two events as rows under the note, which keeps the count
     * where they leave one out; one row and the note without its count where one is left; each
     * row read as one sentence, as «Agenda»'s. Two rows high, the line of times stays: the
     * dial's comfort leaves it one line (step 8.3).
     */
    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
    fun `a tall in words lists the next events where it has the room`() {
        val morning = shownTexts(context, Grants.FourByThree) { WordsWidgetContent(WidgetSamples.model()) }
        assertThat(morning).containsAtLeast("Then 3 more today.", "13:00", "Lunch with Giulia", "15:00", "Dentist")
            .inOrder()
        assertThat(morning).doesNotContain("Swimming lesson")
        assertThat(morning.filter { it.startsWith("Then 13:00") }).isEmpty()

        val afternoon = WidgetSamples.model(now = WidgetSamples.at("2026-10-07T14:20"))
        val one = shownTexts(context, Grants.FourByThree) { WordsWidgetContent(afternoon) }
        assertThat(one).containsAtLeast("Free until then.", "18:30", "Swimming lesson").inOrder()

        val read = describedViews(context, Grants.FourByThree) { WordsWidgetContent(WidgetSamples.model()) }
        assertThat(read).containsAtLeast(
            "Design review, Until 11:00",
            "13:00 to 14:00, Lunch with Giulia, Trattoria da Mario, in Personal",
            "15:00 to 15:45, Dentist, Via Roma 3, in Personal",
        ).inOrder()

        val short = shownTexts(context, Grants.FourByTwo) { WordsWidgetContent(WidgetSamples.model()) }
        assertThat(short).contains("Then 13:00 · 15:00 · 18:30")
        assertThat(short).doesNotContain("Lunch with Giulia")
    }

    /**
     * The second clock (step 8.5): the city and its time, frozen at the sample's 10:20 in Rome,
     * under the date in «Agenda»'s side column and beside «In words»' dial; at the far edge of the
     * date's line on «Agenda»'s header on top; nowhere on a row or where the card has no room.
     */
    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
    fun `the second clock is drawn where the card has room for it`() {
        val newYork = WidgetSamples.model(
            look = WidgetLook(showSecondZone = true),
            settings = second("America/New_York"),
        )
        assertThat(shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(newYork) }).contains("New York 04:20")
        assertThat(shownTexts(context, Grants.FourByThree) { AgendaWidgetContent(newYork) }).contains("New York 04:20")
        assertThat(shownTexts(context, Grants.FourByTwo) { WordsWidgetContent(newYork) }).contains("New York 04:20")
        assertThat(
            shownTexts(context, Grants.FourByOne) {
                AgendaWidgetContent(newYork)
            },
        ).doesNotContain("New York 04:20")
        assertThat(
            shownTexts(context, Grants.FourByOne) {
                WordsWidgetContent(newYork)
            },
        ).doesNotContain("New York 04:20")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
    fun `no second clock unless the card asks for it, or in its own city`() {
        val off = WidgetSamples.model(settings = second("America/New_York"))
        assertThat(shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(off) }).doesNotContain("New York 04:20")
        val home = WidgetSamples.model(look = WidgetLook(showSecondZone = true), settings = second("Europe/Rome"))
        assertThat(shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(home) }.filter { it.startsWith("Rome") })
            .isEmpty()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `the second clock in Italian`() {
        val tokyo = WidgetSamples.model(look = WidgetLook(showSecondZone = true), settings = second("Asia/Tokyo"))
        assertThat(shownTexts(context, Grants.FourByTwo) { AgendaWidgetContent(tokyo) }).contains("Tokyo 17:20")
    }
}
