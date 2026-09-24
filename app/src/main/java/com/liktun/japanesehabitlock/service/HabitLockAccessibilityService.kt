package com.liktun.japanesehabitlock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.surface.ScrollGuard
import com.liktun.japanesehabitlock.domain.surface.SurfaceMonitor
import com.liktun.japanesehabitlock.domain.surface.SurfaceVerdict
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

  /**
   * Decides both whole-app and surface-level blocks.
   *
   * [ForegroundAppMonitor] is gone from the event path: [SurfaceMonitor] subsumes it,
   * enforcing the same always-allowed floor and study-tool exemption while also
   * understanding surfaces. Keeping two monitors would mean two places for the floor
   * to drift out of sync, which is exactly the rule that must never be wrong.
   */
  private val monitor by lazy {
    SurfaceMonitor(
      packageName,
      neverBlockable = Roadmap.STUDY_TOOL_PACKAGES,
      scrollGuard = ScrollGuard(),
    )
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

  @Volatile
  private var blockedSurfaces: Set<String> = emptySet()

  override fun onServiceConnected() {
    super.onServiceConnected()
    // Attached first, synchronously: if the process was killed while the user was in
    // Instagram, the system restarts this service with fresh static state, and the
    // recording flag must be restored BEFORE the first event arrives. Otherwise the
    // window the user turned recording on to observe is exactly the window that is
    // missed.
    SurfaceDiagnostics.attach(PrefsDiagnosticsStore(applicationContext))

    // Belt and braces on top of the XML config. FLAG_REPORT_VIEW_IDS is what makes
    // getViewIdResourceName() return anything at all; without it every id is null and no
    // surface rule can ever match, which is exactly how this shipped broken. Setting it
    // here as well covers a service that was granted BEFORE this version was installed
    // and would otherwise keep its old configuration until the user re-granted it.
    serviceInfo =
      (serviceInfo ?: AccessibilityServiceInfo()).apply {
        flags = flags or
          AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
          AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        // ScrollGuard's fallback needs scroll events delivered at all; without this
        // in the runtime mask (mirroring the XML fix) a service granted before this
        // version would silently never see TYPE_VIEW_SCROLLED until re-granted.
        eventTypes = eventTypes or AccessibilityEvent.TYPE_VIEW_SCROLLED
      }
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
      combine(
        repository.isUnlocked,
        repository.blockedPackages,
        repository.blockedSurfaces,
      ) { unlocked, packages, surfaces ->
        Triple(unlocked, packages, surfaces)
      }.collect { (unlocked, packages, surfaces) ->
        isUnlocked = unlocked
        blockedPackages = packages
        blockedSurfaces = surfaces
        // Clear the debounce when the gate opens, so the very next block after the
        // day resets fires instead of being swallowed as a repeat.
        if (unlocked) monitor.reset()
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    val type = event.eventType

    // Scroll events feed ScrollGuard's clock regardless of what else this event does.
    // Fed unconditionally (not gated on blockedSurfaces, unlike the view-id read below)
    // because the clock itself is cheap - it is one comparison and an add, nothing
    // touches the view tree - and gating it would mean dwell time silently resets the
    // moment someone unticks a surface mid-scroll, which is a confusing thing for a
    // habit tool to do to itself.
    if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
      event.packageName?.toString()?.let { pkg -> monitor.onScrollEvent(pkg, System.currentTimeMillis()) }
    }

    if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
      type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
      type != AccessibilityEvent.TYPE_VIEW_SCROLLED
    ) {
      return
    }
    // Proof of life, throttled. Every window change we handle is evidence the OS is still
    // delivering events to us, which is the only reliable signal that an OEM has not
    // silently reaped the process.
    recordHeartbeat(System.currentTimeMillis())

    val foregroundPackage = event.packageName?.toString() ?: return

    // STATE_CHANGED only: CONTENT_CHANGED fires constantly from systemui (the status
    // bar clock), which would wipe a real scroll session every minute.
    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      monitor.onForegroundChanged(foregroundPackage)
    }

    // Reading the view tree is not free, so it is skipped entirely unless this package
    // actually has a surface rule that could apply. For every other app the decision is
    // still made from the package name alone, exactly as before.
    val needsViewIds =
      blockedSurfaces.isNotEmpty() &&
        com.liktun.japanesehabitlock.domain.surface.KnownSurfaces
          .forPackage(foregroundPackage)
          .any { it.id in blockedSurfaces }

    // When diagnostics are on, read the tree for ANY app even without a rule: the whole
    // point is to discover the ids a broken rule should have matched.
    val viewIds =
      if (needsViewIds || SurfaceDiagnostics.enabled) {
        ViewIdCollector.collect(rootInActiveWindow)
      } else {
        emptySet()
      }

    // Recorded before the debounce, so a screen the user is already sitting on still
    // shows up in the report rather than being swallowed as a repeat.
    if (SurfaceDiagnostics.enabled) {
      val plain =
        monitor.verdict(
          foregroundPackage = foregroundPackage,
          visibleViewIds = viewIds,
          blockedPackages = blockedPackages,
          blockedSurfaceIds = blockedSurfaces,
          isUnlocked = isUnlocked,
        )
      SurfaceDiagnostics.record(
        packageName = foregroundPackage,
        viewIds = viewIds,
        verdict =
          when (plain) {
            is SurfaceVerdict.Allow ->
              if (isUnlocked) "allowed (gate open)" else "allowed (no rule matched)"
            is SurfaceVerdict.BlockApp -> "blocked (whole app)"
            is SurfaceVerdict.BlockSurface -> "blocked (${plain.surface.label})"
            is SurfaceVerdict.BlockSustainedScrolling -> "blocked (sustained scrolling)"
          },
      )
    }

    when (val verdict =
      monitor.shouldLaunchBlocker(
        foregroundPackage = foregroundPackage,
        visibleViewIds = viewIds,
        blockedPackages = blockedPackages,
        blockedSurfaceIds = blockedSurfaces,
        isUnlocked = isUnlocked,
      )
    ) {
      is SurfaceVerdict.Allow -> Unit
      is SurfaceVerdict.BlockApp -> launchBlocker(surfaceLabel = null)
      is SurfaceVerdict.BlockSurface -> launchBlocker(surfaceLabel = verdict.surface.label)
      is SurfaceVerdict.BlockSustainedScrolling ->
        // No named surface to point at - this fired on behaviour, not a screen id - so
        // the blocker gets a generic label naming the mechanism instead of a screen.
        launchBlocker(surfaceLabel = "Extended scrolling")
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
  private fun launchBlocker(surfaceLabel: String?) {
    val intent = Intent()
      .setClassName(packageName, BLOCKER_ACTIVITY)
      // Lets the blocker say "Instagram Reels" rather than a generic message, so the
      // user can see the surface rule fired rather than an app-wide block.
      .putExtra(EXTRA_SURFACE_LABEL, surfaceLabel)
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

    /** Intent extra naming the blocked surface, or absent for a whole-app block. */
    const val EXTRA_SURFACE_LABEL = "surface_label"

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
