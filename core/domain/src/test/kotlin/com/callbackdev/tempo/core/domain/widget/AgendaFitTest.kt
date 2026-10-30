package com.callbackdev.tempo.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** «Agenda»'s forms and budgets at the reference grants. */
class AgendaFitTest {
    /** Measured in Roboto, with the card's 6% margin: «10:20» Bold, the three date styles, a row's times. */
    private val clockEm = 2.65f
    private val dateEms = listOf(9.6f, 4.8f, 4.3f)
    private val rangeEm = 6.5f
    private val startEm = 4.9f

    private fun spec(width: Float, height: Float, clock: Boolean = true, date: Boolean = true, scale: Float = 1f) =
        AgendaSpec(width, height, scale, clock, date, clockEm, dateEms, rangeEm, startEm)

    private fun plan(width: Float, height: Float, clock: Boolean = true, date: Boolean = true, scale: Float = 1f) =
        AgendaFit.plan(spec(width, height, clock, date, scale))

    @Test
    fun `every reference grant has its form`() {
        assertThat(plan(85f, 85f).form).isEqualTo(AgendaForm.CLOCK)
        assertThat(plan(85f, 189f).form).isEqualTo(AgendaForm.CLOCK)
        assertThat(plan(159f, 85f).form).isEqualTo(AgendaForm.CLOCK)
        assertThat(plan(250f, 85f).form).isEqualTo(AgendaForm.LINE)
        assertThat(plan(340f, 85f).form).isEqualTo(AgendaForm.LINE)
        assertThat(plan(159f, 189f).form).isEqualTo(AgendaForm.AGENDA)
        assertThat(plan(250f, 189f).form).isEqualTo(AgendaForm.AGENDA)
        assertThat(plan(340f, 189f).form).isEqualTo(AgendaForm.SIDE)
        assertThat(plan(340f, 293f).form).isEqualTo(AgendaForm.AGENDA)
    }

    @Test
    fun `with the clock and the date hidden the card is its list, on any grant`() {
        listOf(85f to 85f, 340f to 85f, 159f to 189f, 340f to 189f).forEach { (w, h) ->
            val plan = plan(w, h, clock = false, date = false)
            assertThat(plan.header).isNull()
            assertThat(plan.listWidth).isEqualTo(w - AgendaFit.CARD_PADDING * 2)
        }
    }

    @Test
    fun `a one-row card shortens a long date to leave its list a row of times and titles`() {
        val row = plan(340f, 85f)
        assertThat(row.header?.dateChoice).isEqualTo(1)
        assertThat(row.rowStyle).isNotEqualTo(RowStyle.STACKED)
    }

    @Test
    fun `the one-cell clock fills the cell and drops the date before it would shrink under its floor`() {
        val cell = checkNotNull(plan(85f, 85f).header)
        assertThat(cell.clockSp).isAtLeast(AgendaFit.CELL_CLOCK_MIN)
        assertThat(cell.clockSp * clockEm + FIT_SLACK).isAtMost(85f - AgendaFit.CELL_PADDING * 2)
        assertThat(cell.height).isAtMost(85f - AgendaFit.CARD_PADDING_SNUG * 2)
    }

    @Test
    fun `the date alone on a cell is larger than under a clock`() {
        val alone = checkNotNull(plan(85f, 85f, clock = false).header)
        assertThat(alone.clockSp).isEqualTo(0f)
        assertThat(alone.dateChoice).isAtLeast(0)
    }

    @Test
    fun `the header never takes more than the card holds`() {
        listOf(159f to 189f, 250f to 189f, 340f to 189f, 340f to 293f, 250f to 85f, 340f to 85f).forEach { (w, h) ->
            listOf(1f, 1.3f, 2f).forEach { scale ->
                val plan = plan(w, h, scale = scale)
                val header = checkNotNull(plan.header)
                assertThat(header.width).isAtMost(w - AgendaFit.CARD_PADDING * 2)
                val pad = if (plan.snug) AgendaFit.CARD_PADDING_SNUG else AgendaFit.CARD_PADDING
                assertThat(header.height).isAtMost(h - pad * 2)
            }
        }
    }

    @Test
    fun `the top clock grows with the card's height, within its range`() {
        val short = checkNotNull(plan(250f, 189f).header).clockSp
        val tall = checkNotNull(plan(250f, 397f).header).clockSp
        assertThat(short).isAtLeast(AgendaFit.TOP_CLOCK_MIN)
        assertThat(tall).isGreaterThan(short)
        assertThat(tall).isAtMost(AgendaFit.TOP_CLOCK_MAX)
    }

