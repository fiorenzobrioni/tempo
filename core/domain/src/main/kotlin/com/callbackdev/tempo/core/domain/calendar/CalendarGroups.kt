package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.model.CalendarInfo

/** The calendars of one account, as Settings lists them. */
data class AccountCalendars(val accountName: String, val accountType: String, val calendars: List<CalendarInfo>)

/**
 * The phone's calendars grouped by account, for Settings (PLANNING.md §11 Phase 2): the accounts in
 * alphabetical order with the phone's own calendars (`LOCAL`, no sync) last; in each, the primary
 * calendar first, then by name. The same order on every read, so a switch never moves under the
 * reader's finger when a sync lands.
 */
object CalendarGroups {
    /** The account type Android gives a calendar kept on the phone alone. */
    const val LOCAL_ACCOUNT_TYPE = "LOCAL"

    fun of(calendars: List<CalendarInfo>): List<AccountCalendars> = calendars
        .groupBy { it.accountType to it.accountName }
        .map { (account, members) ->
            AccountCalendars(
                accountName = account.second,
                accountType = account.first,
                calendars = members.sortedWith(
                    compareByDescending<CalendarInfo> { it.isPrimary }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                        .thenBy { it.id },
                ),
            )
        }
        .sortedWith(
            compareBy<AccountCalendars> { it.accountType == LOCAL_ACCOUNT_TYPE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.accountName }
                .thenBy { it.accountType },
        )
}
