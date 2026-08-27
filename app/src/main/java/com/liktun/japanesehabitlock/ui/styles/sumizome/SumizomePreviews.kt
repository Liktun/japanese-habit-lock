package com.liktun.japanesehabitlock.ui.styles.sumizome

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
 * Built here rather than reused from `sampleChecklist`, which is pinned at week 3: the
 * whole point of half of these previews is to show the *immersion ramp*, and that is a
 * function of the week number. The date is fixed so the previews stay deterministic.
 *
 * Phase 1 (shadowing) has exactly three blocking tasks — WaniKani, Bunpro, shadowing —
 * plus two optional ones, which makes it the clearest phase to inspect the gate states
 * in.
 */
private fun sumizomeSample(
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

private val ALL_BLOCKING =
  setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING)

// ───────────────────────────────────────────────────────────────── gate states

/**
 * Nothing done yet: 0 of 3. Sakura-lit gate banner, empty ring, embers drifting.
 *
 * Week 3 is the first rung of the ramp, so the section headings are already Japanese
 * (今日 / 任意 / 今週) with readings above them while task names stay English.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomeLockedPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/** Mid-day: 2 of 3, so the ring is part-swept with its gold tip and two cards carry the wash. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomePartialPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Fully unlocked: 3 of 3, the banner graduates to gold and the glint fires.
 *
 * Set to week 4 so the phase-prompt card also appears — the roadmap only suggests
 * advancing from week 4 onward, and this is the one gate-state preview where it renders.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomeUnlockedPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(completed = ALL_BLOCKING, weekNumber = 4),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

// ─────────────────────────────────────────────────────────────── immersion ramp

/**
 * Week 6 — [com.liktun.japanesehabitlock.domain.immersion.ImmersionLevel.TITLES].
 *
 * The middle of the ramp and the most interesting frame to review: headings *and* task
 * names are Japanese with furigana above them (漢字の復習 / かんじのふくしゅう), while
 * every detail line is still English. This is the state the [Ruby] composable exists
 * for — the reading is what stops the switch from being a wall.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomeWeek6ImmersionPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(completed = setOf(Roadmap.ID_WANIKANI), weekNumber = 6),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 14 — [com.liktun.japanesehabitlock.domain.immersion.ImmersionLevel.FULL].
 *
 * The far end of the ramp: everything Japanese and the readings are gone. Compare this
 * against the week-6 preview to confirm the layout does not shift when the ruby
 * disappears — no reserved gap is the requirement, and two previews side by side is the
 * cheapest way to check it.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomeWeek14ImmersionPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(
      completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO),
      weekNumber = 14,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}

/**
 * Week 10 in the self-talk phase, fully unlocked.
 *
 * Covers the combination the other previews miss: a five-task blocking list, the
 * FULL_FURIGANA level where detail lines have also switched, and the gold celebration
 * state all at once.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF161A21)
@Composable
private fun SumizomeSelfTalkFullFuriganaPreview() {
  SumizomeScreen(
    checklist = sumizomeSample(
      completed = ALL_BLOCKING + setOf(Roadmap.ID_AI_TUTOR, Roadmap.ID_SELF_TALK),
      weekNumber = 10,
      phase = Phase.SELF_TALK,
    ),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
  )
}
