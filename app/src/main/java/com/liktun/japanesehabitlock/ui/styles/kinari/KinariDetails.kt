package com.liktun.japanesehabitlock.ui.styles.kinari

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * KINARI's four drawn details.
 *
 * Everything in this file is deliberately *ruled* rather than drawn: straight lines,
 * right angles, one diamond. Wa-Modern gets its character from soft organic shapes —
 * drifting petals, a swept ring, a bouncy spring. KINARI gets its character from
 * precision, so each of these is built out of the smallest possible geometry and
 * animated with a single restrained easing curve. If something here looks hand-made,
 * it is wrong.
 */

/** Every animation in this style shares one curve and one duration. Consistency is the point. */
private const val KINARI_ANIM_MS = 420

// ────────────────────────────────────────────────────────────────────── furigana

/**
 * Text with an optional kana reading set above it — the single most important
 * typographic device in this style.
 *
 * The immersion ramp only works if a learner can still *read* the screen on the day it
 * turns Japanese, and a reading printed above the kanji is what makes that true. So
 * furigana is treated as real typesetting, not as a tooltip: it sits centred over the
 * phrase, at roughly a third of its size, in [KinariPalette.sumiSoft] so it recedes
 * without disappearing.
 *
 * The critical behaviour is the null case. When [ruby] is null — because the user is
 * still in English, or has graduated past readings — this renders the main text alone
 * with **no reserved gap and no placeholder line**. A layout that keeps ruby-sized space
 * for absent readings would make every row jump by a few pixels the week the level
 * changes, which is exactly the kind of visible seam this design cannot afford.
 *
 * @param text the phrase itself, already resolved for the current immersion level.
 * @param ruby the kana reading, or null when no reading should be shown.
 */
@Composable
fun Ruby(
  text: String,
  ruby: String?,
  modifier: Modifier = Modifier,
  fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
  lineHeight: androidx.compose.ui.unit.TextUnit = 21.sp,
  fontWeight: FontWeight = FontWeight.Medium,
  color: Color = KinariPalette.sumi,
  letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
  rubySize: androidx.compose.ui.unit.TextUnit = 9.5.sp,
  rubyColor: Color = KinariPalette.sumiSoft,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (ruby != null) {
      Text(
        text = ruby,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = rubySize,
        // Tight enough that the reading hugs the phrase and reads as one unit.
        lineHeight = rubySize * 1.05f,
        letterSpacing = 0.2.sp,
        color = rubyColor,
      )
      Spacer(Modifier.height(2.dp))
    }
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = fontWeight,
      fontSize = fontSize,
      lineHeight = lineHeight,
      letterSpacing = letterSpacing,
      color = color,
    )
  }
}

// ─────────────────────────────────────────────────────────────── progress as a rule

/**
 * Progress drawn as a printer's rule: one hairline the width of the column, filling
 * from the left in matcha, divided by ticks into one segment per blocking task.
 *
 * This replaces Wa-Modern's swept ring, and the substitution is the whole argument of
 * the style. A ring is an ornament that happens to encode a number; a ruled line with
 * tick marks is a *measurement*, which is what three-of-five actually is. The ticks
 * drop below the baseline so the rule itself stays unbroken, and the fill runs to the
 * exact tick rather than to a rounded cap — you can count the finished segments at a
 * glance without reading a single digit.
 *
 * @param done completed blocking tasks.
 * @param total blocking tasks in total; the rule is divided into this many segments.
 */
@Composable
fun KinariProgressRule(
  done: Int,
  total: Int,
  modifier: Modifier = Modifier,
) {
  val target = if (total <= 0) 1f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
  val progress by animateFloatAsState(
    targetValue = target,
    animationSpec = tween(durationMillis = KINARI_ANIM_MS, easing = FastOutSlowInEasing),
    label = "kinari-progress-rule",
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(9.dp),
  ) {
    val rule = 1.5.dp.toPx()
    val y = rule / 2f
    val tickLength = 5.dp.toPx()

    // The unfilled track: the same hairline used by every border on the screen.
    drawLine(
      color = KinariPalette.hairline,
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = rule,
      cap = StrokeCap.Butt,
    )

    // The filled portion. Butt caps so the fill ends exactly on the tick.
    if (progress > 0f) {
      drawLine(
        color = KinariPalette.matcha,
        start = Offset(0f, y),
        end = Offset(size.width * progress, y),
        strokeWidth = rule,
        cap = StrokeCap.Butt,
      )
    }

    // Segment boundaries, including both ends, so the scale is readable when empty.
    if (total > 0) {
      for (i in 0..total) {
        val fraction = i.toFloat() / total.toFloat()
        val x = (size.width * fraction).coerceIn(rule / 2f, size.width - rule / 2f)
        val passed = fraction <= progress + 0.0001f
        drawLine(
          color = if (passed) KinariPalette.matcha else KinariPalette.hairline,
          start = Offset(x, y),
          end = Offset(x, y + tickLength),
          strokeWidth = 1.dp.toPx(),
          cap = StrokeCap.Butt,
        )
      }
    }
  }
}

