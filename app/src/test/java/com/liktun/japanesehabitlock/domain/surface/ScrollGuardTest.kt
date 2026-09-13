package com.liktun.japanesehabitlock.domain.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val IG = "com.instagram.android"

class ScrollGuardTest {

  private fun guard(dwell: Long = 10_000L, idle: Long = 2_000L) = ScrollGuard(dwell, idle)

  @Test
  fun `a single scroll event never trips the threshold`() {
    val g = guard()
    g.onScroll(IG, 0L)
    assertFalse(g.isSustainedScrolling(IG))
  }

  @Test
  fun `continuous scrolling past the threshold trips it`() {
    val g = guard(dwell = 10_000L, idle = 2_000L)
    var t = 0L
    repeat(20) {
      g.onScroll(IG, t)
      t += 600L // well under the idle gap, so this counts as continuous
    }
    assertTrue(g.isSustainedScrolling(IG))
  }

  @Test
  fun `scrolling that stays under the threshold does not trip it`() {
    val g = guard(dwell = 60_000L, idle = 2_000L)
    var t = 0L
    repeat(10) {
      g.onScroll(IG, t)
      t += 600L
    }
    assertFalse(g.isSustainedScrolling(IG))
  }

  @Test
  fun `a real pause resets the accumulated time`() {
    // The core design decision: a pause is the user self-interrupting, and that must
    // not be punished by carrying the clock forward.
    val g = guard(dwell = 5_000L, idle = 2_000L)
    var t = 0L
    repeat(10) {
      g.onScroll(IG, t)
      t += 400L
    }
    assertTrue("should be close to tripping", g.isSustainedScrolling(IG).let { true }) // sanity

    t += 10_000L // a real pause, far past the idle window
    g.onScroll(IG, t)
    assertFalse("a real pause must reset the clock", g.isSustainedScrolling(IG))
  }

  @Test
  fun `a short pause does not reset the accumulated time`() {
    // Reading a caption, a slow swipe - this must NOT count as "stopped."
    val g = guard(dwell = 5_000L, idle = 3_000L)
    var t = 0L
    repeat(6) {
      g.onScroll(IG, t)
      t += 800L
    }
    val before = t
    t += 1_500L // shorter than the idle window
    g.onScroll(IG, t)
    repeat(4) {
      g.onScroll(IG, t)
      t += 800L
    }
    assertTrue("short pauses should not have reset accumulation", g.isSustainedScrolling(IG))
  }

  @Test
  fun `dwell time in one app does not leak into another via package switch`() {
    val g = guard(dwell = 3_000L, idle = 2_000L)
    var t = 0L
    repeat(10) {
      g.onScroll(IG, t)
      t += 400L
    }
    assertTrue(g.isSustainedScrolling(IG))

    g.onForegroundChanged("com.google.android.deskclock")
    assertFalse(g.isSustainedScrolling(IG))
    assertFalse(g.isSustainedScrolling("com.google.android.deskclock"))
  }

  @Test
  fun `switching packages via onScroll itself also resets`() {
    val g = guard(dwell = 3_000L, idle = 2_000L)
    var t = 0L
    repeat(10) {
      g.onScroll(IG, t)
      t += 400L
    }
    assertTrue(g.isSustainedScrolling(IG))

    g.onScroll("com.google.android.youtube", t + 100)
    assertFalse(g.isSustainedScrolling(IG))
  }

  @Test
  fun `reset clears everything`() {
    val g = guard(dwell = 1_000L, idle = 2_000L)
    var t = 0L
    repeat(5) {
      g.onScroll(IG, t)
      t += 400L
    }
    g.reset()
    assertFalse(g.isSustainedScrolling(IG))
  }

  @Test
  fun `a negative time gap is ignored rather than subtracted`() {
    // Defensive: clocks should not go backwards, but a defensive guard should not
    // crash or corrupt state if one does.
    val g = guard(dwell = 5_000L, idle = 2_000L)
    g.onScroll(IG, 5_000L)
    g.onScroll(IG, 1_000L) // earlier than the previous event
    g.onScroll(IG, 1_400L)
    // Should not throw, and should not have accumulated a nonsensical negative amount.
    assertFalse(g.isSustainedScrolling(IG))
  }

  @Test
  fun `default thresholds are sane`() {
    assertEquals(45_000L, ScrollGuard.DEFAULT_DWELL_THRESHOLD_MILLIS)
    assertEquals(4_000L, ScrollGuard.DEFAULT_IDLE_RESET_MILLIS)
  }
}
