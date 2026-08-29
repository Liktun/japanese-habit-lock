package com.liktun.japanesehabitlock.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Records what the service actually saw on screen, so a failing rule can be diagnosed
 * from the user's own device.
 *
 * Surface detection depends on view ids that are internal to Instagram and YouTube and
 * cannot be verified anywhere except a real, logged-in install. Shipping a guess and
 * asking "did it work?" wastes a release cycle per attempt; this instead lets the phone
 * report the ids it really has.
 *
 * Deliberately small and bounded:
 *  - ids only, never text, exactly as [ViewIdCollector] already guarantees.
 *  - in memory only, cleared when the service restarts.
 *  - capped, so a long session cannot grow without limit.
 *  - off unless the user turns it on in settings.
 */
object SurfaceDiagnostics {

  /** Off by default: this exists for debugging a broken rule, not for normal running. */
  @Volatile
  var enabled: Boolean = false

  private const val MAX_PACKAGES = 8
  private const val MAX_IDS_PER_PACKAGE = 300

  private val seen = ConcurrentHashMap<String, MutableSet<String>>()

  /** The last verdict reached per package, for showing why nothing was blocked. */
  private val lastVerdict = ConcurrentHashMap<String, String>()

  fun record(packageName: String, viewIds: Set<String>, verdict: String) {
    if (!enabled) return
    lastVerdict[packageName] = verdict
    if (viewIds.isEmpty()) return
    if (seen.size >= MAX_PACKAGES && !seen.containsKey(packageName)) return
    val bucket = seen.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }
    if (bucket.size >= MAX_IDS_PER_PACKAGE) return
    // Strip the package prefix: "com.instagram.android:id/clips_viewer" -> "clips_viewer".
    // The prefix is noise once grouped by package, and the short form is what the rules
    // actually match on.
    viewIds.forEach { id -> bucket.add(id.substringAfter(":id/")) }
  }

  fun packages(): List<String> = seen.keys.sorted()

  fun idsFor(packageName: String): List<String> = seen[packageName].orEmpty().sorted()

  fun verdictFor(packageName: String): String? = lastVerdict[packageName]

  fun clear() {
    seen.clear()
    lastVerdict.clear()
  }

  /**
   * A shareable plain-text report.
   *
   * Formatted for pasting into a message, because that is how it gets back to whoever
   * has to fix the rule.
   */
  fun report(): String {
    if (seen.isEmpty()) {
      return "No screens recorded yet.\n\nTurn this on, open the app you want to block " +
        "(for example Instagram, then the Reels tab), then come back here."
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
