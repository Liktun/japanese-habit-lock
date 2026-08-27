package com.liktun.japanesehabitlock.ui.styles.kinari

import androidx.compose.ui.graphics.Color

/**
 * The KINARI (生成り) colour tokens — "quiet stationery".
 *
 * This is Wa-Modern's cream, cooled down and de-sweetened. Where Wa-Modern spends its
 * colour on sakura washes and matcha rings, KINARI spends almost none: the page is
 * unbleached paper, the ink is a warm near-black, and every structural line on the
 * screen is the same single hairline value. The result should read like a well printed
 * notebook rather than an app skin.
 *
 * The discipline, which the whole style depends on:
 *
 * - [kinari] is the page. [surface] is paper laid on the page — nothing else is a fill.
 * - [sumi] is primary text; [sumiSoft] is *every* secondary line, without exception.
 * - [hairline] draws every border, rule, divider and tick. One value, so the grid reads
 *   as one grid.
 * - [seal] is rationed to exactly two jobs: the locked gate marker and the "open" link.
 *   It is the red stamp on an otherwise unstamped page, and it loses all its force the
 *   moment it appears a third time.
 * - [matcha] means progress and done, and nothing else.
 * - [gold] is the *only* ornament in the entire style, and only ever as a 1px rule with
 *   its centre diamond. Never a fill, never text, never a border.
 */
object KinariPalette {

  /** 生成り — unbleached paper, cooled a step from Wa-Modern's warmer cream. */
  val kinari: Color = Color(0xFFF7F4EE)

  /** Card and sheet fill: plain white paper sitting on the page. */
  val surface: Color = Color(0xFFFFFFFF)

  /** 墨 — primary text. Warm near-black, never pure #000. */
  val sumi: Color = Color(0xFF23201C)

  /** Secondary text: details, dates, captions, furigana. */
  val sumiSoft: Color = Color(0xFF5C564E)

  /** The single structural line value: borders, rules, dividers, ticks. */
  val hairline: Color = Color(0xFFDDD7CC)

  /** 朱 — the seal red. Locked state and the "open" link only. */
  val seal: Color = Color(0xFFC0483A)

  /** 抹茶 — progress and completion only. */
  val matcha: Color = Color(0xFF6E8A5F)

  /** 金 — hairline ornament only. Never a fill, never text. */
  val gold: Color = Color(0xFFB99A4B)
}
