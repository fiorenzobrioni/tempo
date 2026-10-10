package com.callbackdev.tempo.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tempo.core.calendar.CalendarAccess
import com.callbackdev.tempo.core.data.widget.TempoWidget
import com.callbackdev.tempo.core.designsystem.components.CalendarDot
import com.callbackdev.tempo.core.designsystem.components.CalendarPermissionCard
import com.callbackdev.tempo.core.designsystem.components.GroupDivider
import com.callbackdev.tempo.core.designsystem.components.GroupHeader
import com.callbackdev.tempo.core.designsystem.components.InfoRow
import com.callbackdev.tempo.core.designsystem.components.RadioDialog
import com.callbackdev.tempo.core.designsystem.components.SettingsGroup
import com.callbackdev.tempo.core.designsystem.components.SwitchRow
import com.callbackdev.tempo.core.designsystem.components.ValueRow
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.format.uses24Hour
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.GroupShape
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.TempoTheme
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import com.callbackdev.tempo.core.domain.calendar.AccountCalendars
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.calendar.CalendarGroups
import com.callbackdev.tempo.core.model.AppFont
import com.callbackdev.tempo.core.model.AppPalette
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.CalendarPermission
import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.DateStyle
import com.callbackdev.tempo.core.model.ThemeMode
import com.callbackdev.tempo.core.model.UserSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenGuide: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val asked = state?.settings?.askedCalendarPermission ?: false
    var permission by remember { mutableStateOf(CalendarPermission.ASKABLE) }
    val refreshPermission = {
        permission = activity?.let { CalendarAccess.permission(it, asked) } ?: CalendarPermission.ASKABLE
    }
    // Back from the system's settings, the permission and the calendars may both have changed.
    LifecycleResumeEffect(asked) {
        refreshPermission()
        viewModel.rereadCalendars()
        onPauseOrDispose { }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionAnswered()
        refreshPermission()
    }
    val context = LocalContext.current
    SettingsScreen(
        state = state,
        permission = permission,
        onBack = onBack,
        actions = SettingsActions(
            update = viewModel::update,
            setCalendarShown = viewModel::setCalendarShown,
            askPermission = { ask.launch(CalendarAccess.PERMISSION) },
            openAppSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context)),
                )
            },
            pinWidget = viewModel::pinWidget,
            openGuide = onOpenGuide,
        ),
    )
}

/** What the list can ask for, as functions: the screen is a plain composable a test can draw. */
class SettingsActions(
    val update: ((UserSettings) -> UserSettings) -> Unit = {},
    val setCalendarShown: (CalendarInfo, Boolean) -> Unit = { _, _ -> },
    val askPermission: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val pinWidget: (TempoWidget) -> Unit = {},
    val openGuide: () -> Unit = {},
)

/**
 * Settings, laid out as Chiaro's and Passo's: every group on one rounded ground under a header in
 * the accent, a live preview of the appearance above the choices that change it, the privacy note
 * as a statement, the licence and the credits last. The guide's card comes first, as in the
 * sisters' lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    permission: CalendarPermission,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TempoIcons.Back, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the store's first answer the list is not drawn: a screen of defaults that may be
        // about to change is a lie with good intentions.
        if (state != null) SettingsList(state, permission, actions, Modifier.fillMaxSize().padding(padding))
    }
}

/**
 * The widgets (PLANNING.md §8): the pair, each handed to the launcher's own dialog where it takes a
 * pin request, and otherwise the way to add one by hand; then where a card's own settings are
 * (its long press), because nothing here changes a placed card's look.
 */