// ───────────────────────────────────────────────────────────────── numeric progress

/**
 * The count set as editorial numerals: `03 / 05`, thin and large, split by a hairline.
 *
 * Zero-padded and monospaced-by-habit so the glyph count never changes as the day
 * progresses — the number should tick over in place, not reflow the header. The
 * completed figure carries the weight of the statement in [KinariPalette.sumi]; the
 * total is set in [KinariPalette.sumiSoft] because it is context, not news. The
 * separating rule is a leaning hairline rather than a slash character, which keeps the
 * whole mark in the same drawn vocabulary as everything else in this file.
 */
@Composable
fun KinariNumericProgress(
  done: Int,
  total: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = done.toString().padStart(2, '0'),
      fontFamily = FontFamily.SansSerif,
      // Light 300 against the Medium/SemiBold body copy: the weight contrast is
      // what makes a two-digit number feel like a headline without shouting.
      fontWeight = FontWeight.Light,
      fontSize = 40.sp,
      lineHeight = 42.sp,
      letterSpacing = (-1).sp,
      color = KinariPalette.sumi,
    )
    Canvas(
      modifier = Modifier
        .padding(horizontal = 9.dp)
        .width(11.dp)
        .height(34.dp),
    ) {
      // A leaning hairline, drawn rather than typed, so it matches the rules elsewhere.
      drawLine(
        color = KinariPalette.hairline,
        start = Offset(size.width, 2.dp.toPx()),
        end = Offset(0f, size.height - 2.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Butt,
      )
    }
    Text(
      text = total.toString().padStart(2, '0'),
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 40.sp,
      lineHeight = 42.sp,
      letterSpacing = (-1).sp,
      color = KinariPalette.sumiSoft.copy(alpha = 0.55f),
    )
  }
}

// ─────────────────────────────────────────────────────────────────── the checkbox

/**
 * The checkbox: a nearly-sharp square that fills with matcha and strokes a geometric
 * tick along its own path.
 *
 * Material's Checkbox is not used, and neither is Wa-Modern's — that one has a 9dp
 * radius and a round-capped stroke that reads as a brush mark. Here the corner radius
 * is 2dp, which is just enough to stop the corners looking chipped at 1x, and the tick
 * uses [StrokeJoin.Miter] with a butt cap so the elbow comes to a proper point. It
 * should look printed onto the row rather than painted.
 *
 * The tick is still revealed with [PathMeasure] because a drawn line has a direction and
 * honouring it is what makes the check feel *made*; the difference from Wa-Modern is
 * that it is drawn quickly and lands hard instead of easing in.
 */
