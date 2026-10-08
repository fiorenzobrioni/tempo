package com.callbackdev.tempo.core.domain.widget

/*
 * «Agenda» (PLANNING.md §7): the clock and the date over the rest of the day, laid out for the
 * size the launcher really granted. Four forms, so every size is a composition and not a squeezed
 * one:
 *
 * - [AgendaForm.CLOCK], one cell, or a one-row card too narrow for a list beside the clock: the
 *   time, and the date under it where there is room.
 * - [AgendaForm.LINE], one row: the clock and the date on the leading side, the next events
 *   beside them.
 * - [AgendaForm.AGENDA], two rows and up: the clock and the date on top, the events under them,
 *   then "N more today".
 * - [AgendaForm.SIDE], two rows and up, four cells wide, where a header on top would leave the
 *   list fewer than [SIDE_BELOW_ROWS] rows: the clock and the date in a column on the leading
 *   side, the events beside them (the default 4×2 placement).
 *
 * With the clock and the date both hidden the card is the list alone, on any grant.
 *
 * Everything is arithmetic on dp and sp, pinned by `AgendaFitTest` at the family's reference
 * grants (a row ~85 dp tall, two ~189; two cells ~159 dp wide, three ~250, four ~340). What only
 * the launcher's face can answer (how wide a time or a date is) is measured by the caller, in ems.
 */

enum class AgendaForm { CLOCK, LINE, AGENDA, SIDE }

/**
 * How an event row is set: the time in a column before the title, as a range ("10:00 – 11:00") or
 * its start alone; or, where the column would leave the title too little, the time on a small
 * line over the title.
 */
enum class RowStyle { INLINE_RANGE, INLINE_START, STACKED }

/**
 * What a card is asked to hold, measured.
 *
 * @property clockEm the widest time the clock may show, in ems of the bold face.
 * @property dateEms the date in each style the card may use, in ems of the regular face, the
 *   reader's own first and shorter ones after it: a date that does not fit is shortened before it
 *   is shrunk, and never cut.
 * @property rangeEm the widest time range among the rows, in ems of the regular face.
 * @property startEm the widest start-only time among the rows ("until 11:00" included).
 */
data class AgendaSpec(
    val width: Float,
    val height: Float,
    val fontScale: Float,
    val clock: Boolean,
    val date: Boolean,
    val clockEm: Float,
    val dateEms: List<Float>,
    val rangeEm: Float,
    val startEm: Float,
)

/**
 * The header as drawn: the clock's size (0 when hidden), which date style ([dateChoice] into
 * [AgendaSpec.dateEms], -1 when hidden), its size and lines, and the space the whole block takes.
 */
data class HeaderPlan(
    val clockSp: Float,
    val dateChoice: Int,
    val dateSp: Float,
    val dateLines: Int,
    val width: Float,
    val height: Float,
)

/**
 * The card's plan.
 *
 * @property header null when the reader hid both the clock and the date.
 * @property listWidth the list's column; 0 on [AgendaForm.CLOCK], which has none.
 * @property listRoom the height the list may fill, its footer included.
 */
data class AgendaPlan(
    val form: AgendaForm,
    val header: HeaderPlan?,
    val listWidth: Float,
    val listRoom: Float,
    val rowStyle: RowStyle,
    val snug: Boolean,
    val sidePadding: Float = AgendaFit.CARD_PADDING,
)

object AgendaFit {
    fun plan(spec: AgendaSpec): AgendaPlan {
        val tall = spec.height >= TALL_MIN_HEIGHT
        val innerWidth = spec.width - CARD_PADDING * 2
        if (!spec.clock && !spec.date) {
            val snug = !tall
            val room = spec.height - (if (snug) CARD_PADDING_SNUG else CARD_PADDING) * 2
            return listPlan(AgendaForm.AGENDA, null, innerWidth, room, spec, snug)
        }
        if (spec.width < CLOCK_FORM_MAX_WIDTH) return clockPlan(spec, tall)
        if (!tall) {
            val header = lineHeader(spec)
            val listWidth = innerWidth - header.width - COLUMN_GAP
            if (listWidth < LIST_MIN_WIDTH) return clockPlan(spec, tall = false)
            return listPlan(AgendaForm.LINE, header, listWidth, spec.height - CARD_PADDING_SNUG * 2, spec, true)
        }
        val innerHeight = spec.height - CARD_PADDING * 2
        val top = topHeader(spec)
        val agenda = listPlan(AgendaForm.AGENDA, top, innerWidth, innerHeight - top.height - HEADER_GAP, spec, false)
        if (spec.width < SIDE_MIN_WIDTH) return agenda
        val rows = (agenda.listRoom / rowHeight(agenda.rowStyle, spec.fontScale)).toInt()
        if (rows >= SIDE_BELOW_ROWS) return agenda
        val side = sideHeader(spec)
        return listPlan(AgendaForm.SIDE, side, innerWidth - side.width - COLUMN_GAP, innerHeight, spec, false)
    }

