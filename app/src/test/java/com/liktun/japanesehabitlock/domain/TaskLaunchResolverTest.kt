package com.liktun.japanesehabitlock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLaunchResolverTest {

  private val launch =
    TaskLaunch(packageCandidates = listOf("com.first", "com.second"), webUrl = "https://example.com/")

  @Test
  fun `a null launch resolves to nothing`() {
    assertNull(TaskLaunchResolver.resolve(null, setOf("com.first")))
  }

  @Test
  fun `the installed candidate is chosen`() {
    val target = TaskLaunchResolver.resolve(launch, setOf("com.second"))
    assertEquals(TaskLaunchResolver.Target.App("com.second"), target)
  }

  @Test
  fun `declared order wins when several candidates are installed`() {
    val target = TaskLaunchResolver.resolve(launch, setOf("com.second", "com.first"))
    // A maintained client should beat an abandoned fork even when both are present.
    assertEquals(TaskLaunchResolver.Target.App("com.first"), target)
  }

  @Test
  fun `nothing installed falls back to the web`() {
    val target = TaskLaunchResolver.resolve(launch, emptySet())
    assertEquals(TaskLaunchResolver.Target.Web("https://example.com/"), target)
  }

  @Test
  fun `unrelated installed packages do not count`() {
    val target = TaskLaunchResolver.resolve(launch, setOf("com.somethingelse"))
    assertEquals(TaskLaunchResolver.Target.Web("https://example.com/"), target)
  }

  @Test
  fun `a launch with no candidates always goes to the web`() {
    val webOnly = TaskLaunch(packageCandidates = emptyList(), webUrl = "https://nhk.example/")
    assertEquals(
      TaskLaunchResolver.Target.Web("https://nhk.example/"),
      TaskLaunchResolver.resolve(webOnly, setOf("com.first")),
    )
  }

  @Test
  fun `the real wanikani task resolves to smouldering durtles when installed`() {
    val wanikani = Roadmap.ALL_TASKS.single { it.id == Roadmap.ID_WANIKANI }
    val target = TaskLaunchResolver.resolve(wanikani.launch, setOf("com.smouldering_durtles.wk"))
    assertEquals(TaskLaunchResolver.Target.App("com.smouldering_durtles.wk"), target)
  }

  @Test
  fun `the real bunpro task falls back to the study page`() {
    val bunpro = Roadmap.ALL_TASKS.single { it.id == Roadmap.ID_BUNPRO }
    val target = TaskLaunchResolver.resolve(bunpro.launch, emptySet())
    assertTrue(target is TaskLaunchResolver.Target.Web)
    assertEquals("https://bunpro.jp/study", (target as TaskLaunchResolver.Target.Web).url)
  }

  @Test
  fun `shadowing has no launch target because it has nowhere to go`() {
    val shadowing = Roadmap.ALL_TASKS.single { it.id == Roadmap.ID_SHADOWING }
    assertNull(shadowing.launch)
    assertNull(TaskLaunchResolver.resolve(shadowing.launch, setOf("com.anything")))
  }
}
