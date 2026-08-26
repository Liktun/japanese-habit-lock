package com.liktun.japanesehabitlock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadmapTest {

  @Test
  fun `task ids are unique because they are persisted`() {
    val ids = Roadmap.ALL_TASKS.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun `phase 1 shows the five shadowing-era tasks`() {
    val tasks = Roadmap.tasksFor(Phase.SHADOWING)
    assertEquals(
      listOf(
        Roadmap.ID_WANIKANI,
        Roadmap.ID_BUNPRO,
        Roadmap.ID_SHADOWING,
        Roadmap.ID_IMMERSION,
        Roadmap.ID_AI_TUTOR,
      ),
      tasks.map { it.id },
    )
  }

  @Test
  fun `immersion is present but never blocking`() {
    val immersion = Roadmap.tasksFor(Phase.SHADOWING).single { it.id == Roadmap.ID_IMMERSION }
    assertFalse(immersion.isBlockingIn(Phase.SHADOWING))
    assertFalse(immersion.isBlockingIn(Phase.SPEAKING))
    assertEquals(3, Roadmap.blockingTasksFor(Phase.SHADOWING).size)
  }

  @Test
  fun `the AI tutor task is visible but optional while only shadowing`() {
    val tutor = Roadmap.tasksFor(Phase.SHADOWING).single { it.id == Roadmap.ID_AI_TUTOR }
    // Phase 1 is explicitly "no speaking pressure yet", so it must not gate.
    assertFalse(tutor.isBlockingIn(Phase.SHADOWING))
  }

  @Test
  fun `the AI tutor task starts blocking once self-talk begins`() {
    val tutor = Roadmap.ALL_TASKS.single { it.id == Roadmap.ID_AI_TUTOR }
    assertTrue(tutor.isBlockingIn(Phase.SELF_TALK))
    assertTrue(tutor.isBlockingIn(Phase.SPEAKING))
  }

  @Test
  fun `self-talk phase adds two blocking tasks`() {
    val tasks = Roadmap.tasksFor(Phase.SELF_TALK)
    assertTrue(tasks.any { it.id == Roadmap.ID_SELF_TALK })
    // Self-talk itself, plus the AI tutor graduating from optional to required.
    assertEquals(5, Roadmap.blockingTasksFor(Phase.SELF_TALK).size)
  }

  @Test
  fun `speaking phase adds a conversation task that does not block`() {
    val tasks = Roadmap.tasksFor(Phase.SPEAKING)
    val conversation = tasks.single { it.id == Roadmap.ID_CONVERSATION }
    // The roadmap is explicit: iTalki/HelloTalk is "a task, not a hard requirement".
    assertFalse(conversation.isBlockingIn(Phase.SPEAKING))
    assertEquals(5, Roadmap.blockingTasksFor(Phase.SPEAKING).size)
  }

  @Test
  fun `study tool packages are collected from every task that has a launch target`() {
    assertTrue("com.smouldering_durtles.wk" in Roadmap.STUDY_TOOL_PACKAGES)
    assertTrue("bunpro.jp.bunpro_srs" in Roadmap.STUDY_TOOL_PACKAGES)
  }

  @Test
  fun `wanikani prefers the maintained client over the abandoned fork`() {
    val wanikani = Roadmap.ALL_TASKS.single { it.id == Roadmap.ID_WANIKANI }
    val candidates = wanikani.launch!!.packageCandidates
    // Smouldering Durtles is maintained; Flaming Durtles (the_tinkering) is not.
    assertTrue(candidates.indexOf("com.smouldering_durtles.wk") < candidates.indexOf("com.the_tinkering.wk"))
  }

  @Test
  fun `every launch target has a usable web fallback`() {
    Roadmap.ALL_TASKS.mapNotNull { it.launch }.forEach { launch ->
      assertTrue(launch.webUrl.startsWith("https://"))
    }
  }

  @Test
  fun `earlier phases do not see later tasks`() {
    assertFalse(Roadmap.tasksFor(Phase.SHADOWING).any { it.id == Roadmap.ID_SELF_TALK })
    assertFalse(Roadmap.tasksFor(Phase.SELF_TALK).any { it.id == Roadmap.ID_CONVERSATION })
  }

  @Test
  fun `the gate stays shut until every blocking task is done`() {
    assertFalse(Roadmap.isUnlocked(Phase.SHADOWING, emptySet()))
    assertFalse(Roadmap.isUnlocked(Phase.SHADOWING, setOf(Roadmap.ID_WANIKANI)))
    assertFalse(Roadmap.isUnlocked(Phase.SHADOWING, setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO)))
    assertTrue(
      Roadmap.isUnlocked(
        Phase.SHADOWING,
        setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
      )
    )
  }

  @Test
  fun `optional work alone never opens the gate`() {
    assertFalse(Roadmap.isUnlocked(Phase.SHADOWING, setOf(Roadmap.ID_IMMERSION)))
  }

  @Test
  fun `advancing a phase can re-lock a day that was already unlocked`() {
    val done = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING)
    assertTrue(Roadmap.isUnlocked(Phase.SHADOWING, done))
    // Self-talk introduces more blocking tasks, so the same completions no longer suffice.
    assertFalse(Roadmap.isUnlocked(Phase.SELF_TALK, done))
  }

  @Test
  fun `completing the AI tutor is required to unlock from self-talk on`() {
    val withoutTutor =
      setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING, Roadmap.ID_SELF_TALK)
    assertFalse(Roadmap.isUnlocked(Phase.SELF_TALK, withoutTutor))
    assertTrue(Roadmap.isUnlocked(Phase.SELF_TALK, withoutTutor + Roadmap.ID_AI_TUTOR))
  }

  @Test
  fun `the self-talk nudge only appears from week 4`() {
    assertNull(Roadmap.phasePrompt(1, Phase.SHADOWING))
    assertNull(Roadmap.phasePrompt(3, Phase.SHADOWING))
    assertNotNull(Roadmap.phasePrompt(4, Phase.SHADOWING))
    assertNotNull(Roadmap.phasePrompt(9, Phase.SHADOWING))
  }

  @Test
  fun `the speaking nudge is always available once in self-talk, and stops at the end`() {
    assertNotNull(Roadmap.phasePrompt(1, Phase.SELF_TALK))
    assertNull(Roadmap.phasePrompt(99, Phase.SPEAKING))
  }

  @Test
  fun `phases advance in order and then stop`() {
    assertEquals(Phase.SELF_TALK, Phase.SHADOWING.next())
    assertEquals(Phase.SPEAKING, Phase.SELF_TALK.next())
    assertNull(Phase.SPEAKING.next())
  }

  @Test
  fun `phase inclusion is cumulative`() {
    assertTrue(Phase.SPEAKING.includes(Phase.SHADOWING))
    assertTrue(Phase.SHADOWING.includes(Phase.SHADOWING))
    assertFalse(Phase.SHADOWING.includes(Phase.SPEAKING))
  }

  @Test
  fun `every week has a focus line`() {
    (1..10).forEach { week -> assertTrue(Roadmap.focusFor(week).isNotBlank()) }
  }
}