@Composable
private fun WidgetsSection(widgets: WidgetsInfo, pin: (TempoWidget) -> Unit) {
    SettingsGroup {
        if (widgets.canPin) {
            ValueRow(
                label = stringResource(R.string.settings_widget_agenda),
                value = stringResource(R.string.settings_widget_agenda_note),
                onClick = { pin(TempoWidget.AGENDA) },
                icon = TempoIcons.Widgets,
                modifier = Modifier.testTag(SettingsTags.PIN_AGENDA),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.settings_widget_words),
                value = stringResource(R.string.settings_widget_words_note),
                onClick = { pin(TempoWidget.WORDS) },
                icon = TempoIcons.Widgets,
                modifier = Modifier.testTag(SettingsTags.PIN_WORDS),
            )
            GroupDivider()
        } else {
            InfoRow(stringResource(R.string.settings_widget_add), stringResource(R.string.settings_widget_manual))
            GroupDivider()
        }
        InfoRow(stringResource(R.string.settings_widget_change), stringResource(R.string.settings_widget_change_note))
    }
}

private enum class Dialog {
    TIME,
    DATE,
    HORIZON,
    THEME,
    PALETTE,
    FONT,
}

@Composable
private fun SettingsList(
    state: SettingsUiState,
    permission: CalendarPermission,
    actions: SettingsActions,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val settings = state.settings
    val locale = LocalLocale.current.platformLocale
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }

    LazyColumn(
        modifier = modifier.testTag(SettingsTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
    ) {
        item { GuideCard(actions.openGuide) }
        item { GroupHeader(stringResource(R.string.settings_group_clock)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_time_format),
                    value = timeLabel(settings.clockFormat),
                    onClick = { dialog = Dialog.TIME },
                    modifier = Modifier.testTag(SettingsTags.TIME),
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_date_format),
                    value = DateTimeText.date(LocalDate.now(), locale, settings.dateStyle),
                    onClick = { dialog = Dialog.DATE },
                    modifier = Modifier.testTag(SettingsTags.DATE),
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_agenda)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_horizon),
                    value = horizonLabel(settings.horizonDays),
                    onClick = { dialog = Dialog.HORIZON },
                    modifier = Modifier.testTag(SettingsTags.HORIZON),
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_all_day),
                    note = stringResource(R.string.settings_all_day_note),
                    checked = settings.showAllDay,
                    onChange = { on -> actions.update { it.copy(showAllDay = on) } },
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_declined),
                    note = stringResource(R.string.settings_declined_note),
                    checked = settings.showDeclined,
                    onChange = { on -> actions.update { it.copy(showDeclined = on) } },
                    modifier = Modifier.testTag(SettingsTags.DECLINED),
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_next_alarm),
                    note = stringResource(R.string.settings_next_alarm_note),
                    checked = settings.showNextAlarm,
                    onChange = { on -> actions.update { it.copy(showNextAlarm = on) } },
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_calendar_button),
                    note = stringResource(R.string.settings_calendar_button_note),
                    checked = settings.showCalendarButton,
                    onChange = { on -> actions.update { it.copy(showCalendarButton = on) } },
                    modifier = Modifier.testTag(SettingsTags.CALENDAR_BUTTON),
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_calendars)) }
        item { CalendarsSection(state.calendars, settings, permission, actions) }

        item { GroupHeader(stringResource(R.string.settings_group_widgets)) }
        item { WidgetsSection(state.widgets, actions.pinWidget) }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        item { AppearancePreview(settings, locale) }
        item {
            SettingsGroup {
                ValueRow(stringResource(R.string.settings_theme), themeLabel(settings.theme), { dialog = Dialog.THEME })
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_palette),
                    value = paletteLabel(settings.palette),
                    onClick = { dialog = Dialog.PALETTE },
                    enabled = !settings.dynamicColor,
                )
                GroupDivider()
                ValueRow(stringResource(R.string.settings_font), fontLabel(settings.font), { dialog = Dialog.FONT })
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    note = stringResource(R.string.settings_dynamic_color_note),
                    checked = settings.dynamicColor,
                    onChange = { on -> actions.update { it.copy(dynamicColor = on) } },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_language)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_language),
                    value = currentLanguageLabel(),
                    trailing = true,
                    onClick = {
                        // The system's per-app language page: one place for it, the same in every app.
                        context.startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, appUri(context)),
                        )
                    },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_privacy)) }
        item { PrivacyCard() }

        item { GroupHeader(stringResource(R.string.settings_group_about)) }
        item {
            SettingsGroup {
                InfoRow(stringResource(R.string.settings_version), state.version)
                GroupDivider()
                InfoRow(stringResource(R.string.settings_developer), stringResource(R.string.settings_developer_note))
                GroupDivider()
                InfoRow(stringResource(R.string.settings_copyright), stringResource(R.string.settings_copyright_note))
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_license),
                    value = stringResource(R.string.settings_license_note),
                    trailing = true,
                    onClick = { openUrl(context, "https://www.gnu.org/licenses/gpl-3.0.html") },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_source_code),
                    value = stringResource(R.string.settings_source_code_note),
                    trailing = true,
                    onClick = { openUrl(context, "https://github.com/fiorenzobrioni/tempo") },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_credits)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_credit_font),
                    value = fontCreditNote(settings.font),
                    trailing = true,
                    onClick = { openUrl(context, fontCreditUrl(settings.font)) },
                )
            }
        }
    }

    val close = { dialog = null }
    fun choose(transform: (UserSettings) -> UserSettings) {
        actions.update(transform)
        dialog = null
    }
    when (dialog) {
        Dialog.TIME -> RadioDialog(
            title = stringResource(R.string.settings_time_format),
            explanation = stringResource(R.string.settings_time_note),
            options = ClockFormat.entries.map { it to timeLabel(it) },
            selected = settings.clockFormat,
            onSelect = { format -> choose { it.copy(clockFormat = format) } },
            onDismiss = close,
        )

        Dialog.DATE -> RadioDialog(
            title = stringResource(R.string.settings_date_format),
            explanation = stringResource(R.string.settings_date_note),
            options = DateStyle.entries.map { it to DateTimeText.date(LocalDate.now(), locale, it) },
            selected = settings.dateStyle,
            onSelect = { style -> choose { it.copy(dateStyle = style) } },
            onDismiss = close,
        )

        Dialog.HORIZON -> RadioDialog(
            title = stringResource(R.string.settings_horizon),
            explanation = stringResource(R.string.settings_horizon_note),
            options = UserSettings.HORIZON_CHOICES.map { it to horizonLabel(it) },
            selected = settings.horizonDays,
            onSelect = { days -> choose { it.copy(horizonDays = days) } },
            onDismiss = close,
        )

        Dialog.THEME -> RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.theme,
            onSelect = { theme -> choose { it.copy(theme = theme) } },
            onDismiss = close,
        )

        Dialog.PALETTE -> RadioDialog(
            title = stringResource(R.string.settings_palette),
            explanation = stringResource(R.string.settings_palette_note),
            options = AppPalette.entries.map { it to paletteLabel(it) },
            selected = settings.palette,
            onSelect = { palette -> choose { it.copy(palette = palette) } },
            onDismiss = close,
        )

        Dialog.FONT -> RadioDialog(
            title = stringResource(R.string.settings_font),
            explanation = stringResource(R.string.settings_font_note),
            options = AppFont.entries.map { it to fontLabel(it) },
            selected = settings.font,
            onSelect = { font -> choose { it.copy(font = font) } },
            onDismiss = close,
        )

        null -> Unit
    }
}

