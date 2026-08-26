package com.liktun.japanesehabitlock.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import com.liktun.japanesehabitlock.domain.weekNumberFor
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes the daily checklist state.
 *
 * The clock and time zone are injected rather than called directly so day-rollover
 * behaviour can be tested without waiting for midnight.
 *
 * Note this class takes a [DataStore] rather than a `Context`: that keeps it free of
 * Android dependencies, so unit tests can hand it a plain file-backed store.
 */
class ChecklistRepository(
  private val dataStore: DataStore<Preferences>,
  private val now: () -> Instant = { Instant.now() },
  private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {

  private object Keys {
    /** The study day the [COMPLETED] set belongs to. Used to auto-reset on a new day. */
    val DAY = stringPreferencesKey("current_day")
    val COMPLETED = stringSetPreferencesKey("completed_task_ids")
    val START_DATE = stringPreferencesKey("roadmap_start_date")
    val PHASE = stringPreferencesKey("active_phase")
    val UNLOCKED = booleanPreferencesKey("all_tasks_done")
    val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")

    /**
     * Wall-clock millis of the last time the accessibility service proved it was alive.
     *
     * Written by [HabitLockAccessibilityService] and read by the UI so it can tell a
     * service that is genuinely running from one an aggressive OEM has killed while
     * leaving its accessibility toggle switched on.
     */
    val LAST_SERVICE_HEARTBEAT = longPreferencesKey("last_service_heartbeat")
  }

  /** Today's checklist, with completions from a previous day already discarded. */
  val checklist: Flow<DailyChecklist> = dataStore.data.map { it.toChecklist(today()) }

  /**
   * Whether the distraction apps are currently open.
   *
   * This is the flow the Accessibility Service should observe. It is *derived*, so
   * it stays correct even when the day rolls over with no writes in between — unlike
   * the persisted [Keys.UNLOCKED] mirror, which only refreshes when something is written.
   */
  val isUnlocked: Flow<Boolean> = checklist.map { it.isUnlocked }

  /** Package names to block while [isUnlocked] is false. Empty until configured. */
  val blockedPackages: Flow<Set<String>> = dataStore.data.map { it[Keys.BLOCKED_PACKAGES].orEmpty() }

  /**
   * When the accessibility service last proved it was alive, or null if it never has.
   *
   * Written by the accessibility service and read by the UI to detect an OEM kill: on
   * Samsung/Xiaomi/Huawei/OPPO the system reaps our process without clearing the
   * accessibility toggle, so a switch that still reads "on" is not evidence that
   * blocking is happening. A timestamp that has stopped advancing is. Absent means the
   * service has never run, which is a different problem — see `ServiceHeartbeat`, which
   * turns this value into a status.
   */
  val lastServiceHeartbeat: Flow<Long?> = dataStore.data.map { it[Keys.LAST_SERVICE_HEARTBEAT] }

  /**
   * Records that the service was alive at [atMillis].
   *
   * Called from the accessibility service, which throttles it: this is a disk write, and
   * window-change events arrive far too often to persist one each time.
   */
  suspend fun recordServiceHeartbeat(atMillis: Long) {
    dataStore.edit { prefs -> prefs[Keys.LAST_SERVICE_HEARTBEAT] = atMillis }
  }

  /** Checks or unchecks one task for today. */
  suspend fun setTaskCompleted(taskId: String, completed: Boolean) {
    dataStore.edit { prefs ->
      val today = today()
      val current = prefs.completedFor(today)
      val updated = if (completed) current + taskId else current - taskId
      prefs.writeDay(today, updated)
    }
  }

  /** Opts in to a new phase, which changes which tasks appear on the checklist. */
  suspend fun setPhase(phase: Phase) {
    dataStore.edit { prefs ->
      val today = today()
      prefs[Keys.PHASE] = phase.name
      // The gate depends on which tasks are active, so recompute the mirror.
      prefs.writeDay(today, prefs.completedFor(today), phase)
    }
  }

  /** Replaces the blocked-package list the future service will enforce. */
  suspend fun setBlockedPackages(packages: Set<String>) {
    dataStore.edit { prefs -> prefs[Keys.BLOCKED_PACKAGES] = packages }
  }

  /** Clears today's completions. Mainly useful for testing the locked state. */
  suspend fun resetToday() {
    dataStore.edit { prefs -> prefs.writeDay(today(), emptySet()) }
  }

  private fun today(): StudyDay = StudyDay.at(now(), zone())

  /**
   * Today's completions, or an empty set when the stored set belongs to an earlier day.
   *
   * This is what makes the checklist reset each morning: nothing is erased on a
   * timer, the stale set is simply ignored and overwritten on the next write.
   */
  private fun Preferences.completedFor(today: StudyDay): Set<String> =
    if (StudyDay.parseOrNull(this[Keys.DAY]) == today) this[Keys.COMPLETED].orEmpty() else emptySet()

  private fun Preferences.phase(): Phase =
    Phase.entries.firstOrNull { it.name == this[Keys.PHASE] } ?: Phase.SHADOWING

  /** Writes the day, its completions, and the derived gate mirror as one unit. */
  private fun androidx.datastore.preferences.core.MutablePreferences.writeDay(
    today: StudyDay,
    completed: Set<String>,
    phase: Phase = phase(),
  ) {
    this[Keys.DAY] = today.key
    this[Keys.COMPLETED] = completed
    this[Keys.UNLOCKED] = Roadmap.isUnlocked(phase, completed)
    // Anchor the roadmap on the first write so week 1 starts when the user does.
    if (this[Keys.START_DATE] == null) this[Keys.START_DATE] = today.key
  }

  private fun Preferences.toChecklist(today: StudyDay): DailyChecklist {
    val phase = phase()
    val start = StudyDay.parseOrNull(this[Keys.START_DATE]) ?: today
    return DailyChecklist(
      day = today,
      weekNumber = weekNumberFor(start, today),
      phase = phase,
      tasks = Roadmap.tasksFor(phase),
      completedTaskIds = completedFor(today),
    )
  }
}
