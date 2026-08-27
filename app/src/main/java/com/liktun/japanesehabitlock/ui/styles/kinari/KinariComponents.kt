package com.liktun.japanesehabitlock.ui.styles.kinari

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.Phase
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings
import java.time.format.DateTimeFormatter

/**
 * KINARI's building blocks: header, gate row, task row, section headers, checkpoints.
 *
 * Every text run on this screen resolves through an [Immersion] instance handed down
 * from the screen, so nothing here hardcodes an English string that the user might
 * have graduated past. Where a phrase can carry a kana reading, it is rendered with
 * [Ruby] rather than [Text], which is what lets the whole interface switch language
 * without becoming unreadable.
 */

/** ISO-ish and unambiguous, in the same spirit as a printed diary header. */
internal val KINARI_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/** The one left margin the entire screen aligns to. Nothing is allowed to break it. */
internal val KINARI_MARGIN: Dp = 24.dp

// ────────────────────────────────────────────────────────────────── the card shell

/**
 * The card shell — and it is barely a card.
 *
 * This is the sharpest divergence from Wa-Modern, which uses 18–20dp corners and a
 * shadow to make its cards feel like soft paper stock. Here there is **no shadow at
 * all** and the radius is 8dp, so the only thing separating paper from page is a 1px
 * hairline and the half-step of tone between [KinariPalette.surface] and
 * [KinariPalette.kinari]. The edge should read as a crease or a trim line, not as a
 * floating sheet. A drop shadow anywhere in this style would break it.
 */
@Composable
fun KinariCard(
  modifier: Modifier = Modifier,
  corner: Dp = 8.dp,
  color: Color = KinariPalette.surface,
  borderColor: Color = KinariPalette.hairline,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(corner)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(color)
      .border(1.dp, borderColor, shape),
  ) {
    content()
  }
}

// ────────────────────────────────────────────────────────────────── section header

/**
 * A section header: an eyebrow label with its reading above it, then a hairline out to
 * the right margin.
 *
 * Wa-Modern sets the Japanese *beside* the English at a smaller size, as garnish. Here
 * the Japanese replaces the English outright once the week earns it, and the reading
 * goes above in ruby — the label is the same object in either language rather than a
 * bilingual pair. The trailing rule gives every section the same rail, which is what
 * holds the page to one grid.
 *
 * Note the [Ruby] alignment: section labels are left-aligned to the strict margin, so
 * the ruby is centred over its own text block rather than over the column.
 */
@Composable
fun KinariSectionHeader(
  label: String,
  ruby: String?,
  modifier: Modifier = Modifier,
  color: Color = KinariPalette.sumi,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Bottom,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      if (ruby != null) {
        Text(
          text = ruby,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 9.sp,
          lineHeight = 9.5.sp,
          letterSpacing = 0.5.sp,
          color = KinariPalette.sumiSoft,
        )
        Spacer(Modifier.height(2.dp))
      }
      // Uppercasing is a no-op on kanji, so the same call works in both languages.
      KinariEyebrow(text = label.uppercase(), color = color, tracking = 3.5.sp)
    }
    Spacer(Modifier.width(12.dp))
    KinariRule(
      modifier = Modifier
        .weight(1f)
        .padding(bottom = 4.dp),
    )
  }
}

// ─────────────────────────────────────────────────────────────────────── header

/**
 * The masthead: phase, summary, week and date on the left; the numeric count on the
 * right; the ruled progress bar spanning the full column beneath both.
 *
 * There is no card here on purpose. The header sits directly on the page so the very
 * first thing the eye meets is paper and type, and the first drawn line on the screen
 * is the progress rule itself. The numerals are the only large element, and they are
 * set Light — the size does the work, the weight stays out of the way.
 */
@Composable
fun KinariHeader(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    KinariEyebrow(
      text = phaseEyebrow(checklist.phase, immersion),
      color = KinariPalette.sumiSoft,
      tracking = 4.sp,
    )
    KinariGap(10.dp)
    Text(
      text = checklist.phase.label,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 22.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.2).sp,
      color = KinariPalette.sumi,
    )
    KinariGap(6.dp)
    Text(
      text = checklist.phase.summary,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 20.sp,
      color = KinariPalette.sumiSoft,
    )
    KinariGap(14.dp)
    KinariMetaRow(
      items = listOf(
        "${immersion.sectionLabel(ImmersionStrings.WEEK)} ${checklist.weekNumber}",
        checklist.day.date.format(KINARI_DAY_FORMAT),
      ),
    )

    KinariGap(22.dp)

    Row(verticalAlignment = Alignment.Bottom) {
      KinariNumericProgress(done = checklist.blockingDone, total = checklist.blockingTotal)
      Spacer(Modifier.weight(1f))
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 6.dp),
      ) {
        KinariEyebrow(
          text = immersion.sectionLabel(ImmersionStrings.REQUIRED).uppercase(),
          color = KinariPalette.sumiSoft,
          tracking = 2.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = immersion.sectionLabel(ImmersionStrings.DONE),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 11.5.sp,
          letterSpacing = 0.4.sp,
          color = KinariPalette.sumiSoft.copy(alpha = 0.8f),
        )
      }
    }

    KinariGap(12.dp)
    KinariProgressRule(done = checklist.blockingDone, total = checklist.blockingTotal)
  }
}

