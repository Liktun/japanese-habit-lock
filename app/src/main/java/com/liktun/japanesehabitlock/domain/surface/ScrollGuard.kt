package com.liktun.japanesehabitlock.domain.surface

/**
 * Detects sustained scrolling, regardless of which named surface it happens on.
 *
 * ## Why this exists on top of [SurfaceMonitor]
 *
 * Naming individual surfaces is a losing game. A user blocked Instagram Reels and kept
 * scrolling — Instagram's Home feed interleaves the same suggested/algorithmic content
 * Reels does, so the craving just moved one tab over. Add "Instagram Feed" as a named
 * surface and the same thing happens with Explore next, or with whatever Instagram
 * renames a view id to after the next redesign. Chasing named screens one at a time
 * never converges.
 *
 * `TYPE_VIEW_SCROLLED` fires on every scroll in every one of these surfaces, because
 * they are all, mechanically, a RecyclerView being flung. Tracking that ONE event
 * generalises to every infinite-scroll surface an app has today and to ones it has not
 * built yet — no view id, no per-surface maintenance.
 *
 * ## Design
 *
 * Deliberately scoped to *sustained* scrolling, not any scrolling: someone jumping to
 * the bottom of a long DM thread scrolls too, and punishing that would make the tool
 * feel adversarial rather than helpful. A short pause is treated as the user
 * self-interrupting — exactly the behaviour a habit tool should reward, not reset past.
 * Only a genuine gap (no scroll event for [idleResetMillis]) drops the accumulated time
 * back to zero; anything shorter keeps building toward [dwellThresholdMillis].
 *
 * Pure Kotlin, zero Android imports, so every rule here is plain JUnit; the service
 * supplies only a package name and a timestamp per scroll event.
 */
class ScrollGuard(
  private val dwellThresholdMillis: Long = DEFAULT_DWELL_THRESHOLD_MILLIS,
  private val idleResetMillis: Long = DEFAULT_IDLE_RESET_MILLIS,
) {

  /** Time accumulated scrolling in [trackedPackage] since the last real pause. */
  private var accumulatedMillis: Long = 0L
  private var lastEventMillis: Long? = null
  private var trackedPackage: String? = null

  /**
   * Call on every scroll event.
   *
   * The first event for a newly-foregrounded package only starts the clock; a gap
   * cannot be measured from a single sample.
   */
  fun onScroll(packageName: String, nowMillis: Long) {
    if (packageName != trackedPackage) {
      trackedPackage = packageName
      accumulatedMillis = 0L
      lastEventMillis = nowMillis
      return
    }
    val last = lastEventMillis
    if (last != null) {
      val gap = nowMillis - last
      if (gap in 0..idleResetMillis) {
        accumulatedMillis += gap
      } else if (gap > idleResetMillis) {
        // A real pause: the user stopped scrolling and did something else for a
        // while. Fresh start, not "doom-scrolling with breaks."
        accumulatedMillis = 0L
      }
      // A negative gap (clock oddity) is ignored rather than subtracted.
    }
    lastEventMillis = nowMillis
  }

  /**
   * Call whenever the foreground app changes, even without a scroll event, so dwell
   * time in one app can never leak into another via a stale [trackedPackage].
   */
  fun onForegroundChanged(packageName: String?) {
    if (packageName != trackedPackage) {
      trackedPackage = packageName
      accumulatedMillis = 0L
      lastEventMillis = null
    }
  }

  /** True once [packageName] has accumulated [dwellThresholdMillis] of near-continuous scrolling. */
  fun isSustainedScrolling(packageName: String): Boolean =
    packageName == trackedPackage && accumulatedMillis >= dwellThresholdMillis

  fun reset() {
    accumulatedMillis = 0L
    lastEventMillis = null
    trackedPackage = null
  }

  companion object {
    /**
     * Set by the user after living with 45s: short and strict on purpose. Paired
     * with a 10s reset, reading slowly with occasional swipes still accumulates,
     * so this catches browsing, not just flinging.
     */
    const val DEFAULT_DWELL_THRESHOLD_MILLIS = 15_000L

    /**
     * A pause shorter than this does not reset the clock - 10s (user-chosen) means
     * a quick pause to read cannot be used to dodge the timer. Longer than this,
     * the user genuinely stopped, and that counts as success.
     */
    const val DEFAULT_IDLE_RESET_MILLIS = 10_000L
  }
}
