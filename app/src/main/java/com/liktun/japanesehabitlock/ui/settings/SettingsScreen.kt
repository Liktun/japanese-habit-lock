package com.liktun.japanesehabitlock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.apps.InstalledApp
import com.liktun.japanesehabitlock.data.apps.InstalledAppsRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.service.AccessibilityServiceStatus
import com.liktun.japanesehabitlock.service.HeartbeatStatus
import com.liktun.japanesehabitlock.service.OemBatteryGuidance
import com.liktun.japanesehabitlock.service.OemGuidance
import com.liktun.japanesehabitlock.service.ServiceHealthLauncher
import com.liktun.japanesehabitlock.theme.JapaneseHabitLockTheme

/**
 * Settings: turn blocking on, and pick which apps it applies to.
 *
 * [serviceEnabled] and [onOpenAccessibilitySettings] are parameters rather than being read
 * from system settings here, so this screen stays a pure function of its inputs and the
 * hosting layer owns the platform lookup.
 */
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  serviceEnabled: Boolean = false,
  onOpenAccessibilitySettings: () -> Unit = {},
) {
  // applicationContext, not the Activity: the DataStore outlives this screen.
  val appContext = LocalContext.current.applicationContext
  val viewModel: SettingsViewModel = viewModel {
    SettingsViewModel(
      checklistRepository = ChecklistRepository(appContext.habitLockDataStore),
      installedAppsRepository =
        InstalledAppsRepository(
          packageManager = appContext.packageManager,
          selfPackage = appContext.packageName,
          hiddenPackages = Roadmap.STUDY_TOOL_PACKAGES,
        ),
    )
  }
  // The user flips the accessibility toggle outside the app, so the freshest value is
  // whatever the caller observed on resume. Push it down rather than reading it here.
  LaunchedEffect(serviceEnabled) { viewModel.setServiceEnabled(serviceEnabled) }

  val guidance = remember { OemBatteryGuidance.forManufacturer(ServiceHealthLauncher.currentManufacturer()) }
  val openBatterySettings: () -> Unit = {
    ServiceHealthLauncher.openGuidanceSettings(appContext, guidance)
  }

  val state by viewModel.uiState.collectAsStateWithLifecycle()

  when (val current = state) {
    SettingsUiState.Loading ->
      SettingsContent(
        apps = emptyList(),
        blockedPackages = emptySet(),
        searchQuery = "",
        serviceEnabled = serviceEnabled,
        onSearchQueryChange = {},
        onToggleBlocked = { _, _ -> },
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onNavigateBack = onNavigateBack,
        loading = true,
        modifier = modifier,
      )
    is SettingsUiState.Error ->
      Text(
        text = "Could not load your apps: ${current.throwable.message}",
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
      )
    is SettingsUiState.Ready ->
      SettingsContent(
        apps = current.visibleApps,
        blockedPackages = current.blockedPackages,
        searchQuery = current.searchQuery,
        serviceEnabled = serviceEnabled,
        onSearchQueryChange = viewModel::setSearchQuery,
        onToggleBlocked = viewModel::setBlocked,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onNavigateBack = onNavigateBack,
        health = current.health,
        guidance = guidance,
        onOpenBatterySettings = openBatterySettings,
        modifier = modifier,
      )
  }
}

/**
 * The whole screen as plain state plus callbacks, with no ViewModel and no Android
 * services, so previews and UI tests can drive it directly.
 *
 * [apps] is already filtered and ordered by the caller.
 */
@Composable
internal fun SettingsContent(
  apps: List<InstalledApp>,
  blockedPackages: Set<String>,
  searchQuery: String,
  serviceEnabled: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onToggleBlocked: (String, Boolean) -> Unit,
  onOpenAccessibilitySettings: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  loading: Boolean = false,
  health: HeartbeatStatus = HeartbeatStatus.Disabled,
  guidance: OemGuidance? = null,
  onOpenBatterySettings: () -> Unit = {},
) {
  LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Header(onNavigateBack = onNavigateBack) }

    item {
      ServiceStatusCard(
        serviceEnabled = serviceEnabled,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
      )
    }

    // Only appears when the toggle says "on" but the service has gone quiet or never
    // ran — the OEM-kill case the ordinary status card cannot detect.
    if (guidance != null) {
      item { ServiceHealthWarning(health, guidance, onOpenBatterySettings) }
    }

    item { SectionLabel("Blocked apps") }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        singleLine = true,
        placeholder = { Text("Search apps") },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    if (apps.isEmpty()) {
      item { EmptyState(loading = loading, searchQuery = searchQuery) }
    } else {
      items(items = apps, key = { it.packageName }) { app ->
        AppRow(
          app = app,
          blocked = app.packageName in blockedPackages,
          onToggle = { onToggleBlocked(app.packageName, it) },
        )
      }
    }

    item { Spacer(Modifier.height(24.dp)) }
  }
}

