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
   and `WordsFit` (four forms and the order the hierarchy spends height). The widget measures,
   the domain decides, and the tests pin the decisions at the reference grants; `WidgetFitTest`
   draws every form 5% wider than measured, in both languages and three text sizes, and finds
   nothing cut but an event's title on its one line.

4. **A card says times, never minutes from now.** It is redrawn at the agenda's boundaries, not
   each minute, so "in 40 minutes" would be wrong a minute later: «In words» says "Until 11:00",
   "15:00", "Free until then; one more after it.", and Today keeps its minutes.

5. **Pushed, never polled** (PLANNING.md §9): a one-shot WorkManager job with a content-URI
   trigger on the Calendar Provider (quiet for 3 s, at most 20 s after a change), re-armed by
   itself, appended so it never cancels the run that arms it; one `AlarmManager.set(RTC)` at the
   next boundary (`NextBoundary`), inexact and non-wakeup, replaced by each render; the exempt
   broadcasts (time set, zone, language, package replaced) from the manifest; Tempo's settings
   changing, and the reader leaving the app. The last card removed disarms the job and the alarm.
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

## Consequences

- Nothing of Tempo runs for the widgets while nothing changes; a calendar change reaches a card
  within the job's batching, a finished event leaves it when the phone is next awake after its end.
- The merged manifest gains no permission: the alarm and the job need none beyond WorkManager's
  own, already there.
- The battery check of §9.6 and the launcher's real grants are the owner's, on the phone:
  `docs/device-checks/widget-refresh.md`.
