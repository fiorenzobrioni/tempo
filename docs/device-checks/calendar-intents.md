# Device check: the calendar app and Tempo's intents

PLANNING.md §4.7 and §11 (Phase 1, moved to Phase 3). Tempo hands every change to the reader's
calendar app through four intents. Calendar apps differ in what they do with them; this check
records what the owner's apps do, so Tempo builds only on what works.

It needs no Tempo build: `adb` sends the same intents Tempo's buttons will send. With Phase 3's
screen the same check is done by touching Today's buttons.

## Setup

A phone with USB debugging on, `adb devices` listing it, and an event on the calendar today.
Times are epoch milliseconds; `date +%s000` gives now.

```bash
NOW=$(date +%s000)
```

## The four intents

1. **A day** (touching the date):

   ```bash
   adb shell am start -a android.intent.action.VIEW -d "content://com.android.calendar/time/$NOW"
   ```

   Expected: the calendar app on today.

2. **A new event** (the button), starting in an hour and lasting an hour:

   ```bash
   B=$((NOW + 3600000)); E=$((B + 3600000))
   adb shell am start -a android.intent.action.INSERT -d content://com.android.calendar/events \
     --el beginTime $B --el endTime $E
   ```

   Expected: the new-event page, the start and end filled in. Then Back without saving: where
   does it land?

3. **One occurrence of an event** (touching it). Find an event id first; the `content` command may
   need the calendar permission granted to the shell, and if it refuses, the id is in the
   calendar app's share link or in Tempo's Phase 3 screen:

   ```bash
   adb shell content query --uri "content://com.android.calendar/instances/when/$NOW/$((NOW + 86400000))" \
     --projection event_id:title:begin:end
   adb shell am start -a android.intent.action.VIEW -d content://com.android.calendar/events/<event_id> \
     --el beginTime <begin> --el endTime <end>
   ```

   Expected: that event's page, at that occurrence (for a recurring event, the right date).

4. **Editing** (not built; this decides whether it ever is):

   ```bash
   adb shell am start -a android.intent.action.EDIT -d content://com.android.calendar/events/<event_id>
   ```

   Expected, if honoured: the event's editor.

## Record

For each calendar app tried (name and version), one line per intent: opened what, prefilled or
not, where Back leads. The result goes into PLANNING.md §15, and `ACTION_EDIT` joins
`CalendarIntents` only if every app the owner uses honours it.
