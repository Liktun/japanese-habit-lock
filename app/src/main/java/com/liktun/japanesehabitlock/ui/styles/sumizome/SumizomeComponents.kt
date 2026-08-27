package com.liktun.japanesehabitlock.ui.styles.sumizome

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings
import com.liktun.japanesehabitlock.domain.immersion.Phrase
import java.time.format.DateTimeFormatter

internal val SUMIZOME_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ────────────────────────────────────────────────────────────────────── ruby

/**
 * Text with an optional furigana reading set above it.
 *
 * This is the single most important composable in the style, because it is what makes
 * the immersion ramp *usable* rather than merely present. A week-9 user is shown
 * 文法の復習 instead of "Bunpro grammar reviews done" — that is only a lesson and not a
 * wall because ぶんぽうのふくしゅう is sitting above it in small type.
 *
 * Two rules the layout has to obey:
 *
 * 1. **No reserved gap when [ruby] is null.** English weeks and post-furigana weeks must
 *    render the bare text with the composable collapsing to exactly what a plain [Text]
 *    would occupy. If a null ruby left an empty 10sp band behind, every card would grow
 *    a phantom margin at week 3 and shrink again at week 13, and the ramp would read as
 *    a layout bug rather than as a milestone. So the null case short-circuits to a plain
 *    [Text] with the caller's modifier — no wrapper, no spacer.
 * 2. **The reading is centred over the word and never competes with it.** It is set at
 *    ~9.5sp in [SumizomePalette.washiSoft] with a line height barely taller than the
 *    glyphs, so it sits close enough to belong to the word beneath it.
 *
 * The reading uses the one colour explicitly *not* allowed to carry essential meaning,
 * which is exactly right: the ruby is a hint, and the phrase below it is the content.
 *
 * @param text the phrase itself, already resolved for the current immersion level.
 * @param ruby the kana reading, or null when it should not be shown at all.
 */
@Composable
fun Ruby(
  text: String,
  ruby: String?,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 15.sp,
  lineHeight: TextUnit = 20.sp,
  fontWeight: FontWeight = FontWeight.SemiBold,
  color: Color = SumizomePalette.washi,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  rubySize: TextUnit = 9.5.sp,
  rubyColor: Color = SumizomePalette.washiSoft,
  textAlign: TextAlign? = null,
) {
  if (ruby.isNullOrBlank()) {
    // The whole point: no Column, no Spacer, nothing that could reserve height.
    Text(
      text = text,
      modifier = modifier,
      fontFamily = FontFamily.SansSerif,
      fontWeight = fontWeight,
      fontSize = fontSize,
      lineHeight = lineHeight,
      letterSpacing = letterSpacing,
      color = color,
      textAlign = textAlign,
    )
    return
  }

  // The column shrink-wraps to the wider of the two lines, so a short reading centres
  // over a long phrase and a long reading centres over a short one.
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = ruby,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = rubySize,
      // Tight: a default line height on 9.5sp text would push the reading a visible
      // step away from the word it belongs to.
      lineHeight = rubySize * 1.08f,
      letterSpacing = 0.2.sp,
      color = rubyColor,
      textAlign = TextAlign.Center,
    )
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = fontWeight,
      fontSize = fontSize,
      lineHeight = lineHeight,
      letterSpacing = letterSpacing,
      color = color,
      textAlign = textAlign ?: TextAlign.Center,
    )
  }
}

/** Convenience overload: renders a [Phrase] at [plan]'s switch level, ruby and all. */
@Composable
fun Ruby(
  immersion: Immersion,
  phrase: Phrase,
  plan: com.liktun.japanesehabitlock.domain.immersion.ImmersionLevel,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 15.sp,
  lineHeight: TextUnit = 20.sp,
  fontWeight: FontWeight = FontWeight.SemiBold,
  color: Color = SumizomePalette.washi,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  rubySize: TextUnit = 9.5.sp,
  textAlign: TextAlign? = null,
) {
  Ruby(
    text = immersion.text(phrase, plan),
    ruby = immersion.ruby(phrase, plan),
    modifier = modifier,
    fontSize = fontSize,
    lineHeight = lineHeight,
    fontWeight = fontWeight,
    color = color,
    letterSpacing = letterSpacing,
    rubySize = rubySize,
    textAlign = textAlign,
  )
}

