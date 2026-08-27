package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings
import java.time.format.DateTimeFormatter

private val NEON_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE · MMMM d")

/** The hanging sign's glyphs: 習慣 (habit) over 日本語 (Japanese). */
private val SIGN_GLYPHS = listOf("習", "慣", "・", "日", "本", "語")

/**
 * **Style 2 — NEON YOKOCHO ネオン横丁.**
 *
 * The same [DailyChecklist] the plain screen renders, restaged as a Shinjuku
 * back-alley at 2am. The composition is a single [Box]: the wet-asphalt backdrop
 * ([NeonAlleyBackdrop]) at the bottom, the scrolling content above it, and a
 * [NeonVerticalSign] pinned to the right edge like a signboard bolted to the wall the
 * user is walking past.
 *
 * The design rules the screen holds to:
 * - **One flickering element.** Only the phase title fails like bad neon. Everything
 *   else is steady, which is what sells the title.
 * - **One breathing element.** Only the gate readout pulses, so the screen has a
 *   single heartbeat rather than a twitch everywhere.
 * - **Colour is meaning, not decoration.** Magenta means the gate is shut, cyan means
 *   done or open, amber means optional or "consider this", violet is structure. A
 *   blocking task is magenta until it is ticked and then turns cyan; optional tasks
 *   are amber and never touch magenta, so the eye can tell at a glance which rows can
 *   actually keep it locked out.
 * - **Nothing is a Material container.** Every panel is [NeonPanel] — a hairline tube
 *   with a hand-stacked halo over a near-black fill.
 *
 * **Immersion.** [Immersion] is resolved once from `checklist.weekNumber` and threaded
 * into every component that renders words, so the whole alley is always at exactly one
 * level. Two rules keep the ramp from fighting the style. First, the *garnish rule*:
 * this screen's signage is bilingual by design — 「今日」TODAY, 「必須」REQUIRED — and
 * that only works while the label beside the bracket is English. Once the ramp turns
 * the label itself into 今日, the bracketed copy is dropped rather than rendered twice.
 * Second, the *unlit rule*: furigana is set flat in [NEON_RUBY_INK] with no
 * [neonTextGlow] at all, because a glowing reading 10sp above a glowing sign is a smear,
 * not a reading.
 *
 * State is fully hoisted: the screen owns no checklist state and only reports taps
 * back through [onToggleTask] and [onOpenTask].
 *
 * @param checklist the day's data, rendered in full.
 * @param onToggleTask task id + desired completion state.
 * @param onOpenTask invoked for tasks with a non-null `launch`.
 */
