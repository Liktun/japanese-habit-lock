package com.liktun.japanesehabitlock.ui.styles.neon

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings

// ---------------------------------------------------------------------------
// Typography — one family, deliberate tracking, uppercase for anything structural.
// ---------------------------------------------------------------------------

/** Wide-tracked uppercase micro-label. The signage voice of the style. */
internal val NeonLabelStyle =
  TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 3.sp,
  )

/** Tight-tracked heading. Condensed on purpose, so it reads as a fabricated sign. */
internal val NeonTitleStyle =
  TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    letterSpacing = (-0.5).sp,
  )

/** Body copy. Monospace like everything else, but relaxed enough to read. */
internal val NeonBodyStyle =
  TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 0.2.sp,
    lineHeight = 17.sp,
  )

// ---------------------------------------------------------------------------
// Panel
// ---------------------------------------------------------------------------

/**
 * The one container primitive: a slightly-lighter-than-black rectangle with a 1dp
 * glowing tube around it.
 *
 * Explicitly not a Material `Card`/`Surface` — no elevation, no tonal fill, no
 * rounded pill. The border *is* the light source, so [NeonPalette.Panel] only has to
 * lift the interior far enough off [NeonPalette.Night] for the edge to read.
 * [intensity] lets callers dim a panel that is not the current point of interest,
 * which is how the screen keeps four saturated colours from fighting.
 */
@Composable
fun NeonPanel(
  color: Color,
  modifier: Modifier = Modifier,
  intensity: Float = 1f,
  glowRadius: Dp = 10.dp,
  contentPadding: Dp = 14.dp,
  content: @Composable () -> Unit,
) {
  Box(
    modifier =
      modifier
        .neonGlow(
          color = color,
          radius = glowRadius,
          cornerRadius = 3.dp,
          borderWidth = 1.dp,
          fill = NeonPalette.Panel,
          intensity = intensity,
        )
        .padding(contentPadding)
  ) {
    content()
  }
}

// ---------------------------------------------------------------------------
// Section labels
// ---------------------------------------------------------------------------

/**
 * A section rule: an optional 「日本語」 garnish, the heading, then a hairline that fades
 * out to the right.
 *
 * The garnish is a lit kanji at the tube colour and the heading carries the meaning at
 * [NeonPalette.TextDim]. That pairing only makes sense while the heading is English —
 * once the immersion ramp turns the heading itself into 必須, printing 「必須」必須 beside
 * it is the same word twice. So [garnish] is nullable and callers drop it at that point
 * (see `Immersion.headingsAreEnglish`); the heading then stands alone with its kana set
 * above it in unlit type, and the row keeps exactly the same height because [Ruby]
 * reserves nothing when there is no reading.
 *
 * The trailing rule fades rather than stopping, which keeps the layout from looking like
 * a table of boxes.
 *
 * @param label the heading, already immersion-resolved. Uppercased on the way out, which
 *   is a no-op on kanji, so one call site serves both languages.
 * @param ruby the heading's kana reading, or null.
 * @param garnish the bracketed decorative kanji, or null once the heading is Japanese.
 * @param trailing a short right-aligned annotation — a count, or "never blocks".
 */
