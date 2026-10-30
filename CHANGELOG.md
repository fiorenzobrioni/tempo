# Changelog

All notable changes to Tempo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- **Invitations you have not answered** say so on Today ("Not answered"), as "Maybe" already did.
- **The alarm opens your clock app.** A touch on the next alarm, under the time on Today,
  opens the Clock app, where alarms are set. Tempo still only reads it.
- **The dial in your calendars' colours.** The events on Today's clock face wear their
  calendar's colour, as in the list under it; the next one stays in the accent.

### Changed

- **One "Free" row on Today.** Only your next free hour or more is a row, so more of the day
  and tomorrow's first events fit on the first screen; the sentence still says how long you
  are free.
- **Rounder chips.** Today's all-day events are fully round, and the first run's widget cards
  match the day cards on Today.
- **Widgets: one size for a list.** On «Agenda» an event's title, its time, the headings and
  the "more today" line are one size, and the note on «In words» matches it, the same sizes
  the family's other cards use.
- **Widgets wear your launcher's corner**, so they match the widgets beside them.
- **Times line up.** Today's columns of times use figures of one width, so the times stand
  under each other and "40 min left" no longer shifts as it counts down; the day's sentence
  breaks into even lines, and a time never parts from its AM or PM.

### Fixed

- **The dial on the nights the clocks change.** An event early the next morning could sit
  under the hand and look like now, and an event across the change drew an arc an hour too
  long or too short. The dial now counts the hours the clock on the wall shows.
- **Only the calendars on this phone.** An account also lists calendars it does not sync here
  (a colleague's, a country's holidays you ticked off): Settings no longer offers switches for
  them, and they no longer count as calendars shown.
- **Live after you allow the calendar.** Allowing it from Today or Settings now keeps the
  page live at once: a change made meanwhile shows without leaving the app first.
- **A new event from "Free" now** starts at the next half hour, and ends with the free time if
  that comes first. It used to start at the very minute, seconds and all.
- **An end on another day says its day**: "Night train until tomorrow at 6:30", "Ends
  Saturday", on Today and on the widgets, where a bare time read as today's.
- **The launcher's "New event"** follows the app's language when you change it.
- **«In words» shows the next event on its dial** as soon as it comes within the dial's twelve
  hours, rather than at midnight; and on one cell its dial stays when the calendar cannot be
  read, as «Agenda»'s clock does.

## [1.0.1] - 2026-10-10

A small fix to Today's words. Installs over 1.0.0 and keeps your
settings; signed with the same key (fingerprint in the
[README](https://github.com/fiorenzobrioni/tempo#install)).

### Changed

- **A run of empty days says it is a run.** Its card read "Tomorrow – Wednesday 14 October",
  as if tomorrow were Wednesday; it now says "From tomorrow to Wednesday 14 October", and
  "4 days with nothing planned." under it.

## [1.0.0] - 2026-10-10

**The first release.** Tempo shows the time, the date and your day in one calm view, in the app
and on two home-screen widgets that never scroll. It reads the calendars your phone already has
and hands every change to your calendar app. No account, no ads, no tracking, and no permission
to use the internet at all.

Android 14 (API 34) or newer. Check the download with the `.sha256` file beside the APK, and the
signing certificate against the fingerprint in the
[README](https://github.com/fiorenzobrioni/tempo#install).

### What is in it

- **Today**: the time large beside a clock face with the next twelve hours' events on its ring,
  the date, your next alarm, and one sentence on the day.
- **The agenda**: all-day events, the day's events on a timeline with a line for now and the
  free hours between them, then the rest of the week, a card a day.
- **Your calendar app does the writing**: touch an event to open it there; the date, the
  calendar button and a day's name open that day; the new-event button and a free hour open a
  new event with its start already set.
- **Two widgets**, «Agenda» and «In words», laid out for every size from one cell up, each with
  its own settings. Their clock is drawn by Android itself, right to the minute.
- **New event** from a long press on Tempo's icon.
- **Your calendars, your choice**: every account's calendars with their colours, each shown or
  hidden; declined invitations kept out.
- **The guide**, light and dark themes, two palettes, three typefaces.
- **For every reader**: TalkBack reads each event as one sentence, text up to twice its size,
  foldables.
- **English and Italian**, through the system per-app language picker.

### Private and light

- No `INTERNET` permission: Tempo cannot send anything. One permission, to read your calendar,
  which Tempo never writes.
- The phone's own calendar is the only source: no account, no sync of its own, no Google
  services.
- No service, no polling, nothing on a timer with the screen off: a widget changes when your
  calendar does, or when an event starts or ends.

The full development record is in
[docs/CHANGELOG-1.0.0.md](https://github.com/fiorenzobrioni/tempo/blob/main/docs/CHANGELOG-1.0.0.md).
