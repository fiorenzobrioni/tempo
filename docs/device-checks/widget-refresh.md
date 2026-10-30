# Device check: the widgets on the home screen, and what they cost

PLANNING.md §9.6 and §11 Phase 4 (acceptance). The cards are tested on the JVM at the family's
reference grants; what only a phone can say is how the launcher grants them, when Android
delivers the refresh, and that the battery pays nothing while nothing changes.

Use the debug build (`com.callbackdev.tempo.debug`) or the debug-signed release; replace the
package below with the one installed.

```bash
PKG=com.callbackdev.tempo.debug
```

## 1. Beside Chiaro's and Passo's cards

Place «Agenda» and «In words» next to a Chiaro and a Passo card, in the same dress (the default
blue). Expected: the same corner, the same insets, the same inks. Resize each from one cell to
four by three: every size is a composition, nothing is cut but a long title, "N more" counts
what is left out.

## 2. The clock costs nothing

Watch the cards' time across a minute's change with the screen on. Expected: right to the
minute. Then:

```bash
adb shell dumpsys alarm | grep -i tempo
```

Expected: at most **one** alarm of Tempo's, type `RTC` (not `RTC_WAKEUP`), at the next start or
end of an event, or at midnight.

## 3. A calendar change reaches the card

With the screen on, create an event for later today in the calendar app and go back to the home
screen. Expected: on the card within about a minute.

```bash
adb shell dumpsys jobscheduler | grep -i -A3 tempo
```

Expected: one job of Tempo's waiting on a content trigger (`content://com.android.calendar`),
none periodic.

## 4. A finished event leaves the card

Note an event's end time; turn the screen off until after it; turn it on. Expected: the event
is gone within moments of the screen coming on (the alarm is delivered when the phone wakes).

## 5. With the screen off, nothing

```bash
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle
# wait a few minutes, then
adb shell dumpsys deviceidle unforce
adb shell dumpsys battery reset
adb shell dumpsys batterystats --charged $PKG | grep -i -E "wake|alarm|job"
```

Expected: no wakelock and no wake-up alarm of Tempo's; jobs only for calendar changes.

## 6. The touches

On a card's settings (long press, then the pencil or "Widget settings"), set «Touching the time
and the date» to each choice and touch the time: Tempo, the calendar app at today, the clock
app. Touch an event: its occurrence in the calendar app (the right one for a repeating event:
today's, not tomorrow's), or Tempo.

## 7. The next alarm follows the clock app (PLANNING.md §11, step 8.2)

Place an «Agenda» card at its default 4×2, with «Next alarm» on in its settings. Open the Clock
app, set an alarm for tomorrow morning, then go to the home screen and leave Tempo in the
background (do not open it). In another window:

```bash
adb logcat -s TempoWidget
```

Then, each time from the Clock app and back to the home screen: move the alarm by an hour; turn
it off; turn it on again; set one for later today. Expected after each change, within a few
seconds and with no Tempo screen opened: a line `NEXT_ALARM_CLOCK_CHANGED: 1 card(s) drawn in …`,
and the card under the date saying the new time ("Tomorrow 07:00", "07:00"), or nothing when no
alarm rings within 24 hours. Touch the alarm on the card: the Clock app opens.

To see the broadcast reach a Tempo that is not running at all, stop its process first (not a
force stop, which keeps an app from any broadcast until it is opened):

```bash
adb shell am kill $PKG
```

Expected: the same line, after a `Start proc … for broadcast` of Tempo's in `adb logcat`.

If no line comes, the alarm comes off the cards and step 8.2's other way is built (the day in a
few words in the column), as the owner decided (PLANNING.md §15, 30 Oct 2026).

## Results

| Check | Phone, Android, launcher | Result | Date |
|---|---|---|---|
| 1 |  |  |  |
| 2 |  |  |  |
| 3 |  |  |  |
| 4 |  |  |  |
| 5 |  |  |  |
| 6 |  |  |  |
| 7 | Galaxy S24 Ultra (owner's) | **Not reliable.** First try: an alarm moved and one turned off reached the card in about 5 s each. Second try, later: no change on the card after about 30 s with Tempo in the background; opening Tempo showed the alarm, and the card changed about 10 s after leaving it. Under investigation, with `adb logcat -s TempoWidget` | 30 Oct 2026 |