@Composable
fun NeonSectionLabel(
  label: String,
  color: Color,
  modifier: Modifier = Modifier,
  ruby: String? = null,
  garnish: String? = null,
  trailing: String? = null,
  trailingRuby: String? = null,
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
    if (garnish != null) {
      Text(
        text = "「$garnish」",
        color = color,
        style =
          NeonLabelStyle.copy(
            fontSize = 11.sp,
            letterSpacing = 0.sp,
            shadow = neonTextGlow(color, radius = 12f, alpha = 0.7f),
          ),
      )
      Spacer(Modifier.width(8.dp))
    }
    Ruby(
      text = label.uppercase(),
      reading = ruby,
      style = NeonLabelStyle,
      color = NeonPalette.TextDim,
      rubyColor = NEON_RUBY_INK,
    )
    Spacer(Modifier.width(10.dp))
    Box(
      Modifier
        .weight(1f)
        .height(1.dp)
        .background(
          androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(color.copy(alpha = 0.5f), Color.Transparent)
          )
        )
    )
    if (trailing != null) {
      Spacer(Modifier.width(10.dp))
      Ruby(
        text = trailing.uppercase(),
        reading = trailingRuby,
        style = NeonLabelStyle,
        color = color.copy(alpha = 0.8f),
        rubyColor = NEON_RUBY_INK,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Sign header
// ---------------------------------------------------------------------------

/**
 * The shop sign at the mouth of the alley: the phase name in big flickering tube type,
 * with the week, date and phase summary set quietly underneath.
 *
 * Only this element flickers. Neon flicker is loud, and applying it to more than one
 * thing turns the screen into noise — so the title takes it and everything else stays
 * steady, which by contrast makes the title feel like it is genuinely failing.
 *
 * The eyebrow row is the clearest case of the garnish rule: at English level it reads
 * 「今日」TODAY, a lit kanji beside its translation. Once the ramp turns the label itself
 * into 今日 the bracket is dropped, because 「今日」今日 is the same word printed twice.
 *
 * @param titleColor magenta while the gate is shut, cyan once it is open.
 * @param immersion the screen's single resolver; every word in this header goes through it.
 */
@Composable
fun NeonSignHeader(
  phaseLabel: String,
  phaseSummary: String,
  weekNumber: Int,
  formattedDate: String,
  titleColor: Color,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  val flicker = rememberNeonFlicker()
  val garnished = immersion.headingsAreEnglish()

  Column(modifier = modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (garnished) {
        Text(
          text = "「今日」",
          color = NeonPalette.Amber,
          style = NeonLabelStyle.copy(fontSize = 11.sp, letterSpacing = 0.sp, shadow = neonTextGlow(NeonPalette.Amber, 12f, 0.7f)),
        )
        Spacer(Modifier.width(8.dp))
      }
      Ruby(
        text = immersion.sectionLabel(ImmersionStrings.TODAY).uppercase(),
        reading = immersion.sectionRuby(ImmersionStrings.TODAY),
        style = NeonLabelStyle,
        color = NeonPalette.TextDim,
        rubyColor = NEON_RUBY_INK,
      )
      Spacer(Modifier.weight(1f))
      // 「週」 stays bracketed at every level: it garnishes the *number*, not a word,
      // so it is never a duplicate of anything.
      Text(
        text = "「週」 W$weekNumber",
        color = NeonPalette.Violet,
        style = NeonLabelStyle.copy(letterSpacing = 1.sp, shadow = neonTextGlow(NeonPalette.Violet, 10f, 0.6f)),
      )
    }

    Spacer(Modifier.height(10.dp))

    // The sign itself. Flicker is a draw-phase alpha so recomposition stays out of it.
    Box(modifier = Modifier.neonFlicker(flicker)) {
      Text(
        text = phaseLabel.uppercase(),
        color = NeonPalette.Text,
        style = NeonTitleStyle.copy(shadow = neonTextGlow(titleColor, radius = 26f, alpha = 0.95f)),
      )
    }

    Spacer(Modifier.height(6.dp))
    Text(text = phaseSummary, color = NeonPalette.TextDim, style = NeonBodyStyle)
    Spacer(Modifier.height(8.dp))
    Text(
      text = formattedDate.uppercase(),
      color = NeonPalette.TextFaint,
      style = NeonLabelStyle.copy(fontWeight = FontWeight.Normal, letterSpacing = 2.sp),
    )
  }
}

// ---------------------------------------------------------------------------
// Task row
// ---------------------------------------------------------------------------

/**
 * One checklist line: diamond check, title, detail, and an optional OPEN affordance.
 *
 * Two independent hit targets, matching the plain screen's rule — the text column
 * toggles, the OPEN bracket launches — so "go do this" can never silently mark a task
 * done. Done rows dim to ~45% and strike through: the eye should slide past them and
 * land on what is still owed.
 *
 * Title, detail and the launch label all resolve through [immersion]. The title's kana
 * is rendered by [Ruby] and aligned Start inside the text column, so the reading is
 * centred on the *phrase* while the phrase itself stays flush against the checkbox
 * gutter — the column of titles does not wander as rows change width. Note the ruby is
 * unlit by construction ([NEON_RUBY_INK] + [NEON_RUBY_STYLE]) even though the title it
 * sits above is glowing.
 *
 * @param accent the tube colour for this row. Cyan when done, [accent] otherwise —
 *   magenta for blocking work, amber for optional.
 */
@Composable
fun NeonTaskRow(
  task: RoadmapTask,
  done: Boolean,
  accent: Color,
  immersion: Immersion,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
) {
  val liveColor = if (done) NeonPalette.Cyan else accent
  val dim by
    animateFloatAsState(
      targetValue = if (done) 0.45f else 1f,
      animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
      label = "neon-task-dim",
    )

  NeonPanel(
    color = liveColor,
    modifier = modifier.fillMaxWidth(),
    intensity = if (done) 0.85f else 0.55f,
    glowRadius = 8.dp,
    contentPadding = 12.dp,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      NeonDiamondCheck(
        checked = done,
        color = liveColor,
        modifier = Modifier.clickable { onToggle(!done) },
      )
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f).clickable { onToggle(!done) }) {
        Ruby(
          text = immersion.taskTitle(task.id, task.title).uppercase(),
          reading = immersion.taskTitleRuby(task.id, task.title),
          style =
            TextStyle(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              letterSpacing = 0.6.sp,
              textDecoration = if (done) TextDecoration.LineThrough else null,
              shadow = if (done) null else neonTextGlow(liveColor, radius = 10f, alpha = 0.35f),
            ),
          color = NeonPalette.Text.copy(alpha = dim),
          rubyColor = NEON_RUBY_INK.copy(alpha = NEON_RUBY_INK.alpha * dim),
          modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = immersion.taskDetail(task.id, task.detail),
          color = NeonPalette.TextDim.copy(alpha = NeonPalette.TextDim.alpha * dim),
          style = NeonBodyStyle.copy(fontSize = 11.sp, lineHeight = 15.sp),
        )
      }
      if (onOpen != null) {
        Spacer(Modifier.width(10.dp))
        NeonOpenButton(
          color = if (done) NeonPalette.Violet else NeonPalette.Cyan,
          label = immersion.action(ImmersionStrings.OPEN_ACTION),
          reading = immersion.ruby(ImmersionStrings.OPEN_ACTION, ImmersionPlan.ACTIONS),
          onClick = onOpen,
        )
      }
    }
  }
}

