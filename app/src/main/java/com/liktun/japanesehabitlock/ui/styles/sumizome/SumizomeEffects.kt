package com.liktun.japanesehabitlock.ui.styles.sumizome

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// ────────────────────────────────────────────────────────── drifting embers

/**
 * One mote's fixed personality: where it drifts, how fast it rises, how it breathes.
 *
 * Generated once from a seeded [Random] and then frozen, so the drift is deterministic
 * across recomposition (and identical in the IDE preview) while still looking
 * hand-scattered rather than mechanical.
 */
private data class EmberSpec(
  /** Horizontal home position, 0f..1f of the canvas width. */
  val x: Float,
  /** Integer multiplier on the shared driver, so faster motes still wrap seamlessly. */
  val speed: Int,
  /** Where in its own rise this mote starts, 0f..1f. */
  val phase: Float,
  /** Core radius in canvas px before the soft falloff is added. */
  val radius: Float,
  /** Peak horizontal sine drift, as a fraction of canvas width. */
  val swayAmount: Float,
  /** How many sway cycles per rise. */
  val swayCycles: Float,
  val swayPhase: Float,
  /** Peak opacity at the middle of its life. */
  val peakAlpha: Float,
  /** Extra flicker frequency, so no two motes breathe in step. */
  val flickerCycles: Float,
  val flickerPhase: Float,
  /** A few motes lean toward sakura instead of gold, which stops the field going amber. */
  val rosy: Boolean,
) {
  /**
   * The mote's halo gradient, built once and reused for every frame.
   *
   * Cached per spec because allocating a shader per mote per frame is the difference
   * between a smooth field and a dead device on software rendering. Per-frame brightness
   * is applied through `drawCircle`'s alpha instead of by rebuilding the stops.
   */
  private var cachedBrush: Brush? = null
  private var cachedCore: Color? = null

  fun brush(core: Color): Brush {
    val existing = cachedBrush
    if (existing != null && cachedCore == core) return existing
    val halo = radius * 3.2f
    val built =
      Brush.radialGradient(
        colorStops =
          arrayOf(
            0.0f to core,
            0.28f to core.copy(alpha = 0.55f),
            0.62f to core.copy(alpha = 0.16f),
            1.0f to Color.Transparent,
          ),
        center = Offset(halo, halo),
        radius = halo,
      )
    cachedBrush = built
    cachedCore = core
    return built
  }
}

private fun emberSpecs(count: Int, random: Random): List<EmberSpec> = List(count) {
  EmberSpec(
    x = random.nextFloat(),
    // Integer speeds keep every mote's loop commensurate with the driver's, so the
    // whole field wraps invisibly instead of stuttering once per period.
    speed = 1 + random.nextInt(3), // 1..3 — slower than falling petals; embers loiter.
    phase = random.nextFloat(),
    radius = 2.2f + random.nextFloat() * 3.4f,
    swayAmount = 0.03f + random.nextFloat() * 0.07f,
    swayCycles = 0.8f + random.nextFloat() * 1.6f,
    swayPhase = random.nextFloat(),
    peakAlpha = 0.30f + random.nextFloat() * 0.34f,
    flickerCycles = 2f + random.nextFloat() * 3f,
    flickerPhase = random.nextFloat(),
    rosy = it % 5 == 0,
  )
}

/**
 * Warm motes drifting *upward* behind the header — embers off a lantern wick, or
 * fireflies over a garden at dusk.
 *
 * This is SUMIZOME's answer to WA-MODERN's falling sakura, and the inversion is
 * deliberate: petals fall because it is daytime and things are settling; embers rise
 * because it is night and the room is warm. Same gentle, single-clock machinery,
 * opposite direction and mood.
 *
 * Driven by **one** [rememberInfiniteTransition]: a single linear 0f..1f driver, with
 * every mote deriving its own position from an integer speed multiplier and a phase
 * offset drawn from a seeded [Random]. Twelve separate transitions would mean twelve
 * animation clocks for decoration.
 *
 * Each mote is drawn as a soft [Brush.radialGradient] disc — never a hard circle. A
 * crisp dot on a dark ground reads as a dead pixel or a UI bug; a falloff reads as
 * light. Note there is no additive blending and no glow stack: alpha stays low, the
 * colours stay warm, and the field sits well under the reading threshold.
 */
