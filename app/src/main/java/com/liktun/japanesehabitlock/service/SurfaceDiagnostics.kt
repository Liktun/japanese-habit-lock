package com.liktun.japanesehabitlock.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Records what the service actually saw on screen, so a failing rule can be diagnosed
 * from the user's own device.
 *
 * Surface detection depends on view ids internal to Instagram and YouTube, which cannot
 * be verified from a build machine — there is no logged-in Instagram there. When a rule
 * fails, the only source of truth is the user's phone.
 *
 * ## Why this is persisted rather than held in memory
 *
 * The diagnostic workflow is: turn recording on, LEAVE the app, use Instagram, come
 * back. Instagram is memory-hungry, so Android frequently kills this app's process while
 * the user is away. The accessibility service is then restarted by the system with fresh
 * static state — which meant the enabled flag silently reverted to false and every
 * captured id was lost, so the report was always empty. That is precisely the window the
 * tool exists to observe, so both the flag and the captured ids must survive process
 * death.
 *
 * Still narrow by construction:
 *  - ids only, never text, exactly as [ViewIdCollector] guarantees.
 *  - capped, so a long session cannot grow without bound.
 *  - off unless the user turns it on, and cleared when they turn it off.
 */
object SurfaceDiagnostics {

  private const val MAX_PACKAGES = 8
  private const val MAX_IDS_PER_PACKAGE = 300

  /**
   * Mirror of the persisted flag, for the hot path.
   *
   * [record] is called on every accessibility event, so it must not touch disk to decide
   * whether to do nothing. The store is the source of truth; this is the fast read.
   */
  @Volatile
  var enabled: Boolean = false
    private set

  /** In-memory cache of what has been captured, backed by [store]. */
  private val seen = ConcurrentHashMap<String, MutableSet<String>>()
  private val lastVerdict = ConcurrentHashMap<String, String>()

  /**
   * Where captures are persisted.
   *
   * An interface so this object keeps zero Android dependencies and stays unit-testable;
   * the real implementation is backed by DataStore.
   */
  interface Store {
    fun loadEnabled(): Boolean

    fun saveEnabled(value: Boolean)

    fun loadCaptures(): Map<String, Set<String>>

    fun saveCaptures(captures: Map<String, Set<String>>)

    fun loadVerdicts(): Map<String, String>

    fun saveVerdicts(verdicts: Map<String, String>)
  }

  @Volatile
  private var store: Store? = null

  /**
   * Attaches persistence and restores anything captured before the process died.
   *
   * Called from both the service and the UI, because either may be the first to start
   * after a restart.
   */
  fun attach(store: Store) {
    this.store = store
    enabled = store.loadEnabled()
    if (seen.isEmpty()) {
      store.loadCaptures().forEach { (pkg, ids) ->
        seen.getOrPut(pkg) { ConcurrentHashMap.newKeySet() }.addAll(ids)
      }
      lastVerdict.putAll(store.loadVerdicts())
    }
  }

  fun setEnabled(value: Boolean) {
    enabled = value
    store?.saveEnabled(value)
    if (!value) clear()
  }

  fun record(packageName: String, viewIds: Set<String>, verdict: String) {
    if (!enabled) return
    val verdictChanged = lastVerdict.put(packageName, verdict) != verdict
    var added = false
    if (viewIds.isNotEmpty() &&
      !(seen.size >= MAX_PACKAGES && !seen.containsKey(packageName))
    ) {
      val bucket = seen.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }
      if (bucket.size < MAX_IDS_PER_PACKAGE) {
        // "com.instagram.android:id/clips_viewer" -> "clips_viewer". The prefix is noise
        // once grouped by package, and the short form is what the rules match on.
        viewIds.forEach { id -> if (bucket.add(id.substringAfter(":id/"))) added = true }
      }
    }
    // Written only when something actually changed. Persisting on every event would mean
    // a DataStore write per window change, which would be a performance disaster on a
    // feed the user is actively scrolling.
    if (added || verdictChanged) flush()
  }

  private fun flush() {
    val s = store ?: return
    s.saveCaptures(seen.mapValues { it.value.toSet() })
    s.saveVerdicts(lastVerdict.toMap())
  }

  fun packages(): List<String> = seen.keys.sorted()

  fun idsFor(packageName: String): List<String> = seen[packageName].orEmpty().sorted()

  fun verdictFor(packageName: String): String? = lastVerdict[packageName]

  fun clear() {
    seen.clear()
    lastVerdict.clear()
    store?.let {
      it.saveCaptures(emptyMap())
      it.saveVerdicts(emptyMap())
    }
  }

  /**
   * Drops in-memory state WITHOUT touching the store, simulating process death.
   *
   * Exists so the persistence contract - the actual bug this class was rewritten for -
   * can be tested without an emulator.
   */
  internal fun clearInMemoryForTest() {
    seen.clear()
    lastVerdict.clear()
    enabled = false
    store = null
  }

  /** A shareable plain-text report, formatted for pasting into a message. */
  fun report(): String {
    if (seen.isEmpty()) {
      // A verdict with no ids anywhere is a specific, diagnosable failure rather than
      // "nothing happened": the service is receiving events and reading the tree, but
      // every getViewIdResourceName() came back null. That is what FLAG_REPORT_VIEW_IDS
      // being absent looks like from the outside, and saying so is far more useful than
      // a generic empty report.
      if (lastVerdict.isNotEmpty()) {
        return buildString {
          appendLine("Screens were seen, but NO view ids were readable.")
          appendLine()
          appendLine("Apps observed:")
          lastVerdict.keys.sorted().forEach { appendLine("  $it  -> ${lastVerdict[it]}") }
          appendLine()
          appendLine(
            "This means the accessibility service is running but Android is not " +
              "reporting element ids to it. Turning the service off and on again in " +
              "Android Settings > Accessibility usually fixes it after an update."
          )
        }
      }
      return if (enabled) {
        "Recording is ON, but nothing has been captured yet.\n\n" +
          "Leave this app, open Instagram, go to the Reels tab and scroll for a few " +
          "seconds, then come back here and tap Refresh."
      } else {
        "Recording is OFF.\n\nTurn it on above, then open the app you want to block."
      }
    }
    return buildString {
      appendLine("Surface diagnostics")
      appendLine("view ids only - no text was read")
      appendLine()
      packages().forEach { pkg ->
        appendLine("== $pkg")
        verdictFor(pkg)?.let { appendLine("last verdict: $it") }
        idsFor(pkg).forEach { appendLine("  $it") }
        appendLine()
      }
    }
  }
}