@Composable
private fun Header(onNavigateBack: () -> Unit) {
  Column {
    TextButton(onClick = onNavigateBack, contentPadding = PaddingValues(0.dp)) { Text("← Back") }
    Text(
      text = "Settings",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = "Choose the apps that stay locked until today's study is done.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * Blocking status plus the prominent disclosure for the accessibility service.
 *
 * The disclosure text is a Google Play policy requirement for accessibility-API use, so it
 * states plainly and honestly what the service can see: the package name of whatever app
 * is in the foreground, and nothing else. Do not soften or shorten it without checking the
 * policy again.
 */
@Composable
internal fun ServiceStatusCard(serviceEnabled: Boolean, onOpenAccessibilitySettings: () -> Unit) {
  val container =
    if (serviceEnabled) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
  val onContainer =
    if (serviceEnabled) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer

  Surface(color = container, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = if (serviceEnabled) "Blocking is active" else "Blocking is INACTIVE",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = onContainer,
      )
      Spacer(Modifier.height(6.dp))
      if (serviceEnabled) {
        Text(
          text =
            "The accessibility service is on. The apps you check below stay closed until " +
              "today's required tasks are done. It still only reads the package name of the " +
              "app in the foreground — never the contents of your screen.",
          style = MaterialTheme.typography.bodyMedium,
          color = onContainer,
        )
      } else {
        Text(
          text = "Nothing is being blocked right now. Your selections below have no effect until you turn it on.",
          style = MaterialTheme.typography.bodyMedium,
          color = onContainer,
        )
        Spacer(Modifier.height(10.dp))
        Text(
          text = "What turning this on does",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = onContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            "Japanese Habit Lock uses Android's accessibility service to notice when an app " +
              "you chose comes to the foreground, and shows a blocking screen over it while " +
              "today's study is unfinished.\n\n" +
              "It reads only one thing: the package name of the app currently in the " +
              "foreground — for example \"com.instagram.android\". It does not read the text " +
              "on your screen, your messages, your passwords, or anything you type. Nothing " +
              "is sent off your device, and nothing is stored beyond the list of apps you " +
              "pick here and today's checklist.\n\n" +
              "You can turn this off at any time in Android Settings › Accessibility.",
          style = MaterialTheme.typography.bodySmall,
          color = onContainer,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenAccessibilitySettings) { Text("Enable blocking") }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.sp,
  )
}

@Composable
private fun AppRow(app: InstalledApp, blocked: Boolean, onToggle: (Boolean) -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (blocked) 1f else 0.5f),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.clickable { onToggle(!blocked) }.padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
      Checkbox(checked = blocked, onCheckedChange = onToggle)
      Spacer(Modifier.width(4.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = app.label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = app.packageName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EmptyState(loading: Boolean, searchQuery: String) {
  Text(
    text =
      when {
        loading -> "Reading your installed apps…"
        searchQuery.isBlank() -> "No launchable apps found on this device."
        else -> "No apps match \"$searchQuery\"."
      },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(vertical = 24.dp),
  )
}

// ---------------------------------------------------------------- previews

private val SAMPLE_APPS =
  listOf(
    InstalledApp("com.instagram.android", "Instagram"),
    InstalledApp("com.reddit.frontpage", "Reddit"),
    InstalledApp("com.google.android.youtube", "YouTube"),
  )

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsServiceDisabledPreview() {
  JapaneseHabitLockTheme {
    SettingsContent(
      apps = SAMPLE_APPS,
      blockedPackages = emptySet(),
      searchQuery = "",
      serviceEnabled = false,
      onSearchQueryChange = {},
      onToggleBlocked = { _, _ -> },
      onOpenAccessibilitySettings = {},
      onNavigateBack = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsServiceEnabledPreview() {
  JapaneseHabitLockTheme {
    SettingsContent(
      apps = SAMPLE_APPS,
      blockedPackages = setOf("com.instagram.android"),
      searchQuery = "",
      serviceEnabled = true,
      onSearchQueryChange = {},
      onToggleBlocked = { _, _ -> },
      onOpenAccessibilitySettings = {},
      onNavigateBack = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}
