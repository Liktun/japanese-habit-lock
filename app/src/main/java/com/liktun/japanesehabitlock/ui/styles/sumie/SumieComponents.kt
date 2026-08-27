package com.liktun.japanesehabitlock.ui.styles.sumie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.domain.immersion.Immersion
import com.liktun.japanesehabitlock.domain.immersion.ImmersionPlan
import com.liktun.japanesehabitlock.domain.immersion.ImmersionStrings
import java.time.format.DateTimeFormatter

internal val SUMIE_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ---------------------------------------------------------------------------------
//  Type
// ---------------------------------------------------------------------------------

/**
 * The label face: serif, small, and tracked out until the word becomes a rule.
 *
 * Held as a [TextStyle] rather than as arguments on every call so that Japanese and
 * English labels are guaranteed to be set identically. That matters more here than in
 * a sans-serif style: when 今週 replaces "THIS WEEK" the *only* thing that should change
 * is the glyphs, and generous tracking on kanji is what makes them read as typeset
 * rather than as a system font drop-in.
 */
internal val SUMIE_LABEL_STYLE: TextStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 3.6.sp,
  )

/** The task title face. Serif, the largest thing in a row, tracked only slightly. */
internal val SUMIE_TITLE_STYLE: TextStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.2.sp,
  )

/**
 * A section label set as small caps with the tracking opened right up.
 *
 * Wide letter spacing is doing the work a card border would do elsewhere: at 3.6sp the
 * word stops behaving like a word and becomes a horizontal rule made of letters, which
 * is enough to separate two blocks of content without drawing a single box.
 *
 * [garnish] is the small kanji this style used to print permanently beside its English
 * headings (今週 next to "THIS WEEK"). It is now conditional: once the immersion ramp
 * makes the heading *itself* Japanese, the garnish is the same word twice, so callers
 * pass it only while [Immersion.headingsAreEnglish] holds.
 *
 * `uppercase()` is applied to [label] because it is a no-op on kanji — one call site
 * therefore serves both languages without a branch.
 */
