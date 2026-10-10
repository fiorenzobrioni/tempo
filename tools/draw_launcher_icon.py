#!/usr/bin/env python3
"""Draws Tempo's launcher mark: the two adaptive-icon layers in app/src/main/res/drawable, and the
mark on no ground as the welcome page shows it (core/designsystem/src/main/res/drawable/ic_app_mark.xml).

    python3 tools/draw_launcher_icon.py

The mark is Chiaro's ring, as Passo and Saldo carry it: the same ring (radius 21, stroke 10) on
the same warm white, cut by an amber emblem the way Chiaro's sun (upper right), Passo's shoe
print (upper left) and Saldo's coin (lower right) cut theirs. Tempo's emblem is a small clock
face at the lower left, the one place none of its sisters has hers, so the four marks side by
side turn the emblem once round the ring. The ring is slate, shading clockwise from just past the
clock round to it again, the way the hours go: a pale morning grey-blue to the deep slate of a
watch's case, so the amber face is the one warm thing in the mark. The clock's hands are cut out
of its face: the minute hand at twelve, the hour hand towards four, the drawing the usual clock
symbol uses, so the face reads as a clock at a launcher's size (ten past ten, tried first, read
as a tick).

Re-running this script IS the drawing: ic_launcher_foreground.xml, ic_launcher_monochrome.xml and
ic_app_mark.xml are its output and are not edited by hand. Only the standard library is used, so
any Python 3 runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
MARK = ROOT / "core/designsystem/src/main/res/drawable/ic_app_mark.xml"

CENTRE = 54.0
RING_RADIUS = 21.0  # Chiaro's
RING_WIDTH = 10.0  # Chiaro's
FACE_ANGLE = 140.0  # degrees clockwise from three o'clock: Chiaro -40, Passo 220, Saldo 40
FACE_RADIUS = 8.2  # Chiaro's sun, Saldo's coin
GAP_RADIUS = 11.2  # Chiaro's: the clear band around the emblem, where it cuts the ring

# The hands, cut out of the face: (direction in degrees clockwise from three o'clock, length).
# The minute hand at twelve, the hour hand towards four.
HANDS = [(-90.0, 5.9), (32.0, 4.3)]
HAND_WIDTH = 1.8
HAND_TAIL = 0.9  # how far each hand reaches back past the centre, so the two meet in one hub

# The ring, from just past the clock round to it again: (position along the sweep, colour). Slate,
# from a pale morning grey-blue to a watch case's deep slate. A colour none of the sisters' rings
# has (Chiaro's is a sky, amber through violet to night blue; Passo's green; Saldo's sea green and
# brick), and calm enough that the amber face is the mark's one warm note. Two rings came before
# it: violet sat too close to Chiaro's (owner, 7 Oct 2026), and the peach, rose and berry day that
# replaced it did not please (owner, 7 Oct 2026). Brighter blues met Chiaro's night again, copper
# melted into the amber face, and a plain graphite read as a disabled icon.
RING_STOPS = [(0.0, "C9D3E0"), (0.45, "6A7F9C"), (1.0, "283548")]
AMBER = ("FFC658", "EF8618")  # the family's emblem amber (Chiaro's sun, Passo's print, Saldo's coin)


def f(v):
    return f"{v:.2f}"


def point(deg, r=RING_RADIUS, c=CENTRE):
    a = math.radians(deg)
    return c + r * math.cos(a), c + r * math.sin(a)


def circle(cx, cy, r):
    """A circle walked counter-clockwise (sweep flag 0): filled on its own, a hole in a clip."""
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def mix(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(ca, cb))


def signed_area(poly):
    return sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(poly, poly[1:] + poly[:1])) / 2


def hand(deg, length):
    """One hand as a rectangle from just behind the face's centre outwards, walked against the
    canvas (negative area), so it is a hole in a clip under the nonzero rule."""
    cx, cy = point(FACE_ANGLE)
    ux, uy = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    px, py = -uy, ux
    corners = [
        (cx + ux * r + px * s * HAND_WIDTH / 2, cy + uy * r + py * s * HAND_WIDTH / 2)
        for r, s in ((-HAND_TAIL, -1), (length, -1), (length, 1), (-HAND_TAIL, 1))
    ]
    if signed_area(corners) > 0:
        corners.reverse()
    return "M " + " L ".join(f"{f(x)},{f(y)}" for x, y in corners) + " Z"


def without(hole):
    """A clip that keeps the canvas less [hole]: the canvas is walked clockwise, the hole the
    other way (Passo's and Saldo's clips, the same rule)."""
    return f'<clip-path android:pathData="M 0,0 H 108 V 108 H 0 Z {hole}"/>'


def ring_stops():
    """The day as one sweep (which starts at three o'clock): its seam hides under the clock."""

    def colour_at(t):
        for (t0, c0), (t1, c1) in zip(RING_STOPS, RING_STOPS[1:]):
            if t0 <= t <= t1:
                return mix(c0, c1, (t - t0) / (t1 - t0))
        raise ValueError(t)

    def colour(deg):
        return colour_at(((deg - FACE_ANGLE) % 360) / 360)

    stops = [(0.0, colour(0.0))]
    stops += [((FACE_ANGLE + t * 360) % 360, c) for t, c in RING_STOPS[1:-1]]
    stops += [(FACE_ANGLE - 0.2, RING_STOPS[-1][1]), (FACE_ANGLE + 0.2, RING_STOPS[0][1]), (360.0, colour(360.0))]
    stops.sort()
    return "\n".join(
        f'                <item android:offset="{d / 360:.4f}" android:color="#FF{c}"/>' for d, c in stops
    )


def ring(stroke):
    cx, cy = point(FACE_ANGLE)
    return f"""<group>
    {without(circle(cx, cy, GAP_RADIUS))}
    <path android:pathData="{circle(CENTRE, CENTRE, RING_RADIUS)}" android:strokeWidth="{f(RING_WIDTH)}" android:fillColor="#00000000"{stroke}
</group>"""


def face(fill):
    """The clock face with its hands cut out: one group per hand, nested clips intersect, so
    the face keeps what lies outside both."""
    cx, cy = point(FACE_ANGLE)
    clips = [f"<group>\n    {without(hand(d, length))}" for d, length in HANDS]
    return "\n".join(clips) + f'\n<path android:pathData="{circle(cx, cy, FACE_RADIUS)}"{fill}\n' + "</group>\n" * len(clips)


def amber_fill():
    cx, cy = point(FACE_ANGLE)
    return f""">
    <aapt:attr name="android:fillColor">
        <gradient android:type="linear" android:startX="{f(cx - 6)}" android:startY="{f(cy - 6)}" android:endX="{f(cx + 6)}" android:endY="{f(cy + 6)}">
            <item android:offset="0" android:color="#FF{AMBER[0]}"/>
            <item android:offset="1" android:color="#FF{AMBER[1]}"/>
        </gradient>
    </aapt:attr>
</path>"""


def sweep_stroke():
    return f""">
        <aapt:attr name="android:strokeColor">
            <gradient android:type="sweep" android:centerX="{f(CENTRE)}" android:centerY="{f(CENTRE)}">
{ring_stops()}
            </gradient>
        </aapt:attr>
    </path>"""


HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def vector(body, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{body}
</vector>
"""


# The welcome page's mark is cut this close round the centre: the face's edge is 29.2 units out.
MARK_HALF = 30.0


def mark():
    """The foreground's ring and clock face on no ground, for the welcome page (an app's module
    cannot reach the adaptive icon itself). The warm white disc it first stood on read as a
    sticker on the page (owner, 10 Oct 2026); without it the mark is cut close, so the ring is as
    large in fewer dp."""
    body = ring(sweep_stroke()) + "\n" + face(amber_fill())
    side = f(2 * MARK_HALF)
    return f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="{side}dp"
    android:height="{side}dp"
    android:viewportWidth="{side}"
    android:viewportHeight="{side}">
<group android:translateX="{f(MARK_HALF - CENTRE)}" android:translateY="{f(MARK_HALF - CENTRE)}">
{body}
</group>
</vector>
"""


def main():
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_foreground.xml").write_text(
        HEADER.format(
            what="The mark: Chiaro's ring shading through slate, cut by an amber clock\n"
            "     face at the lower left, its hands cut out. Everything sits inside the\n"
            "     33-unit safe circle of every launcher mask (ring edge 26, face edge 29.2)."
        )
        + vector(ring(sweep_stroke()) + "\n" + face(amber_fill()))
    )
    (RES / "ic_launcher_monochrome.xml").write_text(
        HEADER.format(
            what="The themed icon (Android 13+): the foreground's shapes in one ink, which the system\n"
            "     tints: the ring with its cut, and the face with its hands."
        )
        + vector(ring(' android:strokeColor="#FF000000"/>') + "\n" + face(' android:fillColor="#FF000000"/>'), aapt=False)
    )
    MARK.parent.mkdir(parents=True, exist_ok=True)
    MARK.write_text(
        HEADER.format(
            what="The launcher icon's mark for the welcome page: the ring and the clock face, on\n"
            "     no ground of their own."
        )
        + mark()
    )
    print("wrote", RES / "ic_launcher_foreground.xml", RES / "ic_launcher_monochrome.xml", "and", MARK)


if __name__ == "__main__":
    main()
