package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------
//  Noise — the small irregularities that separate a brush from a rectangle.
// ---------------------------------------------------------------------------------

/**
 * A fixed strip of smooth pseudo-random values in roughly -1f..1f.
 *
 * Remembered rather than regenerated so a stroke does not shimmer between frames:
 * a brush hair sits where it sits. [seed] lets each stroke on screen be uniquely
 * ragged while staying stable across recomposition.
 */
@Composable
internal fun rememberInkNoise(seed: Int, count: Int = 24): FloatArray = remember(seed, count) {
  val random = Random(seed)
  val raw = FloatArray(count) { random.nextFloat() * 2f - 1f }
  // One box-blur pass: raw white noise reads as static, smoothed noise reads as fibre.
  FloatArray(count) { i ->
    val a = raw[(i - 1 + count) % count]
    val b = raw[i]
    val c = raw[(i + 1) % count]
    (a + b + b + c) / 4f
  }
}

/** Linear-interpolated lookup into [noise] for a 0f..1f position. */
internal fun noiseAt(noise: FloatArray, t: Float): Float {
  if (noise.isEmpty()) return 0f
  val x = (t.coerceIn(0f, 1f) * (noise.size - 1))
  val i = x.toInt().coerceIn(0, noise.size - 1)
  val j = (i + 1).coerceAtMost(noise.size - 1)
  val f = x - i
  return noise[i] * (1f - f) + noise[j] * f
}

// ---------------------------------------------------------------------------------
//  The core primitive: a filled ink stroke with a real width profile.
// ---------------------------------------------------------------------------------

/**
 * Fills a stroke whose *outline* is built from a centreline and a varying half-width.
 *
 * This is the whole trick of the style. A `Stroke` with a fixed width is a machine
 * mark; a brush is a shape whose thickness rises as the tip is pressed down and falls
 * as it is lifted, and whose edges break up where the hairs run dry. So the geometry
 * is walked twice — up one side, back down the other — and closed into a polygon.
 *
 * @param centreline maps 0f..1f along the stroke to a point.
 * @param halfWidth maps 0f..1f along the stroke to half the stroke's thickness there.
 * @param end how much of the stroke to draw, 0f..1f — this is what animates.
 */
internal fun DrawScope.drawInkStroke(
  centreline: (Float) -> Offset,
  halfWidth: (Float) -> Float,
  color: Color,
  end: Float = 1f,
  samples: Int = 56,
  alpha: Float = 1f,
) {
  val stop = end.coerceIn(0f, 1f)
  if (stop <= 0.004f) return

  val points = ArrayList<Offset>(samples + 1)
  val widths = ArrayList<Float>(samples + 1)
  for (i in 0..samples) {
    val t = stop * i / samples
    points += centreline(t)
    widths += halfWidth(t).coerceAtLeast(0f)
  }

  // Perpendicular of the local tangent, so the width is applied across the direction
  // of travel rather than blindly vertically. Matters as soon as a stroke curves.
  fun normalAt(i: Int): Offset {
    val a = points[(i - 1).coerceAtLeast(0)]
    val b = points[(i + 1).coerceAtMost(points.lastIndex)]
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
    return Offset(-dy / len, dx / len)
  }

  val path = Path()
  val first = points.first() + normalAt(0) * widths.first()
  path.moveTo(first.x, first.y)
  for (i in points.indices) {
    val p = points[i] + normalAt(i) * widths[i]
    path.lineTo(p.x, p.y)
  }
  for (i in points.indices.reversed()) {
    val p = points[i] - normalAt(i) * widths[i]
    path.lineTo(p.x, p.y)
  }
  path.close()
  drawPath(path, color, alpha = alpha)
}

// ---------------------------------------------------------------------------------
//  Brush-stroke progress
// ---------------------------------------------------------------------------------

/**
 * Progress drawn as a single loaded brush stroke rather than a bar.
 *
 * The mark tapers to nothing at both ends the way a brush does when it is set down and
 * lifted, swells slightly past the middle where the hand slows, and carries a couple of
 * dry-brush gaps (かすれ *kasure*) scraped out of it in paper colour. The full length of
 * the stroke is always implied by a very pale ghost so the user can read "how far along"
 * spatially, which is what a progress indicator is for.
 *
 * @param progress 0f..1f over the blocking tasks. Animated with `animateFloatAsState`.
 */
