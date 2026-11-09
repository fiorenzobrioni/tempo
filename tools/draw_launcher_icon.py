#!/usr/bin/env python3
"""Draws Tempo's launcher mark: the adaptive icon's three layers in app/src/main/res/drawable.

    python3 tools/draw_launcher_icon.py

The mark is THE DAY'S PAGE AND THE CLOCK (owner, 9 Nov 2026, when the family's marks were
redrawn as one series): a white agenda page with three lines of the day, and in front of it, at
the lower right, an amber clock face with its hands cut white, the minute hand at twelve and the
hour hand towards four. The two halves of the app in one sign. A band of the ground keeps the
clock apart from the page, as the clock's gap kept it apart from the ring it replaced.

The series' rules, the same in every app of the family:
- the badge's ground is the app's own colour, here the coral, from its light tone at the upper
  left to its deep tone at the lower right; the symbol is white with one amber accent;
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to a darker one at the lower right (the white to its deep tone, which is still nearly
  white, the amber to its base), over the box's larger side; no shadow (the store and the
  launchers add their own);
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches it.

The page is the heavier half, so the drawing sits a little right of the middle of its box
(FIT_SHIFT): centred by its box it looked off to the left (owner, 9 Nov 2026); here its box is
1.6 units right of the centre and its weight 0.6 left of it.

Re-running this script IS the drawing: ic_launcher_background.xml, ic_launcher_foreground.xml
and ic_launcher_monochrome.xml are its output and are not edited by hand. Only the standard
library is used, so any Python 3 runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"

CENTRE = 54.0
KEYLINE = 28.0  # the symbol's reach from the centre: measured 26.7

# The family's palette, the tones this mark uses: (light, base, deep).
CORAL = ("FF8E74", "F0604A", "D3432F")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")
LINE_ALPHA = "8C"  # the page's lines: the coral's light tone at 55%

# The drawing, on the 108 grid, then fitted: scaled about the centre, and moved by FIT_SHIFT.
PAGE = (32.5, 30.0, 65.5, 72.0, 6.5)  # left, top, right, bottom, corner
LINES = [(38.5, 40.0, 59.5), (38.5, 48.5, 55.0), (38.5, 57.0, 49.0)]  # from, height, to
LINE_WIDTH = 4.4
CLOCK = (63.5, 63.5, 13.5)  # middle and radius
GAP = 2.6  # the band of ground between the clock and the page
MINUTE, HOUR, HAND_WIDTH = 9.6, 7.0, 2.9  # the hands' lengths to their round tips
HOUR_ANGLE = 30.0  # degrees clockwise from three o'clock: towards four
FIT_SCALE = 0.935
FIT_SHIFT = (0.9, 0.4)

FULL = "M 0,0 H 108 V 108 H 0 Z"

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def lit(box):
    """A shape's light: from the upper-left corner of its box to the lower-right one, along the
    diagonal of a square on the box's larger side, so every shape is lit at the same angle."""
    x0, y0, x1, y1 = box
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2
    return (cx - r, cy - r), (cx + r, cy + r)


def ramp(attr, box, light, deep, indent="    "):
    (sx, sy), (ex, ey) = lit(box)
    return (
        f'{indent}<aapt:attr name="android:{attr}">\n'
        f'{indent}    <gradient android:type="linear" android:startX="{f(sx)}" android:startY="{f(sy)}" '
        f'android:endX="{f(ex)}" android:endY="{f(ey)}">\n'
        f'{indent}        <item android:offset="0" android:color="#FF{light}"/>\n'
        f'{indent}        <item android:offset="1" android:color="#FF{deep}"/>\n'
        f"{indent}    </gradient>\n{indent}</aapt:attr>\n"
    )


def shaded(d, box, light, deep):
    return f'<path android:pathData="{d}">\n{ramp("fillColor", box, light, deep)}</path>\n'


def flat(d, argb):
    return f'<path android:pathData="{d}" android:fillColor="#{argb}"/>\n'


def vector(body, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        f"{body}</vector>\n"
    )


def fitted(body):
    return (
        f'<group android:pivotX="{f(CENTRE)}" android:pivotY="{f(CENTRE)}" android:scaleX="{FIT_SCALE:g}" '
        f'android:scaleY="{FIT_SCALE:g}" android:translateX="{f(FIT_SHIFT[0])}" android:translateY="{f(FIT_SHIFT[1])}">\n'
        f"{body}</group>\n"
    )


def without(holes, body):
    """The body with every hole cut out: one group per hole, since nested clips intersect, so
    the body keeps what is outside them all (in one clip, two overlapping holes fill each other)."""
    opened = "".join(f'<group>\n<clip-path android:pathData="{FULL} {hole}"/>\n' for hole in holes)
    return opened + body + "</group>\n" * len(holes)


