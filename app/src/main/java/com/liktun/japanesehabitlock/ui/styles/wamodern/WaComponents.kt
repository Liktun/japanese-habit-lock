package com.liktun.japanesehabitlock.ui.styles.wamodern

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import java.time.format.DateTimeFormatter

internal val WA_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ────────────────────────────────────────────────────────────── section header

/**
 * A section header: an English label in tracked-out small caps, a small Japanese
 * gloss beside it, and a gold-leaf hairline running out to the right margin.
 *
 * The kanji is deliberately *smaller* and in [WaPalette.warmGray] — it is a seasonal
 * garnish on the English, never a competing headline. The hairline gives every section
 * the same left-aligned "shoji rail" so the page reads as one grid.
 */
@Composable
fun WaSectionHeader(
  title: String,
  japanese: String,
  modifier: Modifier = Modifier,
  accent: Color = WaPalette.indigo,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = title.uppercase(),
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 12.sp,
      letterSpacing = 2.4.sp,
      color = accent,
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = japanese,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 11.sp,
      color = WaPalette.warmGray,
    )
    Spacer(Modifier.width(12.dp))
    Canvas(Modifier.weight(1f).height(1.dp)) {
      drawLine(
        color = WaPalette.goldLeaf,
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = size.height.coerceAtLeast(1f),
        alpha = 0.38f,
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────── header

/**
 * The screen's header card: phase, week and date, and the matcha progress ring,
 * with sakura petals drifting behind it all.
 *
 * The petals are clipped to the card's own rounded shape and drawn *under* the text at
 * low alpha, so the card still reads as paper with something falling past a window
 * rather than as a busy illustration.
 */
@Composable
fun WaHeaderCard(checklist: DailyChecklist, modifier: Modifier = Modifier) {
  WaCard(modifier = modifier, corner = 20.dp) {
    Box {
      SakuraPetalField(
        modifier = Modifier
          .matchParentSize()
          .clip(RoundedCornerShape(20.dp)),
        petalCount = 16,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            text = "日本語",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = WaPalette.warmGray,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = checklist.phase.label,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            color = WaPalette.indigo,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = checklist.phase.summary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = WaPalette.warmGray,
          )
          Spacer(Modifier.height(12.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            WaPill(text = "Week ${checklist.weekNumber}")
            Spacer(Modifier.width(8.dp))
            Text(
              text = checklist.day.date.format(WA_DAY_FORMAT),
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Normal,
              fontSize = 12.sp,
              color = WaPalette.warmGray,
            )
          }
        }
        Spacer(Modifier.width(14.dp))
        MatchaProgressRing(done = checklist.blockingDone, total = checklist.blockingTotal)
      }
    }
  }
}

/** A small sakura-tinted capsule used for the week number. */
@Composable
private fun WaPill(text: String) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .background(WaPalette.sakura.copy(alpha = 0.22f))
      .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      letterSpacing = 0.6.sp,
      color = WaPalette.sakuraDeep,
    )
  }
}

// ────────────────────────────────────────────────────────────────── gate banner

/**
 * The locked / unlocked banner.
 *
 * Intentionally *warm*: locked is a soft sakura wash with an encouraging count, not a
 * red alarm. Nothing has gone wrong when the day is not finished yet — the copy and the
 * colour should both say "here's what's left", so the banner reads as a bookmark rather
 * than an error state. Unlocked graduates to matcha, closing the colour loop with the
 * progress ring.
 */
