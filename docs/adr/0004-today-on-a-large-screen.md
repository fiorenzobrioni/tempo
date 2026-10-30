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
