package com.liktun.japanesehabitlock.ui.styles.sumie

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
 * Furigana for the sumi-e sheet: the reading set *above* the phrase, as typesetting.
 *
 * The immersion ramp turns this screen Japanese one layer at a time, and the only thing
 * that makes that survivable for a learner is a reading printed over the kanji. In this
 * style that reading has to look **printed**, not annotated — so it keeps
 * [FontFamily.Serif] and a little tracking rather than dropping to a sans-serif
 * "tooltip" voice, and it is inked at [SUMIE_RUBY_INK] (twice-diluted wash) so it never
 * reads as a second line of body copy competing with the title.
 *
 * Two behaviours matter more than the look:
 *
 * 1. **Centred.** The reading is centred over its own base text, not left-aligned to the
 *    column. A left-aligned ruby detaches from the word it belongs to and starts reading
 *    as a stray caption; centred, it reads as one typeset unit. Because the [Column]
 *    wraps its content, "centred on the base text" and "centred on the block" are the
 *    same thing here — so callers must never hand this a `weight()` modifier, which
 *    would stretch the block and float the ruby in the middle of the row.
 * 2. **No reserved gap.** When [reading] is null nothing is emitted above the text — no
 *    placeholder, no spacer, no fixed ruby line box. Weeks 1-2 and week 13+ both render
 *    with no reading, and if this reserved space for the absent line, every row on the
 *    page would shift the morning the level changed. The layout must not jump.
 *
 * @param text the phrase, already resolved for the current immersion level.
 * @param reading the kana reading, or null when the level does not show readings.
 * @param style the base text's type. The ruby uses [SUMIE_RUBY_STYLE] regardless, so a
 *   caller cannot accidentally give the reading a display-sized face.
 * @param color base ink.
 * @param rubyColor reading ink. Callers in this style should pass [SUMIE_RUBY_INK]; the
 *   default is only a safe fallback and can still be too dark for a full-ink base.
 * @param baseModifier applied to the base text *only*, never to the block.
 *   [inkStrikeThrough] draws its wavy line relative to the height of whatever it is
 *   attached to, so striking the whole column would put the line through the reading at
 *   a bogus height. This is how a caller strikes the word and leaves the kana alone.
 */
@Composable
fun Ruby(
  text: String,
  reading: String?,
  style: TextStyle,
  color: Color,
  rubyColor: Color = color.copy(alpha = 0.62f),
  modifier: Modifier = Modifier,
  baseModifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (reading != null) {
      Text(
        text = reading,
        style = SUMIE_RUBY_STYLE,
        color = rubyColor,
        textAlign = TextAlign.Center,
      )
      // 1dp, not a typographic gap: the reading should sit *on* the phrase.
      Spacer(Modifier.height(1.dp))
    }
    Text(text = text, style = style, color = color, modifier = baseModifier)
  }
}

/**
 * The ruby face: serif, ~9.5sp, and a line height barely larger than the glyphs.
 *
 * The tight [TextStyle.lineHeight] is what stops the reading from opening a whole extra
 * line of leading above every title — at 10sp against a 9.5sp face the kana hug the
 * phrase, which is how furigana is actually set on a printed page.
 */
internal val SUMIE_RUBY_STYLE: TextStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 9.5.sp,
    lineHeight = 10.sp,
    letterSpacing = 0.6.sp,
  )

/**
 * The one ink readings are allowed to be: twice-diluted wash, never full sumi.
 *
 * Black furigana would sit at the same weight as the title it explains and the row would
 * read as two headlines. It is also held clear of [SumiePalette.inkWash]'s full strength
 * so it stays quieter than the vertical 習慣 margin column — the margin motif is the
 * page's texture and the ruby must not start competing with it for the eye.
 */
internal val SUMIE_RUBY_INK: Color = SumiePalette.inkWash.copy(alpha = 0.82f)

// ---------------------------------------------------------------------------------
//  Level probes
// ---------------------------------------------------------------------------------

/**
 * True while section headings are still English.
 *
 * The sheet garnishes its English labels with a small kanji beside them (今週, 本日).
 * Once the heading *itself* is Japanese that garnish becomes a duplicate of the word it
 * is standing next to, so callers use this to drop it rather than print 本日 今日.
 */
internal fun Immersion.headingsAreEnglish(): Boolean =
  !level.includes(ImmersionPlan.SECTION_LABELS)