// ─────────────────────────────────────────────────────────────── card shell

/**
 * The shared card shell: a lifted charcoal panel on the night ground, 16–20dp corners,
 * separated by a 1dp hairline.
 *
 * The border is doing the job elevation does in the daylight sibling. On a dark ground a
 * drop shadow is invisible — there is nothing darker for it to be darker *than* — so a
 * shadowed dark card reads as an unexplained smudge. A hairline one step brighter than
 * the fill is how physical dark UI separates surfaces, and it keeps the card edges crisp
 * at any brightness.
 */
@Composable
fun SumizomeCard(
  modifier: Modifier = Modifier,
  corner: Dp = 18.dp,
  color: Color = SumizomePalette.surface,
  borderColor: Color = SumizomePalette.hairline,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(corner)
  Surface(
    color = color,
    shape = shape,
    shadowElevation = 0.dp,
    tonalElevation = 0.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, shape),
  ) {
    content()
  }
}

// ────────────────────────────────────────────────────────────── section header

/**
 * A section header: the label (English or Japanese, depending on the week) with its
 * reading above it, and a gold hairline running out to the right margin.
 *
 * Section labels are the *first* thing the immersion ramp switches, which is why they
 * get the ruby treatment even though 今日 is short — the reading is what turns a
 * mysterious glyph into a word the user learns by week four.
 *
 * The rule is inherited from the daylight sibling: every section starts on the same
 * left edge with the same trailing rule, so the page reads as one grid rather than as a
 * pile of unrelated cards.
 */
@Composable
fun SumizomeSectionHeader(
  label: String,
  ruby: String?,
  modifier: Modifier = Modifier,
  accent: Color = SumizomePalette.washi,
) {
  Row(
    verticalAlignment = Alignment.Bottom,
    modifier = modifier.fillMaxWidth(),
  ) {
    Ruby(
      text = label,
      ruby = ruby,
      fontSize = 12.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.6.sp,
      color = accent,
      rubySize = 9.sp,
      textAlign = TextAlign.Start,
    )
    Spacer(Modifier.width(12.dp))
    Canvas(
      Modifier
        .weight(1f)
        .height(1.dp)
        .padding(bottom = 4.dp),
    ) {
      drawLine(
        color = SumizomePalette.goldLeaf,
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = size.height.coerceAtLeast(1f),
        alpha = 0.42f,
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────── header

/** The immersion phrase for a phase name, so the header ramps with everything else. */
private fun phasePhrase(phase: Phase): Phrase = when (phase) {
  Phase.SHADOWING -> ImmersionStrings.PHASE_SHADOWING
  Phase.SELF_TALK -> ImmersionStrings.PHASE_SELF_TALK
  Phase.SPEAKING -> ImmersionStrings.PHASE_SPEAKING
}

/**
 * The screen's header card: phase, week and date, and the progress ring, with warm
 * embers drifting up behind it all.
 *
 * The card sits on [SumizomePalette.surfaceHigh] — a half-step brighter than the other
 * cards — because it is the one panel that should feel closest to the lamp. Embers are
 * clipped to its rounded shape and drawn *under* the text at low alpha, so the card
 * still reads as paper with something moving past a window rather than as an
 * illustration competing with the content.
 *
 * The phase name renders through [Immersion], so at week 6 the user sees シャドーイング
 * where week 2 showed "Shadowing" — with the phase's English summary still beneath it,
 * because prose is the last thing the ramp touches.
 */
@Composable
fun SumizomeHeaderCard(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  SumizomeCard(modifier = modifier, corner = 20.dp, color = SumizomePalette.surfaceHigh) {
    Box {
      EmberDriftField(
        modifier = Modifier
          .matchParentSize()
          .clip(RoundedCornerShape(20.dp)),
        emberCount = 12,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            text = "墨染",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            color = SumizomePalette.goldLeaf.copy(alpha = 0.85f),
          )
          Spacer(Modifier.height(8.dp))
          Ruby(
            immersion = immersion,
            phrase = phasePhrase(checklist.phase),
            plan = ImmersionPlan.SECTION_LABELS,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SumizomePalette.washi,
            rubySize = 10.sp,
            textAlign = TextAlign.Start,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = checklist.phase.summary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = SumizomePalette.washi.copy(alpha = 0.72f),
          )
          Spacer(Modifier.height(12.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            SumizomePill(
              text = "${immersion.sectionLabel(ImmersionStrings.WEEK)} ${checklist.weekNumber}",
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = checklist.day.date.format(SUMIZOME_DAY_FORMAT),
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Normal,
              fontSize = 12.sp,
              color = SumizomePalette.washiSoft,
            )
          }
        }
        Spacer(Modifier.width(14.dp))
        SumizomeProgressRing(done = checklist.blockingDone, total = checklist.blockingTotal)
      }
    }
  }
}

/** A small gold-tinted capsule used for the week number. */
@Composable
private fun SumizomePill(text: String) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .background(SumizomePalette.goldLeaf.copy(alpha = 0.14f))
      .border(1.dp, SumizomePalette.goldLeaf.copy(alpha = 0.30f), CircleShape)
      .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      letterSpacing = 0.6.sp,
      color = SumizomePalette.goldLeaf,
    )
  }
}

