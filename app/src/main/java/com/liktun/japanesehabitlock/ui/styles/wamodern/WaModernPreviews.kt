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
 *
 * [weekNumber] is a parameter rather than a constant because the week is what drives the
 * immersion ramp: the same checklist rendered at week 1, 6 and 14 is three visibly
 * different screens, and the last three previews below exist to make that ramp reviewable
 * side by side without running the app for three months.
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

// ─────────────────────────────────────────────────────────────────── gate states

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

// ──────────────────────────────────────────────────────────────── immersion ramp

/**
 * Week 1 — `ImmersionLevel.ENGLISH`, the baseline.
 *
 * Every word on the screen is English, and the decorative kanji garnish sits beside each
 * heading exactly as the original design intended: "TODAY 今日", "OPTIONAL 任意". No ruby
 * line is rendered anywhere, so this is the reference height every card should return to
 * at week 13 when the readings come off again.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernWeek1ImmersionPreview() {
  WaModernScreen(
    checklist = waSample(weekNumber = 1),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 6 — `ImmersionLevel.TITLES`, the busiest rung of the ramp.
 *
 * Headings, the gate word and every task title are Japanese with kana centred above
 * them, while the detail lines under each task stay English — so this is the one week
 * where a card carries both scripts at once and the layout is under the most pressure.
 * The decorative garnish is gone by now, its job taken over by the readings.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernWeek6ImmersionPreview() {
  WaModernScreen(
    checklist = waSample(completed = setOf(Roadmap.ID_WANIKANI), weekNumber = 6),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 14 — `ImmersionLevel.FULL`.
 *
 * Everything the immersion system covers is Japanese and the readings are switched off,
 * so every ruby line collapses to nothing. Read against the week-6 preview this is the
 * proof that removing furigana leaves no reserved gap behind: the cards simply shrink
 * back to their week-1 heights with Japanese in them.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun WaModernWeek14ImmersionPreview() {
  WaModernScreen(
    checklist = waSample(
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO),
      weekNumber = 14,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