    @Test
    fun `the side column takes no more than its share, and the list the rest`() {
        val side = plan(340f, 189f)
        val inner = 340f - AgendaFit.CARD_PADDING * 2
        assertThat(checkNotNull(side.header).width).isAtMost(inner * AgendaFit.SIDE_SHARE)
        assertThat(side.listWidth).isEqualTo(inner - checkNotNull(side.header).width - AgendaFit.COLUMN_GAP)
        assertThat(side.listRoom).isEqualTo(189f - AgendaFit.CARD_PADDING * 2)
    }

    @Test
    fun `at large text a four-by-two keeps the side form`() {
        assertThat(plan(340f, 189f, scale = 1.3f).form).isEqualTo(AgendaForm.SIDE)
    }

    @Test
    fun `a one-row card writes its date at 16 sp, under a clock that keeps its size`() {
        val header = checkNotNull(plan(340f, 85f).header)
        assertThat(header.dateSp).isEqualTo(AgendaFit.LINE_DATE_SP)
        assertThat(header.clockSp).isEqualTo(AgendaFit.LINE_CLOCK_MAX)
        assertThat(header.height).isAtMost(85f - AgendaFit.CARD_PADDING_SNUG * 2)
    }

    @Test
    fun `a one-row card at twice the text drops the date and keeps the time`() {
        val header = checkNotNull(plan(340f, 85f, scale = 2f).header)
        assertThat(header.clockSp).isAtLeast(AgendaFit.LINE_CLOCK_MIN)
        assertThat(header.dateChoice).isEqualTo(-1)
    }

    @Test
    fun `a row sets its time as a range where the title keeps its minimum, else its start, else above it`() {
        assertThat(AgendaFit.rowStyle(312f, rangeEm, startEm, 1f)).isEqualTo(RowStyle.INLINE_RANGE)
        assertThat(AgendaFit.rowStyle(222f, rangeEm, startEm, 1f)).isEqualTo(RowStyle.INLINE_START)
        assertThat(AgendaFit.rowStyle(170f, rangeEm, startEm, 1f)).isEqualTo(RowStyle.INLINE_START)
        assertThat(AgendaFit.rowStyle(131f, rangeEm, startEm, 1f)).isEqualTo(RowStyle.STACKED)
    }

    @Test
    fun `a two-by-two lists two events under its clock`() {
        val plan = plan(159f, 189f)
        val two = AgendaFit.rowHeight(plan.rowStyle, 1f) * 2 + AgendaFit.footerHeight(1f)
        assertThat(plan.listRoom).isAtLeast(two)
    }

    @Test
    fun `a stacked row is taller than an inline one, and both grow with the text`() {
        val inline = AgendaFit.rowHeight(RowStyle.INLINE_RANGE, 1f)
        val stacked = AgendaFit.rowHeight(RowStyle.STACKED, 1f)
        assertThat(stacked).isGreaterThan(inline)
        assertThat(AgendaFit.rowHeight(RowStyle.INLINE_RANGE, 1.3f)).isGreaterThan(inline)
    }

    @Test
    fun `the default four-by-two lists at least five events beside its clock`() {
        val side = plan(340f, 189f)
        val rows = side.listRoom / AgendaFit.rowHeight(side.rowStyle, 1f)
        assertThat(rows.toInt()).isAtLeast(5)
    }

    @Test
    fun `a long title is stacked under its time where stacking costs no line`() {
        val inline = plan(340f, 189f)
        assertThat(inline.rowStyle).isEqualTo(RowStyle.INLINE_START)
        val two = AgendaFit.plan(spec(340f, 189f).copy(titleEm = 9f, lines = 2))
        assertThat(two.rowStyle).isEqualTo(RowStyle.STACKED)
    }

    @Test
    fun `rows stay inline where stacking would cost a line, or no title is cut`() {
        val full = AgendaFit.plan(spec(340f, 189f).copy(titleEm = 9f, lines = 6))
        assertThat(full.rowStyle).isEqualTo(RowStyle.INLINE_START)
        val short = AgendaFit.plan(spec(340f, 189f).copy(titleEm = 3f, lines = 2))
        assertThat(short.rowStyle).isEqualTo(RowStyle.INLINE_START)
    }

    // --- The time column ("→ 11:00", the all-day line across the list), review of 30 Oct 2026 ----

    /** Measured as [startEm] is: the start-only times of the README's sample at 10:20. */
    private val arrowEm = 3.4f
    private val oneEm = 2.6f
    private val allDayEm = 3.1f
    private val tuttoIlGiornoEm = 6.5f
    private val finoAlleEm = 6.8f

    @Test
    fun `the event under way sets the column by its arrow, where its until would widen it`() {
        val column = AgendaFit.timeColumn(
            listOf(TimeCell(allDayEm, allDay = true), TimeCell(startEm, arrowEm), TimeCell(oneEm), TimeCell(oneEm)),
        )
        assertThat(column.em).isEqualTo(arrowEm)
        assertThat(column.holdsFull(TimeCell(startEm, arrowEm))).isFalse()
        assertThat(column.spans(TimeCell(allDayEm, allDay = true))).isFalse()
    }

