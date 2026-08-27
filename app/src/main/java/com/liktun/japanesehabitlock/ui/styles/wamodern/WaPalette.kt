package com.liktun.japanesehabitlock.ui.styles.wamodern

import androidx.compose.ui.graphics.Color

/**
 * The WA-MODERN (和モダン) colour tokens.
 *
 * Design intent: an unbleached-paper ground (kinari) carries almost the whole screen,
 * and the accents are used like seasoning rather than the meal —
 *
 * - [matcha] / [matchaDeep] mean **progress and done**, and nothing else.
 * - [sakura] / [sakuraDeep] are the seasonal accent: petals, highlights, "open" affordances.
 * - [indigo] is primary text; [warmGray] is every secondary line.
 * - [goldLeaf] is reserved for hairline decoration (the kumiko lattice, rules, bullets)
 *   and is never used as a fill or as text colour.
 *
 * Hexes are taken verbatim from `docs/frontend-styles.md`, Style 3.
 */
object WaPalette {

  /** 生成り — unbleached paper. The page ground. */
  val kinari: Color = Color(0xFFFBF7F0)

  /** Card and sheet fill, sitting a half-step brighter than [kinari]. */
  val surface: Color = Color(0xFFFFFFFF)

  /** 桜 — the soft petal pink. Accent washes, petals, highlights. */
  val sakura: Color = Color(0xFFF2A8B8)

  /** Deeper sakura, for accent text and outlines that need to actually read. */
  val sakuraDeep: Color = Color(0xFFD9647E)

  /** 抹茶 — progress green. Rings, fills, completion washes. */
  val matcha: Color = Color(0xFF7A9A6B)

  /** Deeper matcha, for text and strokes on light matcha fills. */
  val matchaDeep: Color = Color(0xFF4F6B45)

  /** 藍色 — the primary text colour. Warm-cool navy, never pure black. */
  val indigo: Color = Color(0xFF2E4057)

  /** 金箔 — hairline decoration only. */
  val goldLeaf: Color = Color(0xFFC9A227)

  /** Secondary text and quiet rules. */
  val warmGray: Color = Color(0xFF8A8078)
}