@Composable
fun NeonScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  val unlocked = checklist.isUnlocked
  val signColor = if (unlocked) NeonPalette.Cyan else NeonPalette.Magenta
  // One level for the whole alley, keyed on the week. Two levels lit at once would
  // read as a fault in the sign rather than as a stage of the ramp.
  val immersion = remember(checklist.weekNumber) { Immersion.forWeek(checklist.weekNumber) }
  // The bracketed kanji is a garnish on an English word. Once the word is Japanese it
  // would simply be the same word twice, so it goes.
  val garnished = immersion.headingsAreEnglish()

  Box(modifier = modifier.fillMaxSize()) {
    NeonAlleyBackdrop(Modifier.fillMaxSize())

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 36.dp),
    ) {
      item(key = "header") {
        // Header and the hanging sign share a row so the sign starts at the alley mouth.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
          NeonSignHeader(
            phaseLabel = checklist.phase.label,
            phaseSummary = checklist.phase.summary,
            weekNumber = checklist.weekNumber,
            formattedDate = checklist.day.date.format(NEON_DAY_FORMAT),
            titleColor = signColor,
            immersion = immersion,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(14.dp))
          NeonVerticalSign(glyphs = SIGN_GLYPHS, color = signColor)
        }
      }

      gap(20.dp)

      item(key = "gate") {
        NeonGateStatus(
          unlocked = unlocked,
          blockingDone = checklist.blockingDone,
          blockingTotal = checklist.blockingTotal,
          progress = checklist.progress,
          immersion = immersion,
        )
      }

      gap(20.dp)

      item(key = "focus-label") {
        NeonSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.THIS_WEEK),
          ruby = immersion.sectionRuby(ImmersionStrings.THIS_WEEK),
          garnish = "週".takeIf { garnished },
          color = NeonPalette.Violet,
        )
      }
      gap(10.dp)
      item(key = "focus") { NeonFocusPanel(focus = checklist.focus, weekNumber = checklist.weekNumber) }

      gap(22.dp)

      item(key = "today-label") {
        NeonSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.REQUIRED),
          ruby = immersion.sectionRuby(ImmersionStrings.REQUIRED),
          garnish = "必須".takeIf { garnished },
          color = NeonPalette.Magenta,
          trailing = "${checklist.blockingDone}/${checklist.blockingTotal}",
        )
      }
      gap(10.dp)
      taskRows(
        tasks = checklist.blockingTasks,
        checklist = checklist,
        // Magenta = this row is why you are locked out. It turns cyan when cleared.
        accent = NeonPalette.Magenta,
        immersion = immersion,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
      )

      if (checklist.optionalTasks.isNotEmpty()) {
        gap(22.dp)
        item(key = "optional-label") {
          // "never blocks" is a promise, not decoration, so it ramps alongside the
          // heading instead of staying pinned to English forever.
          NeonSectionLabel(
            label = immersion.sectionLabel(ImmersionStrings.OPTIONAL),
            ruby = immersion.sectionRuby(ImmersionStrings.OPTIONAL),
            garnish = "任意".takeIf { garnished },
            color = NeonPalette.Amber,
            trailing = immersion.sectionLabel(ImmersionStrings.NEVER_BLOCKS),
            trailingRuby = immersion.sectionRuby(ImmersionStrings.NEVER_BLOCKS),
          )
        }
        gap(10.dp)
        taskRows(
          tasks = checklist.optionalTasks,
          checklist = checklist,
          // Amber, never magenta: these cannot hold the gate shut at this phase.
          accent = NeonPalette.Amber,
          immersion = immersion,
          onToggleTask = onToggleTask,
          onOpenTask = onOpenTask,
        )
      }

      gap(22.dp)

      item(key = "checkpoint-label") {
        NeonSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.WEEKLY_CHECKPOINT),
          ruby = immersion.sectionRuby(ImmersionStrings.WEEKLY_CHECKPOINT),
          garnish = "点検".takeIf { garnished },
          color = NeonPalette.Violet,
        )
      }
      gap(10.dp)
      item(key = "checkpoints") { NeonCheckpointList(checkpoints = checklist.checkpoints) }

      checklist.phasePrompt?.let { prompt ->
        gap(20.dp)
        item(key = "phase-prompt") { NeonPhasePrompt(prompt = prompt) }
      }

      gap(30.dp)
      item(key = "footer") { NeonFooter(checklist) }
    }
  }
}

/** Vertical rhythm as list items, so spacing survives item recycling. */
private fun LazyListScope.gap(height: androidx.compose.ui.unit.Dp) {
  item { Spacer(Modifier.height(height)) }
}

/**
 * Emits one [NeonTaskRow] per task, keyed by task id so a toggle animates the row it
 * belongs to instead of whatever is currently at that index.
 */
private fun LazyListScope.taskRows(
  tasks: List<RoadmapTask>,
  checklist: DailyChecklist,
  accent: androidx.compose.ui.graphics.Color,
  immersion: Immersion,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
) {
  tasks.forEachIndexed { index, task ->
    item(key = task.id) {
      Column {
        if (index > 0) Spacer(Modifier.height(10.dp))
        NeonTaskRow(
          task = task,
          done = checklist.isDone(task),
          accent = accent,
          immersion = immersion,
          onToggle = { desired -> onToggleTask(task.id, desired) },
          onOpen = task.launch?.let { { onOpenTask(task) } },
        )
      }
    }
  }
}
