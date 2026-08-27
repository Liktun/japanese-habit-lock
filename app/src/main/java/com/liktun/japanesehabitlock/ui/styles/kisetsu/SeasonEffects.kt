package com.liktun.japanesehabitlock.ui.styles.kisetsu

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────── motif

/**
 * Which thing falls (or rises) past the header in each season.
 *
 * A separate enum rather than a lambda on [SeasonTheme] because the palette must stay a
 * pure data class of colours — drawing code has no business living in it, and a
 * `SeasonTheme` should be constructible in a unit test without touching Compose.
 */
enum class SeasonMotif { PETALS, BUBBLES, LEAVES, SNOW }

/** The motif belonging to a theme. Keyed on the kanji, which is the season's identity. */
fun motifFor(theme: SeasonTheme): SeasonMotif = when (theme.kanji) {
  Seasons.HARU.kanji -> SeasonMotif.PETALS
  Seasons.NATSU.kanji -> SeasonMotif.BUBBLES
  Seasons.AKI.kanji -> SeasonMotif.LEAVES
  else -> SeasonMotif.SNOW
}

// ────────────────────────────────────────────────────────────── particle specs

/**
 * One particle's fixed personality: where it travels, how fast, how much it wanders.
 *
 * Generated once from a seeded [Random] and then frozen, so the scatter is deterministic
 * across recompositions and identical in the IDE preview, while still looking
 * hand-thrown rather than gridded.
 */
private data class ParticleSpec(
  /** Home position across the canvas, 0f..1f of width. */
  val x: Float,
  /** Integer multiplier on the shared driver — keeps every loop seamless. */
  val speed: Int,
  /** Where in its own journey this particle starts, 0f..1f. */
  val phase: Float,
  /** Size in canvas units; the unit paths are all roughly 1×1. */
  val scale: Float,
  /** Peak sideways wander, as a fraction of canvas width. */
  val swayAmount: Float,
  val swayCycles: Float,
  val swayPhase: Float,
  /** Turns per journey. */
  val spin: Float,
  val spinPhase: Float,
  val alpha: Float,
  /** Every third particle takes the deeper tone, which gives the field depth. */
  val deep: Boolean,
)

/**
 * Builds the particle field for a motif.
 *
 * The *ranges* are what make each season feel different at a glance, so they live here
 * rather than in the draw call: heavy autumn leaves are big, slow and spin hard; snow is
 * small, slower still and barely sways; bubbles are tiny, quick and do not rotate at all.
 */
private fun particleSpecs(count: Int, motif: SeasonMotif, random: Random): List<ParticleSpec> =
  List(count) { index ->
    when (motif) {
      SeasonMotif.PETALS -> ParticleSpec(
        x = random.nextFloat(),
        speed = 2 + random.nextInt(4),
        phase = random.nextFloat(),
        scale = 11f + random.nextFloat() * 13f,
        swayAmount = 0.05f + random.nextFloat() * 0.11f,
        swayCycles = 1f + random.nextFloat() * 2f,
        swayPhase = random.nextFloat(),
        spin = 0.5f + random.nextFloat() * 1.4f,
        spinPhase = random.nextFloat(),
        alpha = 0.24f + random.nextFloat() * 0.40f,
        deep = index % 3 == 0,
      )
      // Bubbles: small, fast, no spin (a circle spinning is a circle), narrow wobble.
      SeasonMotif.BUBBLES -> ParticleSpec(
        x = random.nextFloat(),
        speed = 3 + random.nextInt(5),
        phase = random.nextFloat(),
        scale = 4f + random.nextFloat() * 9f,
        swayAmount = 0.012f + random.nextFloat() * 0.035f,
        swayCycles = 2f + random.nextFloat() * 3f,
        swayPhase = random.nextFloat(),
        spin = 0f,
        spinPhase = 0f,
        alpha = 0.16f + random.nextFloat() * 0.34f,
        deep = index % 3 == 0,
      )
      // Leaves: the biggest and the most theatrical — hard tumble, wide swing.
      SeasonMotif.LEAVES -> ParticleSpec(
        x = random.nextFloat(),
        speed = 2 + random.nextInt(3),
        phase = random.nextFloat(),
        scale = 14f + random.nextFloat() * 15f,
        swayAmount = 0.09f + random.nextFloat() * 0.15f,
        swayCycles = 1.4f + random.nextFloat() * 2.2f,
        swayPhase = random.nextFloat(),
        spin = 1.6f + random.nextFloat() * 2.6f,
        spinPhase = random.nextFloat(),
        alpha = 0.26f + random.nextFloat() * 0.42f,
        deep = index % 3 == 0,
      )
      // Snow: slow, straight-ish, and turning almost imperceptibly.
      SeasonMotif.SNOW -> ParticleSpec(
        x = random.nextFloat(),
        speed = 1 + random.nextInt(3),
        phase = random.nextFloat(),
        scale = 6f + random.nextFloat() * 10f,
        swayAmount = 0.008f + random.nextFloat() * 0.030f,
        swayCycles = 0.6f + random.nextFloat() * 1.1f,
        swayPhase = random.nextFloat(),
        spin = 0.12f + random.nextFloat() * 0.30f,
        spinPhase = random.nextFloat(),
        alpha = 0.30f + random.nextFloat() * 0.45f,
        deep = index % 3 == 0,
      )
    }
  }

