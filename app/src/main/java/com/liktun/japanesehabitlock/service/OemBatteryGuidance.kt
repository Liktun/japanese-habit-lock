package com.liktun.japanesehabitlock.service

/**
 * What to tell the user, and where to send them, so their manufacturer stops killing us.
 *
 * @param manufacturerLabel human-readable brand name for the UI heading.
 * @param steps ordered, literal instructions. These are read aloud by the user while they
 *   walk through a Settings app we cannot see, so they must name real screens and real
 *   buttons — "check your battery settings" is useless when MIUI hides Autostart three
 *   levels deep in a separate Security app.
 * @param settingsIntentAction intent action to try, or null when only a component is known.
 * @param settingsComponent flattened `package/class` of the OEM screen, or null when the
 *   brand has no special screen and the standard battery-optimization list is enough.
 */
data class OemGuidance(
  val manufacturerLabel: String,
  val steps: List<String>,
  val settingsIntentAction: String?,
  val settingsComponent: String?,
)

/**
 * Per-manufacturer instructions for surviving aggressive background-process reaping.
 *
 * Zero Android imports on purpose: the manufacturer arrives as a plain String so every
 * branch is unit-testable on the JVM. Reading `Build.MANUFACTURER` in here would make the
 * whole table untestable without an emulator, and this table is exactly the sort of thing
 * that rots silently — a typo in a component name only shows up on one brand of device.
 * [ServiceHealthLauncher] is the thin adapter that supplies the real value.
 *
 * The component names below are undocumented OEM internals. They are correct on the
 * firmware versions they were taken from and WILL be wrong on others, which is why every
 * caller must treat them as a best-effort hint and fall back gracefully.
 */
object OemBatteryGuidance {

  /** Standard AOSP battery-optimization list. Always present, never restricted by Play. */
  const val GENERIC_ACTION: String = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"

  private const val APP_NAME = "Japanese Habit Lock"

  /**
   * Guidance for [manufacturer], falling back to generic advice for unknown brands.
   *
   * Matching is case-insensitive and trimmed because `Build.MANUFACTURER` is not
   * normalised across vendors: the same company ships "Xiaomi", "xiaomi" and "XIAOMI"
   * on different ROMs, and a case-sensitive lookup would quietly hand a MIUI user the
   * generic advice that does not mention Autostart at all.
   */
  fun forManufacturer(manufacturer: String): OemGuidance =
    when (manufacturer.trim().lowercase()) {
      "samsung" -> SAMSUNG
      // Redmi and POCO are Xiaomi sub-brands running the same MIUI/HyperOS settings app,
      // so they share the Security-centre Autostart screen verbatim.
      "xiaomi", "redmi", "poco" -> XIAOMI
      "huawei" -> HUAWEI
      // Honor was spun out of Huawei and kept the Phone Manager launch-management screen.
      "honor" -> HONOR
      "oppo" -> OPPO
      "realme" -> REALME
      "vivo" -> VIVO
      "oneplus" -> ONEPLUS
      else -> generic(manufacturer)
    }

