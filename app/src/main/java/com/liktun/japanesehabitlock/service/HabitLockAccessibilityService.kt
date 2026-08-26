package com.liktun.japanesehabitlock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.domain.Roadmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Watches which app is in the foreground and shows the blocker when the gate is shut.
 *
 * This class is intentionally thin. Every decision lives in [ForegroundAppMonitor],
 * which has no Android dependencies and is unit-tested; all this service does is feed
 * it a package name plus the two pieces of state it observes, and act on the answer.
 * Accessibility services are painful to test, so the less logic they hold the better.
 */
class HabitLockAccessibilityService : AccessibilityService() {

  private val monitor by lazy {
    ForegroundAppMonitor(packageName, neverBlockable = Roadmap.STUDY_TOOL_PACKAGES)
  }

  private var scope: CoroutineScope? = null

  /**
   * The repository used for heartbeat writes, kept so [onAccessibilityEvent] can reach it
   * without rebuilding one per event. Null before connect and after unbind.
   */
  @Volatile
  private var repository: ChecklistRepository? = null

  /**
   * Wall-clock millis of our last heartbeat write, or 0 when we have not written one.
   *
   * A plain field rather than anything persisted, because it exists purely to throttle
   * disk writes. `TYPE_WINDOW_STATE_CHANGED` fires many times per minute of normal phone
   * use, and persisting a timestamp on each one would hammer DataStore — a disk write and
   * a full-file rewrite per window change — for no extra signal. Once every
   * [HEARTBEAT_INTERVAL_MILLIS] is more than precise enough to distinguish a live service
   * from one an OEM killed hours ago.
   */
  @Volatile
  private var lastHeartbeatMillis: Long = 0L

  /**
   * Latest gate state, mirrored into fields because [onAccessibilityEvent] is a
   * synchronous callback that cannot suspend to read a Flow. Marked @Volatile because
   * the collector and the event callback are not guaranteed to be the same thread.
   */
  @Volatile
  private var isUnlocked: Boolean = false

  @Volatile
  private var blockedPackages: Set<String> = emptySet()

  override fun onServiceConnected() {
    super.onServiceConnected()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { this.scope = it }
    val repository = ChecklistRepository(applicationContext.habitLockDataStore).also {
      this.repository = it
    }
    // Record one heartbeat the moment we bind. Without this, a service that is enabled
    // but has not yet seen a window change is indistinguishable from one that never
    // started, and the UI would show an alarming "never started" warning to a user whose
    // service is in fact perfectly healthy.
    recordHeartbeat(System.currentTimeMillis(), force = true)
    // One collector for both flows: the two values are only ever read together, so a
    // single subscription keeps them consistent and halves the DataStore reads.
    scope.launch {
      combine(repository.isUnlocked, repository.blockedPackages) { unlocked, packages ->
        unlocked to packages
      }.collect { (unlocked, packages) ->
        isUnlocked = unlocked
        blockedPackages = packages
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    // Proof of life, throttled. Every window change we handle is evidence the OS is still
    // delivering events to us, which is the only reliable signal that an OEM has not
    // silently reaped the process.
    recordHeartbeat(System.currentTimeMillis())
    val foregroundPackage = event.packageName?.toString()
    if (monitor.shouldLaunchBlocker(foregroundPackage, blockedPackages, isUnlocked)) {
      launchBlocker()
    }
  }

  /**
   * Persists [nowMillis] as the latest proof of life, at most once every
   * [HEARTBEAT_INTERVAL_MILLIS] unless [force] is set.
   *
   * The throttle check happens on the event thread and the write is dispatched to the
   * collector scope, so a window change never waits on disk. A backwards clock jump
   * (`nowMillis` before the last write) also passes the check, which is intentional: it
   * refreshes the stored value rather than leaving a future timestamp wedged in place.
   */
  private fun recordHeartbeat(nowMillis: Long, force: Boolean = false) {
    if (!force && nowMillis - lastHeartbeatMillis < HEARTBEAT_INTERVAL_MILLIS &&
      nowMillis >= lastHeartbeatMillis
    ) {
      return
    }
    lastHeartbeatMillis = nowMillis
    val repository = repository ?: return
    scope?.launch { repository.recordServiceHeartbeat(nowMillis) }
  }

  /**
   * The blocker Activity is referenced by name rather than by class literal so this
   * file compiles independently of the UI layer that owns it.
   */
  private fun launchBlocker() {
    val intent = Intent()
      .setClassName(packageName, BLOCKER_ACTIVITY)
      // NEW_TASK is required to start an Activity from a Service; CLEAR_TASK stops a
      // stack of blocker instances building up behind the one the user can see.
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)
  }

  /** No-op: we never announce anything, so there is nothing to interrupt. */
  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    stopCollecting()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    stopCollecting()
    super.onDestroy()
  }

  /** Idempotent: the service can be unbound and destroyed in either order. */
  private fun stopCollecting() {
    scope?.cancel()
    scope = null
    repository = null
  }

  private companion object {
    const val BLOCKER_ACTIVITY = "com.liktun.japanesehabitlock.ui.blocker.BlockerActivity"

    /**
     * Minimum gap between persisted heartbeats.
     *
     * Five minutes is two orders of magnitude below `ServiceHeartbeat`'s six-hour stale
     * window, so the freshness signal is never the limiting factor, while still cutting
     * DataStore writes from "every window change" down to a handful per hour.
     */
    const val HEARTBEAT_INTERVAL_MILLIS = 5L * 60L * 1000L
  }
}