@Composable
fun WaGateBanner(checklist: DailyChecklist, modifier: Modifier = Modifier) {
  val unlocked = checklist.isUnlocked
  val tint = if (unlocked) WaPalette.matcha else WaPalette.sakura
  val ink = if (unlocked) WaPalette.matchaDeep else WaPalette.sakuraDeep
  val remaining = (checklist.blockingTotal - checklist.blockingDone).coerceAtLeast(0)

  Surface(
    color = tint.copy(alpha = 0.14f),
    shape = RoundedCornerShape(18.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
      // A filled dot rather than an icon: quieter, and it keeps the palette closed.
      Box(
        Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(ink.copy(alpha = 0.85f)),
      )
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = if (unlocked) "Unlocked" else "Locked",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = ink,
          )
          Spacer(Modifier.width(7.dp))
          Text(
            text = if (unlocked) "開" else "閉",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            color = WaPalette.warmGray,
            modifier = Modifier.padding(bottom = 2.dp),
          )
        }
        Spacer(Modifier.height(3.dp))
        Text(
          text = if (unlocked) {
            "Everything required is done. Your apps are open — enjoy them."
          } else {
            "${checklist.blockingDone} of ${checklist.blockingTotal} done · " +
              if (remaining == 1) "one more to go." else "$remaining more to go."
          },
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          color = WaPalette.indigo.copy(alpha = 0.82f),
        )
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────── custom checkbox

/**
 * The custom checkbox: a soft rounded square that fills with matcha and strokes a
 * checkmark on.
 *
 * Material's [androidx.compose.material3.Checkbox] is deliberately not used — its
 * corner radius, ripple and tick timing all belong to a different design language.
 * Here the box swells from an empty warm-gray outline to a filled matcha tile, and the
 * tick is drawn progressively along its own [Path] with [PathMeasure] so the stroke
 * reads as a brush being laid down, not as an icon appearing.
 */
@Composable
fun WaCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  size: androidx.compose.ui.unit.Dp = 26.dp,
) {
  val progress by animateFloatAsState(
    targetValue = if (checked) 1f else 0f,
    animationSpec = tween(durationMillis = 280),
    label = "checkbox",
  )
  val tickPath = remember { Path() }
  val measure = remember { PathMeasure() }
  val segment = remember { Path() }
  val interaction = remember { MutableInteractionSource() }

  Canvas(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(9.dp))
      .clickable(
        interactionSource = interaction,
        indication = null,
        role = androidx.compose.ui.semantics.Role.Checkbox,
      ) { onCheckedChange(!checked) },
  ) {
    val s = this.size.minDimension
    val radius = androidx.compose.ui.geometry.CornerRadius(s * 0.30f, s * 0.30f)
    val fillAlpha = progress

    // Empty state: a hairline warm-gray tile, so an unchecked row is quiet.
    drawRoundRect(
      color = WaPalette.warmGray.copy(alpha = 0.34f * (1f - fillAlpha) + 0.0f),
      cornerRadius = radius,
      style = Stroke(width = 1.6f.dp.toPx()),
    )
    // Checked state: matcha fill fading in underneath.
    if (fillAlpha > 0.001f) {
      drawRoundRect(color = WaPalette.matcha, cornerRadius = radius, alpha = fillAlpha)
    }

    if (progress > 0.02f) {
      tickPath.reset()
      tickPath.moveTo(s * 0.26f, s * 0.52f)
      tickPath.lineTo(s * 0.44f, s * 0.70f)
      tickPath.lineTo(s * 0.76f, s * 0.32f)

      measure.setPath(tickPath, false)
      segment.reset()
      // Slight lead-in so the fill lands before the stroke starts travelling.
      val drawn = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
      measure.getSegment(0f, measure.length * drawn, segment, true)
      drawPath(
        path = segment,
        color = WaPalette.surface,
        style = Stroke(
          width = s * 0.11f,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )
    }
  }
}

// ────────────────────────────────────────────────────────────────── task card

/**
 * One task row, as a soft card.
 *
 * The whole body toggles the task, but the "Open" affordance is a separate target so
 * tapping *go do this* never silently marks it done at the same time — the original
 * screen got that right and it survives the restyle.
 *
 * On completion the card rides a [CompletionSpring]: a medium-bouncy scale overshoot
 * plus a matcha wash that tints the surface, softens the title and pushes the whole row
 * back visually, so finished work recedes and the remaining work is what your eye lands
 * on.
 */
@Composable
fun WaTaskCard(
  task: RoadmapTask,
  done: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
) {
  val bounce = rememberCompletionSpring(done)
  val wash = bounce.wash
  val surface = lerp(WaPalette.surface, WaPalette.matcha.copy(alpha = 0.5f), wash * 0.22f)
  val titleColor = lerp(WaPalette.indigo, WaPalette.matchaDeep, wash)

  WaCard(
    modifier = modifier.completionBounce(bounce),
    corner = 18.dp,
    color = surface,
    borderAlpha = 0.05f + 0.10f * wash,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
    ) {
      WaCheckbox(checked = done, onCheckedChange = onToggle)
      Spacer(Modifier.width(14.dp))
      Column(
        Modifier
          .weight(1f)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) { onToggle(!done) },
      ) {
        Text(
          text = task.title,
          fontFamily = FontFamily.SansSerif,
          fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold,
          fontSize = 15.sp,
          lineHeight = 20.sp,
          color = titleColor.copy(alpha = 1f - 0.22f * wash),
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = task.detail,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 12.5.sp,
          lineHeight = 17.sp,
          color = WaPalette.warmGray.copy(alpha = 1f - 0.28f * wash),
        )
      }
      if (onOpen != null) {
        Spacer(Modifier.width(8.dp))
        WaOpenButton(onClick = onOpen)
      }
    }
  }
}