/**
 * The bracketed "go do this now" chip on launchable tasks.
 *
 * The label used to be a hardcoded 起動 glyph, which was safe only because it was
 * decoration. Now it is a real phrase on the ramp — OPEN early, 開く with ひらく later —
 * so the chip wraps its content instead of assuming one width, and the reading rides
 * above it unlit like every other ruby in the alley.
 */
@Composable
private fun NeonOpenButton(
  color: Color,
  label: String,
  reading: String?,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(2.dp))
        .clickable(onClick = onClick)
        .neonGlow(color = color, radius = 7.dp, cornerRadius = 2.dp, borderWidth = 1.dp, intensity = 0.5f)
        .padding(horizontal = 9.dp, vertical = 6.dp)
  ) {
    Ruby(
      text = label.uppercase(),
      reading = reading,
      style = NeonLabelStyle.copy(fontSize = 11.sp, letterSpacing = 1.sp, shadow = neonTextGlow(color, 10f, 0.6f)),
      color = color,
      rubyColor = NEON_RUBY_INK,
    )
  }
}

// ---------------------------------------------------------------------------
// Gate status
// ---------------------------------------------------------------------------

/**
 * The lock readout: LOCKED in magenta, or UNLOCKED in cyan.
 *
 * This is the loudest element on the screen and the only one that breathes — a slow
 * ~2.6s [rememberNeonBreath] swelling the border glow and the status glyph together,
 * so at rest the screen has a heartbeat. The charge bar underneath takes the same
 * colour, tying "how far along" directly to "why you are still locked out".
 *
 * The state word ramps through [Immersion.gate] into 施錠中 / 解錠 with its kana above.
 * While it is still English the row keeps its 「施錠」/「解錠」 garnish above the word, in
 * the tube colour; once the word itself is Japanese the garnish is dropped, since the
 * bracket would otherwise be repeating the sign directly underneath it. The garnish
 * sitting where the ruby will later sit is deliberate — the row's height is stable
 * across the transition instead of collapsing and regrowing.
 */
