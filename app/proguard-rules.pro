# R8 / ProGuard rules for the release build.
#
# Only what is genuinely needed. Every over-broad `-keep` silently gives back the size
# win that turning R8 on was for, so each rule below states why it exists.

# ── kotlinx.serialization ───────────────────────────────────────────────────────
# Navigation 3 route keys (Main, Settings, ThemePicker) are @Serializable objects.
# R8 cannot see that their generated serializers are used, strips them, and the app
# then crashes on the FIRST navigation — which a debug build never reveals.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.liktun.japanesehabitlock.**$$serializer { *; }
-keepclassmembers class com.liktun.japanesehabitlock.** {
    *** Companion;
}
-keepclasseswithmembers class com.liktun.japanesehabitlock.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The nav keys themselves are objects referenced only through serialization.
-keep @kotlinx.serialization.Serializable class com.liktun.japanesehabitlock.** { *; }

# ── AccessibilityService ────────────────────────────────────────────────────────
# Instantiated by the system from the name in AndroidManifest.xml, never from our
# code, so R8 sees no reference to it and would remove it — silently disabling the
# entire blocking feature in release while debug works fine.
-keep class com.liktun.japanesehabitlock.service.HabitLockAccessibilityService { *; }

# ── DataStore ───────────────────────────────────────────────────────────────────
# Preferences DataStore uses protobuf-lite internals reflectively.
-keep class androidx.datastore.*.** { *; }

# ── Compose ─────────────────────────────────────────────────────────────────────
# The Compose compiler and runtime handle their own keep rules via consumer files;
# nothing extra is needed here. Left as a note so nobody adds a blanket -keep for
# androidx.compose.** "just in case" and undoes the shrink.
