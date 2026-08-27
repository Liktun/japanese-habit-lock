package com.liktun.japanesehabitlock.ui.styles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.liktun.japanesehabitlock.domain.DailyChecklist
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.ui.styles.kinari.KinariScreen
import com.liktun.japanesehabitlock.ui.styles.kisetsu.KisetsuScreen
import com.liktun.japanesehabitlock.ui.styles.kisetsu.seasonForDate
import com.liktun.japanesehabitlock.ui.styles.neon.NeonScreen
import com.liktun.japanesehabitlock.ui.styles.sumie.SumieScreen
import com.liktun.japanesehabitlock.ui.styles.sumizome.SumizomeScreen
import com.liktun.japanesehabitlock.ui.styles.wamodern.WaModernScreen

/**
 * The single place that maps a chosen [AppStyle] to the screen that draws it.
 *
 * Every theme is a real, self-contained implementation rather than a palette swap, so
 * the registry is a `when` rather than a table of colours. Adding a theme means adding
 * one enum entry and one branch here; nothing else in the app needs to know.
 */
object ThemeRegistry {

  /** The theme used before the user has chosen, and whenever a stored name is unknown. */
  val DEFAULT: AppStyle = AppStyle.WA_MODERN

  /**
   * Resolves a persisted theme name.
   *
   * An unrecognised name falls back to [DEFAULT] rather than throwing: a theme could be
   * removed in a later version while a user's stored preference still names it, and
   * crashing on launch over a cosmetic setting would be indefensible.
   */
  fun resolve(name: String?): AppStyle =
    AppStyle.entries.firstOrNull { it.name == name } ?: DEFAULT

  /**
   * The colour that should sit behind the system bars for [style].
   *
   * Needed before the screen composes so the status bar never flashes the wrong colour,
   * which is why it lives here rather than being read out of each theme's palette.
   */
  fun backdrop(style: AppStyle, checklist: DailyChecklist? = null): Color =
    when (style) {
      AppStyle.SUMIE -> Color(0xFFF4F1EA)
      AppStyle.NEON -> Color(0xFF0A0812)
      AppStyle.WA_MODERN -> Color(0xFFFBF7F0)
      AppStyle.KINARI -> Color(0xFFF7F4EE)
      AppStyle.SUMIZOME -> Color(0xFF161A21)
      // Kisetsu repaints itself, so its backdrop depends on the study date.
      AppStyle.KISETSU ->
        checklist?.let { seasonForDate(it.day.date).ground } ?: Color(0xFFFDF6F2)
    }

  /** Whether [style] needs light system-bar icons. */
  fun isDark(style: AppStyle): Boolean = style == AppStyle.NEON || style == AppStyle.SUMIZOME

  /** Draws [checklist] in the chosen [style]. */
  @Composable
  fun Render(
    style: AppStyle,
    checklist: DailyChecklist,
    onToggleTask: (String, Boolean) -> Unit,
    onOpenTask: (RoadmapTask) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    when (style) {
      AppStyle.SUMIE -> SumieScreen(checklist, onToggleTask, onOpenTask, modifier)
      AppStyle.NEON -> NeonScreen(checklist, onToggleTask, onOpenTask, modifier)
      AppStyle.WA_MODERN -> WaModernScreen(checklist, onToggleTask, onOpenTask, modifier)
      AppStyle.KINARI -> KinariScreen(checklist, onToggleTask, onOpenTask, modifier)
      AppStyle.SUMIZOME -> SumizomeScreen(checklist, onToggleTask, onOpenTask, modifier)
      AppStyle.KISETSU -> KisetsuScreen(checklist, onToggleTask, onOpenTask, modifier)
    }
  }
}
