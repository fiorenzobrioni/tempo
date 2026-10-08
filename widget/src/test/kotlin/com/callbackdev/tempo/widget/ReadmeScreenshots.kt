package com.callbackdev.tempo.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.tempo.widget.agenda.AgendaWidgetContent
import com.callbackdev.tempo.widget.config.WidgetConfigScreen
import com.callbackdev.tempo.widget.words.WordsWidgetContent
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of the widgets (docs/screenshots), drawn from the README's own sample week
 * (Today's), at 10:20, in English. Run with `./gradlew :widget:testDebugUnitTest -PupdateScreenshots`;
 * skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val output = System.getProperty("tempo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    /** The pair on a home screen: «Agenda» at its default 4×2, «In words» beside a small clock, a one-row «In words». */
    @Test
    fun widgets() {
        val panel = DpSize(361.dp, 189.dp)
        val half = DpSize(172.dp, 189.dp)
        val row = DpSize(361.dp, 85.dp)
        val clay = WidgetLook(cardColor = WidgetCardColor.CLAY)
        val afternoon = WidgetSamples.model(look = clay, now = WidgetSamples.at("2026-10-07T14:20"))
        val board = HomeBoard(context, widthDp = 393)
            .row(panel to renderCard(context, panel) { AgendaWidgetContent(WidgetSamples.model()) })
            .row(
                half to renderCard(context, half) { WordsWidgetContent(afternoon) },
                half to renderCard(context, half) { AgendaWidgetContent(afternoon) },
            )
            .row(row to renderCard(context, row) { WordsWidgetContent(WidgetSamples.model()) })
        board.draw().saveTo(File(checkNotNull(output)), "widgets")
    }

    /** One card's settings: the live card, its sizes, the time's touch to the clock app. */
    @Test
    fun widgetSettings() {
        compose.setContent {
            TempoTheme {
                WidgetConfigScreen(
                    kind = WidgetKind.AGENDA,
                    look = WidgetLook(headerTap = HeaderTap.CLOCK),
                    model = WidgetSamples.model(),
                    placed = null,
                    onLook = {},
                    onOpacityDrag = {},
                    onOpacityDone = {},
                    onDone = {},
                )
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "widget-settings.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
