package com.callbackdev.tempo.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * What no other test reaches, because the launcher reads it: the
 * two providers, which must be the same size spec so the cards can trade places, and their
 * static previews, which may only use views `RemoteViews` can inflate.
 */
class WidgetProviderTest {
    private val xml = File("src/main/res/xml")
    private val layout = File("src/main/res/layout")

    private val remoteViewsTags = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "TextClock", "ImageView", "Button", "ImageButton", "ProgressBar",
    )

    private fun providers() = listOf("widget_agenda_info.xml", "widget_words_info.xml").map { File(xml, it).readText() }

    private fun attributes(text: String): Map<String, String> =
        Regex("""android:(\w+)="([^"]*)"""").findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `the two cards share one size spec`() {
        val sizing = listOf(
            "minWidth", "minHeight", "minResizeWidth", "minResizeHeight", "maxResizeWidth", "maxResizeHeight",
            "targetCellWidth", "targetCellHeight", "resizeMode", "updatePeriodMillis",
        )
        val (agenda, words) = providers().map(::attributes)
        sizing.forEach { key -> assertThat(agenda[key]).isEqualTo(words[key]) }
        assertThat(agenda["minResizeWidth"]).isEqualTo("40dp")
        assertThat(agenda["minResizeHeight"]).isEqualTo("40dp")
        assertThat(agenda["updatePeriodMillis"]).isEqualTo("0")
        // Placed at four cells by two, where «Agenda» has its clock beside a list (PLANNING.md §7).
        assertThat(agenda["targetCellWidth"]).isEqualTo("4")
        assertThat(agenda["targetCellHeight"]).isEqualTo("2")
        assertThat(agenda.keys).containsNoneOf("maxResizeWidth", "maxResizeHeight")
    }

    @Test
    fun `every provider names a preview that exists and inflates in RemoteViews`() {
        providers().forEach { provider ->
            val name = checkNotNull(attributes(provider)["previewLayout"]).removePrefix("@layout/")
            val preview = File(layout, "$name.xml")
            assertThat(preview.isFile).isTrue()
            val tags = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""").findAll(preview.readText())
                .map { it.groupValues[1] }
                .filterNot { it == "xml" }
                .toSet()
            assertThat(remoteViewsTags).containsAtLeastElementsIn(tags)
        }
    }

    @Test
    fun `the clock's own layouts inflate in RemoteViews too`() {
        layout.listFiles { file -> file.name.startsWith("widget_clock_") }.orEmpty().forEach { file ->
            val tags = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""").findAll(file.readText())
                .map { it.groupValues[1] }
                .filterNot { it == "xml" }
                .toSet()
            assertThat(remoteViewsTags).containsAtLeastElementsIn(tags)
        }
    }

    @Test
    fun `the static preview's card is the default card`() {
        val colors = File("src/main/res/values/colors.xml").readText()
        assertThat(colors).contains("<color name=\"widget_preview_card\">#FF0F3B6B</color>")
    }
}
