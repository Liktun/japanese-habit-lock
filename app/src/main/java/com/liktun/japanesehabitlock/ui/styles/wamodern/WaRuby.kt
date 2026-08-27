package com.liktun.japanesehabitlock.ui.styles.wamodern

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A phrase with its kana reading set above it — the typographic device that makes the
 * immersion ramp survivable.
 *
 * The whole progressive-Japanese idea only works if a learner can still *read* the
 * screen on the morning it stops being English, and a reading printed above the kanji is
 * what buys that. So furigana is treated here as real typesetting rather than as a
 * decoration bolted onto a label: it is set at roughly two-thirds the base size, in a
 * softened version of the base ink so it recedes without vanishing, and — critically —
 * it is **centred over the phrase**. Ruby that hangs off the left edge of its base text
 * reads as a separate stray line rather than as part of the word, which is the exact
 * mistake this composable exists to avoid.
 *
 * The null case is the other half of the contract. When [reading] is null — because the
 * user is still at [com.liktun.japanesehabitlock.domain.immersion.ImmersionLevel.ENGLISH],
 * or has graduated past readings at `FULL` — the base text renders **alone, with no
 * reserved gap and no placeholder line**. Holding ruby-sized space for an absent reading
 * would make every card on the screen jump by a few pixels on the week the level
 * changes, and Wa-Modern's soft stacked cards show that kind of seam badly.
 *
 * @param text the phrase itself, already resolved for the current immersion level.
 * @param reading the kana reading, or null when no reading should be shown.
 * @param style the type style for the base text; the ruby derives its own from [rubySize].
 * @param color the base text ink.
 * @param rubyColor the reading's ink, defaulted to a faded [color] so the pair stays a
 *   single visual unit even against Wa-Modern's tinted card surfaces.
 * @param rubySize the reading's type size — kept in the 9–10sp band so it never competes.
 * @param rubyGap the breathing room between reading and phrase.
 */
@Composable
fun Ruby(
  text: String,
  reading: String?,
  style: TextStyle,
  color: Color,
  rubyColor: Color = color.copy(alpha = 0.62f),
  modifier: Modifier = Modifier,
  rubySize: TextUnit = 9.5.sp,
  rubyGap: Dp = 3.dp,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (reading != null) {
      Text(
        text = reading,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = rubySize,
        // Deliberately tighter than the font size: the reading must hug the phrase and
        // read as one unit with it, not float as its own line of text.
        lineHeight = 10.sp,
        letterSpacing = 0.2.sp,
        textAlign = TextAlign.Center,
        color = rubyColor,
      )
      Spacer(Modifier.height(rubyGap))
    }
    Text(
      text = text,
      style = style,
      color = color,
    )
  }
}
