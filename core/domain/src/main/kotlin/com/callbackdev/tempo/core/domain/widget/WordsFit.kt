package com.callbackdev.tempo.core.domain.widget

/*
 * «In words» (PLANNING.md §7): a dial and the day in a few words (owner, 8 Oct 2026, after the
 * first cards on the phone). The dial is the picture a card is read by across a room: the system's
 * own hands over what is left of today, the focus drawn on its ring ([DialArcs]). The words say
 * what the dial cannot: what it is, and when, in clock times.
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the dial: the hands, and the focus's arc on the ring | the family's circle: [WordsFit.cellCircle], [WordsFit.rowCircle], [WordsFit.tallCircle] | – | strong, accent |
 * | 2 | the focus's title ("Design review"; "Free" when nothing is left) | [TITLE_SP], [TALL_TITLE_SP] | Medium | strong |
 * | 3 | when ("Until 11:00", "15:00 – 16:00", "Tomorrow · 9:00 – 10:00") | [WHEN_SP] | Regular | quiet |
 * | 4 | the date (the system's `TextClock`, so it turns at midnight) | [DATE_SP] | Regular | quiet |
 * | 5 | the day's note; the next events as rows, else the line of times | [NOTE_SP], [ROW_SP] | Regular; a row's title Medium | quiet; a row's title strong |
 *
 * No number is large: a time printed at a count's size made a sentence shout ("Until 18:30" at
 * 34 sp left the title three words and an ellipsis), and the dial already says the hour. The
 * sizes: the title at 20 sp, the time and the date at 16 sp.
 *
 * Three forms:
 *
 * - [WordsForm.CELL], one cell wide: the dial as large as the cell, the focus's time under it where
 *   the cell is taller than wide.
 * - [WordsForm.ROW], one row. The dial; the title over its time; the date at the far edge, over
 *   two lines, where the row has the room.
 * - [WordsForm.TALL], two rows and up. The dial in the top trailing corner with the date beside
 *   it, or over the title where the dial leaves it no column; the title, its time and the note
 *   hanging from the bottom leading corner, and under them, where the card has the room, the next
 *   events as rows ("13:00  Lunch").
 *
 * The dial's side is the family's circle for the card's size, the one every card of the family
 * draws there, never the day's words' (owner, 4 Nov 2026).
 */
enum class WordsForm { CELL, ROW, TALL }

/**
 * What the card is asked to hold, measured.
 *
 * @property dial whether the reader shows the clock: the dial, hands and all.
 * @property dateEms the date in each style the card may use, one line in ems of the regular face,
 *   the reader's first and shorter ones after it; empty when the reader hid it.
 * @property dateTwoLineEms the same dates broken over two lines: the wider of the two, in ems.
 * @property titleEm the focus's title on one line, in ems of the medium face.
 * @property titleLines the lines the title takes across a tall card at [WordsFit.TALL_TITLE_SP].
 * @property whenEms the focus's time in words, in ems of the regular face, the fullest first
 *   ("Until 18:30", then "18:30"); empty where there is none ("Free").
 * @property noteLines the lines the note takes across a tall card at [WordsFit.NOTE_SP].
 * @property then whether there are later starts today for the line of times.
 * @property rows how many of the events after the focus a tall card would list as rows, at most
 *   [WordsFit.ROWS_MAX]: all of them or none.
 * @property rowTimeEm the widest of those rows' times ("13:00"), in ems of the regular face.
 * @property secondEm the second clock's widest line ("New York 23:58"), in ems of the regular
 *   face; 0 where the card does not show one (step 8.5).
 * @property note the reader's choice to show the day's note and what follows it on a tall card
 *   (the title's second line, the next events as rows, the line of times); off, a tall card says
 *   the event alone, its title on one line and its time, and its dial takes the band they leave
 *   ([WordsFit.tallCircle]), the size the family's ring takes without its sentence (owner,
 *   5 Nov 2026).
 */
data class WordsSpec(
    val width: Float,
    val height: Float,
    val fontScale: Float,
    val dial: Boolean,
    val dateEms: List<Float>,
    val dateTwoLineEms: List<Float> = dateEms,
    val titleEm: Float,
    val titleLines: Int,
    val whenEms: List<Float>,
    val noteLines: Int,
    val then: Boolean,
    val rows: Int = 0,
    val rowTimeEm: Float = 0f,
    val secondEm: Float = 0f,
    val note: Boolean = true,
)