/** The phase name, immersion-resolved, used as the masthead eyebrow. */
private fun phaseEyebrow(phase: Phase, immersion: Immersion): String =
  when (phase) {
    Phase.SHADOWING -> immersion.sectionLabel(ImmersionStrings.PHASE_SHADOWING)
    Phase.SELF_TALK -> immersion.sectionLabel(ImmersionStrings.PHASE_SELF_TALK)
    Phase.SPEAKING -> immersion.sectionLabel(ImmersionStrings.PHASE_SPEAKING)
  }.uppercase()

// ───────────────────────────────────────────────────────────────────── gate row

/**
 * The gate state, as a ruled row rather than a banner.
 *
 * Wa-Modern softens this into a tinted sakura panel because it did not want "locked" to
 * feel like an error. KINARI takes the opposite route to the same end: it makes the row
 * completely neutral — hairlines top and bottom, no fill — and lets one small seal-red
 * square carry the entire locked state. A stamp on a form is not an alarm, and this is
 * the first of the two places [KinariPalette.seal] is allowed to appear.
 *
 * The state word itself comes from [Immersion.gate], so it becomes 施錠中 / 解錠 with
 * its reading once the ramp reaches that level.
 */
@Composable
fun KinariGateRow(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  val unlocked = checklist.isUnlocked
  val phrase = if (unlocked) ImmersionStrings.UNLOCKED else ImmersionStrings.LOCKED
  val ink = if (unlocked) KinariPalette.matcha else KinariPalette.seal
  val remaining = (checklist.blockingTotal - checklist.blockingDone).coerceAtLeast(0)

  Column(modifier = modifier.fillMaxWidth()) {
    KinariRule()
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
    ) {
      KinariGateMark(unlocked = unlocked)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Ruby(
          text = immersion.gate(phrase),
          ruby = immersion.ruby(phrase, ImmersionPlan.GATE_STATE),
          fontSize = 14.sp,
          lineHeight = 18.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp,
          color = ink,
          modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(5.dp))
        Text(
          text = if (unlocked) {
            "Everything required is done."
          } else if (remaining == 1) {
            "One task still holds the gate."
          } else {
            "$remaining tasks still hold the gate."
          },
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 12.5.sp,
          lineHeight = 18.sp,
          color = KinariPalette.sumiSoft,
        )
      }
    }
    KinariRule()
  }
}

// ────────────────────────────────────────────────────────────────────── task row

/**
 * One task, as a barely-there card.
 *
 * The row is built on the same strict margin as everything else: checkbox, then a
 * fixed gutter, then the text column — so the titles of every task on the screen start
 * on one vertical line regardless of whether they carry a reading. Completion is
 * expressed by *recession* rather than celebration: the title steps down from Medium to
 * Normal, the ink fades toward [KinariPalette.sumiSoft], and the border warms very
 * slightly toward matcha. There is no bounce and no wash, because a finished task
 * should get quieter, not louder.
 *
 * Tapping the text column toggles the task; the "open" affordance is a separate target
 * so going to do the work never silently marks it done.
 */
@Composable
fun KinariTaskRow(
  task: RoadmapTask,
  done: Boolean,
  immersion: Immersion,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
) {
  val settle by animateFloatAsState(
    targetValue = if (done) 1f else 0f,
    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
    label = "kinari-task-settle",
  )
  val border = lerp(KinariPalette.hairline, KinariPalette.matcha.copy(alpha = 0.55f), settle * 0.6f)
  val titleColor = lerp(KinariPalette.sumi, KinariPalette.sumiSoft, settle)

  KinariCard(modifier = modifier, borderColor = border) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
    ) {
      KinariCheckbox(
        checked = done,
        onCheckedChange = onToggle,
        modifier = Modifier.padding(top = 1.dp),
      )
      Spacer(Modifier.width(14.dp))
      Column(
        modifier = Modifier
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
          fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
          color = titleColor,
          modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = immersion.taskDetail(task.id, task.detail),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 12.5.sp,
          lineHeight = 18.sp,
          color = KinariPalette.sumiSoft.copy(alpha = 1f - 0.35f * settle),
        )
      }
      if (onOpen != null) {
        Spacer(Modifier.width(10.dp))
        KinariOpenLink(
          label = immersion.action(ImmersionStrings.OPEN_ACTION),
          ruby = immersion.ruby(ImmersionStrings.OPEN_ACTION, ImmersionPlan.ACTIONS),
          onClick = onOpen,
          modifier = Modifier.padding(top = 1.dp),
        )
      }
    }
  }
}

