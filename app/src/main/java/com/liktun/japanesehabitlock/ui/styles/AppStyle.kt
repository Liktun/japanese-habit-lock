package com.liktun.japanesehabitlock.ui.styles

import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * The candidate front-end directions, kept side by side so they can be compared on a
 * real device rather than argued about in the abstract.
 *
 * Temporary scaffolding: once one style is chosen the rest get deleted and the winner
 * is promoted into the real checklist screen.
 */
enum class AppStyle(val displayName: String, val japanese: String, val blurb: String) {
  // Round one.
  SUMIE("Sumi-e", "墨絵", "Ink and paper. Monochrome, spare, one red seal."),
  NEON("Neon Yokocho", "ネオン横丁", "Shinjuku alley at 2am. Dark, glowing, loud."),
  WA_MODERN("Wa-Modern", "和モダン", "Cream, sakura, matcha. Warm and shippable."),

  // Round two: siblings of the chosen Wa-Modern direction.
  KINARI("Kinari", "生成り", "Quiet stationery. Precise, typeset, hairline rules."),
  SUMIZOME("Sumizome", "墨染", "Wa-Modern after sunset. Warm dark, gold, embers."),
  KISETSU("Kisetsu", "季節", "Palette follows the real season. Spring to winter."),
}

/**
 * A representative checklist for previewing a style without touching real storage.
 *
 * [weekNumber] and [date] are parameters because the immersion ramp and the seasonal
 * palette both key off them — a fixed sample could only ever show one point on either
 * curve.
 */
fun sampleChecklist(
  phase: Phase = Phase.SHADOWING,
  completed: Set<String> = emptySet(),
  weekNumber: Int = 3,
  date: LocalDate = LocalDate.of(2026, 8, 27),
): DailyChecklist =
  DailyChecklist(
    day = StudyDay(date),
    weekNumber = weekNumber,
    phase = phase,
    tasks = Roadmap.tasksFor(phase),
    completedTaskIds = completed,
  )
