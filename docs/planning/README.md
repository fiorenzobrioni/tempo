# PLANNING's archive

The closed periods of Tempo's plan, one per file, **frozen**: they are not edited. The living
plan (the architecture, the phases still open, what is open, the log of the current period) is
[`PLANNING.md`](../../PLANNING.md), which also says how to archive. A decision that changes one of
these is written there, naming the one it changes.

This index is written by `tools/planning_index.py`: it is not edited by hand. Every section
(`##`) and subsection (`###`) heading of the archive with its line, and every dated entry of a log
kept as a list, to look up a reference by name or by date and read from the right place. The log
of the current period is not here: it is in `PLANNING.md`, short, and searched there.


## [01-phases-0-8.md](01-phases-0-8.md): PLANNING, archive 01: from the repository to Phase 8, 7 to 30 October 2026

- L13: 11. Delivery phases
  - L17: Phase 0 — Foundations
  - L41: Phase 1 — The calendar engine (highest risk)
  - L62: Phase 2 — Settings
  - L77: Phase 3 — Today and onboarding
  - L108: Phase 4 — The widget
  - L136: Phase 5 — The guide, accessibility and polish
  - L146: Phase 6 — Release on GitHub (v1.0.0)
  - L158: After 1.0
- L164: 15. Decisions log and open questions
  - L166: Decided
    - L168: 7 Oct 2026, Phase 0. The repository is `fiorenzobrioni/tempo`
    - L169: 7 Oct 2026, Phase 0. Passo's build, copied
    - L170: 7 Oct 2026, Phase 0. `minSdk 34`, Passo's (owner)
    - L171: 7 Oct 2026, Phase 0. No Room
    - L172: 7 Oct 2026, Phase 0. The forbidden list is Passo's, plus `WRITE_CALENDAR`, `READ_CONTACTS`, `WRITE_CONTACTS`
    - L173: 7 Oct 2026, Phase 0. Tempo's own debug key (`tempo-debug` / `android`, committed), not a sister's
    - L174: 7 Oct 2026, Phase 0. The design system is Passo's (itself Chiaro's)
    - L175: 7 Oct 2026, Phase 0. The launcher mark
    - L176: 7 Oct 2026, Phase 0. The concept's corrections (VISION, From the concept to this vision)
    - L178: 7 Oct 2026, owner's review of VISION.md. Every suggestion of the vision accepted, and its open questions answered
    - L187: 7 Oct 2026, Phase 1. A timed event that covers a whole date (a conference from Monday 9:00 to Wednesday 17:00) is in…
    - L188: 7 Oct 2026, Phase 1. Free time is the time between two busy events, plus, today, from now to the next one
    - L189: 7 Oct 2026, Phase 1. An instance whose calendar is not in the calendar list (a sync landing between the two queries) is…
    - L190: 7 Oct 2026, Phase 1. The provider's codes read defensively
    - L191: 7 Oct 2026, Phase 1. `ACTION_EDIT` is not built
    - L192: 7 Oct 2026, Phase 1. "Denied for good" needs to know whether the permission was ever asked, which Android does not say
    - L193: 7 Oct 2026, Phase 1. The query window is today's start minus a day to the horizon's end plus a day (`AgendaWindow`), so…
    - L195: 7 Oct 2026, Phase 2. A calendar choice finds its calendar by id within the same account first (a calendar renamed in its…
    - L196: 7 Oct 2026, Phase 2. The calendar's colour is a dot (`CalendarDot`), stepped in lightness by `markColor` until it reads…
    - L197: 7 Oct 2026, Phase 2. The guide's card is not drawn until the guide exists (Phase 5), and the agenda's settings are on…
    - L198: 7 Oct 2026, Phase 2. The permission card's button is a filled one
    - L200: 7 Oct 2026, Phase 3. The day's sentence names what the reader acts on next
    - L201: 7 Oct 2026, Phase 3. Free rows on today only
    - L202: 7 Oct 2026, Phase 3. Today's past events fold into one "earlier" row from two of them up, so the morning never pushes…
    - L203: 7 Oct 2026, Phase 3. The clock is the hero at 80sp, grown with the reader's text size through the density (Android 14's…
    - L204: 7 Oct 2026, Phase 3. The new-event button floats, as the owner's concept asked
    - L205: 7 Oct 2026, Phase 3. A row's words are cleared from the semantics and said once, as one sentence, by its description
    - L206: 7 Oct 2026, after Phase 3. The launcher ring turns slate, a pale grey-blue to a deep slate (owner: the peach, rose and berry…
    - L207: 7 Oct 2026, after Phase 3. The welcome page shows the launcher icon itself, cropped round as a launcher shows it, in place of…
    - L208: 7 Oct 2026, after Phase 3. Signing until the release key exists (owner: a temporary key, so the app is never reinstalled)
    - L209: 7 Oct 2026, after Phase 3. A crash at launch on a Galaxy Tab S8+ (owner), not reproduced here
    - L210: 7 Oct 2026, after Phase 3. The tablet's crash is the release build's only (owner: the debug build starts), so R8's
    - L211: 7 Oct 2026, after Phase 3. The tablet's trace named it
    - L213: 8 Oct 2026, Phase 4. A touch on the time can open the phone's clock app
    - L214: 8 Oct 2026, Phase 4. The cards say clock times and counts, never minutes from now
    - L215: 8 Oct 2026, Phase 4. On a card, a date's all-day events share one row ("All day · Design Week, Anna's birthday", counted…
    - L216: 8 Oct 2026, Phase 4. The default 4×2 «Agenda» is the side form (the clock and the date in a column, five events beside…
    - L217: 8 Oct 2026, Phase 4. The date on a card is a `TextClock`, so it turns at midnight exactly whatever the alarm's delivery
    - L218: 8 Oct 2026, Phase 4. An event's touch opens its occurrence in the calendar app by default (Tempo's Today is the other…
    - L219: 8 Oct 2026, Phase 4. The widgets' refresh also runs when Tempo's settings change and when the reader leaves the app…
    - L220: 8 Oct 2026, Phase 4. The first run's last button is now «Next» on the calendar's page and «Done» on the widgets' page
    - L222: 8 Oct 2026, after Phase 4. «In words» takes Passo's «At a glance» row sizes (owner, from the phone: beside Passo's card the…
    - L223: 8 Oct 2026, after Phase 4. «Agenda»'s one-row date follows (owner)
    - L224: 8 Oct 2026, after Phase 4. One set of sizes across the family's cards (owner: "where you see other points to make uniform, do")
    - L225: 8 Oct 2026, after Phase 4. A line of Glance text is budgeted with the pixel its font metrics are rounded up to (+1 dp)
    - L227: 8 Oct 2026, after Phase 4. «In words» is redesigned around a dial (owner, from the phone: on a 4×1 the time in words at 34 sp…
    - L228: 8 Oct 2026, after Phase 4. A crash on the owner's Galaxy S24 Ultra, retraced with the run's mapping
    - L230: 9 Oct 2026, review of the widgets. The calendar's broadcast, and a repaint that stays until it is drawn
    - L231: 9 Oct 2026, review of the widgets. The cards' words and layout, from the gallery (owner: "all of them")
    - L233: 9 Oct 2026, review of Today. The hero, redrawn
    - L234: 9 Oct 2026, review of Today. The agenda's layout
    - L236: 9 Oct 2026, review of Today. No second view of Today
    - L238: 10 Oct 2026, before 1.0. The release key, checked before a tag
    - L239: 10 Oct 2026, before 1.0. The welcome page, from the phone
    - L240: 10 Oct 2026, before 1.0. The first run's and Settings' line for «In words» still said what the card was before its dial…
    - L241: 10 Oct 2026, before 1.0. The launcher ring, azure to cobalt
    - L242: 10 Oct 2026, before 1.0. The checks on the phone, done
    - L243: 10 Oct 2026, before 1.0. The widgets' settings screen opens only for one of Tempo's own cards (`configTarget`…
    - L244: 10 Oct 2026, Phase 5. The guide
    - L245: 10 Oct 2026, Phase 5. The accessibility pass is marked done by the owner after the final review of the whole
    - L246: 10 Oct 2026, Phase 6. 1.0.0, ready to tag
    - L247: 10 Oct 2026, Phase 6. Tempo 1.0.0 released on GitHub (`v1.0.0`, tagged by the owner: this sandbox's git proxy refuses a…
    - L248: 10 Oct 2026, after 1.0. The sister apps stay out of the app and the code
    - L249: 10 Oct 2026, after 1.0. A run of empty days says it is a run
    - L250: 10 Oct 2026, after 1.0. 1.0.1, ready to tag
    - L252: 13 Oct 2026, after 1.0. The widgets' type scale
    - L254: 13 Oct 2026, after 1.0. The launcher's corner
    - L256: 16 Oct 2026, after 1.0. The widget settings, in the family's one order
    - L258: 18 Oct 2026, after 1.0. The family's icons, made one suite
    - L260: 18 Oct 2026, after 1.0. The family's onboarding frame
    - L262: 18 Oct 2026, after 1.0. Time zones and clock changes, reviewed
    - L264: 30 Oct 2026, after 1.0. A review of the whole app
    - L271: 30 Oct 2026, after 1.0. Coherence with the family: Tempo follows Passo
    - L272: 30 Oct 2026, after 1.0. The review's proposals, planned
    - L274: 30 Oct 2026, after 1.0. The owner's answers on Phase 8
    - L275: 30 Oct 2026, Phase 8.1. Today: one free row, the alarm's door, the dial in the calendars' colours, the family's shapes
    - L280: 30 Oct 2026, Phase 8.2. The cards I: the rest of the day where the alarm was planned, «Agenda»'s list, the states, the…
    - L292: 30 Oct 2026, Phase 8.3. «In words» uses its room: the dial grows with the card, the next events as rows
    - L300: 30 Oct 2026, Phase 8.5. Step 8.4 on hold
    - L301: 30 Oct 2026, Phase 8.5. Time zones: the event's own, and a second clock
    - L312: 30 Oct 2026, Phase 8.6. Two weeks ahead, the first day's tip, the new event brought into view
    - L325: 30 Oct 2026, Phase 8.7. Today on a large screen: two panes
