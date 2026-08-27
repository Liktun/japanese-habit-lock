package com.liktun.japanesehabitlock.domain.immersion

/**
 * How much of the interface is written in Japanese.
 *
 * The app's whole premise is that daily contact beats intention, and the interface
 * itself is the surface the user touches most — so it graduates from English to
 * Japanese as the weeks pass. By the time the roadmap has them shadowing by ear, the
 * checklist they read every morning is also Japanese.
 *
 * The ramp is deliberately slow and always shows furigana before taking it away.
 * Kanji with no reading is not immersion for a beginner, it is a wall.
 */
enum class ImmersionLevel(val order: Int, val label: String, val summary: String) {
  /** Everything in English. The user is still learning what the app even does. */
  ENGLISH(0, "English", "Interface in English."),

  /** Section headings become Japanese: 今日, 今週, 必須. Content stays English. */
  LABELS(1, "Japanese labels", "Headings in Japanese, everything else English."),

  /** Task titles become Japanese with furigana. Their detail lines stay English. */
  TITLES(2, "Japanese titles", "Task names in Japanese with readings."),

  /** Everything Japanese, furigana still shown. */
  FULL_FURIGANA(3, "Japanese with readings", "All Japanese, readings still shown."),

  /** Everything Japanese, no training wheels. */
  FULL(4, "Japanese", "All Japanese, no readings.");

  /** True when this level is at or beyond [other]. */
  fun includes(other: ImmersionLevel): Boolean = order >= other.order

  /** Whether readings should be rendered above kanji at this level. */
  val showsFurigana: Boolean
    get() = this != FULL && this != ENGLISH

  companion object {

    /**
     * The level a given roadmap week has earned.
     *
     * Weeks 1-2 stay English: the first fortnight is when people quit, and a checklist
     * you cannot read is a reason to quit. The steps after that are wide (four weeks
     * each) because the point is that the change is barely noticeable day to day — the
     * user should discover in month three that they have been reading Japanese for
     * weeks, not be confronted with it on a Monday.
     */
    fun forWeek(weekNumber: Int): ImmersionLevel =
      when {
        weekNumber <= 2 -> ENGLISH
        weekNumber <= 4 -> LABELS
        weekNumber <= 8 -> TITLES
        weekNumber <= 12 -> FULL_FURIGANA
        else -> FULL
      }

    /**
     * The effective level, honouring a manual override.
     *
     * [pinned] exists because forced immersion is the fastest way to make someone stop
     * opening the app. If a week-9 user is drowning they can pin themselves back to
     * LABELS, and if a fast learner wants everything in Japanese on day one they can
     * have it. The automatic ramp is a default, not a punishment.
     */
    fun resolve(weekNumber: Int, pinned: ImmersionLevel? = null): ImmersionLevel =
      pinned ?: forWeek(weekNumber)

    /** The week at which [level] would be reached automatically, for previewing the ramp. */
    fun firstWeekOf(level: ImmersionLevel): Int =
      when (level) {
        ENGLISH -> 1
        LABELS -> 3
        TITLES -> 5
        FULL_FURIGANA -> 9
        FULL -> 13
      }
  }
}
