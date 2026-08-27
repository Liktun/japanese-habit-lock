package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens for **Style 2 — NEON YOKOCHO ネオン横丁**.
 *
 * A Shinjuku back-alley at 2am: almost everything is [Night], panels are barely
 * lighter than the void, and all the energy comes from four saturated tube colours
 * that are used with strict discipline rather than sprinkled everywhere:
 *
 * - [Magenta] — locked / urgent / "you still owe the gate something".
 * - [Cyan] — done / unlocked / "the alley is open".
 * - [Amber] — optional and warnings; the lantern over the door, not the sign.
 * - [Violet] — secondary structure: section rules, week framing, checkpoints.
 *
 * Hexes are taken verbatim from `docs/frontend-styles.md`.
 */
object NeonPalette {

  /** Wet asphalt at night. The background of every surface in this style. */
  val Night: Color = Color(0xFF0A0812)

  /** Panel fill — one step above the void so borders read as light, not chrome. */
  val Panel: Color = Color(0xFF14101F)

  /** Locked / urgent. */
  val Magenta: Color = Color(0xFFFF2D95)

  /** Done / unlocked. */
  val Cyan: Color = Color(0xFF00E5FF)

  /** Optional / warning. */
  val Amber: Color = Color(0xFFFFB020)

  /** Secondary structure. */
  val Violet: Color = Color(0xFF9D4EDD)

  /** Primary text — very slightly violet-shifted white so it sits in the palette. */
  val Text: Color = Color(0xFFF0EBFF)

  // ---- derived, non-token conveniences -------------------------------------
  // Deliberately expressed as alphas of the tokens above so the palette stays
  // exactly the seven documented hexes.

  /** Body copy and detail lines. */
  val TextDim: Color = Text.copy(alpha = 0.62f)

  /** Metadata, unlit sign glass, disabled affordances. */
  val TextFaint: Color = Text.copy(alpha = 0.34f)

  /** Unlit neon tube: the colour a border has before its element matters. */
  val Unlit: Color = Violet.copy(alpha = 0.28f)
}
