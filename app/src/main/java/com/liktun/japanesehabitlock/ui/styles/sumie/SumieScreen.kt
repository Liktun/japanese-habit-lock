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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings

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
 * **Immersion.** Every phrase that has a translation resolves through [Immersion], built
 * once from `checklist.weekNumber` and threaded into each component, so the whole sheet
 * is always at exactly one level. As the weeks pass the section labels, the gate word,
 * the task titles and finally the detail lines turn Japanese, each carrying its kana
 * above it via [Ruby] until week 13 takes the readings away. Two things keep that from
 * looking bolted on: the Japanese is set in the same [androidx.compose.ui.text.font.FontFamily.Serif]
 * with the same open tracking as the English it replaces, so it stays *typeset*; and the
 * small kanji garnishes this sheet used to print beside its English headings (今週 next
 * to "THIS WEEK") are dropped the moment the heading itself becomes Japanese, because a
 * heading reading 今週 今週 is a bug, not immersion.
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
  // Resolved once for the whole sheet and keyed on the week: two different immersion
  // levels visible at the same time would read as a rendering fault, not as a ramp.
  val immersion = remember(checklist.weekNumber) { Immersion.forWeek(checklist.weekNumber) }
  // While headings are still English they keep their decorative kanji; once the heading
  // *is* the kanji, the garnish would simply repeat it.
  val garnished = immersion.headingsAreEnglish()

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
        SumieHeader(checklist = checklist, immersion = immersion)
        Spacer(Modifier.height(30.dp))
        InkHairline(Modifier.fillMaxWidth(), seed = 2)
        Spacer(Modifier.height(26.dp))
      }

      item(key = "gate") {
        SumieGate(checklist = checklist, immersion = immersion)
        Spacer(Modifier.height(34.dp))
      }

      item(key = "focus-label") {
        SumieSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.THIS_WEEK),
          ruby = immersion.sectionRuby(ImmersionStrings.THIS_WEEK),
          garnish = "今週".takeIf { garnished },
        )
        Spacer(Modifier.height(12.dp))
      }
      item(key = "focus") {
        SumieFocusLine(checklist.focus)
        Spacer(Modifier.height(34.dp))
      }

      item(key = "today-label") {
        SumieSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.TODAY),
          ruby = immersion.sectionRuby(ImmersionStrings.TODAY),
          garnish = "本日".takeIf { garnished },
        )
        Spacer(Modifier.height(6.dp))
      }
      sumieTasks(
        tasks = checklist.blockingTasks,
        checklist = checklist,
        immersion = immersion,
        onToggleTask = onToggleTask,
        onOpenTask = onOpenTask,
        dimmed = false,
      )

      if (checklist.optionalTasks.isNotEmpty()) {
        item(key = "optional-label") {
          Spacer(Modifier.height(30.dp))
          // Said plainly and set in the palest ink there is: these rows are tracked,
          // but they can never hold the gate shut, and the user should never wonder.
          // "never blocks" rides along as a trailing clause so the promise survives
          // the switch into Japanese instead of being carried by position alone.
          SumieSectionLabel(
            label = immersion.sectionLabel(ImmersionStrings.OPTIONAL),
            ruby = immersion.sectionRuby(ImmersionStrings.OPTIONAL),
            garnish = "任意".takeIf { garnished },
            trailing = "· ${immersion.sectionLabel(ImmersionStrings.NEVER_BLOCKS)}",
            trailingRuby = immersion.sectionRuby(ImmersionStrings.NEVER_BLOCKS),
          )
          Spacer(Modifier.height(6.dp))
        }
        sumieTasks(
          tasks = checklist.optionalTasks,
          checklist = checklist,
          immersion = immersion,
          onToggleTask = onToggleTask,
          onOpenTask = onOpenTask,
          dimmed = true,
        )
      }

      item(key = "checkpoint-label") {
        Spacer(Modifier.height(34.dp))
        InkHairline(Modifier.fillMaxWidth(), seed = 6)
        Spacer(Modifier.height(24.dp))
        SumieSectionLabel(
          label = immersion.sectionLabel(ImmersionStrings.WEEKLY_CHECKPOINT),
          ruby = immersion.sectionRuby(ImmersionStrings.WEEKLY_CHECKPOINT),
          garnish = "週次点検".takeIf { garnished },
        )
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
          // No immersion phrase exists for this nudge, so it stays bilingual the way
          // the masthead does: English label, kanji garnish, permanently.
          SumieSectionLabel(label = "When you're ready", garnish = "次の段")
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
  immersion: Immersion,
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
          immersion = immersion,
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
