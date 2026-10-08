package com.callbackdev.tempo.core.domain.widget

/*
 * «In words» (Chiaro's «In parole», Passo's «In words»; PLANNING.md §7): a dial and the day in a
 * few words (owner, 8 Oct 2026, after the first cards on the phone). The dial stands where Passo's
 * ring stands on its card, the family's picture a card is read by across a room: the system's
 * own hands over the next twelve hours, the focus drawn on its ring ([DialArcs]). The words say
 * what the dial cannot: what it is, and when, in clock times.
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the dial: the hands, and the focus's arc on the ring | up to [ROW_DIAL_MAX], [TALL_DIAL_MAX] | – | strong, accent |
 * | 2 | the focus's title ("Design review"; "Free" when nothing is left) | [TITLE_SP], [TALL_TITLE_SP] | Medium | strong |
 * | 3 | when ("Until 11:00", "15:00 – 16:00", "Tomorrow · 9:00 – 10:00") | [WHEN_SP] | Regular | quiet |
 * | 4 | the date (the system's `TextClock`, so it turns at midnight) | [DATE_SP] | Regular | quiet |
 * | 5 | the day's note, the line of times after it | [NOTE_SP] | Regular | quiet |
 *
 * No number is large: a time printed at a count's size made a sentence shout ("Until 18:30" at
 * 34 sp left the title three words and an ellipsis), and the dial already says the hour. The
 * sizes are the family's: the title at Chiaro's sentence's 20 sp, the time and the date at
 * Passo's facts' 16 sp.
 *
 * Three forms:
 *
 * - [WordsForm.CELL], one cell wide: the dial as large as the cell, the focus's time under it where
 *   the cell is taller than wide.
 * - [WordsForm.ROW], one row: Passo's row. The dial; the title over its time; the date at the far
 *   edge, over two lines like Passo's sentence, where the row has the room.
 * - [WordsForm.TALL], two rows and up: Passo's tall card. The dial in the top trailing corner with
 *   the date beside it; the title, its time and the note hanging from the bottom leading corner.
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
)

/**
 * The card's plan: the dial's side (0: none); the date's style ([dateChoice] into
 * [WordsSpec.dateEms], -1: none), size, lines and column; the title's size and lines (0: none);
 * which time in words ([whenChoice] into [WordsSpec.whenEms], -1: none) at which size, bold where
 * it stands in the dial's place; the note's lines; whether the line of times is drawn.
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
     * One cell: the dial as large as the cell holds, and the focus's time under it where the cell
     * is tall enough to keep the dial [CELL_DIAL_COMFORT]. Without the dial, the time in its place,
     * bold and as large as fits, the title under it where a line is left.
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
            val alone = minOf(width, inner, CELL_DIAL_MAX)
            val time = whenFor(spec, width, listOf(CELL_WHEN_SP))
            val under = time?.let { lineHeight(it.sp, scale) + CELL_GAP } ?: 0f
            val withTime = minOf(width, inner - under, CELL_DIAL_MAX)
            if (time == null || withTime < CELL_DIAL_COMFORT) return base.copy(dial = alone)
            val title = inner - withTime - under >= lineHeight(CELL_TITLE_SP, scale)
            return base.copy(
                dial = withTime,
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
     * One row, Passo's: the dial, as tall as the row leaves up to [ROW_DIAL_MAX] and never so wide
     * that the words beside it lose [ROW_WORDS_MIN]; the title over its time; the date at the far
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
            (width - DIAL_GAP - ROW_WORDS_MIN).coerceIn(ROW_DIAL_MIN, minOf(inner, ROW_DIAL_MAX))
        } else {
            0f
        }
        val words = width - (if (dial > 0f) dial + DIAL_GAP else 0f)
        val narrow = words < ROW_WORDS_COMFORT
        val time = whenFor(spec, words, if (narrow) listOf(NARROW_WHEN_SP) else WHEN_SIZES)
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
     * Two rows and up, Passo's tall card, bottom up in the order the hierarchy spends the height:
     * the title's first line and its time; the dial at [TALL_DIAL_MIN] (without it, the date on
     * top); the note, whole up to [NOTE_MAX_LINES] or not at all; the dial up to
     * [TALL_DIAL_COMFORT]; the title's second line, for a title too long for one even at
     * [TALL_TITLE_MIN_SP]; the line of times on a wide card; what is left
     * grows the dial to [TALL_DIAL_MAX]. The date takes the band beside the dial, on one line or two.
     */
    private fun tallPlan(spec: WordsSpec): WordsPlan {
        val scale = spec.fontScale
        val inner = spec.height - AgendaFit.CARD_PADDING * 2
        val width = spec.width - AgendaFit.CARD_PADDING * 2
        // A title a little too long for one line steps down to [TALL_TITLE_MIN_SP] to stay whole;
        // a longer one keeps its size and takes a second line where the card has one.
        val fits = spThatFits(width, spec.titleEm, scale)
        val oneLine = fits >= TALL_TITLE_MIN_SP
        val titleSp = if (oneLine) quarterPoint(minOf(TALL_TITLE_SP, fits)) else TALL_TITLE_SP
        val titleLine = lineHeight(titleSp, scale)
        var left = inner - titleLine
        val time = whenFor(spec, width, WHEN_SIZES)
        if (time != null) left -= lineHeight(time.sp, scale)
        val withDate = spec.dateEms.isNotEmpty()
        val dialMost = minOf(TALL_DIAL_MAX, if (withDate) width - DIAL_GAP - DATE_BESIDE_MIN else width)
        var dial = 0f
        var top: DatePick? = null
        if (spec.dial && dialMost >= TALL_DIAL_MIN && left - DIAL_BOTTOM_GAP >= TALL_DIAL_MIN) {
            dial = TALL_DIAL_MIN
            left -= TALL_DIAL_MIN + DIAL_BOTTOM_GAP
        } else if (withDate) {
            top = date(spec, width, Float.MAX_VALUE, twoLines = false)?.takeIf { left >= it.height + TOP_GAP }
            top?.let { left -= it.height + TOP_GAP }
        }
        // The note whole or not at all: a sentence cut off mid-phrase says less than none.
        val noteHeight = lineHeight(NOTE_SP, scale) * spec.noteLines + NOTE_GAP
        val noteLines = if (spec.noteLines in 1..NOTE_MAX_LINES && left >= noteHeight) spec.noteLines else 0
        if (noteLines > 0) left -= noteHeight
        fun grow(to: Float) {
            if (dial <= 0f) return
            val more = minOf(to - dial, left).coerceAtLeast(0f)
            dial += more
            left -= more
        }
        grow(minOf(TALL_DIAL_COMFORT, dialMost))
        var titleLines = 1
        if (!oneLine && spec.titleLines >= 2 && left >= titleLine) {
            left -= titleLine
            titleLines = 2
        }
        val noteLine = lineHeight(NOTE_SP, scale)
        val then = spec.then && width >= THEN_MIN_WIDTH && left >= noteLine
        if (then) left -= noteLine
        grow(dialMost)
        val date = if (dial > 0f && withDate) date(spec, width - dial - DIAL_GAP, dial, twoLines = true) else top
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
        )
    }

    /** Chiaro's sentence beside its temperature: the title on a row. */
    const val TITLE_SP = 20f
    const val TALL_TITLE_SP = 22f
    const val TALL_TITLE_MIN_SP = 18f
    const val NARROW_TITLE_SP = 16f
    const val TITLE_MIN_SP = 14f
    const val CELL_TITLE_SP = 12f

    /** Passo's facts ("of 8,000 steps"): the time in words, and the date. */
    const val WHEN_SP = 16f
    const val NARROW_WHEN_SP = 14f
    private val WHEN_SIZES = listOf(WHEN_SP, 14f)
    const val DATE_SP = 16f
    private val DATE_SIZES = listOf(DATE_SP, 14f, 13f)
    const val NOTE_SP = 14f
    const val NOTE_GAP = 4f
    const val NOTE_MAX_LINES = 3
    const val TOP_GAP = 4f

    /** Passo's ring on its row, and the air beside it (`RowRingMax`, `RingTextGap`). */
    const val ROW_DIAL_MAX = 56f
    const val ROW_DIAL_MIN = 44f
    const val DIAL_GAP = 12f

    /** The narrowest the words beside the dial may be, and where they are set a size smaller. */
    const val ROW_WORDS_MIN = 72f
    const val ROW_WORDS_COMFORT = 150f

    /** What the title keeps on a row before the date is given its column, and the column's floor. */
    const val TITLE_KEEP = 120f
    const val DATE_GAP = 12f
    const val DATE_COLUMN_MIN = 56f
    const val ROW_DATE_ONE_LINE = 100f

    /** Passo's tall ring (44 to 104 dp), with a comfortable middle the note does not take. */
    const val TALL_DIAL_MIN = 48f
    const val TALL_DIAL_COMFORT = 72f
    const val TALL_DIAL_MAX = 104f
    const val DIAL_BOTTOM_GAP = 8f
    const val DATE_BESIDE_MIN = 56f
    const val THEN_MIN_WIDTH = 280f

    const val CELL_DIAL_MAX = 104f
    const val CELL_DIAL_COMFORT = 56f
    const val CELL_GAP = 4f
    const val CELL_WHEN_SP = 13f
    const val CELL_WHEN_MIN = 18f
    const val CELL_WHEN_MAX = 28f
}
