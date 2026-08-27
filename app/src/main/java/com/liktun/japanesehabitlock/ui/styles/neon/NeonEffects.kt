package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.sin

// ---------------------------------------------------------------------------
// 1. GLOW
// ---------------------------------------------------------------------------

/**
 * Fakes neon bloom by stacking several strokes of the same colour at increasing
 * radius and decreasing alpha, drawn additively.
 *
 * A real tube is a thin bright core surrounded by a halo that falls off fast, so that
 * is exactly what this draws: [layers] outer strokes stepping outwards from the
 * element's edge, then a crisp hairline core on top. Additive blending
 * ([BlendMode.Plus]) means overlapping halos build up the way light actually does,
 * which is what keeps the palette from going muddy against [NeonPalette.Night].
 *
 * This is the single glow primitive in the style — every lit element goes through it.
 *
 * @param color the tube colour.
 * @param radius how far the halo reaches beyond the border.
 * @param cornerRadius corner rounding of the drawn outline.
 * @param borderWidth width of the bright core stroke; `0.dp` draws halo only.
 * @param fill optional panel fill painted underneath the glow.
 * @param intensity 0f..1f master dimmer, so callers can breathe or flicker the glow.
 * @param layers how many halo steps to stack. 3–5 reads as neon; more is wasted.
 */
fun Modifier.neonGlow(
  color: Color,
  radius: Dp = 10.dp,
  cornerRadius: Dp = 2.dp,
  borderWidth: Dp = 1.dp,
  fill: Color? = null,
  intensity: Float = 1f,
  layers: Int = 4,
): Modifier = drawBehind {
  drawNeonOutline(
    color = color,
    radiusPx = radius.toPx(),
    cornerPx = cornerRadius.toPx(),
    borderPx = borderWidth.toPx(),
    fill = fill,
    intensity = intensity,
    layers = layers,
  )
}

/**
 * The glow itself, factored out so [Canvas] code can reuse it without a Modifier.
 *
 * Drawn inset by half the core stroke so the hairline lands *on* the bounds instead
 * of straddling them, which keeps neighbouring panels optically aligned.
 */
internal fun DrawScope.drawNeonOutline(
  color: Color,
  radiusPx: Float,
  cornerPx: Float,
  borderPx: Float,
  fill: Color?,
  intensity: Float,
  layers: Int,
) {
  val dim = intensity.coerceIn(0f, 1f)
  if (dim <= 0.001f) return
  val inset = borderPx / 2f
  val topLeft = Offset(inset, inset)
  val boxSize = Size((size.width - borderPx).coerceAtLeast(0f), (size.height - borderPx).coerceAtLeast(0f))
  val corner = CornerRadius(cornerPx, cornerPx)

  if (fill != null) {
    drawRoundRect(color = fill, topLeft = topLeft, size = boxSize, cornerRadius = corner)
  }

  // Halo: widest and faintest first, so the bright steps land on top of the soft ones.
  for (layer in layers downTo 1) {
    val t = layer.toFloat() / layers
    val strokeWidth = borderPx + radiusPx * t * 2f
    // Quadratic falloff — linear alpha looks like a flat outline, not light.
    val alpha = 0.16f * (1f - t) * (1f - t) * dim + 0.03f * dim
    drawRoundRect(
      color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
      topLeft = topLeft,
      size = boxSize,
      cornerRadius = corner,
      style = Stroke(width = strokeWidth),
      blendMode = BlendMode.Plus,
    )
  }

  if (borderPx > 0f) {
    drawRoundRect(
      color = color.copy(alpha = (0.55f + 0.45f * dim).coerceIn(0f, 1f)),
      topLeft = topLeft,
      size = boxSize,
      cornerRadius = corner,
      style = Stroke(width = borderPx),
    )
  }
}

/**
 * Text bloom to match [neonGlow].
 *
 * `Shadow` gives a genuine gaussian blur under the glyphs, which is the one thing a
 * stack of strokes cannot do for text. Used on every lit label so type and panels
 * bloom with the same softness.
 */
