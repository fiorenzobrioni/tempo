# ADR 0004: Today on a large screen (Phase 8.7)

- Status: accepted
- Date: 2026-10-30

## Context

Since Phase 0 every page has been one column, at most 640 dp wide and centred (`PageMaxWidth`,
`pageGutter()`, from the family): on an open foldable or a phone on its side the clock, the agenda
and a line of text keep a phone's proportions. VISION listed "a tablet layout" among the
non-goals, and CLAUDE.md said "No tablet layout".

On the owner's Galaxy Tab S8+ in landscape (1280 × 800 dp) that column is a phone's page between
two empty margins of 320 dp each, the review's eighth finding (30 Oct 2026). The owner asked for
Today in two panes on a large screen and confirmed it as planned (PLANNING.md §11 step 8.7, §15).

## Decisions

1. **Today alone.** Settings, the guide and the first run keep the centred column: they are read
   one line at a time and gain nothing from a second pane. Today is a glance at two things, the
   time and the day, and a large screen can show both at once.

2. **When: an expanded window, 840 dp wide and 480 dp high or more** (`TwoPanes.of`,
   `TwoPaneMinWidth`, `TwoPaneMinHeight`, `:core:designsystem` `PageWidth.kt`). Material's
   expanded width, read from the window as `pageGutter()` reads it, so a tablet split in two
   windows, a foldable open under 840 dp (a book-style foldable's inner screen, about 690 × 829 dp)
   and a tablet upright under 840 dp keep the column. The height is Material's medium: a phone on
   its side is 891 dp wide and 411 dp high, and two panes there would leave the hero a strip.
   No new dependency: `material3-adaptive`'s window size classes would say the same and need the
   owner's approval (CLAUDE.md).

3. **What goes where.** The leading pane holds the hero (the date's row and its buttons, the
   clock, the dial, the alarm, the second clock, the sentence), today's all-day chips and the first
   day's tip; the trailing pane holds the agenda: today's timeline (the fold, "now", the free row),
   the days ahead, a state's card where there is no agenda (no permission, no calendar, every
   calendar hidden), and the word on a missing calendar app. Each pane is a list of its own and
   scrolls on its own; the agenda's first row stands level with the date's. The new-event button
   is over the agenda's pane, its label shown while that pane is at its top.

4. **How wide.** The leading pane is two fifths of the two (`LEADING_PANE_SHARE`), never under
   400 dp (`LeadingPaneMinWidth`, a phone's page, so the hero there is never smaller than on a
   phone); the trailing pane takes the rest. Together they are at most 1280 dp
   (`TwoPaneMaxWidth`, the column twice) and stand centred past it, the insets (a cutout, a side
   navigation bar) kept clear first, as the column's gutter does. At the reference windows:
   1280 × 800, 512 and 768 dp; 900 × 1200, 400 and 500; an open foldable at 841 × 701, 400 and
   441; 1480 × 924, 512 and 768 with 100 dp on each side.

5. **A larger dial, never a smaller clock.** In the hero's pane the dial takes the room the row
   leaves beside the clock at its largest, from a phone's 112 dp up to 200 dp. The clock is
   measured at the widest it reads in the reader's format and typeface (two figures of hours,
   either day marker), so the face keeps its size all day and the time is never shrunk for it.
   At 1280 dp the dial is 200 dp; in a 400 dp pane (900 dp upright, 841 dp open) about 130 dp,
   where a phone's is 112.

6. **One glow across the page.** The hero's glow (the accent's container fading into the page
   under the sentence) is drawn behind both panes, as high as the hero, and stays where it is
   while either pane scrolls; a glow on the leading pane alone ended in a hard edge where the two
   meet.

7. **The reveal follows the pane** (step 8.6, unchanged in what it does): a new event of the
   agenda is brought into view in the agenda's pane; a new all-day event of today glows among the
   chips in the hero's pane. The list's own top padding is counted, so a row comes to rest the
   same distance under the status bar in either layout.

8. **Each layout keeps its own place.** The page's column and the two panes have their own list
   states: turning a tablet between the two does not bring a list to another item.

## Consequences

- VISION's non-goal becomes "a tablet layout beyond Today's two panes"; CLAUDE.md's
  "Accessibility and foldables" says where the two panes apply.
- Today's tests draw the page on a tablet on its side and upright, on an open foldable over and
  under 840 dp and on a phone on its side, walking each pane (`walkPage()`, `assertAccessible()`),
  at twice the text size and in the dark; `PageWidthTest` pins the arithmetic at the reference
  windows. The README shows the tablet (`today-tablet.png`).
