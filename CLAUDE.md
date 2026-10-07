# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**Tempo** ("time") is a private, battery-friendly Android clock and agenda (Kotlin / Jetpack
Compose Material 3, Glance widget). It shows the time, the date and the day's events in one calm
view, in the app and on a home-screen widget that never scrolls. It **reads** the phone's own
Calendar Provider (every account the phone syncs) with `READ_CALENDAR` alone, and hands every
change (view, create, edit) to the reader's calendar app through Android's intents. No account,
no ads, no internet permission at all. It belongs to a small family of single-purpose apps by
callbackdev, with **Chiaro** (weather), **Passo** (step counter) and **Saldo** (finance): same
developer identity, same build and release setup, and the same visual language. Its build and
structure are Passo's, carried over.

Source of truth:

- `VISION.md`: the product. Principles, v1.0 scope, non-goals, the corrections made to the
  owner's concept, key decisions, success criteria, **the open questions**, glossary.
- `PLANNING.md`: the architecture, the calendar engine (§4), the data model (§5), the derived
  views (§6), the widget (§7), battery rules (§9), permissions (§10), and the phased plan with
  checkable steps (§11). **Keep it updated as work progresses**: tick the boxes, and record
  every decision and deviation with its reason in §15 (and as an ADR in `docs/adr/` when it
  is architectural).

Work one phase at a time, starting from the phase's section in PLANNING.md §11. The owner
answered VISION.md's questions on 7 Oct 2026 (VISION, "The owner's answers"; PLANNING §15); a
new question is written in PLANNING §15's Open list and asked before it is built.

## Build and commands

