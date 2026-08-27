package com.liktun.japanesehabitlock.domain.immersion

import com.liktun.japanesehabitlock.domain.Roadmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersionLevelTest {

  @Test
  fun `the first two weeks stay in English`() {
    // The fortnight when people quit is not the time to make the app unreadable.
    assertEquals(ImmersionLevel.ENGLISH, ImmersionLevel.forWeek(1))
    assertEquals(ImmersionLevel.ENGLISH, ImmersionLevel.forWeek(2))
  }

  @Test
  fun `labels switch at week three`() {
    assertEquals(ImmersionLevel.LABELS, ImmersionLevel.forWeek(3))
    assertEquals(ImmersionLevel.LABELS, ImmersionLevel.forWeek(4))
  }

  @Test
  fun `task titles switch at week five`() {
    assertEquals(ImmersionLevel.TITLES, ImmersionLevel.forWeek(5))
    assertEquals(ImmersionLevel.TITLES, ImmersionLevel.forWeek(8))
  }

  @Test
  fun `everything is japanese with readings from week nine`() {
    assertEquals(ImmersionLevel.FULL_FURIGANA, ImmersionLevel.forWeek(9))
    assertEquals(ImmersionLevel.FULL_FURIGANA, ImmersionLevel.forWeek(12))
  }

  @Test
  fun `readings come off at week thirteen and stay off`() {
    assertEquals(ImmersionLevel.FULL, ImmersionLevel.forWeek(13))
    assertEquals(ImmersionLevel.FULL, ImmersionLevel.forWeek(200))
  }

  @Test
  fun `the ramp never goes backwards as weeks increase`() {
    val orders = (1..60).map { ImmersionLevel.forWeek(it).order }
    assertEquals(orders.sorted(), orders)
  }

  @Test
  fun `a week before the start clamps to english rather than crashing`() {
    // weekNumberFor already clamps to 1, but a defensive zero must not throw.
    assertEquals(ImmersionLevel.ENGLISH, ImmersionLevel.forWeek(0))
    assertEquals(ImmersionLevel.ENGLISH, ImmersionLevel.forWeek(-5))
  }

  @Test
  fun `furigana is shown at every japanese level except the last`() {
    assertFalse(ImmersionLevel.ENGLISH.showsFurigana)
    assertTrue(ImmersionLevel.LABELS.showsFurigana)
    assertTrue(ImmersionLevel.TITLES.showsFurigana)
    assertTrue(ImmersionLevel.FULL_FURIGANA.showsFurigana)
    assertFalse(ImmersionLevel.FULL.showsFurigana)
  }

  @Test
  fun `a pinned level overrides the automatic ramp in both directions`() {
    // Drowning at week 20: pin back.
    assertEquals(ImmersionLevel.LABELS, ImmersionLevel.resolve(20, ImmersionLevel.LABELS))
    // Impatient on day one: pin forward.
    assertEquals(ImmersionLevel.FULL, ImmersionLevel.resolve(1, ImmersionLevel.FULL))
  }

  @Test
  fun `no pin means the automatic ramp applies`() {
    assertEquals(ImmersionLevel.forWeek(7), ImmersionLevel.resolve(7, null))
  }

  @Test
  fun `inclusion is cumulative`() {
    assertTrue(ImmersionLevel.FULL.includes(ImmersionLevel.LABELS))
    assertTrue(ImmersionLevel.LABELS.includes(ImmersionLevel.LABELS))
    assertFalse(ImmersionLevel.LABELS.includes(ImmersionLevel.FULL))
  }

  @Test
  fun `firstWeekOf agrees with forWeek for every level`() {
    ImmersionLevel.entries.forEach { level ->
      assertEquals(level, ImmersionLevel.forWeek(ImmersionLevel.firstWeekOf(level)))
    }
  }

  @Test
  fun `firstWeekOf is the earliest week that reaches the level`() {
    ImmersionLevel.entries.filter { it != ImmersionLevel.ENGLISH }.forEach { level ->
      val first = ImmersionLevel.firstWeekOf(level)
      assertTrue(
        "week ${first - 1} should not yet be $level",
        ImmersionLevel.forWeek(first - 1).order < level.order,
      )
    }
  }
}

class ImmersionStringsTest {

  @Test
  fun `every roadmap task has a japanese title`() {
    Roadmap.ALL_TASKS.forEach { task ->
      assertNotNull("missing title for ${task.id}", ImmersionStrings.TASK_TITLES[task.id])
    }
  }

