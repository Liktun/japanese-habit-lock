package com.example.japanesehabitlock.domain

/**
 * Everything the checklist screen needs for one study day, and the gate decision
 * the blocking layer will eventually read.
 *
 * This is a plain value type with no Android or storage dependencies, which keeps
 * the interesting logic — what's done, what still blocks — testable with plain JUnit.
 */
data class DailyChecklist(
  val day: StudyDay,
  val weekNumber: Int,
  val phase: Phase,
  val tasks: List<RoadmapTask>,
  val completedTaskIds: Set<String>,
) {

  /** Tasks that hold the gate shut until they're checked off. */
  val blockingTasks: List<RoadmapTask>
    get() = tasks.filter { it.blocking }

  /** Tracked and displayed, but never gates anything. */
  val optionalTasks: List<RoadmapTask>
    get() = tasks.filterNot { it.blocking }

  fun isDone(task: RoadmapTask): Boolean = task.id in completedTaskIds

  val blockingTotal: Int
    get() = blockingTasks.size

  val blockingDone: Int
    get() = blockingTasks.count { isDone(it) }

  /**
   * True when every blocking task is checked off — distraction apps are open.
   *
   * Delegates to [Roadmap.isUnlocked] so the UI and the persisted mirror flag can
   * never disagree about what "done" means.
   */
  val isUnlocked: Boolean
    get() = Roadmap.isUnlocked(phase, completedTaskIds)

  /** 0f..1f over the blocking tasks only. */
  val progress: Float
    get() = if (blockingTotal == 0) 1f else blockingDone.toFloat() / blockingTotal.toFloat()

  val focus: String
    get() = Roadmap.focusFor(weekNumber)

  val checkpoints: List<String>
    get() = Roadmap.WEEKLY_CHECKPOINTS

  val phasePrompt: String?
    get() = Roadmap.phasePrompt(weekNumber, phase)
}
