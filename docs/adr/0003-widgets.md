# ADR 0003: The widgets (Phase 4)

- Status: accepted
- Date: 2026-10-08

## Context

VISION.md promises the family's pair on the home screen, «Agenda» and «In words»: a clock that
costs nothing, the day's events as many as fit and never a scrolling list, a card that is fresh
without polling, in the same dress as Chiaro's and Passo's cards. PLANNING.md §7 sets the forms,
the taps and the refresh. Passo's widget module (its ADR 0005, from Chiaro) is the family's
working answer to most of it: the card, the inks, the arithmetic of what fits measured in the
launcher's face, the settings screen with the live card, the generated previews. What it does not
have, and Tempo needs, is a clock and a refresh driven by a calendar instead of a step sensor.

## Decisions

1. **Passo's card, carried over.** The 24 dp corner, the insets, the four grounds and Chiaro's six
   colours, the opacity and the ink rule (`WidgetInk`, `widgetPalette`), the measuring in the
   launcher's own face (`textEm`, `measureWidgetLines`, a 6% margin), the settings screen with the
   real card composed into `RemoteViews` at the family's reference grants, the generated previews
   published once per version, `WidgetRefresh`'s revision so a live session reloads, and the
   guarded load that never leaves Glance's spinner on screen.

2. **The clock is the system's `TextClock`**, in six small layouts (bold, medium, regular, and a
   frozen `TextView` of each for the samples) placed with `AndroidRemoteViews`. Its patterns are
   the locale's (`getBestDateTimePattern`), both of them where the card follows the phone's
   12/24-hour switch, so the system picks; the size, ink and lines are set through `RemoteViews`.
   The launcher draws it and the system moves it every minute: no repaint by Tempo, ever. Its
   width is measured on the pattern the phone uses now; switching the phone's format sends
   `ACTION_TIME_CHANGED`, which redraws the cards. Each clock line carries its own height (a
   container Glance sizes alone took its column's whole height beside a weighted spacer). The
   date is a `TextClock` too, so it turns at midnight exactly; it is therefore in the locale's own
   case ("mercoledì 7 ottobre", as Android's lock screen writes it), where Today capitalises it.

3. **What fits is pure Kotlin** (`:core:domain`, `widget/`): `CardAgenda` (what «Agenda» lists:
   the rest of today, a date's all-day events on one line, then the days ahead in the room today
   leaves), `fitLines` (whole lines only, a heading never last, "N more today" or "N more in the
   days ahead"), `AgendaFit` (four forms and their budgets), `WordsDay` (the focus and its note)
   and `WordsFit` (its forms and the order the hierarchy spends height). The widget measures,
   the domain decides, and the tests pin the decisions at the reference grants; `WidgetFitTest`
   draws every form 5% wider than measured, in both languages and three text sizes, and finds
   nothing cut but an event's title on its one line.

4. **A card says times, never minutes from now.** It is redrawn at the agenda's boundaries, not
   each minute, so "in 40 minutes" would be wrong a minute later: «In words» says "Until 11:00",
   "15:00", "Free until then; one more after it.", and Today keeps its minutes.

5. **Pushed, never polled** (PLANNING.md §9): the Calendar Provider's own `PROVIDER_CHANGED`
   broadcast, received in the manifest (9 Oct 2026); a one-shot WorkManager job with a content-URI
   trigger on the provider (quiet for 3 s, at most 20 s after a change) as the safety net, one
   waiting at a time, armed again as each run starts; whatever asked stays until the cards are
   drawn, so the process is not frozen with a card half drawn; one `AlarmManager.set(RTC)` at the
   next boundary (`NextBoundary`), inexact and non-wakeup, replaced by each render; the exempt
   broadcasts (time set, zone, language, package replaced, and since 30 Oct 2026 the next alarm
   changed, decision 12) from the manifest; Tempo's settings changing, and the reader leaving the
   app. The last card removed disarms the job and the alarm.
   No service, no periodic work, no wakelock of Tempo's own.

6. **Touches.** The card opens Tempo; an event opens its occurrence in the calendar app by default
   (as on Today), or Tempo; the clock and the date open Tempo, the calendar app at today, or the
   phone's clock app (owner, 8 Oct 2026). The clock app is the one that answers
   `AlarmClock.ACTION_SHOW_ALARMS` (the reader's default first, else the phone's own), opened by
   its launcher entry: a clock app may guard its alarm actions with `SET_ALARM`, a permission
   Tempo would gain only for this. Seen through `<queries>`. A door that is gone falls back to
   Tempo; a choice that cannot work on the phone is not offered. Two occurrences of one event are
   two doors (`Intent.setIdentifier`): they share a URI, and a pending intent matched on it alone
   would carry one's times for both.

7. **The first run's widget page and Settings' widgets group** offer the pair through Android's
   pin request where the launcher takes one, and the way by hand where it does not. The feature
   modules reach `:widget` through `HomeScreenWidgets` (`:core:data`), bound by Hilt.

8. **«In words» on Passo's row's sizes** (owner, 8 Oct 2026, after the first cards on the phone):
   the line on top at 16 sp, the focus's time up to 34 sp, so the two cards read as one set side
   by side; `WordsFit` keeps the numbers, the tests their consequences. «Agenda»'s one-row date
   follows, at 16 sp under its clock in every form, and its row's clock at Passo's 34 sp. A line of
   Glance text is budgeted with its rounded pixel (+1 dp), and the fit test fails on a squeezed
   line, not only on a cut one.

9. **«In words» around a dial** (owner, 8 Oct 2026, after the 4×1 on the phone: the time in words
   at 34 sp left the title an ellipsis). The card's picture is a clock face where Passo's has its
   ring: the system's `AnalogClock` hands (a `RemoteViews` view like `TextClock`, drawn by the
   launcher, moved by the system) over a face Tempo paints into a bitmap at each redraw, with the
   focus as an arc and the rest of the next twelve hours quieter (`DialArcs`, `WidgetDial.kt`).
   The arcs change only at the agenda's boundaries, where the card is redrawn anyway, so the hour
   hand walks through an event by itself: live progress at no cost. The window is eleven and a
   half hours, so an arc never meets the hand from behind. The hands' drawables are drawn at
   200 dp (lint's ceiling for a vector), above any dial, because an `AnalogClock` scales its drawables down and never up; the
   painter and the drawables share `DialGeometry`, and the samples paint the hands in. The
   words drop to Chiaro's sentence (20 sp) and Passo's facts (16 sp); no number is large. Three
   forms (`WordsFit`): CELL, ROW, TALL. Considered and left: a progress ring (it would need a
   repaint per minute), a day strip with a "now" marker (the same), the event's time kept large
   beside a smaller title (the owner's complaint).
10. **One type scale for the family's cards** (amendment of 13 Oct 2026, from the family's widget
    review). Chiaro's widgets grew a scale of six steps, each a role (caption 11, label 13, body
    16, title 22, display 30, hero 34), and the family's cards print in it, so a row or a note is
    one size on every card: `WidgetType`, held by `WidgetTypeTest`. Here every line of
    «Agenda»'s list moved to the label step (the title from 14, the heading and the footer from
    12, the note from 14), «In words»'s note from 14 to 13, and a card's message and hint from 14
    and 12 to 16 and 13. The clocks and «In words»'s title keep their ladders: they are sized to
    the grant, and a ladder's first choice is a step wherever one exists.
11. **The launcher's corner** (amendment of 13 Oct 2026, from the family's widget review). The card
    takes the radius the launcher gives its widgets (`system_app_widget_background_radius`, in
    `WidgetCard`, the settings preview and `widget_preview_card`) instead of a fixed 24 dp: the
    same on stock Android 16, and the launcher's own wherever a phone sets another.