    private fun listPlan(
        form: AgendaForm,
        header: HeaderPlan?,
        width: Float,
        room: Float,
        spec: AgendaSpec,
        snug: Boolean,
    ) = AgendaPlan(form, header, width, room, rowStyle(width, spec.rangeEm, spec.startEm, spec.fontScale), snug)

    /** The clock on top of a tall card: about a quarter of the card's height, never wider than it. */
    private fun topHeader(spec: AgendaSpec): HeaderPlan {
        val width = spec.width - CARD_PADDING * 2
        val inner = spec.height - CARD_PADDING * 2
        val clock = if (spec.clock) {
            val byHeight = sizeForLine(inner * TOP_CLOCK_SHARE, spec.fontScale, CLOCK_BOX_EM)
            quarterPoint(
                minOf(byHeight.coerceIn(TOP_CLOCK_MIN, TOP_CLOCK_MAX), spThatFits(width, spec.clockEm, spec.fontScale)),
            )
        } else {
            0f
        }
        return header(spec, clock, width, DATE_SP)
    }

    /** The clock beside a tall card's list: a column of at most [SIDE_SHARE] of the card. */
    private fun sideHeader(spec: AgendaSpec): HeaderPlan {
        val inner = spec.height - CARD_PADDING * 2
        val most = (spec.width - CARD_PADDING * 2) * SIDE_SHARE
        val clock = if (spec.clock) {
            val byHeight = sizeForLine(inner * SIDE_CLOCK_SHARE, spec.fontScale, CLOCK_BOX_EM)
            quarterPoint(
                minOf(
                    byHeight.coerceIn(SIDE_CLOCK_MIN, SIDE_CLOCK_MAX),
                    spThatFits(most, spec.clockEm, spec.fontScale),
                ),
            )
        } else {
            0f
        }
        val clockWidth = if (spec.clock) widthOf(spec.clockEm, clock, spec.fontScale) else 0f
        // The date may take two lines in the column, never a wider column than the clock needs
        // unless it is the date alone.
        val room = if (spec.clock) maxOf(clockWidth, SIDE_DATE_MIN) else most
        val plan = header(spec, clock, minOf(room, most), DATE_SP, maxLines = 2)
        return plan.copy(width = minOf(most, maxOf(clockWidth, plan.width)))
    }

    /**
     * The clock on a one-row card: as tall as the row leaves under the date, up to
     * [LINE_CLOCK_MAX]. Where the row cannot hold the date under the clock at its floor (a large
     * text size), the date goes: the time is what the row is read by.
     */
    private fun lineHeader(spec: AgendaSpec): HeaderPlan {
        val inner = spec.height - CARD_PADDING_SNUG * 2
        val most = (spec.width - CARD_PADDING * 2) * LINE_SHARE
        fun planFor(spec: AgendaSpec): HeaderPlan {
            val dateRoom = if (spec.date) lineHeight(LINE_DATE_SP, spec.fontScale) else 0f
            val clock = if (spec.clock) {
                val byHeight = sizeForLine(inner - dateRoom, spec.fontScale, CLOCK_BOX_EM)
                quarterPoint(
                    minOf(
                        byHeight.coerceIn(LINE_CLOCK_MIN, LINE_CLOCK_MAX),
                        spThatFits(most, spec.clockEm, spec.fontScale),
                    ),
                )
            } else {
                0f
            }
            // The date takes no wider a column than the clock needs, or [LINE_DATE_ROOM]: on a row,
            // two events beside the clock say more than a long date, which is shortened instead.
            val clockWidth = if (spec.clock) widthOf(spec.clockEm, clock, spec.fontScale) else 0f
            val room = if (spec.clock) minOf(most, maxOf(clockWidth, LINE_DATE_ROOM)) else most
            return header(spec, clock, room, if (spec.clock) LINE_DATE_SP else DATE_ALONE_SP)
        }
        val plan = planFor(spec)
        return if (plan.height > inner && spec.clock && spec.date) planFor(spec.copy(date = false)) else plan
    }

