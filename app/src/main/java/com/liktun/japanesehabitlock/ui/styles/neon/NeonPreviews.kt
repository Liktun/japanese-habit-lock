package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.StudyDay
import java.time.LocalDate

/**
 * Sample data for the previews.
 *
 * Fixed date and week so the three states below differ only in what has been ticked,
 * which is the whole point of previewing them side by side. [Phase.SHADOWING] gives
 * exactly three blocking tasks (WaniKani, Bunpro, shadowing) plus two optional ones
 * (immersion, AI tutor), so the "0 of 3 / 1 of 3 / 3 of 3" progression is real data
 * and not a hand-tuned fake.
 */
private fun neonSample(completed: Set<String>): DailyChecklist =
  DailyChecklist(
    day = StudyDay(LocalDate.of(2026, 8, 27)),
    weekNumber = 3,
    phase = Phase.SHADOWING,
    tasks = Roadmap.tasksFor(Phase.SHADOWING),
    completedTaskIds = completed,
  )

/** Locked, nothing done: magenta everywhere, empty charge bar, padlock shut. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonLockedPreview() {
  NeonScreen(
    checklist = neonSample(emptySet()),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}

/** Part-way: one blocking task cleared, so the charge bar's leading edge sits mid-run. */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonPartialPreview() {
  NeonScreen(
    checklist = neonSample(setOf(Roadmap.ID_WANIKANI)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}

/**
 * Fully unlocked: every blocking task ticked, so the whole screen flips to cyan,
 * the padlock swings open and the charge bar is at full.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonUnlockedPreview() {
  NeonScreen(
    checklist =
      neonSample(setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING, Roadmap.ID_IMMERSION)),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}
