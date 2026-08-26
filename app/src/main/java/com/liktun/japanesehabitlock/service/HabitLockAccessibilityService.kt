package com.liktun.japanesehabitlock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
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

  private val monitor by lazy { ForegroundAppMonitor(packageName) }

  private var scope: CoroutineScope? = null

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
    val repository = ChecklistRepository(applicationContext.habitLockDataStore)
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
    val foregroundPackage = event.packageName?.toString()
    if (monitor.shouldLaunchBlocker(foregroundPackage, blockedPackages, isUnlocked)) {
      launchBlocker()
    }
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
  }

  private companion object {
    const val BLOCKER_ACTIVITY = "com.liktun.japanesehabitlock.ui.blocker.BlockerActivity"
  }
}
