# Japanese Habit Lock

Android app for tracking a daily Japanese study habit against a structured roadmap.

[![CI](https://github.com/Liktun/japanese-habit-lock/actions/workflows/ci.yml/badge.svg)](https://github.com/Liktun/japanese-habit-lock/actions/workflows/ci.yml)

## Stack

- Kotlin, Jetpack Compose, Material 3
- Navigation 3
- DataStore (Preferences) for persistence
- minSdk 26 / targetSdk 36, JDK 17

## Structure

```
app/src/main/java/com/liktun/japanesehabitlock/
├── domain/          DailyChecklist, Roadmap, StudyDay
├── data/            ChecklistRepository, HabitLockDataStore
├── ui/checklist/    ChecklistScreen, ChecklistViewModel
├── theme/           Compose theme
└── MainActivity.kt, Navigation.kt, NavigationKeys.kt
```

## Build

Requires JDK 17 and the Android SDK (`ANDROID_HOME` set).

```bash
./gradlew test           # unit tests
./gradlew assembleDebug  # debug APK -> app/build/outputs/apk/debug/
```

CI runs `test assembleDebug` on every push and PR to `main`, and uploads the
test reports and debug APK as artifacts.

## Notes

`gradle.properties` forces IPv4 for dependency resolution — a workaround for a
broken IPv6 route to `dl.google.com` on the build host. Harmless elsewhere.
