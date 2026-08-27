package com.liktun.japanesehabitlock.ui.styles.sumizome

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
 * The daily checklist in the SUMIZOME (墨染) style — "evening ink".
 *
 * ## The look
 *
 * This is WA-MODERN after sunset: the same warm, calm, paper-goods sensibility, the same
 * rounded cards and gentle motion, re-grounded on charcoal-indigo. Cards are lifted
 * panels separated by a 1dp hairline instead of a shadow — on a dark ground there is
 * nothing for a shadow to be darker than, so the border does the work elevation does in
 * daylight. Warm motes drift *upward* behind the header where the daylight sibling had
 * petals falling; matcha still means progress and nothing else; gold leaf is ornament
 * and the one celebration colour, catching on the leading edge of the progress ring and
 * on the kumiko lattice between sections.
 *
 * It is deliberately not the neon style. No additive blending, no glow bloom, no
 * saturated cyan or magenta, no monospace. A paper lantern in a dark room, not a sign in
 * an alley.
 *
 * ## The immersion ramp
 *
 * Every user-facing string on this screen is routed through [Immersion], so the
 * interface itself becomes the immersion. The level is derived once per week number and
 * threaded down, which means the whole screen switches together — a screen at two levels
 * at once would look like a bug rather than a milestone.
 *
 * At week 2 this is an English checklist. By week 6 the headings and task names are
 * Japanese with readings above them. By week 10 the detail lines have followed. By week
 * 14 the readings come off. The [Ruby] composable is what makes that survivable: it sets
 * the kana above the phrase in small type and, crucially, collapses to a plain text when
 * there is no reading, so the layout never jumps as the user crosses a threshold.
 *
 * ## What survives from the plain checklist
 *
 * Phase label and summary, week number and formatted date, the locked/unlocked state,
 * `blockingDone` of `blockingTotal`, the blocking tasks with toggleable checkboxes, the
 * optional tasks marked as never-blocking, the weekly focus line and the weekly
 * checkpoints. Tasks carrying a [RoadmapTask.launch] get an open affordance that is a
 * separate tap target from the checkbox.
 *
 * State is fully hoisted — this composable owns no checklist state and performs no side
 * effects; it only reports [onToggleTask] and [onOpenTask] back to its caller.
 *
 * @param checklist the day's checklist, already resolved for the current phase.
 * @param onToggleTask called with `(taskId, newCheckedState)` when a checkbox or task
 *   body is tapped.
 * @param onOpenTask called when the open affordance on a launchable task is tapped.
 */
@Composable
fun SumizomeScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Keyed on the week number alone: the level is a pure function of the week, so this
  // recomputes exactly when the ramp could have moved and never on a task toggle.
  val immersion = remember(checklist.weekNumber) { Immersion.forWeek(checklist.weekNumber) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(SumizomePalette.night),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "header") { SumizomeHeaderCard(checklist, immersion) }

    item(key = "gate") { SumizomeGateBanner(checklist, immersion) }

    item(key = "kumiko-1") { GoldKumikoDivider(Modifier.padding(vertical = 4.dp)) }

    // ── Today: the blocking tasks. These are what actually hold the gate shut.
    item(key = "today-header") {
      SumizomeSectionHeader(
        label = immersion.sectionLabel(ImmersionStrings.TODAY),
        ruby = immersion.sectionRuby(ImmersionStrings.TODAY),
      )
    }
    item(key = "today-count") {
      Text(
        text = "${immersion.sectionLabel(ImmersionStrings.REQUIRED)} · " +
          "${checklist.blockingDone}/${checklist.blockingTotal}",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        color = SumizomePalette.washi.copy(alpha = 0.70f),
      )
    }
    sumizomeTasks(
      tasks = checklist.blockingTasks,
      keyPrefix = "block",
      checklist = checklist,
      immersion = immersion,
      neverBlocks = false,
      onToggleTask = onToggleTask,
      onOpenTask = onOpenTask,
    )

    // ── Optional: tracked, never gating. Called out in words, not just by position.
    if (checklist.optionalTasks.isNotEmpty()) {
      item(key = "optional-header") {
        Column {
          Spacer(Modifier.height(6.dp))
          SumizomeSectionHeader(
            label = immersion.sectionLabel(ImmersionStrings.OPTIONAL),
            ruby = immersion.sectionRuby(ImmersionStrings.OPTIONAL),
            accent = SumizomePalette.washi.copy(alpha = 0.78f),
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Nice to do. These never block your apps.",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 12.5.sp,
            color = SumizomePalette.washi.copy(alpha = 0.66f),
          )
        }
      }
      sumizomeTasks(
        tasks = checklist.optionalTasks,
        keyPrefix = "opt",
        checklist = checklist,
        immersion = immersion,
        neverBlocks = true,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
      )
    }

    item(key = "kumiko-2") { GoldKumikoDivider(Modifier.padding(vertical = 4.dp)) }

    // ── This week: the one-line framing plus the weekly self-check.
    item(key = "week-header") {
      SumizomeSectionHeader(
        label = immersion.sectionLabel(ImmersionStrings.THIS_WEEK),
        ruby = immersion.sectionRuby(ImmersionStrings.THIS_WEEK),
      )
    }
    item(key = "focus") { SumizomeFocusCard(checklist.focus) }

    item(key = "checkpoint-header") {
      Column {
        Spacer(Modifier.height(6.dp))
        SumizomeSectionHeader(
          label = immersion.sectionLabel(ImmersionStrings.WEEKLY_CHECKPOINT),
          ruby = immersion.sectionRuby(ImmersionStrings.WEEKLY_CHECKPOINT),
          accent = SumizomePalette.washi.copy(alpha = 0.78f),
        )
      }
    }
    item(key = "checkpoints") { SumizomeCheckpointList(checklist.checkpoints) }

    checklist.phasePrompt?.let { prompt ->
      item(key = "phase-prompt") {
        Column {
          Spacer(Modifier.height(6.dp))
          SumizomePhasePromptCard(prompt)
        }
      }
    }

    item(key = "footer") { SumizomeFooterMark() }
  }
}

/** Emits a run of task cards, keyed by task id so toggles animate in place. */
private fun LazyListScope.sumizomeTasks(
  tasks: List<RoadmapTask>,
  keyPrefix: String,
  checklist: DailyChecklist,
  immersion: Immersion,
  neverBlocks: Boolean,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
) {
  tasks.forEach { task ->
    item(key = "$keyPrefix-${task.id}") {
      SumizomeTaskCard(
        task = task,
        done = checklist.isDone(task),
        immersion = immersion,
        onToggle = { checked -> onToggleTask(task.id, checked) },
        onOpen = task.launch?.let { { onOpenTask(task) } },
        neverBlocks = neverBlocks,
      )
    }
  }
}
