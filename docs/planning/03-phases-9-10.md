# PLANNING, archive 03: the reviews of November, the month, and Today's two grounds, 4 to 26 November 2026

> Frozen on 26 Nov 2026: a closed period of the log, moved here from `PLANNING.md` without
> changing a line. Not edited. The living plan is [`PLANNING.md`](../../PLANNING.md); the index
> of every title is [`README.md`](README.md). Here are the phases the period closed (§11: Phase 9,
> the month and the time's door, and Phase 10, Today's two grounds, the graphic review of 26 Nov
> 2026; TalkBack on Phase 10's new places, the one check of its step 10.6 not run, is carried to
> the Open list of `PLANNING.md` §15) and §15's log from 4 to 26 Nov 2026: the widgets' circle and
> «At a glance», the new icon, Today's review of 10 Nov 2026, the first run, the family's widgets,
> the two phases, and the review before Phase 10's merge.

---

## 11. Delivery phases

Each phase ends with a merged PR, green CI and its acceptance criteria met. Phases 1 and 4 need **testing on a physical device** with real accounts: an emulator's calendar has no sync and no second calendar app.

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
- [x] **10.4 The month's grid, shared** (point 8, first half; no change to see). Built on 26 Nov
  2026: what moved, the one read Today makes and how the plan's words were read are in §15, «Step
  10.4 built». The grid's
  drawing moves from `:feature:month` to `:core:designsystem` (a month of cells with their marks,
  today and a chosen date: `DayCell`, `drawMarks`, `Weekdays`, their sizes), as the agenda's rows
  moved in 9.3; the pager, the long touch and the page stay in `:feature:month`, which draws as
  before (`MonthScreenTest` unchanged and green, its screenshots compared). `TodayViewModel` reads
  the current month's marks (`MonthPages`, `MonthMarks`, `AgendaBuilder.days`) only while Today
  stands in two panes and is visible, on the observer and the ticker it already has: no read on a
  phone, none with the page closed. Domain tests first where a function is new.
- [x] **10.5 Today on a large screen: the month under the clock** (point 8, second half). Built on
  26 Nov 2026: where it stands, what a touch does and what building it left out are in §15, «Step
  10.5 built». In the
  hero's pane, under today's all-day events: the current month on the page's `surface`, its name a
  heading with the chevron, the shared grid, today marked. A touch on a date opens the month on
  that date (`MonthKey` gains the date); a touch on the name opens it on today. No pager, no
  chosen day, no long touch here: it is a way in, not a second month. `TodayScreenTest` on the
  tablet on its side and upright and on the open foldable over 840 dp (where the pane scrolls: the
  month comes after the hero and must not push the sentence off the first screen); ADR 0004
  amended (the hero's pane, its ground, the month); `today-tablet.png` redrawn; the guide's
  chapter on large screens, both languages.
- [x] **10.6 On the phone and the tablet**, before the merge (§15, Open): the checks listed there. Done by the owner on 26 Nov 2026, on the CI's APK of the branch (§15, «Phase 10 checked on the phone and the tablet»); TalkBack, its check 11, is carried to Open. A review of the implementation followed before the merge (§15, «The review before Phase 10's merge: seven findings»).

**Acceptance.** Today reads as two grounds, the clock's and the calendar's, and one accent that
means now, in both dresses and both themes; every ink on the new grounds is pinned at 4.5:1 and
every mark at 3:1; the first screen at 411 dp shows no fewer events than before the phase (the
measure of 10.2); the month's title is whole at every width and text size the tests walk; no
alarm, no job, no service and no permission added, and the calendar read no more often on a phone
than before.

---

## 15. Decisions log and open questions

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

### Step 10.4 built: the month's grid, shared (26 Nov 2026)

