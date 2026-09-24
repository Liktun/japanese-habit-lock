package com.liktun.japanesehabitlock.domain.surface

/**
 * What the service should do about the screen currently in front of the user.
 */
sealed interface SurfaceVerdict {
  /** Nothing to do. */
  data object Allow : SurfaceVerdict

  /** The whole app is blocked, the old package-level behaviour. */
  data object BlockApp : SurfaceVerdict

  /** Only this surface is blocked; the rest of the app stays usable. */
  data class BlockSurface(val surface: BlockableSurface) : SurfaceVerdict

  /**
   * Caught by [ScrollGuard] rather than a named surface: the user has been scrolling
   * continuously in [packageName] for longer than the tool considers a normal check-in,
   * even though no specific tracked surface matched.
   *
   * This exists because named surfaces are a losing game — see [ScrollGuard]'s KDoc.
   * Only fires in an app where the user has already opted into surface blocking (i.e.
   * they ticked at least one surface belonging to this package), so it never surprises
   * someone who never asked this app to be restricted at all.
   */
  data class BlockSustainedScrolling(val packageName: String) : SurfaceVerdict
}

/**
 * Decides whether a screen should be blocked, given what is on it.
 *
 * Pure Kotlin with no Android imports so every rule below is covered by plain JUnit —
 * the same reason [ForegroundAppMonitor] is shaped this way. The service is a thin
 * adapter that feeds it view ids and acts on the verdict.
 *
 * ## Why this is not just "block the package"
 *
 * Blocking `com.instagram.android` blocks the DMs people rely on, so they turn the
 * blocker off and lose everything. Surface blocking keeps the app usable and removes
 * only the infinite feed, which is the part that actually costs an hour.
 *
 * ## The shared-reel rule
 *
 * A reel a friend sends is not the Reels tab. It opens from a conversation, and the
 * conversation's own views are still in the tree behind the player. So when a
 * DM-context view id is present, the verdict is [SurfaceVerdict.Allow] even though a
 * reel player is on screen. Without this rule the feature would be useless: it would
 * block exactly the reels the user explicitly asked to keep.
 */
