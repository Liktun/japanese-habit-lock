package com.example.japanesehabitlock.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyDayTest {

  private val utc = ZoneId.of("UTC")

  private fun dayAt(iso: String, zone: ZoneId = utc) = StudyDay.at(Instant.parse(iso), zone)

  @Test
  fun `midday belongs to that calendar date`() {
    assertEquals(LocalDate.of(2026, 8, 24), dayAt("2026-08-24T12:00:00Z").date)
  }

  @Test
  fun `after midnight but before the 4am cutoff still counts as the previous day`() {
    // The whole point of the rollover: finishing a session at 01:30 must not
    // wipe the checklist and re-lock everything.
    assertEquals(LocalDate.of(2026, 8, 23), dayAt("2026-08-24T01:30:00Z").date)
  }

  @Test
  fun `one second before the cutoff is still the previous day`() {
    assertEquals(LocalDate.of(2026, 8, 23), dayAt("2026-08-24T03:59:59Z").date)
  }

  @Test
  fun `exactly the cutoff starts the new day`() {
    assertEquals(LocalDate.of(2026, 8, 24), dayAt("2026-08-24T04:00:00Z").date)
  }

  @Test
  fun `time zone is respected`() {
    // 23:00 UTC is already 08:00 the next morning in Tokyo, so it is a new study day there.
    val instant = Instant.parse("2026-08-24T23:00:00Z")
    assertEquals(LocalDate.of(2026, 8, 24), StudyDay.at(instant, utc).date)
    assertEquals(LocalDate.of(2026, 8, 25), StudyDay.at(instant, ZoneId.of("Asia/Tokyo")).date)
  }

  @Test
  fun `a custom rollover hour is honoured`() {
    assertEquals(LocalDate.of(2026, 8, 23), StudyDay.at(Instant.parse("2026-08-24T05:00:00Z"), utc, rolloverHour = 6).date)
    assertEquals(LocalDate.of(2026, 8, 24), StudyDay.at(Instant.parse("2026-08-24T05:00:00Z"), utc, rolloverHour = 0).date)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `an out of range rollover hour is rejected`() {
    StudyDay.at(Instant.parse("2026-08-24T05:00:00Z"), utc, rolloverHour = 24)
  }

  @Test
  fun `key round trips`() {
    val day = dayAt("2026-08-24T12:00:00Z")
    assertEquals("2026-08-24", day.key)
    assertEquals(day, StudyDay.parseOrNull(day.key))
  }

  @Test
  fun `corrupt or missing keys parse to null rather than throwing`() {
    assertNull(StudyDay.parseOrNull(null))
    assertNull(StudyDay.parseOrNull(""))
    assertNull(StudyDay.parseOrNull("   "))
    assertNull(StudyDay.parseOrNull("not-a-date"))
    assertNull(StudyDay.parseOrNull("2026-13-45"))
  }

  @Test
  fun `week number counts from the start day`() {
    val start = StudyDay(LocalDate.of(2026, 8, 24))
    fun weekAfter(days: Long) = weekNumberFor(start, StudyDay(start.date.plusDays(days)))

    assertEquals(1, weekAfter(0))
    assertEquals(1, weekAfter(6))
    assertEquals(2, weekAfter(7))
    assertEquals(2, weekAfter(13))
    assertEquals(3, weekAfter(14))
    assertEquals(5, weekAfter(28))
  }

  @Test
  fun `a clock that moves backwards clamps to week 1 instead of going negative`() {
    val start = StudyDay(LocalDate.of(2026, 8, 24))
    val earlier = StudyDay(LocalDate.of(2026, 8, 1))
    assertEquals(1, weekNumberFor(start, earlier))
  }
}
