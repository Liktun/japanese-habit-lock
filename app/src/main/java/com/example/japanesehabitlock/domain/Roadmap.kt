package com.example.japanesehabitlock.domain

/**
 * How far along the speaking progression the user has opted in to.
 *
 * Advancing is deliberately a *manual* choice rather than a date calculation. The
 * roadmap describes both transitions as felt checkpoints ("around week 4-6",
 * "whenever speaking-from-shadowing feels comfortable"), which the app has no way
 * to detect. So the app suggests, and the user decides.
 */
enum class Phase(val order: Int, val label: String, val summary: String) {
  SHADOWING(0, "Phase 1 — Shadowing", "Input and imitation. No speaking pressure yet."),
  SELF_TALK(1, "Phase 1.5 — Self-talk", "Shadowing, plus production practice on your own."),
  SPEAKING(2, "Phase 2 — Speaking", "Self-talk, plus optional conversation partners.");

  /** True when this phase is at or beyond [other], so [other]'s tasks are active. */
  fun includes(other: Phase): Boolean = order >= other.order

  /** The next phase to opt in to, or null when already at the end. */
  fun next(): Phase? = entries.firstOrNull { it.order == order + 1 }
}

/**
 * One line item on the daily checklist.
 *
 * [blocking] is the important flag: only blocking tasks gate the distraction apps.
 * Optional tasks are tracked and displayed but never keep anything locked.
 */
data class RoadmapTask(
  val id: String,
  val title: String,
  val detail: String,
  val blocking: Boolean,
  val introducedIn: Phase,
)

/**
 * The roadmap content itself: which tasks exist, when they enter the checklist,
 * and what the weekly framing looks like.
 *
 * Task ids are stable strings because they are persisted. Renaming a [RoadmapTask.title]
 * is safe; changing an id would silently orphan a user's completion history.
 */
object Roadmap {

  const val ID_WANIKANI = "wanikani_reviews"
  const val ID_BUNPRO = "bunpro_reviews"
  const val ID_SHADOWING = "shadowing_session"
  const val ID_IMMERSION = "immersion"
  const val ID_SELF_TALK = "self_talk"
  const val ID_CONVERSATION = "conversation_partner"

  /** Every task across every phase, in the order they should appear. */
  val ALL_TASKS: List<RoadmapTask> =
    listOf(
      RoadmapTask(
        id = ID_WANIKANI,
        title = "WaniKani reviews cleared",
        detail = "Review queue down to zero.",
        blocking = true,
        introducedIn = Phase.SHADOWING,
      ),
      RoadmapTask(
        id = ID_BUNPRO,
        title = "Bunpro grammar reviews done",
        detail = "Clear the grammar queue.",
        blocking = true,
        introducedIn = Phase.SHADOWING,
      ),
      RoadmapTask(
        id = ID_SHADOWING,
        title = "Shadowing session",
        detail = "15-20 min. Stay on the same clip for 3-4 days running.",
        blocking = true,
        introducedIn = Phase.SHADOWING,
      ),
      RoadmapTask(
        id = ID_IMMERSION,
        title = "Immersion reading / listening",
        detail = "NHK Easy News, or anime with Japanese subtitles.",
        blocking = false,
        introducedIn = Phase.SHADOWING,
      ),
      RoadmapTask(
        id = ID_SELF_TALK,
        title = "Self-talk production practice",
        detail = "Narrate your day out loud. Answer questions about your clip in your own words.",
        blocking = true,
        introducedIn = Phase.SELF_TALK,
      ),
      RoadmapTask(
        id = ID_CONVERSATION,
        title = "iTalki / HelloTalk",
        detail = "A task, not a hard requirement.",
        blocking = false,
        introducedIn = Phase.SPEAKING,
      ),
    )

  /** The weekly self-check. Explicitly not a daily gate. */
  val WEEKLY_CHECKPOINTS: List<String> =
    listOf(
      "Review your WaniKani / Bunpro retention stats.",
      "Revisit the hardest shadowing clip from the week.",
    )

  /** The tasks active for a given phase. */
  fun tasksFor(phase: Phase): List<RoadmapTask> = ALL_TASKS.filter { phase.includes(it.introducedIn) }

  /** The blocking subset — these are what actually hold the gate shut. */
  fun blockingTasksFor(phase: Phase): List<RoadmapTask> = tasksFor(phase).filter { it.blocking }

  /**
   * The single source of truth for the gate.
   *
   * Both the UI and the persisted mirror flag (the one a future Accessibility
   * Service reads) route through here, so they cannot drift apart.
   */
  fun isUnlocked(phase: Phase, completedTaskIds: Set<String>): Boolean =
    blockingTasksFor(phase).all { it.id in completedTaskIds }

  /** A one-line framing for the current week. */
  fun focusFor(weekNumber: Int): String =
    when (weekNumber) {
      1 -> "Get the loop running. Pick one clip and stay on it all week."
      2 -> "Tighten rhythm and pitch. Keep each clip 3-4 days before swapping."
      3 -> "Drop the text for the last few reps — shadow by ear."
      4 -> "Steady state. Notice which sounds still slip, and drill those."
      else -> "Steady state. Keep the loop, and watch for the itch to start speaking."
    }

  /**
   * A nudge toward the next phase, or null when there's nothing to suggest.
   *
   * These are suggestions only — nothing advances automatically.
   */
  fun phasePrompt(weekNumber: Int, phase: Phase): String? =
    when (phase) {
      Phase.SHADOWING ->
        if (weekNumber >= 4) {
          "You're on week $weekNumber. If shadowing feels automatic, add self-talk: " +
            "narrate your day out loud and answer questions about your clip in your own words."
        } else {
          null
        }
      Phase.SELF_TALK ->
        "When speaking from shadowing feels comfortable, add iTalki or HelloTalk — " +
          "as a task, not a hard requirement."
      Phase.SPEAKING -> null
    }
}
