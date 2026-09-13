package com.liktun.japanesehabitlock.domain.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SELF = "com.liktun.japanesehabitlock"
private const val IG = KnownSurfaces.INSTAGRAM
private val REELS = setOf("instagram_reels")

class SurfaceMonitorScrollFallbackTest {

  private fun scrollPast(guard: ScrollGuard, pkg: String, count: Int = 20, step: Long = 500L) {
    var t = 0L
    repeat(count) {
      guard.onScroll(pkg, t)
      t += step
    }
  }

  @Test
  fun `sustained scrolling in feed blocks even though no named surface matches`() {
    // THE ACTUAL BUG REPORT: Reels blocked, user moved to the plain Home feed instead,
    // whose view ids do not match any tracked surface. ScrollGuard is the fallback.
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG, count = 20, step = 500L) // 10s of continuous scrolling

    val v = monitor.verdict(
      foregroundPackage = IG,
      visibleViewIds = setOf("$IG:id/some_unrelated_view"), // no named surface matches
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS, // opted in via Reels
      isUnlocked = false,
    )
    assertTrue(v is SurfaceVerdict.BlockSustainedScrolling)
    assertEquals(IG, (v as SurfaceVerdict.BlockSustainedScrolling).packageName)
  }

  @Test
  fun `a named surface match still wins over the scroll fallback`() {
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG)

    val v = monitor.verdict(
      foregroundPackage = IG,
      visibleViewIds = setOf("$IG:id/clips_viewer"),
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS,
      isUnlocked = false,
    )
    assertTrue(v is SurfaceVerdict.BlockSurface)
  }

  @Test
  fun `the fallback never fires in an app the user never opted into`() {
    // The safety property: this must never surprise someone who did not ask this app
    // to be restricted at all.
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    val otherApp = "com.reddit.frontpage"
    scrollPast(guard, otherApp)

    val v = monitor.verdict(
      foregroundPackage = otherApp,
      visibleViewIds = emptySet(),
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS, // Instagram Reels ticked, nothing for Reddit
      isUnlocked = false,
    )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `no fallback without a ScrollGuard attached`() {
    // Existing behaviour for every caller that does not pass one - the parameter is
    // additive, never a silent behaviour change for existing tests or code paths.
    val monitor = SurfaceMonitor(SELF) // no scrollGuard
    val v = monitor.verdict(
      foregroundPackage = IG,
      visibleViewIds = setOf("$IG:id/unrelated"),
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS,
      isUnlocked = false,
    )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `a direct message context still exempts even during sustained scrolling`() {
    // The DM exemption must not be undermined by adding a second detection path.
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG)

    val v = monitor.verdict(
      foregroundPackage = IG,
      visibleViewIds = setOf("$IG:id/direct_thread"),
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS,
      isUnlocked = false,
    )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `unlocking still allows everything regardless of scroll time`() {
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG)

    val v = monitor.verdict(
      foregroundPackage = IG,
      visibleViewIds = emptySet(),
      blockedPackages = emptySet(),
      blockedSurfaceIds = REELS,
      isUnlocked = true,
    )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `shouldLaunchBlocker debounces the scroll verdict like any other`() {
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG)
    val ids = setOf("$IG:id/unrelated")

    val first = monitor.shouldLaunchBlocker(IG, ids, emptySet(), REELS, false)
    assertTrue(first is SurfaceVerdict.BlockSustainedScrolling)

    val second = monitor.shouldLaunchBlocker(IG, ids, emptySet(), REELS, false)
    assertEquals(SurfaceVerdict.Allow, second)
  }

  @Test
  fun `reset on the monitor also resets the attached ScrollGuard`() {
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val monitor = SurfaceMonitor(SELF, scrollGuard = guard)
    scrollPast(guard, IG)
    assertTrue(guard.isSustainedScrolling(IG))

    monitor.reset()
    assertEquals(false, guard.isSustainedScrolling(IG))
  }
}
