package com.liktun.japanesehabitlock.ui.settings

import com.liktun.japanesehabitlock.data.apps.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker's filter and ordering are pure, so they are tested directly rather than
 * through a ViewModel — no Looper, no PackageManager, no coroutine plumbing.
 */
class SettingsFilteringTest {

  private val instagram = InstalledApp("com.instagram.android", "Instagram")
  private val reddit = InstalledApp("com.reddit.frontpage", "Reddit")
  private val youtube = InstalledApp("com.google.android.youtube", "YouTube")
  private val apps = listOf(instagram, reddit, youtube)

  @Test
  fun `empty query returns every app in the original order`() {
    assertEquals(apps, visibleApps(apps, blockedPackages = emptySet(), searchQuery = ""))
  }

  @Test
  fun `blank query is treated as empty`() {
    assertEquals(apps, visibleApps(apps, blockedPackages = emptySet(), searchQuery = "   "))
  }

  @Test
  fun `query matches the label case-insensitively`() {
    assertEquals(
      listOf(instagram),
      visibleApps(apps, blockedPackages = emptySet(), searchQuery = "iNsTaGrAm"),
    )
  }

  @Test
  fun `query matches a partial label`() {
    assertEquals(listOf(youtube), visibleApps(apps, blockedPackages = emptySet(), searchQuery = "tube"))
  }

  @Test
  fun `query matches the package name`() {
    assertEquals(
      listOf(reddit),
      visibleApps(apps, blockedPackages = emptySet(), searchQuery = "com.reddit.frontpage"),
    )
  }

  @Test
  fun `query matches the package name case-insensitively`() {
    assertEquals(
      listOf(youtube),
      visibleApps(apps, blockedPackages = emptySet(), searchQuery = "GOOGLE.ANDROID.YOUTUBE"),
    )
  }

  @Test
  fun `blocked apps sort to the top`() {
    val result = visibleApps(apps, blockedPackages = setOf(youtube.packageName), searchQuery = "")
    assertEquals(listOf(youtube, instagram, reddit), result)
  }

  @Test
  fun `blocked ordering is stable within each group`() {
    val result =
      visibleApps(apps, blockedPackages = setOf(youtube.packageName, reddit.packageName), searchQuery = "")
    assertEquals(listOf(reddit, youtube, instagram), result)
  }

  @Test
  fun `blocked apps sort to the top of a filtered list`() {
    val result = visibleApps(apps, blockedPackages = setOf(reddit.packageName), searchQuery = "com.")
    assertEquals(listOf(reddit, instagram, youtube), result)
  }

  @Test
  fun `no match returns an empty list`() {
    val result = visibleApps(apps, blockedPackages = setOf(instagram.packageName), searchQuery = "zzzz")
    assertTrue(result.isEmpty())
  }

  @Test
  fun `Ready exposes the same filtering through visibleApps`() {
    val state =
      SettingsUiState.Ready(
        apps = apps,
        blockedPackages = setOf(youtube.packageName),
        searchQuery = "e",
      )
    assertEquals(listOf(youtube, reddit), state.visibleApps)
  }
}
