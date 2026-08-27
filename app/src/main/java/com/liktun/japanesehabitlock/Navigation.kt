package com.liktun.japanesehabitlock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.service.AccessibilityServiceStatus
import com.liktun.japanesehabitlock.ui.checklist.ThemedChecklistScreen
import com.liktun.japanesehabitlock.ui.settings.SettingsScreen
import com.liktun.japanesehabitlock.ui.styles.ThemeRegistry
import com.liktun.japanesehabitlock.ui.theme.ThemePickerScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val repository = remember(context) { ChecklistRepository(context.applicationContext.habitLockDataStore) }
  // Resolved rather than parsed: an unknown stored name (a theme removed in a later
  // build) degrades to the default instead of crashing on a failed valueOf.
  val styleFlow = remember(repository) { repository.themeName.map { ThemeRegistry.resolve(it) } }
  val style by styleFlow.collectAsStateWithLifecycle(initialValue = ThemeRegistry.DEFAULT)

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

  // The theme's ground colour is painted behind everything, including under the system
  // bars, so switching themes never leaves a mismatched band at the top of the screen.
  Box(Modifier.fillMaxSize().background(ThemeRegistry.backdrop(style))) {
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            // The settings affordance lives here rather than inside each theme: six
            // designs would otherwise each need their own, and the first device run
            // proved the point - every themed screen shipped without one, leaving the
            // theme picker unreachable.
            Box(Modifier.fillMaxSize()) {
              ThemedChecklistScreen(style = style, modifier = Modifier.safeDrawingPadding())
              SettingsFab(
                dark = ThemeRegistry.isDark(style),
                onClick = { backStack.add(Settings) },
                modifier =
                  Modifier.align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 18.dp, bottom = 18.dp),
              )
            }
          }
          entry<Settings> {
            SettingsScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              serviceEnabled = serviceEnabled,
              onOpenAccessibilitySettings = { AccessibilityServiceStatus.openSettings(context) },
              onOpenThemePicker = { backStack.add(ThemePicker) },
              modifier = Modifier.safeDrawingPadding().padding(16.dp),
            )
          }
          entry<ThemePicker> {
            ThemePickerScreen(
              selected = style,
              onSelect = { chosen -> scope.launch { repository.setThemeName(chosen.name) } },
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp),
            )
          }
        },
    )
  }
}

/**
 * A small, theme-agnostic way into settings.
 *
 * Deliberately neutral rather than styled per theme: it is chrome, not content, and six
 * bespoke versions would be six things to keep in sync. It only needs to know whether it
 * is sitting on a light or dark ground so it stays visible on both.
 */
@Composable
private fun SettingsFab(dark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val bg = if (dark) Color(0x33FFFFFF) else Color(0x1A000000)
  val fg = if (dark) Color(0xE6FFFFFF) else Color(0xCC000000)
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
  ) {
    Text(text = "⚙", color = fg, fontSize = 20.sp)
  }
}
