package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.domain.agenda.calendar
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalendarGroupsTest {
    @Test
    fun `accounts in order, the phone's own last, the primary calendar first`() {
        val work = calendar(1).copy(accountName = "work@example.com", name = "Team", isPrimary = false)
        val workPrimary = calendar(
            2,
        ).copy(accountName = "work@example.com", name = "work@example.com", isPrimary = true)
        val home = calendar(3).copy(accountName = "home@example.com", name = "Family", isPrimary = false)
        val local = calendar(
            4,
        ).copy(accountName = "Phone", accountType = CalendarGroups.LOCAL_ACCOUNT_TYPE, name = "Mine")
        val groups = CalendarGroups.of(listOf(local, work, home, workPrimary))
        assertThat(
            groups.map {
                it.accountName
            },
        ).containsExactly("home@example.com", "work@example.com", "Phone").inOrder()
        assertThat(groups[1].calendars.map { it.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun `no calendars, no groups`() {
        assertThat(CalendarGroups.of(emptyList())).isEmpty()
    }
}
