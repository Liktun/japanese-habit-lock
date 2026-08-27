package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * A fixed sample day so the gate-state previews differ only in what's checked off.
 *
 * Deliberately not `Roadmap`-derived beyond the task list: a hard-coded date keeps
 * screenshots of this style directly comparable with the other candidates. [weekNumber]
 * is a parameter rather than a constant because the immersion ramp is driven entirely
 * by the week, so it is the one knob the last three previews need.
 */
private fun sumieSample(
  phase: Phase = Phase.SHADOWING,
  completed: Set<String> = emptySet(),
  weekNumber: Int = 3,
): DailyChecklist =
  DailyChecklist(
    day = StudyDay(LocalDate.of(2026, 8, 27)),
    weekNumber = weekNumber,
    phase = phase,
    tasks = Roadmap.tasksFor(phase),
    completedTaskIds = completed,
  )

// ───────────────────────────────────────────────────────────────────── gate states

/** Nothing done: bare paper, an empty brush stroke, no seal. 0 of 3. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumieLockedPreview() {
  SumieScreen(
    checklist = sumieSample(),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/** Mid-morning: one task struck through, the stroke a third of the way across. 1 of 3. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumiePartialPreview() {
  SumieScreen(
    checklist = sumieSample(completed = setOf(Roadmap.ID_WANIKANI)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/** Everything blocking cleared — the hanko has stamped and the gate is open. 3 of 3. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumieUnlockedPreview() {
  SumieScreen(
    checklist =
      sumieSample(
        completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
      ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

// ────────────────────────────────────────────────────────────────── immersion ramp

/**
 * Week 1 — `ImmersionLevel.ENGLISH`, the bottom of the ramp.
 *
 * Every heading, the gate word, both task columns and the open marks are English, and
 * each heading still carries its small kanji garnish beside it (今週, 本日, 任意). No
 * reading appears anywhere, so this is also the baseline row height to compare the
 * week-6 preview against.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumieWeek1ImmersionPreview() {
  SumieScreen(
    checklist = sumieSample(weekNumber = 1),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 6 — `ImmersionLevel.TITLES`.
 *
 * Headings are 今週 / 今日 / 任意 with kana above them and the garnish kanji gone, the
 * gate reads 施錠中, and the task titles are now 漢字の復習 / 文法の復習 with their
 * readings — while every detail line underneath is still English. This is the rung where
 * the ruby earns its keep: rows grow by one 10sp line and nothing else on the sheet moves.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumieWeek6ImmersionPreview() {
  SumieScreen(
    checklist = sumieSample(completed = setOf(Roadmap.ID_WANIKANI), weekNumber = 6),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 14 — `ImmersionLevel.FULL`.
 *
 * Everything the immersion tables cover is Japanese, including the detail lines, and the
 * readings are switched off. Held against week 6 this is the proof that the ruby reserves
 * no space when it is absent: the rows simply get shorter, the left margin is untouched,
 * and the only faint Japanese texture left on the page is the 習慣 margin column.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun SumieWeek14ImmersionPreview() {
  SumieScreen(
    checklist =
      sumieSample(
        completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO),
        weekNumber = 14,
      ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
