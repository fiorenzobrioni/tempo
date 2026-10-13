package com.callbackdev.tempo.core.domain.widget

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** The widgets' type scale: every fixed size a card prints in is a step of [WidgetType]. */
class WidgetTypeTest {
    private val everySize = mapOf(
        "agenda: title" to AgendaFit.TITLE_SP,
        "agenda: time" to AgendaFit.TIME_SP,
        "agenda: heading" to AgendaFit.HEADING_SP,
        "agenda: note" to AgendaFit.NOTE_SP,
        "agenda: footer" to AgendaFit.FOOTER_SP,
        "agenda: date by a clock" to AgendaFit.LINE_DATE_SP,
        "agenda: date over a list" to AgendaFit.DATE_SP,
        "in words: tall title" to WordsFit.TALL_TITLE_SP,
        "in words: when" to WordsFit.WHEN_SP,
        "in words: date" to WordsFit.DATE_SP,
        "in words: note" to WordsFit.NOTE_SP,
        "in words: a cell's when" to WordsFit.CELL_WHEN_SP,
    )

    @Test
    fun `every fixed widget size is a step of the scale`() {
        everySize.forEach { (role, sp) ->
            assertWithMessage(role).that(WidgetType.steps).contains(sp)
        }
    }

    @Test
    fun `a list's row is one size, title and time alike`() {
        assertThat(AgendaFit.TITLE_SP).isEqualTo(AgendaFit.TIME_SP)
        assertThat(WordsFit.NOTE_SP).isEqualTo(AgendaFit.NOTE_SP)
    }
}
