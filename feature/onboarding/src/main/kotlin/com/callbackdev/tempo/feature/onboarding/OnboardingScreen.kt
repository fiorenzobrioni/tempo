package com.callbackdev.tempo.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarAccess
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.designsystem.R as DesignR

/**
 * The first run's pages (PLANNING.md §11 Phases 3 and 4): what Tempo is, the one
 * permission it needs, and the widgets, with the launcher's own way to place one.
 */
enum class OnboardingStep {
    WELCOME,
    CALENDAR,
    WIDGET,
}

/**
 * The widget page's facts: whether the launcher places a card on request (Android's pin request),
 * and whether one of Tempo's is on a home screen already.
 */
data class WidgetOffer(val canPin: Boolean = false, val placed: Boolean = false)

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val asked = settings?.askedCalendarPermission ?: false
    var permission by remember { mutableStateOf(CalendarPermission.ASKABLE) }
    val refresh = { permission = activity?.let { CalendarAccess.permission(it, asked) } ?: CalendarPermission.ASKABLE }
    // Back from the system's settings, the answer may have changed there.
    LifecycleResumeEffect(asked) {
        refresh()
        onPauseOrDispose { }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.permissionAnswered()
        refresh()
    }
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    // A card placed from the launcher's dialog is seen when the page is back in front.
    var offer by remember { mutableStateOf(viewModel.widgetOffer()) }
    LifecycleResumeEffect(Unit) {
        offer = viewModel.widgetOffer()
        onPauseOrDispose { }
    }
    OnboardingScreen(
        step = step,
        permission = permission,
        widgets = offer,
        actions = OnboardingActions(
            next = { OnboardingStep.entries.getOrNull(step.ordinal + 1)?.let { step = it } },
            back = { OnboardingStep.entries.getOrNull(step.ordinal - 1)?.let { step = it } },
            ask = { ask.launch(CalendarAccess.PERMISSION) },
            openAppSettings = {
                runCatching {
                    val page = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, page))
                }
            },
            finish = viewModel::finish,
            pin = { widget -> viewModel.pin(widget) },
        ),
    )
}

class OnboardingActions(
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val ask: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val finish: () -> Unit = {},
    val pin: (TempoWidget) -> Unit = {},
)

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    permission: CalendarPermission,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    widgets: WidgetOffer = WidgetOffer(),
) {
    val reduced = reducedMotion()
    val gutter = pageGutter()
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = actions.back)
    Surface(modifier = modifier.fillMaxSize().testTag(OnboardingTags.ROOT)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // On every page, the welcome included (owner, 10 Oct 2026): the run's length is said
            // from its first page.
            Box(Modifier.padding(gutter)) { Progress(step.ordinal, OnboardingStep.entries.size) }
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (reduced) {
                        fadeIn(TempoMotion.fade()) togetherWith fadeOut(TempoMotion.fade())
                    } else {
                        val spec = tween<IntOffset>(PAGE_MILLIS, easing = FastOutSlowInEasing)
                        val shift = if (forward) 6 else -6
                        val enter = fadeIn(tween(PAGE_MILLIS)) + slideInHorizontally(spec) { it / shift }
                        val exit = fadeOut(tween(PAGE_MILLIS)) + slideOutHorizontally(spec) { -it / shift }
                        enter togetherWith exit
                    }
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(gutter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag(OnboardingTags.PAGE),
                ) {
                    when (page) {
                        OnboardingStep.WELCOME -> WelcomePage()
                        OnboardingStep.CALENDAR -> CalendarPage(permission)
                        OnboardingStep.WIDGET -> WidgetPage(widgets, actions.pin)
                    }
                }
            }
            Box(Modifier.padding(gutter)) { BottomBar(step, permission, actions) }
        }
    }
}

private const val PAGE_MILLIS = 300

@Composable
private fun Progress(index: Int, count: Int) {
    val reduced = reducedMotion()
    val fraction by animateFloatAsState(
        targetValue = (index + 1f) / count,
        animationSpec = if (reduced) TempoMotion.fade() else tween(400, easing = FastOutSlowInEasing),
        label = "progress",
    )
    val description = stringResource(R.string.onboarding_progress, index + 1, count)
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * The bar under every page: one wide button that moves on, and the quieter way back or past. On
 * the calendar's page the wide button asks (or, refused for good, opens the system's page) until
 * the permission is there; "Not now" is always a way through, because the clock works without it.
 * The widget's page ends the run: a card is offered, never required.
 */
