package com.callbackdev.tempo.core.domain.widget

/**
 * **The widgets' type scale** (13 Oct 2026, the family's widget review): the sizes a
 * home-screen card prints in, by role, so a row, a note or a footer is the same size on every
 * card of the family. Six steps, each a role:
 *
 * - [CAPTION]: read last.
 * - [LABEL]: a list's row (the event's title over its time, both at this size, sorted by weight
 *   and ink), a section's heading, the list's footer, a note under the words.
 * - [BODY]: the second thing read: the date beside a clock, the time in words, a card's message.
 * - [TITLE]: the words of a card whose hero is a drawing («In words»'s title on a tall card).
 * - [DISPLAY]: a hero wider than a figure.
 * - [HERO]: the clock on a one-row card.
 *
 * The clocks and «In words»'s title are sized to the grant and step down to fit (the
 * [AgendaFit] and [WordsFit] ladders), so their sizes are where a ladder starts or lands, not
 * steps; a ladder's first choice is a step wherever one exists. `WidgetTypeTest` holds every
 * fixed size to the scale.
 */
object WidgetType {
    const val CAPTION = 11f
    const val LABEL = 13f
    const val BODY = 16f
    const val TITLE = 22f
    const val DISPLAY = 30f
    const val HERO = 34f

    /** Every step, for the test that holds the cards to them. */
    val steps: Set<Float> = setOf(CAPTION, LABEL, BODY, TITLE, DISPLAY, HERO)
}
