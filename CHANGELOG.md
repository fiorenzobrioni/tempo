# Changelog

All notable changes to Tempo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- The foundations: the build (Passo's, carried over), the family's design system, the launcher
  icon, continuous integration with the permission check, and a first screen with the time and
  the date. Nothing to install yet.
- The calendar engine: Tempo reads the phone's calendars (every account it syncs) for the week
  ahead, places all-day events on their own day in every time zone, follows events across
  midnight and daylight saving, leaves out cancelled events and, unless you want them, declined
  invitations and the calendars you hide, and finds the free time between your events. Not on
  screen yet: Today shows it from Phase 3.
- Settings: the time (the phone's, 24-hour or 12-hour) and the date (long, short or numeric),
  the days ahead (a week by default), all-day events, declined invitations, the next alarm, which
  calendars to show (by account, with their colours), and the family's appearance (light or dark,
  two palettes, three typefaces, wallpaper colours), with a live preview. Today's clock already
  follows the formats and the appearance.
- Today: the time large, the date, your next alarm, and the day in one sentence ("Dentist in 40
  minutes, then 2 more today"); the all-day events; the day's events on a timeline, with the
  morning folded away, a line for now, the event under way and how long is left, and the free
  hours between them, each one a touch away from a new event; tomorrow in full and the rest of
  the week in a line an event. Touch an event to open it in your calendar app, the date to open
  the day, the button to create one. Live while you look; nothing runs once you leave.
- The first run: what Tempo is, the one permission it asks for, with "Not now", and the widgets.
- "New event" on the launcher icon's long press.
- The widgets, the family's pair on the family's card. «Agenda»: the time and the date over the
  rest of your day, as many events as the card holds whole and how many more, the days ahead in
  the room today leaves, laid out for every size from one cell up. «In words»: a clock face with
  what comes next drawn on its ring, the hour hand walking through it by itself, and the day in a
  few words beside it. Their clock and date are drawn by Android itself, right to the
  minute, at no cost; their events change when your calendar does or an event starts or ends,
  never on a timer. Each card has its own settings: the time and the date (shown or not, in its
  own format), what a touch opens (the time: Tempo, your calendar at today, or your clock app; an
  event: your calendar app or Tempo), the background and its opacity, all-day events and the
  days ahead. Add them from the first run or Settings.
