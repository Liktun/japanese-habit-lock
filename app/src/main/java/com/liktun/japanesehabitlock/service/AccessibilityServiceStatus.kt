package com.liktun.japanesehabitlock.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Whether the user has switched our accessibility service on, and how to send them there.
 *
 * An accessibility service can never be enabled programmatically — that restriction is the
 * whole reason the API is safe to hand to third-party apps. So the app can only detect the
 * current state and deep-link the user to the system screen where they flip the switch.
 *
 * State is read from [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES], a colon-separated
 * list of `package/class` entries.
 */
object AccessibilityServiceStatus {

  /** True when [HabitLockAccessibilityService] appears in the system's enabled-services list. */
  fun isEnabled(context: Context): Boolean {
    val expected = ComponentName(context, HabitLockAccessibilityService::class.java)
    val enabled =
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false

    // The list uses ':' separators. Compare parsed ComponentNames rather than raw strings so
    // short form ("pkg/.Class") and long form ("pkg/pkg.Class") both match.
    return enabled
      .split(':')
      .mapNotNull { ComponentName.unflattenFromString(it.trim()) }
      .any { it == expected }
  }

  /**
   * Opens the system accessibility settings screen.
   *
   * Deliberately opens the *list* screen rather than trying to deep-link straight to our
   * own entry: the per-service deep link is undocumented and OEM-specific, so it silently
   * fails on a meaningful fraction of devices. The list always works.
   */
  fun openSettings(context: Context) {
    context.startActivity(
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    )
  }
}
