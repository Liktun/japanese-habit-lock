package com.liktun.japanesehabitlock.ui.styles

import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * The visual themes the user can choose between.
 *
 * These are not palette swaps — each is a separately implemented screen with its own
 * layout, motion and custom drawing. Keeping them all shipped rather than picking one
 * costs a little APK size and buys the user a real choice, which for an app you open
 * every morning for months is worth more than the bytes.
 *
 * The enum NAME is what gets persisted, so entries may be reordered or relabelled but
 * not renamed without orphaning existing preferences.
 */
enum class AppStyle(
  val displayName: String,
  val japanese: String,
  val blurb: String,
  /**
   * False while a theme is implemented but not yet confirmed rendering on a device.
   *
   * Shipping a theme that draws a blank screen would be worse than shipping one fewer,
   * so unverified entries are hidden from the picker until proven.
   */
  val verified: Boolean = true,
) {
  WA_MODERN("Wa-Modern", "和モダン", "Cream, sakura and matcha. Warm and soft."),
  KINARI("Kinari", "生成り", "Quiet stationery. Precise, typeset, hairline rules."),
  KISETSU("Kisetsu", "季節", "Follows the season. Spring blossom to winter snow."),
  SUMIE("Sumi-e", "墨絵", "Ink on paper. Monochrome, spare, one red seal."),
  NEON("Neon Yokocho", "ネオン横丁", "A Shinjuku alley at 2am. Dark and glowing."),
  SUMIZOME("Sumizome", "墨染", "Wa-Modern after sunset. Warm dark and gold.", verified = false);

  companion object {
    /** The themes offered in the picker, in display order. */
    val selectable: List<AppStyle> = entries.filter { it.verified }
  }
}

/**
 * A representative checklist for previewing a style without touching real storage.
 *
 * [weekNumber] and [date] are parameters because the immersion ramp and the seasonal
 * palette key off them — a fixed sample could only ever show one point on either curve.
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