  private val SAMSUNG = OemGuidance(
    manufacturerLabel = "Samsung",
    steps = listOf(
      "Open Settings → Apps → $APP_NAME → Battery.",
      "Set \"Allow background usage\" to on and choose \"Unrestricted\".",
      "Go back to Settings → Battery and device care → Battery → Background usage limits.",
      "Open \"Never sleeping apps\" and tap + to add $APP_NAME to that list.",
      "In the same \"Background usage limits\" screen, check \"Sleeping apps\" and " +
        "\"Deep sleeping apps\" and remove $APP_NAME if it appears there.",
      "Turn off \"Put unused apps to sleep\" so One UI cannot re-add us after a few days.",
      "Open Settings → Accessibility → Installed apps and confirm $APP_NAME is still on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
  )

  private val XIAOMI = OemGuidance(
    manufacturerLabel = "Xiaomi / Redmi / POCO (MIUI or HyperOS)",
    steps = listOf(
      "Open Security → Permissions → Autostart and switch $APP_NAME on. " +
        "Without Autostart, MIUI will not restart the service after a reboot or a kill.",
      "Open Settings → Apps → Manage apps → $APP_NAME → Battery saver and choose " +
        "\"No restrictions\".",
      "Lock the app in Recents: open Recents, swipe down on the $APP_NAME card (or " +
        "long-press it) and tap the padlock. MIUI kills unlocked cards when you clear " +
        "Recents, even with Autostart enabled — both settings are required.",
      "In Settings → Battery & performance, turn off \"Battery saver\" or add $APP_NAME " +
        "as an exception.",
      "Open Settings → Accessibility → Downloaded apps and confirm $APP_NAME is still on.",
    ),
    settingsIntentAction = null,
    settingsComponent =
      "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
  )

  private val HUAWEI = OemGuidance(
    manufacturerLabel = "Huawei (EMUI / HarmonyOS)",
    steps = listOf(
      "Open Phone Manager (or Optimizer) → Battery → App launch.",
      "Find $APP_NAME, turn OFF \"Manage automatically\", then enable all three switches: " +
        "\"Auto-launch\", \"Secondary launch\" and \"Run in background\".",
      "Open Settings → Battery → More battery settings and turn off \"Sleep mode\" / " +
        "\"Close apps after screen lock\".",
      "Lock the app in Recents: open Recents and tap the padlock on the $APP_NAME card.",
      "Open Settings → Accessibility features → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
  )

  private val HONOR = OemGuidance(
    manufacturerLabel = "Honor (MagicOS)",
    steps = listOf(
      "Open Phone Manager → App launch (or Settings → Apps → App launch).",
      "Find $APP_NAME, turn OFF \"Manage automatically\", then enable \"Auto-launch\", " +
        "\"Secondary launch\" and \"Run in background\".",
      "Open Settings → Battery → Battery usage → $APP_NAME and allow background activity.",
      "Lock the app in Recents so clearing Recents does not stop the service.",
      "Open Settings → Accessibility features → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
  )

  private val OPPO = OemGuidance(
    manufacturerLabel = "OPPO (ColorOS)",
    steps = listOf(
      "Open Settings → Battery → More settings (or Power saving) and turn ON " +
        "\"Allow auto-launch\" for $APP_NAME in the Startup manager.",
      "Open Settings → Apps → App management → $APP_NAME → Battery usage and choose " +
        "\"Allow background activity\" / \"Don't optimise\".",
      "In Settings → Battery, turn OFF \"Sleep standby optimisation\" and \"High " +
        "background power consumption\" restrictions for $APP_NAME.",
      "Lock the app in Recents: open Recents, drag the $APP_NAME card down and tap the " +
        "padlock.",
      "Open Settings → Additional settings → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.coloros.safecenter/.startupapp.StartupAppListActivity",
  )

  private val REALME = OemGuidance(
    manufacturerLabel = "realme (realme UI)",
    steps = listOf(
      "Open Settings → Apps → App management → $APP_NAME → Allow auto-launch and turn it on.",
      "In the same screen open Battery usage and choose \"Allow background activity\" and " +
        "\"Don't optimise\".",
      "Open Settings → Battery → turn OFF \"Sleep standby optimisation\".",
      "Lock the app in Recents so clearing Recents does not stop the service.",
      "Open Settings → Additional settings → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.coloros.safecenter/.startupapp.StartupAppListActivity",
  )

  private val VIVO = OemGuidance(
    manufacturerLabel = "vivo (Funtouch OS / OriginOS)",
    steps = listOf(
      "Open iManager (Phone Manager) → App manager → Autostart manager and switch " +
        "$APP_NAME on.",
      "Open Settings → Battery → High background power consumption and allow $APP_NAME.",
      "Open Settings → Battery → Background power consumption management → $APP_NAME → " +
        "\"Allow high background power consumption\".",
      "Lock the app in Recents so clearing Recents does not stop the service.",
      "Open Settings → Shortcuts & accessibility → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
  )

  private val ONEPLUS = OemGuidance(
    manufacturerLabel = "OnePlus (OxygenOS)",
    steps = listOf(
      "Open Settings → Battery → Battery optimisation, switch the filter to \"All apps\", " +
        "find $APP_NAME and choose \"Don't optimise\".",
      "Open Settings → Apps → $APP_NAME → Battery and select \"Unrestricted\" / " +
        "\"Allow background activity\".",
      "Open Settings → Battery → More settings and turn OFF \"Advanced optimisation\" / " +
        "\"Deep optimisation\" and \"Sleep standby optimisation\".",
      "Lock the app in Recents so clearing Recents does not stop the service.",
      "Open Settings → Additional settings → Accessibility and confirm $APP_NAME is on.",
    ),
    settingsIntentAction = null,
    settingsComponent = "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity",
  )

  /**
   * Advice for brands with no known special screen — including Google Pixel, which
   * genuinely needs nothing beyond the AOSP battery-optimization exemption.
   *
   * The unknown manufacturer is still echoed back in the label so the UI can say
   * something honest ("On your device…") instead of pretending to recognise it, and so a
   * support screenshot tells us which brand we are missing a table entry for.
   */
  private fun generic(manufacturer: String): OemGuidance {
    val label = manufacturer.trim().ifBlank { "Your device" }
    return OemGuidance(
      manufacturerLabel = label,
      steps = listOf(
        "Open Settings → Apps → $APP_NAME → Battery and choose \"Unrestricted\".",
        "On the battery-optimisation list that opens, find $APP_NAME and select " +
          "\"Don't optimise\" (or \"Allow\").",
        "Open Settings → Accessibility → $APP_NAME and confirm the service is still on.",
        "If blocking stops after your phone has been idle, search your Settings for " +
          "\"auto-start\", \"autostart\" or \"app launch\" — some manufacturers hide an " +
          "extra permission there that is required for background services to survive.",
      ),
      settingsIntentAction = GENERIC_ACTION,
      settingsComponent = null,
    )
  }
}