/**
 * The card's plan: the dial's side (0: none); the date's style ([dateChoice] into
 * [WordsSpec.dateEms], -1: none), size, lines and column; the title's size and lines (0: none);
 * which time in words ([whenChoice] into [WordsSpec.whenEms], -1: none) at which size, bold where
 * it stands in the dial's place; the note's lines; how many rows of the next events (0: none) and
 * their time column's width; whether the line of times is drawn; whether the second clock is,
 * under the date ([second]); whether the date stands over the title rather than beside the dial
 * ([dateOver]).
 */
data class WordsPlan(
    val form: WordsForm,
    val dial: Float,
    val dateChoice: Int,
    val dateSp: Float,
    val dateLines: Int,
    val dateWidth: Float,
    val titleSp: Float,
    val titleLines: Int,
    val whenChoice: Int,
    val whenSp: Float,
    val whenBold: Boolean,
    val noteLines: Int,
    val then: Boolean,
    val snug: Boolean,
    val sidePadding: Float,
    val rows: Int = 0,
    val rowTimeWidth: Float = 0f,
    val second: Boolean = false,
    val dateOver: Boolean = false,
)

/** A date as drawn: which style, at which size, on how many lines, how wide and tall. */
private data class DatePick(val choice: Int, val sp: Float, val lines: Int, val width: Float, val height: Float)

/** A time in words as drawn: which, at which size. */
private data class WhenPick(val choice: Int, val sp: Float)

object WordsFit {
    fun form(width: Float, height: Float): WordsForm = when {
        width < AgendaFit.CLOCK_FORM_MAX_WIDTH -> WordsForm.CELL
        height < AgendaFit.TALL_MIN_HEIGHT -> WordsForm.ROW
        else -> WordsForm.TALL
    }

    fun plan(spec: WordsSpec): WordsPlan = when (form(spec.width, spec.height)) {
        WordsForm.CELL -> cellPlan(spec)
        WordsForm.ROW -> rowPlan(spec)
        WordsForm.TALL -> tallPlan(spec)
    }

    /**
     * The date in [width] by [height]: the first style that fits on one line, no wider than
     * [oneLineMost], or over two lines where [twoLines]; a size smaller before none. Never cut.
     */
    private fun date(
        spec: WordsSpec,
        width: Float,
        height: Float,
        twoLines: Boolean,
        oneLineMost: Float = Float.MAX_VALUE,
    ): DatePick? {
        val scale = spec.fontScale
        DATE_SIZES.forEach { sp ->
            val line = clockLineHeight(sp, scale)
            spec.dateEms.indices.forEach { i ->
                val one = widthOf(spec.dateEms[i], sp, scale)
                if (one <= minOf(width, oneLineMost) && line <= height) return DatePick(i, sp, 1, one, line)
                val two = widthOf(spec.dateTwoLineEms[i], sp, scale)
                if (twoLines && two <= width && line * 2 <= height) return DatePick(i, sp, 2, two, line * 2)
            }
        }
        return null
    }

    /** The fullest time in words that fits [width] whole at one of [sizes], tried size by size. */
    private fun whenFor(spec: WordsSpec, width: Float, sizes: List<Float>): WhenPick? {
        spec.whenEms.forEachIndexed { i, em ->
            sizes.forEach { sp -> if (widthOf(em, sp, spec.fontScale) <= width) return WhenPick(i, sp) }
        }
        return null
    }

