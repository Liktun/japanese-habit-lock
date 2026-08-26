package com.liktun.japanesehabitlock.service

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * The thin Android adapter around [OemBatteryGuidance].
 *
 * Everything decidable lives in the pure-Kotlin table next door; this object only reads
 * [Build.MANUFACTURER] and tries to open screens. It follows the same split as
 * [AccessibilityServiceStatus]: detect and deep-link, nothing more.
 */
object ServiceHealthLauncher {

  /**
   * The device brand, never null.
   *
   * Passed into [OemBatteryGuidance.forManufacturer] by the caller rather than read
   * inside it, so the guidance table stays JVM-testable.
   */
  fun currentManufacturer(): String = Build.MANUFACTURER.orEmpty()

  /**
   * Sends the user to the most specific battery screen we can actually open.
   *
   * Tries three things in order and returns true as soon as one starts:
   *  1. the OEM component from [guidance], if any,
   *  2. the standard [Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS] list,
   *  3. our own app-details page, which exists on every Android build.
   *
   * EVERY step is wrapped in try/catch, and that is not defensive paranoia — OEM
   * component names are undocumented internals that get renamed, moved between packages,
   * or locked behind a signature permission from one firmware revision to the next. An
   * uncaught [ActivityNotFoundException] or [SecurityException] here would crash the app
   * on precisely the Xiaomi and Samsung devices this whole feature exists to rescue,
   * turning a helpful nudge into the bug report. Falling through quietly to a screen
   * that definitely exists is always better than being right about the fancy one.
   *
   * Note we open the battery-optimization LIST, never
   * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: that direct request needs the
   * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission, which Google Play restricts to a
   * short allowlist of use cases that a habit blocker is not on. The list screen is
   * always permitted and costs the user one extra tap.
   */
  fun openGuidanceSettings(context: Context, guidance: OemGuidance): Boolean {
    val component = guidance.settingsComponent?.let(ComponentName::unflattenFromString)
    if (component != null) {
      val oemIntent = Intent()
        .setComponent(component)
        // Required whenever an Activity is started from a non-Activity context.
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (start(context, oemIntent)) return true
    }

    val action = guidance.settingsIntentAction ?: OemBatteryGuidance.GENERIC_ACTION
    val generic = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (start(context, generic)) return true

    // Last resort: our own app info page. Reachable on every build, and from there the
    // user can walk into the Battery sub-screen the steps describe.
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", context.packageName, null))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return start(context, details)
  }

  /**
   * Whether the OS has already exempted us from Doze/App Standby.
   *
   * This is a pure READ and is always allowed — only the *request* action is gated by the
   * Play-restricted permission, so checking here carries no policy risk and lets the UI
   * skip nagging users who have already granted the exemption.
   */
  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return try {
      power.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: SecurityException) {
      // Some hardened ROMs refuse the query outright. Assume not exempt and keep guiding.
      false
    }
  }

  /** Starts [intent], swallowing the two failures OEM screens realistically produce. */
  private fun start(context: Context, intent: Intent): Boolean =
    try {
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      // Screen does not exist on this firmware version.
      false
    } catch (_: SecurityException) {
      // Screen exists but is not exported to third-party apps on this build.
      false
    }
}
