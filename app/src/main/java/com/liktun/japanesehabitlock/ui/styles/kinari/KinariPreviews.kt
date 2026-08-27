package com.liktun.japanesehabitlock.ui.styles.kinari

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * Previews for KINARI: the three gate states, plus two weeks chosen to show the
 * immersion ramp.
 *
 * `sampleChecklist` in the shared style package is pinned to week 3, which only ever
 * renders one rung of the ramp — so these build their own [DailyChecklist] with an
 * explicit week. That is the point of the last two previews: week 6 and week 14 are the
 * same screen with the same data, and the only difference is how much of it is Japanese.
 */
private fun kinariSample(
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

/** Every blocking task in phase 1, for the "unlocked" previews. */
private val ALL_BLOCKING =
  setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING)

// ─────────────────────────────────────────────────────────────────── gate states

/**
 * Locked, nothing done: 00/03, an empty ruled progress bar showing its three segments,
 * and the seal-red outlined square on the gate row.
 *
 * Week 3 is the first rung of the ramp, so the section labels are already Japanese
 * (今日 / 任意 / 今週) with readings above them, while tasks and prose stay English.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KinariLockedPreview() {
  KinariScreen(
    checklist = kinariSample(),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/** Mid-day: 02/03, so two of the three rule segments are matcha and two rows recede. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KinariPartialPreview() {
  KinariScreen(
    checklist = kinariSample(completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Unlocked: 03/03, the rule fully matcha, the gate mark a solid matcha square.
 *
 * Set to week 4 so the phase-prompt block also renders — the roadmap only suggests
 * advancing from week 4 onward, and this is the one gate-state preview that shows it.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KinariUnlockedPreview() {
  KinariScreen(
    checklist = kinariSample(completed = ALL_BLOCKING, weekNumber = 4),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

// ──────────────────────────────────────────────────────────────── immersion ramp

/**
 * Week 6 — `ImmersionLevel.TITLES`.
 *
 * Task names are now Japanese with kana printed above them (漢字の復習 / 文法の復習),
 * and so is the "open" link, but every detail line and all the weekly prose is still
 * English. This is the rung where the ruby rendering starts doing real work: the row
 * heights grow by one small line and nothing else about the layout moves.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KinariWeek6ImmersionPreview() {
  KinariScreen(
    checklist = kinariSample(completed = setOf(Roadmap.ID_WANIKANI), weekNumber = 6),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 14 — `ImmersionLevel.FULL`.
 *
 * Everything the immersion system covers is Japanese and the readings are gone, so the
 * ruby line collapses to nothing. Compared against the week-6 preview this is the proof
 * that removing furigana does not leave a reserved gap behind: the rows simply get
 * shorter, and the strict left margin is unchanged.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun KinariWeek14ImmersionPreview() {
  KinariScreen(
    checklist = kinariSample(
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO),
      weekNumber = 14,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
