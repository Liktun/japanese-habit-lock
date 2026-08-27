package com.liktun.japanesehabitlock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.apps.InstalledApp
import com.liktun.japanesehabitlock.data.apps.InstalledAppsRepository
import com.liktun.japanesehabitlock.service.HeartbeatStatus
import com.liktun.japanesehabitlock.service.ServiceHeartbeat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the settings screen: which installed apps exist, and which of them are blocked.
 *
 * The installed-app list is loaded exactly once (it cannot change while the screen is
 * open without the process being restarted anyway) and held in a [MutableStateFlow], then
 * combined with the persisted blocked set and the search box so a keystroke never
 * re-queries `PackageManager`.
 */
class SettingsViewModel(
  private val checklistRepository: ChecklistRepository,
  private val installedAppsRepository: InstalledAppsRepository,
  private val heartbeat: ServiceHeartbeat = ServiceHeartbeat(),
  private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val apps = MutableStateFlow<List<InstalledApp>?>(null)
  private val loadFailure = MutableStateFlow<Throwable?>(null)
  private val searchQuery = MutableStateFlow("")

  /**
   * Whether the OS reports our service as enabled.
   *
   * Pushed in from the UI layer on every resume rather than read here: the value lives in
   * Settings.Secure, which needs a Context, and the user changes it by leaving the app
   * entirely.
   */
  private val serviceEnabled = MutableStateFlow(false)

  fun setServiceEnabled(enabled: Boolean) {
    serviceEnabled.update { enabled }
  }

  init {
    viewModelScope.launch {
      runCatching { installedAppsRepository.launchableApps() }
        .onSuccess { loaded -> apps.update { loaded } }
        .onFailure { failure -> loadFailure.update { failure } }
    }
  }

  private val health: StateFlow<HeartbeatStatus> =
    combine(checklistRepository.lastServiceHeartbeat, serviceEnabled) { lastSeen, enabled ->
        heartbeat.status(lastSeen, now(), enabled)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeartbeatStatus.Disabled)

  /**
   * The two blocking rules as one value.
   *
   * `combine` only has typed overloads up to five flows and the chain below already uses
   * all five. Pairing the two persisted rule sets keeps the result strongly typed instead
   * of falling back to the vararg overload's `Array<Any?>` and casting.
   */
  private val blockRules: Flow<Pair<Set<String>, Set<String>>> =
    combine(checklistRepository.blockedPackages, checklistRepository.blockedSurfaces) {
      packages,
      surfaces ->
      packages to surfaces
    }

  val uiState: StateFlow<SettingsUiState> =
    combine(apps, loadFailure, blockRules, searchQuery, health) {
        loaded,
        failure,
        rules,
        query,
        status ->
        when {
          failure != null -> SettingsUiState.Error(failure)
          loaded == null -> SettingsUiState.Loading
          else ->
            SettingsUiState.Ready(
              apps = loaded,
              blockedPackages = rules.first,
              blockedSurfaces = rules.second,
              searchQuery = query,
              health = status,
            )
        }
      }
      .catch { emit(SettingsUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

  /** Adds or removes one package from the persisted blocked set. */
  fun setBlocked(packageName: String, blocked: Boolean) {
    viewModelScope.launch {
      val current = (uiState.value as? SettingsUiState.Ready)?.blockedPackages.orEmpty()
      val updated = if (blocked) current + packageName else current - packageName
      checklistRepository.setBlockedPackages(updated)
    }
  }

  /**
   * Adds or removes one surface id from the persisted blocked set.
   *
   * Deliberately independent of [setBlocked]: a surface rule that is currently superseded
   * by an app block is still kept, so unblocking the app restores the narrower rule the
   * user originally chose rather than silently discarding it.
   */
  fun setSurfaceBlocked(surfaceId: String, blocked: Boolean) {
    viewModelScope.launch {
      val current = (uiState.value as? SettingsUiState.Ready)?.blockedSurfaces.orEmpty()
      val updated = if (blocked) current + surfaceId else current - surfaceId
      checklistRepository.setBlockedSurfaces(updated)
    }
  }

  fun setSearchQuery(query: String) {
    searchQuery.update { query }
  }
}

sealed interface SettingsUiState {
  data object Loading : SettingsUiState

  data class Error(val throwable: Throwable) : SettingsUiState

  data class Ready(
    val apps: List<InstalledApp>,
    val blockedPackages: Set<String>,
    val searchQuery: String,
    val health: HeartbeatStatus = HeartbeatStatus.Disabled,
    val blockedSurfaces: Set<String> = emptySet(),
  ) : SettingsUiState {
    /** The rows the picker actually renders. See [visibleApps] for the ordering rationale. */
    val visibleApps: List<InstalledApp> = visibleApps(apps, blockedPackages, searchQuery)
  }
}

/**
 * Filters [apps] by [searchQuery] and floats the blocked ones to the top.
 *
 * Blocked-first ordering is deliberate: the list is hundreds of entries long, and the
 * question a returning user has is "what am I currently blocking?", not "what is
 * installed?". Putting the current selection where it is visible without scrolling also
 * makes accidental toggles obvious. Within each group the original label ordering from
 * the repository is preserved, so the list never reshuffles unpredictably.
 *
 * This is a pure top-level function so it can be unit-tested without a ViewModel,
 * a Looper, or a `PackageManager`.
 */
internal fun visibleApps(
  apps: List<InstalledApp>,
  blockedPackages: Set<String>,
  searchQuery: String,
): List<InstalledApp> {
  val query = searchQuery.trim()
  val matches =
    if (query.isEmpty()) {
      apps
    } else {
      apps.filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
      }
    }
  // sortedBy is stable, so equal keys keep the repository's case-insensitive label order.
  return matches.sortedBy { if (it.packageName in blockedPackages) 0 else 1 }
}