/**
 * The "open this tool" affordance: an outlined sakura capsule.
 *
 * Outlined rather than filled because it is a *side door* — the primary action on every
 * row is the checkbox, and a solid button here would out-shout it.
 */
@Composable
fun WaOpenButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(CircleShape)
      .border(1.dp, WaPalette.sakuraDeep.copy(alpha = 0.45f), CircleShape)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "Open",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 12.sp,
      letterSpacing = 0.5.sp,
      color = WaPalette.sakuraDeep,
    )
  }
}

// ──────────────────────────────────────────────────────────── focus + checkpoints

/** The one-line weekly framing, set as a quiet pull-quote on paper. */
@Composable
fun WaFocusCard(focus: String, modifier: Modifier = Modifier) {
  WaCard(modifier = modifier, corner = 18.dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      // A single sakura rule standing in for a vertical brush stroke.
      Box(
        Modifier
          .width(3.dp)
          .height(38.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(WaPalette.sakura),
      )
      Spacer(Modifier.width(14.dp))
      Text(
        text = focus,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = WaPalette.indigo,
      )
    }
  }
}

/**
 * The weekly checkpoints.
 *
 * Explicitly *not* checkboxes: these never gate anything, so giving them the same
 * affordance as a blocking task would be a lie about what the app requires. They get
 * gold-leaf dots instead — decorative, inert, and unmistakably a different class of
 * thing.
 */
@Composable
fun WaCheckpointList(lines: List<String>, modifier: Modifier = Modifier) {
  WaCard(modifier = modifier, corner = 18.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      lines.forEach { line ->
        Row(verticalAlignment = Alignment.Top) {
          Box(
            Modifier
              .padding(top = 6.dp)
              .size(5.dp)
              .clip(CircleShape)
              .background(WaPalette.goldLeaf.copy(alpha = 0.75f)),
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = line,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = WaPalette.indigo.copy(alpha = 0.86f),
          )
        }
      }
    }
  }
}

/**
 * A gentle nudge toward the next phase, shown only when the roadmap has one.
 *
 * Framed as an invitation ("when you're ready"), because advancing is a manual,
 * felt decision in this app and the UI must not imply it is overdue.
 */
@Composable
fun WaPhasePromptCard(prompt: String, modifier: Modifier = Modifier) {
  Surface(
    color = WaPalette.goldLeaf.copy(alpha = 0.09f),
    shape = RoundedCornerShape(18.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = "WHEN YOU'RE READY",
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp,
          letterSpacing = 1.8.sp,
          color = WaPalette.indigo.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "次へ",
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 10.sp,
          color = WaPalette.warmGray,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = prompt,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = WaPalette.indigo.copy(alpha = 0.85f),
      )
    }
  }
}

// ───────────────────────────────────────────────────────────────── card shell

/**
 * The shared card shell: white paper on cream, soft 16–20dp corners, and a hairline
 * border instead of a heavy drop shadow.
 *
 * WA-MODERN is the one style in this app allowed to use cards, so the elevation is kept
 * *layered but shallow* — a 1dp tonal edge plus a small shadow reads as a sheet of paper
 * resting on another sheet, which is the effect we want. A big Material elevation would
 * make it float like a dialog.
 */
@Composable
fun WaCard(
  modifier: Modifier = Modifier,
  corner: androidx.compose.ui.unit.Dp = 18.dp,
  color: Color = WaPalette.surface,
  borderAlpha: Float = 0.06f,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(corner)
  Surface(
    color = color,
    shape = shape,
    shadowElevation = 1.dp,
    tonalElevation = 0.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, WaPalette.indigo.copy(alpha = borderAlpha), shape),
  ) {
    content()
  }
}