fun neonTextGlow(color: Color, radius: Float = 16f, alpha: Float = 0.85f): Shadow =
  Shadow(color = color.copy(alpha = alpha.coerceIn(0f, 1f)), offset = Offset.Zero, blurRadius = radius)

// ---------------------------------------------------------------------------
// 2. IRREGULAR FLICKER
// ---------------------------------------------------------------------------

/** Cheap deterministic hash in 0f..1f — the stand-in for a random number generator. */
private fun hash01(seed: Int): Float {
  val s = sin(seed * 12.9898f) * 43758.5453f
  return s - floor(s)
}

/**
 * Maps a linear 0f..1f ramp onto a *stepped, irregular* brightness.
 *
 * The ramp is chopped into [steps] slots and each slot draws a fixed brightness from
 * a hash of its index, so the value **holds flat** for the length of a slot and then
 * jumps — never a smooth curve. Roughly one slot in sixteen is a full dropout (dead
 * tube), one in twelve is a stutter that snaps back mid-slot, and one in eight is a
 * brown-out; the rest sit near full with only a sliver of variance. That mix is what
 * makes it read as failing hardware rather than an animation.
 *
 * Internal + pure so the flicker curve is inspectable without a Compose runtime.
 */
internal fun irregularFlicker(ramp: Float, steps: Int): Float {
  val pos = ramp.coerceIn(0f, 0.99999f) * steps
  val index = floor(pos).toInt()
  val within = pos - index
  val roll = hash01(index)
  val jitter = hash01(index * 7 + 31)
  return when {
    // Dead tube for a whole slot.
    roll < 0.062f -> 0.06f + 0.08f * jitter
    // Off, then snaps back on part-way through the slot.
    roll < 0.140f -> if (within < 0.38f + 0.24f * jitter) 0.18f else 0.97f
    // Brown-out: dim but steady.
    roll < 0.265f -> 0.48f + 0.20f * jitter
    // Healthy, with a barely-there wobble.
    else -> 0.90f + 0.10f * jitter
  }
}

/**
 * Drives [irregularFlicker] from an infinite linear ramp.
 *
 * Deliberately **not** a sine: the animation supplies only a clock, and all of the
 * character comes from quantising it. Returned as a [State] so callers can read it
 * inside a `graphicsLayer` lambda and keep the flicker in the draw phase instead of
 * recomposing the title sixty times a second.
 */
@Composable
fun rememberNeonFlicker(periodMillis: Int = 3400, steps: Int = 26): State<Float> {
  val transition = rememberInfiniteTransition(label = "neon-flicker")
  val ramp =
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
      label = "neon-flicker-ramp",
    )
  return remember(steps) { derivedStateOf { irregularFlicker(ramp.value, steps) } }
}

/** Applies [rememberNeonFlicker] as a draw-phase alpha. Use on the main sign only. */
fun Modifier.neonFlicker(flicker: State<Float>): Modifier = graphicsLayer { alpha = flicker.value }

// ---------------------------------------------------------------------------
// 3. BREATHING PULSE
// ---------------------------------------------------------------------------

/**
 * A slow 0f..1f in-and-out used by the lock status panel.
 *
 * ~2.6s each way with [FastOutSlowInEasing] so it lingers at the extremes — a
 * standing tube warming and cooling, not a blink. The caller picks the colour
 * (magenta locked, cyan unlocked); this only supplies the envelope.
 */
@Composable
fun rememberNeonBreath(periodMillis: Int = 2600): State<Float> {
  val transition = rememberInfiniteTransition(label = "neon-breath")
  return transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(periodMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "neon-breath-value",
  )
}

/** Linear 0f..1f sweep clock, restarting — used by the charge bar and the rain. */
@Composable
fun rememberNeonSweep(periodMillis: Int): State<Float> {
  val transition = rememberInfiniteTransition(label = "neon-sweep")
  return transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
    label = "neon-sweep-value",
  )
}

// ---------------------------------------------------------------------------
// 4. CHARGING PROGRESS BAR
// ---------------------------------------------------------------------------

