package com.liktun.japanesehabitlock.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppMonitorTest {

  private val self = "com.liktun.japanesehabitlock"
  private val blocked = setOf(INSTAGRAM, TIKTOK)

  private fun monitor() = ForegroundAppMonitor(self)

  @Test
  fun `an unlocked gate blocks nothing at all`() {
    assertFalse(monitor().shouldBlock(INSTAGRAM, blocked, isUnlocked = true))
  }

  @Test
  fun `a null package is never blocked`() {
    assertFalse(monitor().shouldBlock(null, blocked, isUnlocked = false))
  }

  @Test
  fun `an empty package is never blocked`() {
    assertFalse(monitor().shouldBlock("", blocked, isUnlocked = false))
  }

  @Test
  fun `a whitespace-only package is never blocked`() {
    assertFalse(monitor().shouldBlock("   ", blocked, isUnlocked = false))
  }

  @Test
  fun `our own package is never blocked even if it is on the blocked list`() {
    // Blocking ourselves would make the blocker screen re-trigger on itself forever.
    assertFalse(monitor().shouldBlock(self, blocked + self, isUnlocked = false))
  }

  @Test
  fun `settings is always allowed so the service can be turned off`() {
    assertFalse(monitor().shouldBlock("com.android.settings", blocked + "com.android.settings", false))
  }

  @Test
  fun `system ui is always allowed`() {
    assertFalse(monitor().shouldBlock("com.android.systemui", blocked + "com.android.systemui", false))
  }

  @Test
  fun `the aosp launcher is always allowed so home always works`() {
    assertFalse(monitor().shouldBlock("com.android.launcher", blocked + "com.android.launcher", false))
  }

  @Test
  fun `the pixel launcher is always allowed`() {
    val pixel = "com.google.android.apps.nexuslauncher"
    assertFalse(monitor().shouldBlock(pixel, blocked + pixel, isUnlocked = false))
  }

  @Test
  fun `the aosp dialer is always allowed for emergency calls`() {
    assertFalse(monitor().shouldBlock("com.android.dialer", blocked + "com.android.dialer", false))
  }

  @Test
  fun `the google dialer is always allowed for emergency calls`() {
    val dialer = "com.google.android.dialer"
    assertFalse(monitor().shouldBlock(dialer, blocked + dialer, isUnlocked = false))
  }

  @Test
  fun `the telecom package is always allowed`() {
    val telecom = "com.android.server.telecom"
    assertFalse(monitor().shouldBlock(telecom, blocked + telecom, isUnlocked = false))
  }

  @Test
  fun `every always-allowed package survives a hostile blocked list`() {
    val hostile = ForegroundAppMonitor.ALWAYS_ALLOWED
    ForegroundAppMonitor.ALWAYS_ALLOWED.forEach { allowed ->
      assertFalse(allowed, monitor().shouldBlock(allowed, hostile, isUnlocked = false))
    }
  }

  @Test
  fun `the always-allowed set covers settings home and the phone`() {
    val required = setOf(
      "com.android.settings",
      "com.android.systemui",
      "com.android.launcher",
      "com.google.android.apps.nexuslauncher",
      "com.android.dialer",
      "com.google.android.dialer",
      "com.android.server.telecom",
    )
    assertTrue(ForegroundAppMonitor.ALWAYS_ALLOWED.containsAll(required))
  }

  @Test
  fun `a genuinely blocked package is blocked while the gate is shut`() {
    assertTrue(monitor().shouldBlock(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `a package that is not on the list is left alone`() {
    assertFalse(monitor().shouldBlock("com.example.notes", blocked, isUnlocked = false))
  }

  @Test
  fun `an empty blocked list blocks nothing`() {
    assertFalse(monitor().shouldBlock(INSTAGRAM, emptySet(), isUnlocked = false))
  }

  @Test
  fun `shouldBlock has no memory so it can be asked repeatedly`() {
    val monitor = monitor()
    assertTrue(monitor.shouldBlock(INSTAGRAM, blocked, isUnlocked = false))
    assertTrue(monitor.shouldBlock(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `the blocker launches once per entry into a blocked app`() {
    val monitor = monitor()
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    // Repeat window events for the same app must not relaunch the blocker.
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `leaving for the launcher and coming back launches the blocker again`() {
    val monitor = monitor()
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    assertFalse(monitor.shouldLaunchBlocker("com.android.launcher", blocked, isUnlocked = false))
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `switching between two blocked apps launches the blocker for each`() {
    val monitor = monitor()
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    assertTrue(monitor.shouldLaunchBlocker(TIKTOK, blocked, isUnlocked = false))
    assertFalse(monitor.shouldLaunchBlocker(TIKTOK, blocked, isUnlocked = false))
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `a null event does not clear the debounce latch`() {
    val monitor = monitor()
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    // Events with no package name are noise, not a real move away from the app.
    assertFalse(monitor.shouldLaunchBlocker(null, blocked, isUnlocked = false))
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `an unlocked gate never launches the blocker`() {
    val monitor = monitor()
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = true))
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = true))
  }

  @Test
  fun `unlocking mid-session clears the latch so re-locking blocks again`() {
    val monitor = monitor()
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    assertFalse(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = true))
    assertTrue(monitor.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  @Test
  fun `our own package never launches the blocker`() {
    val monitor = monitor()
    assertFalse(monitor.shouldLaunchBlocker(self, blocked + self, isUnlocked = false))
  }

  @Test
  fun `two monitors keep independent debounce state`() {
    val first = monitor()
    val second = monitor()
    assertTrue(first.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
    assertTrue(second.shouldLaunchBlocker(INSTAGRAM, blocked, isUnlocked = false))
  }

  private companion object {
    const val INSTAGRAM = "com.instagram.android"
    const val TIKTOK = "com.zhiliaoapp.musically"
  }
}
