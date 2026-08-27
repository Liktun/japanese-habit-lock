package com.liktun.japanesehabitlock.ui.blocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.MainActivity
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.Roadmap
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.StudyDay
import com.liktun.japanesehabitlock.theme.JapaneseHabitLockTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.map

/**
 * The interstitial shown when a blocked app is opened before the day's work is done.
 *
 * Deliberately offers no snooze, no "just 5 minutes", and no override. An escape hatch
 * would defeat the entire point of the app — the user installed this precisely because
 * in-the-moment willpower is the thing they don't have. The only two ways out are the
 * home button and finishing the checklist.
 *
 * Launched by [com.liktun.japanesehabitlock.service.HabitLockAccessibilityService] with
 * FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK, so it replaces the blocked app's
 * task rather than stacking on top of it.
 */
class BlockerActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = ChecklistRepository(applicationContext.habitLockDataStore)
    val checklistFlow = repository.checklist
    // Read once here rather than in the composable: the Activity is relaunched with a
    // fresh Intent on each block, so this is the only place the value is authoritative.
    val surfaceLabel = intent?.getStringExtra("surface_label")

    setContent {
      JapaneseHabitLockTheme {
        val checklist by checklistFlow.map { it as DailyChecklist? }.collectAsState(initial = null)
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
          checklist?.let {
            BlockerContent(
              checklist = it,
              onOpenChecklist = { openChecklist() },
              modifier = Modifier.safeDrawingPadding().padding(24.dp),
              surfaceLabel = surfaceLabel,
            )
          }
        }
      }
    }
  }

  /**
   * Back is intentionally inert.
   *
   * Dismissing the blocker with Back would drop the user straight back into the app
   * that was just blocked, since that task is still behind us.
   */
  @Suppress("MissingSuperCall", "OVERRIDE_DEPRECATION")
  override fun onBackPressed() {
    // No-op by design. Home still works; the user is not trapped.
  }

  private fun openChecklist() {
    startActivity(
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    )
    finish()
  }
}

@Composable
internal fun BlockerContent(
  checklist: DailyChecklist,
  onOpenChecklist: () -> Unit,
  modifier: Modifier = Modifier,
  /**
   * The surface that was blocked, or null for a whole-app block.
   *
   * Naming it matters: a user who chose to block only Reels needs to see that Reels is
   * what stopped them, not a generic wall that looks like the app broke. It is also the
   * fastest way to notice a false positive - if this says "Instagram Reels" while they
   * were reading a DM, the rule is wrong and they can tell us.
   */
  surfaceLabel: String? = null,
) {
  val remaining = checklist.blockingTasks.filterNot { checklist.isDone(it) }

  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
    Text(
      text = "Not yet",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
    if (surfaceLabel != null) {
      Spacer(Modifier.height(6.dp))
      Text(
        text = surfaceLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text =
        "${checklist.blockingDone} of ${checklist.blockingTotal} done. " +
          "Finish today's study and this opens up.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
    if (surfaceLabel != null) {
      Spacer(Modifier.height(6.dp))
      Text(
        text = "The rest of the app still works.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f),
      )
    }

    Spacer(Modifier.height(28.dp))

    Text(
      text = "STILL OWED",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.sp,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
    Spacer(Modifier.height(10.dp))

    remaining.forEach { task -> RemainingTaskRow(task) }

    Spacer(Modifier.height(32.dp))
    Button(onClick = onOpenChecklist, modifier = Modifier.fillMaxWidth()) { Text("Open checklist") }
  }
}

@Composable
private fun RemainingTaskRow(task: RoadmapTask) {
  Surface(
    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.08f),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
  ) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Column {
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
          text = task.detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
        )
      }
    }
  }
}

// ---------------------------------------------------------------- previews

@Preview(showBackground = true, widthDp = 380, heightDp = 760)
@Composable
private fun BlockerPreview() {
  JapaneseHabitLockTheme {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
      BlockerContent(
        checklist =
          DailyChecklist(
            day = StudyDay(LocalDate.of(2026, 8, 26)),
            weekNumber = 3,
            phase = Phase.SHADOWING,
            tasks = Roadmap.tasksFor(Phase.SHADOWING),
            completedTaskIds = setOf(Roadmap.ID_WANIKANI),
          ),
        onOpenChecklist = {},
        modifier = Modifier.padding(24.dp),
      )
    }
  }
}