/**
 * A charging energy bar: dim body, hard bright leading edge, travelling scanline.
 *
 * Three separate ideas stacked in one Canvas:
 * 1. an unlit channel (violet hairline) that shows how far there is left to go;
 * 2. the charged body, a gradient that ramps from ~20% to full toward the head, so
 *    the bar looks like it is being *fed* from the left rather than simply filled;
 * 3. the leading edge — a short cap plus its own extra halo layers, glowing harder
 *    than anything else in the bar, with a narrow scanline sweeping up to meet it.
 *
 * The fill animates with an [Animatable] so ticking a task surges the bar forward
 * instead of snapping, and the sweep is clipped to the charged portion so it never
 * leaks into the empty channel.
 *
 * @param progress 0f..1f, normally `checklist.progress`.
 * @param color drive colour — cyan once unlocked, magenta while locked.
 */
@Composable
fun NeonChargeBar(
  progress: Float,
  color: Color,
  modifier: Modifier = Modifier,
  height: Dp = 12.dp,
) {
  val target = progress.coerceIn(0f, 1f)
  val animated = remember { Animatable(target) }
  LaunchedEffect(target) { animated.animateTo(target, tween(durationMillis = 620, easing = FastOutSlowInEasing)) }
  val sweep by rememberNeonSweep(periodMillis = 1900)
  val headBreath by rememberNeonBreath(periodMillis = 1100)

  Canvas(modifier = modifier.height(height)) {
    val h = size.height
    val corner = CornerRadius(h / 2f, h / 2f)
    val filled = (size.width * animated.value).coerceIn(0f, size.width)

    // 1. Unlit channel.
    drawRoundRect(
      color = NeonPalette.Panel,
      cornerRadius = corner,
    )
    drawRoundRect(
      color = NeonPalette.Violet.copy(alpha = 0.30f),
      cornerRadius = corner,
      style = Stroke(width = 1.dp.toPx()),
    )

    if (filled <= 0.5f) return@Canvas

    clipRect(right = filled) {
      // 2. Charged body — ramps toward the head.
      drawRoundRect(
        brush =
          Brush.horizontalGradient(
            colors =
              listOf(
                color.copy(alpha = 0.18f),
                color.copy(alpha = 0.55f),
                color.copy(alpha = 0.95f),
              ),
            startX = 0f,
            endX = filled,
          ),
        cornerRadius = corner,
      )

      // 2b. Travelling scanline. Runs the charged length and vanishes at both ends.
      val bandWidth = (size.width * 0.18f).coerceAtLeast(28f)
      val bandCentre = -bandWidth + sweep * (filled + bandWidth * 2f)
      drawRect(
        brush =
          Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.55f), Color.Transparent),
            startX = bandCentre - bandWidth / 2f,
            endX = bandCentre + bandWidth / 2f,
          ),
        blendMode = BlendMode.Plus,
      )
    }

    // 3. Leading edge — the brightest thing on screen, with its own bloom.
    val headX = filled.coerceAtMost(size.width - 1f)
    val headGlow = 0.75f + 0.25f * headBreath
    for (layer in 4 downTo 1) {
      val spread = h * 0.55f * layer
      drawRect(
        brush =
          Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.20f * headGlow / layer), Color.Transparent),
            startX = headX - spread,
            endX = headX + spread,
          ),
        topLeft = Offset(headX - spread, -h * 0.5f),
        size = Size(spread * 2f, h * 2f),
        blendMode = BlendMode.Plus,
      )
    }
    drawLine(
      color = NeonPalette.Text.copy(alpha = headGlow),
      start = Offset(headX, h * 0.12f),
      end = Offset(headX, h * 0.88f),
      strokeWidth = 2.dp.toPx(),
      blendMode = BlendMode.Plus,
    )
  }
}

// ---------------------------------------------------------------------------
// 5. CUSTOM CHECK MARK
// ---------------------------------------------------------------------------

/**
 * The checkbox: a hand-drawn diamond inside square brackets, not `Checkbox()`.
 *
 * Unchecked it is an unlit outline in [NeonPalette.Unlit] — visible, clearly a
 * target, but plainly *off*. Checked, the diamond fills, gains a full halo through
 * [drawNeonOutline]'s falloff, and the brackets light up with it. The transition is
 * driven by one animated 0f..1f so scale, alpha and glow all move together, which is
 * what makes a tick feel like a switch being thrown.
 */