Stack: Kotlin 2.4 (compiled by AGP 9's built-in Kotlin), Compose Material 3, Hilt, DataStore,
Glance, WorkManager (Glance's, pinned); Gradle 9.8 / AGP 9.4, version catalog in
`gradle/libs.versions.toml`, convention plugins in `build-logic/`. **No Room**: Tempo stores no
event. Package/applicationId: `com.callbackdev.tempo`. minSdk 34, compile/targetSdk 37 (Passo's). Java 21.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test`
- One module: `./gradlew :core:domain:test`; one class: `--tests "com.callbackdev.tempo.core.domain.clock.ClockTest"`
- Lint (every module, via `checkDependencies`): `./gradlew :app:lintDebug`
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`; rules in `.editorconfig`)
- Forbidden-permission check on the merged manifests: `./gradlew :app:checkForbiddenPermissions`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- Launcher icon: `python3 tools/draw_launcher_icon.py`
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- **Maven Central answers HTTP 429** (Too Many Requests; it happens in the Claude Code cloud
  sandbox): add `--init-script gradle/google-maven-mirror.init.gradle.kts` to any command. It
  puts Google's mirror of Maven Central first, and points Robolectric's own `android-all`
  download at it, for that run only; nothing in the build changes. In the sandbox the SDK is at
  `/opt/android-sdk` (`local.properties`, not committed).

**Convention plugins** (`build-logic/convention`): `tempo.android.application`,
`tempo.android.library`, `tempo.android.compose`, `tempo.android.feature`, `tempo.android.hilt`,
`tempo.jvm.library`. SDK levels and the Java version live in `TempoSdk` / `Tempo.kt`; a module's
own build file only says what is specific to it. The forbidden-permission list is
`ForbiddenPermissionPatterns` in `ForbiddenPermissions.kt`. A Room convention returns (from
Passo's `AndroidRoomConventionPlugin`) only with the first table, and with the owner's approval.

**Version**: `tempo.versionName` in `gradle.properties` is the only place it is written;
`versionCode` is derived (major * 10000 + minor * 100 + patch). `release.yml` refuses a tag
that does not match it.

## Modules (PLANNING.md §2)

| Module | Kind | Holds |
|---|---|---|
| `:core:model` | pure Kotlin/JVM | data classes shared by everything: `EventInstance`, `CalendarInfo`, the provider's codes as enums, `Agenda` (`AgendaDay`, `AllDayEntry`, `TimedEntry`, `FreeGap`), `CalendarPermission`, the settings enums (`ClockFormat`, `DateStyle`, appearance) |
| `:core:domain` | pure Kotlin/JVM | the calendar engine: `AgendaBuilder`, `AgendaFilter`, `allDayDates()` (UTC), `FreeTime`, `NextBoundary`, `AgendaWindow`, `calendarPermission()`; the reader's calendars (`CalendarChoices`, `CalendarGroups`) and their colour as a mark (`markColor`, 3:1); the clock helpers (`uses24Hour`, `untilNextMinute`, `nextHalfHour`); Today's views (`DaySentence`, `DaySummary`, `Timeline`, `AlarmDay`); later `AgendaFit` |
| `:core:data` | Android library | DataStore: `SettingsRepository` (`UserSettings`, only what differs from a default stored), later the widget looks |
| `:core:calendar` | Android library | the Calendar Provider's Android half: `CalendarSource` (Instances, Calendars → `CalendarRead`), `CalendarChanges` (observer, debounced), `CalendarIntents` (view, insert, open a day, `canOpen`), `CalendarAccess` (the permission); `READ_CALENDAR` and the `<queries>` in its manifest. Tested against `FakeCalendarProvider` under Robolectric |
| `:core:designsystem` | Android library | M3 theme (Chiaro's dresses through Passo), typography, shapes, motion, page gutter, the widget card colours, `TempoIcons`; Passo's settings rows (`SettingsRows.kt`) and `StatusCard`, `CalendarPermissionCard`, `DateTimeText` (times and dates from the locale's patterns), `AgendaText` (the agenda's words, for Today and the widgets), `CalendarDot`, `CalendarBar` |
| `:core:testing` | Android library, test-only | `assertAccessible()` (labels, 48dp touch targets) and `walkPage()`, shared by the UI tests as `testImplementation` |
| `:feature:*` | Android library | `today` (the clock, the sentence, the agenda, `TodayViewModel`'s ticker and observer), `settings` (formats, agenda, calendars, appearance, about), `onboarding` (welcome, the permission); `guide` joins in Phase 5 |
| `:widget` | Android library | the two Glance cards («Agenda», «In words»), their settings screen, the refresh (content-URI work, boundary alarm, receivers) |
| `:app` | application | `Application`, `MainActivity` (the reader's appearance, the shortcut's new event), the shell (`TempoRoot`: onboarding, then Navigation 3), `NewEventShortcut`, DI entry points; wires everything |

Rules: `:core:model` and `:core:domain` stay pure Kotlin/JVM; if a class there needs a `Context`
or a `Resources`, it is in the wrong module. Feature modules depend on `core:*`, never on each
other. All business logic lives in `:core:domain`, with unit tests. A module joins the build in
the phase that brings its first code.

## Invariants (non-negotiable)

- **Never add a forbidden permission** (PLANNING.md §10): `INTERNET`, any `ACCESS_*LOCATION`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`,
  `WRITE_CALENDAR`, `READ_CONTACTS`, `WRITE_CONTACTS`, `HIGH_SAMPLING_RATE_SENSORS`,
  `BODY_SENSORS*`. The build fails on the merged manifest; a library that adds one is removed or
  patched with `tools:node="remove"`.
- **Read-only.** Tempo never writes the Calendar Provider. Viewing, creating and editing go to the
  reader's calendar app through intents (`CalendarIntents`, PLANNING.md §4.7), and a button whose
  intent nothing can take is not drawn.
- **The Calendar Provider is the only source of events.** No account, no sync, no ICS or CalDAV
  of Tempo's own, no Google APIs. No event is copied into Tempo's storage.
- **All-day events are read in UTC** (PLANNING.md §4.2). Never place one with the phone's zone.
- **Don't change the calendar engine without its tests** for every edge case in PLANNING.md §4.6.
- **Battery first** (PLANNING.md §9): no service of any kind, no polling, no timers while the
  screen is off, no wakelocks of the app's own, no exact or wake-up alarms, no periodic workers.
  The minute ticker and the calendar observer live in a visible page's lifecycle. The widget's
  clock is the system's `TextClock`, never an app repaint per minute; the widget's list changes
  on the calendar's content-URI trigger, a non-wakeup inexact alarm at the next boundary, and the
  exempt time broadcasts. Nothing else.
- **The widget never scrolls**: a Glance `Column`, never `LazyColumn`; what does not fit is
  counted. Glance drops the eleventh child of a container silently: count them.
- **Calendar colours are data, not roles**: they mark an event, they never colour its text or
  its ground (PLANNING.md §6).
- **Backup is an allowlist** (`app/src/main/res/xml/data_extraction_rules.xml`): the settings
  file, nothing else. Android sends the copy, so the no-`INTERNET` rule stands; say "Tempo sends
  nothing", never "nothing leaves the phone".
- **Every user-facing string is a resource, in English and Italian** (`values/`,
  `values-it/`), with plurals where counts appear. The app language follows the system
  per-app language picker (`locales_config.xml`). Dates and times are formatted with the
  locale's own patterns (`DateFormat.getBestDateTimePattern`), never a hard-coded order.
- **No new third-party dependency without approval**, and its license must be
  GPL-3.0-compatible (Apache-2.0, MIT, BSD).

## Code style

Kotlin official style (ktlint `intellij_idea` via Spotless), no `!!`, `Flow` over `LiveData`,
immutable UI state, MVVM with unidirectional data flow. Code and documentation in English.
Comments explain *why*, not what.

## Design

Tempo keeps the same visual language as Chiaro and Passo, for the app screens and for the widget,
so the family reads as one (`docs/adr/0002-design-language.md`). `:core:designsystem` holds it as
roles (color, type, shape), never as hexes inside a composable: Chiaro's two generated dresses
(`theme/Scheme.kt`, copied through Passo, never hand-edited), Google Sans / Inter / system type,
Chiaro's shapes and springs, and every animation collapses to a fade under reduced motion. Its
principles hold here too: one sentence before any number, every number with the line that says
what it means, no dead tab and no switch for a feature that has not shipped, a section with
nothing to say is not drawn. Icons are `TempoIcons` (Passo's drawings), drawn in code. The
launcher icon (the family's ring in slate, with an amber clock face at the lower left) is written
by `tools/draw_launcher_icon.py`: change the script and re-run it, never the two XML layers or the
welcome page's `ic_app_mark.xml`. The Compose UI tests write screenshots to each module's `build/screenshots`:
look at them after changing a screen.

**Accessibility and foldables** (Passo's ADR 0012, from the start): a screen's tests call
`assertAccessible()` on what they draw, and walk the page at twice the text size and on an open
foldable (`walkPage()`); text inks are pinned at 4.5:1 by `ContrastTest`. A page leaves
`pageGutter()` on each side (in a list's content padding, never by narrowing the list), so an
open foldable shows a centred column and a phone upright is untouched. No tablet layout.

**Widgets** (PLANNING.md §7): the family's pair, «Agenda» and «In words», on the family's card (Passo's ADR 0005, from Chiaro): its corner, insets,
grounds, Chiaro's six colours (`WidgetPalette.kt`), opacity and ink rule. The forms' arithmetic
is pure (`AgendaFit`) and pinned by tests at the family's reference grants; a gallery test draws
every form to `widget/build/screenshots`: look at them after changing the card.

**README screenshots** (`docs/screenshots/`, shown in the root `README.md`): drawn by
`ReadmeScreenshots` test classes from realistic sample days, in English, and only on request:
`./gradlew test -PupdateScreenshots` (plus the mirror init script in the sandbox). The family's
standing rules: **regenerate them** whenever a change alters what an existing one shows, and look
at them before committing; **add one** when a phase brings something worth showing, with its
caption in the README's table; keep the set small.

## Signing and CI

- **Debug signing**: `keystore/debug.keystore` is intentionally committed (alias `tempo-debug`,
  passwords `android`) so debug APKs from CI and any machine share one signature. Do not
  regenerate it. Debug builds carry `applicationIdSuffix ".debug"`. Debug builds and the
  debug-signed release carry a crash page (`app/src/crashpage`): an uncaught exception shows its
  stack trace, to copy or share, so a test device can report a crash without adb (the minified
  one's trace is retraced with that run's `mapping.txt`). Never in a release signed with the
  release key.
- **Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig is
  created only when the four `TEMPO_KEYSTORE*` / `TEMPO_KEY_*` properties are all set (from
  `~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). Without
  them the release build is unsigned; `-PsignReleaseWithDebugKey` signs it with the debug key
  for testing only. The release key does not exist yet: the owner creates it before the first
  release (PLANNING.md §11 Phase 0, `keystore/README.md`).
- **Release key in CI**: the four repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD` (the same names as the sister apps'); `release.yml` decodes the
  keystore into the runner's temp folder and fails by name if one is missing. Never commit
  the real key.
- **CI** (`.github/workflows/android-ci.yml`, every push and PR): formatting, forbidden
  permissions, unit tests and lint run *before* the APKs; a red suite must never produce an
  installable artifact. **Release** (`release.yml`, on `v*` tags): the same gates, then the
  signed APK, its SHA-256 and the R8 mapping on a GitHub Release, with the body taken from
  the tag's `CHANGELOG.md` section (write it before tagging).

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence rather
than swapping in a hyphen: use a colon when the clause explains, a full stop when the thoughts
are separate, parentheses for an aside. Same house style as Chiaro and Passo, deliberately
scoped to that one file: every other file keeps normal punctuation.
