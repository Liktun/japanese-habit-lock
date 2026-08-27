package com.liktun.japanesehabitlock.ui.styles.sumizome

import androidx.compose.ui.graphics.Color

/**
 * The SUMIZOME (墨染) colour tokens — "evening ink".
 *
 * Design intent: this is WA-MODERN after sunset, not a different app. The same warm,
 * calm, paper-goods sensibility, re-grounded on charcoal-indigo instead of unbleached
 * cream. Everything that was ink on paper is now lamplight on ink.
 *
 * The discipline is identical to the daylight sibling, which is what keeps the two
 * readable as one family:
 *
 * - [night] is the page. It dominates; every other token is a small amount of light
 *   placed on top of it.
 * - [surface] / [surfaceHigh] are card fills, one and two half-steps up from [night].
 *   Separation comes from a [hairline] border, never from a drop shadow — shadows are
 *   invisible on a dark ground, so a shadowed card just looks like it is floating for
 *   no reason.
 * - [washi] is primary text and nothing else may take its place. Contrast against
 *   [night] is roughly 13:1, comfortably past WCAG AAA for body copy.
 * - [washiSoft] is strictly secondary: furigana readings, quiet captions, inert rules.
 *   Nothing the user *must* read is allowed to live here.
 * - [matchaNight] means **progress and done**, and nothing else.
 * - [sakuraNight] is the accent: the locked state, "open" affordances, seasonal marks.
 * - [goldLeaf] is ornament (the kumiko lattice, checkpoint bullets, the ring's tip
 *   glint) plus the one celebration colour, used when the gate opens.
 *
 * Deliberately *not* present: any saturated cyan or magenta, any additive glow stack.
 * The reference is a paper lantern in a dark room, not a neon sign in an alley.
 */
object SumizomePalette {

  /** 墨 — the charcoal-indigo ground. The page, and most of the screen. */
  val night: Color = Color(0xFF161A21)

  /** Card fill, one half-step up from [night]. */
  val surface: Color = Color(0xFF1F242D)

  /** The lifted card fill, for the header and anything that should sit forward. */
  val surfaceHigh: Color = Color(0xFF272D38)

  /** 和紙 — warm off-white. Primary text. */
  val washi: Color = Color(0xFFEDE7DA)

  /** Secondary text: furigana, captions, quiet rules. Never load-bearing. */
  val washiSoft: Color = Color(0xFFA79F92)

  /** 夜桜 — the night-blooming accent. Locked state, open affordances. */
  val sakuraNight: Color = Color(0xFFE08A9E)

  /** 抹茶 at night — progress, completion washes, the ring. */
  val matchaNight: Color = Color(0xFF8FB37A)

  /** 金箔 — ornament, and the unlocked celebration. Warm, not yellow-hot. */
  val goldLeaf: Color = Color(0xFFD4AF6A)

  /** The 1dp separating line that replaces elevation on a dark ground. */
  val hairline: Color = Color(0xFF333A45)
}
