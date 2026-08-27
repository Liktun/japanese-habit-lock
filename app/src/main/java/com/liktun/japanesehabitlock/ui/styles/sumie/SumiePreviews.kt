package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * A fixed sample day so the three preview states differ only in what's checked off.
 *
 * Deliberately not `Roadmap`-derived beyond the task list: a hard-coded date and week
 * keep screenshots of this style directly comparable with the other two candidates.
 */
private fun sumieSample(
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
