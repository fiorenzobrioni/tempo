package com.callbackdev.tempo.feature.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.designsystem.format.DateTimeText
import com.callbackdev.tempo.core.designsystem.icons.TempoIcons
import com.callbackdev.tempo.core.designsystem.theme.ScreenMargin
import com.callbackdev.tempo.core.designsystem.theme.tabular
import com.callbackdev.tempo.core.domain.month.MonthPages
import java.time.Month
import java.time.YearMonth

/**
 * The way to a far month, from a touch on the month's name: a year, stepped with its two arrows,
 * and its twelve months to choose from. Any month of any year is two or three touches away, with
 * no page to turn; a touch on a month goes there and closes. A sheet from the bottom, where the
 * thumb is. The month on screen stands on a quiet ground; the month of today is in the accent,
 * which means now.
 *
 * @param shown the month on screen: the year the sheet opens on.
 * @param current today's month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthJumpSheet(shown: YearMonth, current: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by rememberSaveable { mutableIntStateOf(shown.year) }
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = ScreenMargin, end = ScreenMargin, bottom = 16.dp)
                .testTag(MonthTags.JUMP),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { year-- }, enabled = year > MonthPages.YEARS.first) {
                    Icon(TempoIcons.ChevronLeft, stringResource(R.string.month_previous_year), tint = quiet)
                }
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleLarge.tabular(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        // Said again as the arrows step it.
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag(MonthTags.JUMP_YEAR),
                )
                IconButton(onClick = { year++ }, enabled = year < MonthPages.YEARS.last) {
                    Icon(TempoIcons.ChevronRight, stringResource(R.string.month_next_year), tint = quiet)
                }
            }
            Month.entries.chunked(MONTHS_A_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { month ->
                        val choice = YearMonth.of(year, month)
                        MonthChoice(
                            month = choice,
                            isShown = choice == shown,
                            isCurrent = choice == current,
                            onPick = { onPick(choice) },
                            modifier = Modifier.weight(1f).testTag(MonthTags.jump(month.value)),
                        )
                    }
                }
            }
        }
    }
}

/** One of the twelve: its short name to the eye, its name and year to a screen reader. */
@Composable
private fun MonthChoice(
    month: YearMonth,
    isShown: Boolean,
    isCurrent: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val scheme = MaterialTheme.colorScheme
    val name = DateTimeText.month(month, locale)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (isShown) scheme.secondaryContainer else Color.Transparent)
            .clickable(role = Role.Button, onClick = onPick)
            .clearAndSetSemantics {
                contentDescription = name
                selected = isShown
            },
    ) {
        Text(
            text = DateTimeText.monthShort(month.month, locale),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            color = when {
                isCurrent && !isShown -> scheme.primary
                isShown -> scheme.onSecondaryContainer
                else -> scheme.onSurface
            },
            maxLines = 1,
        )
    }
}

private const val MONTHS_A_ROW = 3
