package com.liktun.japanesehabitlock.domain.surface

/**
 * A blockable *surface* inside an app, rather than the whole app.
 *
 * The gate was built around package names, which is a blunt instrument: blocking
 * `com.instagram.android` also blocks the DMs people actually need. A surface is the
 * narrower unit — the Reels tab, the Shorts player — identified by the view IDs its
 * screen puts in the accessibility tree.
 *
 * @param id stable key used for persistence; never renamed once shipped.
 * @param packageName the app this surface lives in.
 * @param label what the user sees.
 * @param detail one line explaining exactly what is and is not blocked.
 * @param viewIdContains view-id fragments that mean "this surface is on screen".
 *   Matched as substrings of the full `pkg:id/name` so a rename of the prefix does
 *   not break detection.
 * @param entryOnlyViewIds view ids that mean "the surface is *reachable* here but not
 *   yet open" — the tab button itself. Present so a future version can grey out the
 *   tab rather than waiting for the user to land on the feed.
 */
data class BlockableSurface(
  val id: String,
  val packageName: String,
  val label: String,
  val detail: String,
  val viewIdContains: Set<String>,
  val entryOnlyViewIds: Set<String> = emptySet(),
  /**
   * Whether seeing this surface is enough to block it.
   *
   * False for surfaces an app LANDS on. Instagram opens on the Home feed, so an
   * on-sight Feed rule blocked Instagram the moment it opened and made DMs
   * unreachable - the exact thing surface blocking exists to protect. Such a surface
   * only blocks once ScrollGuard sees sustained scrolling on it.
   */
  val blockOnSight: Boolean = true,
)

/**
 * The surfaces this app knows how to recognise.
 *
 * These ids come from the apps' own view hierarchies, which are internal and
 * undocumented. They are stable in practice — `clips_viewer` has named Instagram's
 * Reels player for years — but they are not a contract, and a redesign can silently
 * break detection. That is why [SurfaceLearning] exists and why the settings screen
 * says so plainly rather than promising this never happens.
 */
object KnownSurfaces {

  const val INSTAGRAM = "com.instagram.android"
  const val YOUTUBE = "com.google.android.youtube"

  /**
   * Instagram Reels.
   *
   * The critical distinction this whole feature rests on: the Reels *tab* is an
   * endless recommendation feed, while a reel a friend sends you opens from a
   * conversation. They are different surfaces with different view ids, so the tab can
   * be blocked while shared reels keep working — which is the entire reason a user
   * would choose this over blocking Instagram outright.
   */
  val INSTAGRAM_REELS =
    BlockableSurface(
      id = "instagram_reels",
      packageName = INSTAGRAM,
      label = "Instagram Reels",
      detail = "The Reels tab only. DMs, your feed, stories and reels friends send you keep working.",
      // Several spellings because Instagram has renamed this surface repeatedly and
      // the build a given user has is unknowable from here. Matching any one of them is
      // enough; matching none is what a redesign looks like.
      viewIdContains =
        setOf(
          "clips_viewer",
          "clips_tab_feed",
          "reels_viewer",
          "clips_video",
          "reel_viewer",
          "clips_swipe",
          "clips_fragment",
          "reels_tray",
        ),
      entryOnlyViewIds = setOf("clips_tab", "tab_avatar_clips"),
    )

  /** Instagram Explore: the other infinite surface, offered separately. */
  val INSTAGRAM_EXPLORE =
    BlockableSurface(
      id = "instagram_explore",
      packageName = INSTAGRAM,
      label = "Instagram Explore",
      detail = "The search and discovery grid. Your own feed and DMs keep working.",
      viewIdContains = setOf("explore_grid", "discover_container"),
      entryOnlyViewIds = setOf("search_tab"),
    )

  /**
   * Instagram's main Home feed.
   *
   * Added after a user reported that blocking Reels alone just moved the same
   * compulsive scrolling to the Home feed instead - Instagram interleaves suggested
   * and Reels-style content into the feed too, so it is functionally the same surface
   * wearing a different name. This is the last of the three named Instagram surfaces;
   * see [com.liktun.japanesehabitlock.domain.surface.ScrollGuard] for why naming
   * individual surfaces is a losing game and what backs it up.
   */
  val INSTAGRAM_FEED =
    BlockableSurface(
      id = "instagram_feed",
      packageName = INSTAGRAM,
      label = "Instagram Feed",
      detail = "Blocks the home feed after about 15 seconds of scrolling. Opening Instagram and your DMs keep working.",
      blockOnSight = false,
      // "feed_tab" and "tab_bar_home" do NOT belong here: they are the Home tab
      // button in the bottom nav bar, visible on every screen of the app, not just
      // the feed. Shipping them in viewIdContains blocked Instagram on open
      // regardless of which screen the user landed on - the same class of bug as
      // the direct-message nav button that once disabled Reels blocking entirely.
      // Only ids that mean the feed's scrolling list ITSELF is on screen belong here.
      viewIdContains =
        setOf(
          "feed_recycler_view",
          "main_feed",
          "newsfeed_recycler",
          "feed_timeline",
        ),
      entryOnlyViewIds = setOf("feed_tab", "feed_tab_icon", "tab_bar_home"),
    )

  /** YouTube Shorts, the same shape of problem in a different app. */
  val YOUTUBE_SHORTS =
    BlockableSurface(
      id = "youtube_shorts",
      packageName = YOUTUBE,
      label = "YouTube Shorts",
      detail = "The Shorts player only. Search, subscriptions and normal videos keep working.",
      viewIdContains =
        setOf(
          "reel_recycler",
          "reel_watch",
          "shorts_container",
          "reel_player",
          "shorts_video",
          "reel_progress",
        ),
      entryOnlyViewIds = setOf("shorts_tab", "pivot_shorts"),
    )

  val ALL: List<BlockableSurface> =
    listOf(INSTAGRAM_REELS, INSTAGRAM_EXPLORE, INSTAGRAM_FEED, YOUTUBE_SHORTS)

  fun byId(id: String): BlockableSurface? = ALL.firstOrNull { it.id == id }

  /** The surfaces defined for [packageName]. */
  fun forPackage(packageName: String): List<BlockableSurface> =
    ALL.filter { it.packageName == packageName }
}
