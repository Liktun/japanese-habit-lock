package com.liktun.japanesehabitlock.domain

/**
 * Picks how to open a task: an installed app if there is one, otherwise the web.
 *
 * Kept free of Android types so the choice is unit-testable — the caller passes in the
 * set of packages it found installed, rather than this class querying PackageManager.
 */
object TaskLaunchResolver {

  /** What the UI should do when a task row's "open" affordance is tapped. */
  sealed interface Target {
    /** Launch this installed app. */
    data class App(val packageName: String) : Target

    /** No candidate app is installed; open this URL instead. */
    data class Web(val url: String) : Target
  }

  /**
   * Resolves [launch] against [installedPackages].
   *
   * Candidates are tried in declared order, so a maintained client wins over an
   * abandoned fork when both happen to be installed. Returns null when the task has
   * nowhere to go — a shadowing session is done in whatever media player the user
   * already had open, so forcing a destination on it would be worse than offering none.
   */
  fun resolve(launch: TaskLaunch?, installedPackages: Set<String>): Target? {
    if (launch == null) return null
    val installed = launch.packageCandidates.firstOrNull { it in installedPackages }
    return if (installed != null) Target.App(installed) else Target.Web(launch.webUrl)
  }
}