@Composable
fun NeonGateStatus(
  unlocked: Boolean,
  blockingDone: Int,
  blockingTotal: Int,
  progress: Float,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  val color = if (unlocked) NeonPalette.Cyan else NeonPalette.Magenta
  val breath by rememberNeonBreath()
  // Never fully dark: the tube dips, it does not switch off.
  val intensity = 0.45f + 0.55f * breath
  val phrase = if (unlocked) ImmersionStrings.UNLOCKED else ImmersionStrings.LOCKED

  NeonPanel(color = color, modifier = modifier.fillMaxWidth(), intensity = intensity, glowRadius = 16.dp) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        NeonLockGlyph(unlocked = unlocked, color = color, intensity = intensity)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
          if (immersion.gateWordIsEnglish()) {
            Text(
              text = if (unlocked) "「解錠」" else "「施錠」",
              color = color,
              style = NeonLabelStyle.copy(fontSize = 12.sp, letterSpacing = 1.sp, shadow = neonTextGlow(color, 14f, intensity)),
            )
            Spacer(Modifier.height(3.dp))
          }
          Ruby(
            text = immersion.gate(phrase).uppercase(),
            reading = immersion.ruby(phrase, ImmersionPlan.GATE_STATE),
            style =
              TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 4.sp,
                shadow = neonTextGlow(color, radius = 20f, alpha = intensity),
              ),
            color = NeonPalette.Text,
            rubyColor = NEON_RUBY_INK,
            modifier = Modifier.align(Alignment.Start),
          )
        }
        Text(
          text = "$blockingDone/$blockingTotal",
          color = color,
          style =
            TextStyle(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              letterSpacing = (-1).sp,
              shadow = neonTextGlow(color, radius = 18f, alpha = intensity),
            ),
        )
      }

      Spacer(Modifier.height(12.dp))
      NeonChargeBar(progress = progress, color = color, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(8.dp))
      Text(
        // Counted prose switches with the rest of the prose rather than with the gate
        // word: the digits read the same in both languages, so it is safe to hold back.
        text =
          if (unlocked) {
            "ALL REQUIRED TASKS CLEARED — DISTRACTION APPS ARE OPEN."
          } else {
            immersion
              .text(ImmersionStrings.gateSummary(blockingDone, blockingTotal), ImmersionPlan.PROSE)
              .uppercase() + " — DISTRACTION APPS STAY BLOCKED."
          },
        color = NeonPalette.TextDim,
        style = NeonBodyStyle.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
      )
    }
  }
}

/**
 * A hand-drawn padlock that breaks open when the gate clears.
 *
 * Drawn rather than iconed so the shackle can be animated: `openness` swings the arc
 * up and to the right and swaps the tube colour, and the body's halo is stacked in
 * the same additive style as [neonGlow] so it blooms with the rest of the panel.
 */
@Composable
private fun NeonLockGlyph(unlocked: Boolean, color: Color, intensity: Float) {
  val openness by
    animateFloatAsState(
      targetValue = if (unlocked) 1f else 0f,
      animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      label = "neon-lock-open",
    )

  androidx.compose.foundation.Canvas(Modifier.size(30.dp, 36.dp)) {
    val stroke = 2.dp.toPx()
    val bodyTop = size.height * 0.44f
    val bodyW = size.width * 0.78f
    val bodyH = size.height - bodyTop
    val bodyX = (size.width - bodyW) / 2f

    // Shackle: lifts and tilts open.
    val shackleR = bodyW * 0.32f
    val shackleCx = size.width / 2f + openness * shackleR * 0.9f
    val shackleCy = bodyTop - shackleR * 0.15f - openness * shackleR * 0.5f
    for (layer in 3 downTo 1) {
      drawArc(
        color = color.copy(alpha = 0.18f * intensity / layer),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(shackleCx - shackleR, shackleCy - shackleR),
        size = androidx.compose.ui.geometry.Size(shackleR * 2f, shackleR * 2f),
        style = Stroke(width = stroke + layer * 4f),
        blendMode = BlendMode.Plus,
      )
    }
    drawArc(
      color = color,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = androidx.compose.ui.geometry.Offset(shackleCx - shackleR, shackleCy - shackleR),
      size = androidx.compose.ui.geometry.Size(shackleR * 2f, shackleR * 2f),
      style = Stroke(width = stroke),
    )

    // Body.
    for (layer in 3 downTo 1) {
      drawRoundRect(
        color = color.copy(alpha = 0.16f * intensity / layer),
        topLeft = androidx.compose.ui.geometry.Offset(bodyX, bodyTop),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        style = Stroke(width = stroke + layer * 4f),
        blendMode = BlendMode.Plus,
      )
    }
    drawRoundRect(
      color = color,
      topLeft = androidx.compose.ui.geometry.Offset(bodyX, bodyTop),
      size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
      style = Stroke(width = stroke),
    )
    drawCircle(
      color = color.copy(alpha = 0.5f + 0.5f * intensity),
      radius = 2.dp.toPx(),
      center = androidx.compose.ui.geometry.Offset(size.width / 2f, bodyTop + bodyH * 0.45f),
    )
  }
}