/**
 * The phone's calendars, by account, each with its colour and a switch (PLANNING.md §11 Phase 2);
 * or why there is no list: no permission yet (and the way to give it), none on the phone. Nothing
 * at all until the first read, rather than a flash of "no calendar".
 */
@Composable
private fun CalendarsSection(
    calendars: CalendarsState?,
    settings: UserSettings,
    permission: CalendarPermission,
    actions: SettingsActions,
) {
    when (calendars) {
        null -> Unit

        CalendarsState.NoPermission -> CalendarPermissionCard(
            permission = permission,
            onAsk = actions.askPermission,
            onOpenAppSettings = actions.openAppSettings,
            modifier = Modifier.padding(horizontal = ScreenMargin).testTag(SettingsTags.PERMISSION),
        )

        is CalendarsState.Listed -> {
            val all = calendars.accounts.flatMap { it.calendars }
            if (all.isEmpty()) {
                SettingsGroup {
                    InfoRow(
                        stringResource(R.string.settings_group_calendars),
                        stringResource(R.string.settings_calendars_none),
                    )
                }
                return
            }
            val shown = all.count { CalendarChoices.isShown(it, settings.calendarChoices) }
            Text(
                text = pluralStringResource(R.plurals.settings_calendars_summary, all.size, all.size, shown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 20.dp, end = ScreenMargin, bottom = 12.dp)
                    .testTag(SettingsTags.CALENDARS_SUMMARY),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                calendars.accounts.forEach { account -> AccountGroup(account, settings, actions) }
            }
        }
    }
}

