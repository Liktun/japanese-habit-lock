package com.liktun.japanesehabitlock.ui.styles.kisetsu

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * Sample data for the previews.
 *
 * The **date is the interesting parameter here**, which is the whole point of KISETSU:
 * the screen takes its entire palette and its motif from `checklist.day.date`, so
 * previewing all four seasons is a matter of moving one number rather than flipping a
 * theme switch or mocking a clock. Every preview below is fully deterministic.
 *
 * Phase 1 (shadowing) has exactly three blocking tasks — WaniKani, Bunpro, shadowing —
 * plus two optional ones, which makes it the clearest phase to inspect the gate states in.
 */
private fun kisetsuSample(
  date: LocalDate,
  completed: Set<String> = emptySet(),
  weekNumber: Int = 3,
  phase: Phase = Phase.SHADOWING,
) = DailyChecklist(
  day = StudyDay(date),
  weekNumber = weekNumber,
  phase = phase,
  tasks = Roadmap.tasksFor(phase),
  completedTaskIds = completed,
)

/**
 * 春 — April. Cream ground, sakura accent, fresh-green progress, petals drifting down.
 *
 * Week 3 puts immersion at LABELS: the section headings are Japanese with readings while
 * the task titles are still English, which is the first rung of the ramp.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KisetsuHaruPreview() {
  KisetsuScreen(
    checklist = kisetsuSample(
      date = LocalDate.of(2026, 4, 10),
      completed = setOf(Roadmap.ID_WANIKANI),
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * 夏 — July. Bleached cool white, indigo accent, teal progress, bubbles rising *up*.
 *
 * Week 6 is TITLES: task names have switched to Japanese with furigana above them, so
 * this is the preview to check the [Ruby] baseline against — the details underneath are
 * still English, and the rows must not have grown a reserved gap.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KisetsuNatsuPreview() {
  KisetsuScreen(
    checklist = kisetsuSample(
      date = LocalDate.of(2026, 7, 10),
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO),
      weekNumber = 6,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * 秋 — October. Warm ivory, maple accent, russet progress, leaves tumbling hard.
 *
 * Fully unlocked at 3 of 3, so the gate banner and the ring both graduate to the russet
 * progress hue and every card carries the completion wash. Side by side with the summer
 * preview this is the pair that proves the palettes are actually distinct.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KisetsuAkiPreview() {
  KisetsuScreen(
    checklist = kisetsuSample(
      date = LocalDate.of(2026, 10, 10),
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
      weekNumber = 4,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * 冬 — January. Cold white, plum accent, slate progress, snow falling slowly.
 *
 * Nothing done yet: the locked banner and an empty ring, in the coldest of the four
 * palettes.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KisetsuFuyuPreview() {
  KisetsuScreen(
    checklist = kisetsuSample(date = LocalDate.of(2026, 1, 10)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * The immersion ramp at its far end: week 14, in autumn.
 *
 * Week 13 onward is [com.liktun.japanesehabitlock.domain.immersion.ImmersionLevel.FULL] —
 * everything is Japanese and the readings are **gone**. This is the preview that verifies
 * the furigana contract from the other direction: with every ruby null, no row may keep a
 * gap where a reading used to sit, so this screen must be visibly *tighter* than the
 * others rather than English text floating in over-tall boxes.
 *
 * Phase 2 also brings in the self-talk and conversation tasks, so this is the densest
 * layout the style has to survive.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KisetsuImmersionWeek14Preview() {
  KisetsuScreen(
    checklist = kisetsuSample(
      date = LocalDate.of(2026, 10, 10),
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_SHADOWING),
      weekNumber = 14,
      phase = Phase.SPEAKING,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
