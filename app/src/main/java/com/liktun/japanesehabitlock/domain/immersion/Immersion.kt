package com.liktun.japanesehabitlock.domain.immersion

/**
 * Which immersion level each part of the interface switches to Japanese at.
 *
 * Held in one place so the ramp is auditable: you can read the whole progression here
 * instead of hunting for `if (level >= …)` scattered through composables. The ordering
 * is the pedagogy — labels first because they repeat daily and are guessable from
 * position, task details last because they carry the actual instructions.
 */
object ImmersionPlan {

  /** Section headings: 今日, 今週, 必須. Short, repetitive, positionally obvious. */
  val SECTION_LABELS = ImmersionLevel.LABELS

  /** Gate state words: 施錠中 / 解錠. Reinforced by colour and icon, so safe early. */
  val GATE_STATE = ImmersionLevel.LABELS

  /** Task names. The user already knows what their own daily tasks are. */
  val TASK_TITLES = ImmersionLevel.TITLES

  /** Buttons. Switched with titles; the position teaches the meaning. */
  val ACTIONS = ImmersionLevel.TITLES

  /** Task instructions. Last, because getting these wrong means doing the wrong work. */
  val TASK_DETAILS = ImmersionLevel.FULL_FURIGANA

  /** Coaching prose: weekly focus, checkpoints, phase prompts. Longest, so last. */
  val PROSE = ImmersionLevel.FULL_FURIGANA
}

/**
 * Resolves phrases for a given immersion level.
 *
 * A tiny value type rather than a set of free functions so a composable can take one
 * parameter and every call inside it is consistent — mixing two levels on one screen
 * would look like a bug.
 */
data class Immersion(val level: ImmersionLevel) {

  fun text(phrase: Phrase, from: ImmersionLevel): String = phrase.textAt(level, from)

  fun ruby(phrase: Phrase, from: ImmersionLevel): String? = phrase.rubyAt(level, from)

  fun sectionLabel(phrase: Phrase): String = text(phrase, ImmersionPlan.SECTION_LABELS)

  fun sectionRuby(phrase: Phrase): String? = ruby(phrase, ImmersionPlan.SECTION_LABELS)

  fun gate(phrase: Phrase): String = text(phrase, ImmersionPlan.GATE_STATE)

  fun taskTitle(id: String, fallback: String): String =
    text(ImmersionStrings.taskTitle(id, fallback), ImmersionPlan.TASK_TITLES)

  fun taskTitleRuby(id: String, fallback: String): String? =
    ruby(ImmersionStrings.taskTitle(id, fallback), ImmersionPlan.TASK_TITLES)

  fun taskDetail(id: String, fallback: String): String =
    text(ImmersionStrings.taskDetail(id, fallback), ImmersionPlan.TASK_DETAILS)

  fun action(phrase: Phrase): String = text(phrase, ImmersionPlan.ACTIONS)

  /**
   * A one-line description of what just changed, or null when nothing did.
   *
   * Shown once when a week crosses a threshold. Without it the interface silently
   * mutating looks like a bug rather than a milestone — and the milestone is the
   * reward, so it is worth naming.
   */
  fun levelUpNotice(previous: ImmersionLevel?): String? {
    if (previous == null || previous == level || !level.includes(previous)) return null
    return when (level) {
      ImmersionLevel.LABELS -> "Headings are in Japanese from now on."
      ImmersionLevel.TITLES -> "Your tasks are in Japanese now — readings included."
      ImmersionLevel.FULL_FURIGANA -> "Everything is Japanese now. Readings still shown."
      ImmersionLevel.FULL -> "Readings are off. You are reading Japanese."
      ImmersionLevel.ENGLISH -> null
    }
  }

  companion object {
    fun forWeek(weekNumber: Int, pinned: ImmersionLevel? = null): Immersion =
      Immersion(ImmersionLevel.resolve(weekNumber, pinned))
  }
}
