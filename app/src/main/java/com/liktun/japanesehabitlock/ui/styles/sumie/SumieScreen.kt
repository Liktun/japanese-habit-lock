package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask

/**
 * The daily checklist rendered as a sheet of ink-wash painting — Style 1, 墨絵.
 *
 * The design argument here is subtraction. There is no card, no rounded container, no
 * filled button and no second colour: content is separated by hairlines and by generous
 * empty space (間 *ma*), the type is Serif throughout with the tracking opened up on
 * every label, and the entire palette is paper plus three dilutions of ink. Exactly two
 * marks break the monochrome — the vermilion hanko that stamps down when the last
 * blocking task clears, and one gold hairline under the week's focus line.
 *
 * All motion is drawn rather than animated-in-Material: progress is a tapered brush
 * stroke that grows, completion is a wavy ink line struck across the title, and the
 * checkbox is four hand-inked edges that fill with a two-stroke brush check.
 *
 * @param checklist the day's state; this composable is entirely stateless.
 * @param onToggleTask called with the task id and its new completion state.
 * @param onOpenTask called for tasks that carry a [RoadmapTask.launch].
 */
@Composable
fun SumieScreen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    InkWashBackdrop()

    // The margin motif rides on top of the paper but under the content, pinned to the
    // right edge where a scroll's chop mark would sit.
    SumieMarginColumn(
      text = "習慣",
      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding =
        PaddingValues(
          start = 28.dp,
          end = 56.dp,
          top = 44.dp,
          bottom = 56.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      item(key = "header") {
        SumieHeader(checklist)
        Spacer(Modifier.height(30.dp))
        InkHairline(Modifier.fillMaxWidth(), seed = 2)
        Spacer(Modifier.height(26.dp))
      }

      item(key = "gate") {
        SumieGate(checklist)
        Spacer(Modifier.height(34.dp))
      }

      item(key = "focus-label") {
        SumieSectionLabel(text = "This week", japanese = "今週")
        Spacer(Modifier.height(12.dp))
      }
      item(key = "focus") {
        SumieFocusLine(checklist.focus)
        Spacer(Modifier.height(34.dp))
      }

      item(key = "today-label") {
        SumieSectionLabel(text = "Today", japanese = "本日")
        Spacer(Modifier.height(6.dp))
      }
      sumieTasks(
        tasks = checklist.blockingTasks,
        checklist = checklist,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
        dimmed = false,
      )

      if (checklist.optionalTasks.isNotEmpty()) {
        item(key = "optional-label") {
          Spacer(Modifier.height(30.dp))
          // Said plainly and set in the palest ink there is: these rows are tracked,
          // but they can never hold the gate shut, and the user should never wonder.
          SumieSectionLabel(text = "Extra · never blocks", japanese = "任意")
          Spacer(Modifier.height(6.dp))
        }
        sumieTasks(
          tasks = checklist.optionalTasks,
          checklist = checklist,
          onToggleTask = onToggleTask,
          onOpenTask = onOpenTask,
          dimmed = true,
        )
      }

      item(key = "checkpoint-label") {
        Spacer(Modifier.height(34.dp))
        InkHairline(Modifier.fillMaxWidth(), seed = 6)
        Spacer(Modifier.height(24.dp))
        SumieSectionLabel(text = "Weekly checkpoint", japanese = "週次点検")
        Spacer(Modifier.height(12.dp))
      }
      item(key = "checkpoints") {
        Column {
          checklist.checkpoints.forEach { line -> SumieCheckpointLine(line) }
        }
      }

      checklist.phasePrompt?.let { prompt ->
        item(key = "phase-prompt") {
          Spacer(Modifier.height(30.dp))
          SumieSectionLabel(text = "When you're ready", japanese = "次の段")
          Spacer(Modifier.height(10.dp))
          SumieQuietNote(prompt)
        }
      }

      item(key = "tail") {
        Spacer(Modifier.height(40.dp))
        InkHairline(Modifier.fillMaxWidth(0.2f), seed = 9, alpha = 0.3f)
      }
    }
  }
}

/**
 * Emits a run of task rows, each followed by a hairline except the last.
 *
 * Keyed by task id so a toggle re-uses the same row and its strike-through animation
 * plays rather than snapping in on a freshly composed node.
 */
private fun LazyListScope.sumieTasks(
  tasks: List<RoadmapTask>,
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  dimmed: Boolean,
) {
  tasks.forEachIndexed { index, task ->
    item(key = task.id) {
      Column {
        SumieTaskRow(
          task = task,
          done = checklist.isDone(task),
          onToggle = { checked -> onToggleTask(task.id, checked) },
          onOpen = task.launch?.let { { onOpenTask(task) } },
          dimmed = dimmed,
        )
        if (index != tasks.lastIndex) {
          InkHairline(Modifier.fillMaxWidth(), alpha = 0.24f, seed = index + 31)
        }
      }
    }
  }
}
