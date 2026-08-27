package com.liktun.japanesehabitlock.ui.styles

import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * The three candidate front-end directions, kept side by side so they can be compared
 * on a real device rather than argued about in the abstract.
 *
 * This enum is deliberately temporary scaffolding: once one style is chosen the other
 * two packages get deleted and the winner is promoted into the real checklist screen.
 */
enum class AppStyle(val displayName: String, val japanese: String, val blurb: String) {
  SUMIE("Sumi-e", "墨絵", "Ink and paper. Monochrome, spare, one red seal."),
  NEON("Neon Yokocho", "ネオン横丁", "Shinjuku alley at 2am. Dark, glowing, loud."),
  WA_MODERN("Wa-Modern", "和モダン", "Cream, sakura, matcha. Warm and shippable."),
}

/**
 * A representative checklist for previewing a style without touching real storage.
 *
 * Fixed date and week so screenshots of different styles are directly comparable.
 */
fun sampleChecklist(
  phase: Phase = Phase.SHADOWING,
  completed: Set<String> = emptySet(),
): DailyChecklist =
  DailyChecklist(
    day = StudyDay(LocalDate.of(2026, 8, 27)),
    weekNumber = 3,
    phase = phase,
    tasks = Roadmap.tasksFor(phase),
    completedTaskIds = completed,
  )
