package com.liktun.japanesehabitlock.ui.checklist

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.liktun.japanesehabitlock.domain.TaskLaunch
import com.liktun.japanesehabitlock.domain.TaskLaunchResolver

/**
 * Opens the app or web page behind a checklist task.
 *
 * The Android half of [TaskLaunchResolver]: it supplies the installed-package set and
 * fires the Intent, while every decision stays in the pure resolver.
 */
object TaskOpener {

  /**
   * Opens whatever [launch] points at, or returns false when there was nothing to open.
   *
   * Every branch is wrapped: a package can be uninstalled between the query and the
   * launch, and a device with no browser will reject the web fallback. Neither is worth
   * crashing over when the user only tapped a convenience shortcut.
   */
  fun open(context: Context, launch: TaskLaunch?): Boolean {
    val installed = launch?.packageCandidates.orEmpty().filterTo(mutableSetOf()) { isInstalled(context, it) }
    return when (val target = TaskLaunchResolver.resolve(launch, installed)) {
      null -> false
      is TaskLaunchResolver.Target.App -> launchApp(context, target.packageName, launch?.webUrl)
      is TaskLaunchResolver.Target.Web -> openWeb(context, target.url)
    }
  }

  private fun isInstalled(context: Context, packageName: String): Boolean =
    runCatching { context.packageManager.getLaunchIntentForPackage(packageName) != null }
      .getOrDefault(false)

  private fun launchApp(context: Context, packageName: String, webFallback: String?): Boolean {
    val intent = runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      runCatching { context.startActivity(intent) }.onSuccess { return true }
    }
    // The app vanished between the check and the launch; fall back rather than fail.
    return webFallback?.let { openWeb(context, it) } ?: false
  }

  private fun openWeb(context: Context, url: String): Boolean =
    runCatching {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
      true
    }
      .getOrDefault(false)
}