@Composable
fun BrushStrokeProgress(
  progress: Float,
  modifier: Modifier = Modifier,
  height: Dp = 26.dp,
  seed: Int = 7,
) {
  val animated by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      label = "brushProgress",
    )
  val noise = rememberInkNoise(seed = seed, count = 28)
  val edgeNoise = rememberInkNoise(seed = seed + 101, count = 40)

  Canvas(modifier = modifier.height(height)) {
    val w = size.width
    val h = size.height
    val maxHalf = h * 0.30f
    val baseline = h * 0.55f

    // The centreline drifts a hair — no hand draws a true horizontal.
    val line: (Float) -> Offset = { t ->
      Offset(x = t * w, y = baseline + noiseAt(noise, t) * h * 0.055f + sin(t * 3.1f) * h * 0.03f)
    }

    // Width profile: quick swell off the tip, fullest at ~60%, lifted at the end.
    val profile: (Float) -> Float = { t ->
      val body = sin((t.coerceIn(0f, 1f) * Math.PI).toFloat()).pow(0.42f)
      val lean = 0.82f + 0.18f * sin((t * 2.2f + 0.4f))
      val ragged = 1f + noiseAt(edgeNoise, t) * 0.17f
      maxHalf * body * lean * ragged
    }

    // Ghost of the full stroke: where the mark will reach when the day is done.
    drawInkStroke(
      centreline = line,
      halfWidth = { profile(it) * 0.82f },
      color = SumiePalette.inkWash,
      end = 1f,
      alpha = 0.16f,
    )

    if (animated > 0.004f) {
      // The tip of a moving brush is thinner than its body: narrow the last few percent.
      val lead: (Float) -> Float = { t ->
        val distanceToTip = (animated - t).coerceAtLeast(0f)
        val k = (distanceToTip / (0.07f)).coerceIn(0f, 1f)
        0.45f + 0.55f * k
      }
      drawInkStroke(
        centreline = line,
        halfWidth = { profile(it) * lead(it) },
        color = SumiePalette.sumiInk,
        end = animated,
        alpha = 0.94f,
      )
      // かすれ — dry-brush gaps scraped back out in paper colour.
      val gaps = listOf(0.22f to 0.035f, 0.51f to 0.028f, 0.74f to 0.02f)
      gaps.forEach { (at, len) ->
        if (at < animated) {
          drawInkStroke(
            centreline = { t -> line(t) + Offset(0f, -maxHalf * 0.22f) },
            halfWidth = { t ->
              val local = ((t - at) / len).coerceIn(0f, 1f)
              if (t < at || t > at + len) 0f else maxHalf * 0.10f * sin(local * Math.PI.toFloat())
            },
            color = SumiePalette.washi,
            end = (at + len).coerceAtMost(animated),
            alpha = 0.55f,
            samples = 40,
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------------
//  Hand-drawn strike-through
// ---------------------------------------------------------------------------------

/**
 * Crosses out finished work with a wavy ink line that is *drawn*, left to right.
 *
 * `TextDecoration.LineThrough` is a typographic rule: dead straight, instantaneous,
 * and exactly the same on every row. A person striking an item off a list makes a
 * line that wobbles, overshoots the last letter, and takes a moment to happen — so
 * this one does all three, over ~350ms, tapered at both ends like every other mark
 * on the screen.
 */
@Composable
fun Modifier.inkStrikeThrough(
  done: Boolean,
  color: Color = SumiePalette.sumiInk,
  seed: Int = 3,
): Modifier {
  val sweep = remember { Animatable(if (done) 1f else 0f) }
  val noise = rememberInkNoise(seed = seed + 41, count = 18)

  LaunchedEffect(done) {
    if (done) {
      sweep.animateTo(1f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
    } else {
      sweep.animateTo(0f, tween(durationMillis = 160, easing = LinearEasing))
    }
  }

  return this.drawWithContent {
    drawContent()
    val v = sweep.value
    if (v <= 0.004f) return@drawWithContent
    val w = size.width
    val h = size.height
    // Overshoot slightly past both ends, the way a real strike runs off the word.
    val startX = -w * 0.02f
    val span = w * 1.04f
    val mid = h * 0.56f
    drawInkStroke(
      centreline = { t ->
        Offset(
          x = startX + t * span,
          y = mid + sin(t * 7.4f + 0.6f) * h * 0.045f + noiseAt(noise, t) * h * 0.035f,
        )
      },
      halfWidth = { t ->
        val body = sin((t * Math.PI).toFloat()).pow(0.35f)
        h * 0.045f * (0.7f + 0.5f * body) * (1f + noiseAt(noise, t) * 0.25f)
      },
      color = color,
      end = v,
      alpha = 0.88f,
      samples = 40,
    )
  }
}

// ---------------------------------------------------------------------------------
//  Hand-drawn checkbox
// ---------------------------------------------------------------------------------

/**
 * A checkbox drawn the way it would be inked on paper: four separate strokes that do
 * not quite meet at the corners, filled by a two-stroke brush check when toggled.
 *
 * Material's `Checkbox` carries its own ripple, its own minimum touch box, and its own
 * rounded geometry — all of which read as "app chrome" and break the sheet. The click
 * target is provided by the caller instead, so the mark itself can stay pure drawing.
 */
@Composable
fun InkCheckbox(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp, seed: Int = 11) {
  val fill = remember { Animatable(if (checked) 1f else 0f) }
  val noise = rememberInkNoise(seed = seed, count = 16)

  LaunchedEffect(checked) {
    if (checked) {
      fill.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
    } else {
      fill.animateTo(0f, tween(140, easing = LinearEasing))
    }
  }

  Canvas(modifier = modifier.size(size)) {
    val s = this.size.minDimension
    val inset = s * 0.10f
    val a = inset
    val b = s - inset
    val hair = s * 0.030f

    // Four strokes, each with its own jitter and a deliberate corner gap.
    fun edge(from: Offset, to: Offset, n: Int) {
      drawInkStroke(
        centreline = { t ->
          val px = from.x + (to.x - from.x) * t
          val py = from.y + (to.y - from.y) * t
          val wobble = noiseAt(noise, (t + n * 0.23f) % 1f) * s * 0.018f
          Offset(px + wobble * (to.y - from.y) / s, py + wobble * (to.x - from.x) / s)
        },
        halfWidth = { t -> hair * (0.75f + 0.5f * sin((t * Math.PI).toFloat()).pow(0.3f)) },
        color = SumiePalette.inkSoft,
        alpha = 0.9f,
        samples = 16,
      )
    }
    val gap = s * 0.06f
    edge(Offset(a + gap, a), Offset(b, a), 0)
    edge(Offset(b, a + gap), Offset(b, b), 1)
    edge(Offset(b - gap, b), Offset(a, b), 2)
    edge(Offset(a, b - gap), Offset(a, a), 3)

    if (fill.value > 0.004f) {
      val v = fill.value
      // A check made of one brush gesture: down-left short, up-right long.
      val pivot = Offset(a + (b - a) * 0.38f, a + (b - a) * 0.74f)
      val start = Offset(a + (b - a) * 0.12f, a + (b - a) * 0.46f)
      val finish = Offset(b + (b - a) * 0.16f, a - (b - a) * 0.14f)
      drawInkStroke(
        centreline = { t ->
          if (t < 0.34f) {
            val k = t / 0.34f
            Offset(start.x + (pivot.x - start.x) * k, start.y + (pivot.y - start.y) * k)
          } else {
            val k = (t - 0.34f) / 0.66f
            Offset(pivot.x + (finish.x - pivot.x) * k, pivot.y + (finish.y - pivot.y) * k)
          }
        },
        halfWidth = { t ->
          val swell = if (t < 0.34f) 0.55f + t else 1.05f - (t - 0.34f) * 0.85f
          s * 0.055f * swell * (1f + noiseAt(noise, t) * 0.2f)
        },
        color = SumiePalette.sumiInk,
        end = v,
        alpha = 0.95f,
        samples = 32,
      )
    }
  }
}

// ---------------------------------------------------------------------------------
//  Hanko seal
// ---------------------------------------------------------------------------------

/**
 * The 判子 *hanko* that stamps down when the last blocking task is cleared.
 *
 * This is the only vermilion on the screen and the only moment of movement, which is
 * exactly why it works: on a sheet this quiet, one red square landing with a spring is
 * a genuine event. The stamp arrives from above — scale 1.4 → 1.0 with a low damping
 * ratio so it overshoots and settles, rotating a few degrees off-square the way a hand
 * never quite lines a seal up, fading in as the ink takes.
 *
 * The glyphs 日本 are carved in *negative* space, paper showing through the red field,
 * which is how a 朱文 seal actually reads.
 *
 * @param stamped when true the seal is present; flipping to false clears it instantly.
 */
@Composable
fun HankoSeal(stamped: Boolean, modifier: Modifier = Modifier, size: Dp = 56.dp) {
  val scale = remember { Animatable(1.4f) }
  val alpha = remember { Animatable(0f) }
  val tilt = remember { Animatable(-9f) }

  LaunchedEffect(stamped) {
    if (stamped) {
      launch { alpha.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing)) }
      launch {
        scale.animateTo(
          targetValue = 1f,
          animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        )
      }
      launch {
        tilt.animateTo(
          targetValue = -4.5f,
          animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        )
      }
    } else {
      alpha.snapTo(0f)
      scale.snapTo(1.4f)
      tilt.snapTo(-9f)
    }
  }

  Canvas(modifier = modifier.size(size)) {
    if (alpha.value <= 0.004f) return@Canvas
    val s = this.size.minDimension
    val centre = Offset(s / 2f, s / 2f)
    scale(scale.value, pivot = centre) {
      rotate(tilt.value, pivot = centre) {
        drawHanko(s, alpha.value)
      }
    }
  }
}

/** The seal face itself: red field, carved frame, 日本 left as bare paper. */
private fun DrawScope.drawHanko(s: Float, a: Float) {
  val red = SumiePalette.vermilion
  val paper = SumiePalette.washi
  val frame = s * 0.085f
  val pad = s * 0.045f

  // Field. Ink-stone red is never flat, so the fill is a whisper of a gradient.
  drawRect(
    brush =
      Brush.linearGradient(
        colors = listOf(red.copy(alpha = 0.97f), red.copy(alpha = 0.86f)),
        start = Offset(pad, pad),
        end = Offset(s - pad, s - pad),
      ),
    topLeft = Offset(pad, pad),
    size = androidx.compose.ui.geometry.Size(s - pad * 2, s - pad * 2),
    alpha = a,
  )

  // The carved gutter that separates frame from field.
  drawRect(
    color = paper,
    topLeft = Offset(pad + frame, pad + frame),
    size =
      androidx.compose.ui.geometry.Size(
        s - (pad + frame) * 2,
        s - (pad + frame) * 2,
      ),
    alpha = a * 0.92f,
    style = Stroke(width = s * 0.022f),
  )

  // Nicks in the frame — a stone seal chips, and the chips are half the charm.
  listOf(
    Offset(pad + s * 0.30f, pad) to s * 0.05f,
    Offset(s - pad, pad + s * 0.55f) to s * 0.04f,
    Offset(pad + s * 0.68f, s - pad) to s * 0.035f,
  )
    .forEach { (at, r) -> drawCircle(paper, radius = r, center = at, alpha = a * 0.85f) }

  // 日本, carved out. Two stacked cells, strokes left as paper.
  val inner = pad + frame + s * 0.05f
  val innerSize = s - inner * 2
  val cellH = innerSize * 0.46f
  val stroke = s * 0.052f

  fun bar(x: Float, y: Float, w: Float, h: Float) =
    drawRect(paper, Offset(x, y), androidx.compose.ui.geometry.Size(w, h), alpha = a * 0.94f)

  // 日 — box with a waist bar.
  run {
    val l = inner + innerSize * 0.20f
    val r = inner + innerSize * 0.80f
    val t = inner
    val b = inner + cellH
    bar(l, t, r - l, stroke)
    bar(l, b - stroke, r - l, stroke)
    bar(l, t, stroke, b - t)
    bar(r - stroke, t, stroke, b - t)
    bar(l, (t + b) / 2f - stroke / 2f, r - l, stroke)
  }

  // 本 — crossbar, trunk, two legs, and the base rule.
  run {
    val t = inner + innerSize * 0.54f
    val b = inner + innerSize
    val l = inner + innerSize * 0.06f
    val r = inner + innerSize * 0.94f
    val cx = (l + r) / 2f
    bar(l, t + (b - t) * 0.18f, r - l, stroke)
    bar(cx - stroke / 2f, t, stroke, b - t)
    drawLine(
      color = paper,
      start = Offset(cx, t + (b - t) * 0.24f),
      end = Offset(l + (r - l) * 0.10f, b - (b - t) * 0.10f),
      strokeWidth = stroke * 0.9f,
      cap = StrokeCap.Round,
      alpha = a * 0.94f,
    )
    drawLine(
      color = paper,
      start = Offset(cx, t + (b - t) * 0.24f),
      end = Offset(r - (r - l) * 0.10f, b - (b - t) * 0.10f),
      strokeWidth = stroke * 0.9f,
      cap = StrokeCap.Round,
      alpha = a * 0.94f,
    )
    bar(cx - (r - l) * 0.16f, b - stroke, (r - l) * 0.32f, stroke)
  }
}

// ---------------------------------------------------------------------------------
//  Paper
// ---------------------------------------------------------------------------------

/**
 * The sheet: a soft radial bloom of damp paper with a handful of stray ink specks.
 *
 * Washi is never one flat colour — it is lighter where the light falls and darker at
 * the edges where the fibre is denser. A single radial gradient plus a linear one
 * across the corner gives the sheet enough life to stop reading as `#F4F1EA` and start
 * reading as a surface. The specks breathe on a very long loop, so nothing is ever
 * quite still without anything ever visibly moving.
 */
@Composable
fun InkWashBackdrop(modifier: Modifier = Modifier, seed: Int = 19) {
  val specks =
    remember(seed) {
      val random = Random(seed)
      List(14) {
        Speck(
          x = random.nextFloat(),
          y = random.nextFloat(),
          radius = 0.6f + random.nextFloat() * 2.4f,
          alpha = 0.03f + random.nextFloat() * 0.07f,
        )
      }
    }
  val breathe = rememberInfiniteTransition(label = "paperBreath")
  val damp by
    breathe.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.06f,
      animationSpec =
        infiniteRepeatable(animation = tween(durationMillis = 14000, easing = LinearEasing)),
      label = "damp",
    )

  Canvas(modifier = modifier.fillMaxSize()) {
    drawRect(SumiePalette.washi)
    // The bloom: brighter high-left, as if the sheet were lit from a window.
    drawRect(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color.White.copy(alpha = 0.55f),
              SumiePalette.washi.copy(alpha = 0f),
              SumiePalette.paperShade.copy(alpha = 0.55f),
            ),
          center = Offset(size.width * 0.28f, size.height * 0.18f),
          radius = size.maxDimension * 0.95f * damp,
        )
    )
    // A cold wash settling into the bottom corner, like water that ran and dried.
    drawRect(
      brush =
        Brush.linearGradient(
          colors =
            listOf(SumiePalette.paperShade.copy(alpha = 0f), SumiePalette.inkWash.copy(alpha = 0.10f)),
          start = Offset(size.width * 0.45f, size.height * 0.55f),
          end = Offset(size.width, size.height),
        )
    )
    specks.forEach { speck ->
      drawCircle(
        color = SumiePalette.sumiInk,
        radius = speck.radius * density,
        center = Offset(speck.x * size.width, speck.y * size.height),
        alpha = speck.alpha * abs(cos(damp * 3f)).coerceAtLeast(0.6f),
      )
    }
  }
}

private data class Speck(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * A hairline rule — the only separator this style allows itself.
 *
 * Drawn rather than `HorizontalDivider`d so it can taper away at the right edge,
 * which stops a row of rules reading as a table.
 */
@Composable
fun InkHairline(
  modifier: Modifier = Modifier,
  color: Color = SumiePalette.inkWash,
  alpha: Float = 0.42f,
  seed: Int = 5,
) {
  val noise = rememberInkNoise(seed = seed + 77, count = 20)
  Canvas(modifier = modifier.height(1.5.dp)) {
    drawInkStroke(
      centreline = { t -> Offset(t * size.width, size.height / 2f + noiseAt(noise, t) * 0.4f) },
      halfWidth = { t ->
        val fade = (1f - (t - 0.72f).coerceAtLeast(0f) / 0.28f).coerceIn(0f, 1f)
        size.height * 0.30f * fade * (1f + noiseAt(noise, t) * 0.3f)
      },
      color = color,
      alpha = alpha,
      samples = 40,
    )
  }
}
