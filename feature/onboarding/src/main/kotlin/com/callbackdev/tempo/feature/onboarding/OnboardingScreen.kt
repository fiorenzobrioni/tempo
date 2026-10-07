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
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.designsystem.R as DesignR

/**
 * The first run's pages (PLANNING.md §11 Phase 3), in Passo's shape: what Tempo is, then the one
 * permission it needs. The widget's page joins with the widgets (Phase 4): a page offering a card
 * that does not exist yet would be the screen lying.
 */
enum class OnboardingStep {
    WELCOME,
    CALENDAR,
}

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
    OnboardingScreen(
        step = step,
        permission = permission,
        actions = OnboardingActions(
            next = { step = OnboardingStep.CALENDAR },
            back = { step = OnboardingStep.WELCOME },
            ask = { ask.launch(CalendarAccess.PERMISSION) },
            openAppSettings = {
                runCatching {
                    val page = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, page))
                }
            },
            finish = viewModel::finish,
        ),
    )
}

class OnboardingActions(
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val ask: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val finish: () -> Unit = {},
)

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    permission: CalendarPermission,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val gutter = pageGutter()
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = actions.back)
    Surface(modifier = modifier.fillMaxSize().testTag(OnboardingTags.ROOT)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (step != OnboardingStep.WELCOME) {
                Box(Modifier.padding(gutter)) { Progress(step.ordinal, OnboardingStep.entries.size) }
            }
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
 */
@Composable
private fun BottomBar(step: OnboardingStep, permission: CalendarPermission, actions: OnboardingActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val (label, onClick) = when {
            step == OnboardingStep.WELCOME -> stringResource(R.string.onboarding_start) to actions.next

            permission == CalendarPermission.GRANTED -> stringResource(R.string.onboarding_done) to actions.finish

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
                if (permission != CalendarPermission.GRANTED) {
                    TextButton(onClick = actions.finish, modifier = Modifier.testTag(OnboardingTags.NOT_NOW)) {
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
        Spacer(Modifier.height(40.dp))
        // The launcher icon itself (owner, 7 Oct 2026), the mark the reader just touched to get
        // here. Decorative: the app's name follows as the page's heading.
        Image(
            painter = painterResource(DesignR.drawable.ic_app_mark),
            contentDescription = null,
            modifier = Modifier.size(112.dp).testTag(OnboardingTags.MARK),
        )
        Spacer(Modifier.height(24.dp))
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
        Spacer(Modifier.height(36.dp))
        Promise(TempoIcons.Calendar, R.string.onboarding_calendars_title, R.string.onboarding_calendars_body)
        Promise(TempoIcons.Shield, R.string.onboarding_private_title, R.string.onboarding_private_body)
        Promise(TempoIcons.Battery, R.string.onboarding_battery_title, R.string.onboarding_battery_body)
    }
}

@Composable
private fun Promise(icon: ImageVector, title: Int, body: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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

/** Hooks for the UI tests. */
object OnboardingTags {
    const val ROOT = "onboarding_root"
    const val PAGE = "onboarding_page"
    const val MARK = "onboarding_mark"
    const val PRIMARY = "onboarding_primary"
    const val NOT_NOW = "onboarding_not_now"
    const val GRANTED = "onboarding_granted"
}
