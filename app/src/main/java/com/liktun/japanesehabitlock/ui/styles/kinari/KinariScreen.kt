package com.liktun.japanesehabitlock.ui.styles.kinari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
 * The daily checklist in the KINARI (生成り) style — "quiet stationery".
 *
 * The design idea is a **beautifully typeset notebook**, not an app: unbleached paper,
 * one strict left margin that every element on the screen aligns to, generous vertical
 * rhythm, and structure carried entirely by 1px hairlines. It is a sibling of Wa-Modern
 * rather than a replacement — same warmth, same information, same restraint about what
 * gates what — but it trades Wa-Modern's soft shadowed cards, sakura petals and swept
 * progress ring for a ruled progress bar, near-sharp 8dp cards with no shadow at all,
 * and exactly one ornament: a gold hairline with a diamond at its centre.
 *
 * Colour is rationed hard. Matcha means progress and nothing else, and the seal red
 * appears in precisely two places on the whole screen — the locked marker and the
 * "open" link — so it keeps the force of a stamp.
 *
 * **Immersion.** Every phrase on this screen resolves through [Immersion], which is
 * derived once from `checklist.weekNumber` and threaded down into each component. As
 * the roadmap weeks pass, section labels, gate words, task titles and finally task
 * details switch to Japanese; while the level still shows readings, each of those is
 * rendered with [Ruby], which sets the kana above the phrase. That is what makes the
 * ramp survivable — and when a reading is absent the ruby line collapses entirely, so
 * crossing a threshold never shifts the layout.
 *
 * Everything from the plain checklist survives the restyle: phase label and summary,
 * week number and formatted date, the locked/unlocked state, `blockingDone` of
 * `blockingTotal`, the blocking tasks with toggleable checkboxes, the optional tasks
 * marked as never-blocking, the weekly focus line and the weekly checkpoints. Tasks
 * carrying a [RoadmapTask.launch] get an "open" affordance that is a separate tap
 * target from the checkbox.
 *
 * State is fully hoisted — this composable owns no checklist state and performs no side
 * effects; it only reports [onToggleTask] and [onOpenTask] back to its caller.
 *
 * @param checklist the day's checklist, already resolved for the current phase.
 * @param onToggleTask called with `(taskId, newCheckedState)` when a checkbox or task
 *   body is tapped.
 * @param onOpenTask called when the "open" affordance on a launchable task is tapped.
 */
@Composable
fun KinariScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  // One immersion level for the whole screen, keyed on the week. Mixing two levels in
  // one render would look like a bug rather than a ramp, so it is resolved exactly once.
  val immersion = remember(checklist.weekNumber) { Immersion.forWeek(checklist.weekNumber) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(KinariPalette.kinari),
    contentPadding = PaddingValues(
      start = KINARI_MARGIN,
      end = KINARI_MARGIN,
      top = 36.dp,
      bottom = 40.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    item(key = "header") { KinariHeader(checklist = checklist, immersion = immersion) }

    item(key = "gate") {
      Column {
        KinariGap(28.dp)
        KinariGateRow(checklist = checklist, immersion = immersion)
      }
    }

    // ── Today: the blocking tasks. These are what actually hold the gate shut.
    item(key = "today-header") {
      Column {
        KinariGap(32.dp)
        KinariSectionHeader(
          label = immersion.sectionLabel(ImmersionStrings.TODAY),
          ruby = immersion.sectionRuby(ImmersionStrings.TODAY),
        )
        KinariGap(16.dp)
      }
    }
    kinariTasks(
      tasks = checklist.blockingTasks,
      keyPrefix = "block",
      checklist = checklist,
      immersion = immersion,
      onToggleTask = onToggleTask,
      onOpenTask = onOpenTask,
    )

    // ── Optional: tracked, never gating. Said in words, not implied by position.
    if (checklist.optionalTasks.isNotEmpty()) {
      item(key = "optional-header") {
        Column {
          KinariGap(30.dp)
          KinariSectionHeader(
            label = immersion.sectionLabel(ImmersionStrings.OPTIONAL),
            ruby = immersion.sectionRuby(ImmersionStrings.OPTIONAL),
            color = KinariPalette.sumiSoft,
          )
          KinariGap(12.dp)
          KinariNeverBlocksNote(immersion = immersion)
          KinariGap(16.dp)
        }
      }
      kinariTasks(
        tasks = checklist.optionalTasks,
        keyPrefix = "opt",
        checklist = checklist,
        immersion = immersion,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
      )
    }

    // ── The one ornament in the entire style, marking the day/week division.
    item(key = "ornament") {
      Column {
        KinariGap(36.dp)
        KinariGoldHairline()
        KinariGap(36.dp)
      }
    }

    // ── This week: the framing line and the weekly self-check.
    item(key = "week-header") {
      Column {
        KinariSectionHeader(
          label = immersion.sectionLabel(ImmersionStrings.THIS_WEEK),
          ruby = immersion.sectionRuby(ImmersionStrings.THIS_WEEK),
        )
        KinariGap(20.dp)
        KinariFocusBlock(checklist.focus)
      }
    }

    item(key = "checkpoints") {
      Column {
        KinariGap(30.dp)
        KinariSectionHeader(
          label = immersion.sectionLabel(ImmersionStrings.WEEKLY_CHECKPOINT),
          ruby = immersion.sectionRuby(ImmersionStrings.WEEKLY_CHECKPOINT),
          color = KinariPalette.sumiSoft,
        )
        KinariGap(18.dp)
        KinariCheckpointList(checklist.checkpoints)
      }
    }

    checklist.phasePrompt?.let { prompt ->
      item(key = "phase-prompt") {
        Column {
          KinariGap(30.dp)
          KinariPhasePrompt(prompt)
        }
      }
    }

    item(key = "colophon") { KinariColophon() }
  }
}

/**
 * Emits a run of task rows, keyed by task id so toggles animate in place.
 *
 * The gap lives between rows rather than around them so the first row sits flush with
 * its section header — the vertical rhythm is set by the section, not by the list.
 */
private fun LazyListScope.kinariTasks(
  tasks: List<RoadmapTask>,
  keyPrefix: String,
  checklist: DailyChecklist,
  immersion: Immersion,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
) {
  tasks.forEachIndexed { index, task ->
    item(key = "$keyPrefix-${task.id}") {
      Column {
        if (index > 0) Spacer(Modifier.height(10.dp))
        KinariTaskRow(
          task = task,
          done = checklist.isDone(task),
          immersion = immersion,
          onToggle = { checked -> onToggleTask(task.id, checked) },
          onOpen = task.launch?.let { { onOpenTask(task) } },
        )
      }
    }
  }
}

/**
 * A printer's colophon closing the page.
 *
 * Not decoration for its own sake: a long scroll that simply stops feels truncated,
 * and one small centred mark tells the eye it has reached the end of the sheet. Kept
 * to hairlines and 10sp so it never competes with the content above it.
 */
@Composable
private fun KinariColophon(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 44.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(
      Modifier
        .width(20.dp)
        .height(1.dp)
        .background(KinariPalette.hairline),
    )
    Text(
      text = "  生成り  ",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 10.sp,
      letterSpacing = 4.sp,
      color = KinariPalette.sumiSoft.copy(alpha = 0.65f),
    )
    Spacer(
      Modifier
        .width(20.dp)
        .height(1.dp)
        .background(KinariPalette.hairline),
    )
  }
}
