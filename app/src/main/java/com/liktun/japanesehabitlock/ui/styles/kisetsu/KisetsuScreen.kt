package com.liktun.japanesehabitlock.ui.styles.kisetsu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings

/**
 * The daily checklist in the KISETSU (季節) style.
 *
 * KISETSU is Wa-Modern's sibling, not a fresh design: the same warm card structure, the
 * same generous padding, the same weight-contrasted sans-serif, the same hairline-not-
 * shadow restraint. What changes is that *nothing is a constant*. Every colour on the
 * screen, the shape of the particles drifting behind the header and the kanji in the
 * corner are resolved from the study day's month, so April is cream-and-sakura with
 * petals falling, July is bleached white-and-indigo with bubbles rising, October is
 * ivory-and-maple with leaves tumbling, and January is cold white-and-plum with snow.
 *
 * This is the variant that pays off on a timescale no screenshot can show. You install it
 * in February, and one morning in March you open it and the app is pink. Nothing asked
 * you to do anything; it simply moved with the year, the way the thing it is teaching
 * does.
 *
 * Layered on top of that is the **immersion ramp**: rather than hardcoding English, every
 * label, task title, task detail, gate word and button runs through [Immersion], which
 * swaps them to Japanese in stages as the roadmap weeks pass — headings first, then task
 * names, then everything — always with furigana above the kanji until the last step takes
 * the training wheels off. Two slow clocks, then: one turning with the calendar, one with
 * the user's own progress.
 *
 * Everything from the plain checklist survives the restyle: phase label and summary, week
 * number and formatted date, the locked/unlocked state, `blockingDone` of
 * `blockingTotal`, the blocking tasks with toggleable checkboxes, the optional tasks
 * marked as never-blocking, the weekly focus line and the weekly checkpoints. Tasks
 * carrying a [RoadmapTask.launch] get an "open" affordance that is a separate tap target
 * from the checkbox.
 *
 * State is fully hoisted — this composable owns no checklist state and performs no side
 * effects; it only reports [onToggleTask] and [onOpenTask] back to its caller. It also
 * never reads the clock: the season comes from `checklist.day.date`, which is what makes
 * all four seasons previewable and unit-testable.
 *
 * @param checklist the day's checklist, already resolved for the current phase.
 * @param onToggleTask called with `(taskId, newCheckedState)` when a checkbox or task
 *   body is tapped.
 * @param onOpenTask called when the "open" affordance on a launchable task is tapped.
 */
@Composable
fun KisetsuScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  // The two clocks. Both derived from the checklist, both cheap, both memoised on the
  // only input that can change them — so scrolling never re-resolves either.
  val theme = remember(checklist.day.date) { seasonForDate(checklist.day.date) }
  val immersion = remember(checklist.weekNumber) { Immersion.forWeek(checklist.weekNumber) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(theme.ground),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "header") { KisetsuHeaderCard(checklist, immersion, theme) }

    item(key = "gate") { KisetsuGateBanner(checklist, immersion, theme) }

    item(key = "rule-1") { SeasonHairline(theme, Modifier.padding(vertical = 6.dp)) }

    // ── Today: the blocking tasks. These are what actually hold the gate shut.
    item(key = "today-header") {
      KisetsuSectionHeader(ImmersionStrings.TODAY, immersion, theme)
    }
    kisetsuTasks(
      tasks = checklist.blockingTasks,
      keyPrefix = "block",
      checklist = checklist,
      immersion = immersion,
      theme = theme,
      onToggleTask = onToggleTask,
      onOpenTask = onOpenTask,
    )

    // ── Optional: tracked, never gating. Called out in words, not just by position,
    //    and each card repeats the claim so a row read in isolation still tells the truth.
    if (checklist.optionalTasks.isNotEmpty()) {
      item(key = "optional-header") {
        Column {
          Spacer(Modifier.height(6.dp))
          KisetsuSectionHeader(
            phrase = ImmersionStrings.OPTIONAL,
            immersion = immersion,
            theme = theme,
            accent = theme.inkSoft,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Nice to do. These never block your apps.",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 12.5.sp,
            color = theme.inkSoft,
          )
        }
      }
      kisetsuTasks(
        tasks = checklist.optionalTasks,
        keyPrefix = "opt",
        checklist = checklist,
        immersion = immersion,
        theme = theme,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
        neverBlocks = true,
      )
    }

    item(key = "rule-2") { SeasonHairline(theme, Modifier.padding(vertical = 6.dp)) }

    // ── This week: the one-line framing plus the weekly self-check.
    item(key = "week-header") {
      KisetsuSectionHeader(ImmersionStrings.THIS_WEEK, immersion, theme)
    }
    item(key = "focus") { KisetsuFocusCard(checklist.focus, theme) }

    item(key = "checkpoint-header") {
      Column {
        Spacer(Modifier.height(6.dp))
        KisetsuSectionHeader(
          phrase = ImmersionStrings.WEEKLY_CHECKPOINT,
          immersion = immersion,
          theme = theme,
          accent = theme.inkSoft,
        )
      }
    }
    item(key = "checkpoints") { KisetsuCheckpointList(checklist.checkpoints, theme) }

    checklist.phasePrompt?.let { prompt ->
      item(key = "phase-prompt") {
        Column {
          Spacer(Modifier.height(6.dp))
          KisetsuPhasePromptCard(prompt, theme)
        }
      }
    }

    item(key = "footer") { SeasonFooterMark(theme) }
  }
}

/** Emits a run of task cards, keyed by task id so toggles animate in place. */
private fun LazyListScope.kisetsuTasks(
  tasks: List<RoadmapTask>,
  keyPrefix: String,
  checklist: DailyChecklist,
  immersion: Immersion,
  theme: SeasonTheme,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  neverBlocks: Boolean = false,
) {
  tasks.forEach { task ->
    item(key = "$keyPrefix-${task.id}") {
      KisetsuTaskCard(
        task = task,
        done = checklist.isDone(task),
        immersion = immersion,
        theme = theme,
        onToggle = { checked -> onToggleTask(task.id, checked) },
        onOpen = task.launch?.let { { onOpenTask(task) } },
        neverBlocks = neverBlocks,
      )
    }
  }
}