    /**
     * One cell: the clock as large as the cell holds, the date under it where it still leaves the
     * clock [CELL_CLOCK_MIN]; without the clock, the date alone, larger.
     */
    private fun clockPlan(spec: AgendaSpec, tall: Boolean): AgendaPlan {
        val pad = if (tall) CARD_PADDING else CARD_PADDING_SNUG
        // One cell is all clock: the card's corner needs less air beside it than words do.
        val side = if (spec.width < CLOCK_FORM_MAX_WIDTH) CELL_PADDING else CARD_PADDING
        val width = spec.width - side * 2
        val inner = spec.height - pad * 2
        fun clockFor(room: Float) = quarterPoint(
            minOf(
                sizeForLine(room, spec.fontScale, CLOCK_BOX_EM),
                spThatFits(width, spec.clockEm, spec.fontScale),
                CELL_CLOCK_MAX,
            ),
        )
        val withDate = if (spec.date) header(spec, 0f, width, CELL_DATE_SP) else null
        var plan = if (!spec.clock) {
            header(spec, 0f, width, DATE_ALONE_SP)
        } else {
            val dateRoom = withDate?.takeIf { it.dateChoice >= 0 }?.height ?: 0f
            val clock = clockFor(inner - dateRoom)
            if (withDate != null && clock >= CELL_CLOCK_MIN) {
                withDate.copy(clockSp = clock, height = withDate.height + clockLineHeight(clock, spec.fontScale))
            } else {
                val alone = clockFor(inner)
                HeaderPlan(alone, -1, 0f, 0, width, clockLineHeight(alone, spec.fontScale))
            }
        }
        plan = plan.copy(width = width)
        return AgendaPlan(AgendaForm.CLOCK, plan, 0f, 0f, RowStyle.STACKED, snug = !tall, sidePadding = side)
    }

    /**
     * The date as [width] holds it: the reader's style at [sizeSp] if it fits, else (where
     * [maxLines] allows) the reader's own over two lines, else the first shorter one that fits,
     * else the shortest at the size that fits it, down to [DATE_MIN_SP], else none; the clock's line above it.
     */
    private fun header(spec: AgendaSpec, clockSp: Float, width: Float, sizeSp: Float, maxLines: Int = 1): HeaderPlan {
        val clockHeight = if (clockSp > 0f) clockLineHeight(clockSp, spec.fontScale) else 0f
        val clockWidth = if (clockSp > 0f) widthOf(spec.clockEm, clockSp, spec.fontScale) else 0f
        if (!spec.date || spec.dateEms.isEmpty()) return HeaderPlan(clockSp, -1, 0f, 0, clockWidth, clockHeight)
        val whole = spec.dateEms.indexOfFirst { widthOf(it, sizeSp, spec.fontScale) <= width }
        val (choice, size, lines) = when {
            whole == 0 -> Triple(0, sizeSp, 1)

            // Two lines of the reader's own date, where the column allows them, before a shorter one.
            maxLines >= 2 && widthOf(spec.dateEms.first(), sizeSp, spec.fontScale) <= width * 2 * WRAP_FILL ->
                Triple(0, sizeSp, 2)

            whole > 0 -> Triple(whole, sizeSp, 1)

            else -> {
                val last = spec.dateEms.lastIndex
                val shrunk = quarterPoint(minOf(sizeSp, spThatFits(width, spec.dateEms[last], spec.fontScale)))
                // A date too small to read, or cut, says less than none: the clock stands alone.
                if (shrunk < DATE_MIN_SP) return HeaderPlan(clockSp, -1, 0f, 0, clockWidth, clockHeight)
                Triple(last, shrunk, 1)
            }
        }
        val dateWidth = minOf(width, widthOf(spec.dateEms[choice], size, spec.fontScale))
        return HeaderPlan(
            clockSp = clockSp,
            dateChoice = choice,
            dateSp = size,
            dateLines = lines,
            width = maxOf(clockWidth, dateWidth),
            height = clockHeight + lineHeight(size, spec.fontScale) * lines,
        )
    }

