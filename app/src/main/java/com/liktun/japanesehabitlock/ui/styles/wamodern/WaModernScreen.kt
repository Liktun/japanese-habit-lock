package com.liktun.japanesehabitlock.ui.styles.wamodern

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask

/**
 * The daily checklist in the WA-MODERN (和モダン) style.
 *
 * The design idea is *contemporary Japanese paper goods*: an unbleached cream ground,
 * white cards with shallow layered edges, one matcha ring carrying all the progress
 * meaning, sakura reserved for accents and the petals drifting behind the header, and
 * gold leaf used only for hairlines and the kumiko lattice between sections. Japanese
 * glosses sit beside the English section labels at a smaller size and in warm gray, so
 * they read as garnish rather than as a second headline.
 *
 * Everything from the plain checklist survives the restyle: phase label and summary,
 * week number and formatted date, the locked/unlocked state, `blockingDone` of
 * `blockingTotal`, the blocking tasks with toggleable checkboxes, the optional tasks
 * marked as never-blocking, the weekly focus line and the weekly checkpoints. Tasks
 * carrying a [RoadmapTask.launch] get an "Open" affordance that is a separate tap
 * target from the checkbox.
 *
 * State is fully hoisted — this composable owns no checklist state and performs no side
 * effects; it only reports [onToggleTask] and [onOpenTask] back to its caller.
 *
 * @param checklist the day's checklist, already resolved for the current phase.
 * @param onToggleTask called with `(taskId, newCheckedState)` when a checkbox or task
 *   body is tapped.
 * @param onOpenTask called when the "Open" affordance on a launchable task is tapped.
 */
@Composable
fun WaModernScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(WaPalette.kinari),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "header") { WaHeaderCard(checklist) }

    item(key = "gate") { WaGateBanner(checklist) }

    item(key = "kumiko-1") { KumikoDivider(Modifier.padding(vertical = 4.dp)) }

    // ── Today: the blocking tasks. These are what actually hold the gate shut.
    item(key = "today-header") {
      WaSectionHeader(title = "Today", japanese = "今日")
    }
    waTasks(
      tasks = checklist.blockingTasks,
      keyPrefix = "block",
      checklist = checklist,
      onToggleTask = onToggleTask,
      onOpenTask = onOpenTask,
    )

    // ── Optional: tracked, never gating. Called out in words, not just by position.
    if (checklist.optionalTasks.isNotEmpty()) {
      item(key = "optional-header") {
        Column {
          Spacer(Modifier.height(6.dp))
          WaSectionHeader(title = "Optional", japanese = "任意", accent = WaPalette.warmGray)
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Nice to do. These never block your apps.",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 12.5.sp,
            color = WaPalette.warmGray,
          )
        }
      }
      waTasks(
        tasks = checklist.optionalTasks,
        keyPrefix = "opt",
        checklist = checklist,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
      )
    }

    item(key = "kumiko-2") { KumikoDivider(Modifier.padding(vertical = 4.dp)) }

    // ── This week: the one-line framing plus the weekly self-check.
    item(key = "week-header") {
      WaSectionHeader(title = "This week", japanese = "週")
    }
    item(key = "focus") { WaFocusCard(checklist.focus) }

    item(key = "checkpoint-header") {
      Column {
        Spacer(Modifier.height(6.dp))
        WaSectionHeader(
          title = "Weekly checkpoint",
          japanese = "確認",
          accent = WaPalette.warmGray,
        )
      }
    }
    item(key = "checkpoints") { WaCheckpointList(checklist.checkpoints) }

    checklist.phasePrompt?.let { prompt ->
      item(key = "phase-prompt") {
        Column {
          Spacer(Modifier.height(6.dp))
          WaPhasePromptCard(prompt)
        }
      }
    }

    item(key = "footer") { WaFooterMark() }
  }
}

/** Emits a run of task cards, keyed by task id so toggles animate in place. */
private fun LazyListScope.waTasks(
  tasks: List<RoadmapTask>,
  keyPrefix: String,
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
) {
  tasks.forEach { task ->
    item(key = "$keyPrefix-${task.id}") {
      WaTaskCard(
        task = task,
        done = checklist.isDone(task),
        onToggle = { checked -> onToggleTask(task.id, checked) },
        onOpen = task.launch?.let { { onOpenTask(task) } },
      )
    }
  }
}

/**
 * A closing hanko-style mark.
 *
 * Pure decoration, but it gives the scroll a deliberate end instead of trailing off —
 * the paper equivalent of a stamp in the bottom corner.
 */
@Composable
private fun WaFooterMark(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(
      Modifier
        .width(28.dp)
        .height(1.dp)
        .background(WaPalette.goldLeaf.copy(alpha = 0.4f)),
    )
    Text(
      text = "  一日一歩  ",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 11.sp,
      letterSpacing = 2.sp,
      color = WaPalette.warmGray.copy(alpha = 0.8f),
    )
    Spacer(
      Modifier
        .width(28.dp)
        .height(1.dp)
        .background(WaPalette.goldLeaf.copy(alpha = 0.4f)),
    )
  }
}
