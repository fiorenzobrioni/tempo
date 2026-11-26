# Tempo — Technical Planning

> Companion to [VISION.md](VISION.md). This document defines the architecture, the design of the calendar engine, the widget, and the delivery phases.
> It is written to be read by humans and by Claude Code.
>
> Keep the phase checklists up to date: tick the boxes as work is merged, and record every decision and deviation with its reason in §15's log. How the log is written and archived is in How it is made, below.

---

## How it is made (2 Nov 2026)

- **This file** is the living plan: the architecture and the rules the code keeps (§1 to §10, §12 to §14: living, changed in place with the code, never archived), the phases still open (§11), what is open (§15, Open), and the log of the current period (§15, after Open). New decisions are written **at the end of the log**, one `###` section per intervention, with who asked and the date (and the step, where there is one), as always.
- **`docs/planning/NN-<period>.md`** are the closed periods of the log, from Phase 0 on, with the phases they closed, **frozen**: they are not edited. A decision that changes an old one is written here, naming the one it changes (the file, and the title or the entry's date).
- **`docs/planning/README.md`** is the index of every `##` and `###` heading of the archive, and of every dated entry of the log kept as a list until archive 01, with its line: a reference by name or by date in the code ("PLANNING.md §11 Phase 3", "§15, 9 Oct 2026") that is not in this file is found there. It is written by `python3 tools/planning_index.py`, never by hand, and changes only when archiving.
- **Archive** when the log passes ~80 KB (`sed -n '/^### Log since/,$p' PLANNING.md | wc -c`), or at a release, whichever first. A size and not a count of lines: this file writes a paragraph to a line. The log becomes the next archive file (the same header as the previous ones), and the phases of §11 that closed go with it (a closed phase's open box carried to Open); here an empty log stays with its start date, «Open» is reread, and the index script is run again.

## State

- **Released**: 1.1.0 (2 Nov 2026, `v1.1.0`). What came in after is in `CHANGELOG.md`'s `[Unreleased]` section.
- **Closed**: Phases 0 to 6 (the 1.0.0, 10 Oct 2026) and the 1.0.1 after them: their steps, their acceptance and their log are in `docs/planning/01-phases-0-8.md`. Phase 8, the 1.1.0 (30 Oct to 2 Nov 2026): its plan, its steps and the log from 2 Nov 2026 are in `docs/planning/02-phase-8.md`, the entries of its steps 8.0 to 8.7 in archive 01.
- **In progress**: Phase 10, Today's two grounds (§11; owner, 26 Nov 2026), on `claude/today-two-grounds`: steps 10.1 to 10.3 built, 10.4 to 10.6 open. Phase 9, the month and the time's door (§11; owner, 23 Nov 2026), is built and was checked on the owner's phone the same day (PR #35); it is in `CHANGELOG.md`'s `[Unreleased]`. Open, in §15: 8.4 («Join», on hold), 1.1's battery check on the phone, and two boxes carried from before.
- **Later, and optional**: Phase 7, Google Play (§11).

---

## 1. Tech stack

The family's stack, as Passo set it up (Passo ADR 0001), minus what Tempo has no use for.

| Concern | Choice |
|---|---|
| Language | Kotlin 2.4, compiled by AGP 9's built-in Kotlin, with the Compose compiler Gradle plugin |
| Build | Gradle 9.8 with Kotlin DSL, AGP 9.4; version catalog (`gradle/libs.versions.toml`); convention plugins in `build-logic/` |
| JDK | Java 21, `jvmTarget` 21, without a Gradle toolchain |
| SDK levels | `minSdk 34`, `targetSdk 37`, `compileSdk 37` (Passo's) |
| UI | Jetpack Compose, Material 3 in Chiaro's design language (its generated schemes, dynamic color on request), edge-to-edge |
| Widget | Jetpack Glance (`glance-appwidget` + `glance-material3`) 1.2.x, `SizeMode.Exact`, the system `TextClock` through `AndroidRemoteViews` |
| Architecture | MVVM with unidirectional data flow; coroutines and `Flow` |
| Dependency injection | Hilt |
| Calendar | The system Calendar Provider (`CalendarContract`) through the `ContentResolver`: no library |
| Persistence | DataStore (Preferences) for the settings and the widgets' looks. **No Room**: Tempo stores no event (§5) |
| Background | WorkManager (already brought by Glance, pinned as in Passo) for the calendar's content-URI trigger; `AlarmManager` inexact non-wakeup alarms for the boundaries (§7) |
| Navigation | Navigation 3, as Passo's shell |
| Time | `java.time`; Android's `DateFormat.getBestDateTimePattern` for the locale's own patterns |
| Testing | JUnit 4, Truth, Turbine, coroutines-test; Robolectric (the provider through a fake `ContentProvider`, receivers, Compose UI tests); Glance's unit test utilities |
| Quality | Android Lint (`checkDependencies`), ktlint through Spotless; Baseline Profiles later |
| CI | GitHub Actions: formatting, the forbidden-permission check on the merged manifest, unit tests and lint before the APKs. A release workflow on version tags builds the signed APK and publishes it to GitHub Releases |
| License | GPL-3.0. Every new dependency must have a GPL-3.0-compatible license (Apache-2.0, MIT, BSD) and the owner's approval |

Always pick the **latest stable** versions at setup time and record them in the version catalog. Avoid alpha versions unless a phase needs them and says why.

---

## 2. Project structure

```
tempo/
├── app/                    # Application, MainActivity, navigation, DI entry points
├── build-logic/            # Convention plugins (android-application, library, compose, feature, hilt, jvm)
├── core/
│   ├── model/              # Pure Kotlin data classes (no Android deps)
│   ├── domain/             # Pure Kotlin: the agenda, the day's sentence, the widget's fit, the next boundary
│   ├── data/               # DataStore: settings, widget looks; repositories exposing Flow
│   ├── calendar/           # The Calendar Provider's Android half: queries, change observer, intents
│   ├── designsystem/       # M3 theme, typography, shared components, icons
│   └── testing/            # test-only: the accessibility checks and the page walk the UI tests share
├── feature/
│   ├── today/              # the clock, the sentence, the agenda, the new-event button
│   ├── month/              # the month at a glance, the chosen day under it (Phase 9)
│   ├── settings/           # settings, the calendars, about, credits
│   ├── onboarding/         # welcome, the calendar permission, the widget
│   └── guide/              # the guide, in Chiaro's shape (Phase 5)
├── widget/                 # the Glance card(s), the config screen, the refresh (job, alarm, receivers)
├── tools/                  # the launcher icon script
├── docs/                   # ADRs, screenshots
├── VISION.md
├── PLANNING.md
└── CLAUDE.md
```

Rules:

- `core:model` and `core:domain` are pure Kotlin/JVM modules. All business logic lives there and is unit-tested without Android.
- Feature modules depend on `core:*`, never on each other.
- `app` wires everything together.
- A module joins the build in the phase that brings its first code, not before (`feature:guide` in Phase 5).

---

## 3. Architecture overview

```mermaid
flowchart LR
    subgraph PHONE["Android"]
        CP[("Calendar Provider<br/>(every synced account)")]
        CALAPP["The reader's calendar app"]
        AM["AlarmManager<br/>(inexact, non-wakeup)"]
        JS["JobScheduler via WorkManager<br/>(content-URI trigger)"]
        BCAST["Time set, time zone, locale,<br/>boot broadcasts (exempt)"]
        NEXTALARM["Next alarm clock"]
    end

    subgraph CAL[":core:calendar"]
        SRC["CalendarSource<br/>(Instances, Calendars)"]
        OBS["CalendarChanges<br/>(ContentObserver → Flow)"]
        INT["CalendarIntents<br/>(view, insert, open day)"]
    end

    subgraph DOMAIN[":core:domain (pure Kotlin)"]
        AG["AgendaBuilder → Agenda<br/>(days, all-day, timed, now)"]
        SENT["DaySentence"]
        FIT["AgendaFit<br/>(what fits a card)"]
        NEXT["NextBoundary"]
    end

    subgraph DATA[":core:data"]
        DS[("DataStore: settings,<br/>widget looks")]
        REPO["SettingsRepository<br/>WidgetLookRepository"]
    end

    UI["Compose screens (:feature:*)"]
    WIDGET["Glance card (:widget)"]

    CP --> SRC
    CP -.->|"change notification"| OBS
    CP -.->|"content-URI trigger"| JS
    SRC --> AG
    REPO --> AG
    AG --> SENT
    AG --> UI
    AG --> WIDGET
    FIT --> WIDGET
    AG --> NEXT
    NEXT -->|"arm the next one"| AM
    AM -->|"when the phone is next awake"| WIDGET
    JS --> WIDGET
    BCAST --> WIDGET
    OBS -->|"while a screen collects"| UI
    NEXTALARM --> UI
    UI --> INT
    WIDGET --> INT
    INT --> CALAPP
    CALAPP --> CP
    REPO <--> DS
```

**One way in, one way out.** Events come in from the Calendar Provider only, read by `CalendarSource`. Changes go out through the calendar app only, opened by `CalendarIntents`. Tempo writes nothing but its own settings.

**The read path is computed, not stored.** A screen or a card asks for the instances of a time window and the calendars' metadata, and `:core:domain` turns them into an `Agenda`: days, each with its all-day row and its timed events, where "now" falls, what is under way, the sentence. Nothing is cached in a table; the provider answers a day's query in milliseconds.

**Live while visible.** Today collects `CalendarChanges` (a `ContentObserver` on the provider, registered only while collected) and a minute ticker, both tied to the page's lifecycle. With the page gone, nothing of Tempo runs.

**The widget is pushed, never polled** (§7): the calendar's content-URI trigger, the next boundary's inexact alarm, and the exempt time broadcasts each ask the card to recompose. Its clock is the system's `TextClock` and needs none of it.

---

## 4. The calendar engine (core design)

The riskiest part of the app: wrong days, missing occurrences and stale cards are what make readers drop agenda widgets. Build it first, with its tests, before any screen.

### 4.1 Reading the provider

- **Instances**, not Events: `CalendarContract.Instances.CONTENT_URI` with the window `[start, end)` in epoch millis appended (`Instances.CONTENT_URI.buildUpon()`, `ContentUris.appendId(begin)`, `appendId(end)`). The provider expands recurrences and applies exceptions; Tempo never parses an `RRULE`.
- **Projection** (only what is shown or decided on): `EVENT_ID`, `BEGIN`, `END`, `START_DAY`, `END_DAY`, `START_MINUTE`, `END_MINUTE`, `ALL_DAY`, `TITLE`, `EVENT_LOCATION`, `CALENDAR_ID`, `DISPLAY_COLOR` (the event's colour, or its calendar's), `STATUS`, `SELF_ATTENDEE_STATUS`, `EVENT_TIMEZONE`, `AVAILABILITY`, `IS_ORGANIZER` (so an invitation the reader has not answered is told from a meeting they set up, review of 30 Oct 2026).
- **Calendars**: `Calendars.CONTENT_URI` for `_ID`, `CALENDAR_DISPLAY_NAME`, `ACCOUNT_NAME`, `ACCOUNT_TYPE`, `CALENDAR_COLOR`, `VISIBLE`, `SYNC_EVENTS`, `IS_PRIMARY`. `VISIBLE = 0` (hidden in the calendar app) is hidden by default in Tempo too, and the reader's own choice in Tempo wins over it. `SYNC_EVENTS = 0` (a calendar the account lists but this phone does not sync: a colleague's, a country's holidays ticked off) is not on the phone: no switch in Settings, not counted, its events never shown, as the calendar apps list only the synced ones (`CalendarChoices.onPhone`, review of 30 Oct 2026).
- **The window**: from the start of today to the end of the horizon's last day, in the zone of the phone, widened by a day on each side so that all-day events (stored in UTC, §4.2) are never cut off by the window itself; the domain trims.
- **Selection in the domain, not in SQL**: hidden calendars, declined invitations and cancelled events are filtered by `AgendaBuilder`, in pure Kotlin, where they are tested. The query stays the same for every reader.
- **Threading**: queries run on `Dispatchers.IO`; a `SecurityException` (the permission revoked while the app runs) becomes a state, never a crash.

### 4.2 All-day events and time zones

- An all-day event is stored from **00:00 UTC** of its first day to 00:00 UTC of the day after its last (`END` exclusive). Its days are therefore read in **UTC** (`START_DAY`/`END_DAY` are Julian days computed by the provider; or `BEGIN`/`END` converted with `ZoneOffset.UTC`), never in the phone's zone. In the phone's zone, a one-day event at UTC−5 starts at 19:00 the day before.
- A timed event is placed by its `BEGIN`/`END` in the phone's **current** zone. `EVENT_TIMEZONE` is never used to place it; since step 8.5 it is said, as one note on Today's row, where it names a place and its offset at the event's start is not the phone's: "09:00 in Lisbon" ("09:00 ora di Lisbona"), the city as the locale's CLDR data writes it (`EventZone.differs`, `AgendaText.ownZone`). Never for an all-day event, for `UTC`, `GMT`, a bare offset or an `Etc/` zone, nor for a zone on the phone's clock at that moment (Paris beside Rome; London beside Lisbon, all year). The cards have no room for it.
- A time-zone change (travel) re-reads everything: the days move, the provider's own instances table is rebuilt by the provider itself.

### 4.3 Days, overlaps, multi-day events

- A timed event that crosses midnight belongs to every day it touches; on the second day it reads "until 01:30", on the first "from 23:00".
- A multi-day all-day event is in each day's all-day row with "day 2 of 5".
- Overlapping events are listed by start, then by end, then by title; the timeline never draws them side by side (one column, the phone's width).
- The event under way: `BEGIN <= now < END`. Past: `END <= now`. Next: the first with `BEGIN > now`.

### 4.4 What is not shown

- `STATUS = STATUS_CANCELED`: never.
- `SELF_ATTENDEE_STATUS = ATTENDEE_STATUS_DECLINED`: hidden unless the reader shows declined invitations.
- Calendars the reader hid in Tempo, or hidden in the calendar app and not shown in Tempo.
- Events with no title are shown as "(No title)", the calendar app's own convention.

### 4.5 Watching for changes

- **In the app**: `CalendarChanges` is a `callbackFlow` around a `ContentObserver` registered on `CalendarContract.CONTENT_URI` with descendants, unregistered when the collector stops. Changes are debounced (a sync writes many rows at once). Android refuses the observer without `READ_CALENDAR`, so the pages watch through `CalendarChanges.flow(restarts)`, started again at every reread: a permission granted from the page's card or the system's settings makes the page live at once (review of 30 Oct 2026).
- **For the widget**, two ways, the first fast and the second a safety net (9 Oct 2026, §15):
  - the provider's own `PROVIDER_CHANGED` broadcast on `content://com.android.calendar`, received in the manifest (`CalendarChangedReceiver`). It is not on Android 8's exemption list, but the AOSP provider sends it with `FLAG_RECEIVER_INCLUDE_BACKGROUND`, about a second after an app's edit and half a minute after a sync's, so a manifest receiver has it. The waiting trigger is replaced first, so the change is drawn once;
  - a WorkManager `OneTimeWorkRequest` with `Constraints.addContentUriTrigger(CalendarContract.CONTENT_URI, true)` (quiet for 3 s, at most 20 s late), tagged, one waiting at a time; each run arms the next *before* it repaints, so a change during the run is seen (content-URI triggers are one-shot by design). It costs nothing while nothing changes, and catches a maker's provider that does not send the broadcast.
  - Either stays until the cards are drawn (`TempoWidgets.repaintAll`, `WidgetRefresh.awaitDrawn`): Glance composes in a WorkManager worker started inside Tempo's process with no job bound, and a process left cached can be frozen with the card half drawn.

### 4.6 Edge cases (all need unit tests)

- An all-day event, one day and several days, in a phone zone west of UTC, east of UTC, and at UTC.
- A timed event crossing midnight; one crossing two midnights.
- A recurring event with an exception (one occurrence moved) and a deletion (one occurrence cancelled).
- The night daylight saving starts (the 02:00–03:00 hour does not exist) and ends (01:00–02:00 happens twice); an event in the repeated hour.
- The phone's zone changing between two reads (travel).
- A zero-length event (`BEGIN = END`): a moment, shown at its time.
- An event that started yesterday and ends tomorrow (timed): on today it is in the all-day row, "day 2 of 3"; its first date keeps it as "from 21:00", its last as "until 02:00" (decided in Phase 1, §15).
- Declined, tentative, cancelled; an event with no title; a calendar with no colour.
- Today empty, the horizon empty, every calendar hidden, no calendar at all.
- Midnight while the page is open: the day rolls over without a restart.
- The permission revoked while the app is open; granted from the system's settings while the app is in the background.

### 4.7 Handing over to the calendar app

`CalendarIntents` builds every intent, and says whether something can take it (`resolveActivity`, with `<queries>` in the manifest for Android 11+ package visibility):

| What | Intent |
|---|---|
| Open an event (one occurrence) | `ACTION_VIEW`, `ContentUris.withAppendedId(Events.CONTENT_URI, eventId)`, extras `EXTRA_EVENT_BEGIN_TIME` / `EXTRA_EVENT_END_TIME` = the instance's |
| New event | `ACTION_INSERT`, `Events.CONTENT_URI`, `EXTRA_EVENT_BEGIN_TIME` = the next half hour (or the free gap's start; from the month, the chosen date at the time of day the next half hour has now, `newEventOn`), `EXTRA_EVENT_END_TIME` = one hour later |
| Open the calendar at a day | `ACTION_VIEW`, `CalendarContract.CONTENT_URI/time/<millis>` |
| Edit an event | `ACTION_EDIT` on the event URI: only if Phase 1 measures it working on the owner's calendar apps (VISION, From the concept); otherwise `ACTION_VIEW` |

From the widget the same intents travel as `PendingIntent`s (immutable). A "No app can open this" state replaces a button that would do nothing.

---

## 5. Data model

Tempo has no database. The Calendar Provider is the only truth for events; Tempo keeps the reader's choices.

### `:core:model` (pure)

- `CalendarInfo(id, name, accountName, accountType, color, visibleInProvider, isPrimary, syncsEvents)`
- `EventInstance(eventId, calendarId, title, location, begin: Instant, end: Instant, allDay, color, status, selfStatus, availability, timeZone, isOrganizer)`, with `awaitsAnswer` (invited, and not by themselves): as stored; an all-day event's dates are derived in the domain (`allDayDates()`, UTC), never stored beside it
- `EventStatus`, `AttendeeStatus`, `Availability`, `CalendarPermission`
- `Agenda(now, today, days, underWay, next)`, `AgendaDay(date, allDay: List<AllDayEntry>, timed: List<TimedEntry>, free: List<FreeGap>)`, `AllDayEntry(event, dayNumber, dayCount)`, `TimedEntry(event, startsOnDate, endsOnDate, state)`, `FreeGap(start, end)`. The day's sentence is computed from an `AgendaDay` by `DaySentence` (Phase 3), not stored in it
- `UserSettings`, `WidgetLook` (below)

### DataStore (Preferences), file `settings`

| Key | Default | Meaning |
|---|---|---|
| `theme`, `palette`, `font`, `dynamic_color` | system, VIVID, GOOGLE_SANS, false | The family's appearance |
| `clock_format` | SYSTEM | `ClockFormat` |
| `date_style` | LONG | `DateStyle` |
| `horizon_days` | 7 (a week; owner, §15) | Days shown: 1, 2, 7 or 14 (two weeks, step 8.6) |
| `calendar_choices` | empty | The calendars the reader turned on or off in Tempo (a string set, one `CalendarChoice` per line, URL-encoded fields), each kept as `id` **and** `account_type/account_name/display name`, so a restore that renews the ids can still match them (`CalendarChoices`, §15) |
| `show_declined` | false | Declined invitations |
| `show_all_day` | true | All-day events |
| `show_next_alarm` | true | The next alarm line |
| `time_opens_clock` | true | Whether a touch on Today's time, its figures or its face, opens the phone's clock app (Phase 9; §15, 23 Nov 2026) |
| `second_zone` | none | The second clock's city, as a time-zone id ("America/New_York"; ICU's canonical name, "Asia/Calcutta", which `java.time` knows too). An id this phone's time-zone data does not know reads as none (step 8.5) |
| `onboarding_completed` | false | |
| `today_tip_seen` | false | Today's first tip ("How to read the dial") dismissed or followed to the guide (step 8.6) |
| `asked_calendar_permission` | false | Whether the permission was ever asked: what tells "askable" from "denied for good" (§15) |

### DataStore, file `widgets` (not backed up)

One `WidgetLook` per `appWidgetId`: show clock, clock format, show date, date style, header action, event action, card ground (light, dark, system, one of seven colours; terracotta by default), opacity, show all-day, show the days ahead, show the second clock (off by default, step 8.5), show «At a glance»'s note (on by default; off, the event alone: §15, 5 Nov 2026). Removed in `onDeleted`.

### Backup

`data_extraction_rules.xml`: `files/datastore/settings.preferences_pb` only (Passo's ADR 0007 rule: an allowlist, decided). The widgets' file stays out: widget ids are renewed by a restore.

---

## 6. Derived views (`core:domain`)

- **`AgendaBuilder`**: instances + calendars + settings + now + zone → `Agenda`. Applies §4.2–§4.4. Pure, the most tested class of the app.
- **`DaySentence`**: the day in one sentence, from the agenda alone: the count of what is left, the next event and when, the free time before it, the end of the day ("Nothing left today; tomorrow starts at 9"). Returns a structure (kind + values), never a string: the words are resources, in both languages, with plurals (`:feature:today` and `:widget` format it).
- **`FreeTime`**: the gaps between a date's busy timed events (overlaps merged; free-availability and declined events do not take the time; all-day ones neither), and on today the time from now to the next busy one. Never the evening after the last event or the morning before the first, which are not "free time" any more than the night is (§15). Every gap feeds the sentence; the first gap of an hour or more still ahead today is also a row of the timeline (owner, §15, 7 and 30 Oct 2026).
- **`NextBoundary`**: the next moment the agenda changes by itself: the nearest `BEGIN` or `END` after now, or the next midnight, whichever is first; with an «At a glance» card placed, also the moment one of today's events comes onto the dial's face (`DialArcs.comesOnTheFace`, review of 30 Oct 2026; today's only since §15, 5 Nov 2026). The widget arms its alarm on it.
- **`EventZone`** and **`SecondZone`** (step 8.5): whether an event's own zone reads another clock than the phone's at its start, and which zone the second clock shows (none where it would repeat the phone's own) and on which day there. The cities' names are the presentation's (`ZoneCities`, `:core:designsystem`, from ICU).
- **`MonthGrid`, `MonthPages`, `MonthMarks`** (Phase 9, ADR 0005): a month as whole weeks from the reader's first day; the months as pages and the dates to read for one (itself and the two beside it); and what each date says of itself in its cell, as marks and never titles: a dot for an event with a time (three at most, then a plus), a bar for an event of whole dates, on one lane across all its dates (two lanes, given out over the whole run of dates read). The dates themselves are `AgendaBuilder.days(from, to)`: the agenda's own, for any span.
- **`AgendaSections`**: the agenda's days as Today lays them out: today, tomorrow in full, the days after it compact, and every run of empty days folded into one row (§15, 9 Oct 2026).
- **`AgendaFit`** (pure arithmetic, as Passo's `GlanceLayout`): given a card's granted size in dp, the header's choice and the text sizes measured in the system face, how many event rows fit, which form the card takes, and what the "N more" line says. Pinned by tests at the family's reference grants (one row ≈ 85 dp tall, two ≈ 189; widths 2 cells ≈ 159 dp, 3 ≈ 250, 4 ≈ 340).
- **Calendar colours are data, not roles.** They are chosen in the calendar app with no thought for Tempo's grounds, so they mark an event (a dot, a bar at the start of its row, a quiet arc on Today's dial) and never colour its text or its ground. A mark keeps a 3:1 contrast with its ground by stepping its lightness when it must (`markColor`), and a mark drawn quieter fades only as far as that ratio allows (`markAlpha`): pure functions with their tests.

---

## 7. Widget (Glance)

Two cards in v1, the family's pair (owner, §15): «At a glance» / «A colpo d'occhio» (`WordsWidget`, the dial: the family's card with a picture, Chiaro's glyph card and Passo's ring card; «In words» / «In parole» until 15 Nov 2026, owner, §15) and «Agenda» (`AgendaWidget`, the concept's). «At a glance» is declared first and opens at 4×1, «Agenda» at 4×2; both from one cell, with no maximum; the same settings screen, the same refresh. The family's card (Passo's ADR 0005, from Chiaro): its corner, insets, grounds, the family's seven colours (the app's terracotta by default, owner, 15 Nov 2026), opacity, and Chiaro's ink rule.

### Configuration

- `SizeMode.Exact`: every form is read off the size the launcher really granted. Glance does the work the concept gave to `onAppWidgetOptionsChanged`, and recomposes on every resize.
- Default placement «At a glance» 4×1 (`targetCellHeight` 1, `minHeight` 40 dp; owner, 15 Nov 2026), «Agenda» 4×2 (`targetCellWidth` 4, `targetCellHeight` 2), minimum one cell, `resizeMode="horizontal|vertical"`, **no maximum** (Passo's ADR 0005: Launcher3 turns a dp maximum into the smallest cell count across its grids). Each form has a layout for any grant (`AgendaFit`, `WordsFit`, `:core:domain`; ADR 0003).
- `updatePeriodMillis = 0`. `configure` is the per-widget settings screen, `reconfigurable|configuration_optional`.
- `widgetCategory="home_screen"` in v1; `keyguard` is a later idea.

| Form | When | Content |
|---|---|---|
| CLOCK | one cell, or a row too narrow for a list beside the clock (two cells) | The time, as large as the cell holds; the date under it where it still reads (else none) |
| LINE | one row, three cells and up | The time and, under it at 16 sp (Passo's facts, «At a glance»'s line on top), the date (shortened before it is shrunk) on the leading side; the next events beside them, and "N more" |
| AGENDA | two rows and up | The header (clock and date at 16 sp, each optional; the second clock at the far edge of the date's line where both fit whole, or in the date's place under a clock without one), then the events that fit, then "N more today" |
| SIDE | two rows and up, four cells, where a header on top would leave fewer than five rows (the default 4×2) | The clock and the date (over two even lines if need be: "Wednesday, / October 7") in a column on the leading side, the second clock under the date where it is on and fits ("New York 04:20"), the rest of the day in a few words under them ("4 left today", "until 19:30"); the events beside them |

With the clock and the date both hidden, the card is its list alone, on any grant. A date's all-day events share one row ("All day · Design Week, Anna's birthday"), so two holidays never push out what happens next. A row sets its time as a range where the title keeps 120 dp, its start alone where it keeps 72, else over the title. The time column is set by the rows the card shows, the event under way as "→ 11:00" where "Until 11:00" would widen it for every title, and an all-day label more than an em wider than the timed rows' times (Italian's "Tutto il giorno") stands over its titles across the list instead (`AgendaFit.timeColumn`, ADR 0003 decision 13).

**The rest of the day in a few words** (8.2, ADR 0003 decision 12): under the date in «Agenda»'s side column, at the label step in the secondary ink, how many timed events are left today, the one under way among them, and when the last one ends ("4 left today" / "until 19:30", "Ancora 4 oggi" / "fino alle 19:30"; `CardSummary`), each line where it fits whole; nothing once today's timed events are over. Counts and clock times the card is redrawn at anyway, so they never go stale. It stands where the next alarm was planned: the alarm's change did not reach a card in the background reliably on the owner's phone, so the alarm stays on Today alone (§15, 30 Oct 2026, Phase 8.2).

| «At a glance» form | When | Content |
|---|---|---|
| CELL | one cell wide | The dial, the family's circle (the cell's shorter side less 8 dp each side, as Passo's ring; §15, 4 Nov 2026); under it the focus's time ("11:00") where the cell is taller than wide. Without the dial, that time bold in its place, the title under it |
| ROW | one row | Passo's row: the dial, Passo's ring (44 to 56 dp, keeping the words beside it 84 dp; §15, 4 Nov 2026), drawn where the words keep 72 dp; the title (20 sp, Chiaro's sentence) over when ("Until 11:00", "15:00 – 15:45", "Tomorrow · 9:00 – 9:30", 16 sp, Passo's facts); the date at the far edge over two lines ("Wednesday / 7 October") where the words keep 120 dp. A narrow row sets the words at 16 and 14 sp, a long title over two lines |
| TALL | two rows and up | Passo's tall card: the dial in the top trailing corner, the family's circle for the card's size, whatever the day says (the height left over a budgeted band of words, up to 45% of the card's inner height and width, never held under 104 dp; Passo's ring at the same size: §15, 4 Nov 2026, "at every size") with the date beside it, or on its own line over the title where the dial leaves it no column (two cells wide, three rows high); with «The day's note» off in the card's settings, the event alone (its title on one line, its time) under a dial the size of Passo's ring without its sentence, the date where it fits without shrinking it (§15, 5 Nov 2026), and the second clock under the date where the band beside the dial holds it (without the dial, on top under the date); the title (22 sp, down to 18 to stay on one line), when, the day's note hanging from the bottom leading corner. Under them, where two more lines fit once the dial has its comfort (72 dp), the next one or two events as rows ("13:00  Lunch with Giulia", 13 sp, the time quiet and the title strong; 8.3), the note keeping its count only where they leave some out; else, on a card four cells wide with a line to spare, the rest of the day as a line of times ("Then 13:00 · 15:00 · 18:30") in place of the note's count |

**The dial** (owner, 8 Oct 2026; `DialArcs`, `WidgetDial.kt`, ADR 0003): a clock face where Passo's card has its ring. The hands are the system's `AnalogClock`, drawn by the launcher and moved by the system like the `TextClock`; the face under them is a bitmap Tempo paints at each redraw: the ring's track, what is left of today as arcs (today only, owner, §15, 5 Nov 2026: a later day's events are in the words and the list, never on the face), the focus among them in the accent from its start to its end, the rest quieter, four quarter dots. The arcs change only where an event starts or ends, or comes onto the face, the boundaries the card is redrawn at (the last one since the review of 30 Oct 2026, where an «At a glance» card is placed, for today's events), so the hour hand walks into an arc, through it and out of it with no work by Tempo. A face shows twelve hours: the window opens now (or at the start of the event under way, six hours back at most) and closes eleven and a half hours later, so no arc ever meets the hand from behind; what lies beyond it is left off, never wrapped, and an event of today that reaches past it (only early in the day) is cut there, its arc fading out at the cut end, as at a start cut six hours back (`DialArc.cutAtEnd`, `cutAtStart`, `DialFade`; owner, §15, 11 Nov 2026). An event of today that runs past midnight is drawn whole. The samples paint the hands into the face at their moment.

The focus is the event under way, else the next one today, else (with the days ahead) the first one after today under its day's name, else "Free" (`WordsDay`). The note says times and counts, never minutes from now (§15): "Then 2 more today.", "Free until then; one more after it.", "Nothing left today.", or a free day's all-day events.

### Rendering

- **No scrolling container**: a Glance `Column`, never `LazyColumn` (a `ListView` underneath). Glance drops the eleventh child of a container silently: events go in groups of at most five, the header and the footer counted.
- **The second clock** (step 8.5) is one more `TextClock`, its pattern the city as a quoted literal before the card's time pattern ("'New York' HH:mm"), its zone set with `setTimeZone`: the system moves it as the first, at no cost, across both midnights. It says no day ("· tomorrow" is Today's): a `TextClock` cannot tell the phone's date from the city's, and a word written in would be wrong between the two midnights. At the label step (13 sp), in the secondary ink, in the face of the card's date; its touch is the header's. Measured by `AgendaFit` and `WordsFit` (`secondEm`), drawn where it fits whole, else not at all; never on a row or a cell.
- **The clock and the date are a `TextClock`** in a small XML layout, placed in the card with Glance's `AndroidRemoteViews`; `format12Hour`/`format24Hour` set per widget (`RemoteViews.setCharSequence`), and with both set to the same pattern when the reader forced one. It is drawn by the launcher, updated by the system every minute, and costs the app nothing. Its text size and ink are set like the card's other text.
- Inks resolved against the ground the card really has (`widgetInk`, Chiaro's rule, as in Passo), never left to the launcher's day/night resolution.
- An event row: the calendar's mark, the time ("10:00–11:00", or "All day"), the title on one line, ellipsised; the event under way in the strong ink with "ends 11:00"; past events are not on the card at all (the card is the rest of the day).
- Previews: a static `previewLayout`, and generated previews on Android 15+ (`providePreview`) from a sample day, once per version.

### Taps

- Header (the clock and the date): Tempo's Today (the default), the calendar app at today, or the phone's clock app (owner, §15), per widget.
- Event: the event's occurrence in the calendar app (the default, as on Today), or Tempo's Today, per widget.
- "N more today", the rest of the card: Tempo's Today.
- No permission: Tempo, where Today (or the first run) asks for it.
- A door that is not on the phone (no calendar app, no clock app) falls back to Tempo, and the settings screen does not offer it.

### Update strategy (battery-aware)

- **Calendar changed**: the provider's broadcast, else the content-URI-triggered work (§4.5), updates every card and stays until they are drawn; the work arms the next one as it starts.
- **A boundary**: after every render the card arms one `AlarmManager.set(RTC, NextBoundary)` (inexact, **non-wakeup**): if the phone is asleep it is delivered when the phone next wakes, which is when someone can see the card. No `SCHEDULE_EXACT_ALARM`, no `RTC_WAKEUP`.
- **Clock, zone, language**: `TIME_SET`, `TIMEZONE_CHANGED`, `LOCALE_CHANGED`, exempt from the implicit-broadcast limits, received in the manifest.
- **Boot and update**: the system sends the card its update after a boot and after an app update; the alarm and the work are armed again from it.
- **Settings changed** in the app or the card's own screen: an immediate update.
- Nothing else. No periodic work, no polling, no foreground service.

---

## 8. Notifications and system surfaces

- **No notification channel in v1**, and so no `POST_NOTIFICATIONS` (VISION, System surfaces).
- **Launcher shortcut** (dynamic, `NewEventShortcut`: a static one names its package, and the debug build's is another): "New event", the `ACTION_INSERT` of §4.7 (owner, §15). Published again when its words are not the app's any more (the reader changed the app's language, review of 30 Oct 2026).
- **Pin request**: onboarding and Settings offer "Add the widget to the home screen" through `AppWidgetManager.requestPinAppWidget`, where the launcher supports it. The first run's widget page shows each card as the reader's day will stand on it, at its default size on the default look (`HomeWidgetPreview`, passed in by `:app`; read by `WidgetModelLoader.loadUnplaced`, which arms nothing; owner, 15 Nov 2026).

---

## 9. Battery rules (non-negotiable)

1. No service, foreground or background. Tempo runs only when a screen of it is visible, when a card is being drawn, or when the calendar changed.
2. No wakelocks of the app's own (WorkManager holds its own while its work runs, as in Chiaro and Passo).
3. No exact alarms, no wake-up alarms, no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no periodic workers.
4. Nothing runs on a timer while the screen is off. The minute ticker lives in the page's lifecycle (`repeatOnLifecycle(STARTED)`), the observer in the collector's.
5. The widget's clock is the system's `TextClock`: never a repaint per minute by the app.
6. A phase that touches the refresh ends with a check: `adb shell dumpsys alarm | grep tempo` (one non-wakeup alarm at most), `adb shell dumpsys jobscheduler | grep tempo` (one content-triggered job), `dumpsys batterystats --charged com.callbackdev.tempo`, Doze (`dumpsys deviceidle force-idle`).

---

## 10. Permissions and manifest

| Permission / element | Type | Why |
|---|---|---|
| `READ_CALENDAR` | Runtime | Read the Calendar Provider (Phase 1, declared by `:core:calendar`) |
| `<queries>` for `ACTION_VIEW`/`ACTION_INSERT` on calendar URIs | Manifest | Lets Android 11+ show Tempo the calendar app, to know whether a button can work. Not a permission |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` | Normal | Brought by WorkManager, which Glance runs its sessions on and the calendar trigger uses, as in Chiaro and Passo. Without `INTERNET` nothing can leave the phone. Tempo's own code uses none of them. Reviewed in Phase 4 against the merged manifest: the same set as Phase 0's, nothing added by the widgets (the boundary alarm is `AlarmManager.set(RTC)`, which needs no permission); kept as WorkManager declares them, as in the sisters |
| `<queries>` for `AlarmClock.ACTION_SHOW_ALARMS` | Manifest | Lets Tempo find the phone's clock app (`ClockApp`, declared by `:core:calendar` since 8.1): Today's alarm line and a card whose time opens it (Phase 4). Not a permission; Tempo opens the app's front door and never sets an alarm |
| `android:allowBackup="true"` + `dataExtractionRules` | Manifest | The settings file only (§5) |

**Forbidden:** `INTERNET`, `ACCESS_*_LOCATION`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WRITE_CALENDAR`, `READ_CONTACTS`, `WRITE_CONTACTS`, `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS*`.

`checkForbiddenPermissions` (build-logic, `ForbiddenPermissionPatterns`) checks every variant's merged manifest, is wired to `check`, and runs in CI and in the release workflow. A library that adds one is removed or patched with `tools:node="remove"`.

---

## 11. Delivery phases

Each phase ends with a merged PR, green CI and its acceptance criteria met. Phases 1 and 4 need **testing on a physical device** with real accounts: an emulator's calendar has no sync and no second calendar app.

**Phases 0 to 6, and after 1.0: closed.** The foundations, the calendar engine, Settings, Today and the first run, the widgets, the guide and the accessibility pass, the 1.0.0 and the 1.0.1 (7 to 10 Oct 2026): their steps and acceptance are in `docs/planning/01-phases-0-8.md`, §11, where the index (`docs/planning/README.md`) finds each phase by its title. Phase 5's one open box, the Baseline Profile, is in §15's Open list.

**Phase 8, the 1.1: closed.** The review of 30 Oct 2026 and the 1.1.0 (30 Oct to 2 Nov 2026): its plan and its steps 8.0 to 8.8 are in `docs/planning/02-phase-8.md`, §11. Its step 8.4 («Join», on hold) and 8.8's battery check are boxes in §15's Open.

### Phase 9 — The month, and the time's door (owner, 23 Nov 2026)

Two proposals of the owner's, weighed and built on one branch (`claude/month-and-clock-door`); the
reasons are in §15's entry of 23 Nov 2026 and in `docs/adr/0005-the-month.md`.

- [x] **9.1 The time opens the clock app.** Today's time row, the figures and the face as one touch
  target (`TimeRow`), opens `ClockApp` by its front door; `time_opens_clock` (on by default), its
  switch in Settings' Clock group, drawn only where the phone has a clock app.
- [x] **9.2 The month's dates and marks**, pure (`:core:domain`): `AgendaBuilder.days(from, to)`,
  `AgendaWindow.of(from, to)`, `MonthGrid` (whole weeks), `MonthPages` (pages and the dates to
  read), `MonthMarks` (dots, bars, lanes), `newEventOn(date)`, `NewEvents` over a run of dates.
  Tests first, the §4.6 cases the month meets named in them.
- [x] **9.3 A day's rows shared.** `AgendaEventRow`, `AgendaAllDayRow`, `DayHeading` and their
  words in `:core:designsystem`; `CalendarDoors` in `:core:calendar`. Today draws as before.
- [x] **9.4 The page** (`:feature:month`): the grid under the finger, the chosen day under it, the
  top row (back, the month's name, "Today", the arrows), the way to a far month, the new-event
  button and the long touch, the states, two panes on a large screen, the reveal of the event
  made. `MonthScreenTest` on states: `assertAccessible()`, `walkPage()` at twice the text size, on
  an open foldable and a phone on its side, a tablet in two panes, both languages, both themes.
- [x] **9.5 The door.** Today's button beside the gear opens the month (`MonthKey` on the one back
  stack); the date keeps opening the calendar app. The guide's chapter, the README's picture.
- [x] **9.6 On the phone**, before the merge (§15, Open): the checks listed there. Done by the owner on 23 Nov 2026, on the CI's APK of the branch (§15, «Phase 9 checked on the phone»).

**Acceptance.** The month shows the right marks on the right dates in every zone (the engine's
tests, run on the month's dates); a touch on a date lists it as Today would; a new event lands on
the chosen date; nothing of Tempo runs with the page closed (§9: no alarm, no job, no service
added: `dumpsys alarm` and `dumpsys jobscheduler` as after 1.1); the merged manifest is unchanged.

### Phase 10 — Today has two grounds: the graphic review (owner, 26 Nov 2026)

The owner's review of 26 Nov 2026 ("functional and clean, but a little flat"): eight points, all
approved; the reasons, what each changes of the decisions of 10 Nov 2026, and what was weighed and
left out are in §15's entry of 26 Nov 2026. One step is one session's work, on one branch
(`claude/today-two-grounds`); each step ends with `spotlessApply`, `test`, `:app:lintDebug` and
`:app:checkForbiddenPermissions` green and its screenshots looked at. The steps are in the order
they are built: 10.3 stands on 10.2's ground, 10.5 on 10.4's grid.

- [x] **10.1 What changes no look: the month's title whole, Today's rows moving, the launch's
  ground** (points 7 and 6, and the review's one refinement).
  - The month's title is never cut (`MonthTopBar`): "Today" becomes an icon button, the calendar
    with today's number in it (a new drawing in `TempoIcons`, the number set as text over it),
    48 dp where the text button took about 70; its TalkBack label stays "Today". `MonthScreenTest`:
    away from the current month at 411 dp and at 360 dp, the title's text is laid out whole, in
    English ("September 2026") and in Italian ("settembre 2026"); the icon passes
    `assertAccessible()`. Look at `month-empty-day.png`.
  - Today's rows move (`TodayScreen.kt`, `row()` and `card()`): `animateItem` with the theme's
    springs (`TempoMotion.spatial`, `effects`), a plain fade under reduced motion, as the month's
    rows already do. The "earlier" rows open and close in place; a row that changes state keeps
    its key, so it does not leave and come back.
  - The launch: `windowSplashScreenBackground` in `Theme.Tempo` (`values/`, `values-night/`) set
    to the Vivid dress's surface, so the system's splash and the page's first frame are one ground.
    The one place a ground is a hex outside `Scheme.kt`: said in a comment there, with the role it
    copies.
- [x] **10.2 Today's two grounds** (points 1, 2, 3 and 4, one review of the page: each asks for
  the next). Built on 26 Nov 2026: what it measured and what building it added are in §15,
  «Step 10.2 built».
  - **The clock's ground.** `Hero` stands on `surfaceContainer`, up to the screen's top edge
    (the status bar's inset inside it, no longer on the list), its lower corners 28 dp; the agenda
    stays on the page. On two panes the ground is the whole leading pane (ADR 0004, amended).
    `DayDial` takes the ground it stands on: its face is filled with the page's `surface`, and
    its arcs' marks are stepped to 3:1 against the hero's ground (`markColor`, `markAlpha`). The
    guide's example of the dial follows, being the same composable.
  - **"Now" in the accent.** The event under way stands on `primaryContainer`, its words in
    `onPrimaryContainer`, its bar stepped against that ground; `UnderWayProgress` is 6 dp, its
    track a tone of the accent. In `AgendaRows.kt`, so the month's chosen day follows.
  - **The button in amber.** `NewEventButton` on Today and on the month: `secondaryContainer`,
    `onSecondaryContainer`.
  - **A day's name larger, no hairline.** `DayHeading`: the name in `titleLarge`, the divider and
    its parameter gone (the month's panel keeps its own line under the grid). The long names in
    Italian ("Da giovedì 12 novembre a sabato 14 novembre") looked at again at this size, at 360 dp
    and at twice the text size.
  - Tests and pictures: `ContrastTest` gains every ink on the two new grounds (the quiet ink and
    the accent on `surfaceContainer`; the title, the quiet words and the accent on
    `primaryContainer`), in both dresses and themes; `TodayScreenTest` and `MonthScreenTest` whole,
    every walk; measure where the event under way and tomorrow's name stand at 411 dp against
    today's (the review's drawing says about 6 dp lower for the first, level for the second) and
    write the figures in §15. The README's pictures of Today and the month redrawn and looked at;
    the guide's Today chapter reread in both languages.
- [x] **10.3 The compact bar** (point 5). Built on 26 Nov 2026: when the bar comes, its shape and
  what building it added are in §15, «Step 10.3 built». On one column, once the hero has left the screen, a bar
  stays at the top in the hero's ground: the date (short), the time in figures, the month's and
  the settings' buttons. It reads the page's own minute ticker (no timer added, §9); it fades in
  and out (a plain cut under reduced motion); the date and the time are the doors they are in the
  hero (the calendar app, the clock app), each drawn only where the phone has the app. Not on two
  panes, where the hero never leaves. `TodayScreenTest`: there after the scroll and gone at the
  top, `assertAccessible()`, the walk at twice the text size and on a phone on its side (the bar
  must not take the page: look at the screenshot), TalkBack's order (the bar before the list).
  The reveal of a new event (`revealNewEvent`) counts the bar's height.
- [ ] **10.4 The month's grid, shared** (point 8, first half; no change to see). The grid's
  drawing moves from `:feature:month` to `:core:designsystem` (a month of cells with their marks,
  today and a chosen date: `DayCell`, `drawMarks`, `Weekdays`, their sizes), as the agenda's rows
  moved in 9.3; the pager, the long touch and the page stay in `:feature:month`, which draws as
  before (`MonthScreenTest` unchanged and green, its screenshots compared). `TodayViewModel` reads
  the current month's marks (`MonthPages`, `MonthMarks`, `AgendaBuilder.days`) only while Today
  stands in two panes and is visible, on the observer and the ticker it already has: no read on a
  phone, none with the page closed. Domain tests first where a function is new.
- [ ] **10.5 Today on a large screen: the month under the clock** (point 8, second half). In the
  hero's pane, under today's all-day events: the current month on the page's `surface`, its name a
  heading with the chevron, the shared grid, today marked. A touch on a date opens the month on
  that date (`MonthKey` gains the date); a touch on the name opens it on today. No pager, no
  chosen day, no long touch here: it is a way in, not a second month. `TodayScreenTest` on the
  tablet on its side and upright and on the open foldable over 840 dp (where the pane scrolls: the
  month comes after the hero and must not push the sentence off the first screen); ADR 0004
  amended (the hero's pane, its ground, the month); `today-tablet.png` redrawn; the guide's
  chapter on large screens, both languages.
- [ ] **10.6 On the phone and the tablet**, before the merge (§15, Open): the checks listed there.

**Acceptance.** Today reads as two grounds, the clock's and the calendar's, and one accent that
means now, in both dresses and both themes; every ink on the new grounds is pinned at 4.5:1 and
every mark at 3:1; the first screen at 411 dp shows no fewer events than before the phase (the
measure of 10.2); the month's title is whole at every width and text size the tests walk; no
alarm, no job, no service and no permission added, and the calendar read no more often on a phone
than before.

### Phase 7 — Google Play (optional, later)

- [ ] As Passo's Phase 9: the same key, the store listing, the data safety form ("no data collected, no data shared").

**For the sisters** (not Tempo's work; written here so their sessions find it): Passo takes Tempo's filled `StatusCard` button (§15, 7 Oct 2026), its first-run and Settings "Add to the home screen" (`requestPinAppWidget`), the picker's generated previews and a Widgets group in Settings; Passo's `FilterChip`s (8 dp) against its own "chips are fully round" rule is its call; Chiaro takes the generated previews. The family decides together whether to adopt Chiaro's segmented groups, and makes the Material 3 Expressive pass when `material3` 1.5 is stable.

---

## 12. Testing strategy

- **Pure Kotlin first.** `AgendaBuilder`, `DaySentence`, `FreeTime`, `NextBoundary`, `AgendaFit` and the colour mark are tested on the JVM with fixed instants and zones, every §4.6 case named in a test.
- **The provider under Robolectric.** A fake provider registered for `com.android.calendar` returns cursors built from fixtures, so `CalendarSource` is tested with the real `ContentResolver` path.
- **Screens from states.** Compose UI tests draw a screen from a state, never from a database, with `assertAccessible()` and `walkPage()` (`:core:testing`), at twice the text size and on an open foldable; their screenshots land in each module's `build/screenshots` and are looked at.
- **The widget's forms.** Glance's test utilities for content; a gallery test that draws every form at the reference grants.
- **On a device**, for what only a device can say: real accounts and sync, the calendar apps' handling of the intents, the launcher's grants, the alarm's delivery.

---

## 13. Working with Claude Code

`CLAUDE.md` holds the operating rules (commands, modules, invariants, design, signing). The workflow, as in the family:

- Work one phase at a time, from its section in §11.
- Write the domain test first for anything that decides a day, a boundary or a fit.
- Tick the boxes as work lands; record every decision and deviation, with its reason, in §15, as a `###` section at the end of its log (and as an ADR in `docs/adr/` when it is architectural). The closed periods of the log are archived, frozen, in `docs/planning/` (How it is made, at the top).
- Run `spotlessApply`, `test`, `:app:lintDebug` and `:app:checkForbiddenPermissions` before pushing.

---

## 14. Risks and mitigations

| Risk | Mitigation |
|---|---|
| All-day events on the wrong day | Read in UTC (§4.2); tests in zones west, east and at UTC |
| A calendar app that ignores an intent | `resolveActivity` before drawing a button; VIEW over EDIT (§4.7); measured in Phase 1 |
| A card that is stale after a change | The content-URI trigger re-enqueued after each run; on return to the app, the observer; measured in Phase 4 |
| A finished event lingering on the card | The non-wakeup alarm at the boundary; the card says times, so a minute's delay never misleads |
| Launchers that grant odd sizes | `SizeMode.Exact` and a form for every grant, no maximum (Passo's ADR 0005) |
| Glance's eleventh-child limit | Events in groups of five, a test that counts children |
| Work-profile calendars invisible | Said in the guide: install Tempo in the work profile too |
| Calendar colours unreadable on a card | Marks only, never text; lightness stepped to 3:1 (§6) |
| OEM calendar providers with gaps (no `DISPLAY_COLOR`, odd `VISIBLE`) | Defensive reading with defaults; tested with columns missing |
| WorkManager's permissions in the merged manifest | Reviewed in Phase 4; the forbidden list guards what matters |

---

## 15. Decisions log and open questions

### Open

What is still to decide or to do, in one place, besides the steps of the phases in §11 (which keep their boxes there): a box opens here, with the reference to the log entry it comes from, and closes here, with the one that closes it. A new question is written here and asked before it is built.

- [ ] **«In words» two rows high: may the words rise beside the dial?** (30 Oct 2026, step 8.3.) The tall card budgets its height as if the dial took the card's whole width, so on the three- and four-by-two the leading side under the date, beside the dial, stays empty (about 65 dp of 161 on the four-by-two at 10:20), and the next events do not fit under the words. The words hang from the bottom; they could rise beside the dial wherever each line beside it is narrower than the room the dial leaves (on the four-by-two at 10:20 about 215 dp: the title and its time beside the dial, the note and the next two events under it, the dial kept at its 73.75 dp, the family's circle since 4 Nov 2026). Options: (a) as now, the rows on three rows and up; (b) the words rise beside the dial, the two-row cards listing the next events where their lines fit beside it. The two-by-two gains nothing either way: beside its 52.75 dp dial (the tall circle since 4 Nov 2026; it was 63) the room is 66 dp, the date's. Asked of the owner; not built.
- [ ] **Baseline Profile, if start-up measures slow** (Phase 5's one open box, carried from archive 01, §11, Phase 5; §1's "Baseline Profiles later"). The log records no measure of start-up; the work opens when one finds it slow.
- [ ] **«Join», on hold** (step 8.4; owner, 30 Oct 2026: "not a priority; I want to weigh it again in the future"). A video call's link on Today's event under way or about to start, opened by a button; nothing of it is built. Its plan, to start from: `docs/planning/02-phase-8.md`, §11, Phase 8, step 8.4.
- [x] **The new icon on the phone** (§15, 9 Nov 2026, «The new icon: the day's page and the clock»): the launcher icon in the phone's own mask, the themed icon, the «New event» shortcut. Done by the owner on 9 Nov 2026 (§15, «The new icon checked on the phone»).
- [x] **The «New event» shortcut as an adaptive icon, on the phone** (§15, 9 Nov 2026, «The «New event» shortcut, an adaptive icon»): long press on the icon, in the phone's own mask and with the themed icons on. Done by the owner on 9 Nov 2026 (§15, «The adaptive shortcut checked on the phone, and Tetto in the family»).
- [ ] **Today's review on the phone** (§15, 10 Nov 2026, «Today reviewed: one clock, one "now", one ground»; and 11 Nov 2026, «The dial: a cut arc fades, and the face stays while today has events»): the page in both dresses and both themes, early morning (the face with its ring empty, a long event fading at 12:25), morning and evening (the dial there and gone), the «In words» card early in the day, the day markers on the 12-hour clock, the button's icon over the last rows, TalkBack on the all-day events and the button.
- [x] **Phase 9 on the phone, before the merge** (§11, step 9.6; §15, 23 Nov 2026, «The month, and the time's door»). (1) A touch on Today's time opens the Clock app, on its figures and on the face, morning and evening; off with the switch. (2) The month from Today's button, and back with the gesture, previewed under the finger. (3) The months under a real finger: the grid's height following it between a five-week and a six-week month, the day under it sliding. (4) The marks against the calendar app's own month, with the owner's real calendars: a holiday of several days as one bar, two at once on two lanes, a day of more than three events with its plus. (5) A new event from the button and from a long touch on a date: does the calendar app open on **that date**, at the time proposed (Google Calendar and Samsung Calendar both; §4.7's intent was measured for today's dates only, `docs/device-checks/calendar-intents.md`)? Back in Tempo, the event made is chosen and glows. (6) The way to a far month, and "Today" back. (7) TalkBack on a date ("Thursday 26 November, 3 events"), on the month's name, on the time row. (8) The Tab S8+: the month beside the day. (9) §9.6's `dumpsys alarm` and `dumpsys jobscheduler` unchanged with the month open and closed. Done by the owner on 23 Nov 2026 (§15, «Phase 9 checked on the phone»): "everything works, the new event on a date other than today too". The owner's word is on the whole; (9), which needs adb, rides with 1.1's battery check below.
- [x] **The month button's switch: keep it, or let it go?** (§15, 23 Nov 2026.) `show_calendar_button` was a switch for a button that doubled the touch on the date (owner, 9 Oct 2026). The button is now the month's only door: turned off, the month cannot be reached, and a reader who had it off before the update does not find the month until they turn it on. Options: (a) as built: the switch stays, renamed «Month button», its note saying what it opens; (b) the switch goes, the button always there, the stored choice ignored. Asked of the owner; (a) was built. Answered on 23 Nov 2026: (b), the switch goes (§15, «The month's button has no switch, and nothing is logged to a file»).
- [ ] **1.1's battery check on the phone** (step 8.8's open box; §15, 2 Nov 2026, "1.1.0 released", in archive 02). §9.6's check with «In words» placed: still one non-wakeup alarm of Tempo's at most, about two boundaries an event a day plus the moments events come onto the dial; written in `docs/device-checks/widget-refresh.md`.
- [ ] **Phase 10 on the phone and the tablet, before the merge** (§11, step 10.6; §15, 26 Nov 2026, «Today has two grounds: the graphic review»). On the CI's APK of the branch, in both themes and, once, in the Paper dress. (1) Today's two grounds: the hero's against the page (a tone, 1.1:1: is it seen as a ground on the phone's screen, in daylight?), the dial's face on it, the status bar's icons readable over the hero's ground, and, the page scrolled, the strip of that ground left behind the status bar (no test draws a status bar: §15, «Step 10.2 built»). (2) The event under way in the accent, in the light and in the dark: is the dark one too loud? Its bar with a blue calendar and with a red one. (3) The amber button, over the last rows. (4) The days' names at their new size with the owner's real calendar in Italian: no word alone on a line, the chevron in its place. (5) How many events the first screen shows, against 1.1's. (6) The compact bar (§15, «Step 10.3 built»): it comes as the hero's foot goes up under it and goes as the foot comes back, without a jump, and with the status bar's strip over it reads as one ground with no seam (no test draws a status bar); its round foot over the rows passing under it; its time moves with the minute; its two doors open the calendar and the clock, and a touch on it never opens the row under it; on the 12-hour clock ("10:20 am" whole), and at the phone's largest text size, where the date takes two lines. (7) The rows' movement: "earlier" opened and closed, an event made in the calendar app and one deleted with Today open, an event ending. (8) The month away from today: the title whole, the "Today" icon understood at a glance. (9) The launch: no flash between the splash and the page. (10) The Tab S8+, on its side and upright: the month under the clock, a touch on a date, the pane scrolling where it must. (11) TalkBack on the compact bar, the "Today" icon, the month in the hero's pane.

Reread on 2 Nov 2026, at archive 02: four boxes open, the question on the two-row «In words» (asked of the owner on 30 Oct 2026), Phase 5's Baseline Profile (carried from archive 01), 8.4's «Join» (on hold) and 1.1's battery check (both carried from archive 02); the phases' open steps are in §11 (Phase 7, optional). No proposal undecided.

---

### Log since 2 Nov 2026, after archive 02

### «In words» and Passo's «At a glance»: one circle at one size (owner, 4 Nov 2026)

**Where it comes from.** The owner, with Tempo's «In words» and Passo's «At a glance» side by side on the phone, two by two: "make the circles of the two cards uniform, so that at the same size of card and the same content they are the same size; leave the circles' resizing as it is, but at the same size and text the circles must match. Now with no events the clock is larger than with events to show. If the two circles are the same it is very nice to look at. Two by two, as in the screenshots." (The owner's screenshots did not reach the session; the gallery's were drawn and measured instead.)

**What was measured** (the reference two-by-two, 159 × 189 dp, the default text size). Passo's ring took the height its words left (the number, the sentence over two lines, the goal: 108.24 dp of 161), 52.75 dp, and grew where the sentence was shorter (73.75 dp for one line, 95 for none). «In words»'s dial grew to the date beside it (63 dp) or to what its note left (60.5 dp under a note over two lines); on a wider two-by-two a free day's dial grew to the date's limit while a busy day's stayed where its words held it (at 175 dp wide, 79 dp against 60.5): the jump the owner saw, and never the ring's size.

**What changed.**

1. **The tall circle** (`WordsFit.tallCircle`; in Passo `tallCircle` in `GlanceLayout.kt`, the same arithmetic and constants). Under three cells wide, the circle in a tall card's top trailing corner is the height the card's inside leaves over a budgeted band of words: a 34 sp line and three 16 sp lines at 1.32 em (108.24 dp at the default size, the band Passo's usual words fill), rounded down to a quarter, never under 44 dp (the row's floor, and Passo's). Budgeted, never measured: the day's words do not move it. On the reference two-by-two both cards draw 52.75 dp, a free day and a busy one, a long sentence and a short one; at 115% and 130% the band grows and both stand at 44 dp; resized taller, both grow alike, while neither card's own limit holds them (below).
2. **What stays each card's own.** The dial keeps its limits (the date's 56 dp beside it, the ceiling of step 8.3), Passo's ring its own (104 dp, the card's width): the two match wherever neither binds, which on a card two cells wide is up to about 199 dp tall, the two-by-two on every grid measured. A two-by-three keeps its 63 dp dial, held by the date, and Passo's ring its 104 dp. From three cells wide nothing changes: Passo draws its panel there (the ring at 56 dp in the header's row), and «In words»'s dial grows with the room its words leave (step 8.3, 78.5 dp on the three- and four-by-two).
3. **«In words»'s words are what they were.** The room the circle gives up under the dial's comfort stays empty: the two-by-two keeps its note's count and lists no row ("Free until then; one more after it." at 14:20, not "Free until then." over "18:30  Swimming le…"). The tall dial still starts at 48 dp, or at the circle where that is smaller (a large text size), where it would have been dropped.
4. Passo's side, written in its own PLANNING: the band is its usual words, and where the reader hid the sentence, the number and the goal (95 dp on the two-by-two, as before).

Tests: `WordsTest` (the circle at the reference height and text sizes; one dial through the day at 159, 175 and 200 dp wide; the floor at 130% with the time kept; the words kept; three cells wide unchanged; the two-by-two's pin, 52.75 dp where it was 63, and the two-by-three's, 63). `WidgetFitTest` finds nothing cut at every grant and text size. The README's widget picture redrawn: the two-by-two's dial is smaller, nothing else moves.

### The family's circle at every size (owner, 4 Nov 2026)

**Where it comes from.** The owner, after the entry above: "not only two by two: you must check every size; I wrote it badly before". Then, asked about the two cases where the cards stand in different compositions: two cells wide and three rows high, "Tempo grows" (the dial to the ring's 104 dp, the date on a line over the title); three and four cells wide, "Passo's ring grows" (one size per card for both, the hours' bars as they were).

**What was measured** (both galleries at every reference grant: «In words» at 10:20 and on a free day, «At a glance» with the hours and without; dp, after the entry above):

| | 1×1, 1×2, 1×3 | 2×1 | 3×1, 4×1 | 2×2 | 2×3 | 3×2 | 4×2 | 3×3 | 4×3 |
|---|---|---|---|---|---|---|---|---|---|
| «In words», busy / free | 69 | 47 | 56 | 52.75 | 63 | 78.5 / 101 | 82.5 / 101 | 104 | 119.25 |
| «At a glance», hours / none | 69 | 44 | 56 | 52.75 | 104 | 56 / 74 | 56 / 74 | 56 / 104 | 56 / 104 |

**What changed.**

1. **The family's circle**, three functions, the same arithmetic in both apps (`WordsFit.cellCircle`, `rowCircle`, `tallCircle`; Passo's `dotRingSize`, `rowRingSize`, `tallCircle`). One cell: the shorter side less 8 dp each side, 36 to 104 dp. One row: the height inside the snug insets, the words beside it keeping 84 dp, 44 to 56 dp. Two rows and up: the height left over a budgeted band of words, never measured (a 34 sp line and three 16 sp lines at 1.32 em under three cells wide; two from three cells, where a sentence that takes two lines on two cells takes one), between 44 dp and step 8.3's ceiling (104 dp, or 45% of the inner side) or the inner width, rounded down to a quarter. At the reference grants: 69 dp on one cell; 44, 56, 56 on a row two, three, four cells wide; 52.75, 73.75, 73.75 two rows high; 104, 104, 119.25 three rows high. Measured again on both galleries: the same at every grant, with events or without, with the hours or without.
2. **«In words»'s dial is the circle, whatever the day says**: a free day's no longer grows (101 dp on the three- and four-by-two). It yields only where the title's first line and its time would not fit under it (a large text size), never under 44 dp. On a row two cells wide 44 dp (it was 47). One cell: the time under the dial only where it fits under the cell's circle, which is never made smaller for it (the reference cells keep their 69 dp and their time). Two cells wide and three rows high: the dial at 104 dp (it was 63, held by the date), the date on its own line over the title ("Wed 7 Oct", one line), the second clock under it; wherever the dial leaves its 56 dp column the date stays beside it, as on every other reference grant. What follows the note (the title's second line, the rows, the line of times) still comes where it fits with the dial at its 72 dp comfort, so each card keeps the words it held: the two-by-three its note and two rows, the four-by-two its line of times, the two-by-two its note's count. At 130% a four-by-three's dial keeps its 119.25 dp and its two rows go, where before its dial gave them its room.
3. Passo's side, in its own PLANNING: the panel's ring is the circle (73.75 dp on the three- and four-by-two, from 56; 104 and 119.25 on three rows), the bars as they were; on the four-by-three the sentence stands under the number, beside the ring.

Tests: `WordsTest` (the family's circle at the reference grants; one dial through the day at five widths and four heights; the date over the title on the two-by-three, with its note, its rows and the second clock; the words kept on the two-by-two; the dial's side at every reference grant). `WidgetFitTest` finds nothing cut at every grant and text size.

### The owner's two-by-two keeps its title's second line (owner, 5 Nov 2026)

**Where it comes from.** The owner, with two screenshots of the phone, before the family's circle (4 Nov, 21:19: "Prenotare / cambio gom…" over two lines) and after it (5 Nov, 08:26: "Prenotare ca…" on one): "the change is very good, but could Tempo show the event in the same room as before, for example on the two-by-two? Without changing the circle's size, and without touching the work just done."

**Measured** on the screenshots (1,080 px wide): the cards 438 × 509 px, both circles 191 px after and the dial 181 before, so 2.33 px a dp: the owner's two-by-two is about 188 × 218 dp at the default text size, its circle 81.75 dp (the dial grew to 78 before). Under it, 190 dp inside: the title's first line 30.04, its time 22.12, the gap 8, the note 22.16, and 26 dp left where the title's second line was counted at 30.04.

**What changed** (the circle as it was):

1. **A Text's later lines are counted as they are drawn** (`linesHeight`): its first line at 1.32 em and the rounding pixel, each further one at 1.2 em, the face's ascent and descent, as a `TextClock`'s line is already counted. A Text's font padding is its first line's top and its last line's bottom, never paid between two lines: Roboto's second line takes 1.17 em. On the tall card: the title's second line, and a note over two lines or three.
2. **The air under the tall dial is 4 dp** (it was 8). With the title's own leading over its capitals (0.35 em, 7.7 dp at 22 sp), the dial stands about 11.6 dp over the title's capitals, as the ring of «At a glance» over its number (11.7).

Together they give the owner's two-by-two its title's second line, its time and its note, with 3.6 dp to spare. The reference grants show what they showed (a two-row card still budgets what follows its note at the dial's 72 dp comfort), and the README's picture is drawn byte for byte as committed.

Tests: `WordsTest` (the owner's two-by-two: the circle at 81.75 dp, a title too long for one line over two, its time, its note, the date beside the dial), `WidgetFitTest` (the owner's grant and a title too long for one line added to its cards: nothing cut at 100, 115 and 130%), the gallery's `widget-words-owners.png`.

### «In words» kept to the event: «The day's note» (owner, 5 Nov 2026)

**Where it comes from.** The owner, in the same message: "in Passo, when I hide the day's sentence the circle grows, and that's just right. I'd like a similar setting on Tempo's widget, so the event's description is terser and the circle can grow to the same size as Passo's: a still greater uniformity a reader could choose when setting the widgets up on the home screen. Evaluate it well." Weighed and asked, three answers: off, the card hides the note and what follows it, and says the event's title on one line and its time; where the larger dial leaves the date no room beside it or over the title, the circle keeps its size and the date goes; and Passo's panel (three and four cells wide, with the hours) takes the larger ring too when its sentence is hidden, so the two match there as well.

**What it is.**

1. **A switch on «In words»'s settings, «The day's note» / «La nota del giorno»**, in the content group, on by default (`WidgetLook.showNote`, the card's own, in the `widgets` file). Its line says what it shows and what turning it off does. «Agenda» has none.
2. **Off, a tall card says the event alone**: its title on one line (a title too long for one line even at 18 sp is set at 18, to say more of it before its ellipsis) and its time; no note, no next events as rows, no line of times, no title's second line. One cell and one row are as they were: they never showed the note.
3. **The dial takes the band they leave** (`WordsFit.tallCircle(note = false)`, `EVENT_WORDS_BAND`): a figure's line and one line of text, 66 dp at the default text size, the band of Passo's ring without its sentence (the number and the goal), at every width. At the reference grants: 95 dp two rows high (the two-by-two, the three- and four-by-two), 104 three rows high on two and three cells, 119.25 on four; the owner's two-by-two (188 × 218 dp) 104, where the note's band gives 81.75. A free day's dial is a busy day's, as with the note.
4. **The date never shrinks it**: beside the dial where its 56 dp column is left, else on its own line over the title where it fits under the circle, else it goes. On the owner's two-by-two it stands over the title ("Wed, 7 Oct"); on the reference two-by-two (159 × 189 dp) the 95 dp dial leaves it 24 dp beside and not enough height over the title, and it goes; on the three- and four-by-two it stays beside the dial.
5. Passo's side, in its own PLANNING: its panel's ring follows its sentence's switch as its tall card's does.

Tests: `WordsTest` (the event's band at the reference grants and the owner's; one dial through the day at every size; the title on one line at 18 sp, the time, nothing after; the date over the title on the owner's two-by-two, gone on the reference one, beside the dial on three cells), `WidgetFitTest` (the card kept to the event, and with a long title, at every grant and text size: nothing cut), `WidgetLookStoreTest` (the switch kept per card, on by default), `WidgetConfigScreenTest` (the switch on «In words»'s page, none on «Agenda»'s), the gallery's `widget-words-owners.png`.

### The dial shows today only (owner, 5 Nov 2026)

**Where it comes from.** The owner, with a screenshot of Today at 22:37: "if there's an event the day after, its bar on the clock shows only partly (the event lasts 10 hours, but only part of it shows, because it seems only the next 24 hours are drawn), and it isn't very intuitive. It would be better if the clock showed only the same day's events, and whole. What do you honestly think?" The day had nothing left; tomorrow had one event, 08:00 to 18:00.

**What was wrong.** The face carried the next eleven and a half hours (§7, owner, 8 Oct 2026; ADR 0003, decision 9), and the review of 30 Oct 2026 woke an evening's card when tomorrow's first event came onto it. At 22:37 the window closed at 10:07: tomorrow's arc ran from 8 to 10 o'clock, a fifth of the event, and sat just behind the hand, where an arc reads as just over. Beside "Nothing in the calendar today" the face said the opposite of the sentence over it. Two faults in one arc: a face has no date, so another day's hours read as today's; and the cut arc said the event ended at 10:07.

**Weighed.** (a) Keep the window, never cut an arc (draw it whole or not at all): tomorrow's one-hour 08:00 would still sit behind the hand at night. (b) The owner's: today's events only. It agrees with the sentence and the list, which name tomorrow under its own name, and the arc is cut only where a twelve-hour face cannot hold an event of today: early in the day, one that ends more than eleven and a half hours after now (at 07:00, 13:00 to 20:00 would cross the hand), as before and rare. (b), as the owner asked.

**What it is.**

1. **`DialArcs` draws today's events not yet over** (`DialArcs.today`, `agenda.days.first()`), the focus among them when it is today's. A later day's event is never drawn, the focus included: where «In words»'s words name tomorrow's first event, its face is empty, as the day is. `DialArcs.of` loses its `showDaysAhead`: the card's switch still decides its words, no longer its face. An event of today running past midnight is drawn whole (it is today's).
2. **The window and the cut are as they were** (eleven and a half hours, six back for the event under way), measured on the wall clock; the DST tests now cross the jump within one date.
3. **`NextBoundary` with the dial counts today's events only** coming onto the face: an evening's card is no longer woken for tomorrow morning's (one alarm fewer a night; §9).
4. **Words**: Today's dial description ("with what is left of today on its ring"), its tip and the guide's paragraph (another day's events are in the list, not on the face), in both languages; `README.md`, `VISION.md` (Today, the dial; «In words»), `CHANGELOG.md` [Unreleased]. ADR 0003's decision 9 ("the rest of the next twelve hours") is changed by this entry for which events the window holds; the window itself stands.

Tests: `WordsTest` (tomorrow never on the face, the focus included; an event of today past midnight whole; the early cut; the DST nights within one date), `NextBoundaryTest` (tomorrow's first event no longer a boundary at night; today's evening event still comes onto the face), `TodayScreenTest` (the dial's description).

### The new icon: the day's page and the clock (owner, 9 Nov 2026)

**Where it comes from.** The owner asked for the four apps' icons to be redrawn from scratch as
one series: icons that say the app and recall what it does, full frame in Android's style, clean
and readable when small, flat (at most a shade of one colour, the light from the same side for
all), bright colours but not too much, one palette for the series. The proposals were drawn on a
board (three symbols per app, two grounds, the home-screen check, the grid); the owner chose the
series, asked for a margin between the drawing and the badge's edge (the first board let a symbol
reach the edge), and, with the margins in, saw Tempo's drawing a little off to the left and asked
for it to be centred better.

**What it is.** The ring and its clock face (ADR 0002, point 6; archive 01, «The launcher mark»,
«The launcher ring, azure to cobalt» and «The family's icons, made one suite», which this entry
changes) give way to **the day's page and the clock**: on the app's coral (`FF8E74 · F0604A ·
D3432F`), a white agenda page with three lines of the day, and in front of it at the lower right
an amber clock (`FFD36A · FDB528`) with its hands cut white, a band of the ground between them.
The family's rules: the app's colour as the ground, a white symbol with one amber accent, every
shape a ramp from its light tone at the upper left, no shadow, the whole symbol within 28 units of
the centre (a 56 dp circle on the 72 that show: 8 dp of air in a round mask, and no mask reaches
it). **Centred by its weight**: centred by its box, the drawing's weight (the white page) stood
2.3 units left of the centre, which is what the owner saw; it now stands 1.6 units right by its
box and 0.6 left by its weight, between the two (`FIT_SHIFT`).

**What changed.** `tools/draw_launcher_icon.py` is rewritten and writes three layers: the ground
is a drawing (`ic_launcher_background.xml`) where it was the colour `#F7F4EE`, so
`values/colors.xml`, which held only that colour, is gone and `ic_launcher.xml` points at the
drawing; the themed icon cuts the page's lines and the clock's hands out of their shapes. Asked in
the same review, the icons inside the app: Tempo has no notification, so no status-bar icon; the
«New event» shortcut (`ic_shortcut_new_event`) is its page and plus in white on the coral; the
guide and the first run show no app icon. The README's badges take the coral's deep tone,
`D3432F`, as they took the ring's. CLAUDE.md and ADR 0002 (point 6) say it.

**Checked.** The XML the script writes, drawn and compared pixel by pixel with the approved
drawing: 0.6% of the badge's pixels differ, all at the edges (the antialiasing).

### The new icon checked on the phone (owner, 9 Nov 2026)

The owner merged the icon's PR and checked the APK on the phone ("Ho mergiato anche la PR 149 e
anche le PR degli altri repo. Ho verificato gli APK."; then "segna le caselle"). The box in §15's
Open list is ticked; nothing else changes.

### The «New event» shortcut, an adaptive icon (owner, 9 Nov 2026)

**Where it comes from.** Seeing the shortcuts of Tempo and Passo on the phone, the owner found a
circle with white around it ("ora vedo un cerchio e intorno bianco"), then asked for them to be
adaptive ("Fai adattive per Saldo e anche gli altri due repo"). The shortcut drawn as a coral
disc («The new icon: the day's page and the clock», above, which this entry changes) kept its
shape only under a round mask: under any other the launcher shrank it onto a white plate, as it
does with any icon that is not adaptive.

**What it is.** `ic_shortcut_new_event` is now an `<adaptive-icon>`: the launcher icon's ground
(`ic_launcher_background`, written by `tools/draw_launcher_icon.py`) under the page and plus in
white (`ic_shortcut_new_event_foreground`, its 24 units at 55% of the 72 that show, the share it
had on the disc), the glyph again as the themed layer. `NewEventShortcut` loads it by its
resource as before. The family's rule for the launcher's shortcuts is now the white glyph on the
app's colour, in the launcher's own shape.

**Checked.** The layers drawn in a round, a rounded-square and a squircle mask and as the themed
icon; `spotlessCheck`, `:app:checkForbiddenPermissions`, `:app:assembleDebug`, `:app:lintDebug`.
On the phone: the Open list.

### The adaptive shortcut checked on the phone, and Tetto in the family (owner, 9 Nov 2026)

The owner checked the adaptive shortcut on the phone ("Ho fatto tutti i test sul telefono ed è
tutto ok. Puoi chiudere tutte le caselle."); the box in §15's Open list is ticked. Asked in the
same message ("Hai allineato anche i documenti con il riferimento alla nuova app di famiglia
"Tetto" nei vari repo?"): Tetto (budget) is the family's fifth app, so the README's «The family»,
CLAUDE.md's opening and «The family in the code», and VISION's opening and its readers who want a
matching set of widgets name it beside Chiaro, Passo and Saldo. VISION's naming table («Consistent
with Chiaro, Passo and Saldo») is the 7 Oct 2026 decision and stays as it was written.

### Today reviewed: one clock, one "now", one ground (owner, 10 Nov 2026)

**Where it comes from.** The owner asked for a graphic review of the main screen ("Trasforma in
app premium da PlayStore. Review grafico della schermata principale. Niente rumore, solo valore
aggiunto."). The review was drawn on a canvas beside the README's two pictures of Today, each
change named, with the three that reverse earlier decisions set apart; the owner approved all of
it ("Approvo tutte le tue scelte e i punti che cambiano le decisioni già prese. Implementa.").
The rule under every change: one thing says one thing once.

**What changed, and what it changes.**

- **One clock.** The figures are regular, no longer bold (`heroNumber`, −0.03em), and the page's
  face is drawn finer than the card's (`DayDial`: ring 0.058 of its side, hands 0.036 and 0.026;
  `DialGeometry` still gives the card its own and both their lengths). In bold beside a ring as
  heavy, the hero was two clocks of one weight. This changes 9 Oct 2026's "the «In words» card's
  dial" (archive 01): the same face, not the same weight. The guide's example follows, being the
  same composable; the widgets are untouched.
- **No face with nothing on it** (reverses "the face works without the calendar, its ring just
  empty", 9 Oct 2026, archive 01). The dial is drawn while today has an arc for it (`heroArcs`);
  in the evening, on an empty day and in every state without an agenda the figures stand alone.
  It is the house rule, "a section with nothing to say is not drawn", applied to the hero. The
  first day's tip explains the dial, so it waits for a day that shows one.
- **The sentence, then the alarm.** The alarm line and the second clock stood between the time
  and the sentence; they are under the sentence now. The sentence is `headlineSmall`, its first
  clause in ink and medium, the rest quieter (`AgendaText.sentenceText`, `SentenceText`: the
  clause before the count, or before the semicolon where today's clause ends and tomorrow's
  starts). The words are unchanged.
- **"Now" once** (changes `Timeline`, `:core:domain`). An event under way was said by the "now"
  line, its ground, its bar and its words. `Timeline` gives the line only while nothing is under
  way; the event under way is "now" itself, on the page's one ground (`surfaceContainer`, the
  group's 24 dp corner), with a finer bar. The accent on the page now means now and nothing else:
  the free row's plus is quiet.
- **No other ground** (reverses the day cards of 9 Oct 2026 and the all-day chips, archive 01).
  A day ahead is its name, its summary under it and its rows, under a hairline (`DayAhead`); an
  all-day event is its calendar's dot and its title (`AllDayMark`), still a touch of 48 dp. A day
  on a card with chips inside it was three grounds deep. Every row keeps one edge (20 dp) and one
  column for titles: the free row, the "earlier" row and a later day's dot stand where the bar
  does.
- **Times.** A row's start sets its day marker small and quiet on the 12-hour clock (the locale's
  own text, order and space); the times' column is 68 dp there, from 80. The free row writes no
  time: it starts where the row above ends, and TalkBack is still told.
- **The button is its icon** (reverses "a label at the top, its icon alone once scrolled",
  step 8.6). The label covered the last rows of the first screen for a button learnt the first
  day; its description is unchanged.
- **No glow.** The hero's gradient is gone, on a phone and across the two panes (ADR 0004,
  decision 6, amended there).

**Not changed.** The engine, the sentence's words, the widgets, the two dresses, the settings.
No string was added; the guide's Today section says the page as it is now, in both languages.

**Checked.** `TimelineTest` (the line with and without an event under way), `TodayScreenTest`
(the face there and gone, the order of the hero, the line at noon, the button, the tip waiting
for the dial, both panes), every page walked at twice the text size, in the dark, on a foldable
and a tablet, the screenshots looked at; the README's three pictures of Today redrawn. On the
phone: the Open list.

### Today on the owner's phone: broken lines, the chevron, the status bar (owner, 10 Nov 2026)

**Where it comes from.** The owner's screenshot of the reviewed page, in Italian and in the dark,
scrolled to the days ahead ("Controlla bene gli a capo involontari e le frasi spezzate male, ad
esempio la riga "Da giovedì 12...". Anche il triangolino in quella riga è allineato/posizionato
male rispetto alle altre righe. Analizza la schermata e vedi se c'è altro da sistemare."). It
follows «Today reviewed», above, and corrects what that entry built.

- **A day's name and its chevron.** "Da giovedì 12 novembre a sabato 14 novembre" took two lines
  and left "novembre" alone on the second, and the chevron, a neighbour of the text in a row, went
  to the page's far side. The chevron is now set in the line, after the name's last word
  (`DayAhead`, inline content), and the name breaks balanced.
- **"Tutto il giorno" in the times' column** broke in two on every all-day row of a later day
  (56 dp on the 24-hour clock). A later day's all-day events are drawn as today's and tomorrow's,
  a dot and a title (`AllDayRow`); the day's summary already says they are all-day, and TalkBack
  still hears it. Their title takes two lines before it is cut ("Carlos Pineda Perez -
  complean…").
- **"fino alle 06:30" in the same column**, for an event begun the day before, broke the same
  way (found reading the code, not in the screenshot): it is a note under the title now, and the
  column is empty there (`CompactRow`, now for timed events only; its title takes two lines).
- **The page under the status bar.** Scrolled, the sentence went up behind the system's clock and
  icons and was read through them: with the glow gone nothing parted the two. The lists end under
  the status bar (`statusBarsPadding` on the list, and on both panes); the reveal's arithmetic no
  longer counts the bar.

**Checked.** `TodayScreenTest`: the days ahead in Italian (no "Tutto il giorno" as a text, the
run's name whole, the screenshot looked at), every walk again. The README's pictures redrawn.

### Today's vertical rhythm, and the sentence's size (owner, 10 Nov 2026)

**Where it comes from.** The owner, with the page right on the phone ("Ora va bene."), asked
whether the rows' vertical spacing is right or room can be recovered within Material 3's
guidance, keeping the page premium, and to check the sizes of the clock and the sentence.

**What was measured** (411 dp wide, the tests' phone). The rows are already at or under
Material's list heights and cannot give more without losing the 48 dp touch: a one-line row
(all-day events, "earlier", free, a later day's event) is 48 dp where Material's is 56; a two-line
event is 61 dp where Material's is 72; the event under way keeps 14 dp inside its ground. What
could be recovered was between the blocks and in the sentence.

**What changed.**

- **The sentence is `titleLarge` again (22/28), regular, its first clause medium and in ink.**
  «Today reviewed» (above) had set it at `headlineSmall` (24/32): with the first clause told
  apart by weight and ink it does not need the size, and a long sentence in Italian took four
  lines, 128 dp of the first screen; it takes three or four of 28 now, and most days two.
- **The clock stays at 80 sp, regular**: 3.6 times the sentence, the one large thing on the page;
  it already shrinks to fit beside the dial and is capped at 104 dp for a large text size.
- **Gaps**: the sentence 12 dp under the clock's row (16); nothing added under it before the
  alarm (4 dp less); the hero keeps 16 dp under the alarm line only where that line is a door (its
  touch must stand clear of the all-day events'), 8 dp otherwise; a day ahead starts 8 dp under the
  rows before it (12).

About 20 dp recovered on the first screen on a usual day (measured: the event under way
stands 16 dp higher, the days ahead 20), more on a long sentence; one more row of tomorrow is in
sight.

**Checked.** `TodayScreenTest` whole (touch targets at every walk), the screenshots looked at; the
README's pictures redrawn.

### The dial: a cut arc fades, and the face stays while today has events (owner, 11 Nov 2026)

**Where it comes from.** The owner, at 00:55 on the phone, with today 10:00 to 22:00 («Ordinare
regalo») and 16:00 to 17:00: the ring drew 10 o'clock to 12:25 in the accent and nothing of the
visit. Asked whether the window could run from now to midnight, so that the day's events are
drawn whole, and at what cost; and whether the face could always be drawn, its ring empty.

**Weighed.** From now to midnight is up to 23 hours on a face of twelve: before noon an hour is
two places at once, and the 16:00 visit would sit at 4 o'clock, three hours ahead of the hand, read
as 04:00; the twelve-hour event would close the ring and meet the hand, read as now. These are
the faults the window was made against (§7; «The dial shows today only», 5 Nov 2026). After
12:30 the window already reaches midnight, as it carries today's events only: the change would
have touched the morning alone, where it cannot work. No battery cost either way (the arcs are
computed at redraws that happen anyway; the window's own boundary, `comesOnTheFace`, would have
gone). A 24-hour face (the card's hands are the system's `AnalogClock`, twelve hours only: the
app and the card would be two clocks) and two rings (too much for a cell) were set aside. The
owner chose (A), the window kept and its cut said, and the new rule for the face.

**What changed.**

1. **A cut arc fades out at the cut end** (`DialArc.cutAtStart`, `cutAtEnd`; `DialFade.stops`,
   pure and shared by `DayDial` and `WidgetDial`). Over at most 45 degrees (an hour and a half),
   and at most 80% of the arc between its fades, so a short one keeps some ink. The end is cut
   where the window closes first (early in the day), the start where the event under way began
   more than six hours back: both say the event goes on, where the arc ended as if it did. The
   gradient is a sweep gradient, the face turned so that it runs along the arc in one piece.
2. **The face stays while today has a timed event left**, even when none is on its ring yet
   (`heroArcs`: null, no face; empty, a face with an empty ring). This changes 10 Nov 2026's «No
   face with nothing on it» («Today reviewed» above): the face was drawn while it had an arc, so
   with only an afternoon event it was missing until the event came onto it (at 04:31 for a
   16:00), and the page changed shape for no change in the day, saying "nothing" beside an agenda
   with something in it. Once the day is done, and in every state without an agenda, the time
   stands alone, as it did. The first day's tip still waits for an arc to explain.
3. **Words**: the empty face says why to a screen reader (`today_dial_later`: "today's next
   event is more than eleven hours away and not yet on its ring"); the guide's paragraph on the
   dial says the ring's hours, the fade and the empty morning; VISION (Today, the dial),
   `CHANGELOG.md` [Unreleased]. The «In words» card already kept its face with nothing on it;
   only the fade reaches it.

**Checked.** `WordsTest` (the owner's morning: 10:00 to 22:00 at 00:55 cut at its end, at 17:00
at its start, an event drawn whole cut at neither; the early cut and the night the clocks go back
now cut at the end; `DialFade`'s stops), `TodayScreenTest` (the face with an empty ring at 02:00
and its description; the cut arc at 00:55, screenshots looked at; the tip waits for an arc). On
the phone: the Open list.

### The first run, reviewed with the family's (owner, 12 Nov 2026)

**Asked.** A review of Tempo's, Tetto's and Saldo's first runs, Passo's as the model: are their
words true to the app as it is today, and are their look, layout, type and motion one?

**Found.** The frame was already Passo's (18 Oct 2026, archive 02): the same page, centred in the
room above the buttons, the same type steps (`displaySmall`, `titleMedium`, `headlineMedium`,
`bodyLarge`), the same 300 ms slide and fade, the 100 ms fade under reduced motion. What
differed was in the words.

**What changed.**

1. **The buttons speak as Passo's**: «Get started» / «Iniziamo» on the welcome (was «Start» /
   «Inizia»), «Continue» / «Continua» (was «Next» / «Avanti»), and the quiet way past «Later» /
   «Più tardi» (was «Not now» / «Non ora»). Tetto and Saldo take the same three.
2. **The step in Italian is «Passaggio 1 di 3»** (was «Passo 1 di 3»): Passo's own word, and
   «Passo» is also a sister's name, which TalkBack would read on every heading.
3. **The battery promise said "never on a timer" and "when your calendar changes"**: true of the
   app, but a widget also redraws when an event starts or ends (§7, the boundary alarm). It now
   says so: «Tempo works while you look, and a widget only when your calendar changes or an event
   starts or ends», with "nothing on a timer" as the guide's battery chapter says it.

The other words were checked against the app and hold: the calendar's page (read-only, `READ_CALENDAR`
alone, allowed later from Today's card or from Settings), the two widgets and their system clock,
the card placed or added by hand.

**Checked.** `OnboardingScreenTest` (the calendar's page moves on with «Continue»);
`onboarding.png` regenerated (its button reads «Get started») and looked at; the README's alt text
follows. `CHANGELOG.md` [Unreleased].

### The first run's lines balanced, in the four apps (owner, 12 Nov 2026)

**Asked.** After the review above, the owner: if a word alone at the end of a line should be
fixed in Passo too, it is fixed in all four repositories. It should: the first run's texts are
centred and short, where «…niente da / sincronizzare.» or «…accesso / a internet.» reads as a
slip; the budget app already broke them balanced.

**What changed.** `:core:designsystem` gains `balanced()` (`LineBreak.Heading`: lines of about
one width, never a word alone on the last), and every centred text of the first run takes it: the
titles, the sentence, the promises, the bodies, the line under the calendar's button, the widget
page's notes. Only the first run: the rest of the app keeps its breaks until a review of its own
looks at it screen by screen.

**Checked.** The onboarding's tests and their screenshots (Italian, twice the text size);
`onboarding.png` regenerated and looked at.


### The family's widgets in order: «At a glance», 4×1, the app's colour, a seventh colour (owner, 15 Nov 2026)

**Where it comes from.** The owner, putting the family's widgets in order: in the four apps the
card called «A colpo d'occhio» / «At a glance» is the one with a picture (the weather app's glyph
card, Passo's ring, here the dial), and «In parole» / «In words» the one in type alone; Tempo's
«In parole» is the dial card, so it is renamed. Every «A colpo d'occhio» comes first in the
launcher's list and opens at 4×1. Every widget of every app wears its app's colour by default.
Then a seventh card colour, one that stands apart from the six, in their style, no shade of violet.

**Decided and built.**

1. **«In words» is «At a glance» / «A colpo d'occhio».** The labels, the onboarding's and
   Settings' rows (now «At a glance» first, as in the launcher's list), the guide (its «Two
   widgets» names the dial first; Today's dial paragraph), the comments, the test names, README,
   VISION, CLAUDE.md, §5-§7 and ADR 0003 (amended). §15's entries above keep the names of their
   time. The code keeps `WordsWidget`, `WidgetKind.WORDS`, `widget_words_*` and the `words/`
   package: a placed card is bound to its receiver's class name.
2. **First in the list.** Launcher3 sorts an app's cards by label (`WidgetItemComparator`, the
   collator), so a manifest order alone does not decide it; the receiver is declared first anyway
   (`WidgetProviderTest`, «at a glance is declared first»), for the launchers that keep it. In
   Italian «A colpo d'occhio» sorts before «Agenda»; in English «Agenda» sorts before «At a
   glance», and the owner chose to accept that rather than rename «Agenda» (the question's option
   (c), with (a): the Italian label «A colpo d'occhio» in the whole family).
3. **4×1.** `widget_words_info.xml`: `targetCellHeight` 1 and `minHeight` 40 dp (it was 4×2, 110
   dp). «Agenda» keeps 4×2. The two share their minimum and no maximum, no longer their default:
   `WidgetProviderTest` says so. The static preview (`widget_words_preview.xml`) is redrawn as the
   one-row form (the dial at 56 dp, the title at 20 sp over when at 16 sp, the date at the far
   edge over two lines); its tall form's note string went with it. The generated previews already
   offered 4×1.
4. **Terracotta by default** (`WidgetLook.cardColor = CLAY`): the family's six have no coral, and
   the terracotta (`#7A3320`) is the icon's coral darkened to carry white ink, the same hue. A card
   placed before and never configured takes it too, its colour having been the default and not a
   choice. `widget_preview_card` follows, and `WidgetProviderTest` reads it from the default look.
5. **The ochre** (`#5C4A0E`, `WidgetCardColor.OCHRE`, «Ochre» / «Ocra»): the family's seventh,
   30 ΔE from its nearest (the terracotta), white ink 8.6:1, the 75% ink 5.7:1, the 85% one 6.8:1
   (the weather app's DESIGN §2.6 is the family's table). Seven is where the list stops: the owner
   asked whether a bright series or a scrolling strip would be better, and both were declined (a
   bright ground needs a dark ink measured on every mark the cards draw; a strip that scrolls hides
   colours).
6. **The strip of colours starts under the radio buttons** (10 dp from the edge, the first disc on
   the buttons' line; it started under their labels at 50 dp): seven 48 dp targets want 336 dp: in the settings group (16 dp margins), less the strip's
   own 12, a 384 dp phone leaves 340 and holds them in one row; a 360 dp phone leaves 316, and
   `swatchesPerRow` breaks them into four over three, never one alone (with six, the strip under
   the labels already broke there, five over one). Tested at 384 dp (one row) and in arithmetic.

**Checked.** The widget module's tests (the provider's, the look store's, the config screen's at
384 dp, the gallery), the onboarding's and Settings' tests, spotless. `widgets.png` (now the
one-row «At a glance» on top, a pair in ochre, «Agenda» at the bottom) and `widget-settings.png`
regenerated and looked at.

### The first run's widget page shows the cards (owner, 15 Nov 2026)

**Where it comes from.** The owner, after the budget app's first run gained both cards drawn from
the reader's own caps: "bring the previews to Tempo too, so the two match".

**Decided and built.**

1. **Each choice draws its card**, between its line and «Add»: the card as the reader's own day
   will stand on it, at its default size (the dial card at 4×1, «Agenda» at 4×2), on the default
   look (the app's terracotta), on the group itself (see below). The onboarding module cannot see `:widget`, so the page takes the drawing as a
   slot (`OnboardingRoute(widgetPreview)`), and `TempoRoot` passes `HomeWidgetPreview`.
2. **Read without arming.** `WidgetModelLoader.loadUnplaced()` reads the day as a placed card
   would, guarded by the cards' timeout, on `WidgetLook()`, and arms no boundary alarm: that
   alarm is a placed card's (§9). A permission refused or no calendar shown is drawn as the card
   would say it.
3. **The widget settings open on the card's own default size**: `defaultPreviewSize(kind)`, 4×1
   for «At a glance» since its provider moved there today (it opened on 4×2 for both).

**Checked.** `OnboardingScreenTest` (each choice holds its picture, «At a glance» above, with a
stand-in card), `WidgetConfigScreenTest` (4×1 chosen on «At a glance», the two previews drawn from
a sample day, `first-run-previews.png` looked at), the app's tests, spotless.

**Then, the same day: no stage under the cards** (owner: the ground round the cards, a wallpaper
in the containers' colours, sat badly with the card's colour). On the first run the card stands
on the choice group itself: the default card is solid, so nothing shows through it, and the stage
was a third ground between the page and the card. The widget settings keep their stage, where
the opacity slider makes it the wallpaper the card thins over.

### The month, and the time's door (owner, 23 Nov 2026; Phase 9)

**Where it comes from.** Two proposals of the owner's, with "tell me honestly whether each is
valid, and if you have a better idea say it", and "only what adds real value, no noise".

**1. A touch on the time opens the clock app.** The owner proposed the touch on the analog face,
with a switch in Settings, on by default.

- *Valid, with one change:* the face is drawn only while today has an event left (§15, 11 Nov
  2026), so a door on the face alone would be gone every evening, when an alarm is set. The door
  is the whole time row, the figures and the face, one touch target read once by a screen reader
  ("10:20, clock face at 10:20…, opens the clock app"). It pairs with what the page already does:
  the date opens the calendar, the time opens the clock.
- *Built:* `TimeRow` in Today's hero; `UserSettings.timeOpensClock` (`time_opens_clock`, true);
  the switch «Touch the time» in Settings' Clock group, not drawn where the phone has no clock
  app (`SettingsUiState.hasClockApp`), since it would switch nothing. `CalendarDoors` (which holds
  the clock app's package) moved to `:core:calendar`. With no sentence between them (no calendar
  to read), the alarm's touch would have run into the time's: the alarm line stands 8 dp lower
  there (found by `assertAccessible()`: 46 dp for a finger).
- The time and the face are inside one touch target now, so Today's tests find them in the
  unmerged tree.

**2. The month.** The owner proposed a calendar view beside the agenda, month only, reached by a
bottom bar as the step counter's, a touched day opening a bottom sheet (or, on an empty day, the
new event at once), with a way between months, a quick way back to the current one and a quick
way to a far one.

- *Valid, and against a non-goal:* VISION left "week and month grids" to the calendar app. That
  line was right about a grid of titles to edit in, and wrong about the month as a glance. VISION
  is rewritten; ADR 0005 holds the reasons and the ten decisions.
- *Two things proposed otherwise, asked of the owner with both drawn, and chosen by the owner:*
  (a) **no bottom bar**: with two destinations it would take about 80 dp from Today's first
  screen, every day, for a page opened now and then; Today's calendar button, a duplicate of the
  touch on the date, opens the month instead; (b) **the chosen day under the month, in the page**,
  not in a sheet: the grid takes half the screen and the half under it would be empty until a
  touch, and one touch would do two different things (a sheet, or another app) depending on a dot.
  A touch chooses; the button, or a long touch on a date, starts a new event there.
- *Added beyond the request, each because it answers "which days are taken":* a bar for an event
  of whole days, running across them, so a week away reads as a week (two lanes; `MonthMarks`);
  dates gone by drawn quieter; the dates of the months beside completing the weeks, so no row has
  a hole; the grid as tall as its month, its height following the finger between months;
  "Today" drawn only while the page is elsewhere; the event made in the calendar app brought
  forward on return, as on Today.
- *Left out as noise:* week numbers, a year view, a heat map, a month widget, a search of events.

**Decided in building.**

1. **One drawing of an event.** The month's chosen day needed Today's rows, and a feature module
   cannot depend on another: `AgendaEventRow`, `AgendaAllDayRow`, `DayHeading`, the times'
   column and the glow's local moved to `:core:designsystem` (`AgendaRows.kt`), their words to
   `strings_agenda.xml` (`agenda_*`, `a11y_*`, `state_*`). Today's file lost four hundred lines
   and draws as before (its 86 tests, its screenshots looked at).
2. **A span of days is a bar on all its dates, a timed one too.** A conference from Monday 9:00
   to Wednesday 17:00 is in Tuesday's all-day row and has times on Monday and Wednesday: on the
   month it is one bar over the three, not a dot, a bar and a dot. A night that crosses midnight
   covers no date whole and is a dot on each.
3. **A new event on another date starts at the time of day the next half hour has now**
   (`newEventOn`), as calendar apps propose; late in the evening, when that time falls on the date
   after, at 9:00, and so where the date's clock skips it.
4. **The reveal looks only over the dates held when the reader left** (`Awaiting.span`): turned
   to another month, every event of it would be new.
5. **The arrows stand where the top row has room** (380 dp at the usual text size): on a phone
   with large text the finger turns the months, a screen reader the pager, and the month's name
   reaches any month.
6. **`show_calendar_button` keeps its key and hides the month's door.** Removing a switch the
   owner asked for (9 Oct 2026) is the owner's to say: asked in Open.

**Checked.** `:core:domain` (the builder's span, the grid, the pages, the marks with their lanes,
the new event's start), `:feature:today` (86), `:feature:month` (24: the page on states, both
languages, both themes, twice the text size, an open foldable, a phone on its side, a tablet in
two panes, each with `assertAccessible()`), `:feature:settings`, `:feature:guide`, `:core:data`;
then the whole build's `spotlessCheck`, `test`, `:app:lintDebug`,
`:app:checkForbiddenPermissions` and the debug APK. The month's screenshots looked at
(`feature/month/build/screenshots`); `month.png` added to the README, `settings-calendars.png`
and `guide.png` regenerated. **Not checked: the phone.** No device was on adb: the calendar apps'
answer to a new event on another date, the pager under a real finger, the long touch's haptic and
the Clock app from the time row are the Open box's.

**Then, the same day: the date says it is a door** (owner, on reading the change: "did you remove
the calendar button? I still want a point that opens the calendar app; tell me honestly what you
suggest, a premium solution"). The button was not removed but given to the month, and the calendar
app's doors left were the date, a day's name and the month's chosen day: none of them a button,
and the date the only one with no sign of being one. Proposed, with the alternatives drawn (a
third button in the top row; both), and chosen by the owner: **the date wears the chevron the
days' names wear**, so one sign means one thing on every page, a day that opens the calendar app
on itself. A third icon would have narrowed the date (three buttons take 144 dp of a phone's
row), set two calendar pictures side by side, and brought back the duplicate. Built as `DayName`
(`AgendaRows.kt`), which `DayHeading` now uses too; a screen reader still hears the date alone.
No chevron where no calendar app takes the day. `today.png`, `today-evening-dark.png` and
`today-tablet.png` regenerated.

**Then, the same day: the day marker cut beside the dial** (owner: "fix the cut AM in this branch
too"). Found while looking at the regenerated `today.png`, and already on `main`: on the 12-hour
clock, on a phone 393 dp wide, "AM" lost its second letter beside the dial. The clock's figures
are drawn one by one, each in its own place so that only the one that changed rolls, and were
measured whole: a text's tracking is not applied at the two ends of a line, so five figures apart
keep none of the hero's −0.03 em and stand about 10 dp wider at the clock's size than "10:20"
measured as one text. The fit was reckoned on the narrower figure, and the row took the marker's
room. `clockWidths` now measures the figures as they are drawn, one by one; the dial's size on a
large screen, reckoned by the same function, is unchanged (200 dp on the tablet). A test at the
README's size looks at the marker's last column of pixels: ink there is a letter cut. It failed
before the change. `today.png` and `today-tablet.png` regenerated and looked at.

### Phase 9 checked on the phone (owner, 23 Nov 2026)

The owner installed the APK the CI built from the branch (`claude/month-and-clock-door`, commit
`453841a`: the month, the time's door, the date's chevron, the day marker whole) and reported:
"everything works, the new event on a date other than today too". That last was the one answer
only a phone could give: §4.7's `ACTION_INSERT` had been measured with today's dates alone
(`docs/device-checks/calendar-intents.md`), and the month hands the calendar app another date
(`newEventOn`). Step 9.6 and its box in Open are closed on that word. It is a word on the whole:
the nine checks were not answered one by one, and the ninth (`dumpsys alarm`, `dumpsys
jobscheduler`), which needs adb, stays with 1.1's battery check in Open; the month adds no alarm,
no job and no service, so there is nothing new of its own to find there. Still open: whether the
month button keeps its switch.

### The month's button has no switch, and nothing is logged to a file (owner, 23 Nov 2026)

**The switch goes.** The owner, on the question left in Open: "maybe better to remove it; leaving
the button always on the main page is fine". Agreed, and for the reason the question gave: the
switch was made for a button that doubled the touch on the date (9 Oct 2026), and the button is
now the month's only door. Turned off it would leave the month out of reach, and a reader who
had it off before the update would not find the month at all. Built: Today's button is always
drawn; `UserSettings.showCalendarButton`, its row in Settings and its words are gone; the stored
key `show_calendar_button` leaves the settings file at the next write (`Keys.RETIRED`, with its
test), so the file holds only what is read. §5's table loses the row; ADR 0005's consequence and
VISION are rewritten. This changes the entry of 23 Nov 2026 above, decision 6.

**Logs, asked by the owner** ("I cannot do the adb check for now; what matters is that the app
does not log too much, and that no log grows without end: it must stay as a production app").
Looked at, nothing to change:

- **Tempo writes no log file.** No file is opened for writing anywhere in the shipped code but
  the two DataStore files: `settings` (only what differs from a default) and `widgets` (one look
  per placed card, removed when the card is). No database of Tempo's, no cache of events. Neither
  file grows with use.
- **What is logged goes to Android's own log**, a ring of fixed size the system keeps in memory
  and overwrites: it cannot grow, and it is not in Tempo's storage. Eight lines in all, in
  `:widget` alone (tag `TempoWidget`): a card's refresh (what asked, how long the read took), a
  resize, and the failures (a read timed out, an alarm or a trigger not armed). A few lines per
  calendar change, none while nothing changes, none from Today, the month, Settings or the guide.
  They carry widget ids and milliseconds, never an event's title, place or time.
- **WorkManager's own database** (Glance's sessions and the calendar's trigger run on it) prunes
  its finished work by itself; Tempo keeps one waiting trigger at a time (§4.5).
- **The crash page** keeps nothing either: the trace travels in the intent that shows it, capped,
  and the page is in debug builds and the debug-signed release only, never in a release signed
  with the release key.

The adb check of §9.6 (`dumpsys alarm`, `dumpsys jobscheduler`) stays in Open with 1.1's battery
check: it measures alarms and jobs, which Phase 9 did not touch.

### Today has two grounds: the graphic review (owner, 26 Nov 2026)

**Where it comes from.** The owner asked for a review of the whole app's graphics and UI/UX
("Attualmente è funzionale e pulita ma mi sembra un po' piatta dal punto di vista grafico. […]
Solo modifiche o suggerimenti di nuove feature che danno valore all'app e non rumore."), with the
sisters' repositories at hand. The review was drawn as a page beside the README's pictures: the
finding, eight points each with what it costs and which decision it changes, and what was left
out. The owner approved all eight ("Approvo tutti i punti.") and asked for a phase in steps, each
one session's work: §11, Phase 10. Nothing is built yet; this entry records the decisions, and
each step adds what it measured.

**The finding.** «Today reviewed» (10 Nov 2026, above) took away the day cards, the all-day chips
and the hero's glow, each for a reason that stands: a day on a card with chips in it was three
grounds deep. It went as far as none: the one ground left, the event under way's, is 1.1:1 from
the page (`#E6EFF9` on `#F6FAFF`), the colour on the page is in bars of 4 dp and dots of 8, and
the one raised thing, the button, is the page's palest (`primaryContainer` on the page, 1.2:1).
The sisters' main pages have a layer each (Passo's ring and cards, Chiaro's sky and its sheet);
Tempo's is one column of words. The answer is not to put back what was taken away but one ground
where the page has two subjects, the clock and the calendar.

**Decided** (the numbers are the review's; the steps are §11's).

1. **The clock has its ground** (step 10.2). The hero on `surfaceContainer`, to the screen's top
   edge, the dial's face filled with the page's surface. *This changes «Today reviewed», "No other
   ground"*: the page has two, told apart by what they mean (the clock's, and now's), not one. It
   does not bring back the glow: a flat tone, the one a top bar takes when a page scrolls. It also
   ends what «Today on the owner's phone» (10 Nov 2026) had to patch: the list cut off under the
   status bar with nothing between them. ADR 0004 is amended when it is built (the leading pane is
   the ground).
2. **"Now" in the accent** (10.2). `primaryContainer` under the event under way, a bar of 6 dp.
   This keeps 10 Nov's rule, "the accent means now and nothing else", and makes it seen: computed,
   to be pinned in `ContrastTest`, the accent on that ground is about 5:1 in the light and the
   words about 7:1 in the dark.
3. **The button in amber** (10.2). With 2 it would have had now's colour without being now; in
   `secondaryContainer` it is the page's one warm note, as the amber clock is the launcher icon's,
   and the colour the month already gives the chosen day. One of Material's own colours for the
   button.
4. **A day's name larger, no hairline** (10.2). The name was 16 sp semibold over rows of 16 sp
   medium, and needed a line to be a heading; at `titleLarge` the line says what the size says.
   *This changes «Today reviewed»*, which drew a day ahead "under a hairline".
5. **A compact bar once the hero has left the screen** (10.3): the date, the time, the two
   buttons. A clock whose time leaves with the first scroll, and whose only doors to the month and
   the settings leave with it. Chiaro does the same with its temperature. It costs the first
   screen nothing.
6. **Today's rows move** (10.1), as the month's do since Phase 9.
7. **The month's title is never cut** (10.1). A fault, not a taste: at 411 dp, away from the
   current month, "Today" appears and the title reads "November 20…" (`month-empty-day.png`): the
   year goes when it is needed. "Today" becomes an icon, the calendar with the day's number.
8. **On a large screen, the month under the clock** (10.4, 10.5). The hero's pane is half empty
   on the tablet; the month fills it with something the page is for. Not on a phone, where it
   would take the first screen from the agenda. The dearest of the eight: the grid's drawing moves
   to `:core:designsystem` (feature modules do not depend on each other) and Today reads a month's
   marks, on a large screen and while visible only. ADR 0004 amended.

And one refinement outside the eight, in 10.1: the launch's ground set to the page's.

**Weighed and left out, as noise.** A hero that changes with the hour (the glow again, and the
time said twice). A card for every event, or Chiaro's segmented groups for the agenda (8 to 10 dp
an event, a row less on the first screen, and now lost among ten grounds; the family's decision on
the groups stays where §11 leaves it). Events filled with their calendar's colour (the invariant:
colours are data). The icon's coral as the accent in the app (it meets the red and orange
calendars as the azure meets the blue ones, and the scheme is the family's, generated; point 3
brings the icon in without it). Material 3 Expressive's wavy progress (a movement without end for
a figure that changes each minute). The days ahead closed to a line each (it hides the titles,
which are what an agenda is opened for). Filling an empty day (half a screen of nothing on a day
with nothing is true).

**Not changed.** The widgets (already the strongest drawing the app has), Settings, the guide, the
first run, the type scale, the sentence, the two dresses, the engine.

**To measure when built** (the review's figures come from a drawing in HTML, not from Compose):
where the event under way stands with the hero's ground (drawn about 6 dp lower), that a day's
larger name is paid by its hairline (drawn level), the contrasts above.

### Step 10.1 built: the month's title whole, Today's rows moving, the launch's ground (26 Nov 2026)

Built on `claude/today-two-grounds`, as §11 says, with what building it found.

- **The month's title.** "Today" is `TodayIcon` (`MonthScreen.kt`): `TempoIcons.CalendarPage`, a
  new drawing (the calendar's frame and rings with no line under them, so a number has room), and
  today's number over it in the accent, 9 dp high and set in dp, so the icon stays an icon at any
  text size; TalkBack hears "Today". Four tests draw September, the year's longest name, with
  another day chosen, at 411 and 360 dp, in English and Italian, and read the title's own text
  layout: one line, not ellipsized, its last letter within the room given. Run against the old top
  row, the 411 dp English one fails (the cut the review saw); it passes now. At 360 dp the name
  takes 508 px of 531: whole, with 8 dp to spare, so a longer name in a third language would need
  looking at again. `TextLayoutResult.didOverflowWidth` is true for a line that fits and is not
  used. The guide's line on the way back says the small calendar, not the word, in both languages;
  ADR 0005's decision 8 notes it. The README's picture of the month shows today chosen, where the
  icon is not drawn: unchanged.
- **Today's rows move.** `moving()` on `row()` and `card()` (`TodayScreen.kt`): `animateItem` with
  `TempoMotion.spatial` for the place and `effects` for the fade, no placement under reduced
  motion, the month's own values. The hero is not among them: it does not come or go. No key was
  changed: every item already had a stable one (the free row's is its end, which does not move
  with the minute). `TodayScreenTest` is green unchanged; a test cannot see a movement, so this
  one is for the phone (Open, check 7).
- **The launch.** `Theme.Tempo` sets `windowSplashScreenBackground` and `windowBackground` to
  `@color/launch_ground`, `#F6FAFF` and in `values-night` `#0D141B`: the Vivid dress's `surface`,
  copied by hand, with the comment that says so and names the scheme. Known and accepted: a reader
  on the Paper dress or on the wallpaper's colours sees the Vivid ground until the first frame,
  where before they saw the phone's white or black. The widget's settings page has its own theme
  (`Theme.Tempo.WidgetConfig`) and is not touched.

**Checked.** `spotlessApply`, `test` (every module), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green. The month's screenshots looked at
(`month-away`, new; `month-empty-day`; the top row at twice the text size, where the arrows and
the title already had room). Note for the next steps: Gradle's build cache gives back a passed
test task without running it, and then the screenshots on disk are an earlier run's; before
looking at them, run the module's `cleanTestDebugUnitTest` with it.

### Step 10.2 built: Today's two grounds (26 Nov 2026)

Built on `claude/today-two-grounds`, as §11 says. What each point became, what building it added
to the plan, and the measures the plan asked for.

- **The clock's ground.** `Hero` stands on `surfaceContainer` (`heroGround`, `TodayScreen.kt`). In
  the page's one column the ground is the hero's item, from the screen's top edge (the list no
  longer stops under the status bar: the inset is inside the hero), across the window whatever
  the gutter, its lower corners the scheme's 28 dp; 16 dp of it under the hero's last line and
  4 dp of page before the agenda's first row. On two panes it is the whole leading pane, behind
  the status bar and the gutter too, with a straight edge where the panes meet (ADR 0004, amended).
  *Added while building*: (a) a strip of the same ground stays behind the status bar when the
  hero has scrolled away, so the rows do not pass behind the system's clock (the fault of 10 Nov
  2026, which the list's padding had patched): at the top it is the hero's own ground and unseen;
  step 10.3's bar will stand under it. Robolectric draws no status bar, so no test sees it: check
  1 on the phone. (b) The reveal of a new event counts the status bar in the one column, where
  the list's top is now the screen's. (c) In the hero's pane, today's all-day events step their
  dots against the pane's ground (`AgendaAllDayRow`'s `ground`), and the first day's tip, a note
  on `surfaceContainerHigh`, a step from the pane's tone and lost on it, is drawn on the page's
  `surface` (`StatusCard`'s `onGround`).
- **The dial.** `DayDial` takes `ground`. Its face is a disc of the page's `surface` out to the
  ring's rim, so an arc has the face on one side and the ground on the other. A quiet arc is its
  calendar's colour stepped to 3:1 against the ground and drawn over the face as faint as that
  ratio allows (`dialMark`): `markAlpha` gained `over`, what the mark is drawn on where that is
  not what it is read against, with its tests in `:core:domain` (a mark fades more over the
  lighter face than over the ground, and the old arithmetic would have left it under 3:1 against
  the ground). An arc with no colour known, the page's ink at 45%, was 2.9:1 on the page before
  the step: it goes through the same function now. The guide's example stands on a plate of the
  same ground (`DialSample`), so it shows the dial Today shows.
- **"Now" in the accent.** The event under way on `primaryContainer`, its title and start in
  `onPrimaryContainer`, its quiet words (the end, the place, the notes) in `onSurfaceVariant`, its
  "40 min left" and its bar in `primary`; the bar 6 dp on a track of `primary` at 20%. The row's
  bar with no calendar colour, the outline's, was 2.95:1 on that ground in the dark: `CalendarBar`
  steps it as it steps a colour. The ground's inner top and bottom went from 14 to 12 dp, which
  pays the hero's 4 dp (below). In `AgendaRows.kt`: the month's chosen day follows.
- **The button** is `secondaryContainer` / `onSecondaryContainer` on Today and on the month. In
  the Vivid dress that is amber; in Paper, whose accent is amber, it is the dress's azure: the
  rule kept is the role (never now's colour), not the hue.
- **A day's name** is `titleLarge`; `DayHeading` draws no hairline and has no `divider`. Today
  gives a day 4 dp over its name's own 12 (`DAY_GAP`). The month draws its own line under the
  grid, in the day's first item (so the items the reveal counts are the same), and none where the
  day stands beside the month, where the list's top padding went from 4 to 2 dp to keep the day's
  name level with the month's.

**Measured** (411 × 891 dp, the sample's Wednesday at 10:20, in dp from the screen's top; before
is the commit of step 10.1).

| | before | after |
|---|---|---|
| The event under way, top and bottom | 378, 476 | 382, 476 |
| "Tomorrow", the name's top and bottom | 732, 756 | 727, 755 |
| Tomorrow's first event, top and bottom | 830, 891 | 829, 890 |

The event under way starts 4 dp lower (the drawing said about 6) and ends where it ended, its
ground 4 dp less high; tomorrow's name ends 1 dp higher (the drawing said level) and every row
after it is 1 dp higher. The first screen holds the same five events whole, the event under way,
the rest of today and tomorrow's first: pinned by a test. With no alarm line the hero's foot grew
from 8 to 20 dp (one measure for the ground's foot, with or without the line), and the same rows
stand 7 dp lower than before, the event under way 12: said here because the sample has an alarm.

**The contrasts**, the same in the two dresses to a tenth, light and dark. On the clock's ground:
the ink 14.8 and 12.7, the quiet ink 8.1 and 9.6, the accent 5.6 and 9.7. On now's: the title 13.3
and 7.3, the quiet words 7.3 and 5.5, the accent 5.0 and 5.5 (the review computed "about 5:1 in
the light"; the words in the dark are 7.3 as it said). The button's icon 13.3 and 7.2. The grounds
against the page: the clock's 1.11 and 1.13, a tone and not a colour, seen as a ground by its
size and its round foot; now's 1.23 in the light and 1.98 in the dark, where it is the loudest
thing on the page (check 2 asks whether too loud).

**Tests.** `ContrastTest`: the inks on the two grounds by name, every calendar colour as a mark at
3:1 on the page, the clock's ground and now's (thirteen colours and the outline, the four
schemes), and the dial's quiet arcs against the ground and against the face. `MarkColorTest`: the
two cases of `over`. `TodayScreenTest`: the four grounds read from the page's pixels (the hero's
corner, the page beside a row, the event under way, the button), in the light and in the dark;
the dial's face and what is around it; the hero's pane and the tip on a tablet; the first
screen's five events; a day ahead under its name; the year's longest run in Italian ("Da
mercoledì 14 ottobre a martedì 20 ottobre") at 360 dp, where it breaks in two halves, and at twice
the text size, where it takes three lines with no word alone. Every walk of Today, the month and
the guide run afresh and looked at: phone, dark, twice the text size, a phone on its side, the
foldable in one column and in two panes, the tablet on its side, upright and in the dark.

**The words.** The guide's line on the day in order says the event under way stands "on a ground
in the accent" («su un fondo nel colore d'accento»); the rest of the Today chapter reread in both
languages and left: it says nothing of grounds or lines. VISION's line on the timeline, the
CHANGELOG, CLAUDE.md's Design and ADR 0004 follow. The README's pictures of Today, the evening,
the tablet and the month are redrawn.

**Checked.** `spotlessCheck`, `test` (every module), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green.

### Step 10.3 built: the compact bar (26 Nov 2026)

Built on `claude/today-two-grounds`, as §11 says (owner, 26 Nov 2026: "continue Phase 10, build
step 10.3"). What the bar is, the one place where building it read the plan's words rather than
their letter, and what it added.

- **What it is.** `CompactBar` (`TodayScreen.kt`), in the page's one column only: the date in the
  reader's own style, a long one cut to its short names ("Wed, 7 Oct", "Mer 7 ott"; a numeric one
  as it is), the time in figures on one line (`titleLarge`, tabular; "10:20 am" whole on the
  12-hour clock), the month's and the settings' buttons. The date and the buttons are the hero's
  own composables (`DateDoor`, `PageButtons`, taken out of `Hero`) and stand where the hero's top
  row has them, so coming back up nothing changes place. The date opens the calendar app on today
  and wears its chevron; the time opens the clock app where the hero's time does (a clock app on
  the phone, and the reader's switch on); with no such app each is plain words, with no chevron.
  It stands on `surfaceContainer`, over the strip step 10.2 left behind the status bar, which
  stays: it holds the ground while the bar fades.
- **When it comes: the plan's "once the hero has left the screen", read as the screen the bar
  leaves.** The bar comes when the hero's foot has gone up under the bar's place (the hero's
  ground ends at or over the status bar's height plus the bar's, `heroAbove`), not when the
  hero's item has left the list. By the letter there was a stretch, the hero's last 48 dp and
  more, with the hero's time gone and no bar yet, a strip of the hero's last line under the status
  bar; and the bar then came over the agenda's first rows. Read this way the bar comes over the
  hero's own ground, the rows only ever pass under a bar that is there already, and the stretch
  with no time on the page is 48 dp shorter. It takes the hero's foot (the same 28 dp lower
  corners): at the moment it comes it is the foot's own outline, so what changes is the words.
  One line and two states, on a timed transition, never a bar that grows with the scroll: the
  family's lesson, from Chiaro's bar, which takes its temperature once the number has gone under
  it.
- **The fade** is 300 ms in and out; the theme's spring for an alpha, about a tenth of a second,
  reads as a snap on a thing as wide as the screen (measured on Chiaro's bar, on the phone). Under
  reduced motion a plain cut, as the plan says.
- **No timer** (§9): its time is the state's `now`, the page's minute ticker's. `TodayViewModel`
  is not touched.
- **A touch on the bar never reaches the row under it**: it is a `Surface`. Pinned by a test that
  brings the event under way half under the bar and touches the time where it is no door.
- **TalkBack hears the bar before the list**: its own traversal group, before everything else on
  the page. The list was not made a group too: that would send the new-event button after the
  list's last row, a week of swipes away, where today it is reached at the foot of each screen.
- **The reveal counts the bar** (`revealNewEvent`): the event made is brought a margin under the
  status bar and the bar, 48 dp until the bar is drawn and then its measured height.
- **At a large text size the bar grows, and is not capped.** At twice the text size on a phone
  411 dp wide the date takes two lines ("Wed, 7 / Oct") and the bar is 96 dp, two lines of the date; on a phone on
  its side everything is on one line and the bar is 48 dp of the screen's 411. Weighed and left
  out: capping the bar's text (as the hero's clock is capped): the reader who asked for twice the
  size would have the date at two thirds of their own text, to save 48 dp upright, where the
  height is there.

**Added while building.**

- **A day's name keeps its chevron.** At twice the text size the bar's date broke between its
  last word and the chevron, which stood alone on a second line. `DayName` now joins the two (a
  word joiner, U+2060): where a name breaks, the chevron goes with its last word. In
  `AgendaRows.kt`, so it holds for every day's name on Today and under the month, and for Today's
  own date, which is also balanced where it takes two lines ("Wednesday / 7 October" at twice the
  text size).
- **The accessibility check knows a bar over a page** (`COVER_TAG`, `:core:designsystem`;
  `assertAccessible`, `:core:testing`). A row half under the bar was measured as a control with
  half its height ("«Tomorrow 3 events, 09:00 to 18:00.»: 52 by 20dp for a finger": the walk
  failed, verified by taking the mark away). What a marked bar covers of a control is not on
  screen, as what a list's edge cuts: such a row is measured when it is scrolled clear, and one
  wholly under the bar is not there. The bar's own controls are measured as any other: each has
  its 48 dp.

**Tests** (`TodayScreenTest`, fifteen new and two grown). The bar gone at the top, there after
the scroll with its date, time and two buttons, 48 dp high at the screen's top, and gone again
back at the top; it comes within 4 dp of the hero's foot passing under its place, with the
agenda's first row still below it, and goes as the foot comes back; its four doors; a touch on it
that reaches no row; no calendar app and no clock app, and the reader's switch off; the time
moving with the state's minute; a numeric date on the 12-hour clock; Italian; its ground read
from the page's pixels, in the light and in the dark; the traversal group; the reveal a margin
under the bar; 80 ms after the scroll the bar whole under reduced motion and still fading
otherwise. A phone on its side ends its walk with the bar on one line, 48 dp; two panes have no
bar. Every walk of Today (phone, dark, twice the text size, a phone on its side, the foldable in
one column) now crosses the bar and was looked at.

**The words.** The guide's «The date and the time» says the bar keeps the two at the top with
the two buttons, in both languages; the CHANGELOG has its line; CLAUDE.md's Design and ADR 0004
(the bar is the one column's alone) follow. The README's pictures are unchanged: none shows the
page scrolled, and the set is kept small.

**For the phone** (Open, check 6, rewritten): Robolectric draws no status bar, so the bar under
the system's clock, and the seam or its absence between the strip and the bar, are seen there
first.

**Checked.** `spotlessCheck`, `test` (every module: 620 tests), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green. No phone on adb: the bar under a real
status bar waits for check 6.
