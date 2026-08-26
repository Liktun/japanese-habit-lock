package com.liktun.japanesehabitlock.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ServiceHeartbeatTest {

  private val window = ServiceHeartbeat.DEFAULT_STALE_AFTER_MILLIS
  private val now = 1_700_000_000_000L

  private fun heartbeat(staleAfterMillis: Long = window) = ServiceHeartbeat(staleAfterMillis)

  @Test
  fun `disabled in settings reports Disabled even with a fresh heartbeat`() {
    assertEquals(
      HeartbeatStatus.Disabled,
      heartbeat().status(lastSeenMillis = now, nowMillis = now, serviceEnabledInSettings = false),
    )
  }

  @Test
  fun `disabled in settings reports Disabled even with no heartbeat at all`() {
    assertEquals(
      HeartbeatStatus.Disabled,
      heartbeat().status(lastSeenMillis = null, nowMillis = now, serviceEnabledInSettings = false),
    )
  }

  @Test
  fun `disabled overrides a heartbeat old enough to look like an OEM kill`() {
    // The toggle being off means no process could have checked in, so "Silenced" would
    // be a false accusation against the manufacturer.
    assertEquals(
      HeartbeatStatus.Disabled,
      heartbeat().status(
        lastSeenMillis = now - window * 10,
        nowMillis = now,
        serviceEnabledInSettings = false,
      ),
    )
  }

  @Test
  fun `enabled with no heartbeat ever reports NeverStarted`() {
    assertEquals(
      HeartbeatStatus.NeverStarted,
      heartbeat().status(lastSeenMillis = null, nowMillis = now, serviceEnabledInSettings = true),
    )
  }

  @Test
  fun `NeverStarted is distinct from Silenced`() {
    val neverStarted =
      heartbeat().status(lastSeenMillis = null, nowMillis = now, serviceEnabledInSettings = true)
    val silenced = heartbeat().status(
      lastSeenMillis = now - window - 1,
      nowMillis = now,
      serviceEnabledInSettings = true,
    )
    assertNotEquals(neverStarted, silenced)
  }

  @Test
  fun `a heartbeat from this instant is Healthy`() {
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(lastSeenMillis = now, nowMillis = now, serviceEnabledInSettings = true),
    )
  }

  @Test
  fun `a heartbeat a minute ago is Healthy`() {
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now - 60_000L,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a heartbeat just inside the window is Healthy`() {
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now - window + 1,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `an age exactly equal to the stale window is still Healthy`() {
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now - window,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `one millisecond past the stale window is Silenced`() {
    assertEquals(
      HeartbeatStatus.Silenced,
      heartbeat().status(
        lastSeenMillis = now - window - 1,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a heartbeat from days ago is Silenced`() {
    assertEquals(
      HeartbeatStatus.Silenced,
      heartbeat().status(
        lastSeenMillis = now - 3L * 24 * 60 * 60 * 1000,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a clock moved backwards is Healthy not Silenced`() {
    // The stored heartbeat is in the "future" because the wall clock jumped back. That is
    // a clock problem, never evidence of an OEM kill.
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now + 60_000L,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a clock moved backwards by more than the whole stale window is still Healthy`() {
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now + window * 5,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a backwards clock while disabled is still reported as Disabled`() {
    assertEquals(
      HeartbeatStatus.Disabled,
      heartbeat().status(
        lastSeenMillis = now + window,
        nowMillis = now,
        serviceEnabledInSettings = false,
      ),
    )
  }

  @Test
  fun `the default stale window is six hours`() {
    assertEquals(6L * 60 * 60 * 1000, ServiceHeartbeat.DEFAULT_STALE_AFTER_MILLIS)
  }

  @Test
  fun `an overnight gap does not cry wolf under the default window`() {
    // Eight hours of a phone on a nightstand emits no window changes at all, so five
    // hours of silence must not be reported as a kill.
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat().status(
        lastSeenMillis = now - 5L * 60 * 60 * 1000,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a custom shorter window makes the same gap Silenced`() {
    val fiveMinutes = 5L * 60 * 1000
    assertEquals(
      HeartbeatStatus.Silenced,
      heartbeat(fiveMinutes).status(
        lastSeenMillis = now - 10L * 60 * 1000,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a custom shorter window still honours its own boundary as Healthy`() {
    val fiveMinutes = 5L * 60 * 1000
    assertEquals(
      HeartbeatStatus.Healthy,
      heartbeat(fiveMinutes).status(
        lastSeenMillis = now - fiveMinutes,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `a zero window treats any elapsed millisecond as Silenced`() {
    assertEquals(
      HeartbeatStatus.Silenced,
      heartbeat(0L).status(
        lastSeenMillis = now - 1,
        nowMillis = now,
        serviceEnabledInSettings = true,
      ),
    )
  }

  @Test
  fun `epoch zero is a real heartbeat and not treated as missing`() {
    // 0L is a legitimate Long; only null means "never recorded".
    assertEquals(
      HeartbeatStatus.Silenced,
      heartbeat().status(lastSeenMillis = 0L, nowMillis = now, serviceEnabledInSettings = true),
    )
  }
}