// ───────────────────────────────────────────────────────────────── unit shapes

/**
 * A sakura petal: a rounded teardrop with the notch at the wide end.
 *
 * The notch is the whole point — without it this is a generic blossom, and sakura is the
 * one shape in this app that has to be recognised instantly. Centred on the origin,
 * about 1.0 wide by 1.4 tall.
 */
private fun unitPetalPath(): Path = Path().apply {
  val w = 0.50f
  val h = 0.70f
  val notch = 0.22f
  moveTo(0f, -h)
  quadraticTo(w * 0.95f, -h * 0.55f, w, h * 0.18f)
  quadraticTo(w * 0.80f, h, 0f, h - notch)
  quadraticTo(-w * 0.80f, h, -w, h * 0.18f)
  quadraticTo(-w * 0.95f, -h * 0.55f, 0f, -h)
  close()
}

/**
 * A maple leaf: five lobes radiating from a short stem.
 *
 * Built by walking ten alternating radii around the circle — long ones for the lobe tips,
 * short ones for the valleys between them — which is the cheapest construction that
 * still reads as momiji rather than as a star. The lobes are pulled *upward* (the tip
 * lobe is longest) and a stem is tacked on below so a tumbling leaf has an obvious
 * heavy end.
 */
private fun unitMapleLeafPath(): Path = Path().apply {
  // Ten points: tip, valley, tip, valley … starting at the top lobe.
  val radii = floatArrayOf(0.72f, 0.26f, 0.62f, 0.24f, 0.55f, 0.20f, 0.55f, 0.24f, 0.62f, 0.26f)
  for (i in radii.indices) {
    // -90° puts point 0 at the top; leaves hang from their stem, so the tip leads.
    val angle = (-90f + i * 36f) * PI.toFloat() / 180f
    val r = radii[i]
    val px = cos(angle) * r * 0.9f
    val py = sin(angle) * r
    if (i == 0) moveTo(px, py) else lineTo(px, py)
  }
  close()
  // The stem: a stubby rectangle hanging off the bottom valley.
  moveTo(-0.045f, 0.24f)
  lineTo(0.045f, 0.24f)
  lineTo(0.045f, 0.60f)
  lineTo(-0.045f, 0.60f)
  close()
}

/**
 * A six-pointed snowflake, drawn as a path of six spokes with side branches.
 *
 * Six-fold symmetry is not decoration here, it is the *only* thing that makes a small
 * white mark read as snow rather than as dust. Each spoke gets a pair of barbs at 60°,
 * which is what survives at 8px.
 */