// ---------------------------------------------------------------------------
// Week focus & checkpoints
// ---------------------------------------------------------------------------

/**
 * The week's one-line framing, set as a violet-bordered panel with an oversized
 * quote glyph — secondary information, deliberately cooler than the gate.
 */
@Composable
fun NeonFocusPanel(focus: String, weekNumber: Int, modifier: Modifier = Modifier) {
  NeonPanel(
    color = NeonPalette.Violet,
    modifier = modifier.fillMaxWidth(),
    intensity = 0.5f,
    glowRadius = 9.dp,
  ) {
    Row {
      Text(
        text = "◤",
        color = NeonPalette.Violet.copy(alpha = 0.7f),
        style = NeonLabelStyle.copy(fontSize = 13.sp, shadow = neonTextGlow(NeonPalette.Violet, 10f, 0.6f)),
      )
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
          text = "WEEK $weekNumber FOCUS",
          color = NeonPalette.Violet,
          style = NeonLabelStyle.copy(shadow = neonTextGlow(NeonPalette.Violet, 10f, 0.5f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(text = focus, color = NeonPalette.Text.copy(alpha = 0.86f), style = NeonBodyStyle)
      }
    }
  }
}

/**
 * The weekly self-check, rendered as numbered violet entries rather than bullets.
 *
 * Numbering (not ticks) is the point: these are explicitly *not* a daily gate, so
 * they must never look like something the user can fail to check off.
 */
@Composable
fun NeonCheckpointList(checkpoints: List<String>, modifier: Modifier = Modifier) {
  NeonPanel(
    color = NeonPalette.Violet,
    modifier = modifier.fillMaxWidth(),
    intensity = 0.4f,
    glowRadius = 8.dp,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      checkpoints.forEachIndexed { index, line ->
        Row(verticalAlignment = Alignment.Top) {
          Text(
            text = "%02d".format(index + 1),
            color = NeonPalette.Violet,
            style = NeonLabelStyle.copy(fontSize = 11.sp, letterSpacing = 0.sp, shadow = neonTextGlow(NeonPalette.Violet, 9f, 0.6f)),
          )
          Spacer(Modifier.width(12.dp))
          Text(text = line, color = NeonPalette.TextDim, style = NeonBodyStyle, modifier = Modifier.weight(1f))
        }
      }
      Text(
        text = "NOT A DAILY GATE — REVIEW ONCE A WEEK.",
        color = NeonPalette.TextFaint,
        style = NeonLabelStyle.copy(fontWeight = FontWeight.Normal, letterSpacing = 1.5.sp),
      )
    }
  }
}

/**
 * The amber "when you're ready" nudge toward the next phase.
 *
 * Amber, not magenta: it is a suggestion the app has no way to verify, so it must
 * read as a lantern over the door rather than as another thing holding the gate shut.
 */
@Composable
fun NeonPhasePrompt(prompt: String, modifier: Modifier = Modifier) {
  NeonPanel(
    color = NeonPalette.Amber,
    modifier = modifier.fillMaxWidth(),
    intensity = 0.55f,
    glowRadius = 10.dp,
  ) {
    Column {
      Text(
        text = "「次」 WHEN YOU'RE READY",
        color = NeonPalette.Amber,
        style = NeonLabelStyle.copy(letterSpacing = 1.5.sp, shadow = neonTextGlow(NeonPalette.Amber, 12f, 0.7f)),
      )
      Spacer(Modifier.height(7.dp))
      Text(text = prompt, color = NeonPalette.Text.copy(alpha = 0.82f), style = NeonBodyStyle)
    }
  }
}

/** Bottom-of-alley sign-off, so the scroll ends on something rather than stopping. */
@Composable
fun NeonFooter(checklist: DailyChecklist, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = "ネオン横丁",
      color = NeonPalette.Violet.copy(alpha = 0.55f),
      style = NeonLabelStyle.copy(fontSize = 12.sp, letterSpacing = 6.sp, shadow = neonTextGlow(NeonPalette.Violet, 12f, 0.4f)),
    )
    Spacer(Modifier.height(5.dp))
    Text(
      text = "DAY ${checklist.day.key} · ${checklist.tasks.size} TASKS TRACKED",
      color = NeonPalette.TextFaint,
      style = NeonLabelStyle.copy(fontWeight = FontWeight.Normal, fontSize = 9.sp, letterSpacing = 1.5.sp),
    )
  }
}