@Composable
private fun AccountGroup(account: AccountCalendars, settings: UserSettings, actions: SettingsActions) {
    SettingsGroup {
        Text(
            text = if (account.accountType == CalendarGroups.LOCAL_ACCOUNT_TYPE) {
                stringResource(R.string.settings_calendar_phone)
            } else {
                account.accountName
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ScreenMargin, end = ScreenMargin, top = 14.dp, bottom = 2.dp),
        )
        account.calendars.forEachIndexed { index, calendar ->
            if (index > 0) GroupDivider()
            CalendarRow(
                calendar = calendar,
                shown = CalendarChoices.isShown(calendar, settings.calendarChoices),
                onChange = { actions.setCalendarShown(calendar, it) },
            )
        }
    }
}

/** A calendar's switch: the row is the target, the dot its colour, the words its state for TalkBack. */
@Composable
private fun CalendarRow(calendar: CalendarInfo, shown: Boolean, onChange: (Boolean) -> Unit) {
    val state = if (shown) R.string.settings_calendar_shown else R.string.settings_calendar_hidden
    val description = stringResource(state, calendar.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onChange(!shown) }, role = Role.Switch)
            .semantics {
                contentDescription = description
                toggleableState = ToggleableState(shown)
            }
            .padding(horizontal = ScreenMargin, vertical = 12.dp)
            .testTag(SettingsTags.calendar(calendar.id)),
    ) {
        CalendarDot(calendar.color, MaterialTheme.colorScheme.surfaceContainerLow, size = 14.dp)
        Text(calendar.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = shown, onCheckedChange = null)
    }
}

/**
 * The appearance as Today will wear it: the time in the hero type, the date, an event row with
 * its calendar's mark. Every choice below changes it the moment it is made. A picture, silent to a
 * screen reader: the rows say every choice in words.
 */
