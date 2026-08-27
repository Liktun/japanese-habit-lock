package com.liktun.japanesehabitlock.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Collects the view *ids* on screen — and nothing else.
 *
 * ## Why this file is written so defensively
 *
 * Surface blocking requires `canRetrieveWindowContent="true"`, which means the service
 * is technically able to read the text of everything on screen, including messages.
 * This app must never do that, so the read path is deliberately narrow and kept in one
 * small file that can be audited at a glance:
 *
 *  - Only [AccessibilityNodeInfo.getViewIdResourceName] is ever touched. `text`,
 *    `contentDescription` and `hintText` are never read, anywhere.
 *  - Nothing is stored. The returned set lives for one event and is discarded.
 *  - Nothing leaves the device. There is no network permission in the manifest.
 *
 * If a future change makes this file read `text`, that is a privacy regression and the
 * disclosure in settings becomes a lie.
 */
object ViewIdCollector {

  /**
   * Depth cap. Instagram's tree is deep and this runs on the main thread for every
   * window event, so an unbounded walk would jank the UI of whatever app is in front.
   * Surface-identifying containers sit near the top; 12 is comfortably past them.
   */
  private const val MAX_DEPTH = 12

  /** Node cap, for the same reason: a runaway tree must not stall the foreground app. */
  private const val MAX_NODES = 400

  /**
   * Walks [root] and returns every non-null view id resource name found.
   *
   * Returns an empty set on a null root, which the monitor treats as "cannot identify
   * this screen" and therefore allows — failing open rather than blocking something we
   * could not actually see.
   */
  fun collect(root: AccessibilityNodeInfo?): Set<String> {
    if (root == null) return emptySet()
    val ids = HashSet<String>(64)
    var visited = 0

    // Explicit stack rather than recursion: the tree is attacker-shaped in the sense
    // that another app controls its depth, and a StackOverflowError here would take
    // down the service and silently stop all blocking.
    val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
    stack.addLast(root to 0)

    while (stack.isNotEmpty() && visited < MAX_NODES) {
      val (node, depth) = stack.removeLast()
      visited++

      // The ONLY property read from the node.
      node.viewIdResourceName?.let(ids::add)

      if (depth < MAX_DEPTH) {
        for (i in 0 until node.childCount) {
          val child = try { node.getChild(i) } catch (_: Exception) { null }
          if (child != null) stack.addLast(child to depth + 1)
        }
      }
    }
    return ids
  }
}