    /**
     * The row style a list of [width] can give its times: the range where it still leaves the
     * title [TITLE_COMFORT], the start alone where that leaves it [TITLE_MIN], else the time over
     * the title. A row's title is what the reader reads it by; its end is the calendar app's to tell.
     */
    fun rowStyle(width: Float, rangeEm: Float, startEm: Float, fontScale: Float): RowStyle {
        val title = width - MARK_WIDTH - MARK_GAP - TIME_GAP
        return when {
            title - widthOf(rangeEm, TIME_SP, fontScale) >= TITLE_COMFORT -> RowStyle.INLINE_RANGE
            title - widthOf(startEm, TIME_SP, fontScale) >= TITLE_MIN -> RowStyle.INLINE_START
            else -> RowStyle.STACKED
        }
    }

    /** The time column of an inline row; none on a stacked one. */
    fun timeColumn(style: RowStyle, rangeEm: Float, startEm: Float, fontScale: Float): Float = when (style) {
        RowStyle.INLINE_RANGE -> widthOf(rangeEm, TIME_SP, fontScale)
        RowStyle.INLINE_START -> widthOf(startEm, TIME_SP, fontScale)
        RowStyle.STACKED -> 0f
    }

    fun rowHeight(style: RowStyle, fontScale: Float): Float = when (style) {
        RowStyle.INLINE_RANGE, RowStyle.INLINE_START -> lineHeight(TITLE_SP, fontScale) + ROW_GAP
        RowStyle.STACKED -> lineHeight(TIME_SP, fontScale) + lineHeight(TITLE_SP, fontScale) + ROW_GAP
    }

    fun headingHeight(fontScale: Float): Float = HEADING_TOP + lineHeight(HEADING_SP, fontScale) + ROW_GAP

    fun noteHeight(lines: Int, fontScale: Float): Float = lineHeight(NOTE_SP, fontScale) * lines + ROW_GAP

    fun footerHeight(fontScale: Float): Float = FOOTER_TOP + lineHeight(FOOTER_SP, fontScale)

    fun height(line: CardLine, style: RowStyle, fontScale: Float, noteLines: Int): Float = when (line) {
        is CardLine.Heading -> headingHeight(fontScale)
        is CardLine.Note -> noteHeight(noteLines, fontScale)
        is CardLine.AllDay, is CardLine.Timed -> rowHeight(style, fontScale)
    }

    // The family's card (Chiaro's, Passo's): its insets, and the grants its forms change at.
    const val CARD_PADDING = 14f
    const val CARD_PADDING_SNUG = 6f
    const val CELL_PADDING = 8f
    const val TALL_MIN_HEIGHT = 150f
    const val CLOCK_FORM_MAX_WIDTH = 120f
    const val SIDE_MIN_WIDTH = 300f
    const val SIDE_BELOW_ROWS = 5
    const val LIST_MIN_WIDTH = 96f
    const val COLUMN_GAP = 16f
    const val HEADER_GAP = 8f

    // The header.
    const val TOP_CLOCK_SHARE = 0.22f
    const val TOP_CLOCK_MIN = 30f
    const val TOP_CLOCK_MAX = 52f
    const val SIDE_SHARE = 0.45f
    const val SIDE_CLOCK_SHARE = 0.3f
    const val SIDE_CLOCK_MIN = 30f
    const val SIDE_CLOCK_MAX = 48f
    const val SIDE_DATE_MIN = 110f
    const val LINE_SHARE = 0.5f
    const val LINE_CLOCK_MIN = 22f
    const val LINE_DATE_ROOM = 110f
    const val LINE_CLOCK_MAX = 36f
    const val LINE_DATE_SP = 13f
    const val CELL_CLOCK_MIN = 20f
    const val CELL_CLOCK_MAX = 56f
    const val CELL_DATE_SP = 12f
    const val DATE_SP = 14f
    const val DATE_ALONE_SP = 18f
    const val DATE_MIN_SP = 11f

    /** How full a line of words breaks: two lines hold less than twice one. */
    const val WRAP_FILL = 0.8f

    // The list.
    const val TITLE_SP = 14f
    const val TIME_SP = 13f
    const val HEADING_SP = 12f
    const val NOTE_SP = 14f
    const val FOOTER_SP = 12f
    const val ROW_GAP = 4f
    const val HEADING_TOP = 6f
    const val FOOTER_TOP = 2f
    const val MARK_WIDTH = 3f
    const val MARK_GAP = 8f
    const val TIME_GAP = 8f
    const val TITLE_MIN = 72f
    const val TITLE_COMFORT = 120f
    const val NOTE_MAX_LINES = 2
}
