package com.callbackdev.tempo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.callbackdev.tempo.core.designsystem.format.ZoneCities
import com.callbackdev.tempo.core.designsystem.format.ZoneCity
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.padding
import com.callbackdev.tempo.core.designsystem.theme.pageGutter
import java.time.Instant
import java.time.ZoneId

/**
 * The second clock's city (step 8.5), over Settings on the whole screen: four hundred cities are a
 * page, not a dialog's list. The search on top, "No second clock" first, then every city of the
 * phone's time-zone data in the language's order, each with its country and its offset now; the
 * phone's own zone is not among them. A touch chooses and closes, as every choice in Settings does.
 */
@Composable
internal fun SecondZoneDialog(selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        SecondZonePage(selected, onPick, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondZonePage(
    selected: String?,
    onPick: (String?) -> Unit,
    onBack: () -> Unit,
    phone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
) {
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    val cities = remember(resources, locale, phone) { ZoneCities.all(resources, locale, phone, now) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(cities, query) { cities.filter { it.matches(query) } }
    // Opened on the reader's city, a few rows down from the top, so the list says where it stands.
    val list = rememberLazyListState(
        initialFirstVisibleItemIndex = (cities.indexOfFirst { it.id == selected } + 1 - ROWS_ABOVE).coerceAtLeast(0),
    )
    val gutter = pageGutter(sideInsets = false)
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_second_zone)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(TempoIcons.Back, contentDescription = stringResource(R.string.settings_back))
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets.add(gutter.asInsets()),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.settings_zone_search)) },
                    leadingIcon = { Icon(TempoIcons.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(gutter)
                        .padding(horizontal = ScreenMargin)
                        .padding(bottom = 8.dp)
                        .testTag(SettingsTags.ZONE_SEARCH),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .selectableGroup()
                .testTag(SettingsTags.ZONE_LIST),
            contentPadding = gutter.contentPadding(bottom = 24.dp),
        ) {
            if (query.isBlank()) {
                item(key = "none") {
                    ZoneRow(
                        label = stringResource(R.string.settings_second_zone_none),
                        note = null,
                        selected = selected == null,
                        onClick = { onPick(null) },
                        modifier = Modifier.testTag(SettingsTags.ZONE_NONE),
                    )
                }
            }
            items(shown, key = { it.id }) { city ->
                ZoneRow(
                    label = city.city,
                    note = note(city),
                    selected = city.id == selected,
                    onClick = { onPick(city.id) },
                    modifier = Modifier.testTag(SettingsTags.zone(city.id)),
                )
            }
            if (shown.isEmpty()) {
                item(key = "not-found") {
                    Text(
                        text = stringResource(R.string.settings_zone_not_found, query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** "United States · UTC−4": the country where the zone has one, then the offset now. */
private fun note(city: ZoneCity): String = listOfNotNull(city.country, city.offset).joinToString(" · ")

/** A choice: the row is the touch, the radio button only shows it. */
@Composable
private fun ZoneRow(label: String, note: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Rows shown over the reader's city when the list opens on it. */
private const val ROWS_ABOVE = 2
