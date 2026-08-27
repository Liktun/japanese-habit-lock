package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.ui.graphics.Color

/**
 * The sumi-e 墨絵 colour set: paper, ink, and one red.
 *
 * The discipline of this style is subtractive. There are only three real colours here —
 * the paper, the ink, and the seal — and the ink appears at three dilutions the way a
 * brush loaded once and used three times would lay it down. [vermilion] is reserved for
 * the hanko seal and at most one hairline accent; [gold] is allowed exactly once on the
 * screen. Anything that needs emphasis gets it from weight, size, or empty space, never
 * from a new hue.
 */
object SumiePalette {

  /** Washi paper — the ground everything sits on. */
  val washi = Color(0xFFF4F1EA)

  /** A slightly damper fold of the same sheet, used only in gradients and rules. */
  val paperShade = Color(0xFFE8E3D8)

  /** Full-strength sumi ink: titles, glyphs, the loaded part of a brush stroke. */
  val sumiInk = Color(0xFF1C1A17)

  /** Ink once diluted: body copy, secondary lines. */
  val inkSoft = Color(0xFF4A453D)

  /** Ink twice diluted, almost water: labels, hairlines, the margin kanji. */
  val inkWash = Color(0xFF8B857A)

  /** 朱色 — the seal, and nothing else worth spending it on. */
  val vermilion = Color(0xFFC8452F)

  /** Kindachi gold. Budget: one stroke per screen. */
  val gold = Color(0xFFB8925A)
}
