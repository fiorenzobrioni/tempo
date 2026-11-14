# Changelog

All notable changes to Tempo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Changed

- **«In words» looks like its sisters.** One cell is the clock face alone; the face's ring is
  a little heavier; the title is larger on a row, and the day's note is the size of the time,
  stronger, on two lines at most.

### Added

- **«In words», the event alone.** Turn off «The day’s note» in the widget’s settings and a widget
  two rows high or more says only what comes next and when, and its clock face takes the room.

### Changed

- **The first run's words.** Its buttons read «Get started», «Continue» and «Later», and its
  battery promise says when a widget redraws: when your calendar changes or an event starts or ends.

- **Today, quieter.** The time is lighter and its clock face finer, and the face is there only
  while today has something left to show. The sentence comes first, with what is true now in
  bold; the alarm is under it. The event under way is the only filled card on the page, so "now"
  is said once; the days ahead and the all-day events lost their grounds. On the 12-hour clock
  "AM" and "PM" are set small. The new-event button is its icon alone.

- **The clock face, honest early in the day.** An event that goes on past the face's twelve
  hours fades out at that end, instead of seeming to end there. On Today the face stays while
  today has an event left, its ring empty in the small hours until the afternoon comes onto it.

- **A new icon**: the day's page with a clock in front of it, on coral. The «New event» shortcut
  follows it, in the same shape as your launcher's icons.

- **«In words» keeps one dial.** At every size the clock face keeps one size all day, with
  events or without. On a widget two cells wide and three rows high it is larger, with the date
  on its own line over the event.
- **The clock face shows today only.** Today's clock face and the «In words» widget's carry what
  is left of today. A later day's events are in the list and in the words, no longer on the
  face, where tomorrow morning's read as tonight's hours and were cut short.

## [1.1.0] - 2026-11-02

**A second clock, your events' own time zones, two weeks ahead, and Today on a tablet**, with
widgets that make more of their room. Installs over 1.0.1 and keeps your settings; signed with
the same key (fingerprint in the [README](https://github.com/fiorenzobrioni/tempo#install)).

### Added

- **Invitations you have not answered** say so on Today ("Not answered"), as "Maybe" already did.
- **The alarm opens your clock app.** A touch on the next alarm, under the time on Today,
  opens the Clock app, where alarms are set. Tempo still only reads it.
- **The dial in your calendars' colours.** The events on Today's clock face wear their
  calendar's colour, as in the list under it; the next one stays in the accent.
- **The rest of the day on «Agenda».** Under the date on the wide widget, how many events are
  left today and until when: "4 left today, until 7:30 PM".
- **A second clock.** Choose a city in Settings, under Clock, and its time stands under the
  alarm on Today: "New York 4:20 AM", with "tomorrow" or "yesterday" when its date is not yours.
  Each widget can show it too, by its date, from the widget's own settings.
- **An event's own time zone.** An event written where the clock reads another time says when
  it starts there too: "9:00 AM in Lisbon".
- **Two weeks ahead.** Days ahead, in Settings, now reaches two weeks; the days with nothing
  planned are still said once, so a quiet fortnight costs a card or two.
- **A tip for the first day.** Under the clock, how to read the dial, with the way to the guide;
  "Got it" puts it away for good.
- **Your new event, found for you.** Back from your calendar app with a new event, Today scrolls
  to it and its row glows for a moment.
- **Today on a tablet.** On a large screen (a tablet, or a foldable open wide) Today stands in two
  panes: the time, a larger dial and the day's sentence on one side, the agenda beside them, each
  scrolling on its own. Phones are unchanged.
- **Reset a widget.** "Reset this widget", at the bottom of a widget's settings, puts the card
  back as it was when you placed it.

### Changed

- **One "Free" row on Today.** Only your next free hour or more is a row, so more of the day
  and tomorrow's first events fit on the first screen; the sentence still says how long you
  are free.
- **Rounder chips.** Today's all-day events are fully round, and the first run's widget cards
  match the day cards on Today.
- **Widgets: one size for a list.** On «Agenda» an event's title, its time, the headings and
  the "more today" line are one size, and the note on «In words» matches it.
- **Widgets wear your launcher's corner**, so they match the widgets beside them.
- **A widget's settings in a clearer order**: under the card, its background first, then the
  time and the date, what the card shows, and what a touch opens.
- **More of the day on «Agenda».** The event under way says "→ 11:00" where "Until 11:00"
  would squeeze every title beside it, and in Italian the all-day line spans the list: on the
  default size the rest of the day fits with its titles whole, five events where Italian
  showed three.
- **«In words» uses its room.** On a widget three rows high the clock face grows with the
  widget, and under what comes next the next one or two events are listed with their times
  ("1:00 PM  Lunch with Giulia"), not only their times.
- **Widget messages in the middle.** "Tempo cannot read your calendar" and the other messages
  stand centred in the widget, not at its top over an empty card.
- **The widget's size in view.** In a widget's settings the size picked is scrolled into
  view; the default one was past the screen's edge, as if none were picked.
- **Times line up.** Today's columns of times use figures of one width, so the times stand
  under each other and "40 min left" no longer shifts as it counts down; the day's sentence
  breaks into even lines, and a time never parts from its AM or PM.
- **A quieter first run.** Three pages, each moved on by its own button, with no picture and no
  progress bar; once you allow the calendar, its page moves on by itself.
- **The icon's clock hands end round**, where their square ends read as a cut.

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
- **The next alarm is yours.** Some calendar apps keep their reminders as alarms, and Today
  showed one as "Alarm tomorrow at 10:00" with no alarm set. Only an alarm set in a clock app
  is shown now.
- **«In words» shows the next event on its dial** as soon as it comes within the dial's twelve
  hours, rather than at midnight; and on one cell its dial stays when the calendar cannot be
  read, as «Agenda»'s clock does.
- **Animations off means off.** With animations removed in Android's accessibility settings,
  the bar of the event under way on Today no longer slides.

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
