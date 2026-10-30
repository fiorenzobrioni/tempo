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
 * Everything is arithmetic on dp and sp, pinned by `AgendaFitTest` at the reference
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
 * @property rangeEm the time column the rows' ranges need, in ems of the regular face ([timeColumn]).
 * @property startEm the same for the start-only times ("→ 11:00" for the one under way).
 * @property titleEm the widest title among the inline rows, in ems of the medium face; 0 when unknown.
 * @property lines how many lines the list has to show (headings and notes included); 0 when
 *   unknown, as for a message, which keeps the inline rows.
 * @property spanning the positions, among the [lines], of the all-day lines that stand over their
 *   titles across the list even where the rows are inline ([TimeColumn.spans]).
 * @property summaryEms the rest of today in a few words ([CardSummary]), its lines in order
 *   ("4 left today", "until 19:30"), in ems of the medium face; empty with nothing left today.
 * @property secondEm the second clock's widest line ("New York 23:58"), in ems of the medium face;
 *   0 where the card does not show one (step 8.5).
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
    val titleEm: Float = 0f,
    val lines: Int = 0,
    val spanning: Set<Int> = emptySet(),
    val summaryEms: List<Float> = emptyList(),
    val secondEm: Float = 0f,
)

/**
 * Where a header's second clock stands (step 8.5): on a line of its own under the date (in a side
 * column; on top, under a clock with no date), at the far edge of the date's line on top, or
 * nowhere: on a row and a cell, or where it does not fit whole.
 */
enum class SecondPlace { NONE, UNDER, BESIDE }

/**
 * The header as drawn: the clock's size (0 when hidden), which date style ([dateChoice] into
 * [AgendaSpec.dateEms], -1 when hidden), its size and lines, and the space the whole block takes;
 * and how many lines of the day in a few words ([summaryLines] of [AgendaSpec.summaryEms]) stand
 * under the date, in a side column only: on top, the list is right under the date; and where the
 * second clock stands ([second]), its line counted in the height where it has one of its own.
 */
data class HeaderPlan(
    val clockSp: Float,
    val dateChoice: Int,
    val dateSp: Float,
    val dateLines: Int,
    val width: Float,
    val height: Float,
    val summaryLines: Int = 0,
    val second: SecondPlace = SecondPlace.NONE,
)

/**
 * A row's time as the inline column may print it, in ems: [full], and [short] where a shorter form
 * says the same ("→ 11:00" for "Until 11:00"); [allDay] for a date's all-day line ("All day",
 * "Day 2 of 5").
 */
data class TimeCell(val full: Float, val short: Float = full, val allDay: Boolean = false)

/**
 * The inline rows' time column ([AgendaFit.timeColumn]): [em] wide, set by the timed rows at their
 * shortest and by the all-day labels about as wide as them.
 */
data class TimeColumn(val em: Float, private val timedEm: Float) {
    /**
     * Whether [cell], an all-day line, stands over its titles across the list instead: its label
     * would widen the column by more than [AgendaFit.ALL_DAY_SLACK] for every title under it.
     */
    fun spans(cell: TimeCell): Boolean = cell.allDay && timedEm > 0f && cell.full > timedEm + AgendaFit.ALL_DAY_SLACK