    /**
     * One cell: the dial is the cell's circle ([cellCircle]), and the focus's time under it where
     * the cell is tall enough to hold it whole beside the dial's size, the title under that where a
     * line is left. Without the dial, the time in its place, bold and as large as fits, the title
     * under it where a line is left.
     */
    private fun cellPlan(spec: WordsSpec): WordsPlan {
        val snug = spec.height < AgendaFit.TALL_MIN_HEIGHT
        val inner = spec.height - (if (snug) AgendaFit.CARD_PADDING_SNUG else AgendaFit.CARD_PADDING) * 2
        val width = spec.width - AgendaFit.CELL_PADDING * 2
        val scale = spec.fontScale
        val base = WordsPlan(
            form = WordsForm.CELL,
            dial = 0f,
            dateChoice = -1,
            dateSp = DATE_SP,
            dateLines = 0,
            dateWidth = 0f,
            titleSp = CELL_TITLE_SP,
            titleLines = 0,
            whenChoice = -1,
            whenSp = CELL_WHEN_SP,
            whenBold = false,
            noteLines = 0,
            then = false,
            snug = snug,
            sidePadding = AgendaFit.CELL_PADDING,
        )
        if (spec.dial) {
            val dial = cellCircle(spec.width, spec.height)
            val time = whenFor(spec, width, listOf(CELL_WHEN_SP))
            val under = time?.let { lineHeight(it.sp, scale) + CELL_GAP } ?: 0f
            if (time == null || inner - dial < under) return base.copy(dial = dial)
            val title = inner - dial - under >= lineHeight(CELL_TITLE_SP, scale)
            return base.copy(
                dial = dial,
                whenChoice = time.choice,
                whenSp = time.sp,
                titleLines = if (title) 1 else 0,
            )
        }
        val title = lineHeight(CELL_TITLE_SP, scale)
        fun size(em: Float, room: Float) =
            quarterPoint(minOf(sizeForLine(room, scale), spThatFits(width, em, scale), CELL_WHEN_MAX))
        // The fullest time that keeps [CELL_WHEN_MIN], else the shortest as large as it goes.
        val choice = spec.whenEms.indices.firstOrNull { size(spec.whenEms[it], inner) >= CELL_WHEN_MIN }
            ?: spec.whenEms.lastIndex
        if (choice < 0) {
            return base.copy(titleSp = quarterPoint(minOf(sizeForLine(inner, scale), CELL_WHEN_MAX)), titleLines = 1)
        }
        val withTitle = size(spec.whenEms[choice], inner - title)
        val showTitle = withTitle >= CELL_WHEN_MIN
        return base.copy(
            whenChoice = choice,
            whenSp = if (showTitle) withTitle else size(spec.whenEms[choice], inner),
            whenBold = true,
            titleLines = if (showTitle) 1 else 0,
        )
    }

    /**
     * One row: the dial is the row's circle ([rowCircle]), drawn where the words beside it keep
     * [ROW_WORDS_MIN]; the title over its time; the date at the far
     * edge where the words keep what they need ([TITLE_KEEP] of the title), over two lines when on
     * one it would be wider than [ROW_DATE_ONE_LINE]. Under a large text size the time goes before
     * the title shrinks under [TITLE_MIN_SP]. A narrow row sets the words a size smaller, and a long
     * title over two lines where the height holds them.
     */
    private fun rowPlan(spec: WordsSpec): WordsPlan {
        val scale = spec.fontScale
        val inner = spec.height - AgendaFit.CARD_PADDING_SNUG * 2
        val width = spec.width - AgendaFit.CARD_PADDING * 2
        val dial = if (spec.dial && width - DIAL_GAP - ROW_DIAL_MIN >= ROW_WORDS_MIN) {
            rowCircle(spec.width, spec.height)
        } else {
            0f
        }
        val words = width - (if (dial > 0f) dial + DIAL_GAP else 0f)
        val narrow = words < ROW_WORDS_COMFORT
        val time = whenFor(spec, words, if (narrow) NARROW_WHEN_SIZES else WHEN_SIZES)
        val most = if (narrow) NARROW_TITLE_SP else TITLE_SP
        val timeHeight = time?.let { lineHeight(it.sp, scale) } ?: 0f
        val withTime = minOf(most, sizeForLine(inner - timeHeight, scale))
        val keepTime = time != null && withTime >= TITLE_MIN_SP
        val titleSp = quarterPoint(if (keepTime) withTime else minOf(most, sizeForLine(inner, scale)))
        val shown = time?.takeIf { keepTime }
        // A narrow row gives a long title the second line its height holds, rather than an ellipsis
        // three letters in.
        val shownHeight = shown?.let { lineHeight(it.sp, scale) } ?: 0f
        val titleLines = if (narrow && widthOf(spec.titleEm, titleSp, scale) > words &&
            lineHeight(titleSp, scale) * 2 + shownHeight <= inner
        ) {
            2
        } else {
            1
        }
        val timeWidth = shown?.let { widthOf(spec.whenEms[it.choice], it.sp, scale) } ?: 0f
        val need = maxOf(timeWidth, minOf(widthOf(spec.titleEm, titleSp, scale), TITLE_KEEP))
        val dateRoom = words - need - DATE_GAP
        val date = if (dateRoom >= DATE_COLUMN_MIN) {
            date(spec, dateRoom, inner, twoLines = true, oneLineMost = ROW_DATE_ONE_LINE)
        } else {
            null
        }
        return WordsPlan(
            form = WordsForm.ROW,
            dial = dial,
            dateChoice = date?.choice ?: -1,
            dateSp = date?.sp ?: DATE_SP,
            dateLines = date?.lines ?: 0,
            dateWidth = date?.width ?: 0f,
            titleSp = titleSp,
            titleLines = titleLines,
            whenChoice = shown?.choice ?: -1,
            whenSp = shown?.sp ?: WHEN_SP,
            whenBold = false,
            noteLines = 0,
            then = false,
            snug = true,
            sidePadding = AgendaFit.CARD_PADDING,
        )
    }

