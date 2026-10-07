package com.callbackdev.tempo.core.calendar

import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Events
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.EventStatus

/*
 * The provider's integer codes, read into the model's words. An unknown code, or a provider that
 * left the column empty, reads as the ordinary case (confirmed, no invitation, busy): a calendar
 * from a maker's own sync adapter must never lose an event to a value Tempo did not expect.
 */

internal fun eventStatusOf(code: Int?): EventStatus = when (code) {
    Events.STATUS_TENTATIVE -> EventStatus.TENTATIVE
    Events.STATUS_CANCELED -> EventStatus.CANCELED
    else -> EventStatus.CONFIRMED
}

internal fun attendeeStatusOf(code: Int?): AttendeeStatus = when (code) {
    Attendees.ATTENDEE_STATUS_ACCEPTED -> AttendeeStatus.ACCEPTED
    Attendees.ATTENDEE_STATUS_DECLINED -> AttendeeStatus.DECLINED
    Attendees.ATTENDEE_STATUS_INVITED -> AttendeeStatus.INVITED
    Attendees.ATTENDEE_STATUS_TENTATIVE -> AttendeeStatus.TENTATIVE
    else -> AttendeeStatus.NONE
}

internal fun availabilityOf(code: Int?): Availability = when (code) {
    Events.AVAILABILITY_FREE -> Availability.FREE
    Events.AVAILABILITY_TENTATIVE -> Availability.TENTATIVE
    else -> Availability.BUSY
}
