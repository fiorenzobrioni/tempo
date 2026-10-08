package com.callbackdev.tempo.core.domain.widget

/*
 * «In words» (Chiaro's «In parole», Passo's «In words»; PLANNING.md §7): the day in type alone, its
 * hierarchy built out of size, weight and ink:
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the focus's time ("15:00", "Until 11:00", "Free") | scaled to the grant | Bold | strong |
 * | 2 | its title | [TITLE_SP] | Medium | strong |
 * | 3 | the time and the date on top (the system's `TextClock`); a later day's name over the focus | [TOP_SP], [LABEL_SP] | Regular / Medium | quiet |
 * | 4 | the day's note, the line of times after it | [NOTE_SP] | Regular | quiet |
 *
 * The sizes are Passo's «At a glance» row, so the family's cards read as one set on a home screen
 * (owner, 8 Oct 2026): the focus's time up to its count's [LINE_HERO_MAX], the line on top at its
 * facts' [TOP_SP].
 *
 * Four forms:
 *
 * - [WordsForm.NEXT], one cell: the focus's time alone, its label over it, its title under it
 *   where the cell has room.
 * - [WordsForm.LINE], one row: the time and the date as a small line, then the time and the title
 *   on one line; on a narrow row, the title under the time.
 * - [WordsForm.STACK], two rows and up: the time and the date on top; the focus large at the
 *   bottom, then the note.
 * - [WordsForm.PANEL], two rows and up, four cells wide: the same, and the rest of the day as a
 *   line of times.
 */
enum class WordsForm { NEXT, LINE, STACK, PANEL }

/**
 * What the card is asked to hold, measured.
 *
 * @property topEms the small line on top in each style the card may use, in ems of the regular
 *   face, the reader's first ("10:20 · Wednesday 8 October", then shorter); empty when the reader
 *   hid both the clock and the date.
 * @property topDated how many of [topEms], from the first, carry the date: the rest are the time
 *   alone, which a narrow card takes only when no dated line fits even a size smaller.
 * @property heroEm the focus's time, in ems of the bold face.
 * @property label whether the focus carries a label over it (a later day's name).
 * @property title whether the focus has a title ("Free" has none).
 * @property titleLines how many lines the title takes at the card's width, at [TITLE_SP].
 * @property noteLines how many lines the note takes at the card's width, at [NOTE_SP].
 * @property then whether there are later starts today for the line of times.
 */
data class WordsSpec(
    val width: Float,
    val height: Float,
    val fontScale: Float,
    val topEms: List<Float>,
    val topDated: Int = topEms.size,
    val heroEm: Float,
    val label: Boolean,
    val title: Boolean,
    val titleLines: Int,
    val noteLines: Int,
    val then: Boolean,
)

/**
 * The card's plan: [topChoice] into [WordsSpec.topEms] (-1: no top line) at [topSp], the focus's size, the
 * title's size and lines (0: not drawn; [titleInline] beside the time), the note's lines, and
 * whether the line of times is drawn.
 */
data class WordsPlan(
    val form: WordsForm,
    val topChoice: Int,
    val label: Boolean,
    val heroSp: Float,
    val titleSp: Float,
    val titleLines: Int,
    val titleInline: Boolean,
    val noteLines: Int,
    val then: Boolean,
    val snug: Boolean,
    val sidePadding: Float = AgendaFit.CARD_PADDING,
    val topSp: Float = WordsFit.TOP_SP,
)

/** The line on top as drawn: which style, at which size. */
private data class TopLine(val choice: Int, val sp: Float)

object WordsFit {
    fun form(width: Float, height: Float): WordsForm = when {
        width < AgendaFit.CLOCK_FORM_MAX_WIDTH -> WordsForm.NEXT
        height < AgendaFit.TALL_MIN_HEIGHT -> WordsForm.LINE
        width >= PANEL_MIN_WIDTH -> WordsForm.PANEL
        else -> WordsForm.STACK
    }

    fun plan(spec: WordsSpec): WordsPlan = when (val form = form(spec.width, spec.height)) {
        WordsForm.NEXT -> nextPlan(spec)
        WordsForm.LINE -> linePlan(spec)
        WordsForm.STACK, WordsForm.PANEL -> tallPlan(spec, form)
    }

    private fun innerWidth(spec: WordsSpec) = spec.width - sidePadding(spec) * 2