- The other pages still use `pageGutter()`; a page that wants two panes later takes `twoPanes()`
  and decides its own split, with its ADR.

## Changed on 10 Nov 2026

The owner's review of Today (PLANNING.md §15, 10 Nov 2026, «Today reviewed: one clock, one "now",
one ground») changes three things written above, on every screen and so on this one:

- **Decision 6 is withdrawn**: the hero has no glow, on a phone or across the two panes; the page
  is one flat ground.
- **The new-event button** is its icon alone, whatever the agenda's pane shows (decision 1 said
  its label was shown while the pane was at its top).
- **The dial** is drawn only while today has something left on its ring; with none the time
  stands alone in the hero's pane. Today's all-day events, in the hero's pane, are a dot and a
  title each, no longer chips.

## Changed on 26 Nov 2026

The graphic review (PLANNING.md §15, 26 Nov 2026, «Today has two grounds: the graphic review»;
§11 step 10.2) gives the clock a ground of its own, on every screen and so on this one:

- **The leading pane is the clock's ground, whole**: `surfaceContainer` from the window's top
  edge to its bottom, behind the status bar, and from the window's outer edge (the page's gutter
  with it) to where the two panes meet, in a straight edge. The trailing pane is the page. This
  is not decision 6's glow back: a flat tone, which needs no fading across the two panes because
  it ends where the clock's pane does. Each pane's list still ends under the status bar, the
  leading one's on its own ground.
- **What stands in the leading pane stands on that ground.** The dial's face is a disc of the
  page's surface, its ring at the disc's rim, and its arcs are stepped to 3:1 against the pane's
  ground (`dialMark`); today's all-day events step their dots against it; the first day's tip, a
  note, is drawn on the page's surface, since a note's own tone is a step from the pane's and was
  lost on it (`StatusCard`'s `onGround`).
- **In the page's one column** the ground is under the hero alone, from the screen's top edge,
  its lower corners round (28 dp), across the window whatever the gutter: on a foldable open under
  840 dp the clock's ground is the screen's top and its words keep the centred column.
- **The compact bar is the one column's alone** (step 10.3; PLANNING.md §15, 26 Nov 2026, «Step
  10.3 built»): the date, the time and the two buttons kept at the top once the hero has gone up.
  In two panes there is none: the hero's pane scrolls on its own and the clock never leaves, which
  is what decision 2 gave a large screen.

## Changed on 26 Nov 2026: the month under the clock

The graphic review's eighth point (PLANNING.md §11 step 10.5; §15, 26 Nov 2026, «Step 10.5
built»): the leading pane was half empty on a tablet, and the current month fills it with
something the page is for. This adds to decision 3, which listed what the pane holds.

- **What.** The leading pane's last item, after the hero, today's all-day events and the first
  day's tip: the current month. Its name with its year, a heading with the chevron a day's name
  wears; the week's days; its weeks of dates with their marks, drawn by the grid the month's page
  draws (`MonthWeekdays`, `MonthWeeks`, `:core:designsystem`), today on the accent. It stands on
  a plate of the page's `surface`, as the dial's face does, so the marks are stepped against the
  ground the month's page steps them against.
- **A way in, not a second month.** A touch on a date opens the month's page on that date, its
  month shown and the date chosen (`MonthKey` carries it); a touch on the name opens it on
  today. Nothing turns under the finger, no day is chosen, and a long touch does nothing: each
  of those is the month's page's, a touch away. A screen reader hears a date and what it holds,
  and is told of no choice.
- **Where it stands in the pane.** Last, so that where the pane is short and scrolls (an open
  foldable at 841 × 701 dp) the clock, the sentence and the alarm keep the first screen and the
  month is what is scrolled to. At 1280 × 800 dp a month of five weeks stands whole under the
  hero with nothing scrolled (the card ends about 30 dp over the pane's foot, the sample's day);
  a month of six weeks, or a longer hero, scrolls by a row.
- **A date's height is its least** (`monthCellHeight()`), whatever the pane's: the month's page
  grows its cells to fill its pane, because the month is that pane's one subject; here it is
  the third thing in a list, and its height does not depend on what stands over it. In a tall
  pane (900 × 1200 dp) the ground under the month stays empty.
- **What is read.** The month's dates come with the agenda's, in one query, in two panes only
  (step 10.4): a phone reads what it read. Until that read lands, and where there is no calendar
  to read, the month is today's and its dates have no marks.
- **Not in the page's one column**: a phone, a phone on its side, a foldable open under 840 dp.
  There the month would take the first screen from the agenda; the button beside the settings'
  is its door.
