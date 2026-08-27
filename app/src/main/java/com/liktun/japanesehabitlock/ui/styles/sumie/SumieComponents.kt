package com.liktun.japanesehabitlock.ui.styles.sumie

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import java.time.format.DateTimeFormatter

internal val SUMIE_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ---------------------------------------------------------------------------------
//  Type
// ---------------------------------------------------------------------------------

/**
 * A section label set as small caps with the tracking opened right up.
 *
 * Wide letter spacing is doing the work a card border would do elsewhere: at 3.6sp the
 * word stops behaving like a word and becomes a horizontal rule made of letters, which
 * is enough to separate two blocks of content without drawing a single box.
 */
@Composable
fun SumieSectionLabel(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = SumiePalette.inkWash,
  japanese: String? = null,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
    Text(
      text = text.uppercase(),
      fontFamily = FontFamily.Serif,
      fontSize = 10.sp,
      lineHeight = 14.sp,
      letterSpacing = 3.6.sp,
      fontWeight = FontWeight.Medium,
      color = color,
    )
    if (japanese != null) {
      Spacer(Modifier.width(10.dp))
      Text(
        text = japanese,
        fontFamily = FontFamily.Serif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
        color = color.copy(alpha = 0.55f),
      )
    }
  }
}

// ---------------------------------------------------------------------------------
//  Header
// ---------------------------------------------------------------------------------

/**
 * The top of the sheet: phase, week, date, and the seal's landing spot.
 *
 * Everything is left-aligned against a single margin and separated only by air. The
 * phase label is the largest thing on the page and the only thing at full ink; the
 * summary sits a full dilution below it, and the week/date line is set in tracked
 * small caps so it reads as a caption rather than as prose.
 */
@Composable
fun SumieHeader(checklist: DailyChecklist, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    Column(Modifier.weight(1f)) {
      SumieSectionLabel(text = "Daily practice", japanese = "日課")
      Spacer(Modifier.height(14.dp))
      Text(
        text = checklist.phase.label,
        fontFamily = FontFamily.Serif,
        fontSize = 27.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Normal,
        color = SumiePalette.sumiInk,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = checklist.phase.summary,
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
        color = SumiePalette.inkSoft,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text =
          "WEEK ${checklist.weekNumber}   ·   ${checklist.day.date.format(SUMIE_DAY_FORMAT).uppercase()}",
        fontFamily = FontFamily.Serif,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 3.sp,
        color = SumiePalette.inkWash,
      )
    }
    // The seal hangs in the top margin the way a chop sits on a finished scroll.
    // It occupies the space whether or not it has stamped, so nothing reflows when
    // the last task is checked — the red simply appears.
    Box(Modifier.padding(start = 12.dp, top = 6.dp)) {
      HankoSeal(stamped = checklist.isUnlocked, size = 58.dp)
    }
  }
}

// ---------------------------------------------------------------------------------
//  Gate
// ---------------------------------------------------------------------------------

/**
 * Locked / unlocked, the count, and the brush stroke that carries the progress.
 *
 * There is no container and no colour change between the two states. The difference is
 * carried entirely by the words, by how far the stroke has travelled, and by whether
 * the seal above it has landed — which is a far quieter and far more Japanese way to
 * say "you are done" than filling a rectangle green.
 */
@Composable
fun SumieGate(checklist: DailyChecklist, modifier: Modifier = Modifier) {
  val unlocked = checklist.isUnlocked
  Column(modifier = modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = if (unlocked) "OPEN" else "CLOSED",
        fontFamily = FontFamily.Serif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 5.sp,
        fontWeight = FontWeight.Medium,
        color = if (unlocked) SumiePalette.sumiInk else SumiePalette.inkSoft,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "${checklist.blockingDone} / ${checklist.blockingTotal}",
        fontFamily = FontFamily.Serif,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.6.sp,
        color = SumiePalette.sumiInk,
      )
    }
    Spacer(Modifier.height(10.dp))
    BrushStrokeProgress(progress = checklist.progress, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Text(
      text =
        if (unlocked) {
          "Everything required is done. The gate is open."
        } else {
          "${checklist.blockingDone} of ${checklist.blockingTotal} done — distractions stay shut."
        },
      fontFamily = FontFamily.Serif,
      fontSize = 13.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.3.sp,
      color = SumiePalette.inkSoft,
    )
  }
}

// ---------------------------------------------------------------------------------
//  Task row
// ---------------------------------------------------------------------------------

