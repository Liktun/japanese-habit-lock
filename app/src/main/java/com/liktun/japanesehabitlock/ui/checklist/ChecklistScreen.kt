package com.liktun.japanesehabitlock.ui.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.StudyDay
import com.liktun.japanesehabitlock.theme.JapaneseHabitLockTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

@Composable
fun ChecklistScreen(
  modifier: Modifier = Modifier,
  serviceEnabled: Boolean = true,
  onOpenSettings: () -> Unit = {},
) {
  // applicationContext, not the Activity: the DataStore outlives this screen.
  val appContext = LocalContext.current.applicationContext
  val viewModel: ChecklistViewModel = viewModel {
    ChecklistViewModel(ChecklistRepository(appContext.habitLockDataStore))
  }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  when (val current = state) {
    ChecklistUiState.Loading -> Unit // First frame only; DataStore resolves immediately after.
    is ChecklistUiState.Error ->
      Text(
        text = "Could not load your checklist: ${current.throwable.message}",
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
      )
    is ChecklistUiState.Ready ->
      ChecklistContent(
        checklist = current.checklist,
        onToggleTask = viewModel::setTaskCompleted,
        onAdvancePhase = viewModel::advanceTo,
        onOpenTask = { task -> TaskOpener.open(appContext, task.launch) },
        serviceEnabled = serviceEnabled,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
      )
  }
}

@Composable
internal fun ChecklistContent(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onAdvancePhase: (Phase) -> Unit,
  modifier: Modifier = Modifier,
  onOpenTask: (RoadmapTask) -> Unit = {},
  serviceEnabled: Boolean = true,
  onOpenSettings: () -> Unit = {},
) {
  LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Header(checklist, onOpenSettings) }

    if (!serviceEnabled) {
      item { SetupBanner(onOpenSettings) }
    }

    item { GateBanner(checklist) }

    item { WeekFocus(checklist) }

    item { SectionLabel("Today") }
    items(checklist.blockingTasks) { task ->
      TaskRow(
        task = task,
        done = checklist.isDone(task),
        onToggle = { onToggleTask(task.id, it) },
        onOpen = task.launch?.let { { onOpenTask(task) } },
      )
    }

    if (checklist.optionalTasks.isNotEmpty()) {
      item { SectionLabel("Optional — never blocks") }
      items(checklist.optionalTasks) { task ->
        TaskRow(
          task = task,
          done = checklist.isDone(task),
          onToggle = { onToggleTask(task.id, it) },
          onOpen = task.launch?.let { { onOpenTask(task) } },
        )
      }
    }

    item { SectionLabel("Weekly checkpoint") }
    item { Checkpoints(checklist) }

    checklist.phasePrompt?.let { prompt ->
      item { PhasePromptCard(prompt = prompt, nextPhase = checklist.phase.next(), onAdvance = onAdvancePhase) }
    }

    item { Spacer(Modifier.height(24.dp)) }
  }
}

/** LazyListScope.items for a plain List, keyed by task id so toggles animate correctly. */
private fun androidx.compose.foundation.lazy.LazyListScope.items(
  tasks: List<RoadmapTask>,
  content: @Composable (RoadmapTask) -> Unit,
) {
  tasks.forEach { task -> item(key = task.id) { content(task) } }
}

@Composable
private fun Header(checklist: DailyChecklist, onOpenSettings: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.weight(1f)) {
      Text(
        text = checklist.phase.label,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = "Week ${checklist.weekNumber}  ·  ${checklist.day.date.format(DAY_FORMAT)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    TextButton(onClick = onOpenSettings) { Text("Apps") }
  }
}

/**
 * Shown until the accessibility service is switched on.
 *
 * Without the service the checklist still works but nothing is actually blocked, which
 * would be a silent, invisible failure — so it is called out loudly rather than left
 * for the user to discover when a "blocked" app opens normally.
 */
@Composable
private fun SetupBanner(onOpenSettings: () -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = "Blocking is off",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "Your checklist works, but no apps are actually blocked yet. Pick your apps and turn on blocking.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedButton(onClick = onOpenSettings) { Text("Set up blocking") }
    }
  }
}

@Composable
private fun GateBanner(checklist: DailyChecklist) {
  val unlocked = checklist.isUnlocked
  val container =
    if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
  val onContainer =
    if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

  Surface(color = container, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = if (unlocked) "Unlocked" else "Locked",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = onContainer,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text =
          if (unlocked) {
            "Everything required is done. Your distraction apps are open."
          } else {
            "${checklist.blockingDone} of ${checklist.blockingTotal} done — distraction apps stay blocked."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = onContainer,
      )
      Spacer(Modifier.height(12.dp))
      LinearProgressIndicator(
        progress = { checklist.progress },
        modifier = Modifier.fillMaxWidth(),
        color = onContainer,
        trackColor = onContainer.copy(alpha = 0.24f),
      )
    }
  }
}

@Composable
private fun WeekFocus(checklist: DailyChecklist) {
  Column {
    SectionLabel("This week")
    Spacer(Modifier.height(4.dp))
    Text(text = checklist.focus, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.sp,
  )
}

@Composable
private fun TaskRow(
  task: RoadmapTask,
  done: Boolean,
  onToggle: (Boolean) -> Unit,
  onOpen: (() -> Unit)? = null,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (done) 0.5f else 1f),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      // Only the text area toggles. The open button is a separate target so tapping
      // "go do this" never silently marks it done at the same time.
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
      Checkbox(checked = done, onCheckedChange = onToggle)
      Spacer(Modifier.width(4.dp))
      Column(Modifier.weight(1f).clickable { onToggle(!done) }) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          textDecoration = if (done) TextDecoration.LineThrough else null,
        )
        Text(
          text = task.detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (onOpen != null) {
        TextButton(onClick = onOpen) { Text("Open") }
      }
    }
  }
}

@Composable
private fun Checkpoints(checklist: DailyChecklist) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    checklist.checkpoints.forEach { line ->
      Row {
        Text(text = "•  ", style = MaterialTheme.typography.bodyMedium)
        Text(
          text = line,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun PhasePromptCard(prompt: String, nextPhase: Phase?, onAdvance: (Phase) -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = "When you're ready",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = prompt,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      if (nextPhase != null) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onAdvance(nextPhase) }) { Text("Start ${nextPhase.label}") }
      }
    }
  }
}

// ---------------------------------------------------------------- previews

private fun sampleChecklist(phase: Phase = Phase.SHADOWING, completed: Set<String> = emptySet()) =
  DailyChecklist(
    day = StudyDay(LocalDate.of(2026, 8, 24)),
    weekNumber = 3,
    phase = phase,
    tasks = Roadmap.tasksFor(phase),
    completedTaskIds = completed,
  )

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ChecklistLockedPreview() {
  JapaneseHabitLockTheme {
    ChecklistContent(
      checklist = sampleChecklist(completed = setOf(Roadmap.ID_WANIKANI)),
      onToggleTask = { _, _ -> },
      onAdvancePhase = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ChecklistUnlockedPreview() {
  JapaneseHabitLockTheme {
    ChecklistContent(
      checklist =
        sampleChecklist(
          completed = setOf(Roadmap.ID_WANIKANI, Roadmap.ID_BUNPRO, Roadmap.ID_SHADOWING),
        ),
      onToggleTask = { _, _ -> },
      onAdvancePhase = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}