    /**
     * Two rows and up. The dial is the tall circle ([tallCircle]), a size of the card alone, so it
     * is one size through the day; it yields only where the title's first line and its time would
     * not fit under it (a large text size), never under [TALL_CIRCLE_MIN]. The date stands beside
     * it where its column holds it ([DATE_BESIDE_MIN]), else on its own line over the title
     * ([dateOver]), whichever leaves the larger dial; the second clock under the date. Then, in the
     * band left under the dial, bottom up: the note, whole up to [NOTE_MAX_LINES] or not at all;
     * then, where they fit with the dial at [TALL_DIAL_COMFORT], the title's second line, for a
     * title too long for one even at [TALL_TITLE_MIN_SP], and the next events as rows, all of them
     * or none, else the line of times on a wide card. What is left is air. Without the dial, the date on top and the second clock under it.
     */
    private fun tallPlan(spec: WordsSpec): WordsPlan {
        val scale = spec.fontScale
        val inner = spec.height - AgendaFit.CARD_PADDING * 2
        val width = spec.width - AgendaFit.CARD_PADDING * 2
        // A title a little too long for one line steps down to [TALL_TITLE_MIN_SP] to stay whole;
        // a longer one keeps its size and takes a second line where the card has one.
        // With the event alone the title has one line: a long one at [TALL_TITLE_MIN_SP], to say more
        // of it before its ellipsis.
        val fits = spThatFits(width, spec.titleEm, scale)
        val oneLine = fits >= TALL_TITLE_MIN_SP
        val titleSp = when {
            oneLine -> quarterPoint(minOf(TALL_TITLE_SP, fits))
            spec.note -> TALL_TITLE_SP
            else -> TALL_TITLE_MIN_SP
        }
        val titleLine = lineHeight(titleSp, scale)
        var left = inner - titleLine
        val time = whenFor(spec, width, WHEN_SIZES)
        if (time != null) left -= lineHeight(time.sp, scale)
        val withDate = spec.dateEms.isNotEmpty()
        val secondWidth = widthOf(spec.secondEm, SECOND_SP, scale)
        val secondLine = clockLineHeight(SECOND_SP, scale)
        var dial = 0f
        var date: DatePick? = null
        var dateOver = false
        var second = false
        if (spec.dial && left - DIAL_BOTTOM_GAP >= TALL_CIRCLE_MIN) {
            val most = minOf(tallCircle(spec.width, spec.height, scale, spec.note), left - DIAL_BOTTOM_GAP)
            dial = most
            if (withDate) {
                fun beside(side: Float) = if (width - side - DIAL_GAP >= DATE_BESIDE_MIN) {
                    date(spec, width - side - DIAL_GAP, side, twoLines = true)
                } else {
                    null
                }
                val besideMost = beside(most)
                if (besideMost != null) {
                    date = besideMost
                } else {
                    // Too wide for the date's column: the date over the title, or beside a dial
                    // made smaller for it, whichever dial is the larger; neither, and it goes. With
                    // the event alone the dial is never made smaller for the date (owner, 5 Nov
                    // 2026): over the title where it fits under the circle, else it goes.
                    val over = date(spec, width, Float.MAX_VALUE, twoLines = false)
                    val overDial = over?.let { minOf(most, left - DIAL_BOTTOM_GAP - it.height - DATE_OVER_GAP) } ?: 0f
                    val narrower = minOf(most, width - DIAL_GAP - DATE_BESIDE_MIN)
                    val besideNarrower = beside(narrower)?.takeIf { narrower >= TALL_CIRCLE_MIN && spec.note }
                    val overFits = over != null && overDial >= TALL_CIRCLE_MIN && (spec.note || overDial >= most)
                    when {
                        over != null && overFits && (besideNarrower == null || overDial >= narrower) -> {
                            date = over
                            dateOver = true
                            dial = overDial
                            left -= over.height + DATE_OVER_GAP
                        }

                        besideNarrower != null -> {
                            date = besideNarrower
                            dial = narrower
                        }
                    }
                }
            }
            left -= dial + DIAL_BOTTOM_GAP
            if (spec.secondEm > 0f) {
                second = if (dateOver) {
                    // Over the title, under the date, where a line is left for it.
                    (secondWidth <= width && left >= SECOND_TOP + secondLine).also {
                        if (it) left -= SECOND_TOP + secondLine
                    }
                } else {
                    // Beside the dial, under the date, where its line fits the band whole; the date
                    // is never shortened for it.
                    secondWidth <= width - dial - DIAL_GAP &&
                        (date?.let { it.height + SECOND_TOP } ?: 0f) + secondLine <= dial
                }
            }
        } else {
            if (withDate) {
                date = date(spec, width, Float.MAX_VALUE, twoLines = false)?.takeIf { left >= it.height + TOP_GAP }
                date?.let { left -= it.height + TOP_GAP }
            }
            // Without the dial, the second clock stands on top under the date, or alone in its place.
            val line = secondLine + if (date != null) SECOND_TOP else TOP_GAP
            if (spec.secondEm > 0f && secondWidth <= width && left >= line) {
                second = true
                left -= line
            }
        }
        // The note whole or not at all: a sentence cut off mid-phrase says less than none.
        val noteHeight = linesHeight(NOTE_SP, spec.noteLines, scale) + NOTE_GAP
        val noteLines =
            if (spec.note && spec.noteLines in 1..NOTE_MAX_LINES && left >= noteHeight) spec.noteLines else 0
        if (noteLines > 0) left -= noteHeight
        // The rest come where they fit with the dial at its comfort, as they did when the dial grew
        // to it first: a smaller circle leaves air, and a card keeps the words it held.
        if (dial > 0f) left -= (TALL_DIAL_COMFORT - dial).coerceIn(0f, left.coerceAtLeast(0f))
        var titleLines = 1
        // The title's second line, in the same Text as its first: the face's ascent and descent.
        val secondTitleLine = linesHeight(titleSp, 2, scale) - titleLine
        if (spec.note && !oneLine && spec.titleLines >= 2 && left >= secondTitleLine) {
            left -= secondTitleLine
            titleLines = 2
        }
        // The rows stand where the line of times would, each title keeping [ROW_TITLE_MIN] beside
        // the time column, or an ellipsis says nothing.
        val rowTime = if (spec.rows > 0) widthOf(spec.rowTimeEm, ROW_SP, scale) else 0f
        val rowsHeight = lineHeight(ROW_SP, scale) * spec.rows + ROWS_GAP
        val rows = if (spec.note && spec.rows in 1..ROWS_MAX && width - rowTime - ROW_TIME_GAP >= ROW_TITLE_MIN &&
            left >= rowsHeight
        ) {
            spec.rows
        } else {
            0
        }
        if (rows > 0) left -= rowsHeight
        val then = spec.note && rows == 0 && spec.then && width >= THEN_MIN_WIDTH &&
            left >= lineHeight(NOTE_SP, scale)
        return WordsPlan(
            form = WordsForm.TALL,
            dial = quarterPoint(dial),
            dateChoice = date?.choice ?: -1,
            dateSp = date?.sp ?: DATE_SP,
            dateLines = date?.lines ?: 0,
            dateWidth = date?.width ?: 0f,
            titleSp = titleSp,
            titleLines = titleLines,
            whenChoice = time?.choice ?: -1,
            whenSp = time?.sp ?: WHEN_SP,
            whenBold = false,
            noteLines = noteLines,
            then = then,
            snug = false,
            sidePadding = AgendaFit.CARD_PADDING,
            rows = rows,
            rowTimeWidth = if (rows > 0) rowTime else 0f,
            second = second,
            dateOver = dateOver,
        )
    }