    @Test
    fun `until stays where the column is as wide anyway`() {
        // The ranges ("13:00 – 14:00") are wider than "Until 11:00": the column holds it whole.
        val column = AgendaFit.timeColumn(listOf(TimeCell(startEm, arrowEm), TimeCell(rangeEm)))
        assertThat(column.em).isEqualTo(rangeEm)
        assertThat(column.holdsFull(TimeCell(startEm, arrowEm))).isTrue()
    }

    @Test
    fun `an all-day label much wider than the times goes over its titles, and leaves the column to them`() {
        val tutto = TimeCell(tuttoIlGiornoEm, allDay = true)
        val column = AgendaFit.timeColumn(listOf(tutto, TimeCell(finoAlleEm, arrowEm), TimeCell(oneEm)))
        assertThat(column.spans(tutto)).isTrue()
        assertThat(column.em).isEqualTo(arrowEm)
    }

    @Test
    fun `a list of all-day lines alone is set by them`() {
        val tutto = TimeCell(tuttoIlGiornoEm, allDay = true)
        val column = AgendaFit.timeColumn(listOf(tutto, TimeCell(allDayEm, allDay = true)))
        assertThat(column.spans(tutto)).isFalse()
        assertThat(column.em).isEqualTo(tuttoIlGiornoEm)
    }

    @Test
    fun `an all-day line across the list is weighed at its stacked height, where it stands`() {
        // Four inline rows fit a four-by-two whole; four lines over their titles would not, and
        // then stacking every row shows as many and cuts no title.
        val wide = spec(340f, 189f).copy(startEm = arrowEm, titleEm = 12f, lines = 4)
        assertThat(AgendaFit.plan(wide).rowStyle).isEqualTo(RowStyle.INLINE_START)
        assertThat(AgendaFit.plan(wide.copy(spanning = setOf(0, 1, 2, 3))).rowStyle).isEqualTo(RowStyle.STACKED)
        // Today's all-day line over its titles, and the days ahead's further down, past what the
        // card shows: the rows stay inline, five lines where stacked ones would be three.
        val week = wide.copy(lines = 15, spanning = setOf(0, 6, 9, 12))
        assertThat(AgendaFit.plan(week).rowStyle).isEqualTo(RowStyle.INLINE_START)
    }

    @Test
    fun `the four-by-two's titles keep their room once the arrow sets the column`() {
        val before = AgendaFit.plan(spec(340f, 189f))
        val after = AgendaFit.plan(spec(340f, 189f).copy(startEm = arrowEm))
        val gained = AgendaFit.timeColumn(before.rowStyle, rangeEm, startEm, 1f) -
            AgendaFit.timeColumn(after.rowStyle, rangeEm, arrowEm, 1f)
        // Two letters and more for every title: "Lunch with Gi…" was short of "ulia".
        assertThat(after.rowStyle).isEqualTo(RowStyle.INLINE_START)
        assertThat(gained).isAtLeast(18f)
    }

    // --- The day in a few words, under the date in the side column -------------------------

    /** "4 left today", "until 19:30" in the medium face. */
    private val summaryEms = listOf(6.2f, 5.2f)

    private fun withSummary(width: Float, height: Float, scale: Float = 1f, ems: List<Float> = summaryEms) =
        AgendaFit.plan(spec(width, height, scale = scale).copy(summaryEms = ems))

    @Test
    fun `the four-by-two's side column says the rest of the day under the date, in the room it has`() {
        val without = checkNotNull(plan(340f, 189f).header)
        val with = checkNotNull(withSummary(340f, 189f).header)
        assertThat(with.summaryLines).isEqualTo(2)
        assertThat(with.width).isEqualTo(without.width)
        assertThat(with.height).isEqualTo(
            without.height + AgendaFit.SUMMARY_TOP + lineHeight(AgendaFit.SUMMARY_SP, 1f) * 2,
        )
        assertThat(with.height).isAtMost(189f - AgendaFit.CARD_PADDING * 2)
        assertThat(withSummary(340f, 189f).listWidth).isEqualTo(plan(340f, 189f).listWidth)
    }

    @Test
    fun `a line too wide for the column is left out, and the ones after it too`() {
        // The count alone where "until tomorrow at 06:30" does not fit; nothing where the count does not.
        assertThat(checkNotNull(withSummary(340f, 189f, ems = listOf(6.2f, 11f)).header).summaryLines).isEqualTo(1)
        assertThat(checkNotNull(withSummary(340f, 189f, ems = listOf(11f, 5.2f)).header).summaryLines).isEqualTo(0)
    }

