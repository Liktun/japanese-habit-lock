# Release signing

## The one thing that matters

**The release keystore cannot be replaced.** Android identifies an app by the
certificate that signed it. Lose the keystore and you can never ship an update to
the same Play listing again — you would have to publish a new app under a new
package name and every existing install would be orphaned. Leak it and someone
else can sign builds that Android treats as genuinely yours.

It is not in this repository and it never should be. `.gitignore` blocks `*.jks`,
`*.keystore` and `keystore.properties`.

## Where it lives

| Copy | Location | Purpose |
|---|---|---|
| Working | `~/keystore/japanese-habit-lock-release.jks` on the build VPS | Local `assembleRelease` |
| CI | GitHub Secret `KEYSTORE_BASE64` on `Liktun/japanese-habit-lock` | Signed builds in Actions |
| **Offline** | **not yet created — see below** | **Survives losing both of the above** |

The first two are a single point of failure with a shared fate: both depend on
accounts and machines that can be lost. Make a third copy somewhere that is
neither, such as a password manager entry or an encrypted archive on a drive you
control.

```bash
# Run this and store the output file somewhere durable and private.
tar czf ~/habitlock-signing-backup.tgz -C ~ keystore/
# Then copy it off the VPS and delete the local tarball.
```

## Certificate

```
Alias:       habitlock
Algorithm:   RSA 4096
Validity:    10950 days (~30 years, from 2026-08-27)
DN:          CN=Liktun, OU=Japanese Habit Lock, O=Liktun, C=CA
SHA-256:     E3:5B:C2:4A:32:FF:C5:63:7A:9C:FA:61:D1:63:BE:71:
             3A:02:5B:80:9F:02:45:72:A8:3A:B0:50:86:19:87:1D
```

Play requires a validity end date after 2033-10-22; 30 years clears that with room
to spare. The fingerprint above is what a signed APK must report — if a build ever
shows a different one, something has replaced the signing key and it must not be
published.

## Building a signed release locally

`keystore.properties` must exist at the repository root (git-ignored):

```properties
storeFile=/home/hermes/keystore/japanese-habit-lock-release.jks
storePassword=<from ~/keystore/keystore.properties>
keyAlias=habitlock
keyPassword=<same as storePassword>
```

The store and key passwords are identical because PKCS12 keystores — the modern
default — do not support a separate key password. `keytool` warns and ignores the
`-keypass` value; the properties file records what is actually true rather than
what was asked for.

```bash
export JAVA_HOME=~/toolchain/jdk17
export ANDROID_HOME=~/toolchain/android-sdk
./gradlew assembleRelease bundleRelease
```

Outputs:
- `app/build/outputs/apk/release/app-release.apk` — sideloading
- `app/build/outputs/bundle/release/app-release.aab` — Play Store

Without `keystore.properties`, the build still succeeds and produces an *unsigned*
APK. That is deliberate so a fresh clone is not broken, but it means **a successful
build is not proof of a signature.** Always verify:

```bash
$ANDROID_HOME/build-tools/*/apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

CI runs exactly this check for the same reason.

## CI

The `release` job in `.github/workflows/ci.yml` decodes the keystore from secrets
into `$RUNNER_TEMP` (never the workspace, so it cannot be swept into an artifact),
builds, verifies the signature, and shreds the key material in an `always()` step.
It runs only on `main` and never on pull requests, because forks cannot read the
secrets.

Required secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`.

## R8

Release builds run R8 with resource shrinking: **13.7 MB → 1.9 MB**, an 86%
reduction. That is a large enough change to be dangerous, because R8 removes code
it cannot see being used, and reflection is invisible to it. Two things here are
only reachable reflectively and would break in release while working perfectly in
debug:

- **Navigation 3 route keys** (`Main`, `Settings`, `ThemePicker`) are
  `@Serializable` objects whose generated serializers are resolved at runtime.
  Stripped, the app crashes on the first navigation.
- **`HabitLockAccessibilityService`** is instantiated by the system from the name in
  the manifest. Stripped, blocking silently stops working — the worst failure mode
  this app has, because the toggle still reads "on".

Both have keep rules in `app/proguard-rules.pro`. **Any release build must be
installed and navigated before shipping**, not just compiled — this is verified on
an emulator, not assumed.