@Composable
fun KinariCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 22.dp,
) {
  val progress by animateFloatAsState(
    targetValue = if (checked) 1f else 0f,
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "kinari-checkbox",
  )
  val tickPath = remember { Path() }
  val measure = remember { PathMeasure() }
  val segment = remember { Path() }
  val interaction = remember { MutableInteractionSource() }

  Canvas(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(2.dp))
      .clickable(
        interactionSource = interaction,
        indication = null,
        role = Role.Checkbox,
      ) { onCheckedChange(!checked) },
  ) {
    val s = this.size.minDimension
    val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    val stroke = 1.dp.toPx()

    // Unchecked: a hairline square, identical in weight to every rule on the page.
    drawRoundRect(
      color = KinariPalette.hairline,
      cornerRadius = radius,
      style = Stroke(width = stroke),
      topLeft = Offset(stroke / 2f, stroke / 2f),
      size = Size(s - stroke, s - stroke),
      alpha = 1f - progress,
    )

    // Checked: a flat matcha tile. No gradient, no glow.
    if (progress > 0.001f) {
      drawRoundRect(
        color = KinariPalette.matcha,
        cornerRadius = radius,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(s - stroke, s - stroke),
        alpha = progress,
      )
    }

    if (progress > 0.02f) {
      tickPath.reset()
      tickPath.moveTo(s * 0.24f, s * 0.51f)
      tickPath.lineTo(s * 0.43f, s * 0.70f)
      tickPath.lineTo(s * 0.78f, s * 0.30f)

      measure.setPath(tickPath, false)
      segment.reset()
      // A short lead-in so the tile lands first and the tick is struck onto it.
      val drawn = ((progress - 0.20f) / 0.80f).coerceIn(0f, 1f)
      measure.getSegment(0f, measure.length * drawn, segment, true)
      drawPath(
        path = segment,
        color = KinariPalette.surface,
        style = Stroke(
          width = s * 0.09f,
          cap = StrokeCap.Butt,
          join = StrokeJoin.Miter,
        ),
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────── the one ornament

/**
 * The gold hairline: a single 1px rule with a small open diamond at its centre.
 *
 * This is the *only* decorative mark in KINARI. Wa-Modern earns its warmth with petals
 * and a kumiko lattice; this style earns its character by refusing almost all of that,
 * which means the one thing left has to be exact. The rule breaks either side of the
 * diamond rather than passing behind it, the diamond is stroked rather than filled, and
 * the whole mark sits at low alpha — from arm's length it should register as a change of
 * texture in the paper, and only resolve into an ornament when you look at it.
 */
@Composable
fun KinariGoldHairline(
  modifier: Modifier = Modifier,
  alpha: Float = 0.55f,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(8.dp),
  ) {
    drawGoldHairline(alpha)
  }
}

/** The ornament's geometry, factored out so it can be drawn inside other canvases too. */
private fun DrawScope.drawGoldHairline(alpha: Float) {
  val y = size.height / 2f
  val hair = 1.dp.toPx()
  val half = 3.dp.toPx()
  val gap = 7.dp.toPx()
  val cx = size.width / 2f
  val gold = KinariPalette.gold

  drawLine(
    color = gold,
    start = Offset(0f, y),
    end = Offset(cx - gap, y),
    strokeWidth = hair,
    alpha = alpha,
    cap = StrokeCap.Butt,
  )
  drawLine(
    color = gold,
    start = Offset(cx + gap, y),
    end = Offset(size.width, y),
    strokeWidth = hair,
    alpha = alpha,
    cap = StrokeCap.Butt,
  )

  val diamond = Path().apply {
    moveTo(cx, y - half)
    lineTo(cx + half, y)
    lineTo(cx, y + half)
    lineTo(cx - half, y)
    close()
  }
  drawPath(
    path = diamond,
    color = gold,
    style = Stroke(width = hair, join = StrokeJoin.Miter),
    alpha = alpha,
  )
}

// ────────────────────────────────────────────────────────────────── shared rules

/** A plain full-width hairline. The structural divider, as opposed to the ornament. */
@Composable
fun KinariRule(
  modifier: Modifier = Modifier,
  color: Color = KinariPalette.hairline,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(1.dp),
  ) {
    drawLine(
      color = color,
      start = Offset(0f, size.height / 2f),
      end = Offset(size.width, size.height / 2f),
      strokeWidth = size.height.coerceAtLeast(1f),
      cap = StrokeCap.Butt,
    )
  }
}

/**
 * A small square gate marker: seal red when locked, matcha when open.
 *
 * A square rather than a dot, and stroked when locked but solid when open, so the state
 * is legible without colour — the shape changes, not just the hue.
 */
@Composable
fun KinariGateMark(
  unlocked: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 9.dp,
) {
  Canvas(modifier = modifier.size(size)) {
    val s = this.size.minDimension
    val stroke = 1.5.dp.toPx()
    if (unlocked) {
      drawRect(color = KinariPalette.matcha, size = Size(s, s))
    } else {
      drawRect(
        color = KinariPalette.seal,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(s - stroke, s - stroke),
        style = Stroke(width = stroke),
      )
    }
  }
}

/** A run of tracked-out small-caps, the style's only label treatment. */
@Composable
fun KinariEyebrow(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = KinariPalette.sumiSoft,
  tracking: androidx.compose.ui.unit.TextUnit = 3.sp,
) {
  Text(
    text = text,
    modifier = modifier,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    letterSpacing = tracking,
    color = color,
  )
}

/** Vertical rhythm helper, so spacing is chosen from a scale rather than ad hoc. */
@Composable
fun KinariGap(height: Dp) {
  Spacer(Modifier.height(height))
}

/** A row of hairline-separated meta items (week, date), used in the header. */
@Composable
fun KinariMetaRow(
  items: List<String>,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
  ) {
    items.forEachIndexed { index, item ->
      if (index > 0) {
        Canvas(
          modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(10.dp),
        ) {
          drawLine(
            color = KinariPalette.hairline,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width.coerceAtLeast(1f),
          )
        }
      }
      Text(
        text = item,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        letterSpacing = 0.4.sp,
        color = KinariPalette.sumiSoft,
      )
    }
  }
}