@Composable
private fun AppearancePreview(settings: UserSettings, locale: Locale) {
    val uses24 = uses24Hour(settings.clockFormat)
    val sample = ZonedDateTime.now().with(PREVIEW_TIME)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, bottom = 12.dp)
            .clearAndSetSemantics { },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(16.dp)) {
            Text(
                text = DateTimeText.time(sample, locale, uses24),
                style = TempoTheme.type.heroNumber.copy(fontSize = 40.sp, lineHeight = 44.sp),
            )
            Text(
                text = DateTimeText.date(sample, locale, settings.dateStyle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                CalendarDot(PREVIEW_CALENDAR_COLOR, MaterialTheme.colorScheme.surfaceContainerLow)
                Text(
                    text = DateTimeText.time(sample.plusMinutes(19), locale, uses24),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.settings_preview_event), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private val PREVIEW_TIME: LocalTime = LocalTime.of(9, 41)

/** Google Calendar's "Basil", the colour of a typical first calendar. */
private const val PREVIEW_CALENDAR_COLOR = 0xFF0B8043.toInt()

/**
 * The way to the guide, first in the list as in Chiaro's and Passo's Settings: the place a reader
 * comes back to the day the question arrives, which a card shown once on Today could never be.
 */
@Composable
private fun GuideCard(onOpenGuide: () -> Unit) {
    Surface(
        onClick = onOpenGuide,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp)
            .testTag(SettingsTags.GUIDE),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(TempoIcons.Info, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_guide), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_guide_note), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(TempoIcons.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(16.dp)) {
            Icon(TempoIcons.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_privacy_headline), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Each choice with the time it writes now, so the reader picks by what they will see. */
@Composable
private fun timeLabel(format: ClockFormat): String {
    val locale = LocalLocale.current.platformLocale
    val now = ZonedDateTime.now()
    return when (format) {
        ClockFormat.SYSTEM -> stringResource(
            R.string.settings_time_system,
            DateTimeText.time(now, locale, uses24Hour(format)),
        )

        ClockFormat.H24 -> stringResource(R.string.settings_time_24, DateTimeText.time(now, locale, true))

        ClockFormat.H12 -> stringResource(R.string.settings_time_12, DateTimeText.time(now, locale, false))
    }
}

@Composable
private fun horizonLabel(days: Int): String = stringResource(
    when (days) {
        1 -> R.string.settings_horizon_1
        2 -> R.string.settings_horizon_2
        else -> R.string.settings_horizon_7
    },
)

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun paletteLabel(palette: AppPalette): String = when (palette) {
    AppPalette.PAPER -> stringResource(R.string.settings_palette_paper)
    AppPalette.VIVID -> stringResource(R.string.settings_palette_vivid)
}

@Composable
private fun fontLabel(font: AppFont): String = when (font) {
    AppFont.GOOGLE_SANS -> stringResource(R.string.settings_font_google_sans)
    AppFont.INTER -> stringResource(R.string.settings_font_inter)
    AppFont.SYSTEM -> stringResource(R.string.settings_font_system)
}

/** Both bundled faces travel in the APK, so both are credited; the note says which one is on screen. */
@Composable
private fun fontCreditNote(font: AppFont): String {
    val inUse = when (font) {
        AppFont.SYSTEM -> stringResource(R.string.settings_credit_font_inuse_system)
        else -> stringResource(R.string.settings_credit_font_inuse, fontLabel(font))
    }
    return stringResource(R.string.settings_credit_font_note) + " · " + inUse
}

private fun fontCreditUrl(font: AppFont): String = when (font) {
    AppFont.INTER -> "https://rsms.me/inter/"
    AppFont.GOOGLE_SANS -> "https://fonts.google.com/specimen/Google+Sans"
    AppFont.SYSTEM -> "https://openfontlicense.org"
}

/** What the app is speaking: the reader's pick in the system picker, or the phone's language. */
@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val appLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    if (appLocales == null || appLocales.isEmpty) return stringResource(R.string.settings_language_system)
    val chosen = appLocales[0]
    return chosen.getDisplayLanguage(chosen).replaceFirstChar { it.titlecase(locale) }
}

/** This app's own page in the system's settings, for its permissions and its language. */
private fun appUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

private fun openUrl(context: Context, url: String) {
    // The browser opens it: Tempo itself has no network access. No browser, no crash.
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

/** Hooks for the UI tests. */
object SettingsTags {
    const val LIST = "settings_list"
    const val TIME = "settings_time"
    const val DATE = "settings_date"
    const val HORIZON = "settings_horizon"
    const val DECLINED = "settings_declined"
    const val CALENDAR_BUTTON = "settings_calendar_button"
    const val PERMISSION = "settings_permission"
    const val CALENDARS_SUMMARY = "settings_calendars_summary"
    const val PIN_AGENDA = "settings_pin_agenda"
    const val PIN_WORDS = "settings_pin_words"
    const val GUIDE = "settings_guide"

    fun calendar(id: Long) = "settings_calendar_$id"
}
