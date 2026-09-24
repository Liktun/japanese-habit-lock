package com.liktun.japanesehabitlock.domain.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SELF = "com.liktun.japanesehabitlock"
private const val IG = KnownSurfaces.INSTAGRAM
private const val YT = KnownSurfaces.YOUTUBE

private val REELS = setOf("instagram_reels")
private val REELS_AND_SHORTS = setOf("instagram_reels", "youtube_shorts")

/** Ids as they actually arrive from the accessibility tree, fully qualified. */
private fun ig(vararg names: String) = names.map { "$IG:id/$it" }.toSet()

private fun yt(vararg names: String) = names.map { "$YT:id/$it" }.toSet()

class SurfaceMonitorTest {

  private fun monitor() = SurfaceMonitor(SELF)

  // ── the headline behaviour ────────────────────────────────────────────────────

  @Test
  fun `the reels tab is blocked when the surface is selected`() {
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("clips_viewer", "clips_video_container"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertTrue(v is SurfaceVerdict.BlockSurface)
    assertEquals("instagram_reels", (v as SurfaceVerdict.BlockSurface).surface.id)
  }

  @Test
  fun `instagram DMs stay usable while reels is blocked`() {
    // The entire reason for surface blocking: the app keeps working.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("direct_thread_toggle", "message_composer"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `a reel a friend sent in a DM is NOT blocked`() {
    // The user asked for this explicitly. The reel player is on screen - the same
    // clips_viewer as the tab - but the conversation is in the tree behind it.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("clips_viewer", "direct_thread", "thread_message_list"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `the DM exemption does not leak into the reels tab`() {
    // No conversation context, so this is the algorithmic feed and must be blocked.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("clips_viewer"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertTrue(v is SurfaceVerdict.BlockSurface)
  }

  @Test
  fun `the home feed is not the reels tab`() {
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("feed_recycler_view", "row_feed_photo"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `opening instagram at all does not block Feed - only the feed list itself does`() {
    // THE REAL BUG: feed_tab is the Home tab BUTTON in the bottom nav bar, present on
    // every screen of the app - the profile, DMs, settings, everywhere. Shipping it in
    // viewIdContains meant Feed matched the instant Instagram opened, regardless of
    // which screen was actually showing. Same class of bug as the DM nav-button
    // exemption: a nav element mistaken for the surface it merely links to.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        // Landed on the profile tab; the Home button is still visible in the nav bar,
        // but the feed list itself is not on screen.
        visibleViewIds = ig("feed_tab", "profile_header", "tab_bar_home"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = setOf("instagram_feed"),
        isUnlocked = false,
      )
    assertEquals("the nav button alone must not trigger a Feed block", SurfaceVerdict.Allow, v)
  }

  @Test
  fun `landing on the feed does not block - Instagram opens there`() {
    // THE REPORTED BUG: Instagram opens on the Home feed, so an on-sight Feed rule
    // blocked the app the moment it opened and DMs became unreachable.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("feed_recycler_view", "feed_tab"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = setOf("instagram_feed"),
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `sustained scrolling on the feed blocks it`() {
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val m = SurfaceMonitor("com.liktun.japanesehabitlock", scrollGuard = guard)
    var t = 0L
    repeat(20) { guard.onScroll(IG, t); t += 500L }
    val v = m.verdict(IG, ig("feed_recycler_view"), emptySet(), setOf("instagram_feed"), false)
    assertTrue(v is SurfaceVerdict.BlockSurface)
    assertEquals("instagram_feed", (v as SurfaceVerdict.BlockSurface).surface.id)
  }

  @Test
  fun `leaving instagram ends the scroll session so reopening does not block`() {
    // Stale dwell: after one long session, the next open blocked before any scroll.
    val guard = ScrollGuard(dwellThresholdMillis = 5_000L)
    val m = SurfaceMonitor("com.liktun.japanesehabitlock", scrollGuard = guard)
    var t = 0L
    repeat(20) { guard.onScroll(IG, t); t += 500L }
    m.onForegroundChanged("com.liktun.japanesehabitlock") // the blocker / launcher
    m.onForegroundChanged(IG) // reopen
    val v = m.verdict(IG, ig("feed_recycler_view"), emptySet(), setOf("instagram_feed", "instagram_reels"), false)
    assertEquals(SurfaceVerdict.Allow, v)
  }

  // ── selection ─────────────────────────────────────────────────────────────────

  @Test
  fun `a surface that was not selected is not blocked`() {
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("explore_grid"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS, // explore not selected
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `explore is blocked independently of reels`() {
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("explore_grid"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = setOf("instagram_explore"),
        isUnlocked = false,
      )
    assertEquals("instagram_explore", (v as SurfaceVerdict.BlockSurface).surface.id)
  }

  @Test
  fun `youtube shorts is blocked while the rest of youtube works`() {
    val blocked =
      monitor().verdict(YT, yt("reel_recycler"), emptySet(), REELS_AND_SHORTS, false)
    assertTrue(blocked is SurfaceVerdict.BlockSurface)

    val normal =
      monitor().verdict(YT, yt("watch_player", "player_control"), emptySet(), REELS_AND_SHORTS, false)
    assertEquals(SurfaceVerdict.Allow, normal)
  }

  @Test
  fun `a surface only matches inside its own app`() {
    // Another app happening to use a view named clips_viewer must not trip Instagram's rule.
    val v =
      monitor().verdict(
        foregroundPackage = "com.someone.else",
        visibleViewIds = setOf("com.someone.else:id/clips_viewer"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  // ── interaction with whole-app blocking ───────────────────────────────────────

  @Test
  fun `blocking the whole app still wins over a surface rule`() {
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("direct_thread"),
        blockedPackages = setOf(IG),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    // Even in a DM: the user chose the stricter setting and it must not be softened.
    assertEquals(SurfaceVerdict.BlockApp, v)
  }

  // ── the floors ────────────────────────────────────────────────────────────────

  @Test
  fun `everything is allowed once the gate is open`() {
    val v = monitor().verdict(IG, ig("clips_viewer"), setOf(IG), REELS, isUnlocked = true)
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `our own app is never blocked`() {
    val v = monitor().verdict(SELF, setOf("$SELF:id/clips_viewer"), setOf(SELF), REELS, false)
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `system essentials are never blocked`() {
    listOf(
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.android.dialer",
        "com.android.server.telecom",
      )
      .forEach { pkg ->
        val v = monitor().verdict(pkg, emptySet(), setOf(pkg), REELS, false)
        assertEquals("$pkg must never be blocked", SurfaceVerdict.Allow, v)
      }
  }

  @Test
  fun `study tools are never blocked`() {
    val m = SurfaceMonitor(SELF, neverBlockable = setOf("com.smouldering_durtles.wk"))
    val v = m.verdict("com.smouldering_durtles.wk", emptySet(), emptySet(), REELS, false)
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `a null or blank package is allowed rather than crashing`() {
    assertEquals(SurfaceVerdict.Allow, monitor().verdict(null, emptySet(), emptySet(), REELS, false))
    assertEquals(SurfaceVerdict.Allow, monitor().verdict("", emptySet(), emptySet(), REELS, false))
  }

  @Test
  fun `an empty view tree cannot match a surface`() {
    // A dropped or empty tree must fail open, not block a screen we cannot identify.
    val v = monitor().verdict(IG, emptySet(), emptySet(), REELS, false)
    assertEquals(SurfaceVerdict.Allow, v)
  }

  // ── debounce ──────────────────────────────────────────────────────────────────

  @Test
  fun `the blocker fires once per arrival, not on every window event`() {
    val m = monitor()
    val first = m.shouldLaunchBlocker(IG, ig("clips_viewer"), emptySet(), REELS, false)
    assertTrue(first is SurfaceVerdict.BlockSurface)

    val second = m.shouldLaunchBlocker(IG, ig("clips_viewer"), emptySet(), REELS, false)
    assertEquals(SurfaceVerdict.Allow, second)
  }

  @Test
  fun `leaving and returning fires again`() {
    val m = monitor()
    m.shouldLaunchBlocker(IG, ig("clips_viewer"), emptySet(), REELS, false)
    m.shouldLaunchBlocker(IG, ig("direct_thread"), emptySet(), REELS, false) // left
    val again = m.shouldLaunchBlocker(IG, ig("clips_viewer"), emptySet(), REELS, false)
    assertTrue(again is SurfaceVerdict.BlockSurface)
  }

  @Test
  fun `moving from one blocked surface to another fires again`() {
    val m = monitor()
    val both = setOf("instagram_reels", "instagram_explore")
    val reels = m.shouldLaunchBlocker(IG, ig("clips_viewer"), emptySet(), both, false)
    assertTrue(reels is SurfaceVerdict.BlockSurface)
    val explore = m.shouldLaunchBlocker(IG, ig("explore_grid"), emptySet(), both, false)
    assertEquals("instagram_explore", (explore as SurfaceVerdict.BlockSurface).surface.id)
  }
}

class DirectMessageContextTest {

  @Test
  fun `every DM marker is recognised`() {
    SurfaceMonitor.DIRECT_MESSAGE_CONTEXT.forEach { marker ->
      assertTrue(marker, SurfaceMonitor.isDirectMessageContext(setOf("$IG:id/$marker")))
    }
  }

  @Test
  fun `matching ignores case and prefix`() {
    assertTrue(SurfaceMonitor.isDirectMessageContext(setOf("com.x:id/DIRECT_THREAD_view")))
    assertTrue(SurfaceMonitor.isDirectMessageContext(setOf("direct_thread")))
  }

  @Test
  fun `feed views are not mistaken for a conversation`() {
    assertFalse(SurfaceMonitor.isDirectMessageContext(ig("clips_viewer", "feed_recycler_view")))
  }
}

class RealDeviceRegressionTest {

  private fun monitor() = SurfaceMonitor(SELF)

  @Test
  fun `the DM nav button does not exempt the reels tab`() {
    // THE BUG that made this fail on a real phone. Instagram's bottom nav carries a
    // direct-messages button on every screen, Reels included. The old marker list
    // matched it, so isDirectMessageContext was true everywhere and nothing was ever
    // blocked - the feature silently did nothing while appearing to be on.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("clips_viewer", "direct_tab", "tab_icon"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertTrue("nav chrome must not exempt the Reels tab", v is SurfaceVerdict.BlockSurface)
  }

  @Test
  fun `an actual conversation still exempts a shared reel`() {
    // The other half: narrowing the markers must not break the behaviour the user
    // explicitly asked to keep.
    val v =
      monitor().verdict(
        foregroundPackage = IG,
        visibleViewIds = ig("clips_viewer", "direct_thread", "message_composer"),
        blockedPackages = emptySet(),
        blockedSurfaceIds = REELS,
        isUnlocked = false,
      )
    assertEquals(SurfaceVerdict.Allow, v)
  }

  @Test
  fun `every known reels id spelling is detected`() {
    // Instagram has renamed this surface repeatedly; a user's build could carry any of
    // these. Missing all of them is what a redesign looks like, and is why the settings
    // screen warns that detection can break.
    listOf(
        "clips_viewer",
        "clips_tab_feed",
        "reels_viewer",
        "clips_video",
        "reel_viewer",
        "clips_swipe",
        "clips_fragment",
      )
      .forEach { id ->
        val v = monitor().verdict(IG, ig(id), emptySet(), REELS, false)
        assertTrue("$id should be recognised as Reels", v is SurfaceVerdict.BlockSurface)
      }
  }

  @Test
  fun `ids are matched on the short form as well as the qualified form`() {
    // uiautomator reports "com.instagram.android:id/clips_viewer" but a hand-entered
    // rule may be the bare name. Both must work.
    assertTrue(monitor().verdict(IG, setOf("clips_viewer"), emptySet(), REELS, false)
      is SurfaceVerdict.BlockSurface)
    assertTrue(monitor().verdict(IG, ig("clips_viewer"), emptySet(), REELS, false)
      is SurfaceVerdict.BlockSurface)
  }
}

class KnownSurfacesTest {

  @Test
  fun `surface ids are unique and stable`() {
    val ids = KnownSurfaces.ALL.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun `every surface names at least one view id`() {
    KnownSurfaces.ALL.forEach { assertTrue(it.label, it.viewIdContains.isNotEmpty()) }
  }

  @Test
  fun `every surface explains what it does not block`() {
    // The detail line is the promise the user is trusting; it must exist.
    KnownSurfaces.ALL.forEach { assertTrue(it.label, it.detail.isNotBlank()) }
  }

  @Test
  fun `lookup by id round-trips`() {
    KnownSurfaces.ALL.forEach { assertEquals(it, KnownSurfaces.byId(it.id)) }
  }
}
