package com.liktun.japanesehabitlock.ui.styles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.domain.RoadmapTask
import com.liktun.japanesehabitlock.ui.styles.neon.NeonScreen
import com.liktun.japanesehabitlock.ui.styles.sumie.SumieScreen
import com.liktun.japanesehabitlock.ui.styles.wamodern.WaModernScreen

/**
 * A throwaway gallery for comparing the three candidate front-ends on a real device.
 *
 * Deliberately separate from [com.liktun.japanesehabitlock.MainActivity] and backed by
 * in-memory state rather than DataStore: this is a decision aid, not a feature, and it
 * must not be able to corrupt a real day's completions. It is launched explicitly by
 * adb during screenshot capture and has no launcher icon.
 *
 * Delete this package once a style is chosen.
 */
class StyleGalleryActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // `style` and `done` intents let the capture script drive one screen per launch.
    val requested = intent.getStringExtra("style")?.uppercase()

    // Edge to edge with per-style system bars. Without this the status bar renders as an
    // opaque grey slab over every theme, which reads as a theming bug and was the single
    // most damaging flaw in all three designs.
    val dark = requested == AppStyle.NEON.name
    enableEdgeToEdge(
      statusBarStyle =
        if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
      navigationBarStyle =
        if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
    )
    // "none" is a sentinel: `am start --es` refuses an empty string value.
    val preset = intent.getStringExtra("done").orEmpty().takeUnless { it == "none" }.orEmpty()
    setContent { StyleGallery(initial = AppStyle.entries.firstOrNull { it.name == requested }, preset = preset) }
  }
}

@Composable
private fun StyleGallery(initial: AppStyle?, preset: String) {
  var style by remember { mutableStateOf(initial ?: AppStyle.SUMIE) }
  val completed = remember {
    mutableStateListOf<String>().apply {
      addAll(preset.split(',').map { it.trim() }.filter { it.isNotEmpty() })
    }
  }

  val checklist = sampleChecklist(completed = completed.toSet())
  val toggle: (String, Boolean) -> Unit = { id, on ->
    if (on) completed.add(id) else completed.remove(id)
  }
  val open: (RoadmapTask) -> Unit = {} // No-op in the gallery.

  // The style's own background colour must extend under the system bars, so it is painted
  // on the outer Box while the content itself is inset by safeDrawingPadding.
  val backdrop =
    when (style) {
      AppStyle.SUMIE -> Color(0xFFF4F1EA)
      AppStyle.NEON -> Color(0xFF0A0812)
      AppStyle.WA_MODERN -> Color(0xFFFBF7F0)
    }

  Box(Modifier.fillMaxSize().background(backdrop)) {
    val inset = Modifier.fillMaxSize().safeDrawingPadding()
    when (style) {
      AppStyle.SUMIE -> SumieScreen(checklist, toggle, open, inset)
      AppStyle.NEON -> NeonScreen(checklist, toggle, open, inset)
      AppStyle.WA_MODERN -> WaModernScreen(checklist, toggle, open, inset)
    }

    // Only shown when no specific style was requested, so screenshots stay clean.
    if (initial == null) {
      Row(
        Modifier.fillMaxWidth().background(Color(0xCC000000)).padding(4.dp)
      ) {
        AppStyle.entries.forEach { candidate ->
          TextButton(onClick = { style = candidate }) {
            Text(
              text = candidate.japanese,
              color = if (candidate == style) Color.White else Color(0x99FFFFFF),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}
