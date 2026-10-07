# Keystores

One keystore lives in this folder, committed **on purpose**. It is not secret: its passwords are
written below.

| File | Alias | Store / key password | What it signs |
|---|---|---|---|
| `debug.keystore` | `tempo-debug` | `android` / `android` | Debug builds, and the debug-signed release APK CI builds for testing (`-PsignReleaseWithDebugKey`) |

Do not regenerate it: every debug install would have to be uninstalled. Committing it means
debug APKs from CI and from any machine share one signature and update each other in place, the
same arrangement as Chiaro, Passo and Saldo. It is Tempo's own key (created on 7 Oct 2026, RSA
2048, PKCS12), not a copy of a sister's: each app's debug builds are signed apart.

SHA-256 certificate fingerprints:

- `debug.keystore`: `6A:3C:A0:4B:5E:DC:B6:63:9B:8C:21:5B:34:53:A4:95:DA:EB:5C:3B:AC:F5:4A:6A:7B:30:59:FE:AA:DF:22:EB`
- release key: not created yet (PLANNING.md §11 Phase 0, the owner's step). Its fingerprint goes
  here and in the root README once it exists.

## The release key

It must **never** enter the repo (`.gitignore` refuses `*.jks`, `*.keystore`, `*.p12`, `*.pfx`
outside `debug.keystore`). It signs every release for the life of the app, and a future Google
Play listing must reuse it (PLANNING.md §11 Phase 7), so losing it means users cannot update
without uninstalling. It is kept outside the repo, with an offline backup.

Creating it (once, by the owner, on a machine of theirs):

```bash
keytool -genkeypair -v -keystore tempo-release.jks -storetype PKCS12 \
  -alias tempo -keyalg RSA -keysize 4096 -validity 10950
```

### In GitHub Actions

`release.yml` reads it from four repository secrets (Settings, Secrets and variables, Actions),
the same names as Chiaro's, Passo's and Saldo's:

- `KEYSTORE_BASE64`: the keystore file, `base64 -w0 tempo-release.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow decodes the keystore into the runner's temp folder, passes the rest to Gradle as
`ORG_GRADLE_PROJECT_*` variables, and stops by name if a secret is missing. Nothing secret is
written to the logs or to the command line.

### Locally

Put the four properties in `~/.gradle/gradle.properties`:

```properties
TEMPO_KEYSTORE=/absolute/path/to/tempo-release.jks
TEMPO_KEYSTORE_PASSWORD=...
TEMPO_KEY_ALIAS=...
TEMPO_KEY_PASSWORD=...
```

With all four set, `./gradlew :app:assembleRelease` signs with the release key; without them the
release build is unsigned, and `-PsignReleaseWithDebugKey` signs it with the debug key for
testing only.