// ────────────────────────────────────────────────────────────────── gate banner

/**
 * The locked / unlocked banner, lit from behind by a soft lantern glow.
 *
 * Intentionally *warm*: locked is a low sakura wash with an encouraging count, not a red
 * alarm. Nothing has gone wrong when the day is not finished yet, so the copy and the
 * colour both say "here's what's left" and the banner reads as a bookmark rather than an
 * error. Unlocked graduates to gold — the celebration colour — closing the loop with the
 * ring's tip and the kumiko lattice.
 *
 * The [drawLanternGlow] behind it is animated by the same 0f..1f value that swaps the
 * tint, so opening the gate is a slow warm brightening rather than a hard cut.
 */
@Composable
fun SumizomeGateBanner(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  val unlocked = checklist.isUnlocked
  val lit by animateFloatAsState(
    targetValue = if (unlocked) 1f else 0f,
    animationSpec = tween(durationMillis = 480),
    label = "lantern",
  )
  val lamp = lerp(SumizomePalette.sakuraNight, SumizomePalette.goldLeaf, lit)
  val remaining = (checklist.blockingTotal - checklist.blockingDone).coerceAtLeast(0)
  val statePhrase = if (unlocked) ImmersionStrings.UNLOCKED else ImmersionStrings.LOCKED
  val shape = RoundedCornerShape(18.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(SumizomePalette.surface)
      .drawBehind {
        // Lamplight on paper: one very soft, very low-alpha radial, offset toward the
        // status dot so the light appears to come *from* the marker.
        drawLanternGlow(color = lamp, intensity = 0.55f + 0.45f * lit)
      }
      .border(1.dp, lamp.copy(alpha = 0.28f), shape),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
      // A filled dot rather than an icon: quieter, and it keeps the palette closed.
      // When unlocked it grows a small gold glint, the one flourish on the screen.
      Box(
        Modifier
          .size(22.dp)
          .drawBehind {
            drawCircle(color = lamp, radius = 5.dp.toPx(), alpha = 0.95f)
            if (lit > 0.02f) {
              drawGoldGlint(
                center = center,
                radius = 10.dp.toPx(),
                color = SumizomePalette.goldLeaf,
                alpha = 0.55f * lit,
              )
            }
          },
      )
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Ruby(
          text = immersion.gate(statePhrase),
          ruby = immersion.ruby(statePhrase, ImmersionPlan.GATE_STATE),
          fontSize = 15.sp,
          lineHeight = 19.sp,
          fontWeight = FontWeight.Bold,
          color = lamp,
          rubySize = 9.sp,
          textAlign = TextAlign.Start,
        )
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
          color = SumizomePalette.washi.copy(alpha = 0.88f),
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
 * Material's [androidx.compose.material3.Checkbox] is deliberately not used — its corner
 * radius, ripple and tick timing all belong to a different design language. Here the box
 * goes from a hairline outline to a filled matcha tile, and the tick is drawn
 * progressively along its own [Path] with [PathMeasure] so the stroke reads as a brush
 * being laid down rather than an icon appearing.
 *
 * On dark, the empty outline is brighter than its daylight counterpart (a 0.34-alpha
 * gray that reads fine on cream disappears entirely on charcoal), and the tick is drawn
 * in [SumizomePalette.night] so it punches *out* of the matcha fill.
 */
@Composable
fun SumizomeCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 26.dp,
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
        role = Role.Checkbox,
      ) { onCheckedChange(!checked) },
  ) {
    val s = this.size.minDimension
    val radius = CornerRadius(s * 0.30f, s * 0.30f)
    val fillAlpha = progress

    // Empty state: a hairline tile, warm enough to be visible on charcoal but quiet.
    drawRoundRect(
      color = SumizomePalette.washiSoft.copy(alpha = 0.55f * (1f - fillAlpha)),
      cornerRadius = radius,
      style = Stroke(width = 1.6f.dp.toPx()),
    )
    // Checked state: matcha fill rising underneath.
    if (fillAlpha > 0.001f) {
      drawRoundRect(color = SumizomePalette.matchaNight, cornerRadius = radius, alpha = fillAlpha)
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
        color = SumizomePalette.night,
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
 * One task row, as a soft card, with its title and detail routed through [Immersion].
 *
 * The whole body toggles the task, but the open affordance is a separate target so
 * tapping *go do this* never silently marks it done at the same time.
 *
 * On completion the card rides a [SumizomeCompletionSpring]: a medium-bouncy scale
 * overshoot plus a matcha wash that lifts the surface toward green, softens the title
 * and pushes the row back visually, so finished work recedes into the dark and the
 * remaining work is what the eye lands on. On a dark ground the wash has to *add* light
 * rather than subtract it, which is why the surface lerps upward instead of down.
 *
 * Both the title and the detail carry furigana when the level supplies it, and neither
 * reserves space for it when it does not — see [Ruby].
 */
@Composable
fun SumizomeTaskCard(
  task: RoadmapTask,
  done: Boolean,
  immersion: Immersion,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
  neverBlocks: Boolean = false,
) {
  val bounce = rememberSumizomeCompletionSpring(done)
  val wash = bounce.wash
  val surface = lerp(SumizomePalette.surface, SumizomePalette.matchaNight, wash * 0.13f)
  val border = lerp(SumizomePalette.hairline, SumizomePalette.matchaNight.copy(alpha = 0.6f), wash)
  val titleColor = lerp(SumizomePalette.washi, SumizomePalette.matchaNight, wash)

  val detailPhrase = ImmersionStrings.taskDetail(task.id, task.detail)

  SumizomeCard(
    modifier = modifier.sumizomeBounce(bounce),
    corner = 18.dp,
    color = surface,
    borderColor = border,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
    ) {
      SumizomeCheckbox(checked = done, onCheckedChange = onToggle)
      Spacer(Modifier.width(14.dp))
      Column(
        Modifier
          .weight(1f)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) { onToggle(!done) },
      ) {
        Ruby(
          text = immersion.taskTitle(task.id, task.title),
          ruby = immersion.taskTitleRuby(task.id, task.title),
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold,
          color = titleColor.copy(alpha = 1f - 0.24f * wash),
          textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(3.dp))
        Ruby(
          text = immersion.taskDetail(task.id, task.detail),
          ruby = immersion.ruby(detailPhrase, ImmersionPlan.TASK_DETAILS),
          fontSize = 12.5.sp,
          lineHeight = 17.sp,
          fontWeight = FontWeight.Light,
          color = SumizomePalette.washi.copy(alpha = (1f - 0.30f * wash) * 0.66f),
          rubySize = 9.sp,
          textAlign = TextAlign.Start,
        )
        if (neverBlocks) {
          Spacer(Modifier.height(7.dp))
          SumizomeNeverBlocksChip(immersion)
        }
      }
      if (onOpen != null) {
        Spacer(Modifier.width(8.dp))
        SumizomeOpenButton(immersion = immersion, onClick = onOpen)
      }
    }
  }
}

