package com.liktun.japanesehabitlock.service

import android.content.Context

/**
 * Persists diagnostics state so it survives the app's process being killed.
 *
 * SharedPreferences rather than the DataStore used everywhere else in this app, for one
 * specific reason: the accessibility service needs the enabled flag *synchronously* the
 * moment it binds, before any coroutine could collect a Flow. DataStore has no blocking
 * read, and a service that starts with `enabled = false` for even a second misses exactly
 * the events the user turned recording on to capture.
 *
 * This is diagnostic scratch data - view ids the user is about to share - not app state,
 * so it does not belong in the main DataStore anyway.
 */
class PrefsDiagnosticsStore(context: Context) : SurfaceDiagnostics.Store {

  private val prefs =
    context.applicationContext.getSharedPreferences("surface_diagnostics", Context.MODE_PRIVATE)

  override fun loadEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

  override fun saveEnabled(value: Boolean) {
    // commit(), not apply(): the very next thing that happens after the user flips this
    // switch is that they leave the app, and the process may be killed before an async
    // write lands.
    prefs.edit().putBoolean(KEY_ENABLED, value).commit()
  }

  override fun loadCaptures(): Map<String, Set<String>> {
    val packages = prefs.getStringSet(KEY_PACKAGES, emptySet()).orEmpty()
    return packages.associateWith { pkg -> prefs.getStringSet(idsKey(pkg), emptySet()).orEmpty() }
  }

  override fun saveCaptures(captures: Map<String, Set<String>>) {
    val editor = prefs.edit()
    // Clear stale buckets first, or a package removed from the map would linger forever.
    prefs.getStringSet(KEY_PACKAGES, emptySet()).orEmpty().forEach { editor.remove(idsKey(it)) }
    editor.putStringSet(KEY_PACKAGES, captures.keys)
    captures.forEach { (pkg, ids) -> editor.putStringSet(idsKey(pkg), ids) }
    editor.apply()
  }

  override fun loadVerdicts(): Map<String, String> {
    val packages = prefs.getStringSet(KEY_PACKAGES, emptySet()).orEmpty()
    return packages.mapNotNull { pkg -> prefs.getString(verdictKey(pkg), null)?.let { pkg to it } }
      .toMap()
  }

  override fun saveVerdicts(verdicts: Map<String, String>) {
    val editor = prefs.edit()
    verdicts.forEach { (pkg, verdict) -> editor.putString(verdictKey(pkg), verdict) }
    editor.apply()
  }

  private companion object {
    const val KEY_ENABLED = "enabled"
    const val KEY_PACKAGES = "packages"

    fun idsKey(pkg: String) = "ids_$pkg"

    fun verdictKey(pkg: String) = "verdict_$pkg"
  }
}
