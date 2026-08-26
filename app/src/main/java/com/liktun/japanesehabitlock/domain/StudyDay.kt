package com.liktun.japanesehabitlock.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Which "study day" a moment in time belongs to.
 *
 * This is not the same as the calendar date. A habit app that flips at midnight
 * punishes exactly the person it's meant to help: finishing a shadowing session at
 * 00:30 would land on a fresh, empty checklist and re-lock every distraction app.
 *
 * So the day boundary is shifted to [DEFAULT_ROLLOVER_HOUR] (4 AM). Anything before
 * that still counts toward the previous day.
 */
data class StudyDay(val date: LocalDate) : Comparable<StudyDay> {

  /** ISO-8601 (`2026-08-24`). This is the persisted form. */
  val key: String
    get() = date.toString()

  override fun compareTo(other: StudyDay): Int = date.compareTo(other.date)

  companion object {
    const val DEFAULT_ROLLOVER_HOUR: Int = 4

    /** The study day that [instant] falls into, for a given [zone]. */
    fun at(instant: Instant, zone: ZoneId, rolloverHour: Int = DEFAULT_ROLLOVER_HOUR): StudyDay {
      require(rolloverHour in 0..23) { "rolloverHour must be in 0..23, was $rolloverHour" }
      return StudyDay(instant.atZone(zone).minusHours(rolloverHour.toLong()).toLocalDate())
    }

    /** Parses a persisted [key]; returns null for missing or corrupt values. */
    fun parseOrNull(key: String?): StudyDay? {
      if (key.isNullOrBlank()) return null
      return runCatching { StudyDay(LocalDate.parse(key)) }.getOrNull()
    }
  }
}

/**
 * Which week of the roadmap [today] falls in, counting from [start].
 *
 * Weeks are 1-based: the start day itself is week 1. A [today] before [start]
 * (possible if the device clock moves backwards) clamps to week 1 rather than
 * returning zero or a negative week.
 */
fun weekNumberFor(start: StudyDay, today: StudyDay): Int {
  val elapsedDays = ChronoUnit.DAYS.between(start.date, today.date)
  if (elapsedDays < 0) return 1
  return (elapsedDays / 7 + 1).toInt()
}
