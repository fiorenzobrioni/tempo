package com.callbackdev.tempo.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** «Agenda»'s forms and budgets at the family's reference grants. */
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
    fun `a one-row card writes its date at the family's 16 sp, under a clock that keeps its size`() {
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
}
