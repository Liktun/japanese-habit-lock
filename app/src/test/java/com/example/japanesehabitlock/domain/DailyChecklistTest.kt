package com.example.japanesehabitlock.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChecklistTest {

  private fun checklist(phase: Phase = Phase.SHADOWING, completed: Set<String> = emptySet()) =
    DailyChecklist(
      day = StudyDay(LocalDate.of(2026, 8, 24)),
      weekNumber = 3,
      phase = phase,
      tasks = Roadmap.tasksFor(phase),
      completedTaskIds = completed,
    )

  @Test
  fun `a fresh day is locked with no progress`() {
    val today = checklist()
    assertFalse(today.isUnlocked)
    assertEquals(0, today.blockingDone)
    assertEquals(3, today.blockingTotal)
    assertEquals(0f, today.progress, 0.0001f)
  }

  @Test
  fun `progress tracks only the blocking tasks`() {
    val today = checklist(completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_IMMERSION))
    // Immersion is optional, so it must not move the bar.
    assertEquals(1, today.blockingDone)
    assertEquals(1f / 3f, today.progress, 0.0001f)
  }

  @Test
  fun `finishing every blocking task unlocks the day`() {
    val today =
      checklist(completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING))
    assertTrue(today.isUnlocked)
    assertEquals(1f, today.progress, 0.0001f)
  }

  @Test
  fun `optional tasks are listed separately from blocking ones`() {
    val today = checklist()
    assertEquals(listOf(Roadmap.ID_IMMERSION), today.optionalTasks.map { it.id })
    assertEquals(
      listOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
      today.blockingTasks.map { it.id },
    )
  }

  @Test
  fun `isDone reflects the completed set`() {
    val today = checklist(completed = setOf(Roadmap.ID_BUNPRO))
    assertTrue(today.isDone(today.tasks.single { it.id == Roadmap.ID_BUNPRO }))
    assertFalse(today.isDone(today.tasks.single { it.id == Roadmap.ID_WANIKANI }))
  }

  @Test
  fun `unknown ids left over from an older roadmap are ignored`() {
    val today = checklist(completed = setOf("a_task_that_no_longer_exists"))
    assertEquals(0, today.blockingDone)
    assertFalse(today.isUnlocked)
  }

  @Test
  fun `week focus and checkpoints are surfaced`() {
    val today = checklist()
    assertEquals(Roadmap.focusFor(3), today.focus)
    assertEquals(Roadmap.WEEKLY_CHECKPOINTS, today.checkpoints)
  }
}