@Composable
fun SumieSectionLabel(
  label: String,
  modifier: Modifier = Modifier,
  ruby: String? = null,
  color: Color = SumiePalette.inkWash,
  garnish: String? = null,
  trailing: String? = null,
  trailingRuby: String? = null,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
    Ruby(
      text = label.uppercase(),
      reading = ruby,
      style = SUMIE_LABEL_STYLE,
      color = color,
      rubyColor = SUMIE_RUBY_INK,
    )
    if (garnish != null) {
      Spacer(Modifier.width(10.dp))
      Text(
        text = garnish,
        fontFamily = FontFamily.Serif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
        color = color.copy(alpha = 0.55f),
      )
    }
    if (trailing != null) {
      Spacer(Modifier.width(12.dp))
      Ruby(
        text = trailing.uppercase(),
        reading = trailingRuby,
        style = SUMIE_LABEL_STYLE.copy(fontWeight = FontWeight.Normal, letterSpacing = 2.4.sp),
        color = color.copy(alpha = 0.7f),
        rubyColor = SUMIE_RUBY_INK.copy(alpha = 0.7f),
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
 *
 * The masthead's own "DAILY PRACTICE 日課" mark is left permanently bilingual: it is the
 * sheet's letterhead rather than a section heading, and the immersion tables carry no
 * phrase for it. Only the week word ramps, via [ImmersionStrings.WEEK].
 */
@Composable
fun SumieHeader(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    Column(Modifier.weight(1f)) {
      SumieSectionLabel(label = "Daily practice", garnish = "日課")
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
          "${immersion.sectionLabel(ImmersionStrings.WEEK).uppercase()} ${checklist.weekNumber}" +
            "   ·   ${checklist.day.date.format(SUMIE_DAY_FORMAT).uppercase()}",
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
 *
 * The state word ramps through [Immersion.gate], so "OPEN" / "CLOSED" become 解錠 /
 * 施錠中 with かいじょう / せじょうちゅう set above them. Because the word is the *only*
 * carrier of state in this style, it is the one place where losing the reading would
 * actually cost the user information — which is why it takes ruby at every level that
 * offers one.
 */
@Composable
fun SumieGate(
  checklist: DailyChecklist,
  immersion: Immersion,
  modifier: Modifier = Modifier,
) {
  val unlocked = checklist.isUnlocked
  val phrase = if (unlocked) ImmersionStrings.UNLOCKED else ImmersionStrings.LOCKED

  Column(modifier = modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
      // Boxed rather than weighted directly: Ruby centres its reading over its own
      // measured width, so stretching it across the row would float the kana in the
      // middle of the sheet instead of over the word.
      Box(Modifier.weight(1f)) {
        Ruby(
          text = immersion.gate(phrase).uppercase(),
          reading = immersion.ruby(phrase, ImmersionPlan.GATE_STATE),
          style =
            SUMIE_LABEL_STYLE.copy(
              fontSize = 12.sp,
              lineHeight = 16.sp,
              letterSpacing = 5.sp,
            ),
          color = if (unlocked) SumiePalette.sumiInk else SumiePalette.inkSoft,
          rubyColor = SUMIE_RUBY_INK,
        )
      }
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
          // Counted prose switches with the rest of the prose, not with the gate word:
          // the numbers are legible in either language, so it is safe to hold it back.
          immersion.text(
            ImmersionStrings.gateSummary(checklist.blockingDone, checklist.blockingTotal),
            ImmersionPlan.PROSE,
          )
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
 * One line of the checklist: hand-drawn box, title, detail, and an optional open mark.
 *
 * The row has no background, no border, and no rounded anything. Its only structure is
 * the hairline that follows it and the indentation that lines every detail line up with
 * every title. Tapping the text toggles; the open affordance is a separate target so
 * "go and do this" can never silently mean "mark this done".
 *
 * Title, detail and the open word all resolve through [immersion], and the title carries
 * its reading via [Ruby]. The title's ruby is aligned Start inside the text column so it
 * stays centred on the *phrase* while the phrase itself stays on the row's margin.
 *
 * @param onOpen non-null only when the task has a [RoadmapTask.launch].
 */
@Composable
fun SumieTaskRow(
  task: RoadmapTask,
  done: Boolean,
  immersion: Immersion,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  onOpen: (() -> Unit)? = null,
  dimmed: Boolean = false,
) {
  val titleInk = if (dimmed) SumiePalette.inkSoft else SumiePalette.sumiInk
  val toggleSource = remember { MutableInteractionSource() }

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
      Ruby(
        text = immersion.taskTitle(task.id, task.title),
        reading = immersion.taskTitleRuby(task.id, task.title),
        style = SUMIE_TITLE_STYLE,
        color = if (done) SumiePalette.inkSoft else titleInk,
        rubyColor = SUMIE_RUBY_INK,
        modifier = Modifier.align(Alignment.Start),
        // The strike runs through the word, not through its reading.
        baseModifier = Modifier.inkStrikeThrough(done = done, seed = task.id.hashCode()),
      )
      Spacer(Modifier.height(3.dp))
      Text(
        text = immersion.taskDetail(task.id, task.detail),
        fontFamily = FontFamily.Serif,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = SumiePalette.inkWash,
      )
    }
    if (onOpen != null) {
      Spacer(Modifier.width(12.dp))
      SumieOpenMark(
        label = immersion.action(ImmersionStrings.OPEN_ACTION),
        reading = immersion.ruby(ImmersionStrings.OPEN_ACTION, ImmersionPlan.ACTIONS),
        onClick = onOpen,
      )
    }
  }
}

/**
 * The "go and do this" affordance: one tracked word over a short ink rule.
 *
 * This used to be a bare 「開」 glyph, which only worked because it was decoration. Now
 * that the label is a real phrase on the immersion ramp — "OPEN" early, 開く with ひらく
 * later — it has to survive changing width, so it wraps its content instead of sitting
 * in a fixed 28dp square. The underline is an [InkHairline] rather than a border, which
 * keeps the affordance inside the style's drawn-not-boxed vocabulary.
 */
@Composable
private fun SumieOpenMark(
  label: String,
  reading: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val openSource = remember { MutableInteractionSource() }
  Column(
    modifier = modifier
      .padding(top = 1.dp)
      .clickable(interactionSource = openSource, indication = null, onClick = onClick)
      .padding(horizontal = 2.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Ruby(
      text = label.uppercase(),
      reading = reading,
      style = SUMIE_LABEL_STYLE.copy(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 2.sp),
      color = SumiePalette.inkSoft,
      rubyColor = SUMIE_RUBY_INK,
    )
    Spacer(Modifier.height(4.dp))
    InkHairline(
      modifier = Modifier.width(26.dp),
      color = SumiePalette.inkSoft,
      alpha = 0.45f,
      seed = 41,
    )
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
 *
 * It is also the reason furigana on this sheet is inked at [SUMIE_RUBY_INK] rather than
 * at full wash: two competing faint Japanese textures on one page would read as noise,
 * so the margin keeps the texture and the ruby stays strictly functional.
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
