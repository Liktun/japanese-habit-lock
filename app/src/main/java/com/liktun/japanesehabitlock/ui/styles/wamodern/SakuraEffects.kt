package com.liktun.japanesehabitlock.ui.styles.wamodern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────── falling sakura

/**
 * One petal's fixed personality: where it falls, how fast, how far it sways.
 *
 * These are generated once from a seeded [Random] and then never change, so the drift
 * is deterministic across recompositions (and identical in the IDE preview) while still
 * looking hand-scattered rather than mechanical.
 */
private data class PetalSpec(
  /** Horizontal home position, 0f..1f of the canvas width. */
  val x: Float,
  /** Integer multiplier on the shared driver, so faster petals still wrap seamlessly. */
  val speed: Int,
  /** Where in its own fall this petal starts, 0f..1f. */
  val phase: Float,
  /** Petal height in dp-ish canvas units. */
  val scale: Float,
  /** Peak horizontal sine drift, as a fraction of canvas width. */
  val swayAmount: Float,
  /** How many sway cycles per fall. */
  val swayCycles: Float,
  val swayPhase: Float,
  val spin: Float,
  val spinPhase: Float,
  val alpha: Float,
  /** Petals alternate between the two sakura tones so the drift has depth. */
  val deep: Boolean,
)

private fun petalSpecs(count: Int, random: Random): List<PetalSpec> = List(count) {
  PetalSpec(
    x = random.nextFloat(),
    speed = 2 + random.nextInt(4), // 2..5 — integer keeps the loop seamless.
    phase = random.nextFloat(),
    scale = 10f + random.nextFloat() * 12f,
    swayAmount = 0.04f + random.nextFloat() * 0.10f,
    swayCycles = 1f + random.nextFloat() * 2f,
    swayPhase = random.nextFloat(),
    spin = 0.5f + random.nextFloat() * 1.5f,
    spinPhase = random.nextFloat(),
    alpha = 0.22f + random.nextFloat() * 0.40f,
    deep = it % 3 == 0,
  )
}

/**
 * A unit sakura petal: a rounded teardrop with the characteristic notch at the wide end.
 *
 * Built once and reused for every petal via a canvas transform — a petal is a *shape*,
 * not a dot, and the notch is what makes it read as sakura rather than as a generic
 * blossom. Coordinates are centred on the origin, roughly 1.0 wide by 1.4 tall.
 */
private fun unitPetalPath(): Path = Path().apply {
  val w = 0.50f
  val h = 0.70f
  val notch = 0.22f
  moveTo(0f, -h) // narrow tip
  quadraticTo(w * 0.95f, -h * 0.55f, w, h * 0.18f) // right shoulder, bulging out
  quadraticTo(w * 0.80f, h, 0f, h - notch) // right lobe down into the centre notch
  quadraticTo(-w * 0.80f, h, -w, h * 0.18f) // left lobe
  quadraticTo(-w * 0.95f, -h * 0.55f, 0f, -h) // back up the left shoulder
  close()
}

/**
 * Sakura petals drifting down behind whatever sits in front of them.
 *
 * Deliberately driven by a **single** [rememberInfiniteTransition]: one linear 0f..1f
 * driver, with every petal deriving its own position from an integer speed multiplier
 * and a phase offset. Twenty separate transitions would mean twenty animation clocks
 * for a decorative background, and integer multipliers mean each petal's fall wraps
 * exactly when the driver does, so nothing visibly jumps.
 *
 * Petals travel from just above the canvas to just below it, so the reset happens
 * off-screen.
 */
