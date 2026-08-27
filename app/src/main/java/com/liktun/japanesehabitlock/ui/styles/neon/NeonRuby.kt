package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan

/**
 * Furigana for the alley: the reading set above the phrase, deliberately *unlit*.
 *
 * Every other piece of type in this style has a [neonTextGlow] behind it, because the
 * conceit is that the screen is made of tubes. The reading is the one exception, and
 * that is the whole design decision here: a glowing kana line above a glowing title is
 * two light sources 10sp apart, which turns into a smear rather than a reading. So the
 * ruby is monospace like everything else, but flat — no shadow, no accent colour, just
 * dim white — which reads as a stencilled annotation on the sign rather than part of it.
 * It should be legible when you look for it and invisible when you are not.
 *
 * Two behaviours matter more than the look:
 *
 * 1. **Centred.** The reading is centred over its own base text via
 *    [Alignment.CenterHorizontally], not left-aligned to whatever container it lands in.
 *    A left-aligned ruby detaches from its word and reads as a stray label. Because the
 *    [Column] wraps its content, callers must not hand this a `weight()` modifier — that
 *    would stretch the block and strand the kana in the middle of the panel.
 * 2. **No reserved gap.** With [reading] null, nothing at all is emitted above the text:
 *    no placeholder line, no spacer, no fixed ruby line box. Weeks 1-2 and week 13+ both
 *    render readingless, and reserving space for the absent line would make every panel
 *    on the screen jump the week the level crossed a threshold.
 *
 * @param text the phrase, already resolved for the current immersion level.
 * @param reading the kana reading, or null when the level does not show readings.
 * @param style the base text's type, glow and all.
 * @param color base colour, normally a lit tube colour.
 * @param rubyColor reading colour. Neon callers should pass [NEON_RUBY_INK]; the default
 *   derives from [color], which on a saturated magenta would be far too loud.
 */
@Composable
fun Ruby(
  text: String,
  reading: String?,
  style: TextStyle,
  color: Color,
  rubyColor: Color = color.copy(alpha = 0.62f),
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (reading != null) {
      Text(
        text = reading,
        style = NEON_RUBY_STYLE,
        color = rubyColor,
        textAlign = TextAlign.Center,
      )
      // 1dp: the reading sits on the sign, it does not float above it.
      Spacer(Modifier.height(1.dp))
    }
    Text(text = text, style = style, color = color)
  }
}

/**
 * The ruby face: monospace, 9.5sp, tight leading, and explicitly **no shadow**.
 *
 * The absent `shadow` is load-bearing — [NeonLabelStyle] and friends all carry one, and
 * copying any of them here would give the readings a halo. The tight
 * [TextStyle.lineHeight] keeps the kana hugging the phrase instead of opening a full
 * line of leading above every task title.
 */
internal val NEON_RUBY_STYLE: TextStyle =
  TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 9.5.sp,
    lineHeight = 10.sp,
    letterSpacing = 0.3.sp,
  )

/**
 * The one colour readings are allowed: unlit white, dimmer than body copy.
 *
 * Never a tube colour. Magenta or cyan kana would compete with the sign they annotate
 * and, worse, would imply a state meaning this style reserves for colour alone.
 */
internal val NEON_RUBY_INK: Color = NeonPalette.Text.copy(alpha = 0.42f)

// ---------------------------------------------------------------------------
// Level probes — when the bracketed kanji garnish must go
// ---------------------------------------------------------------------------

/**
 * True while section headings are still English, i.e. while 「今日」TODAY makes sense.
 *
 * This style garnishes its English signage with a bracketed kanji standing next to the
 * word — 「必須」REQUIRED, 「点検」WEEKLY CHECKPOINT. That reads as a bilingual shopfront
 * exactly as long as the heading beside it is English. The moment the immersion ramp
 * turns the heading itself into 必須, the garnish is the same word twice: 「今日」今日,
 * which is a rendering bug wearing a costume. So callers gate the garnish on this and
 * let the heading stand alone once it is Japanese.
 */
internal fun Immersion.headingsAreEnglish(): Boolean =
  !level.includes(ImmersionPlan.SECTION_LABELS)

/** The same rule for the gate readout's 「施錠」/「解錠」 garnish. */
internal fun Immersion.gateWordIsEnglish(): Boolean =
  !level.includes(ImmersionPlan.GATE_STATE)