@Composable
fun EmberDriftField(
  modifier: Modifier = Modifier,
  emberCount: Int = 12,
  seed: Int = 20260827,
  periodMillis: Int = 26_000,
) {
  val embers = remember(emberCount, seed) { emberSpecs(emberCount, Random(seed)) }

  val transition = rememberInfiniteTransition(label = "embers")
  val driver by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
    label = "ember-driver",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    embers.forEach { ember ->
      // frac() of an integer-scaled driver: continuous across the loop boundary.
      val t = (driver * ember.speed + ember.phase).let { it - floor(it) }

      // Rising: born just below the canvas, retired just above it, so both the birth
      // and the death happen off-screen and nothing ever pops.
      val cy = h * 1.12f - t * (h * 1.30f)
      val sway = sin((t * ember.swayCycles + ember.swayPhase) * 2f * PI.toFloat())
      val cx = (ember.x + ember.swayAmount * sway) * w

      // Envelope: fade in over the first fifth of the rise, out over the last third,
      // multiplied by a slow flicker so each mote breathes independently.
      val fadeIn = (t / 0.20f).coerceIn(0f, 1f)
      val fadeOut = ((1f - t) / 0.32f).coerceIn(0f, 1f)
      val flicker =
        0.62f + 0.38f * (0.5f + 0.5f * sin((t * ember.flickerCycles + ember.flickerPhase) * 2f * PI.toFloat()))
      val alpha = ember.peakAlpha * fadeIn * fadeOut * flicker
      if (alpha <= 0.004f) return@forEach

      val core = if (ember.rosy) SumizomePalette.sakuraNight else SumizomePalette.goldLeaf
      // The halo is ~3.2x the core so the falloff is genuinely soft rather than a
      // circle with a feathered edge.
      val haloRadius = ember.radius * 3.2f
      // Alpha is applied to the whole layer rather than baked into the gradient stops.
      // Building a Brush.radialGradient per mote per frame allocates a new shader 12x
      // every frame, which a GPU absorbs but a software-rendered emulator does not — it
      // took the device down repeatedly. The gradient is now built once per mote in its
      // own local space and translated into place, and the per-frame brightness rides on
      // drawCircle's alpha parameter instead.
      translate(left = cx - haloRadius, top = cy - haloRadius) {
        drawCircle(
          brush = ember.brush(core),
          radius = haloRadius,
          center = Offset(haloRadius, haloRadius),
          alpha = alpha.coerceIn(0f, 1f),
        )
      }
    }
  }
}

// ────────────────────────────────────────────────────────────── progress ring

/**
 * The matcha progress ring: `blockingDone / blockingTotal` drawn as a filling arc,
 * with a gold glint riding the leading edge.
 *
 * A ring rather than a bar because the rest of the screen is stacked cards and
 * horizontal rules — the circle is the one place the eye can land. It sweeps from
 * twelve o'clock with a round cap so the head reads as ink rather than as a cut, and
 * animates through [animateFloatAsState] so a toggle glides instead of snapping.
 *
 * The gold tip is SUMIZOME's addition over its daylight sibling: a short arc of
 * [SumizomePalette.goldLeaf] laid over the last few degrees of the sweep. On cream this
 * would be mud; on charcoal it catches like gilt on a lacquer bowl. It is a *tip*, not
 * a glow — one stroke, no blur, no blend mode.
 */
@Composable
fun SumizomeProgressRing(
  done: Int,
  total: Int,
  modifier: Modifier = Modifier,
  diameter: Dp = 76.dp,
  strokeWidth: Dp = 7.dp,
) {
  val target = if (total <= 0) 1f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
  val sweep by animateFloatAsState(
    targetValue = target,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessLow,
    ),
    label = "ring-sweep",
  )
  val complete = total > 0 && done >= total

  Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = strokeWidth.toPx()
      val inset = stroke / 2f
      val arcSize = Size(size.width - stroke, size.height - stroke)
      val topLeft = Offset(inset, inset)

      // Track: a whisper of matcha, kept just above the card fill so the empty ring is
      // legible without becoming a second element.
      drawArc(
        color = SumizomePalette.matchaNight.copy(alpha = 0.18f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )

      if (sweep > 0.001f) {
        val sweepDegrees = 360f * sweep
        drawArc(
          color = SumizomePalette.matchaNight,
          startAngle = -90f,
          sweepAngle = sweepDegrees,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // The glint: the final slice of the sweep restated in gold. Capped in length so
        // a nearly-empty ring shows a dot of gold rather than a gold ring.
        val tipDegrees = sweepDegrees.coerceAtMost(16f)
        drawArc(
          color = SumizomePalette.goldLeaf,
          startAngle = -90f + sweepDegrees - tipDegrees,
          sweepAngle = tipDegrees,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          alpha = 0.85f,
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
        // Completion promotes the numerator to gold — the same celebration colour the
        // gate banner takes — so the two agree without needing words.
        color = if (complete) SumizomePalette.goldLeaf else SumizomePalette.washi,
      )
      Text(
        text = "/$total",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        color = SumizomePalette.washiSoft,
        modifier = Modifier.padding(start = 1.dp, bottom = 3.dp),
      )
    }
  }
}

// ─────────────────────────────────────────────────────────── completion bounce

/**
 * The spring a task card rides when it is checked off.
 *
 * Exposes a `scale` that overshoots then settles, plus a 0f..1f `wash` the card uses to
 * cross-fade toward a soft matcha tint. The scale is one [Animatable] driven by a
 * medium-bouncy [spring]: the card should feel like it *settles into place*, which is
 * the cheapest way to make "done" feel earned rather than merely recorded.
 */
