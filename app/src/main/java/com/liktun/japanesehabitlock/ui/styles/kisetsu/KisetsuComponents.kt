@file:Suppress("FunctionName")

package com.liktun.japanesehabitlock.ui.styles.kisetsu

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
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings
import java.time.format.DateTimeFormatter

internal val KISETSU_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ──────────────────────────────────────────────────────────────────── furigana

/**
 * Text with an optional reading set above it — the app's furigana primitive.
 *
 * This is the single most load-bearing composable in KISETSU. The immersion ramp turns
 * labels and task titles into kanji over the course of months, and kanji without a
 * reading is not immersion for a learner, it is a wall. So every string that *can* be
 * Japanese goes through here.
 *
 * Two rules it must never break:
 *
 * 1. **No reserved gap.** When [ruby] is null nothing is emitted above the text at all —
 *    not a zero-height `Spacer`, not a blank `Text`, not a fixed-height `Box`. If the
 *    layout reserved space for an absent reading, then crossing an immersion threshold
 *    would leave every English row sitting in a slightly-too-tall box forever, and the
 *    day the readings appear the whole page would jump. Rows must be exactly as tall as
 *    their content.
 * 2. **The reading is a whisper.** ~9.5sp, [SeasonTheme.inkSoft], and a line height
 *    barely taller than the glyphs, so it sits *close* to the word it belongs to. A
 *    reading that competes with the word is a second headline, and the eye then reads
 *    kana instead of learning kanji.
 *
 * The reading is centred over the text because that is where ruby goes, and the block is
 * deliberately **wrap-content**: it is as wide as its widest line, no wider. That is what
 * puts the reading over the *word* rather than over the row, and it also keeps a section
 * header's hairline and the open button's capsule border tight against the label instead
 * of being shoved out by a greedy full-width block. Long text still wraps normally, since
 * the incoming max-width constraint is unchanged.
 */
@Composable
fun Ruby(
  text: String,
  ruby: String?,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 15.sp,
  lineHeight: TextUnit = 20.sp,
  fontWeight: FontWeight = FontWeight.SemiBold,
  color: Color = theme.ink,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  rubyFontSize: TextUnit = 9.5.sp,
  rubyColor: Color = theme.inkSoft,
  textAlign: TextAlign = TextAlign.Start,
) {
  Column(
    modifier = modifier,
    // Centres the reading over the word beneath it.
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (ruby != null) {
      Text(
        text = ruby,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = rubyFontSize,
        // Deliberately tight: the reading must hug the word beneath it.
        lineHeight = rubyFontSize * 1.06f,
        letterSpacing = 0.2.sp,
        color = rubyColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 1.dp),
      )
    }
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = fontWeight,
      fontSize = fontSize,
      lineHeight = lineHeight,
      letterSpacing = letterSpacing,
      color = color,
      textAlign = textAlign,
    )
  }
}

// ────────────────────────────────────────────────────────────────── card shell

/**
 * The shared card shell: season paper on season ground, soft 16–20dp corners, hairline
 * border instead of a heavy drop shadow.
 *
 * Inherited wholesale from Wa-Modern, because the *structure* is the part the user
 * approved — only the colours are allowed to move with the calendar. Elevation stays
 * layered but shallow: a 1dp tonal edge plus a 1dp shadow reads as a sheet of paper
 * resting on another sheet. A big Material elevation would make it float like a dialog.
 */
@Composable
fun KisetsuCard(
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
  corner: Dp = 18.dp,
  color: Color = theme.surface,
  borderAlpha: Float = 0.07f,
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
      .border(1.dp, theme.ornament.copy(alpha = borderAlpha * 5f), shape),
  ) {
    content()
  }
}

// ─────────────────────────────────────────────────────────────── section header

/**
 * A section header: the label (with its reading when immersion supplies one) and an
 * ornament hairline running out to the right margin.
 *
 * The hairline is the only place [SeasonTheme.ornament] is allowed to appear at size, and
 * it gives every section the same left-aligned rail so the page reads as one grid in all
 * four seasons.
 */
