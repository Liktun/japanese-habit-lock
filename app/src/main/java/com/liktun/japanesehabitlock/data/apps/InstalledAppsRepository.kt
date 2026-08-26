package com.liktun.japanesehabitlock.data.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the apps the user can actually launch, so the picker only offers real choices.
 *
 * [selfPackage] is excluded on purpose: this app is the thing enforcing the block, so
 * letting the user add it to the blocked list would make the checklist — the only way to
 * unlock again — unreachable. Excluding it at the source is simpler and safer than
 * filtering it in the UI, where a future screen could forget to.
 *
 * The [PackageManager] and the self package name are constructor parameters rather than
 * being pulled from a `Context` so this class stays trivially fake-able in tests.
 */
class InstalledAppsRepository(
  private val packageManager: PackageManager,
  private val selfPackage: String,
) {

  /**
   * Every launchable app except this one, de-duplicated by package and sorted by label.
   *
   * Runs on [Dispatchers.IO]: `queryIntentActivities` walks every installed package and
   * loads a label for each, which is far too slow for the main thread even though it is
   * called from a ViewModel.
   */
  suspend fun launchableApps(): List<InstalledApp> =
    withContext(Dispatchers.IO) {
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      queryLaunchers(intent)
        .asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != selfPackage }
        // An app may expose several launcher activities; the user picks a package, not one.
        .distinctBy { it.packageName }
        .map { InstalledApp(packageName = it.packageName, label = it.loadLabel(packageManager).toString()) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
    }

  private fun queryLaunchers(intent: Intent): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
      legacyQueryLaunchers(intent)
    }

  /** minSdk is 26, so the pre-API-33 overload is still required; the warning is expected here. */
  @Suppress("DEPRECATION")
  private fun legacyQueryLaunchers(intent: Intent): List<ResolveInfo> =
    packageManager.queryIntentActivities(intent, 0)
}
