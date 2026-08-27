package com.liktun.japanesehabitlock.ui.settings

import com.liktun.japanesehabitlock.domain.surface.BlockableSurface
import com.liktun.japanesehabitlock.domain.surface.KnownSurfaces
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The precedence rule between app blocking and surface blocking.
 *
 * Worth its own test file because it is the one piece of this screen where being wrong is
 * invisible: a checkbox that renders happily while the service ignores it would leave the
 * user believing a rule is running when it is not.
 */
class SurfaceRowStateTest {

  private val reels = KnownSurfaces.INSTAGRAM_REELS
  private val explore = KnownSurfaces.INSTAGRAM_EXPLORE
  private val shorts = KnownSurfaces.YOUTUBE_SHORTS

  @Test
  fun `unselected surface with no app block is available`() {
    assertEquals(
      SurfaceRowState.AVAILABLE,
      surfaceRowState(reels, blockedSurfaceIds = emptySet(), blockedPackages = emptySet()),
    )
  }

  @Test
  fun `selected instagram surface is checked`() {
    assertEquals(
      SurfaceRowState.CHECKED,
      surfaceRowState(reels, blockedSurfaceIds = setOf("instagram_reels"), blockedPackages = emptySet()),
    )
  }

  @Test
  fun `selected youtube surface is checked`() {
    assertEquals(
      SurfaceRowState.CHECKED,
      surfaceRowState(shorts, blockedSurfaceIds = setOf("youtube_shorts"), blockedPackages = emptySet()),
    )
  }

  @Test
  fun `blocking the whole app supersedes an unchecked surface`() {
    assertEquals(
      SurfaceRowState.SUPERSEDED_BY_APP_BLOCK,
      surfaceRowState(reels, blockedSurfaceIds = emptySet(), blockedPackages = setOf(KnownSurfaces.INSTAGRAM)),
    )
  }

  @Test
  fun `app block wins over a surface that is also checked`() {
    assertEquals(
      SurfaceRowState.SUPERSEDED_BY_APP_BLOCK,
      surfaceRowState(
        reels,
        blockedSurfaceIds = setOf("instagram_reels"),
        blockedPackages = setOf(KnownSurfaces.INSTAGRAM),
      ),
    )
  }

  @Test
  fun `app block wins for youtube shorts too`() {
    assertEquals(
      SurfaceRowState.SUPERSEDED_BY_APP_BLOCK,
      surfaceRowState(
        shorts,
        blockedSurfaceIds = setOf("youtube_shorts"),
        blockedPackages = setOf(KnownSurfaces.YOUTUBE),
      ),
    )
  }

  @Test
  fun `blocking instagram supersedes every instagram surface at once`() {
    val packages = setOf(KnownSurfaces.INSTAGRAM)
    assertEquals(SurfaceRowState.SUPERSEDED_BY_APP_BLOCK, surfaceRowState(reels, emptySet(), packages))
    assertEquals(SurfaceRowState.SUPERSEDED_BY_APP_BLOCK, surfaceRowState(explore, emptySet(), packages))
  }

  @Test
  fun `blocking instagram leaves youtube surfaces untouched`() {
    val packages = setOf(KnownSurfaces.INSTAGRAM)
    assertEquals(SurfaceRowState.AVAILABLE, surfaceRowState(shorts, emptySet(), packages))
    assertEquals(SurfaceRowState.CHECKED, surfaceRowState(shorts, setOf("youtube_shorts"), packages))
  }

  @Test
  fun `a checked sibling surface does not affect this row`() {
    assertEquals(
      SurfaceRowState.AVAILABLE,
      surfaceRowState(explore, blockedSurfaceIds = setOf("instagram_reels"), blockedPackages = emptySet()),
    )
  }

  @Test
  fun `an unrelated blocked package does not supersede`() {
    assertEquals(
      SurfaceRowState.CHECKED,
      surfaceRowState(
        reels,
        blockedSurfaceIds = setOf("instagram_reels"),
        blockedPackages = setOf("com.reddit.frontpage"),
      ),
    )
  }

  @Test
  fun `every known surface resolves to available when nothing is blocked`() {
    KnownSurfaces.ALL.forEach { surface ->
      assertEquals(
        "expected ${surface.id} to be AVAILABLE",
        SurfaceRowState.AVAILABLE,
        surfaceRowState(surface, emptySet(), emptySet()),
      )
    }
  }

  @Test
  fun `state is decided by package and id, not by identity`() {
    // A surface built ad hoc must resolve exactly like a KnownSurfaces constant: the rule
    // reads fields, never object identity, so learned or future surfaces behave the same.
    val custom =
      BlockableSurface(
        id = "instagram_reels",
        packageName = KnownSurfaces.INSTAGRAM,
        label = "Copy",
        detail = "Copy",
        viewIdContains = setOf("clips_viewer"),
      )
    assertEquals(SurfaceRowState.CHECKED, surfaceRowState(custom, setOf("instagram_reels"), emptySet()))
    assertEquals(
      SurfaceRowState.SUPERSEDED_BY_APP_BLOCK,
      surfaceRowState(custom, setOf("instagram_reels"), setOf(KnownSurfaces.INSTAGRAM)),
    )
  }

  @Test
  fun `app labels are human readable and fall back to the package name`() {
    assertEquals("Instagram", surfaceAppLabel(KnownSurfaces.INSTAGRAM))
    assertEquals("YouTube", surfaceAppLabel(KnownSurfaces.YOUTUBE))
    assertEquals("com.example.unknown", surfaceAppLabel("com.example.unknown"))
  }
}