@Composable
fun KisetsuSectionHeader(
  phrase: com.liktun.japanesehabitlock.domain.immersion.Phrase,
  immersion: Immersion,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
  accent: Color = theme.accentDeep,
) {
  val label = immersion.sectionLabel(phrase)
  val reading = immersion.sectionRuby(phrase)

  Row(
    verticalAlignment = Alignment.Bottom,
    modifier = modifier.fillMaxWidth(),
  ) {
    Ruby(
      text = label,
      ruby = reading,
      theme = theme,
      fontSize = 12.5.sp,
      lineHeight = 16.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.8.sp,
      color = accent,
      textAlign = TextAlign.Start,
    )
    Spacer(Modifier.width(12.dp))
    Canvas(
      Modifier
        .weight(1f)
        .height(1.dp)
        .padding(bottom = 5.dp),
    ) {
      drawLine(
        color = theme.ornament,
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = size.height.coerceAtLeast(1f),
        alpha = 0.55f,
      )
    }
  }
}

/** A plain ornament hairline used between major blocks. Decoration only. */
@Composable
fun SeasonHairline(theme: SeasonTheme, modifier: Modifier = Modifier) {
  Canvas(modifier.fillMaxWidth().height(1.dp)) {
    drawLine(
      color = theme.ornament,
      start = Offset(0f, size.height / 2f),
      end = Offset(size.width, size.height / 2f),
      strokeWidth = size.height.coerceAtLeast(1f),
      alpha = 0.35f,
    )
  }
}

// ────────────────────────────────────────────────────────────────────── header

/**
 * The header card: the season's kanji, the phase, the week and date, and the progress
 * ring — with the season's particles drifting behind all of it.
 *
 * The kanji sits in a soft accent-washed tile at the top left, at a size that lets it act
 * as the screen's mark without shouting: it is the first thing you see on 1 March and the
 * thing that tells you, three months in, that the app has quietly redecorated itself. The
 * particles are clipped to the card's own rounded shape and drawn *under* the text at low
 * alpha, so the card still reads as paper with something falling past a window rather than
 * as an illustration.
 */
@Composable
fun KisetsuHeaderCard(
  checklist: DailyChecklist,
  immersion: Immersion,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
) {
  KisetsuCard(theme = theme, modifier = modifier, corner = 20.dp) {
    Box {
      SeasonParticleField(
        theme = theme,
        modifier = Modifier
          .matchParentSize()
          .clip(RoundedCornerShape(20.dp)),
        particleCount = 18,
      )
      Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          SeasonKanjiTile(theme)
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text = theme.name.uppercase(),
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.5.sp,
              letterSpacing = 3.sp,
              color = theme.accentDeep,
            )
            Spacer(Modifier.height(5.dp))
            Text(
              text = checklist.phase.label,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              lineHeight = 24.sp,
              color = theme.ink,
            )
          }
          Spacer(Modifier.width(10.dp))
          SeasonProgressRing(
            done = checklist.blockingDone,
            total = checklist.blockingTotal,
            theme = theme,
            diameter = 70.dp,
          )
        }
        Spacer(Modifier.height(12.dp))
        Text(
          text = checklist.phase.summary,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 13.sp,
          lineHeight = 19.sp,
          color = theme.inkSoft,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          SeasonPill(
            text = "${immersion.sectionLabel(ImmersionStrings.WEEK)} ${checklist.weekNumber}",
            theme = theme,
          )
          Spacer(Modifier.width(9.dp))
          Text(
            text = checklist.day.date.format(KISETSU_DAY_FORMAT),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = theme.inkSoft,
          )
        }
      }
    }
  }
}

/**
 * The season's kanji in a washed tile.
 *
 * A tile rather than bare text so the glyph gets a shape of its own and does not read as
 * a stray character in the phase heading. The ornament hairline around it is the same
 * one used on every section rule, which ties the mark to the rest of the page.
 */
@Composable
fun SeasonKanjiTile(theme: SeasonTheme, modifier: Modifier = Modifier, size: Dp = 54.dp) {
  val shape = RoundedCornerShape(16.dp)
  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(theme.accent.copy(alpha = 0.20f))
      .border(1.dp, theme.ornament.copy(alpha = 0.5f), shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = theme.kanji,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      color = theme.accentDeep,
    )
  }
}

/** A small accent-washed capsule. Used for the week number. */
@Composable
fun SeasonPill(text: String, theme: SeasonTheme, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(theme.accent.copy(alpha = 0.22f))
      .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      letterSpacing = 0.6.sp,
      color = theme.accentDeep,
    )
  }
}

// ───────────────────────────────────────────────────────────────── gate banner

