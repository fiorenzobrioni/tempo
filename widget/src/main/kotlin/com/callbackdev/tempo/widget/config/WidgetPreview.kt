package com.callbackdev.tempo.widget.config

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.tempo.core.domain.widget.AgendaFit
import com.callbackdev.tempo.core.domain.widget.AgendaForm
import com.callbackdev.tempo.core.domain.widget.AgendaSpec
import com.callbackdev.tempo.core.domain.widget.WordsFit
import com.callbackdev.tempo.core.domain.widget.WordsForm
import com.callbackdev.tempo.widget.R
import com.callbackdev.tempo.widget.WidgetKind
import com.callbackdev.tempo.widget.WidgetLook
import com.callbackdev.tempo.widget.WidgetModel
import com.callbackdev.tempo.widget.agenda.AgendaWidgetContent
import com.callbackdev.tempo.widget.words.WordsWidgetContent

/**
 * A launcher grant, named by its cells: the household's reference sizes (Chiaro's), the ones
 * the layout tests measure against, so what a chip shows is what the tests hold. Both cards
 * resize over the same range, so they offer the same chips.
 */
internal enum class PreviewSize(val label: String, val size: DpSize) {
    ONE_BY_ONE("1×1", DpSize(85.dp, 85.dp)),
    TWO_BY_ONE("2×1", DpSize(159.dp, 85.dp)),
    THREE_BY_ONE("3×1", DpSize(250.dp, 85.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 85.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    THREE_BY_TWO("3×2", DpSize(250.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 293.dp)),
}

/** Where a card lands when first placed: its provider's `targetCell*`. */
internal val DefaultPreviewSize = PreviewSize.FOUR_BY_TWO

/**
 * The size this widget really has on the home screen in portrait (the launcher's
 * `minWidth × maxHeight`), or null before its first layout.
 */
internal fun placedWidgetSize(context: Context, appWidgetId: Int): DpSize? {
    val options = runCatching { AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId) }.getOrNull()
        ?: return null
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

/**
 * One sentence saying what a card of [kind] carries at [size], read off the same arithmetic the
 * card lays itself out with: «Agenda»'s form needs the card's own measurements, so it is asked
 * for here with the sample's ([AgendaFit] decides on widths, not on what the day holds).
 */
internal fun formNote(kind: WidgetKind, size: DpSize, look: WidgetLook): Int = when (kind) {
    WidgetKind.AGENDA -> when {
        !look.showClock && !look.showDate -> R.string.agenda_form_list

        else -> when (agendaForm(size)) {
            AgendaForm.CLOCK -> R.string.agenda_form_clock
            AgendaForm.LINE -> R.string.agenda_form_line
            AgendaForm.AGENDA -> R.string.agenda_form_agenda
            AgendaForm.SIDE -> R.string.agenda_form_side
        }
    }

    WidgetKind.WORDS -> when (WordsFit.form(size.width.value, size.height.value)) {
        WordsForm.NEXT -> R.string.words_form_next
        WordsForm.LINE -> R.string.words_form_line
        WordsForm.STACK -> R.string.words_form_stack
        WordsForm.PANEL -> R.string.words_form_panel
    }
}

/** «Agenda»'s form at [size] for a card with its clock and date, measured as Roboto measures them. */
private fun agendaForm(size: DpSize): AgendaForm = AgendaFit.plan(
    AgendaSpec(
        width = size.width.value,
        height = size.height.value,
        fontScale = 1f,
        clock = true,
        date = true,
        clockEm = 2.65f,
        dateEms = listOf(9.6f, 4.8f, 4.3f),
        rangeEm = 6.5f,
        startEm = 4.9f,
    ),
).form

/**
 * The card as it will look, on a ground that stands in for a wallpaper, with the sizes it can
 * be given underneath and a line saying what that size carries. It opens on the size the card
 * really has when the launcher has said it: the reader's own card first.
 */
@Composable
internal fun WidgetPreviewSection(kind: WidgetKind, model: WidgetModel?, placed: DpSize?) {
    var size by remember { mutableStateOf(placed ?: DefaultPreviewSize.size) }
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        WidgetPreview(kind, model, size)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        placed?.let { here ->
            FilterChip(
                selected = size == here,
                onClick = { size = here },
                label = { Text(stringResource(R.string.widget_config_size_placed)) },
            )
        }
        PreviewSize.entries.forEach { option ->
            FilterChip(
                selected = size == option.size && size != placed,
                onClick = { size = option.size },
                label = { Text(option.label) },
            )
        }
    }
    Text(
        text = stringResource(formNote(kind, size, model?.look ?: WidgetLook())),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    )
    Text(
        text = stringResource(R.string.widget_config_resize_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
    )
}

/**
 * The card itself, not a lookalike (Chiaro's lesson of 23 Sep 2026): Glance composes the same
 * content function the receiver runs into `RemoteViews` for the chosen size, and
 * `RemoteViews.apply` inflates them as the launcher does. The host swallows every touch, so the
 * card's own "open the app" never fires from its settings screen, and it is one picture to a
 * screen reader.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetPreview(kind: WidgetKind, model: WidgetModel?, size: DpSize) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(kind, model, size) {
        if (model == null) return@LaunchedEffect
        views = runCatching {
            GlanceRemoteViews().compose(context, size) {
                when (kind) {
                    WidgetKind.AGENDA -> AgendaWidgetContent(model)
                    WidgetKind.WORDS -> WordsWidgetContent(model)
                }
            }.remoteViews
        }.getOrNull()
    }
    val description = stringResource(R.string.widget_config_preview_desc)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        val scale = minOf(1f, maxWidth / size.width)
        Box(modifier = Modifier.fillMaxWidth().height(size.height * scale), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .requiredSize(size.width, size.height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                views?.let { remote ->
                    AndroidView(
                        factory = { TouchlessFrame(it) },
                        update = { frame ->
                            frame.removeAllViews()
                            runCatching { remote.apply(frame.context, frame) }.getOrNull()?.let { frame.addView(it) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewConstructor")
private class TouchlessFrame(context: Context) : FrameLayout(context) {
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = true
}
