package com.callbackdev.tempo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.widget.agenda.AgendaWidgetContent
import com.callbackdev.tempo.widget.words.WordsWidgetContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every form of both cards, drawn to `build/screenshots` to be looked at after a change to a card
 * (CLAUDE.md). Not an assertion: the arithmetic is pinned by `AgendaFitTest` and `WordsTest`, and
 * what is cut by `WidgetFitTest`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetGalleryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/screenshots")

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.TwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.FourByThree,
    )

    @Test
    fun `agenda, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(size to renderCard(context, size) { AgendaWidgetContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-agenda-sizes")
    }

    @Test
    fun `in words, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(size to renderCard(context, size) { WordsWidgetContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-words-sizes")
    }

    @Test
    fun `the pair side by side, in every dress`() {
        val looks = listOf(
            WidgetLook(),
            WidgetLook(cardColor = WidgetCardColor.CLAY),
            WidgetLook(background = WidgetBackground.LIGHT),
            WidgetLook(background = WidgetBackground.DARK),
            WidgetLook(opacityPct = 0, background = WidgetBackground.SYSTEM),
        )
        val board = HomeBoard(context, widthDp = 380)
        looks.forEach { look ->
            board.row(
                Grants.FourByTwo to
                    renderCard(context, Grants.FourByTwo) { AgendaWidgetContent(WidgetSamples.model(look)) },
            )
            board.row(
                Grants.FourByTwo to
                    renderCard(context, Grants.FourByTwo) { WordsWidgetContent(WidgetSamples.model(look)) },
            )
        }
        board.draw().saveTo(out, "widget-dresses")
    }

    @Test
    fun `through the day`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf("2026-10-07T07:00", "2026-10-07T14:20", "2026-10-07T21:10").forEach { time ->
            val model = WidgetSamples.model(now = WidgetSamples.at(time))
            board.row(Grants.FourByTwo to renderCard(context, Grants.FourByTwo) { AgendaWidgetContent(model) })
            board.row(Grants.FourByTwo to renderCard(context, Grants.FourByTwo) { WordsWidgetContent(model) })
        }
        val tall = WidgetSamples.model(now = WidgetSamples.at("2026-10-07T21:10"))
        board.row(Grants.FourByThree to renderCard(context, Grants.FourByThree) { AgendaWidgetContent(tall) })
        board.draw().saveTo(out, "widget-through-the-day")
    }

    @Test
    fun `the reader's choices`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(
            WidgetLook(showClock = false, showDate = false),
            WidgetLook(showClock = false),
            WidgetLook(showDate = false, clockFormat = ClockFormat.H12),
            WidgetLook(dateStyle = DateStyle.NUMERIC, showAllDay = false, showDaysAhead = false),
        ).forEach { look ->
            board.row(
                Grants.FourByTwo to
                    renderCard(context, Grants.FourByTwo) { AgendaWidgetContent(WidgetSamples.model(look)) },
            )
            board.row(
                Grants.FourByOne to
                    renderCard(context, Grants.FourByOne) { WordsWidgetContent(WidgetSamples.model(look)) },
            )
        }
        board.draw().saveTo(out, "widget-choices")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in Italian`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.OneByOne, Grants.TwoByOne).forEach { size ->
            board.row(
                size to renderCard(context, size) { AgendaWidgetContent(WidgetSamples.model()) },
                size to renderCard(context, size) { WordsWidgetContent(WidgetSamples.model()) },
            )
        }
        listOf(Grants.FourByOne, Grants.TwoByTwo, Grants.FourByTwo).forEach { size ->
            board.row(size to renderCard(context, size) { AgendaWidgetContent(WidgetSamples.model()) })
            board.row(size to renderCard(context, size) { WordsWidgetContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-italian")
    }

    @Test
    fun `states that stand in for the day`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(WidgetContent.NoPermission, WidgetContent.AllHidden, WidgetContent.Unavailable).forEach { content ->
            val model = WidgetSamples.model(content = content)
            board.row(Grants.FourByTwo to renderCard(context, Grants.FourByTwo) { AgendaWidgetContent(model) })
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsWidgetContent(model) })
        }
        val none = WidgetSamples.model(content = WidgetContent.NoPermission)
        board.row(
            Grants.OneByOne to renderCard(context, Grants.OneByOne) { AgendaWidgetContent(none) },
            Grants.OneByOne to renderCard(context, Grants.OneByOne) { WordsWidgetContent(none) },
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsWidgetContent(none) },
        )
        board.draw().saveTo(out, "widget-states")
    }
}
