package com.liktun.japanesehabitlock.ui.styles.kisetsu

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.Month

/**
 * One season's complete colour world.
 *
 * KISETSU (季節) is Wa-Modern's restless sibling: the card structure, the paper ground
 * and the hairline discipline all survive, but *every* colour token is swapped four
 * times a year. So the palette is not a set of loose `val`s in an object — it is a value
 * type, resolved once per screen and threaded through every composable. That is what
 * makes the whole thing testable: give it an April date and you get spring, with no
 * clock, no locale and no hidden global to reset between tests.
 *
 * The token roles are borrowed verbatim from Wa-Modern so the two styles stay legible as
 * relatives, and so the colour discipline is enforceable by reading a diff:
 *
 * - [ground] is the page; [surface] is card paper, always a half-step brighter.
 * - [ink] is primary text, [inkSoft] every secondary line. Never pure black.
 * - [accent] / [accentDeep] mean **season and attention**: the locked gate, highlights,
 *   the "open" affordance, the kanji tile. Two weights so an accent can both wash a
 *   surface and carry text on it.
 * - [progress] means **done**, and nothing else — the ring, the checkbox fill, the
 *   completion wash, the unlocked banner.
 * - [ornament] is hairlines and dots only. Never a fill, never body text.
 *
 * Keeping "attention" and "done" on two different hues per season is why a half-finished
 * day reads instantly in every season: the unfinished half is always accent, the finished
 * half is always progress, whatever month it is.
 */
data class SeasonTheme(
  /** Romaji name, shown tracked-out in the header. */
  val name: String,
  /** The Japanese word, shown as a small gloss. */
  val japanese: String,
  /** The single kanji — the season's logo. */
  val kanji: String,
  val ground: Color,
  val surface: Color,
  val ink: Color,
  val inkSoft: Color,
  val accent: Color,
  val accentDeep: Color,
  val progress: Color,
  val ornament: Color,
)

/**
 * The four palettes.
 *
 * They are deliberately pushed apart in both hue *and* temperature — spring is a warm
 * cream that leans pink, summer is a cool bleached white that leans blue, autumn is a
 * yellowed ivory, winter is a blue-grey white. Four near-identical beiges with a
 * different accent would fail the only test that matters here: two screenshots side by
 * side should be identifiable without reading a word.
 */
object Seasons {

  /** 春 — cream paper, sakura, new green. March through May. */
  val HARU: SeasonTheme = SeasonTheme(
    name = "Haru",
    japanese = "春",
    kanji = "春",
    ground = Color(0xFFFDF6F2),
    surface = Color(0xFFFFFDFC),
    // A warm aubergine ink: black would kill the blossom, brown would muddy it.
    ink = Color(0xFF4A3540),
    inkSoft = Color(0xFF917C84),
    accent = Color(0xFFF2A8B8),
    accentDeep = Color(0xFFD9647E),
    progress = Color(0xFF7A9A6B),
    ornament = Color(0xFFC9A227),
  )

  /** 夏 — bleached cool white, indigo, deep teal. June through August. */
  val NATSU: SeasonTheme = SeasonTheme(
    name = "Natsu",
    japanese = "夏",
    kanji = "夏",
    ground = Color(0xFFF4F9FA),
    surface = Color(0xFFFFFFFF),
    // Near-black with a sea-green cast — summer ink is the wettest of the four.
    ink = Color(0xFF12303D),
    inkSoft = Color(0xFF6C8794),
    accent = Color(0xFF2E6B8A),
    accentDeep = Color(0xFF1B4A63),
    progress = Color(0xFF3E8C7F),
    ornament = Color(0xFF8FB8C9),
  )

  /** 秋 — yellowed ivory, maple, russet. September through November. */
  val AKI: SeasonTheme = SeasonTheme(
    name = "Aki",
    japanese = "秋",
    kanji = "秋",
    ground = Color(0xFFFBF4E8),
    surface = Color(0xFFFFFCF5),
    ink = Color(0xFF433021),
    inkSoft = Color(0xFF8B7355),
    accent = Color(0xFFC4622D),
    accentDeep = Color(0xFF8F4419),
    progress = Color(0xFFA8763E),
    ornament = Color(0xFFB08D57),
  )

  /** 冬 — cold white, plum, slate. December through February. */
  val FUYU: SeasonTheme = SeasonTheme(
    name = "Fuyu",
    japanese = "冬",
    kanji = "冬",
    ground = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF2B2A33),
    inkSoft = Color(0xFF7C8391),
    accent = Color(0xFF8A5A7A),
    accentDeep = Color(0xFF5F3B54),
    progress = Color(0xFF5A7184),
    ornament = Color(0xFF9AA5B1),
  )

  /** All four, in calendar order from spring. Handy for palette previews. */
  val ALL: List<SeasonTheme> = listOf(HARU, NATSU, AKI, FUYU)
}

/**
 * The season a calendar [month] belongs to.
 *
 * Meteorological boundaries (Mar/Jun/Sep/Dec) rather than solstices, because the switch
 * should land on the first of a month. Waking up on 21 March to a redecorated app is a
 * delight; waking up on 21 March to *half* a redecoration because the equinox moved is a
 * bug report.
 */
fun seasonFor(month: Month): SeasonTheme = when (month) {
  Month.MARCH, Month.APRIL, Month.MAY -> Seasons.HARU
  Month.JUNE, Month.JULY, Month.AUGUST -> Seasons.NATSU
  Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Seasons.AKI
  Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Seasons.FUYU
}

/**
 * The season a [date] falls in.
 *
 * Callers must pass `checklist.day.date` — the study day the screen is *rendering* — and
 * never `LocalDate.now()`. Reading the clock inside the UI would make the screen
 * impure, un-previewable and un-testable, and would also be subtly wrong: the study day
 * rolls over at 4 AM, so at 00:30 on 1 June the app is still showing 31 May's checklist
 * and should still be wearing spring.
 */
fun seasonForDate(date: LocalDate): SeasonTheme = seasonFor(date.month)