Built on `claude/today-two-grounds`, as §11 says (owner, 26 Nov 2026: "continue Phase 10 with
step 10.4"). Nothing to see: the month draws as it drew, and Today draws nothing new until 10.5.
What moved, what Today reads, and the two places where building it read the plan's words rather
than their letter.

- **The grid's drawing is `:core:designsystem`'s** (`components/MonthCells.kt`): `MonthWeekdays`
  (the week's days over the columns) and `MonthWeeks` (a month's weeks of dates: the cell, its
  marks' inks and their drawing, today's disc and the chosen date's, their sizes), with what they
  need around them: `MonthDays` (what the grid asks of its page), `monthCellHeight()`,
  `MonthGridEdge`, a date's test tags (`MonthCellTags`, which `MonthTags` now hands on) and what
  TalkBack reads of a date (`MonthDayLabels`, with its three strings, moved from the month's
  resources in both languages). `:feature:month` keeps `MonthPager` (the months under the finger,
  the height that follows it), the page, the way to a far month, and what a touch does.
- **What a touch does is the page's.** The cell takes the touch's label from its page
  (`MonthDays.chooseLabel`: "Show the day" stays the month's string, and Today's will say another
  thing), and takes a long touch only where the page gives one (`onNew`, null otherwise). *The
  plan's "the long touch stays in `:feature:month`", read as what the touch does*: a new event on
  that date is the month's; the gesture on a cell, its haptic and its label ("New event", the
  design system's already) are the cell's, where the cell's one touch target is. Today passes none.
- **Today reads the month in the agenda's own read.** *The plan's "reads the current month's
  marks", built as one query and not a second beside the agenda's.* Where the page stands in two
  panes, the span asked of the provider is the agenda's and the month's as one
  (`TodayMonth.window`, on `AgendaWindow.covering`), and `AgendaBuilder` trims each view to its own
  dates, as it always trimmed the agenda's (the window was already a day wider on each side).
  Today is a date of its month, so the two spans always meet; near the month's end the agenda
  runs past the month's last week, and the read ends with the agenda. On two panes the provider is
  asked exactly as often as on a phone, for about six weeks of instances where the default agenda
  asks for nine days. On a phone the span is the one it was, to the instant: `showMonth` is never
  true there.
- **The page says its panes** (`TodayRoute`: `twoPanes() != null`, in a `SideEffect`, so before
  the first read starts; `TodayViewModel.showMonth`). A tablet turned to one column, or a foldable
  closed, says so, and the next read is the agenda's alone. It rides the same collection as the
  agenda's: no flow of its own, so nothing of it outlives the page (§9).
- **The marks are built with the agenda, at each tick** (`TodayMonth.of`: `AgendaBuilder.days`
  over the month's dates, then `MonthMarks.of`, the month's own two steps), on the thread the
  agenda is built on: six weeks of dates, the agenda's week six times, where the month's page
  builds a quarter of a year and does it off the main thread. The state says which month the
  marks are of (`TodayContent.Ready.month`): in the moment after a month's last midnight, until
  the new read lands, they are still the month before's, and are drawn as that.
- **The dates to read for one month** are `MonthGrid.span`: every date its grid can hold whatever
  day the week starts on, six of the month before and six of the month after at most.
  `MonthPages.span`, the page's three months, is written with it and gives the dates it gave
  (pinned by a test written before the change).

**Until 10.5** a large screen reads six weeks for a page that shows one: on this branch only, the
two steps reach `main` together.

**Tests.** The domain's first, as the plan asks. `TodayMonthTest`: the one read holds the agenda's
dates and the month's whole grid, from any first day of the week, on every day of a year at every
horizon the settings offer, in Rome and in New York; it is no wider than the two ask for; every
date the grid can hold has its marks and no other; a date says what it holds before today and
after; the reader's choices hold as on the agenda (a hidden calendar, all-day events left out);
an all-day event keeps its date west of Greenwich (§4.2). `AgendaWindowTest`: two spans read as
one, and a span that holds the other. `MonthGridTest`: one month's dates, tight on both sides for
every month of four years; the page's three months unchanged. `AgendaBuilderTest`: a wider read
gives the same agenda (events weeks before today and past the horizon, the event under way and
the next one the same). `MonthScreenTest` unchanged, its twenty-eight tests green; the month's
seventeen screenshots and Today's fifty-four, drawn afresh before the step and after it, compared
pixel by pixel: identical. No test of the ViewModel itself: the features have none (it would take
a provider, a DataStore and the main looper); what it decides is `TodayMonth`'s, tested there, and
what is left in it, a flag and a call, is for the tablet's check.

**The words.** None for the reader: no string changed, the CHANGELOG has no line (nothing of this
is seen until 10.5), the README's pictures are as they were. CLAUDE.md's modules and §4.1, §6 and
§11 here follow.

**For the tablet** (Open, check 10, grown): with 10.5's month on screen, an event made in the
calendar app appears in the grid as it appears in the agenda, and after midnight the month's
"today" moves.

**What 10.5 finds.** In the state, on two panes, `TodayContent.Ready.month` (`TodayMonth`: the
month and its marks); null in one column, and there is none in the contents that are not
`Ready`, where a grid can still be drawn with no marks from the month of `state.now` (a month is
a month). To draw it: `MonthWeekdays(firstDay)` and `MonthWeeks(MonthGrid.of(month, firstDay),
MonthDays(…))`, `firstDay` from `DateTimeText.firstDayOfWeek(locale)`, kept `MonthGridEdge` from
the sides. To change in the shared grid: `MonthDays.selected` is a date, and Today has no chosen
day, so it becomes nullable and a cell then says no `selected` to a screen reader; `onNew` is
null on Today; `chooseLabel` wants Today's own words, a new string in both languages. The marks
are stepped against the page's `surface`, which is the ground the plan gives the month in the
hero's pane. `MonthKey` is a `data object` (`TempoRoot.kt`) and gains the date; `MonthScreen`
already takes `initialMonth` and `initialDay`, and `MonthRoute` is to hand them on.
`TodaySample.state()` builds no month: the tablet's tests give it one with `TodayMonth.of`.

**The log's size.** With this entry the log is 82 KB, past the ~80 KB at which «How it is made»
archives it. Left for the phase's close, so that Phase 10 goes to the archive whole and §11's
steps keep pointing here meanwhile: a box in Open says so.

**Checked.** `spotlessCheck`, `test` (every module: 631 tests), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green; the merged manifest is unchanged.
No phone or tablet on adb: check 10 waits for 10.5's month on the branch's APK.

### Step 10.5 built: Today on a large screen, the month under the clock (26 Nov 2026)

Built on `claude/today-two-grounds`, as §11 says (owner, 26 Nov 2026: "continue with step 10.5").
Where the month stands, what a touch on it does, and what building it weighed and left out.

- **What it is.** `MonthCard` (`TodayScreen.kt`), in the hero's pane only: the current month's
  name with its year ("October 2026"), the week's days, and the month's weeks of dates with their
  marks, drawn by the shared grid of step 10.4 (`MonthWeekdays`, `MonthWeeks`). On a plate of the
  page's `surface` with the groups' 24 dp corner, the pane's ground around it: the dial's face
  over again, and the ground the marks are stepped against on the month's page, so a mark here is
  the mark there. Today's number on the accent, which still means now.
- **Where it stands: last in the pane**, after the hero, today's all-day events and the first
  day's tip. *The plan's "under today's all-day events", with the tip between the two while it
  is shown*: the tip explains the dial and stays by it, and it is there one day. Last, so that
  where the pane is short the clock, the sentence and the alarm keep the first screen and the
  month is what is scrolled to. It is an item of the pane's list as the tip is (`card()`), so it
  slides up when the tip is dismissed.
- **The name** is the month with its year in `titleMedium`, the size of the date at the pane's
  top, with the chevron a day's name wears (`DayName`): the plan's "a heading with the chevron".
  Not the `titleLarge` of a day's name in the agenda: under a clock 104 dp high and beside
  dates of 16 sp, the larger size made the card's head the loudest thing after the time. Its
  touch is the card's whole width, 48 dp high.
- **A touch** on a date opens the month's page on that date; on a date of the month beside
  (the days that complete a week) the page opens on that month; on the name, on today. A screen
  reader hears "Wednesday 7 October, today, 8 events" and, of the touch, "Open the month on this
  day" («Apri il mese su questo giorno»), a new string in both languages; the name's touch says
  the button's "Open the month". No long touch, no chosen day, nothing to turn.
- **`MonthKey` carries the date** (`TempoRoot.kt`): a `data class` with the day's count, null
  for today, since a key is saved with the back stack and a date is not serializable as it is.
  `TodayRoute`'s `onOpenMonth` and `TodayActions.openMonth` take the date (null from the button
  and the name); `MonthRoute` takes `openOn` and hands `MonthScreen` its `initialMonth` and
  `initialDay`, which it already had. Back from the month returns to Today, as from the button.
- **In the shared grid**, `MonthDays.selected` is nullable: with none, no date is drawn as
  chosen, and a cell sets no `selected` at all, so a screen reader is not told "not selected" on
  thirty-one dates (pinned by a test). `chooseLabel` and `onNew` were the page's since 10.4.
- **Which month.** The one the marks were read for (`TodayContent.Ready.month`); with none, the
  month of `state.now` with no marks. That is the moment after a tablet is turned to two panes,
  before the wider read lands, and every state with no calendar to read (no permission, no
  calendar, every calendar hidden, loading): the grid is drawn all the same, as on the month's
  page, and is still the way in. After a month's last midnight, until the new read lands, the
  month drawn is still the one before, with today in the week the two share.

**Measured** (the sample's Wednesday at 10:20, October 2026, five weeks).

| window | the hero's pane | the month's card |
|---|---|---|
| 1280 × 800 dp, a tablet on its side | 512 dp | whole on the first screen, ending about 30 dp over the pane's foot |
| 900 × 1200 dp, a large tablet upright | 400 dp | whole, about 480 dp of ground under it |
| 841 × 701 dp, an open foldable | 400 dp | starts on the first screen under the all-day events; its last week is scrolled to |

At 1280 × 800 a month of six weeks, 58 dp more, scrolls by a row; so does a sentence over three
lines. The card is 366 dp of grid and name (48, about 28, five weeks of 58) and 12 of air.

**Weighed and left out.**

- **Cells that grow to fill a tall pane**, as the month's page grows them in its own pane (to
  1.75 times). There the month is the pane's one subject and its height is the pane's less two
  rows; here it is the last item of a lazy list, and the room left depends on what stands over
  it (the sentence's lines, the alarm, the second clock, the all-day events, the tip), known only
  once those are measured: a frame late, so the grid would be drawn small and then grow. On the
  owner's tablet on its side there is no room to grow into (30 dp); the room is on a tablet over
  840 dp wide held upright. Left at the least height (`monthCellHeight()`), said in ADR 0004; to
  open again if a large tablet upright is someone's daily window.
- **The month on a foldable open under 840 dp, or on a phone on its side**: one column there,
  and the month would take the first screen from the agenda (the plan's own reason).
- **A second door on the card to the calendar app's month**: the date above opens the calendar
  app on today, a day's name on that day; the card is Tempo's month.

**Tests** (`TodayScreenTest`, six new and five grown; `MonthScreenTest`, one new). On a tablet on
its side: the card in the hero's pane under the all-day events and whole on the first screen, its
name a heading, three dates read as the month's page reads them, no date `selected`, the plate's
ground and the pane's beside it and today's disc read from the page's pixels; a touch on a date, on
a date of the month beside and on the name, and no long touch; the month drawn with no marks until
they are read, and with no permission; the month before's marks after a month's last midnight;
Italian ("Ottobre 2026", the touch's two labels). The tip stands before the month. Upright at
900 × 1200 the month whole under the hero; on the open foldable at 841 × 701 the clock, the
sentence and the alarm whole on the first screen, the month running under the fold, and its last
week there at the end of the walk. On a phone, no month even if the state holds one. Every walk
of two panes (the tablet, at twice the text size, upright, the foldable, in the dark) now crosses
the month and was looked at. `MonthScreenTest`: opened on a date of another month, the page shows
that month, the date chosen, the way back to today, and asks for that month's dates alone. The
two-pane tests draw `TodaySample.large()`, the sample with its month's marks; a phone's draws none.

**The words.** The guide's Today chapter gains «On a large screen» («Su uno schermo grande»): the
two parts, and the month under the clock with what a touch does. *The plan's "the guide's chapter
on large screens"*: there was none, the guide had never said the two panes; one entry, in Today's
chapter, where the reader of a tablet looks. The CHANGELOG has its line under Added; the README's
feature line and the tablet's picture and caption, VISION's «On a large screen» and ADR 0004
follow. `today-tablet.png` is redrawn.

**For the tablet** (Open, check 10, rewritten): the Tab S8+ upright is 800 dp wide, one column,
so the month under the clock is seen on its side only; what a test cannot say is the read (an
event made in the calendar app coming onto the grid, today moving at midnight, the marks back
after the tablet is turned), which is `TodayViewModel`'s and has no test of its own (§15, «Step
10.4 built»).

**Checked.** `spotlessCheck`, `test` (every module: 638 tests), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green; the merged manifest is unchanged.
The pictures of every walk of two panes, the guide's new entry and `today-tablet.png` looked at.
No phone or tablet on adb: check 10 is for the branch's APK, with the rest of 10.6.

### Phase 10 checked on the phone and the tablet (owner, 26 Nov 2026)

Step 10.6, on the CI's APK of `claude/today-two-grounds` (PR #36, the run of commit `7af2fc3`).
The owner: "I ran all the tests and everything is fine". The word is on the whole of Open's
list for the phase, its checks 1 to 10. Check 11, TalkBack on the compact bar, the "Today" icon and
the month in the hero's pane, was not run: the owner has no way to try TalkBack now (said the same
day). It is carried to Open as a box of its own; what the tests say of those places (their labels,
their roles, their touch targets, and what a screen reader is offered of a row under the bar) is
in the next entry.

### The review before Phase 10's merge: seven findings (owner, 26 Nov 2026)

Before merging PR #36 the owner asked for a review of the phase's implementation, "to see if
there are errors". The whole diff of the branch against `main` was read; seven findings, none of
them stopping the merge. The owner: all of 2 to 7 in this PR, and for 1, which needs TalkBack to
try, "see what you can deduce from the code and decide whether to step in". Each was first shown
to be what the review said, in a test, before it was changed.

- **1. A row under the compact bar.** In the page's one column the list runs to the screen's top
  edge since step 10.2, and since 10.3 the bar is drawn over it. Two readers, two answers, both
  measured under Robolectric. *A screen reader*: nothing to change. Compose offers a node to the
  accessibility services only where nothing drawn over it covers it: the row half under the bar
  is offered from the bar's foot down, and one behind the bar is offered with no bounds at all
  (`TodayScreenTest`, «a screen reader is offered a row under the bar only where it shows», on
  the page's own `AccessibilityNodeProvider`). The bar's ground is a `Surface` with semantics of
  its own, which is what makes it count as a cover: keep it so. *A keyboard's focus*: a defect.
  The focus on a row brings it into the list's view by the least scroll, and the list's view was
  the screen's top: the event under way, half under the bar, took the focus and stopped at y = 0,
  48 dp of it behind the bar (measured). Fixed with the list's own rule for what is in view
  (`UnderCover`, a `BringIntoViewSpec` given to the list through `LocalBringIntoViewSpec`): the
  status bar's strip, and the bar while it is shown, are taken off the top of the room a row is
  brought into. `LocalBringIntoViewSpec` is experimental in Compose foundation 1.12 (`@OptIn` on
  `TodayPage`, the one use): if it changes, `TodayKeyboardTest` fails. That class stands apart
  from `TodayScreenTest` because a row takes the focus only out of touch mode, which is set before
  the window is made. The owner's Tab S8+ upright is one column, with the bar, and takes a
  keyboard cover: not a case for nobody.
- **2. The event just made, under large text.** The row is brought a margin under the bar, and
  the bar's height was the least it has (48 dp) until the bar was first drawn: where bringing the
  row is what draws the bar, and the bar is taller (at twice the text size on a phone the date
  takes two lines), the day's name over the row stood behind it. `revealNewEvent` now brings
  the row again while it glows, whenever the bar turns out taller (`snapshotFlow` on the edge it
  is brought to): no second scroll at the usual text size, where the bar is its least.
- **3. A declined invitation, under way.** Since 10.2 the event under way took the accent's ink
  and the title's weight before the row asked whether it was quiet, so with «Show declined» on, a
  meeting the reader declined was the loudest thing on the page for its hour. *Kept on now's
  ground, with its bar and how long is left; its words quiet* (`onSurfaceVariant`, pinned on
  `primaryContainer` by `ContrastTest` since 10.2), struck through as before. Not taken off the
  ground: the engine counts it as what is under way (the sentence says it, and `Timeline` draws
  no line for now while an event holds the hour), so a row without the ground would leave the page
  with no "now" at all; whether a declined event is the reader's now is the engine's question,
  and not this review's. The month's chosen day follows (`AgendaRows.kt` is shared).
- **4. The month opened the morning after** (Phase 9's, found here; archive 03, §15, 23 Nov 2026).
  `MonthViewModel` is the activity's, since the back stack has no store of its own for a page's
  view model, and a `StateFlow` keeps its last value: the month opened on 30 Nov, the app left in
  the recents overnight and the month opened again on 1 Dec began from the state of the 30th, and
  what the page begins on (its month, its chosen day) is remembered from then on. `MonthRoute`
  now begins on a state of the clock's day (`MonthUiState.isOfToday`), drawing the empty page it
  draws before the first read until one comes: a frame or two, and only when the day has changed.
  Opened on a date from Today's month it was right already. *Not the view model's sharing
  changed* (a state that expires when the page goes): the page would lose its month and its
  chosen day each time the reader came back from the calendar app. The predicate has its test;
  the wait itself is the route's, which no test draws (it needs the Hilt graph).
- **5. Today's state off the main thread.** `TodayViewModel` built its state where it collected:
  the agenda at every minute and, in two panes, a month's marks with it. `flowOn(Dispatchers.Default)`,
  as `MonthViewModel` has had since Phase 9. The cost on the main thread was never measured.
- **6. The launch's ground has its test.** `launch_ground` (step 10.1) is the default dress's
  `surface` copied by hand, by day and at night, because a window's theme is read before any code
  runs. `LaunchGroundTest` (`:app`) compares the two in both: schemes generated again with another
  surface now fail a test instead of bringing back the flash.
- **7. Archive 03.** The log was past the ~80 KB of «How it is made» since step 10.4 (over 90 KB with
  these entries), deferred to the phase's close, which is now: the log since 2 Nov 2026 and
  Phases 9 and 10 go to `docs/planning/03-phases-9-10.md`, and the index is written again.

**Checked.** `spotlessCheck`, `test` (every module: 647 tests), `:app:lintDebug`,
`:app:checkForbiddenPermissions`, `:app:assembleDebug`: green; the merged manifest is unchanged.
The two tests of findings 1 and 2 fail on the branch's code as it was and pass on the fixes. The
picture of the declined event under way looked at (`today-declined-under-way`). No README picture
shows any of this: none redrawn. No device on adb: the keyboard's focus and the month opened the
morning after are for the owner's tablet and phone, on the APK of this commit.