    /**
     * The most a tall card's dial grows to: [TALL_DIAL_SHARE] of the card's inner height and of its
     * inner width, so a large card's face grows with it and its words keep their room; never less
     * than [TALL_DIAL_CEILING], where a card's own room already holds the dial under it.
     */
    fun tallDialCeiling(innerHeight: Float, innerWidth: Float): Float =
        maxOf(TALL_DIAL_CEILING, TALL_DIAL_SHARE * minOf(innerHeight, innerWidth))

    /*
     * The family's circle (owner, 4 Nov 2026): every card of the family that draws a circle (a dial,
     * a ring) draws it at the size these three give for the card's size, whatever the day's words
     * say, so two cards of one size side by side on the home screen show one circle, and a card's
     * own does not grow on a free day. It grows with the card, and with the reader's text size it
     * yields where the words need the room.
     */

    /** One cell wide: the cell's shorter side, less [CELL_CIRCLE_INSET] on each side. */
    fun cellCircle(width: Float, height: Float): Float =
        (minOf(width, height) - CELL_CIRCLE_INSET * 2).coerceIn(CELL_CIRCLE_MIN, CELL_DIAL_MAX)

    /**
     * One row: the height inside the snug insets, but never so wide that the words beside it keep
     * less than [ROW_CIRCLE_WORDS]; between [ROW_DIAL_MIN] and [ROW_DIAL_MAX].
     */
    fun rowCircle(width: Float, height: Float): Float = minOf(
        height - AgendaFit.CARD_PADDING_SNUG * 2,
        width - AgendaFit.CARD_PADDING * 2 - DIAL_GAP - ROW_CIRCLE_WORDS,
    ).coerceIn(ROW_DIAL_MIN, ROW_DIAL_MAX)

