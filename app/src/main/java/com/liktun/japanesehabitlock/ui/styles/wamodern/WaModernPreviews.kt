package com.liktun.japanesehabitlock.ui.styles.wamodern

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
 * Phase 1 (shadowing) has exactly three blocking tasks — WaniKani, Bunpro, shadowing —
 * plus two optional ones, which makes it the clearest phase to inspect the three gate
 * states in. The date is fixed so the previews are deterministic.
 */
private fun waSample(
  completed: Set<String> = emptySet(),
  weekNumber: Int = 3,
  phase: Phase = Phase.SHADOWING,
) = DailyChecklist(
  day = StudyDay(LocalDate.of(2026, 8, 27)),
  weekNumber = weekNumber,
  phase = phase,
  tasks = Roadmap.tasksFor(phase),
  completedTaskIds = completed,
)

/** Nothing done yet: 0 of 3, sakura banner, empty matcha ring. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernLockedPreview() {
  WaModernScreen(
    checklist = waSample(),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/** Mid-day: 2 of 3, so the ring is partly swept and two cards carry the matcha wash. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernPartialPreview() {
  WaModernScreen(
    checklist = waSample(completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Fully unlocked: 3 of 3, banner graduates to matcha.
 *
 * Set to week 4 so the phase-prompt card also appears — the roadmap only suggests
 * advancing from week 4 onward, and this is the one preview where that card renders.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernUnlockedPreview() {
  WaModernScreen(
    checklist = waSample(
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
      weekNumber = 4,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