  @Test
  fun `every roadmap task has a japanese detail`() {
    Roadmap.ALL_TASKS.forEach { task ->
      assertNotNull("missing detail for ${task.id}", ImmersionStrings.TASK_DETAILS[task.id])
    }
  }

  @Test
  fun `no phrase has an empty japanese or english form`() {
    (ImmersionStrings.TASK_TITLES.values + ImmersionStrings.TASK_DETAILS.values).forEach {
      assertTrue(it.english.isNotBlank())
      assertTrue(it.japanese.isNotBlank())
    }
  }

  @Test
  fun `an untranslated id falls back to the roadmap english on both sides`() {
    val phrase = ImmersionStrings.taskTitle("not_a_real_task", "Fallback title")
    assertEquals("Fallback title", phrase.english)
    // Falling back to English for the Japanese form is deliberate: showing a blank or a
    // placeholder would be worse than showing the language the user already reads.
    assertEquals("Fallback title", phrase.japanese)
  }
}

class ImmersionTest {

  private val week1 = Immersion.forWeek(1)
  private val week3 = Immersion.forWeek(3)
  private val week6 = Immersion.forWeek(6)
  private val week10 = Immersion.forWeek(10)
  private val week20 = Immersion.forWeek(20)

  @Test
  fun `week one shows english everywhere`() {
    assertEquals("Today", week1.sectionLabel(ImmersionStrings.TODAY))
    assertEquals(
      "WaniKani reviews cleared",
      week1.taskTitle(Roadmap.ID_WANIKANI, "WaniKani reviews cleared"),
    )
  }

  @Test
  fun `week three switches labels but not task titles`() {
    assertEquals("今日", week3.sectionLabel(ImmersionStrings.TODAY))
    assertEquals(
      "WaniKani reviews cleared",
      week3.taskTitle(Roadmap.ID_WANIKANI, "WaniKani reviews cleared"),
    )
  }

  @Test
  fun `week six switches task titles but not their details`() {
    assertEquals("漢字の復習", week6.taskTitle(Roadmap.ID_WANIKANI, "x"))
    assertEquals(
      "Review queue down to zero.",
      week6.taskDetail(Roadmap.ID_WANIKANI, "Review queue down to zero."),
    )
  }

  @Test
  fun `week ten switches everything including details`() {
    assertEquals("復習をゼロにする。", week10.taskDetail(Roadmap.ID_WANIKANI, "x"))
  }

  @Test
  fun `furigana is offered while ramping and withdrawn at the end`() {
    assertNotNull(week3.sectionRuby(ImmersionStrings.TODAY))
    assertNotNull(week10.sectionRuby(ImmersionStrings.TODAY))
    assertNull(week20.sectionRuby(ImmersionStrings.TODAY))
  }

  @Test
  fun `english text never carries a reading`() {
    // A reading above an English word would be nonsense.
    assertNull(week1.sectionRuby(ImmersionStrings.TODAY))
    assertNull(week3.taskTitleRuby(Roadmap.ID_WANIKANI, "x"))
  }

  @Test
  fun `a phrase with no kanji has no reading even when japanese`() {
    val shadowing = ImmersionStrings.TASK_TITLES.getValue(Roadmap.ID_SHADOWING)
    assertNull(shadowing.furigana)
    assertNull(week10.taskTitleRuby(Roadmap.ID_SHADOWING, "x"))
  }

  @Test
  fun `a level-up notice appears only when the level actually rises`() {
    assertNotNull(week3.levelUpNotice(ImmersionLevel.ENGLISH))
    assertNull(week3.levelUpNotice(ImmersionLevel.LABELS))
    // Pinning backwards must not congratulate the user for regressing.
    assertNull(week3.levelUpNotice(ImmersionLevel.FULL))
    assertNull(week3.levelUpNotice(null))
  }

  @Test
  fun `every rising level has a notice worth showing`() {
    listOf(
        ImmersionLevel.LABELS,
        ImmersionLevel.TITLES,
        ImmersionLevel.FULL_FURIGANA,
        ImmersionLevel.FULL,
      )
      .forEach { level ->
        val notice = Immersion(level).levelUpNotice(ImmersionLevel.ENGLISH)
        assertTrue("no notice for $level", !notice.isNullOrBlank())
      }
  }
}