/**
 * The locked / unlocked banner.
 *
 * Intentionally *warm* in every season: locked is a soft accent wash with an encouraging
 * count, never a red alarm. Nothing has gone wrong when the day is not finished yet, so
 * the copy and the colour both say "here's what's left" and the banner reads as a
 * bookmark rather than an error. Unlocked graduates to the progress hue, closing the
 * colour loop with the ring above it.
 *
 * The state word runs through the immersion system with its reading, because 施錠中 /
 * 解錠 are backed up by colour, position and a filled dot — which makes them one of the
 * safest places to introduce Japanese early.
 */
@Composable
fun KisetsuGateBanner(
  checklist: DailyChecklist,
  immersion: Immersion,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
) {
  val unlocked = checklist.isUnlocked
  val phrase = if (unlocked) ImmersionStrings.UNLOCKED else ImmersionStrings.LOCKED
  val tint = if (unlocked) theme.progress else theme.accent
  val markInk = if (unlocked) theme.progress else theme.accentDeep
  val remaining = (checklist.blockingTotal - checklist.blockingDone).coerceAtLeast(0)

  Surface(
    color = tint.copy(alpha = 0.15f),
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
          .background(markInk.copy(alpha = 0.9f)),
      )
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Ruby(
          text = immersion.gate(phrase),
          ruby = immersion.ruby(phrase, ImmersionPlan.GATE_STATE),
          theme = theme,
          fontSize = 15.sp,
          lineHeight = 19.sp,
          fontWeight = FontWeight.Bold,
          color = markInk,
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
          color = theme.ink.copy(alpha = 0.80f),
        )
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────── custom checkbox

/**
 * The custom checkbox: a soft rounded square that fills with the season's progress colour
 * and strokes a checkmark on.
 *
 * Material's [androidx.compose.material3.Checkbox] is deliberately not used — its corner
 * radius, ripple and tick timing all belong to a different design language, and it would
 * be the one element on screen that ignores the season. Here the box swells from an empty
 * hairline outline to a filled tile, and the tick is drawn progressively along its own
 * [Path] with [PathMeasure], so the stroke reads as a brush being laid down rather than
 * as an icon appearing.
 */
@Composable
fun KisetsuCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  theme: SeasonTheme,
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

    // Empty state: a hairline tile, so an unchecked row is quiet.
    drawRoundRect(
      color = theme.inkSoft.copy(alpha = 0.40f * (1f - fillAlpha)),
      cornerRadius = radius,
      style = Stroke(width = 1.6f.dp.toPx()),
    )
    if (fillAlpha > 0.001f) {
      drawRoundRect(color = theme.progress, cornerRadius = radius, alpha = fillAlpha)
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
        color = theme.surface,
        style = Stroke(width = s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────── task card

/**
 * One task row, as a soft card, with its title and detail routed through immersion.
 *
 * The whole body toggles the task, but the "open" affordance is a separate target so
 * tapping *go do this* never silently marks it done at the same time.
 *
 * On completion the card rides a [CompletionSpring]: a medium-bouncy scale overshoot plus
 * a wash of the season's progress colour that tints the surface, softens the title and
 * pushes the row back visually — so finished work recedes and what remains is what your
 * eye lands on.
 *
 * @param neverBlocks marks the row as tracked-but-not-gating, stated in words rather
 *   than implied by position.
 */
@Composable
fun KisetsuTaskCard(
  task: RoadmapTask,
  done: Boolean,
  immersion: Immersion,
  theme: SeasonTheme,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
  neverBlocks: Boolean = false,
) {
  val bounce = rememberCompletionSpring(done)
  val wash = bounce.wash
  val surface = lerp(theme.surface, theme.progress.copy(alpha = 0.5f), wash * 0.20f)
  val titleColor = lerp(theme.ink, theme.progress, wash * 0.85f)

  KisetsuCard(
    theme = theme,
    modifier = modifier.completionBounce(bounce),
    corner = 18.dp,
    color = surface,
    borderAlpha = 0.06f + 0.10f * wash,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
    ) {
      KisetsuCheckbox(checked = done, onCheckedChange = onToggle, theme = theme)
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
          theme = theme,
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold,
          color = titleColor.copy(alpha = 1f - 0.20f * wash),
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = immersion.taskDetail(task.id, task.detail),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 12.5.sp,
          lineHeight = 17.sp,
          color = theme.inkSoft.copy(alpha = 1f - 0.28f * wash),
        )
        if (neverBlocks) {
          Spacer(Modifier.height(6.dp))
          NeverBlocksTag(immersion = immersion, theme = theme)
        }
      }
      if (onOpen != null) {
        Spacer(Modifier.width(8.dp))
        KisetsuOpenButton(onClick = onOpen, immersion = immersion, theme = theme)
      }
    }
  }
}