@Composable
private fun BottomBar(step: OnboardingStep, permission: CalendarPermission, actions: OnboardingActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val (label, onClick) = when {
            step == OnboardingStep.WELCOME -> stringResource(R.string.onboarding_start) to actions.next

            step == OnboardingStep.WIDGET -> stringResource(R.string.onboarding_done) to actions.finish

            permission == CalendarPermission.GRANTED -> stringResource(R.string.onboarding_next) to actions.next

            permission == CalendarPermission.DENIED_FOR_GOOD ->
                stringResource(R.string.onboarding_open_settings) to actions.openAppSettings

            else -> stringResource(R.string.onboarding_allow) to actions.ask
        }
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(OnboardingTags.PRIMARY)) {
            Text(label)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            if (step != OnboardingStep.WELCOME) {
                TextButton(onClick = actions.back) { Text(stringResource(R.string.onboarding_back)) }
                if (step == OnboardingStep.CALENDAR && permission != CalendarPermission.GRANTED) {
                    TextButton(onClick = actions.next, modifier = Modifier.testTag(OnboardingTags.NOT_NOW)) {
                        Text(stringResource(R.string.onboarding_not_now))
                    }
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        // The launcher icon's mark (owner, 7 Oct 2026), the one the reader just touched to get
        // here, on the page's own ground (owner, 10 Oct 2026). Decorative: the app's name follows
        // as the page's heading. The spaces are measured so the page fits a phone upright without
        // scrolling (OnboardingScreenTest).
        Image(
            painter = painterResource(DesignR.drawable.ic_app_mark),
            contentDescription = null,
            modifier = Modifier.size(96.dp).testTag(OnboardingTags.MARK),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_app_name),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Promise(TempoIcons.Calendar, R.string.onboarding_calendars_title, R.string.onboarding_calendars_body)
        Promise(TempoIcons.Shield, R.string.onboarding_private_title, R.string.onboarding_private_body)
        Promise(TempoIcons.Battery, R.string.onboarding_battery_title, R.string.onboarding_battery_body)
    }
}

@Composable
private fun Promise(icon: ImageVector, title: Int, body: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarPage(permission: CalendarPermission) {
    Text(
        text = stringResource(R.string.onboarding_calendar_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_calendar_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    if (permission == CalendarPermission.GRANTED) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(OnboardingTags.GRANTED),
            ) {
                Icon(TempoIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    stringResource(R.string.onboarding_calendar_granted),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.onboarding_calendar_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The widgets, offered: the pair, each with what it shows and, where the launcher takes a pin
 * request, a button that hands the card to the launcher's own dialog (where the reader decides).
 * Where it does not, the way to add one by hand. A card placed meanwhile is said, with how to
 * change it.
 */
@Composable
private fun WidgetPage(offer: WidgetOffer, pin: (TempoWidget) -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_widget_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_widget_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    WidgetChoice(
        TempoWidget.AGENDA,
        R.string.onboarding_widget_agenda,
        R.string.onboarding_widget_agenda_body,
        offer,
        pin,
    )
    Spacer(Modifier.height(12.dp))
    WidgetChoice(TempoWidget.WORDS, R.string.onboarding_widget_words, R.string.onboarding_widget_words_body, offer, pin)
    Spacer(Modifier.height(16.dp))
    if (offer.placed) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(OnboardingTags.PLACED),
            ) {
                Icon(TempoIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    stringResource(R.string.onboarding_widget_placed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    } else if (!offer.canPin) {
        Text(
            text = stringResource(R.string.onboarding_widget_manual),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WidgetChoice(widget: TempoWidget, title: Int, body: Int, offer: WidgetOffer, pin: (TempoWidget) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        TempoIcons.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (offer.canPin) {
                    // Under the words, not beside them: at a large text size a button at the side
                    // leaves the description a column one word wide.
                    val label = stringResource(R.string.onboarding_widget_add_a11y, stringResource(title))
                    FilledTonalButton(
                        onClick = { pin(widget) },
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .semantics { contentDescription = label }
                            .testTag(OnboardingTags.pin(widget)),
                    ) {
                        Text(stringResource(R.string.onboarding_widget_add))
                    }
                }
            }
        }
    }
}

/** Hooks for the UI tests. */
object OnboardingTags {
    const val ROOT = "onboarding_root"
    const val PAGE = "onboarding_page"
    const val MARK = "onboarding_mark"
    const val PRIMARY = "onboarding_primary"
    const val NOT_NOW = "onboarding_not_now"
    const val GRANTED = "onboarding_granted"
    const val PLACED = "onboarding_widget_placed"

    fun pin(widget: TempoWidget) = "onboarding_pin_${widget.name.lowercase()}"
}
