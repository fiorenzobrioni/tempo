# Tempo — Product Vision

> **Tempo** (Italian for "time") is the fourth of a small family of focused, single-purpose apps, alongside **Chiaro** (weather), **Passo** (step counter) and **Saldo** (personal finance).
> Name: **Tempo**, in the launcher and on GitHub Releases (the only channel for now). Repository: `fiorenzobrioni/tempo`.
> Package name: `com.callbackdev.tempo`, the `callbackdev` namespace shared with the sibling apps.
>
> **Status: reviewed by the owner** (7 Oct 2026). Written from the owner's concept document
> «Concept App: Tempo (Orologio & Agenda)»; what this vision changes in it, and why, is in
> [From the concept to this vision](#from-the-concept-to-this-vision), accepted in full by the
> owner; the questions the review settled are in [The owner's answers](#the-owners-answers).
> Development starts with PLANNING §11, Phase 1.

## One-liner

The time, the date and the day ahead in one calm view: a clock and the day's agenda, read from
the calendar already on the phone, in an app and in a home-screen widget that never scrolls.
Tempo reads; your calendar app writes.

## Why this app

The question "what time is it, and what is next?" is answered on most home screens by two
widgets from two apps that do not match:

- **A clock widget** that knows nothing about the day.
- **A calendar app's agenda widget**, styled for the calendar app rather than for the home
  screen: a scrolling list inside a page that already scrolls, rows cut in half at the bottom
  edge, a loading flash at every refresh.

The apps that join the two often bring their own account, their own sync, a permission to write
the calendar, or ads.

Tempo does one thing. It reads the calendars the phone already has (every account it syncs),
shows the time and what is coming in the family's look, and hands every change to the calendar
app the reader already uses.

## Product principles

1. **The phone's calendar is the only source.** Tempo reads the system Calendar Provider, which
   holds every calendar the phone syncs: Google, Exchange, a CalDAV account through DAVx⁵, a
   calendar kept only on the phone. No events of its own, no account, no sync, no network.
2. **Read-only, on purpose.** One calendar permission, `READ_CALENDAR`. Viewing an event in full,
   creating one and editing one are handed to the reader's calendar app through Android's own
   intents. Tempo cannot write the calendar, so it can never damage it.
3. **Private by design.**
   - No `INTERNET` permission, no location, no contacts, no analytics, no ads SDKs.
   - The events never leave the phone through Tempo: Tempo sends nothing.
4. **Battery first.**
   - The widget's clock ticks for free: the system's own `TextClock` draws it, with no work by
     the app.
   - The agenda changes only when the calendar changes, when an event starts or ends, and at
     midnight. No polling, no exact alarms, no periodic workers, nothing on a timer while the
     screen is off.
5. **One sentence before any number** (the family's rule). Before the list, the day in words:
   "Three things today; the next, Dentist, in 40 minutes." "Free until 3 pm." "Nothing left
   today; tomorrow starts at 9."
6. **Fits, never scrolls.** On the widget, what fits is shown whole and what does not is
   counted ("2 more today"), never cut in half.
7. **The screen does not lie.** A missing permission says so, and the clock still works. A
   calendar the reader hid stays hidden and the settings say how many are hidden. A past event
   looks past. An all-day event is on its own day whatever the time zone.
8. **One family.** Chiaro's design language, as Passo carries it: the same palettes, type,
   shapes and motion in the app, the same card, inks and colours on the home screen, so a Tempo
   widget beside a Chiaro or a Passo one reads as one set.

## Target users

- People who want the time and the day's agenda on one glanceable card on the home screen.
- People with several calendars (work, family, shared ones, a sports club's) who want one merged
  view without signing in to yet another service.
- Privacy-conscious readers and minimal home screens.
- Readers of Chiaro, Passo and Saldo who want a matching set of widgets.

## Scope for v1.0

### Today (the app's one main screen)

- **The clock**: the time large (the family's hero type, tabular figures so the minutes do not
  wobble, each figure rolling to its next value as the minute changes), the date above it in the
  page's top row, both in the reader's format. Digital (Key decisions), beside the dial.
- **The dial**: the «In words» card's clock face beside the time (the owner's review of 9 Oct
  2026): the next twelve hours' events as arcs on its ring, what comes next in the accent, the
  hands moving with the minute. When the page opens the hands wind forward to now and the arcs
  draw themselves, in one short movement.
- **The day's sentence**, before any list.
- **The next alarm**: "Alarm tomorrow at 7:00", from the system's own next alarm clock, which
  needs no permission. On by default, one switch hides it.
- **All-day and multi-day events** in a row of their own above the timeline ("Holiday, day 2 of 5").
- **The timeline of the day's timed events**: the time range, the title, the calendar's colour
  as a mark, the location as one line of text (no map). The event under way says how much is
  left ("ends in 25 min"), past events are quieter, and a "now" line stands between what has
  been and what is next.
- **Free time**: said in the day's sentence ("Free until 3 pm"), and drawn on today's timeline as
  a quiet row of its own only for a gap of an hour or more, so the timeline does not fill up with
  the ten minutes between two meetings. A touch on the row starts a new event there.
- **The days ahead**: tomorrow and the rest of the week (a week by default, the reader's
  horizon), each day on its own card with its own short sentence beside its name. Today stays on
  top, whole; tomorrow is drawn as today is; the days after it are compact (one line per event);
  a run of empty days is said once ("Monday 12 October – Tuesday 13 October, nothing planned"),
  so a full week stays a glance below the fold and an empty week never pushes the first real
  event out of view. In the evening, when today has nothing left, tomorrow moves up.
- **Touch an event**: the calendar app opens it, at that occurrence of it; editing is one tap
  away there.
- **The new-event button** (Material's floating action button): the calendar app's new event
  page, with the start already set to the next half hour.
- **Touch the date, or the calendar button beside the gear**: the calendar app opens on today
  (the button has a switch in Settings). Touch a day's name: it opens on that day.
- **Live while visible**: the clock moves on each minute's start, and a change made in the
  calendar app is on screen the moment the reader comes back. Nothing runs once the page is gone.
- **States that say what is true**: no permission (the clock works; one card explains why the
  agenda needs the permission and asks), no calendar on the phone, nothing today, every
  calendar hidden, no calendar app to hand an event to.

### The widgets (Jetpack Glance)

Two cards, the family's pair (owner, 7 Oct 2026), as Chiaro's «Colpo d'occhio» and «In parole»
and Passo's «At a glance» and «In words»: the same sizes for both, the same card, the same
settings screen.

#### «Agenda»

The concept's widget, in the family's dress.

- **A header with the clock and the date**, each shown or hidden and in its own format, drawn by
  the system's `TextClock`: always right to the minute, with no work by the app.
- **The day's events under it, as many as the card's size allows, never a scrolling list.** The
  card is laid out for the size the launcher really granted, every time it is resized; whatever
  does not fit is counted at the bottom ("2 more today").
- **Forms for every size**, from one cell (the time alone) to as large as the launcher allows:
  one row is the clock and the next event; two rows and up add the agenda; a wide card puts the
  clock beside the list.
- **Per-widget settings**, from the launcher's widget settings (the card is placed at once with
  sensible defaults, as the family's are, and changed later):
  - *Clock*: shown or hidden, and its format.
  - *Date*: shown or hidden, and its format.
  - *Touching the header*: opens Tempo, the calendar app at today, or the phone's clock app,
    where the alarms are (owner, 8 Oct 2026).
  - *Touching an event*: opens Tempo, or opens that event in the calendar app.
  - *The card*: light, dark, following the phone, or one of Chiaro's six colours, at any
    opacity, with a live preview: the family's widget settings.
  - *Content*: all-day events shown or not; the days ahead when today is done.
- **States**: no permission ("Touch to let Tempo read your calendar"), nothing left today (and
  tomorrow's first event, if the reader wants it).
- **Updates without polling**: when the calendar changes, when the next event starts or ends,
  at midnight, and when the clock, the time zone or the language changes. Never on a timer.
#### «In words» («In parole»)

The family's card in words, for a reader who wants to know where the day stands rather than read
its list. Redesigned around a dial after the first cards on the phone (owner, 8 Oct 2026).

- **A dial where Passo's card has its ring**: the system's own hands, and on the ring what comes
  next as an arc from its start to its end, the rest of the next twelve hours quieter. The hour
  hand walks into the meeting, through it and out of it by itself: a progress no other calendar
  widget shows, at no cost to the battery (the arcs change only where an event starts or ends).
- **What comes next, in words**: its title, and when ("Until 11:00", "15:00 – 15:45",
  "Tomorrow · 9:00"); the day's sentence under it on a larger card ("Then 3 more today.").
- **The date**, drawn by `TextClock` like the «Agenda» card's: at the far edge of a row, beside
  the dial on a taller card. The dial is the clock; the reader can hide either.
- **Forms for every size**: one cell is the dial; one row is the dial, the words and the date,
  Passo's row; two rows and up put the dial and the date on top and the words at the bottom,
  Passo's tall card, with the rest of the day as a line of times where there is room.
- The same settings, states and updates as «Agenda».

### Calendars and filters

- **Which calendars**: every calendar on the phone, grouped by account, each with its colour and a
  switch. A calendar added later is shown until the reader hides it.
- **Declined invitations** hidden by default. Cancelled events never shown.
- **All-day events** shown or not.
- **Days ahead**: today only, today and tomorrow, or a week (the default).

### Settings and personalization

- **Clock**: the phone's format (default), 24-hour or 12-hour.
- **Date**: long ("Tuesday 7 October"), medium ("Tue 7 Oct") or numeric, in the words and order
  of the reader's language.
- **Appearance**, the family's: theme (system, light, dark), palette (Vivid, Paper), typeface
  (Google Sans, Inter, the phone's), wallpaper colours on or off.
- **Language**: Italian or English, through Android's per-app language setting.
- **The guide**, as in Chiaro and Passo: what the screen answers, how the widget fits, what Tempo
  does not do and who does it instead.
- **About**: the privacy statement (what Tempo reads, what it never does), credits, the licence.

### Onboarding

- A short flow: welcome; the calendar permission, with what it reads and what it never does;
  the widget, with a button that offers to put it on the home screen (Android's pin request),
  from Phase 4, when the widgets exist.
- Refusing the permission is a choice the app respects: the clock and the widget's clock work
  without it, and the agenda's place says how to change one's mind.

### System surfaces

- **A launcher shortcut** (long press on the icon): "New event", straight to the calendar app's
  new-event page.
- **No notifications in v1**: the calendar app already reminds the reader of their events, and a
  second reminder from Tempo would be noise. So Tempo does not ask for the notification
  permission either.

### Data

- Tempo stores only the reader's settings and the widgets' looks; the events stay in the
  calendar. Android's backup carries the settings, an allowlist of one file, as in the family.

## Out of scope (non-goals)

- **Writing the calendar**: creating, editing, moving or deleting events inside Tempo, and with
  them `WRITE_CALENDAR`. The calendar app does it.
- **A calendar of Tempo's own**: accounts, sync, CalDAV or ICS subscriptions, anything that
  needs the network.
- **Event reminders and notifications**: the calendar app's job.
- **Alarms, timers, a stopwatch, a world clock**: the system Clock app's job. Tempo shows the
  next alarm, read-only, and nothing else of it; a widget's time can open the Clock app.
- **Tasks and to-dos**: they are not in the Calendar Provider (Google Tasks is a separate service).
- **Week and month grids**: the calendar app's job. Tempo is the day, and the few days after it.
- **Weather on the agenda**: Chiaro's job, and each app of the family stands alone.
- **The work profile's calendars from the personal one**: Android keeps the two profiles apart on
  purpose. Tempo installed inside the work profile reads the work calendars there.
- Wear OS, a tablet layout (an open foldable shows the phone's column, centred, as in the
  family), ads, analytics, crash reporting over the network.

### Possible post-1.0 ideas (not committed)

- An analog face, in the family's style: the ring of the icon as the dial.
- A second time zone, as one line under the clock.
- A lock-screen (keyguard) form of the widget, where the launcher offers it.
- A morning summary notification, opt-in.
- A countdown to an event the reader picks.
- Publication on Google Play.

## From the concept to this vision

The concept document is the starting point and most of it stands. These are the changes, the
corrections and the additions, each with its reason.

| Concept | In this vision | Why |
|---|---|---|
| Query `CalendarContract.Instances` | Kept, with the `Calendars` table for names, colours, accounts and visibility, and each instance's own status and the reader's answer to it | `Instances` expands recurring events into occurrences, which is right. But it alone cannot say which calendar is hidden, whether an invitation was declined or an event cancelled |
| (not in the concept) | **All-day events are read in UTC** | The provider stores an all-day event as midnight to midnight UTC. Read in local time, it lands on the previous day everywhere west of Greenwich, and spills into the next one east of it. The most common bug of agenda widgets |
| Only `READ_CALENDAR` | Kept, and enforced: `WRITE_CALENDAR`, contacts, network and location are on the build's forbidden list | A promise the build checks is a promise kept. Android 11+ also needs a `<queries>` entry to find the calendar app for the intents: a manifest declaration, not a permission |
| Edit with `Intent.ACTION_EDIT` | View with `ACTION_VIEW` (editing is one tap away in the calendar app); `ACTION_EDIT` only where Phase 1 shows the reader's calendar apps honour it | Android documents `ACTION_EDIT` for events, but calendar apps differ in what they do with it; some ignore it. `ACTION_VIEW` on the instance (with its start and end) works everywhere |
| "Automatic return to Tempo when the operation ends" | Not promised: what happens on Save or Back is the calendar app's. What Tempo promises is that the agenda is already up to date when the reader is back | Tempo watches the calendar for changes while it is on screen, so it never shows an event the reader just changed in its old state |
| (not in the concept) | If no app can take the intent (no calendar app, or one without a new-event page), the button is not drawn and the reason is said | A button that does nothing is a lie |
| Clock "digital or analog, following the ecosystem's guidelines" | **Digital** in v1; an analog face is a later idea | The family's language is typographic: a hero number (Chiaro's temperature, Passo's steps) with one sentence before it. The time is that number |
| Widget "with no ListView or RecyclerView" | Kept, and with Jetpack Glance that also means no Glance `LazyColumn` (it is a `ListView` underneath) | Glance's plain `Column` it is. Glance also drops the eleventh child of a container without a word, so the events are laid out in groups (Passo's lesson) |
| `onAppWidgetOptionsChanged` to read the size | Jetpack Glance with `SizeMode.Exact`: the card is recomposed with the size the launcher really granted, at every resize | The same result as the concept, without writing the callback by hand, and the family's widgets are Glance. The arithmetic of what fits is pure Kotlin, tested at the family's reference sizes |
| `TextClock` in the XML layout | Kept, inside the Glance card (`AndroidRemoteViews`) | The system redraws it every minute without waking the app. Like every widget text it is drawn in the launcher's system face, as the family's widgets already are |
| (not in the concept) | **When the list of events changes**: the calendar's own broadcast, a job triggered by the calendar's own changes as a safety net, an inexact alarm at the next start or end of an event and at midnight, and the system's time, zone and language broadcasts | Since Android 8 most broadcasts cannot be received from the manifest; the calendar's is sent so that they can (PLANNING.md §15, 9 Oct 2026), and the job with a content-URI trigger, which costs nothing while nothing changes, covers a phone whose calendar does not send it. The alarm is inexact and does not wake the phone: it is delivered when the screen is next on, which is when someone can see the card |
| A configuration activity "when the widget is added" | Optional at placement, reachable later from the launcher's widget settings | The family's choice (Passo's ADR 0005): the card is on the home screen at once with sensible defaults, and nothing is lost by changing it later |
| "Control centre of the day" | Kept as an aim, bounded by the non-goals | Tempo answers "what time, what day, what is next". Alarms, tasks, weather and reminders each already have their app |
| The name **Tempo** | Kept, plain, as the owner chose: the app is published on GitHub only | In Italian *tempo* also means weather, which is Chiaro's, and many apps are called Tempo. A store listing would need a subtitle ("Orologio e agenda"); that is Phase 7's to decide, if Google Play ever comes |

## Key decisions

| Area | Decision | Rationale |
|---|---|---|
| Data source | The system Calendar Provider (`Instances`, `Calendars`), through the `ContentResolver` | Every account the phone syncs is already there, with no account, no network and no library |
| Writing | None: the reader's calendar app, through `ACTION_VIEW` and `ACTION_INSERT` | No write permission, no way to damage the reader's calendar, no editor to build and keep right |
| Storage | No database: DataStore for the settings and the widgets' looks | There is nothing of Tempo's to store; the provider is the truth. Room joins the stack the day a feature needs a table (none in v1) |
| Widget | Jetpack Glance, `SizeMode.Exact`, a plain `Column`, the system `TextClock` for the clock | The family's widget technology; exact sizes; no scrolling; a clock that costs nothing |
| Refresh | Content-URI-triggered job on calendar changes; inexact non-wakeup alarm at the next event boundary and at midnight; exempt broadcasts for time, zone and language | Event-driven, never periodic. No exact alarms and no `SCHEDULE_EXACT_ALARM`: a card that drops a finished event a few minutes late is acceptable; one that drains the battery is not |
| Platform | `minSdk 34` (Android 14), `targetSdk`/`compileSdk 37` (Android 17) | Passo's levels (owner): one platform floor for the two apps built from one build-logic. Per-app languages, themed icons and the Android 12 widget APIs are native. Android 13 phones are left out, a choice the owner made knowingly |
| UI | Jetpack Compose + Material 3 in Chiaro's design language; Jetpack Glance for the widget | One design system for the family |
| Languages | Italian and English for the app; English for code and documentation | As the family |
| Name | **Tempo** | Consistent with Chiaro, Passo and Saldo |
| License | GPL-3.0 | As the family. Every dependency must be GPL-3.0-compatible (Apache-2.0, MIT, BSD) |
| Distribution | Signed APKs on GitHub Releases; Google Play kept open as a later option | As the family. The release key is created before the first release and kept for life |

## Success criteria

- **The right events on the right day.** Every instance in the provider within the horizon is on
  screen on its own day: all-day events in every time zone, multi-day events, recurring events
  and their exceptions, the days a clock changes for daylight saving, a trip across time zones.
  Each case has a unit test (PLANNING §4.6).
- **Fresh on return.** An event created or changed in the calendar app is on Tempo's screen when
  the reader comes back, with no gesture.
- **Fresh on the home screen.** The widget shows a calendar change within about a minute while
  the screen is on, and drops a finished event within a few minutes of its end (an inexact alarm's
  window). Its clock is right to the minute, always, at no cost.
- **Battery.** With the screen off Tempo does nothing, except a job when the calendar itself
  changes. No wake-up alarms, no wakelocks, no periodic work: checked with `dumpsys alarm`,
  `dumpsys jobscheduler` and `batterystats`.
- **Privacy is enforced.** The merged release manifest holds no `INTERNET`, location,
  `WRITE_CALENDAR` or contacts permission, and CI fails if one appears.
- **For every reader.** TalkBack reads each event as one sentence; text works at twice its size;
  an open foldable shows a centred column.

## The owner's answers

The questions this vision left open, settled in the owner's review (7 Oct 2026; PLANNING §15).
Every suggestion of the vision was accepted with them.

1. **Widgets**: the family's pair, «Agenda» and «In words», both in v1.
2. **Default horizon**: a week. Today stays on top and the days after tomorrow are compact, so
   the week costs the screen nothing at a glance; on the widgets the days ahead only fill the
   room today leaves.
3. **The next alarm** on Today: yes, on by default, one switch hides it.
4. **Free time**: in the sentence, and as a row for gaps of an hour or more.
5. **minSdk 34**, Passo's.
6. **Name**: Tempo, plain; GitHub Releases is the only channel for now.
7. **The launcher shortcut "New event"**: in v1.

## Glossary

- **Calendar Provider**: Android's shared database of calendars and events, filled by every
  calendar account the phone syncs (`CalendarContract`).
- **Calendar app**: the app the reader uses to see and edit their calendar (Google Calendar,
  Samsung Calendar, Etar, …). Tempo hands it every change.
- **Event**: one entry of a calendar, possibly recurring.
- **Instance**: one occurrence of an event on a given day and time; a weekly meeting has one
  instance a week. What Tempo shows.
- **All-day event**: an event with dates and no times, stored by the provider from midnight to
  midnight UTC.
- **Horizon**: how many days ahead Tempo shows.
- **Boundary**: a moment the agenda changes by itself: an event starts or ends, a day begins.
- **Card**: one widget on the home screen. **Form**: the layout a card takes at a given size.
