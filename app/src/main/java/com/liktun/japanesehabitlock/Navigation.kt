package com.liktun.japanesehabitlock

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.liktun.japanesehabitlock.service.AccessibilityServiceStatus
import com.liktun.japanesehabitlock.ui.checklist.ChecklistScreen
import com.liktun.japanesehabitlock.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current

  // Re-read on every resume: the user leaves the app entirely to flip the system toggle,
  // so the only reliable moment to refresh this is when we come back to the foreground.
  var serviceEnabled by remember { mutableStateOf(AccessibilityServiceStatus.isEnabled(context)) }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        serviceEnabled = AccessibilityServiceStatus.isEnabled(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          ChecklistScreen(
            serviceEnabled = serviceEnabled,
            onOpenSettings = { backStack.add(Settings) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
          )
        }
        entry<Settings> {
          SettingsScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            serviceEnabled = serviceEnabled,
            onOpenAccessibilitySettings = { AccessibilityServiceStatus.openSettings(context) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
          )
        }
      },
  )
}