@Composable
fun NeonDiamondCheck(
  checked: Boolean,
  color: Color,
  modifier: Modifier = Modifier,
  boxSize: Dp = 26.dp,
) {
  val lit = remember { Animatable(if (checked) 1f else 0f) }
  LaunchedEffect(checked) {
    lit.animateTo(if (checked) 1f else 0f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
  }

  Canvas(modifier = modifier.size(boxSize)) {
    val on = lit.value
    val w = size.width
    val h = size.height
    val stroke = 1.5.dp.toPx()
    val bracketColor = lerpAlpha(NeonPalette.Unlit, color, on)
    val armX = w * 0.26f
    val armY = h * 0.30f

    // Square brackets [ ] framing the diamond.
    listOf(true, false).forEach { leftSide ->
      val x0 = if (leftSide) stroke else w - stroke
      val dir = if (leftSide) 1f else -1f
      val path =
        Path().apply {
          moveTo(x0 + dir * armX, stroke)
          lineTo(x0, stroke)
          lineTo(x0, h - stroke)
          lineTo(x0 + dir * armX, h - stroke)
        }
      drawPath(path, color = bracketColor, style = Stroke(width = stroke))
      if (on > 0.01f) {
        drawPath(
          path,
          color = color.copy(alpha = 0.22f * on),
          style = Stroke(width = stroke + 6f),
          blendMode = BlendMode.Plus,
        )
      }
    }

    // The diamond. Grows slightly as it lights.
    val cx = w / 2f
    val cy = h / 2f
    val r = (h / 2f - armY * 0.45f) * (0.82f + 0.18f * on)
    val diamond =
      Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
      }
    if (on > 0.01f) {
      for (layer in 3 downTo 1) {
        drawPath(
          diamond,
          color = color.copy(alpha = 0.20f * on / layer),
          style = Stroke(width = layer * 5f),
          blendMode = BlendMode.Plus,
        )
      }
      drawPath(diamond, color = color.copy(alpha = 0.55f + 0.45f * on))
    }
    drawPath(diamond, color = lerpAlpha(NeonPalette.Unlit, NeonPalette.Text, on), style = Stroke(width = stroke))
  }
}

/** Straight-line blend between two colours; avoids pulling in the graphics lerp import. */
private fun lerpAlpha(from: Color, to: Color, t: Float): Color {
  val f = t.coerceIn(0f, 1f)
  return Color(
    red = from.red + (to.red - from.red) * f,
    green = from.green + (to.green - from.green) * f,
    blue = from.blue + (to.blue - from.blue) * f,
    alpha = from.alpha + (to.alpha - from.alpha) * f,
  )
}

// ---------------------------------------------------------------------------
// 6. ALLEY BACKDROP
// ---------------------------------------------------------------------------

/**
 * The wet-asphalt backdrop: [NeonPalette.Night], a faint perspective grid, a low
 * magenta/violet sky-glow bleeding down from the top, and a handful of rain streaks.
 *
 * Everything here sits at 2–6% alpha. It is not meant to be *seen*, only to stop the
 * background reading as flat black — the depth is what makes the lit elements feel
 * like they are hanging in a space. Cost is kept trivial: no layers, no bitmaps,
 * about thirty primitives, and one shared linear clock for the rain.
 */