class SurfaceMonitor(
  private val selfPackage: String,
  private val neverBlockable: Set<String> = emptySet(),
  /**
   * Detects sustained scrolling as a fallback when no named surface matches. Optional
   * because ScrollGuard needs live timestamps the pure [verdict] call does not have;
   * the service feeds it separately via [onScrollEvent]. Null disables the fallback
   * entirely, which is what every existing test that doesn't care about it uses.
   */
  private val scrollGuard: ScrollGuard? = null,
) {

  private var lastBlockedKey: String? = null

  /**
   * Feeds a scroll event to the underlying [ScrollGuard]. Call this from
   * `TYPE_VIEW_SCROLLED`; [verdict] and [shouldLaunchBlocker] read the accumulated
   * state but never advance the clock themselves, since they may be called from a
   * different event type (or not at all, for a static screen).
   */
  /**
   * Call when a different app comes to the front. Leaving an app (to the launcher,
   * or to the blocker itself) must end its scroll session; otherwise dwell time from
   * an earlier session survives, and the next time the app opens it is blocked
   * before the user has scrolled at all.
   */
  fun onForegroundChanged(packageName: String?) {
    scrollGuard?.onForegroundChanged(packageName)
  }

  fun onScrollEvent(packageName: String, nowMillis: Long) {
    scrollGuard?.onScroll(packageName, nowMillis)
  }

  /**
   * @param foregroundPackage the app in front, or null if unknown.
   * @param visibleViewIds every view id currently in the accessibility tree. Ids are
   *   matched as substrings, so callers may pass either the bare name or the full
   *   `pkg:id/name` form.
   * @param blockedPackages packages blocked outright.
   * @param blockedSurfaceIds surfaces blocked individually, by [BlockableSurface.id].
   * @param isUnlocked whether today's required tasks are done.
   */
  fun verdict(
    foregroundPackage: String?,
    visibleViewIds: Set<String>,
    blockedPackages: Set<String>,
    blockedSurfaceIds: Set<String>,
    isUnlocked: Boolean,
  ): SurfaceVerdict {
    if (isUnlocked) return SurfaceVerdict.Allow
    if (foregroundPackage.isNullOrBlank()) return SurfaceVerdict.Allow
    if (foregroundPackage == selfPackage) return SurfaceVerdict.Allow
    if (foregroundPackage in ForegroundAppMonitorFloor.ALWAYS_ALLOWED) return SurfaceVerdict.Allow
    if (foregroundPackage in neverBlockable) return SurfaceVerdict.Allow

    // Whole-app blocking wins: it is the stricter choice and the user asked for it
    // explicitly, so a surface rule must not quietly weaken it.
    if (foregroundPackage in blockedPackages) return SurfaceVerdict.BlockApp

    // A reel opened from a conversation is content a person deliberately sent, not an
    // algorithmic feed. Checked BEFORE surface matching because the reel player's own
    // views are present either way - only the surrounding context tells them apart.
    if (isDirectMessageContext(visibleViewIds)) return SurfaceVerdict.Allow

    val hit =
      KnownSurfaces.forPackage(foregroundPackage).firstOrNull { surface ->
        surface.id in blockedSurfaceIds && surface.matches(visibleViewIds)
      }
    if (hit != null) {
      if (hit.blockOnSight) return SurfaceVerdict.BlockSurface(hit)
      // A landing surface (Instagram's feed): seeing it is normal and expected, only
      // sustained scrolling on it counts.
      if (scrollGuard?.isSustainedScrolling(foregroundPackage) == true) {
        return SurfaceVerdict.BlockSurface(hit)
      }
      return SurfaceVerdict.Allow
    }

    // Fallback: no named surface matched, but the user has opted into blocking SOME
    // surface in this app and has been scrolling continuously well past a normal
    // check-in. Gated on opt-in so this can never surprise someone who never asked
    // this app to be restricted - it only tightens a rule already chosen, never adds
    // a new one silently.
    val hasOptedIntoThisApp =
      KnownSurfaces.forPackage(foregroundPackage).any { it.id in blockedSurfaceIds }
    if (hasOptedIntoThisApp && scrollGuard?.isSustainedScrolling(foregroundPackage) == true) {
      return SurfaceVerdict.BlockSustainedScrolling(foregroundPackage)
    }

    return SurfaceVerdict.Allow
  }

  /**
   * Transition-only variant, so the blocker is not relaunched on every window event
   * while the user sits on the same screen.
   *
   * Keyed by package *and* surface so moving from Reels to Explore still fires.
   */
  fun shouldLaunchBlocker(
    foregroundPackage: String?,
    visibleViewIds: Set<String>,
    blockedPackages: Set<String>,
    blockedSurfaceIds: Set<String>,
    isUnlocked: Boolean,
  ): SurfaceVerdict {
    val verdict =
      verdict(foregroundPackage, visibleViewIds, blockedPackages, blockedSurfaceIds, isUnlocked)
    val key =
      when (verdict) {
        is SurfaceVerdict.Allow -> null
        is SurfaceVerdict.BlockApp -> "app:$foregroundPackage"
        is SurfaceVerdict.BlockSurface -> "surface:${verdict.surface.id}"
        is SurfaceVerdict.BlockSustainedScrolling -> "scroll:${verdict.packageName}"
      }
    if (key == null) {
      lastBlockedKey = null
      return SurfaceVerdict.Allow
    }
    if (key == lastBlockedKey) return SurfaceVerdict.Allow
    lastBlockedKey = key
    return verdict
  }

  /** Forgets the debounce state, e.g. when the gate opens. */
  fun reset() {
    lastBlockedKey = null
    scrollGuard?.reset()
  }

  companion object {

    /**
     * View ids that mean the user is inside a conversation.
     *
     * Deliberately broad. A false positive here means one algorithmic reel slips
     * through; a false negative means blocking a reel a friend sent, which is the one
     * behaviour the user explicitly asked to keep. The asymmetry is intentional.
     */
    val DIRECT_MESSAGE_CONTEXT: Set<String> =
      setOf(
        // Being *inside* a conversation. Deliberately NOT a bare "direct_" prefix:
        // Instagram's bottom nav carries a direct-messages button on every screen,
        // including the Reels tab, so a loose marker matched everywhere and silently
        // disabled blocking entirely. That was the first real-device failure.
        "direct_thread",
        "thread_message",
        "message_composer",
        "row_thread",
        "direct_reply",
        "message_list",
        "thread_composer",
        "direct_fragment_container",
      )

    /**
     * Nav chrome that merely *links* to messages and must never count as being in a
     * conversation. Checked first, so these can never trigger the exemption.
     */
    private val DM_NAV_CHROME: Set<String> =
      setOf(
        "direct_tab",
        "action_bar_inbox",
        "tab_icon",
        "direct_button",
        "inbox_button",
      )

    fun isDirectMessageContext(visibleViewIds: Set<String>): Boolean =
      visibleViewIds.any { id ->
        val short = id.substringAfter(":id/")
        // Nav chrome first: a button that opens messages is not a conversation.
        if (DM_NAV_CHROME.any { short.equals(it, ignoreCase = true) }) return@any false
        DIRECT_MESSAGE_CONTEXT.any { short.contains(it, ignoreCase = true) }
      }
  }
}

/** True when any visible view id names this surface. */
fun BlockableSurface.matches(visibleViewIds: Set<String>): Boolean =
  visibleViewIds.any { id -> viewIdContains.any { id.contains(it, ignoreCase = true) } }

/**
 * The hardcoded always-allowed floor, shared with [ForegroundAppMonitor].
 *
 * Duplicated as a tiny object rather than imported so both monitors keep zero
 * dependencies on each other while enforcing the identical floor.
 */
internal object ForegroundAppMonitorFloor {
  val ALWAYS_ALLOWED: Set<String> =
    setOf(
      "com.android.settings",
      "com.android.systemui",
      "com.android.launcher",
      "com.google.android.apps.nexuslauncher",
      "com.android.dialer",
      "com.google.android.dialer",
      "com.android.server.telecom",
    )
}
