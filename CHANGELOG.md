# Changelog

All notable changes to Tempo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

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