/**
 * The "this one never gates your apps" marker on optional tasks.
 *
 * Said in words on the card itself rather than relying on the section it happens to sit
 * under, because the promise the app makes about what it will and will not lock is the
 * one thing it cannot afford to be ambiguous about.
 */
@Composable
private fun SumizomeNeverBlocksChip(immersion: Immersion, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(SumizomePalette.washiSoft.copy(alpha = 0.12f))
      .padding(horizontal = 8.dp, vertical = 3.dp),
  ) {
    Ruby(
      text = immersion.sectionLabel(ImmersionStrings.NEVER_BLOCKS),
      ruby = immersion.sectionRuby(ImmersionStrings.NEVER_BLOCKS),
      fontSize = 10.5.sp,
      lineHeight = 13.sp,
      fontWeight = FontWeight.Medium,
      color = SumizomePalette.washi.copy(alpha = 0.80f),
      letterSpacing = 0.4.sp,
      rubySize = 8.sp,
    )
  }
}

/**
 * The "open this tool" affordance: an outlined sakura capsule.
 *
 * Outlined rather than filled because it is a *side door* — the primary action on every
 * row is the checkbox, and a solid button here would out-shout it. Its label ramps with
 * the rest of the interface, 開く with its reading once the week earns it.
 */
@Composable
fun SumizomeOpenButton(
  immersion: Immersion,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(CircleShape)
      .border(1.dp, SumizomePalette.sakuraNight.copy(alpha = 0.50f), CircleShape)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    Ruby(
      text = immersion.action(ImmersionStrings.OPEN_ACTION),
      ruby = immersion.ruby(ImmersionStrings.OPEN_ACTION, ImmersionPlan.ACTIONS),
      fontSize = 12.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.5.sp,
      color = SumizomePalette.sakuraNight,
      rubySize = 8.sp,
    )
  }
}