    /**
     * Two rows and up: the height inside the card left over a budgeted band of words
     * ([TALL_WORDS_BAND], [WIDE_WORDS_BAND] from [WIDE_BAND_MIN_WIDTH]; [EVENT_WORDS_BAND] where the
     * reader keeps the card to the event, without its [note]), never measured, between
     * [TALL_CIRCLE_MIN] and the ceiling ([tallDialCeiling]) or the card's inner width, rounded down
     * to a quarter.
     */
    fun tallCircle(width: Float, height: Float, fontScale: Float, note: Boolean = true): Float {
        val innerHeight = height - AgendaFit.CARD_PADDING * 2
        val innerWidth = width - AgendaFit.CARD_PADDING * 2
        val band = when {
            !note -> EVENT_WORDS_BAND
            width < WIDE_BAND_MIN_WIDTH -> TALL_WORDS_BAND
            else -> WIDE_WORDS_BAND
        }
        val most = minOf(tallDialCeiling(innerHeight, innerWidth), innerWidth)
        return quarterPoint((innerHeight - band * LINE_BOX_EM * fontScale).coerceIn(TALL_CIRCLE_MIN, most))
    }

    /** The title on a row. */
    const val TITLE_SP = 20f
    const val TALL_TITLE_SP = WidgetType.TITLE
    const val TALL_TITLE_MIN_SP = 18f
    const val NARROW_TITLE_SP = 16f
    const val TITLE_MIN_SP = 14f
    const val CELL_TITLE_SP = 12f

    /** The facts' size: the time in words, and the date. */
    const val WHEN_SP = WidgetType.BODY
    const val NARROW_WHEN_SP = 14f
    private val WHEN_SIZES = listOf(WHEN_SP, 14f)

    /**
     * A narrow row's time steps down to 12 sp before it is shortened: "Until 11:00" whole says
     * more than "→ 11:00", which says more than nothing (owner, 9 Oct 2026).
     */
    private val NARROW_WHEN_SIZES = listOf(NARROW_WHEN_SP, 13f, 12f)
    const val DATE_SP = WidgetType.BODY
    private val DATE_SIZES = listOf(DATE_SP, 14f, 13f)