@Stable
class SumizomeCompletionSpring internal constructor(
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
fun rememberSumizomeCompletionSpring(done: Boolean): SumizomeCompletionSpring {
  val holder = remember { SumizomeCompletionSpring(Animatable(1f)) }
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

/** Applies a [SumizomeCompletionSpring]'s scale. Kept separate so callers own the layout. */
fun Modifier.sumizomeBounce(spring: SumizomeCompletionSpring): Modifier = this.scale(spring.scale)

// ───────────────────────────────────────────────────────────── lantern glow

/**
 * A very soft warm radial wash, drawn *behind* the gate banner like lamplight falling
 * on paper.
 *
 * The whole point of this effect is restraint. It is an elliptical gradient with a peak
 * alpha in the low tenths, no blend mode, no second pass, and no saturated hue — the
 * brief is a shoji screen with a lamp behind it, and the moment you can point at "the
 * glow" as a distinct object, it has failed. It exists so the banner feels *lit from
 * within* rather than merely tinted, which is the difference between a warm dark UI and
 * a flat one.
 *
 * @param color the lamp colour — gold when the gate is open, sakura while it is shut.
 * @param intensity 0f..1f multiplier, animated by the caller so unlocking brightens.
 */
fun DrawScope.drawLanternGlow(
  color: Color,
  intensity: Float,
  centerFraction: Float = 0.24f,
) {
  val w = size.width
  val h = size.height
  if (w <= 0f || h <= 0f || intensity <= 0.001f) return

  val center = Offset(w * centerFraction, h * 0.5f)
  // Wide and shallow: the lamp is off to one side of a banner that is much wider than
  // it is tall, so a circular falloff would clip visibly at the top and bottom edges.
  val radius = maxOf(w * 0.62f, h * 1.9f)

  // Alpha rides on drawCircle rather than being baked into the stops. This gradient is
  // enormous (radius ~2x the banner height) and `intensity` is animated, so folding the
  // intensity into the colour stops rebuilt a very large shader on every single frame —
  // survivable on a GPU, fatal on a software-rendered emulator, which it repeatedly
  // killed. The stops are now constant and only the layer alpha changes.
  drawCircle(
    brush =
      Brush.radialGradient(
        colorStops =
          arrayOf(
            0.0f to color.copy(alpha = 0.11f),
            0.42f to color.copy(alpha = 0.11f * 0.46f),
            0.78f to color.copy(alpha = 0.11f * 0.12f),
            1.0f to Color.Transparent,
          ),
        center = center,
        radius = radius,
      ),
    radius = radius,
    center = center,
    alpha = intensity.coerceIn(0f, 1f),
  )
}

// ─────────────────────────────────────────────────────────── kumiko divider

/**
 * A kumiko (組子) lattice used as a section divider, in gold leaf.
 *
 * Real kumiko is thin cedar strips assembled without nails into repeating polygons;
 * here it is a band of interlocking triangles and hexagon waists in gold hairlines. The
 * motif is inherited straight from WA-MODERN — a sibling should share its furniture —
 * but it genuinely reads *better* here: warm gold on charcoal has the contrast that
 * gold on cream never quite manages, so the same alpha buys a crisper lattice.
 *
 * It is still decoration, so it stays under the reading threshold: present when you look
 * for it, invisible while you are reading the tasks above it. The band fades out at both
 * ends so it reads as a woven panel rather than a rule chopped off by the screen edge.
 */
@Composable
fun GoldKumikoDivider(
  modifier: Modifier = Modifier,
  height: Dp = 22.dp,
  cellWidth: Dp = 26.dp,
  color: Color = SumizomePalette.goldLeaf,
  alpha: Float = 0.34f,
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

    // Two horizontal rails: the frame the lattice is pinned into.
    var x = 0f
    while (x < w) {
      val nx = (x + step).coerceAtMost(w)
      val a = edgeFade((x + nx) / 2f)
      line(x, top, nx, top, a * 0.55f)
      line(x, bottom, nx, bottom, a * 0.55f)
      x = nx
    }

    // Interlocking triangles, alternating point-up and point-down along the band —
    // the asanoha-adjacent kumiko motif at its simplest.
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
    line(0f, mid, w, mid, alpha * 0.20f)
  }
}

// ──────────────────────────────────────────────────────────────── seal mark

/**
 * A small eight-point starburst, drawn in gold at low alpha.
 *
 * Used once, next to the unlocked banner, as the celebration mark. It is drawn rather
 * than typeset because a glyph would inherit the font's weight and colour and end up
 * looking like text the user should read; a hairline burst reads unambiguously as a
 * flourish.
 */
fun DrawScope.drawGoldGlint(center: Offset, radius: Float, color: Color, alpha: Float) {
  if (radius <= 0f || alpha <= 0.004f) return
  repeat(8) { i ->
    val angle = (i * 45f) * PI.toFloat() / 180f
    // Long and short rays alternate, which is what makes a burst read as a sparkle
    // rather than as a wheel.
    val len = if (i % 2 == 0) radius else radius * 0.52f
    drawLine(
      color = color,
      start = center,
      end = Offset(center.x + cos(angle) * len, center.y + sin(angle) * len),
      strokeWidth = 1.2f,
      cap = StrokeCap.Round,
      alpha = if (i % 2 == 0) alpha else alpha * 0.6f,
    )
  }
}
