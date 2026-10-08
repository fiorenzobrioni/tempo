# R8 rules for what this module brings: Glance, and through it WorkManager and its Room database.
#
# R8's full mode (the default) keeps no constructor a rule does not name. Two of the libraries'
# own rules predate it and name the class alone, so the constructor that is then called by
# reflection is shrunk away:
#
# - WorkManager 2.10's Room 2.6.1 ships `-keep class * extends androidx.room.RoomDatabase`, and
#   WorkManager builds WorkDatabase_Impl through its no-argument constructor at every start (the
#   startup provider). Without it the minified app crashed at launch, before any screen, with
#   NoSuchMethodException: WorkDatabase_Impl.<init> (a Galaxy Tab S8+ on Android 16, 7 Oct 2026;
#   PLANNING.md §15). Room 2.7+ ships this very rule; it no-ops once Room is newer.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# - Glance 1.2 ships `-keep public class * extends androidx.glance.appwidget.action.ActionCallback`
#   and creates a card's callbacks by their no-argument constructor, on a touch: the same trap,
#   closed before the widgets' first callback (Phase 4).
-keep public class * extends androidx.glance.appwidget.action.ActionCallback { public <init>(); }

# - The calendar's trigger (CalendarChangeWorker) is built by WorkManager's default factory, by
#   reflection through its (Context, WorkerParameters) constructor: kept by name, whatever the
#   library's own rule says in the version that resolves.
-keep class com.callbackdev.tempo.widget.refresh.CalendarChangeWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