/**
 * The "open this tool" affordance: a seal-red word over a seal-red hairline.
 *
 * The second and last permitted use of [KinariPalette.seal]. It is a rule-underlined
 * link rather than a button or capsule because a filled control would out-weigh the
 * checkbox, which is the actual primary action on the row — and because an underline is
 * the most stationery-native way to say "this word does something".
 */
@Composable
fun KinariOpenLink(
  label: String,
  ruby: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Ruby(
      text = label.uppercase(),
      ruby = ruby,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.6.sp,
      color = KinariPalette.seal,
      rubySize = 9.sp,
      rubyColor = KinariPalette.seal.copy(alpha = 0.65f),
    )
    Spacer(Modifier.height(3.dp))
    Box(
      Modifier
        .width(if (ruby != null) 34.dp else 30.dp)
        .height(1.dp)
        .background(KinariPalette.seal.copy(alpha = 0.5f)),
    )
  }
}

// ────────────────────────────────────────────────────────────────── weekly blocks

/** The one-line weekly framing, set as an indented pull-quote behind a hairline. */
@Composable
fun KinariFocusBlock(focus: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth()) {
    Box(
      Modifier
        .width(1.dp)
        .height(46.dp)
        .background(KinariPalette.sumi.copy(alpha = 0.28f)),
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = focus,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 22.sp,
      color = KinariPalette.sumi.copy(alpha = 0.9f),
    )
  }
}

/**
 * The weekly checkpoints, as a numbered list.
 *
 * Deliberately not checkboxes: these never gate anything, and giving them the same
 * affordance as a blocking task would be a lie about what the app requires. Wa-Modern
 * marks them with gold dots; here they get Light-weight index numerals, which keeps the
 * gold reserved for the single ornament and makes the list read like a printed
 * enumeration.
 */
@Composable
fun KinariCheckpointList(lines: List<String>, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    lines.forEachIndexed { index, line ->
      Row(verticalAlignment = Alignment.Top) {
        Text(
          text = (index + 1).toString().padStart(2, '0'),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 13.sp,
          lineHeight = 20.sp,
          letterSpacing = 0.5.sp,
          color = KinariPalette.sumiSoft.copy(alpha = 0.7f),
          modifier = Modifier.width(26.dp),
        )
        Text(
          text = line,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Normal,
          fontSize = 13.sp,
          lineHeight = 20.sp,
          color = KinariPalette.sumi.copy(alpha = 0.86f),
        )
      }
    }
  }
}

/**
 * The optional-tasks caption: says in words that these never gate anything.
 *
 * Position alone is not enough — a user who scrolls past the required list should be
 * told outright that nothing below it can lock them out.
 */
@Composable
fun KinariNeverBlocksNote(
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier
        .size(width = 14.dp, height = 1.dp)
        .background(KinariPalette.hairline),
    )
    Spacer(Modifier.width(10.dp))
    Ruby(
      text = immersion.sectionLabel(ImmersionStrings.NEVER_BLOCKS),
      ruby = immersion.sectionRuby(ImmersionStrings.NEVER_BLOCKS),
      fontSize = 12.sp,
      lineHeight = 16.sp,
      fontWeight = FontWeight.Normal,
      letterSpacing = 0.6.sp,
      color = KinariPalette.sumiSoft,
    )
  }
}

/**
 * A nudge toward the next phase, shown only when the roadmap offers one.
 *
 * Framed as an invitation, and set inside the quietest container in the style — a
 * hairline box with no fill at all — because advancing is a manual, felt decision here
 * and the UI must not imply it is overdue.
 */
@Composable
fun KinariPhasePrompt(prompt: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, KinariPalette.hairline, RoundedCornerShape(8.dp))
      .padding(18.dp),
  ) {
    Column {
      KinariEyebrow(text = "WHEN YOU'RE READY", tracking = 3.sp)
      Spacer(Modifier.height(10.dp))
      Text(
        text = prompt,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = KinariPalette.sumi.copy(alpha = 0.85f),
      )
    }
  }
}