private fun unitSnowflakePath(): Path = Path().apply {
  val arm = 0.62f
  for (i in 0 until 6) {
    val angle = (i * 60f) * PI.toFloat() / 180f
    val ax = cos(angle)
    val ay = sin(angle)
    // The spoke itself, as a very thin quad so the path can be filled in one pass.
    val nx = -ay * 0.055f
    val ny = ax * 0.055f
    moveTo(nx, ny)
    lineTo(ax * arm + nx * 0.35f, ay * arm + ny * 0.35f)
    lineTo(ax * arm - nx * 0.35f, ay * arm - ny * 0.35f)
    lineTo(-nx, -ny)
    close()

    // Two barbs, branching forward at ±60° from two thirds along the spoke.
    val bx = ax * arm * 0.62f
    val by = ay * arm * 0.62f
    for (side in intArrayOf(-1, 1)) {
      val ba = angle + side * 60f * PI.toFloat() / 180f
      val ex = bx + cos(ba) * arm * 0.30f
      val ey = by + sin(ba) * arm * 0.30f
      val bnx = -(ey - by) * 0.10f
      val bny = (ex - bx) * 0.10f
      moveTo(bx + bnx, by + bny)
      lineTo(ex + bnx * 0.4f, ey + bny * 0.4f)
      lineTo(ex - bnx * 0.4f, ey - bny * 0.4f)
      lineTo(bx - bnx, by - bny)
      close()
    }
  }
}

// ──────────────────────────────────────────────────────────── the particle field

/**
 * The season's particles, drifting behind whatever sits in front of them.
 *
 * Driven by exactly **one** [rememberInfiniteTransition]: a single linear 0f..1f clock,
 * with every particle deriving its position from an integer speed multiplier plus its own
 * phase offset. Twenty transitions would mean twenty animation clocks for a decorative
 * background; integer multipliers mean each particle's journey wraps exactly when the
 * driver does, so nothing visibly teleports.
 *
 * Direction is per-season and deliberate. Spring petals, autumn leaves and winter snow
 * fall **down**; summer bubbles rise **up**, which is the single cheapest way to make
 * summer feel unlike the other three even before you register the colour.
 *
 * @param theme the resolved season — supplies both the shape and the two tones.
 * @param seed fixed by default so the scatter is stable and previews are reproducible.
 */
@Composable
fun SeasonParticleField(
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
  particleCount: Int = 18,
  seed: Int = 20260827,
  periodMillis: Int = 30_000,
) {
  val motif = remember(theme.kanji) { motifFor(theme) }
  val particles = remember(particleCount, motif, seed) {
    particleSpecs(particleCount, motif, Random(seed))
  }
  val shape = remember(motif) {
    when (motif) {
      SeasonMotif.PETALS -> unitPetalPath()
      SeasonMotif.LEAVES -> unitMapleLeafPath()
      SeasonMotif.SNOW -> unitSnowflakePath()
      SeasonMotif.BUBBLES -> null // circles are drawn directly; no path needed
    }
  }

  val transition = rememberInfiniteTransition(label = "kisetsu")
  val driver by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
    label = "kisetsu-driver",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val rising = motif == SeasonMotif.BUBBLES

    particles.forEach { p ->
      // frac() of an integer-scaled driver: continuous across the loop boundary.
      val t = (driver * p.speed + p.phase).let { it - floor(it) }
      // Travel a little beyond both edges so the wrap always happens off-canvas.
      val travel = if (rising) 1f - t else t
      val cy = -0.18f * h + travel * (h * 1.36f)
      val sway = sin((t * p.swayCycles + p.swayPhase) * 2f * PI.toFloat())
      val cx = (p.x + p.swayAmount * sway) * w
      val tone = if (p.deep) theme.accentDeep else theme.accent

      if (rising) {
        drawBubble(cx = cx, cy = cy, radius = p.scale, color = tone, alpha = p.alpha)
      } else {
        val degrees = (p.spinPhase + t * p.spin) * 360f
        withTransform({
          translate(cx, cy)
          rotate(degrees, Offset.Zero)
          scale(p.scale, p.scale, Offset.Zero)
        }) {
          shape?.let { drawPath(path = it, color = tone, alpha = p.alpha) }
        }
      }
    }
  }
}

