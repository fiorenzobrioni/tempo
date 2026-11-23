# ADR 0005: The month (Phase 9)

- Status: accepted
- Date: 2026-11-23

## Context

Since the vision (7 Oct 2026) Tempo has been one page, Today: the day and the few days after it.
"Week and month grids" stood among the non-goals as "the calendar app's job".

On 23 Nov 2026 the owner asked for a second view beside the agenda: a month, "to see at a glance
a whole month with the days I have something on, and from there touch a day to add an event",
with a way to move between months, a quick way back to the current one and a quick way to a far
one; and proposed a bottom bar of destinations, as the step counter of the family has, and a
bottom sheet for a touched day. The owner also said what it must not become: "always a month,
never a week or a day view as other apps do: with the agenda the two are enough, and more would
be noise".

The non-goal was right about what it feared, a second calendar app inside Tempo, and wrong about
the month as such. A calendar app's month is built to be edited in: titles cut to four letters in
cells, as many as fit. The question the owner asks of a month is another one: which days are
taken. That question Tempo can answer better than a calendar app does, in the family's quiet
look and with the choices the reader already made (which calendars, declined invitations,
all-day events), and without writing anything.

## Decisions

1. **A month, read-only, and nothing else.** No week view, no day view, no year view. Today is the
   day. The page reads the same provider through the same engine and hands every change to the
   calendar app, as Today does: no new permission, no storage, no write.

2. **Marks, never titles** (`MonthMarks`, `:core:domain`). A date says what it holds with marks
   under its number. An event with a time is a dot in its calendar's colour, up to three, and a
   small plus where there are more. An event that takes whole dates (an all-day event, or a timed
   one that covers a date from midnight to midnight) is a bar under the dates it covers, one line
   across them, rounded where it starts and ends and square where it goes on into the next cell,
   so a week away reads as a week. Two bars at once keep a lane each on all their dates (lanes
   given out over the whole run of dates read, the span that starts first taking the first free
   one); a third has no lane and is said by the plus. Calendar colours stay data: marks at 3:1 on
   the page (`markColor`), quieter on a date gone or of the month beside (`markAlpha`, mixed with
   the page's ground so a bar's round end does not show twice as strong over its own line).

3. **The chosen day stands under the month, in the page; not in a sheet.** The owner's proposal
   was a bottom sheet for a touched day that has events, and the calendar app's new-event page at
   once for one that has none. Two things were against it. The grid takes half a phone's screen:
   the half under it would be empty until a touch, on a page of an owner whose standing complaint
   is empty space. And one touch would do two different things depending on a dot the reader has
   to look for first, one of them leaving the app. So: a touch chooses the date and its events are
   listed under the month; the new-event button starts an event on the chosen date; a long touch
   on a date does it in one gesture. Asked of the owner with both drawn, and chosen (23 Nov 2026).

4. **The day's rows are Today's rows.** `AgendaEventRow`, `AgendaAllDayRow` and `DayHeading` moved
   from `:feature:today` to `:core:designsystem` (`AgendaRows.kt`) with their words
   (`strings_agenda.xml`), and both pages draw a day with them: the event under way on its ground
   with its progress, a past event quieter, a declined one struck through, TalkBack's one
   sentence. A feature module cannot depend on another, and a second drawing of an event would
   drift from the first. `CalendarDoors` moved to `:core:calendar` for the same reason.

5. **The dates are built by the agenda's own builder.** `AgendaBuilder.days(from, to)` builds any
   span of dates by the code that builds Today's (all-day events in UTC, an event on every date it
   touches, the reader's filter), so every rule of §4.6 holds on the month with no second copy.
   Its tests say a span's dates equal the agenda's for the same dates.

6. **What is read: the month on screen and the two beside it** (`MonthPages.span`), so the page
   under the finger has its marks before it is turned to; read again when the page comes to rest
   on another month. Built off the main thread (a quarter of a year is more dates than a week).
   Live while visible, as Today: the calendar's observer and a minute ticker in the page's
   collection, nothing once it is gone (§9). No widget, no alarm, no work.

7. **Whole weeks, and a grid as tall as its month.** The grid shows the weeks the month touches
   (four, five or six rows), the dates of the months beside that complete a week drawn quieter
   and a touch on one turning to its month. No empty cell, and no empty row under a five-week
   month: between two months of different heights the grid's height follows the finger, so the
   day under it slides with the page. The week starts where the reader's does (the locale's, or
   the phone's regional preference, read by ICU).

8. **Far months: the month's name.** A touch on it opens a sheet with a year, stepped by two
   arrows, and its twelve months: any month of any year in two or three touches. "Today" stands in
   the top row only while the page is elsewhere (no button that does nothing). The months are two
   centuries of pager pages (1900 to 2099).

9. **Reached from Today's top row, not from a bar of destinations.** The owner proposed a bottom
   bar. With two destinations a bar is weak (Material asks for three to five), and it would take
   about 80 dp from Today, a row and a half of events on its first screen, every day, for a page
   opened now and then. Today's top row already had a calendar button, which opened the calendar
   app at today, as a touch on the date beside it does. That button now opens the month: no new
   chrome, one duplicate less. The month is a page over Today on the one back stack, and back
   (previewed under the finger) returns. Asked of the owner with both drawn, and chosen (23 Nov
   2026).

10. **On a large screen the month stands beside the day** (`twoPanes()`, ADR 0004's arithmetic):
    the month in the leading pane, its cells grown to share the pane's height as a wall calendar's
    do, the chosen day in the trailing one, the new-event button over the day. Under 840 dp, and
    on a phone on its side, the one column.

## Consequences

- VISION's non-goal is rewritten: what Tempo leaves to the calendar app is the week, the day in
  hours, the year, and a month with titles to edit in.
- `show_calendar_button` keeps its key and now hides the month's door. A reader who had hidden the
  button does not see the month until they turn it back on; the switch's note says what it opens.
  Whether the switch should go is asked of the owner (PLANNING.md §15, Open).
- A new module, `:feature:month`. No new dependency: the pager, the sheet and the long touch are
  Compose Foundation's and Material 3's, already in the build.
- The cost of the page is a read of about three months of instances when it opens and when a page
  comes to rest. Nothing runs with the page closed, so §9 stands as it was.

## Not done, on purpose

- Week numbers, a year view, a heat map of busy hours, a month widget: each was weighed against
  "at a glance, which days are taken" and added nothing to that answer.
- A search of events. The owner asked for "a search or a way to reach a far month": the month's
  name is that way. Searching events by their words is the calendar app's.
