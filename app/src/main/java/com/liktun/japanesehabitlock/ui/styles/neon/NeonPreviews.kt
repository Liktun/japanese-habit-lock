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
 * Fixed date so the gate-state previews differ only in what has been ticked, which is
 * the whole point of previewing them side by side. [Phase.SHADOWING] gives exactly three
 * blocking tasks (WaniKani, Bunpro, shadowing) plus two optional ones (immersion, AI
 * tutor), so the "0 of 3 / 1 of 3 / 3 of 3" progression is real data and not a
 * hand-tuned fake. [weekNumber] is a parameter because the immersion ramp keys off it
 * and nothing else.
 */
private fun neonSample(completed: Set<String>, weekNumber: Int = 3): DailyChecklist =
  DailyChecklist(
    day = StudyDay(LocalDate.of(2026, 8, 27)),
    weekNumber = weekNumber,
    phase = Phase.SHADOWING,
    tasks = Roadmap.tasksFor(Phase.SHADOWING),
    completedTaskIds = completed,
  )

// ───────────────────────────────────────────────────────────────────── gate states

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

// ────────────────────────────────────────────────────────────────── immersion ramp

/**
 * Week 1 — `ImmersionLevel.ENGLISH`, the bottom of the ramp.
 *
 * The alley is at its most bilingual: every heading is a lit bracketed kanji next to
 * an English word (「今日」TODAY, 「必須」REQUIRED, 「任意」OPTIONAL), the gate reads
 * 「施錠」 over LOCKED, task titles and launch chips are English. No reading is drawn
 * anywhere, which makes this the height baseline for the week-6 preview.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonWeek1ImmersionPreview() {
  NeonScreen(
    checklist = neonSample(emptySet(), weekNumber = 1),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}

/**
 * Week 6 — `ImmersionLevel.TITLES`.
 *
 * Every bracketed garnish has gone, because the headings themselves are now 今日 / 必須 /
 * 任意 with dim unlit kana above them — this is the preview that proves the screen never
 * renders 「今日」今日. Task titles are 漢字の復習 / 文法の復習 with readings and the launch
 * chip reads 開く, while all the detail lines and prose stay English.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonWeek6ImmersionPreview() {
  NeonScreen(
    checklist = neonSample(setOf(Roadmap.ID_WANIKANI), weekNumber = 6),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}

/**
 * Week 14 — `ImmersionLevel.FULL`.
 *
 * Everything the immersion tables cover is Japanese, detail lines included, and the
 * readings are switched off. Compared with week 6 this shows the ruby reserving no space
 * when absent — the panels get shorter rather than keeping an empty line — and it is the
 * quietest the alley ever looks, since the only remaining Japanese with a glow behind it
 * is the signage itself.
 */
@Preview(showBackground = true, widthDp = 400, heightDp = 900, backgroundColor = 0xFF0A0812)
@Composable
private fun NeonWeek14ImmersionPreview() {
  NeonScreen(
    checklist = neonSample(setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO), weekNumber = 14),
    onToggleTask = { _, _ -> },
    onOpenTask = {},
    modifier = Modifier.fillMaxSize().background(NeonPalette.Night),
  )
}