@Composable
fun NeonAlleyBackdrop(modifier: Modifier = Modifier) {
  val rain by rememberNeonSweep(periodMillis = 2200)

  Box(
    modifier =
      modifier.drawBehind {
        drawRect(NeonPalette.Night)

        // Sky-glow from the signs above, and a wet-floor bounce at the bottom.
        drawRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(NeonPalette.Magenta.copy(alpha = 0.055f), Color.Transparent),
              startY = 0f,
              endY = size.height * 0.42f,
            ),
        )
        drawRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, NeonPalette.Cyan.copy(alpha = 0.045f)),
              startY = size.height * 0.62f,
              endY = size.height,
            ),
        )

        // Grid: verticals converge slightly toward the centre so the alley has walls.
        val columns = 9
        val gridColor = NeonPalette.Violet.copy(alpha = 0.055f)
        for (i in 0..columns) {
          val f = i.toFloat() / columns
          val topX = size.width * f
          val bottomX = size.width * (0.5f + (f - 0.5f) * 1.35f)
          drawLine(gridColor, Offset(topX, 0f), Offset(bottomX, size.height), strokeWidth = 1f)
        }
        val rows = 14
        for (i in 1 until rows) {
          // Squared spacing = horizon compression.
          val f = (i.toFloat() / rows) * (i.toFloat() / rows)
          val y = size.height * f
          drawLine(gridColor.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        // Rain: fixed lanes, staggered phases, all sliding on one clock.
        val streaks = 18
        for (i in 0 until streaks) {
          val laneX = size.width * hash01(i * 3 + 1)
          val phase = (rain + hash01(i * 5 + 2)) % 1f
          val len = size.height * (0.06f + 0.09f * hash01(i * 11 + 3))
          val y = -len + phase * (size.height + len * 2f)
          val tint = if (i % 3 == 0) NeonPalette.Cyan else NeonPalette.Text
          drawLine(
            color = tint.copy(alpha = 0.05f + 0.04f * hash01(i * 13 + 4)),
            start = Offset(laneX, y),
            end = Offset(laneX + size.width * 0.012f, y + len),
            strokeWidth = 1f,
          )
        }
      }
  ) {
    Box(Modifier.fillMaxSize())
  }
}

// ---------------------------------------------------------------------------
// 7. VERTICAL HANGING SIGN
// ---------------------------------------------------------------------------

/**
 * The 縦看板 — a narrow signboard bolted to the alley wall, one glyph per line.
 *
 * Reads as *hanging* rather than floating because of three details drawn by hand:
 * a mounting bracket and bolt at the top, a glowing tube border around the板 itself,
 * and a soft pool of its own colour spilling out onto the wall behind it. Each glyph
 * gets an independent, slightly out-of-phase flicker so the sign has bad segments
 * like real signage, and the whole column is dimmer than the main title so it stays
 * scenery instead of competing for the eye.
 *
 * @param glyphs one character per entry, top to bottom.
 * @param color tube colour — magenta while locked, cyan once open.
 */
@Composable
fun NeonVerticalSign(
  glyphs: List<String>,
  color: Color,
  modifier: Modifier = Modifier,
  width: Dp = 34.dp,
) {
  val flicker by rememberNeonFlicker(periodMillis = 5100, steps = 19)

  Box(modifier = modifier.width(width), contentAlignment = Alignment.TopCenter) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      // Mounting hardware: a stub of conduit and the bolt it hangs from.
      Canvas(Modifier.size(width, 14.dp)) {
        val cx = size.width / 2f
        drawLine(
          color = NeonPalette.Violet.copy(alpha = 0.45f),
          start = Offset(cx, 0f),
          end = Offset(cx, size.height),
          strokeWidth = 2.dp.toPx(),
        )
        drawCircle(
          color = NeonPalette.Violet.copy(alpha = 0.55f),
          radius = 2.5.dp.toPx(),
          center = Offset(cx, 2.5.dp.toPx()),
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
          Modifier
            // Wall spill: a wide, very faint halo under the board.
            .neonGlow(color = color, radius = 22.dp, cornerRadius = 3.dp, borderWidth = 0.dp, intensity = 0.55f)
            // The tube border and the板 fill.
            .neonGlow(
              color = color,
              radius = 9.dp,
              cornerRadius = 3.dp,
              borderWidth = 1.dp,
              fill = NeonPalette.Panel,
              intensity = 0.35f + 0.65f * flicker,
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
      ) {
        glyphs.forEachIndexed { index, glyph ->
          // Per-glyph offset keeps segments from failing in unison.
          val local = (0.55f + 0.45f * irregularFlicker((flicker + index * 0.29f) % 1f, 11))
          Text(
            text = glyph,
            color = NeonPalette.Text.copy(alpha = 0.72f + 0.28f * local),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            style =
              androidx.compose.ui.text.TextStyle(
                shadow = neonTextGlow(color, radius = 14f, alpha = 0.55f + 0.45f * local),
              ),
          )
        }
      }
    }
  }
}

