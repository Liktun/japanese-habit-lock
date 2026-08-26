package com.liktun.japanesehabitlock.service

/**
 * The four states the accessibility service can be in, from the app's point of view.
 *
 * The distinction between [Disabled], [NeverStarted] and [Silenced] is the entire reason
 * this type exists. All three mean "blocking is not happening", but they need completely
 * different words in the UI: one is a user choice, one is a setup problem, and one is an
 * OEM killing us behind the user's back while the toggle still reads "on".
 */
sealed interface HeartbeatStatus {

  /**
   * The service is not enabled in system accessibility settings.
   *
   * Nothing is wrong — the user simply has not turned it on, or turned it off on purpose.
   * The right response is an onboarding prompt, not an alarm.
   */
  data object Disabled : HeartbeatStatus

  /** Enabled, and it checked in recently enough that we believe it is alive. */
  data object Healthy : HeartbeatStatus

  /**
   * Enabled in settings, but no heartbeat has EVER been recorded.
   *
   * Usually one of two things: the OS refused to bind the service (some ROMs silently
   * decline), or the user flipped the toggle and never came back to the app, so the
   * service has genuinely never observed a window change. Either way it is a setup
   * issue rather than proof of a kill, so it deserves gentler wording than [Silenced].
   */
  data object NeverStarted : HeartbeatStatus

  /**
   * Enabled in settings, but the last heartbeat is older than the stale window.
   *
   * THIS IS THE OEM-KILL SIGNATURE. Samsung, Xiaomi, Huawei and OPPO reap background
   * processes without clearing the accessibility toggle, so the user opens Settings,
   * sees a switch that is still on, and believes they are protected while nothing is
   * being blocked at all. That silent false sense of safety is the worst failure mode
   * this app has, and it is the one case worth shouting about.
   */
  data object Silenced : HeartbeatStatus
}

/**
 * Turns "when did the service last check in?" into a status worth showing the user.
 *
 * Deliberately free of Android types, following [ForegroundAppMonitor]: the whole value
 * here is in the boundary conditions (never started vs. gone quiet, clock skew, the exact
 * staleness edge), and those are only cheap to test when plain JUnit can construct the
 * class. The service and the UI stay thin adapters that supply three plain values.
 */
class ServiceHeartbeat(private val staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS) {

  /**
   * Classifies the service's health.
   *
   * [serviceEnabledInSettings] wins over everything else: if the toggle is off there is
   * no process that could have emitted a heartbeat, so a stale timestamp says nothing
   * about OEM behaviour and reporting [HeartbeatStatus.Silenced] would be a lie.
   */
  fun status(
    lastSeenMillis: Long?,
    nowMillis: Long,
    serviceEnabledInSettings: Boolean,
  ): HeartbeatStatus {
    if (!serviceEnabledInSettings) return HeartbeatStatus.Disabled
    if (lastSeenMillis == null) return HeartbeatStatus.NeverStarted
    // Clock moved backwards (manual change, NTP correction, timezone-driven wall-clock
    // jump). The heartbeat is in the "future", which is not evidence of anything except
    // a clock, and must never be reported as an OEM kill — accusing the user's phone of
    // killing us because they adjusted the time would destroy trust in the warning that
    // actually matters.
    if (nowMillis < lastSeenMillis) return HeartbeatStatus.Healthy
    val age = nowMillis - lastSeenMillis
    // Inclusive: an age exactly equal to the window is still inside it.
    return if (age <= staleAfterMillis) HeartbeatStatus.Healthy else HeartbeatStatus.Silenced
  }

  companion object {
    /**
     * Six hours before a quiet service counts as killed.
     *
     * The service only emits a heartbeat when it *observes a window change*, so silence
     * is not the same as death: a phone sitting on a nightstand from 23:00 to 07:00
     * produces no events at all while the service is perfectly alive. Any window shorter
     * than a sleep cycle would therefore cry wolf every single morning, and a warning
     * that is wrong every morning is a warning the user learns to ignore — which would
     * cost us the one time it is real. Six hours is long enough to sit out a normal
     * night's sleep, short enough that a kill is surfaced within the same day.
     */
    const val DEFAULT_STALE_AFTER_MILLIS: Long = 6L * 60L * 60L * 1000L
  }
}