// ──────────────────────────────────────────────────────── focus + checkpoints

/** The one-line weekly framing, set as a quiet pull-quote with a sakura rule. */
@Composable
fun SumizomeFocusCard(focus: String, modifier: Modifier = Modifier) {
  SumizomeCard(modifier = modifier, corner = 18.dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      // A single sakura rule standing in for a vertical brush stroke.
      Box(
        Modifier
          .width(3.dp)
          .height(38.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(SumizomePalette.sakuraNight.copy(alpha = 0.85f)),
      )
      Spacer(Modifier.width(14.dp))
      Text(
        text = focus,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = SumizomePalette.washi,
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
fun SumizomeCheckpointList(lines: List<String>, modifier: Modifier = Modifier) {
  SumizomeCard(modifier = modifier, corner = 18.dp) {
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
              .background(SumizomePalette.goldLeaf.copy(alpha = 0.85f)),
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = line,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = SumizomePalette.washi.copy(alpha = 0.90f),
          )
        }
      }
    }
  }
}

/**
 * A gentle nudge toward the next phase, shown only when the roadmap has one.
 *
 * Framed as an invitation ("when you're ready"), because advancing is a manual, felt
 * decision in this app and the UI must not imply it is overdue. Gold-tinted, because it
 * is the good kind of news.
 */
@Composable
fun SumizomePhasePromptCard(prompt: String, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(18.dp)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(SumizomePalette.goldLeaf.copy(alpha = 0.07f))
      .border(1.dp, SumizomePalette.goldLeaf.copy(alpha = 0.24f), shape)
      .padding(18.dp),
  ) {
    Text(
      text = "WHEN YOU'RE READY",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      letterSpacing = 1.8.sp,
      color = SumizomePalette.goldLeaf,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = prompt,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 19.sp,
      color = SumizomePalette.washi.copy(alpha = 0.90f),
    )
  }
}

/**
 * A closing mark: a gold hairline either side of a short motto.
 *
 * Pure decoration, but it gives the scroll a deliberate end instead of trailing off —
 * the paper equivalent of a stamp in the bottom corner.
 */
@Composable
fun SumizomeFooterMark(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(
      Modifier
        .width(28.dp)
        .height(1.dp)
        .background(SumizomePalette.goldLeaf.copy(alpha = 0.42f)),
    )
    Text(
      text = "  一日一歩  ",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 11.sp,
      letterSpacing = 2.sp,
      color = SumizomePalette.washiSoft,
    )
    Spacer(
      Modifier
        .width(28.dp)
        .height(1.dp)
        .background(SumizomePalette.goldLeaf.copy(alpha = 0.42f)),
    )
  }
}