    /** The note: the scale's [WidgetType.LABEL] (13 Oct 2026, from 14), a list's own size. */
    const val NOTE_SP = WidgetType.LABEL
    const val NOTE_GAP = 4f
    const val NOTE_MAX_LINES = 3
    const val TOP_GAP = 4f

    /** The dial on its row, and the air beside it. */
    const val ROW_DIAL_MAX = 56f
    const val ROW_DIAL_MIN = 44f
    const val DIAL_GAP = 12f

    /**
     * The narrowest the words beside the dial may be, and where they are set a size smaller; the
     * width the row's circle leaves beside it on every card of the family (a count and its goal).
     */
    const val ROW_WORDS_MIN = 72f
    const val ROW_WORDS_COMFORT = 150f
    const val ROW_CIRCLE_WORDS = 84f

    /** What the title keeps on a row before the date is given its column, and the column's floor. */
    const val TITLE_KEEP = 120f
    const val DATE_GAP = 12f
    const val DATE_COLUMN_MIN = 56f
    const val ROW_DATE_ONE_LINE = 100f

    /**
     * The tall dial's comfort, which the words after the note leave it (step 8.3: a two-row card's
     * band is spent there), and its ceiling: a share of the card, or [TALL_DIAL_CEILING] where that
     * share is smaller (review of 30 Oct 2026, step 8.3).
     */
    const val TALL_DIAL_COMFORT = 72f
    const val TALL_DIAL_CEILING = 104f
    const val TALL_DIAL_SHARE = 0.45f

    /**
     * The air under the tall dial: with the title's own leading over its capitals (0.35 em, 7.7 dp
     * at 22 sp), the dial stands as far over the title's capitals as the ring of the family's tall
     * card over its number (about 11.7 dp). It was 8 dp, and the owner's two-by-two lost its
     * title's second line for it (5 Nov 2026).
     */
    const val DIAL_BOTTOM_GAP = 4f
    const val DATE_BESIDE_MIN = 56f
    const val THEN_MIN_WIDTH = 280f

    /**
     * The tall circle ([tallCircle]): the words' band under it, in sp of 1.32 em lines with no
     * rounding pixel: a figure's line and three lines of text under three cells wide (108.24 dp at
     * the default text size), a figure's line and two from three cells, where a sentence that
     * takes two lines on two cells takes one (87.12 dp); three cells measure 250 dp, two 159. Its
     * floor is a row's own. The date's line over the title, where the dial leaves it no column.
     */
    const val TALL_WORDS_BAND = WidgetType.HERO + 3 * WidgetType.BODY
    const val WIDE_WORDS_BAND = WidgetType.HERO + 2 * WidgetType.BODY

    /**
     * The band of a card kept to the event: a figure's line and one line of text (66 dp at the
     * default text size), the family's ring's without its sentence; the title and its time.
     */
    const val EVENT_WORDS_BAND = WidgetType.HERO + WidgetType.BODY
    const val WIDE_BAND_MIN_WIDTH = 230f
    const val TALL_CIRCLE_MIN = ROW_DIAL_MIN
    const val DATE_OVER_GAP = TOP_GAP

    /**
     * The next events as rows (step 8.3): one or two, each a line at the label step, the time in
     * its column and the title beside it, as «Agenda» sets an inline row; a little air over them.
     */
    const val ROWS_MAX = 2
    const val ROW_SP = WidgetType.LABEL
    const val ROWS_GAP = NOTE_GAP
    const val ROW_TIME_GAP = AgendaFit.TIME_GAP
    const val ROW_TITLE_MIN = AgendaFit.TITLE_MIN

    /**
     * The second clock (step 8.5), under the date on a tall card: a quiet line at the label step,
     * as «Agenda» sets it, with a little air over it.
     */
    const val SECOND_SP = WidgetType.LABEL
    const val SECOND_TOP = 2f

    /** The cell's circle: its ceiling, its floor and the inset on each side of it. */
    const val CELL_DIAL_MAX = 104f
    const val CELL_CIRCLE_MIN = 36f
    const val CELL_CIRCLE_INSET = 8f
    const val CELL_GAP = 4f
    const val CELL_WHEN_SP = WidgetType.LABEL
    const val CELL_WHEN_MIN = 18f
    const val CELL_WHEN_MAX = 28f
}