@Composable
fun SakuraPetalField(
  modifier: Modifier = Modifier,
  petalCount: Int = 16,
  seed: Int = 20260827,
  periodMillis: Int = 30_000,
) {
  val petals = remember(petalCount, seed) { petalSpecs(petalCount, Random(seed)) }
  val petalPath = remember { unitPetalPath() }

  val transition = rememberInfiniteTransition(label = "sakura")
  val driver by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
    label = "sakura-driver",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    petals.forEach { petal ->
      // frac() of an integer-scaled driver: continuous across the loop boundary.
      val t = (driver * petal.speed + petal.phase).let { it - floor(it) }

      val cy = -0.18f * h + t * (h * 1.36f)
      val sway = sin((t * petal.swayCycles + petal.swayPhase) * 2f * PI.toFloat())
      val cx = (petal.x + petal.swayAmount * sway) * w
      val degrees = (petal.spinPhase + t * petal.spin) * 360f

      withTransform({
        translate(cx, cy)
        rotate(degrees, Offset.Zero)
        scale(petal.scale, petal.scale, Offset.Zero)
      }) {
        drawPath(
          path = petalPath,
          color = if (petal.deep) WaPalette.sakuraDeep else WaPalette.sakura,
          alpha = petal.alpha,
        )
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────── progress ring

/**
 * The matcha progress ring: `blockingDone / blockingTotal` drawn as a filling arc.
 *
 * A ring rather than a bar because the whole screen is otherwise horizontal rules and
 * stacked cards — the circle is the one place the eye can land. It sweeps from twelve
 * o'clock, uses a round stroke cap so the leading edge reads as ink rather than as a
 * cut-off, and animates through [animateFloatAsState] so a toggle glides instead of
 * snapping.
 */
@Composable
fun MatchaProgressRing(
  done: Int,
  total: Int,
  modifier: Modifier = Modifier,
  diameter: Dp = 76.dp,
  strokeWidth: Dp = 7.dp,
) {
  val target = if (total <= 0) 1f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
  val sweep by animateFloatAsState(
    targetValue = target,
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
    label = "ring-sweep",
  )
  val complete = total > 0 && done >= total

  Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = strokeWidth.toPx()
      val inset = stroke / 2f
      val arcSize = Size(size.width - stroke, size.height - stroke)
      val topLeft = Offset(inset, inset)

      // Track: a whisper of matcha so the empty ring still belongs to the palette.
      drawArc(
        color = WaPalette.matcha.copy(alpha = 0.16f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )

      if (sweep > 0.001f) {
        drawArc(
          color = if (complete) WaPalette.matchaDeep else WaPalette.matcha,
          startAngle = -90f,
          sweepAngle = 360f * sweep,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
      }
    }

    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "$done",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = if (complete) WaPalette.matchaDeep else WaPalette.indigo,
      )
      Text(
        text = "/$total",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        color = WaPalette.warmGray,
        modifier = Modifier.padding(start = 1.dp, bottom = 3.dp),
      )
    }
  }
}

// ─────────────────────────────────────────────────────────── completion bounce

/**
 * The spring the task card rides when it is checked off.
 *
 * Exposes a `scale` that overshoots then settles, plus a 0f..1f `wash` the card uses to
 * cross-fade toward a soft matcha tint. The scale is one [Animatable] driven by a
 * medium-bouncy [spring]: the card should feel like it *settles into place*, which is
 * the cheapest way to make "done" feel earned rather than merely recorded.
 */
@Stable
class CompletionSpring internal constructor(
  internal val anim: Animatable<Float, AnimationVector1D>,
) {
  internal var firstPass: Boolean = true
  internal val washState: MutableFloatState = mutableFloatStateOf(0f)

  /** 1f at rest; overshoots briefly on toggle. */
  val scale: Float get() = anim.value

  /** 0f..1f — how strongly the matcha wash is applied. */
  val wash: Float get() = washState.floatValue
}

/**
 * Remembers a completion spring bound to [done].
 *
 * The bounce fires only on an actual change, never on first composition, so a screen
 * that opens with tasks already checked is calm rather than a popcorn of animations.
 */
@Composable
fun rememberCompletionSpring(done: Boolean): CompletionSpring {
  val holder = remember { CompletionSpring(Animatable(1f)) }
  val wash by animateFloatAsState(
    targetValue = if (done) 1f else 0f,
    animationSpec = tween(durationMillis = 320),
    label = "matcha-wash",
  )
  holder.washState.floatValue = wash

  LaunchedEffect(done) {
    if (holder.firstPass) {
      holder.firstPass = false
      return@LaunchedEffect
    }
    holder.anim.snapTo(if (done) 0.955f else 1.02f)
    holder.anim.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
    )
  }
  return holder
}

/** Applies a [CompletionSpring]'s scale. Kept separate so callers own the layout. */
fun Modifier.completionBounce(spring: CompletionSpring): Modifier = this.scale(spring.scale)

// ────────────────────────────────────────────────────────── kumiko divider

/**
 * A kumiko (組子) lattice used as a section divider.
 *
 * Real kumiko is thin cedar strips assembled without nails into repeating polygons;
 * here it is a band of interlocking triangles and hexagon hints in gold-leaf hairlines
 * at very low alpha. It is decoration, so it must sit *under* the reading threshold —
 * present when you look for it, invisible when you are reading the tasks above it.
 *
 * The lattice fades out at both ends so it reads as a woven panel rather than as a rule
 * that was cut off by the screen edge.
 */
@Composable
fun KumikoDivider(
  modifier: Modifier = Modifier,
  height: Dp = 22.dp,
  cellWidth: Dp = 26.dp,
  color: Color = WaPalette.goldLeaf,
  alpha: Float = 0.42f,
) {
  Canvas(modifier = modifier.fillMaxWidth().height(height)) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val step = cellWidth.toPx().coerceAtLeast(8f)
    val stroke = 1f.dp.toPx().coerceAtLeast(0.8f)
    val top = h * 0.16f
    val bottom = h * 0.84f
    val mid = h * 0.5f

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, a: Float) {
      drawLine(
        color = color,
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
        alpha = a,
      )
    }

    // Fade the lattice toward both edges: strongest in the middle third.
    fun edgeFade(x: Float): Float {
      val n = (x / w).coerceIn(0f, 1f)
      val d = 1f - abs(n - 0.5f) * 2f
      return (d * 1.9f).coerceIn(0f, 1f) * alpha
    }

    // Two horizontal rails, the frame the lattice is pinned into.
    var x = 0f
    while (x < w) {
      val nx = (x + step).coerceAtMost(w)
      val a = edgeFade((x + nx) / 2f)
      line(x, top, nx, top, a * 0.55f)
      line(x, bottom, nx, bottom, a * 0.55f)
      x = nx
    }

    // Interlocking triangles: up-pointing and down-pointing alternate along the band,
    // which is the asanoha-adjacent kumiko motif at its simplest.
    var i = 0
    x = -step / 2f
    while (x < w + step) {
      val a = edgeFade(x + step / 2f)
      val left = x
      val right = x + step
      val centre = x + step / 2f
      if (i % 2 == 0) {
        line(left, bottom, centre, top, a)
        line(centre, top, right, bottom, a)
      } else {
        line(left, top, centre, bottom, a)
        line(centre, bottom, right, top, a)
      }
      // A short horizontal tie at the waist turns each pair of triangles into a hexagon.
      line(centre - step * 0.18f, mid, centre + step * 0.18f, mid, a * 0.7f)
      x += step
      i++
    }

    // A single hairline through the waist ties the whole band together.
    line(0f, mid, w, mid, alpha * 0.18f)
  }
}