    /** Whether [cell]'s full form fits the column; else the row prints its short one. */
    fun holdsFull(cell: TimeCell): Boolean = cell.full <= em
}

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
    ): AgendaPlan {
        val inline = rowStyle(width, spec.rangeEm, spec.startEm, spec.fontScale)
        val style = if (inline == RowStyle.INLINE_START &&
            stackingHelps(width, room, spec)
        ) {
            RowStyle.STACKED
        } else {
            inline
        }
        return AgendaPlan(form, header, width, room, style, snug)
    }

    /**
     * Whether a list set inline would cut a title while stacked it would show as many lines: then
     * the time goes over the title, which has the whole column, rather than an ellipsis beside a
     * time over an empty bottom of the card (owner, 9 Oct 2026). Stacking never costs a line.
     */
    private fun stackingHelps(width: Float, room: Float, spec: AgendaSpec): Boolean {
        if (spec.lines <= 0 || spec.titleEm <= 0f) return false
        val inlineTitle = width - MARK_WIDTH - MARK_GAP - widthOf(spec.startEm, TIME_SP, spec.fontScale) - TIME_GAP
        if (widthOf(spec.titleEm, TITLE_SP, spec.fontScale) <= inlineTitle) return false
        fun shown(style: RowStyle): Int {
            // The all-day lines across the list are stacked whatever the other rows are.
            val heights = (0 until spec.lines).map { line ->
                rowHeight(if (line in spec.spanning) RowStyle.STACKED else style, spec.fontScale)
            }
            if (heights.sum() <= room) return spec.lines
            var used = footerHeight(spec.fontScale)
            var shown = 0
            for (height in heights) {
                if (used + height > room) break
                used += height
                shown++
            }
            return shown
        }
        return shown(RowStyle.STACKED) >= shown(RowStyle.INLINE_START)
    }

    /** The clock on top of a tall card: about a quarter of the card's height, never wider than it. */
    private fun topHeader(spec: AgendaSpec): HeaderPlan {
        val width = spec.width - CARD_PADDING * 2
        val inner = spec.height - CARD_PADDING * 2
        val clock = if (spec.clock) {
            val byHeight = clockSizeForLine(inner * TOP_CLOCK_SHARE, spec.fontScale)
            quarterPoint(
                minOf(byHeight.coerceIn(TOP_CLOCK_MIN, TOP_CLOCK_MAX), spThatFits(width, spec.clockEm, spec.fontScale)),
            )
        } else {
            0f
        }
        return secondOnTop(spec, header(spec, clock, width, DATE_SP), width)
    }

    /**
     * The second clock in a header on top: at the far edge of the date's line where both fit whole
     * on it, so it costs the list nothing; on a line of its own under a clock with no date, in the
     * date's place; else not at all. A date is never shortened for it.
     */
    private fun secondOnTop(spec: AgendaSpec, plan: HeaderPlan, width: Float): HeaderPlan {
        if (spec.secondEm <= 0f) return plan
        val second = widthOf(spec.secondEm, SECOND_SP, spec.fontScale)
        return when {
            plan.dateChoice >= 0 -> {
                val date = widthOf(spec.dateEms[plan.dateChoice], plan.dateSp, spec.fontScale)
                if (plan.dateLines == 1 && date + SECOND_GAP + second <= width) {
                    plan.copy(second = SecondPlace.BESIDE)
                } else {
                    plan
                }
            }

            plan.clockSp > 0f && second <= width ->
                plan.copy(second = SecondPlace.UNDER, height = plan.height + secondLine(plan, spec.fontScale))

            else -> plan
        }
    }

    /** The height a second clock's own line adds under [plan]'s lines, with the air over it. */
    private fun secondLine(plan: HeaderPlan, fontScale: Float): Float =
        (if (plan.height > 0f) SECOND_TOP else 0f) + clockLineHeight(SECOND_SP, fontScale)

    /** The clock beside a tall card's list: a column of at most [SIDE_SHARE] of the card. */
    private fun sideHeader(spec: AgendaSpec): HeaderPlan {
        val inner = spec.height - CARD_PADDING * 2
        val most = (spec.width - CARD_PADDING * 2) * SIDE_SHARE
        val clock = if (spec.clock) {
            val byHeight = clockSizeForLine(inner * SIDE_CLOCK_SHARE, spec.fontScale)
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
        var column = plan.copy(width = minOf(most, maxOf(clockWidth, plan.width)))
        // The second clock under the date, where its line fits whole in the room the date had and
        // the column's height holds it.
        if (spec.secondEm > 0f) {
            val second = widthOf(spec.secondEm, SECOND_SP, spec.fontScale)
            val height = column.height + secondLine(column, spec.fontScale)
            if (second <= minOf(room, most) && height <= inner) {
                column = column.copy(width = maxOf(column.width, second), height = height, second = SecondPlace.UNDER)
            }
        }
        // The day in a few words under the date, its lines in order while each fits whole in the
        // room the column already has (it never widens it), and the column's height holds them.
        var lines = 0
        var height = column.height
        for (em in spec.summaryEms) {
            val next = height + (if (lines == 0) SUMMARY_TOP else 0f) + lineHeight(SUMMARY_SP, spec.fontScale)
            if (widthOf(em, SUMMARY_SP, spec.fontScale) > column.width || next > inner) break
            height = next
            lines++
        }
        return column.copy(summaryLines = lines, height = height)
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
            val dateRoom = if (spec.date) clockLineHeight(LINE_DATE_SP, spec.fontScale) else 0f
            val clock = if (spec.clock) {
                val byHeight = clockSizeForLine(inner - dateRoom, spec.fontScale)
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
                clockSizeForLine(room, spec.fontScale),
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
            // The date is a `TextClock` too, without font padding: its lines are the clock's.
            height = clockHeight + clockLineHeight(size, spec.fontScale) * lines,
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

    /**
     * The inline rows' time column for [cells]. The timed rows set it at their shortest: "→ 11:00"
     * where "Until 11:00" would be the widest time of the list and widen the column for every
     * title (the four-by-two cut "Lunch with Gi…" and "Swimming les…" for it, review of 30 Oct
     * 2026); the full form stays where the column is as wide anyway ([TimeColumn.holdsFull]). An
     * all-day label sets it too, unless it would widen it by more than [ALL_DAY_SLACK] ("Tutto il
     * giorno" is as wide as "Fino alle 11:00"): that line stands over its titles across the list
     * instead ([TimeColumn.spans]). A list with no timed row is set by its all-day labels.
     */
    fun timeColumn(cells: List<TimeCell>): TimeColumn {
        val timed = cells.filterNot { it.allDay }.maxOfOrNull { it.short } ?: 0f
        val bare = TimeColumn(timed, timed)
        val labels = cells.filter { it.allDay && !bare.spans(it) }.maxOfOrNull { it.full } ?: 0f
        return TimeColumn(maxOf(timed, labels), timed)
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

    // The card (`docs/adr/0003-widgets.md`): its insets, and the grants its forms change at.
    const val CARD_PADDING = 14f
    const val CARD_PADDING_SNUG = 6f
    const val CELL_PADDING = 8f
    const val TALL_MIN_HEIGHT = 150f
    const val CLOCK_FORM_MAX_WIDTH = 120f
    const val SIDE_MIN_WIDTH = 300f
    const val SIDE_BELOW_ROWS = 5
    const val LIST_MIN_WIDTH = 96f
    const val COLUMN_GAP = 16f
    const val HEADER_GAP = 6f

    // The header.
    const val TOP_CLOCK_SHARE = 0.2f
    const val TOP_CLOCK_MIN = 28f
    const val TOP_CLOCK_MAX = 52f
    const val SIDE_SHARE = 0.45f
    const val SIDE_CLOCK_SHARE = 0.3f
    const val SIDE_CLOCK_MIN = 30f
    const val SIDE_CLOCK_MAX = 48f
    const val SIDE_DATE_MIN = 110f
    const val LINE_SHARE = 0.5f
    const val LINE_CLOCK_MIN = 22f
    const val LINE_DATE_ROOM = 110f

    /** The clock on a row, as «In words»'s time on its: the cards' row numbers match. */
    const val LINE_CLOCK_MAX = WidgetType.HERO

    /**
     * The date under a one-row card's clock: 16 sp, as «In words»'s line on top, so the rows read
     * as one set (owner, 8 Oct 2026).
     */
    const val LINE_DATE_SP = WidgetType.BODY
    const val CELL_CLOCK_MIN = 20f
    const val CELL_CLOCK_MAX = 56f
    const val CELL_DATE_SP = 12f

    /** The date under a tall card's clock: 16 sp, as on a row and «In words»'s line on top. */
    const val DATE_SP = WidgetType.BODY
    const val DATE_ALONE_SP = 18f
    const val DATE_MIN_SP = 11f

    /** How full a line of words breaks: two lines hold less than twice one. */
    const val WRAP_FILL = 0.8f

    // The list: every line of it at the widget scale's [WidgetType.LABEL] (13 Oct 2026, from a
    // 14 sp title, 13 sp time, 12 sp heading, 14 sp note and 12 sp footer). A list's row is one
    // size on every card of the family; the title over its time is sorted by weight and ink.
    const val TITLE_SP = WidgetType.LABEL
    const val TIME_SP = WidgetType.LABEL
    const val HEADING_SP = WidgetType.LABEL
    const val NOTE_SP = WidgetType.LABEL
    const val FOOTER_SP = WidgetType.LABEL
    const val ROW_GAP = 4f
    const val HEADING_TOP = 6f
    const val FOOTER_TOP = 2f
    const val MARK_WIDTH = 3f
    const val MARK_GAP = 8f
    const val TIME_GAP = 8f
    const val TITLE_MIN = 72f
    const val TITLE_COMFORT = 120f
    const val NOTE_MAX_LINES = 2

    /**
     * How much wider than the timed rows' times an all-day label may set the column, in ems of the
     * time: "All day" beside "→ 11:00" and "13:00" stays inline, "Tutto il giorno" (two and a half
     * ems wider) goes over its titles. A line over its titles costs the list a line's height; a
     * column an em wider costs every title about two letters.
     */
    const val ALL_DAY_SLACK = 1f

    // The day in a few words (PLANNING.md §7): at the list's size, in the secondary ink.
    const val SUMMARY_SP = WidgetType.LABEL

    /** Under the date in a side column, as the list is under the header on top. */
    const val SUMMARY_TOP = HEADER_GAP

    /**
     * The second clock (step 8.5): a quiet line at the list's size, as the day in a few words; a
     * little air over it under the date, and a column's gap from the date beside it.
     */
    const val SECOND_SP = WidgetType.LABEL
    const val SECOND_TOP = 2f
    const val SECOND_GAP = 12f
}