    @Test
    fun `the side column never grows past the card for the summary, at any text size`() {
        listOf(1f, 1.3f, 2f).forEach { scale ->
            val plan = withSummary(340f, 189f, scale)
            if (plan.form != AgendaForm.SIDE) return@forEach
            val header = checkNotNull(plan.header)
            assertThat(header.height).isAtMost(189f - AgendaFit.CARD_PADDING * 2)
            summaryEms.take(header.summaryLines).forEach { em ->
                assertThat(widthOf(em, AgendaFit.SUMMARY_SP, scale)).isAtMost(header.width)
            }
        }
    }

    @Test
    fun `only a side column says it, not a header on top, a row or a cell`() {
        listOf(159f to 189f, 250f to 189f, 340f to 293f, 85f to 85f, 340f to 85f, 85f to 189f).forEach { (w, h) ->
            val header = checkNotNull(withSummary(w, h).header)
            assertThat(header.summaryLines).isEqualTo(0)
            assertThat(header.height).isEqualTo(checkNotNull(plan(w, h).header).height)
        }
    }

    /** "New York 23:58" in the medium face. */
    private val secondEm = 7f

    private fun withSecond(width: Float, height: Float, scale: Float = 1f, date: Boolean = true, em: Float = secondEm) =
        AgendaFit.plan(spec(width, height, date = date, scale = scale).copy(secondEm = em, summaryEms = summaryEms))

    @Test
    fun `the four-by-two's side column holds the second clock under the date, and the day in a few words under it`() {
        val without = checkNotNull(withSummary(340f, 189f).header)
        val with = checkNotNull(withSecond(340f, 189f).header)
        assertThat(with.second).isEqualTo(SecondPlace.UNDER)
        assertThat(with.summaryLines).isEqualTo(2)
        assertThat(with.height).isEqualTo(
            without.height + AgendaFit.SECOND_TOP + clockLineHeight(AgendaFit.SECOND_SP, 1f),
        )
        assertThat(with.height).isAtMost(189f - AgendaFit.CARD_PADDING * 2)
    }

    @Test
    fun `a second clock too wide for the side column is left out, the column kept`() {
        val plan = withSecond(340f, 189f, em = 14f)
        assertThat(checkNotNull(plan.header).second).isEqualTo(SecondPlace.NONE)
        assertThat(plan.listWidth).isEqualTo(withSummary(340f, 189f).listWidth)
    }

    @Test
    fun `on top, the second clock stands at the far edge of the date's line where both fit, for no height`() {
        val wide = checkNotNull(withSecond(340f, 293f).header)
        assertThat(wide.second).isEqualTo(SecondPlace.BESIDE)
        assertThat(wide.height).isEqualTo(checkNotNull(plan(340f, 293f).header).height)
        // A two-by-two's date leaves no room beside it, and is not shortened for the second clock.
        val narrow = checkNotNull(withSecond(159f, 189f).header)
        assertThat(narrow.second).isEqualTo(SecondPlace.NONE)
        assertThat(narrow.dateChoice).isEqualTo(checkNotNull(plan(159f, 189f).header).dateChoice)
    }

    @Test
    fun `on top with no date, the second clock takes the date's line`() {
        val header = checkNotNull(withSecond(250f, 189f, date = false).header)
        assertThat(header.second).isEqualTo(SecondPlace.UNDER)
        assertThat(header.height).isEqualTo(
            checkNotNull(plan(250f, 189f, date = false).header).height + AgendaFit.SECOND_TOP +
                clockLineHeight(AgendaFit.SECOND_SP, 1f),
        )
    }

    @Test
    fun `a row and a cell have no second clock`() {
        listOf(85f to 85f, 250f to 85f, 340f to 85f, 85f to 189f).forEach { (w, h) ->
            assertThat(checkNotNull(withSecond(w, h).header).second).isEqualTo(SecondPlace.NONE)
        }
    }

    @Test
    fun `the second clock never takes the header past the card, at any text size`() {
        listOf(159f to 189f, 250f to 189f, 340f to 189f, 340f to 293f).forEach { (w, h) ->
            listOf(1f, 1.3f, 2f).forEach { scale ->
                listOf(true, false).forEach { date ->
                    val plan = withSecond(w, h, scale, date)
                    val header = checkNotNull(plan.header)
                    assertThat(header.height).isAtMost(h - AgendaFit.CARD_PADDING * 2)
                    assertThat(header.width).isAtMost(w - AgendaFit.CARD_PADDING * 2)
                    if (header.second == SecondPlace.UNDER) {
                        assertThat(widthOf(secondEm, AgendaFit.SECOND_SP, scale)).isAtMost(
                            if (plan.form == AgendaForm.SIDE) header.width else w - AgendaFit.CARD_PADDING * 2,
                        )
                    }
                }
            }
        }
    }
}
