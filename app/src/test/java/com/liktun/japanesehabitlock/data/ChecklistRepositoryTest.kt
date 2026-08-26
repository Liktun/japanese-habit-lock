package com.liktun.japanesehabitlock.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory stand-in for a real DataStore. Keeps these tests pure JVM — no files, no Android. */
private class FakePreferencesDataStore : DataStore<Preferences> {
  private val state = MutableStateFlow(emptyPreferences())
  private val mutex = Mutex()

  override val data: Flow<Preferences> = state.asStateFlow()

  override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
    mutex.withLock {
      val updated = transform(state.value)
      state.value = updated
      updated
    }
}

class ChecklistRepositoryTest {

  private val store = FakePreferencesDataStore()
  private var clock: Instant = Instant.parse("2026-08-24T10:00:00Z")
  private val repo =
    ChecklistRepository(store, now = { clock }, zone = { ZoneId.of("UTC") })

  private val allBlocking =
    listOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING)

  private suspend fun mirrorFlag(): Boolean? =
    store.data.first()[booleanPreferencesKey("all_tasks_done")]

  private suspend fun completeAllBlocking() =
    allBlocking.forEach { repo.setTaskCompleted(it, true) }

  @Test
  fun `a brand new install starts on week 1, locked, with nothing done`() = runTest {
    val today = repo.checklist.first()
    assertEquals(1, today.weekNumber)
    assertEquals(Phase.SHADOWING, today.phase)
    assertTrue(today.completedTaskIds.isEmpty())
    assertFalse(today.isUnlocked)
    assertFalse(repo.isUnlocked.first())
  }

  @Test
  fun `checking and unchecking a task round trips`() = runTest {
    repo.setTaskCompleted(Roadmap.ID_WANIKANI, true)
    assertEquals(setOf(Roadmap.ID_WANIKANI), repo.checklist.first().completedTaskIds)

    repo.setTaskCompleted(Roadmap.ID_WANIKANI, false)
    assertTrue(repo.checklist.first().completedTaskIds.isEmpty())
  }

  @Test
  fun `clearing every blocking task opens the gate and updates the mirror flag`() = runTest {
    assertNull(mirrorFlag())
    completeAllBlocking()

    assertTrue(repo.checklist.first().isUnlocked)
    assertTrue(repo.isUnlocked.first())
    // The mirror is what a future Accessibility Service reads cheaply.
    assertEquals(true, mirrorFlag())
  }

  @Test
  fun `the optional immersion task never opens the gate`() = runTest {
    repo.setTaskCompleted(Roadmap.ID_IMMERSION, true)

    assertFalse(repo.isUnlocked.first())
    assertEquals(false, mirrorFlag())
  }

  @Test
  fun `unchecking a task after unlocking closes the gate again`() = runTest {
    completeAllBlocking()
    repo.setTaskCompleted(Roadmap.ID_BUNPRO, false)

    assertFalse(repo.isUnlocked.first())
    assertEquals(false, mirrorFlag())
  }

  @Test
  fun `a late night session before 4am keeps the same day's progress`() = runTest {
    completeAllBlocking()

    // 03:00 the next calendar morning — still the same study day.
    clock = Instant.parse("2026-08-25T03:00:00Z")

    val stillToday = repo.checklist.first()
    assertEquals("2026-08-24", stillToday.day.key)
    assertEquals(allBlocking.toSet(), stillToday.completedTaskIds)
    assertTrue(stillToday.isUnlocked)
  }

  @Test
  fun `a new study day discards yesterday's completions and re-locks`() = runTest {
    completeAllBlocking()

    clock = Instant.parse("2026-08-25T10:00:00Z")

    val tomorrow = repo.checklist.first()
    assertEquals("2026-08-25", tomorrow.day.key)
    assertTrue(tomorrow.completedTaskIds.isEmpty())
    assertFalse(tomorrow.isUnlocked)
    assertFalse(repo.isUnlocked.first())
  }

  @Test
  fun `the persisted mirror flag is stale after rollover until the next write`() = runTest {
    completeAllBlocking()
    clock = Instant.parse("2026-08-25T10:00:00Z")

    // Known limitation, asserted so it can't change silently: the stored boolean still
    // reads true on the new day, which is why the derived flow is the source of truth.
    assertEquals(true, mirrorFlag())
    assertFalse(repo.isUnlocked.first())

    // Any write on the new day corrects it.
    repo.setTaskCompleted(Roadmap.ID_WANIKANI, true)
    assertEquals(false, mirrorFlag())
  }

  @Test
  fun `the roadmap is anchored on the first write, not the install date`() = runTest {
    repo.setTaskCompleted(Roadmap.ID_WANIKANI, true)

    clock = Instant.parse("2026-08-30T10:00:00Z") // 6 days later
    assertEquals(1, repo.checklist.first().weekNumber)

    clock = Instant.parse("2026-08-31T10:00:00Z") // 7 days later
    assertEquals(2, repo.checklist.first().weekNumber)

    clock = Instant.parse("2026-09-21T10:00:00Z") // 28 days later
    assertEquals(5, repo.checklist.first().weekNumber)
  }

  @Test
  fun `opting in to self-talk adds a task and re-locks an already finished day`() = runTest {
    completeAllBlocking()
    assertTrue(repo.isUnlocked.first())

    repo.setPhase(Phase.SELF_TALK)

    val today = repo.checklist.first()
    assertEquals(Phase.SELF_TALK, today.phase)
    assertEquals(4, today.blockingTotal)
    assertTrue(today.tasks.any { it.id == Roadmap.ID_SELF_TALK })
    // The extra blocking task means the day is no longer complete.
    assertFalse(today.isUnlocked)
    assertEquals(false, mirrorFlag())

    repo.setTaskCompleted(Roadmap.ID_SELF_TALK, true)
    assertTrue(repo.isUnlocked.first())
  }

  @Test
  fun `changing phase preserves today's completions`() = runTest {
    repo.setTaskCompleted(Roadmap.ID_WANIKANI, true)
    repo.setPhase(Phase.SELF_TALK)

    assertEquals(setOf(Roadmap.ID_WANIKANI), repo.checklist.first().completedTaskIds)
  }

  @Test
  fun `the blocked package list round trips`() = runTest {
    assertTrue(repo.blockedPackages.first().isEmpty())

    val packages = setOf("com.instagram.android", "com.zhiliaoapp.musically")
    repo.setBlockedPackages(packages)

    assertEquals(packages, repo.blockedPackages.first())
  }

  @Test
  fun `resetToday clears progress and re-locks`() = runTest {
    completeAllBlocking()
    repo.resetToday()

    assertTrue(repo.checklist.first().completedTaskIds.isEmpty())
    assertFalse(repo.isUnlocked.first())
    assertEquals(false, mirrorFlag())
  }
}