    /** One cell is all time: the card's corner needs less air beside it than words do. */
    private fun sidePadding(spec: WordsSpec) =
        if (spec.width < AgendaFit.CLOCK_FORM_MAX_WIDTH) AgendaFit.CELL_PADDING else AgendaFit.CARD_PADDING

    /**
     * The line on top: the first dated style whose width fits at [TOP_SP], else a size smaller
     * (down to [TOP_MIN_SP]) rather than lose the date, else the time alone; none where nothing fits.
     */
    private fun topLine(spec: WordsSpec): TopLine? {
        val width = innerWidth(spec)
        fun fits(choice: Int, sp: Float) = widthOf(spec.topEms[choice], sp, spec.fontScale) <= width
        val dated = 0 until spec.topDated.coerceAtMost(spec.topEms.size)
        TOP_SIZES.forEach { sp -> dated.firstOrNull { fits(it, sp) }?.let { return TopLine(it, sp) } }
        return (dated.last + 1 until spec.topEms.size).firstOrNull { fits(it, TOP_SP) }?.let { TopLine(it, TOP_SP) }
    }

    /** What the line on top takes of the height: a `TextClock` without font padding. */
    private fun topHeight(top: TopLine?, scale: Float): Float = top?.let { clockLineHeight(it.sp, scale) } ?: 0f

    /** One cell: the label, the time as large as fits, the title under it if a line is left. */
    private fun nextPlan(spec: WordsSpec): WordsPlan {
        val snug = spec.height < AgendaFit.TALL_MIN_HEIGHT
        val inner = spec.height - (if (snug) AgendaFit.CARD_PADDING_SNUG else AgendaFit.CARD_PADDING) * 2
        val label = if (spec.label) lineHeight(LABEL_SP, spec.fontScale) else 0f
        val byWidth = spThatFits(innerWidth(spec), spec.heroEm, spec.fontScale)
        fun hero(room: Float) = quarterPoint(minOf(sizeForLine(room, spec.fontScale), byWidth, NEXT_HERO_MAX))
        val title = lineHeight(CELL_TITLE_SP, spec.fontScale)
        val withTitle = hero(inner - label - title)
        val showTitle = spec.title && withTitle >= NEXT_HERO_COMFORT
        return WordsPlan(
            form = WordsForm.NEXT,
            topChoice = -1,
            label = spec.label,
            heroSp = if (showTitle) withTitle else hero(inner - label),
            titleSp = CELL_TITLE_SP,
            titleLines = if (showTitle) 1 else 0,
            titleInline = false,
            noteLines = 0,
            then = false,
            snug = snug,
            sidePadding = AgendaFit.CELL_PADDING,
        )
    }

    /**
     * One row: the time and the title on one line where the title keeps [INLINE_TITLE_MIN] beside
     * it, under the small line on top where the row still holds the time at [LINE_HERO_FLOOR];
     * else the title under the time.
     */
    private fun linePlan(spec: WordsSpec): WordsPlan {
        val inner = spec.height - AgendaFit.CARD_PADDING_SNUG * 2
        val width = innerWidth(spec)
        val top = topLine(spec)
        val topHeight = topHeight(top, spec.fontScale)
        val label = if (spec.label) lineHeight(LABEL_SP, spec.fontScale) else 0f
        fun hero(room: Float, most: Float) = quarterPoint(
            minOf(sizeForLine(room, spec.fontScale), LINE_HERO_MAX, spThatFits(most, spec.heroEm, spec.fontScale)),
        )
        // Inline: the time beside the title, the label (a later day) said before the time.
        val inlineHero = hero(inner - topHeight - label, width - INLINE_TITLE_MIN - INLINE_GAP)
        val titleRoom = width - widthOf(spec.heroEm, inlineHero, spec.fontScale) - INLINE_GAP
        if (!spec.title || (inlineHero >= LINE_HERO_FLOOR && titleRoom >= INLINE_TITLE_MIN)) {
            return WordsPlan(
                form = WordsForm.LINE,
                topChoice = top?.choice ?: -1,
                topSp = top?.sp ?: TOP_SP,
                label = spec.label,
                heroSp = inlineHero,
                titleSp = LINE_TITLE_SP,
                titleLines = if (spec.title) 1 else 0,
                titleInline = true,
                noteLines = 0,
                then = false,
                snug = true,
            )
        }
        // Stacked: the title under the time; the small line only where the time keeps its floor.
        val titleHeight = lineHeight(CELL_TITLE_SP + 2f, spec.fontScale)
        val withTop = hero(inner - topHeight - label - titleHeight, width)
        val keepTop = top != null && withTop >= LINE_HERO_FLOOR
        val heroSp = if (keepTop) withTop else hero(inner - label - titleHeight, width)
        return WordsPlan(
            form = WordsForm.LINE,
            topChoice = if (keepTop) top?.choice ?: -1 else -1,
            topSp = top?.sp ?: TOP_SP,
            label = spec.label,
            heroSp = heroSp,
            titleSp = CELL_TITLE_SP + 2f,
            titleLines = 1,
            titleInline = false,
            noteLines = 0,
            then = false,
            snug = true,
        )
    }