/**
 * The "this never gates your apps" tag.
 *
 * Set in ornament so it cannot be mistaken for either an accent (attention) or the
 * progress colour (done) — it is a statement about the *rules*, not about state.
 */
@Composable
fun NeverBlocksTag(immersion: Immersion, theme: SeasonTheme, modifier: Modifier = Modifier) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier
        .size(4.dp)
        .clip(CircleShape)
        .background(theme.ornament),
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = immersion.sectionLabel(ImmersionStrings.NEVER_BLOCKS),
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 10.5.sp,
      letterSpacing = 0.8.sp,
      color = theme.inkSoft.copy(alpha = 0.9f),
    )
  }
}

/**
 * The "open this tool" affordance: an outlined accent capsule carrying 開く with its
 * reading once immersion reaches task titles.
 *
 * Outlined rather than filled because it is a *side door* — the primary action on every
 * row is the checkbox, and a solid button here would out-shout it.
 */
@Composable
fun KisetsuOpenButton(
  onClick: () -> Unit,
  immersion: Immersion,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(CircleShape)
      .border(1.dp, theme.accentDeep.copy(alpha = 0.5f), CircleShape)
      .clickable(onClick = onClick)
      .padding(horizontal = 13.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Ruby(
      text = immersion.action(ImmersionStrings.OPEN_ACTION),
      ruby = immersion.ruby(ImmersionStrings.OPEN_ACTION, ImmersionPlan.ACTIONS),
      theme = theme,
      fontSize = 12.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.5.sp,
      color = theme.accentDeep,
      textAlign = TextAlign.Center,
    )
  }
}

// ─────────────────────────────────────────────────────────── focus + checkpoints

/**
 * The one-line weekly framing, set as a quiet pull-quote on paper.
 *
 * The vertical accent rule stands in for a brush stroke and is the one place a solid slab
 * of accent appears outside the kanji tile, which keeps the eye moving down the page.
 */
@Composable
fun KisetsuFocusCard(focus: String, theme: SeasonTheme, modifier: Modifier = Modifier) {
  KisetsuCard(theme = theme, modifier = modifier, corner = 18.dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Box(
        Modifier
          .width(3.dp)
          .height(40.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(theme.accent),
      )
      Spacer(Modifier.width(14.dp))
      Text(
        text = focus,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = theme.ink,
      )
    }
  }
}

/**
 * The weekly checkpoints.
 *
 * Explicitly *not* checkboxes: these never gate anything, so giving them the same
 * affordance as a blocking task would be a lie about what the app requires. They get
 * ornament dots instead — decorative, inert, and unmistakably a different class of thing.
 */
@Composable
fun KisetsuCheckpointList(
  lines: List<String>,
  theme: SeasonTheme,
  modifier: Modifier = Modifier,
) {
  KisetsuCard(theme = theme, modifier = modifier, corner = 18.dp) {
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
              .background(theme.ornament),
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = line,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = theme.ink.copy(alpha = 0.86f),
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
 * decision in this app and the UI must not imply it is overdue.
 */
@Composable
fun KisetsuPhasePromptCard(prompt: String, theme: SeasonTheme, modifier: Modifier = Modifier) {
  Surface(
    color = theme.ornament.copy(alpha = 0.12f),
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
          color = theme.ink.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "次へ",
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 10.sp,
          color = theme.inkSoft,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = prompt,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = theme.ink.copy(alpha = 0.85f),
      )
    }
  }
}

/**
 * The closing mark: the season's own word between two ornament rules.
 *
 * Pure decoration, but it gives the scroll a deliberate end instead of trailing off — and
 * it is the second, quieter place the season names itself.
 */
@Composable
fun SeasonFooterMark(theme: SeasonTheme, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(Modifier.width(28.dp).height(1.dp).background(theme.ornament.copy(alpha = 0.5f)))
    Text(
      text = "  ${theme.kanji}の一日  ",
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 11.sp,
      letterSpacing = 2.sp,
      color = theme.inkSoft.copy(alpha = 0.85f),
    )
    Spacer(Modifier.width(28.dp).height(1.dp).background(theme.ornament.copy(alpha = 0.5f)))
  }
}
