package com.liktun.japanesehabitlock.service

/**
 * Decides whether the app currently in the foreground should be interrupted.
 *
 * This class is deliberately free of Android types. The blocking decision is the
 * riskiest logic in the app — a wrong `true` can trap the user in a loop they cannot
 * escape from the device UI — so it lives here where plain JUnit can exercise every
 * branch without an emulator. The Accessibility Service stays a thin adapter that
 * only forwards a package name and the two pieces of state it observes.
 *
 * The self package is injected rather than read from a `Context` for the same reason:
 * the class must be constructible in a unit test.
 */
class ForegroundAppMonitor(
  private val selfPackage: String,
  /**
   * Extra packages that can never be blocked, on top of [ALWAYS_ALLOWED].
   *
   * Injected rather than imported so this class keeps zero dependencies — the caller
   * passes `Roadmap.STUDY_TOOL_PACKAGES`, and tests can pass whatever they like.
   */
  private val neverBlockable: Set<String> = emptySet(),
) {

  /**
   * The last package we told the caller to block.
   *
   * Held here rather than in the service so the debounce rule is covered by the same
   * unit tests as the decision itself.
   */
  private var lastBlockedPackage: String? = null

  /**
   * Whether [foregroundPackage] should be blocked right now.
   *
   * Pure and side-effect free: callers can ask as often as they like. Use
   * [shouldLaunchBlocker] when the answer drives an Activity launch.
   */
  fun shouldBlock(
    foregroundPackage: String?,
    blockedPackages: Set<String>,
    isUnlocked: Boolean,
  ): Boolean {
    // The gate is open: the user did the work today, so nothing is off limits.
    if (isUnlocked) return false
    if (foregroundPackage.isNullOrBlank()) return false
    // Never block ourselves. The blocker screen is our own Activity, so blocking our
    // package would make the blocker re-trigger on itself forever with no way out.
    if (foregroundPackage == selfPackage) return false
    if (foregroundPackage in ALWAYS_ALLOWED) return false
    // Blocking a study tool would deadlock the gate: it only opens once the reviews
    // are done, and the reviews are done inside these apps. Refused here as well as
    // hidden in the picker, so a hand-edited list cannot create that state either.
    if (foregroundPackage in neverBlockable) return false
    return foregroundPackage in blockedPackages
  }

  /**
   * Returns true only on a TRANSITION into a blocked package, so the blocker is not
   * relaunched on every window event.
   *
   * Android emits `TYPE_WINDOW_STATE_CHANGED` many times for a single app — dialogs,
   * keyboards and internal screen changes all fire it. Launching the blocker on each
   * one would restart the Activity repeatedly, flickering the screen and stealing
   * focus from the blocker itself. Instead we remember the package we last blocked and
   * stay silent until the foreground moves somewhere that is not blocked, at which
   * point the memory is cleared so returning to the same app blocks again.
   *
   * This is stateful by design, so it is not safe to share one instance across threads
   * without external synchronisation; the service calls it only from accessibility
   * event callbacks, which are delivered on a single thread.
   */
  fun shouldLaunchBlocker(
    foregroundPackage: String?,
    blockedPackages: Set<String>,
    isUnlocked: Boolean,
  ): Boolean {
    if (!shouldBlock(foregroundPackage, blockedPackages, isUnlocked)) {
      // Only a genuine move away from the blocked app clears the latch. Transient
      // null/blank events carry no package, so they must not count as a departure.
      if (!foregroundPackage.isNullOrBlank()) lastBlockedPackage = null
      return false
    }
    if (foregroundPackage == lastBlockedPackage) return false
    lastBlockedPackage = foregroundPackage
    return true
  }

  companion object {
    /**
     * Packages that are never blocked, whatever the user configures.
     *
     * The user must ALWAYS be able to reach Settings to turn this service off, get back
     * to their home screen, and place an emergency call. Blocking any of those would be
     * user-hostile, and in the case of the dialer and telecom stack it could be
     * genuinely dangerous. Treating this as a hardcoded floor — rather than something
     * the settings UI merely filters out — means a corrupt or hand-edited blocked list
     * can never lock someone out of their own phone.
     */
    val ALWAYS_ALLOWED: Set<String> = setOf(
      // Escape hatch: disabling the accessibility service lives here.
      "com.android.settings",
      // Status bar, notification shade, recents, power menu.
      "com.android.systemui",
      // Home screens, so Back/Home always works.
      "com.android.launcher",
      "com.google.android.apps.nexuslauncher",
      // Phone app: emergency calls must never be interrupted.
      "com.android.dialer",
      "com.google.android.dialer",
      // The in-call/telecom service behind emergency dialling.
      "com.android.server.telecom",
    )
  }
}
