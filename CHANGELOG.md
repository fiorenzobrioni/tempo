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