/**
 * A water bubble: a soft filled disc with a ring and a small highlight.
 *
 * Drawn imperatively instead of as a shared [Path] because the three parts scale
 * non-uniformly — the highlight must stay a *fraction* of the radius, not a fixed
 * transform of it, or big bubbles end up looking like eyes.
 */
private fun DrawScope.drawBubble(cx: Float, cy: Float, radius: Float, color: androidx.compose.ui.graphics.Color, alpha: Float) {
  val centre = Offset(cx, cy)
  drawCircle(color = color, radius = radius, center = centre, alpha = alpha * 0.42f)
  drawCircle(
    color = color,
    radius = radius,
    center = centre,
    alpha = alpha,
    style = Stroke(width = (radius * 0.14f).coerceAtLeast(1f)),
  )
  drawCircle(
    color = color,
    radius = radius * 0.22f,
    center = Offset(cx - radius * 0.34f, cy - radius * 0.34f),
    alpha = alpha * 0.55f,
  )
}

// ─────────────────────────────────────────────────────────────── progress ring

/**
 * The season's progress ring: `done / total` as a filling arc in [SeasonTheme.progress].
 *
 * A ring rather than a bar because the rest of the screen is stacked cards and
 * horizontal rules — the circle is the one place the eye can land. It sweeps from twelve
 * o'clock with a [StrokeCap.Round] leading edge so it reads as ink rather than as a
 * cut-off, and animates through [animateFloatAsState] so a toggle glides instead of
 * snapping.
 *
 * Colour discipline: the ring is the *only* large object on the screen wearing the
 * progress hue, which is what lets a glance answer "how far am I?" without counting.
 */
@Composable
fun SeasonProgressRing(
  done: Int,
  total: Int,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
  diameter: Dp = 78.dp,
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

      // Track: a whisper of the progress hue, so the empty ring still belongs.
      drawArc(
        color = theme.progress.copy(alpha = 0.15f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )

      if (sweep > 0.001f) {
        drawArc(
          color = theme.progress,
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
        color = if (complete) theme.progress else theme.ink,
      )
      Text(
        text = "/$total",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        color = theme.inkSoft,
        modifier = Modifier.padding(start = 1.dp, bottom = 3.dp),
      )
    }
  }
}

// ─────────────────────────────────────────────────────────── completion bounce

/**
 * The spring a task card rides when it is checked off.
 *
 * Exposes a [scale] that overshoots then settles, plus a 0f..1f [wash] the card uses to
 * cross-fade toward the season's progress tint. The scale is one [Animatable] driven by a
 * medium-bouncy [spring]: the card should feel like it *settles into place*, which is the
 * cheapest way to make "done" feel earned rather than merely recorded.
 */
@Stable
class CompletionSpring internal constructor(
  internal val anim: Animatable<Float, AnimationVector1D>,
) {
  internal var firstPass: Boolean = true
  internal val washState: MutableFloatState = mutableFloatStateOf(0f)

  /** 1f at rest; overshoots briefly on toggle. */
  val scale: Float get() = anim.value

  /** 0f..1f — how strongly the progress-coloured wash is applied. */
  val wash: Float get() = washState.floatValue
}

/**
 * Remembers a completion spring bound to [done].
 *
 * The bounce fires only on an actual change, never on first composition, so opening the
 * app with three tasks already ticked is calm rather than a popcorn of animations.
 */
@Composable
fun rememberCompletionSpring(done: Boolean): CompletionSpring {
  val holder = remember { CompletionSpring(Animatable(1f)) }
  val wash by animateFloatAsState(
    targetValue = if (done) 1f else 0f,
    animationSpec = tween(durationMillis = 320),
    label = "season-wash",
  )
  holder.washState.floatValue = wash

  LaunchedEffect(done) {
    if (holder.firstPass) {
      holder.firstPass = false
      return@LaunchedEffect
    }
    holder.anim.snapTo(if (done) 0.95f else 1.025f)
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