/**
 * One line of the checklist: hand-drawn box, title, detail, and an optional 「開」.
 *
 * The row has no background, no border, and no rounded anything. Its only structure is
 * the hairline that follows it and the indentation that lines every detail line up with
 * every title. Tapping the text toggles; the open affordance is a separate target so
 * "go and do this" can never silently mean "mark this done".
 *
 * @param onOpen non-null only when the task has a [RoadmapTask.launch].
 */
@Composable
fun SumieTaskRow(
  task: RoadmapTask,
  done: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
  dimmed: Boolean = false,
) {
  val titleInk = if (dimmed) SumiePalette.inkSoft else SumiePalette.sumiInk
  val toggleSource = remember { MutableInteractionSource() }
  val openSource = remember { MutableInteractionSource() }

  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      Modifier
        .padding(top = 2.dp)
        .clickable(
          interactionSource = toggleSource,
          indication = null,
          onClick = { onToggle(!done) },
        )
    ) {
      InkCheckbox(checked = done, size = 22.dp, seed = task.id.hashCode())
    }
    Spacer(Modifier.width(16.dp))
    Column(
      Modifier.weight(1f).clickable(
        interactionSource = toggleSource,
        indication = null,
        onClick = { onToggle(!done) },
      )
    ) {
      Text(
        text = task.title,
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
        color = if (done) SumiePalette.inkSoft else titleInk,
        modifier = Modifier.inkStrikeThrough(done = done, seed = task.id.hashCode()),
      )
      Spacer(Modifier.height(3.dp))
      Text(
        text = task.detail,
        fontFamily = FontFamily.Serif,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = SumiePalette.inkWash,
      )
    }
    if (onOpen != null) {
      Spacer(Modifier.width(12.dp))
      Text(
        text = "開",
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = SumiePalette.inkSoft,
        textAlign = TextAlign.Center,
        modifier =
          Modifier
            .padding(top = 1.dp)
            .size(28.dp)
            .clickable(interactionSource = openSource, indication = null, onClick = onOpen)
            .padding(top = 4.dp),
      )
    }
  }
}

// ---------------------------------------------------------------------------------
//  Margin motif
// ---------------------------------------------------------------------------------

/**
 * 「習慣」— *shūkan*, habit — set down the margin, one glyph at a time.
 *
 * Compose has no vertical writing mode, so this is a `Column` of single characters
 * with the tracking turned into leading. Held at 12% ink it functions the way a chop
 * mark on the edge of a scroll does: it establishes that the page is Japanese and that
 * the margin is deliberate, without ever asking to be read.
 */
@Composable
fun SumieMarginColumn(
  text: String = "習慣",
  modifier: Modifier = Modifier,
  alpha: Float = 0.13f,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    text.forEach { glyph ->
      Text(
        text = glyph.toString(),
        fontFamily = FontFamily.Serif,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        color = SumiePalette.sumiInk.copy(alpha = alpha),
      )
    }
  }
}

// ---------------------------------------------------------------------------------
//  Small pieces
// ---------------------------------------------------------------------------------

/** A checkpoint line, marked with a thin ink dash rather than a bullet. */
@Composable
fun SumieCheckpointLine(text: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
    Text(
      text = "—",
      fontFamily = FontFamily.Serif,
      fontSize = 13.sp,
      lineHeight = 20.sp,
      color = SumiePalette.inkWash,
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = text,
      fontFamily = FontFamily.Serif,
      fontSize = 13.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.2.sp,
      color = SumiePalette.inkSoft,
    )
  }
}

/**
 * The week's framing line, set slightly larger and in the one gold accent on the page.
 *
 * Gold is spent here and nowhere else: a single hairline under the focus text. It marks
 * this as the sentence worth re-reading each morning, and because it appears exactly
 * once it stays precious.
 */
@Composable
fun SumieFocusLine(focus: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = focus,
      fontFamily = FontFamily.Serif,
      fontSize = 15.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.3.sp,
      color = SumiePalette.sumiInk,
    )
    Spacer(Modifier.height(10.dp))
    InkHairline(
      modifier = Modifier.fillMaxWidth(0.34f),
      color = SumiePalette.gold,
      alpha = 0.7f,
      seed = 23,
    )
  }
}

/** A whisper of a note, used for the phase suggestion. Deliberately the quietest text. */
@Composable
fun SumieQuietNote(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    fontFamily = FontFamily.Serif,
    fontSize = 12.5.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.2.sp,
    color = SumiePalette.inkWash,
    modifier = modifier.fillMaxWidth(),
  )
}
