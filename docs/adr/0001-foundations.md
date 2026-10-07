# ADR 0001: Foundations (Phase 0)

- Status: accepted
- Date: 2026-10-07

## Context

Tempo starts from an empty repository, from the owner's concept document «Concept App: Tempo
(Orologio & Agenda)». The owner asked for Passo's structure as the starting point: the same
stack, the same technical choices, the same design, the same licence, so the family of four
(Chiaro, Passo, Saldo, Tempo) is built and maintained one way.

## Decisions

1. **Identity.** Developer namespace `callbackdev`, as the family: package and applicationId
   `com.callbackdev.tempo` (debug: `com.callbackdev.tempo.debug`), modules under
   `com.callbackdev.tempo.*`. Author Fiorenzo Brioni, licence GPL-3.0 (full text in `LICENSE`).
   Repository `fiorenzobrioni/tempo`, the bare app name, as the sisters'.
2. **Passo's build, carried over.** Gradle 9.8.0, AGP 9.4.1 with its built-in Kotlin, Kotlin
   2.4.20, KSP 2.3.12, Compose BOM 2026.09.00, Hilt 2.60.1, Glance 1.2.0, WorkManager 2.10.5
   (pinned, Passo's ADR 0005), Navigation 3 1.2.0, DataStore 1.2.1; the version catalog and the
   convention plugins in `build-logic/` (renamed `tempo.*`); Java 21 without a toolchain;
   ktlint through Spotless; JUnit 4, Truth, Turbine, Robolectric. Passo's reasons hold
   unchanged (Passo ADR 0001).
3. **minSdk 34**, Passo's, target and compile SDK 37 (owner, 7 Oct 2026). 33, Chiaro's and
   Saldo's level, was proposed first: Tempo has no `health` foreground service, the reason Passo
   takes 34, and 33 already gives what Tempo uses (the per-app language picker, themed icons, the
   Android 12 widget APIs, `java.time` in full) while reaching Android 13 phones too. The owner
   chose Passo's floor, so the two apps built from one build-logic share it.
4. **No Room.** Tempo stores no event: the Calendar Provider is the only truth (VISION,
   principle 1). The settings and the widgets' looks are DataStore. Passo's Room convention
   plugin is not carried over; it returns with the first table, if one ever comes.
5. **The permission gate, widened.** Passo's `checkForbiddenPermissions` on every variant's
   merged manifest, with Tempo's own additions: `WRITE_CALENDAR`, `READ_CONTACTS` and
   `WRITE_CONTACTS`. "Read-only" is then something the build checks, not something the README
   promises.
6. **A module joins the build with its first code.** Phase 0 has the modules of v1 (§2) that
   Phase 1 to 4 fill; `:feature:guide` joins in Phase 5. Empty modules build and test as
   "no source".
7. **Signing, as the family.** Tempo's own debug keystore, committed for good (`tempo-debug` /
   `android`, PKCS12, RSA 2048, created 7 Oct 2026, SHA-256 in `keystore/README.md`): each app's
   debug builds are signed apart. The release key is the owner's to create before the first
   release, stored in the same four secrets as the sisters'; until then a tag build would fail
   by name at the keystore step (`release.yml`), which is the intended state.
8. **Versioning from the start.** `tempo.versionName` in `gradle.properties` (0.1.0 for the
   foundations), `versionCode = major * 10000 + minor * 100 + patch`; the release workflow
   refuses a tag that does not match.
9. **Only English and Italian resources ship** (`localeFilters`), as Passo.
10. **Phase 0's screen is Today's clock**, not an empty `Scaffold`: the time and the date in the
    family's hero type, read once a minute on the minute while the page is started
    (`untilNextMinute`, tested). Nothing else is drawn until it is real.

## Consequences

- AGP 9's DSL, as Passo's: snippets from Chiaro (AGP 8.13) or Saldo need translating.
- A change in Passo's build-logic is not inherited: it is copied here by hand when it matters,
  as Passo copies Chiaro's design.
- Android 13 phones cannot install Tempo. Lowering the floor later is one constant
  (`TempoSdk.MIN`) and this ADR's third point; nothing in the code depends on 34.
