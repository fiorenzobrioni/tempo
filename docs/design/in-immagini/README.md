# «In pictures» («In immagini»): the design's sources

The boards of the canvas the owner approved on 15 Nov 2026
(https://claude.ai/artifact/VFwVjbxyAyWcWRLcRW9uhY, page «Tempo»), **frozen on that day**. They are
the reference of Phase 10 (`PLANNING.md` §11, «Phase 10 — «In pictures», the day in blocks»): where
the plan writes no measure, the measure is here.

| File | Board |
|---|---|
| `TempoCard.dc.html` | The card itself, every form: the one component every board draws |
| `TempoForms.dc.html` | The eight forms, 2×1 to 4×4, at their real sizes in dp, and four grounds |
| `TempoConfig.dc.html` | The settings screen (interactive on the canvas) |

**How to read them.** They are pages in the canvas's Design Component format: without its runtime
they do not open in a browser, they are read as source. The inline styles are the measures (`px` =
dp, `font-size` = sp, the strings are the Italian UI); the blocks' geometry (the window from 8 to
22, a block's start, width and state, the ticks) is computed in the `Component` class at the end of
`TempoCard.dc.html`, by `renderVals()` and its `lane`, `lanePlate` and `ticks` functions. The day
(Sunday 15 Nov, 14:05) is a sample: the card reads the Calendar Provider.

**Not to be edited.** A measure the implementation changes is written in `PLANNING.md` with its
reason (Phase 10's fidelity rule), not here.