def circle(cx, cy, r):
    """A circle walked counter-clockwise (sweep flag 0): filled on its own, a hole in a clip."""
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def capsule(a, b, w):
    """A segment with round ends, walked counter-clockwise: a hole in a clip too."""
    r = w / 2
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    nx, ny = -uy * r, ux * r
    p1, p2, p3, p4 = (a[0] + nx, a[1] + ny), (b[0] + nx, b[1] + ny), (b[0] - nx, b[1] - ny), (a[0] - nx, a[1] - ny)
    tips = (b[0] + ux * r, b[1] + uy * r), (a[0] - ux * r, a[1] - uy * r)
    poly = [p1, p2, tips[0], p3, p4, tips[1]]
    if sum(p[0] * q[1] - q[0] * p[1] for p, q in zip(poly, poly[1:] + poly[:1])) > 0:
        p1, p2, p3, p4 = p4, p3, p2, p1  # the other way round
    return (
        f"M {f(p1[0])},{f(p1[1])} L {f(p2[0])},{f(p2[1])} A {f(r)},{f(r)} 0 0,0 {f(p3[0])},{f(p3[1])} "
        f"L {f(p4[0])},{f(p4[1])} A {f(r)},{f(r)} 0 0,0 {f(p1[0])},{f(p1[1])} Z"
    )


def rounded_rect(x0, y0, x1, y1, r):
    return (
        f"M {f(x1 - r)},{f(y0)} L {f(x0 + r)},{f(y0)} A {f(r)},{f(r)} 0 0,0 {f(x0)},{f(y0 + r)} "
        f"L {f(x0)},{f(y1 - r)} A {f(r)},{f(r)} 0 0,0 {f(x0 + r)},{f(y1)} L {f(x1 - r)},{f(y1)} "
        f"A {f(r)},{f(r)} 0 0,0 {f(x1)},{f(y1 - r)} L {f(x1)},{f(y0 + r)} A {f(r)},{f(r)} 0 0,0 {f(x1 - r)},{f(y0)} Z"
    )


def lines():
    return [capsule((a, y), (b, y), LINE_WIDTH) for a, y, b in LINES]


def hands():
    cx, cy, _ = CLOCK
    a = math.radians(HOUR_ANGLE)
    w = HAND_WIDTH
    minute = capsule((cx, cy), (cx, cy - MINUTE + w / 2), w)
    hour = capsule((cx, cy), (cx + (HOUR - w / 2) * math.cos(a), cy + (HOUR - w / 2) * math.sin(a)), w)
    return [minute, hour]


def background():
    body = shaded(FULL, (18, 18, 90, 90), CORAL[0], CORAL[2])
    return HEADER.format(what="The badge's ground: the app's coral, lit from the upper left.") + vector(body)


def foreground():
    x0, y0, x1, y1, r = PAGE
    cx, cy, cr = CLOCK
    page = shaded(rounded_rect(x0, y0, x1, y1, r), (x0, y0, x1, y1), WHITE[0], WHITE[2]) + "".join(
        flat(d, LINE_ALPHA + CORAL[0]) for d in lines()
    )
    body = (
        without([circle(cx, cy, cr + GAP)], page)
        + shaded(circle(cx, cy, cr), (cx - cr, cy - cr, cx + cr, cy + cr), SUN[0], SUN[1])
        + flat(" ".join(hands()), "FFFFFFFF")
    )
    what = (
        "The mark: the day's page with three lines, and an amber clock in front of it at the lower\n"
        f"     right, its hands white; inside {KEYLINE:g} units of the centre, the keyline."
    )
    return HEADER.format(what=what) + vector(fitted(body))


def monochrome():
    x0, y0, x1, y1, r = PAGE
    cx, cy, cr = CLOCK
    body = without([circle(cx, cy, cr + GAP), *lines()], flat(rounded_rect(x0, y0, x1, y1, r), "FFFFFFFF")) + without(
        hands(), flat(circle(cx, cy, cr), "FFFFFFFF")
    )
    what = (
        "The themed icon (Android 13+): the system reads only the alpha, so the page's lines and the\n"
        "     clock's hands are cut out of their shapes, and the band round the clock with them."
    )
    return HEADER.format(what=what) + vector(fitted(body), aapt=False)


if __name__ == "__main__":
    for name, text in (
        ("ic_launcher_background.xml", background()),
        ("ic_launcher_foreground.xml", foreground()),
        ("ic_launcher_monochrome.xml", monochrome()),
    ):
        (RES / name).write_text(text)
        print("wrote", RES / name)