    /**
     * Two rows and up, bottom up, in the order the hierarchy spends: the time at [TALL_HERO_FLOOR]
     * and the title's first line; the small line on top; the note; the title's second line; the
     * line of times (on the panel); what is left grows the time up to [TALL_HERO_MAX]. The note is
     * drawn whole, up to [NOTE_MAX_LINES], or not at all.
     */
    private fun tallPlan(spec: WordsSpec, form: WordsForm): WordsPlan {
        val inner = spec.height - AgendaFit.CARD_PADDING * 2
        val scale = spec.fontScale
        val byWidth = spThatFits(innerWidth(spec), spec.heroEm, scale)
        val floor = minOf(TALL_HERO_FLOOR, byWidth)
        var left = inner - lineHeight(floor, scale) - (if (spec.label) lineHeight(LABEL_SP, scale) else 0f)
        val titleLine = lineHeight(TITLE_SP, scale)
        var titleLines = 0
        if (spec.title && left >= titleLine) {
            left -= titleLine
            titleLines = 1
        }
        var top = topLine(spec)
        val topHeight = topHeight(top, scale) + TOP_GAP
        if (top != null && left >= topHeight) left -= topHeight else top = null
        // The note whole or not at all: a sentence cut off mid-phrase says less than none.
        val noteLine = lineHeight(NOTE_SP, scale)
        val noteHeight = noteLine * spec.noteLines + NOTE_GAP
        val noteLines = if (spec.noteLines in 1..NOTE_MAX_LINES && left >= noteHeight) spec.noteLines else 0
        if (noteLines > 0) left -= noteHeight
        if (titleLines == 1 && spec.titleLines >= 2 && left >= titleLine) {
            left -= titleLine
            titleLines = 2
        }
        val then = form == WordsForm.PANEL && spec.then && left >= noteLine
        if (then) left -= noteLine
        val hero =
            quarterPoint(
                minOf(sizeForLine(lineHeight(floor, scale) + left.coerceAtLeast(0f), scale), TALL_HERO_MAX, byWidth),
            )
        return WordsPlan(
            form = form,
            topChoice = top?.choice ?: -1,
            topSp = top?.sp ?: TOP_SP,
            label = spec.label,
            heroSp = hero,
            titleSp = TITLE_SP,
            titleLines = titleLines,
            titleInline = false,
            noteLines = noteLines,
            then = then,
            snug = false,
        )
    }

    const val PANEL_MIN_WIDTH = 300f

    /** Passo's facts ("of 8,000 steps"); a narrow card's dated line steps down to [TOP_MIN_SP]. */
    const val TOP_SP = 16f
    const val TOP_MIN_SP = 13f
    private val TOP_SIZES = listOf(TOP_SP, 14f, TOP_MIN_SP)
    const val TOP_GAP = 4f
    const val LABEL_SP = 14f
    const val TITLE_SP = 18f
    const val LINE_TITLE_SP = 18f
    const val CELL_TITLE_SP = 12f
    const val NOTE_SP = 14f
    const val NOTE_GAP = 4f
    const val NOTE_MAX_LINES = 3

    const val NEXT_HERO_MAX = 32f
    const val NEXT_HERO_COMFORT = 20f

    /** Passo's count on its row ("86"), so the two cards' numbers match side by side. */
    const val LINE_HERO_MAX = 34f
    const val LINE_HERO_FLOOR = 20f
    const val INLINE_TITLE_MIN = 72f
    const val INLINE_GAP = 8f
    const val TALL_HERO_FLOOR = 28f
    const val TALL_HERO_MAX = 56f
}