12. **The next alarm on «Agenda»** (amendment of 30 Oct 2026, PLANNING.md §11 step 8.2; the
    owner's choices). The leading column of the 4×2 was empty under the date for about 40% of the
    card; it now says the phone's next alarm, a bell drawn as a vector (`widget_alarm`, Today's
    bell) and a clock time at the label step in the secondary ink: "Tomorrow 07:00", or "07:00"
    where the column has no room for the day (within a day a clock time names one moment only).
    Only an alarm that rings within 24 hours (`CardAlarm`): one on Monday seen on Friday is noise.
    In a side column under the date, never widening the column; on a header on top, at the far
    end of the date's line where both fit whole, else not (the date is not shortened for it); not
    on a row or a cell. A per-widget switch, on by default, «Agenda»'s alone until «In words»
    shows it too. Its touch opens the clock app, else Tempo. Tempo reads the alarm
    (`AlarmManager.getNextAlarmClock`, no permission) and never sets, rings or moves one. It stays
    true without polling: the system's `NEXT_ALARM_CLOCK_CHANGED`, which AOSP's
    `AlarmManagerService` sends with `FLAG_RECEIVER_INCLUDE_BACKGROUND` (as the provider sends
    `PROVIDER_CHANGED`) and the exempt-broadcast list names beside `TIME_SET`, reaches the
    manifest's receiver; and the moments it changes by itself (a day before it rings, midnight
    for tomorrow's, the ring) are boundaries where an «Agenda» card is placed. The step asked for
    the broadcast to be seen on the phone before the alarm was built; the owner chose to build it
    on the source's evidence and check it on the phone before the merge (PLANNING.md §15). Where
    the alarm stands under the date its line fills the column: a vertical `LinearLayout` whose
    lines all fill it takes its widest line's width, and one line that only wraps its words set
    the column to its own and cut a 12-hour clock.
13. **«Agenda»'s time column is set by the rows it shows** (amendment of 30 Oct 2026, step 8.2).
    The event under way says "→ 11:00" on an inline row where "Until 11:00" would be the widest
    time of the list and widen the column for every title, and keeps "Until 11:00" where the
    column is as wide anyway and on a stacked row. An all-day label much wider than the timed
    rows' times (Italian's "Tutto il giorno", over an em wider) stands over its titles across the
    list instead, and the column is left to the timed rows (`AgendaFit.timeColumn`, `TimeColumn`).
    And the column is measured over the lines the card shows: the card is laid out once with
    every line of the week, then again with the lines that fitted, and shows no more than those (a
    line past them was not measured). A time never drawn ("Day 3 of 4" under tomorrow) had widened
    it. On the README's four-by-two at 10:20 the titles are whole in English ("Lunch with Gi…"
    and "Swimming les…" before) and Italian lists five events where it listed three.

## Consequences

- Nothing of Tempo runs for the widgets while nothing changes; a calendar change reaches a card
  within the job's batching, a finished event leaves it when the phone is next awake after its end.
- The merged manifest gains no permission: the alarm and the job need none beyond WorkManager's
  own, already there.
- The battery check of §9.6 and the launcher's real grants are the owner's, on the phone:
  `docs/device-checks/widget-refresh.md`.
