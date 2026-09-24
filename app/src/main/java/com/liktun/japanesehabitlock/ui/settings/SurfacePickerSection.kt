package com.liktun.japanesehabitlock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.domain.surface.BlockableSurface
import com.liktun.japanesehabitlock.domain.surface.KnownSurfaces
import com.liktun.japanesehabitlock.theme.JapaneseHabitLockTheme

/**
 * How one surface row should render.
 *
 * Pulled out of the composable because the interesting part is not the layout, it is the
 * precedence question: a whole-app block and a surface block can both be switched on for
 * the same app, and only one of them can be the rule that actually runs. Deciding that in
 * a pure function keeps it unit-testable and keeps the UI from quietly disagreeing with
 * the service.
 */
internal enum class SurfaceRowState {
  /** Not selected, and free to be selected. */
  AVAILABLE,

  /** Selected and enforced. */
  CHECKED,

  /**
   * The parent app is blocked outright, so this surface rule is dead weight.
   *
   * Deliberately outranks [CHECKED]: if the user blocked Instagram entirely *and* ticked
   * Reels, the app block is what they experience, and the row must say so rather than
   * show a tick that changes nothing.
   */
  SUPERSEDED_BY_APP_BLOCK,
}

/**
 * Resolves the rule that actually wins for [surface].
 *
 * App-level blocking is strictly coarser than surface blocking, so it swallows it. The
 * order of the checks below is the whole contract.
 */
internal fun surfaceRowState(
  surface: BlockableSurface,
  blockedSurfaceIds: Set<String>,
  blockedPackages: Set<String>,
): SurfaceRowState =
  when {
    surface.packageName in blockedPackages -> SurfaceRowState.SUPERSEDED_BY_APP_BLOCK
    surface.id in blockedSurfaceIds -> SurfaceRowState.CHECKED
    else -> SurfaceRowState.AVAILABLE
  }

/**
 * A readable app name for a group header.
 *
 * The surface list is a hand-written constant, so a small lookup is honest here — there is
 * no `PackageManager` query to make and no guarantee the app is even installed.
 */
internal fun surfaceAppLabel(packageName: String): String =
  when (packageName) {
    KnownSurfaces.INSTAGRAM -> "Instagram"
    KnownSurfaces.YOUTUBE -> "YouTube"
    else -> packageName
  }

/**
 * Lets the user block one surface inside an app instead of the whole app.
 *
 * This section sits above the whole-app list because it is the gentler choice, and most
 * people reaching for "block Instagram" actually mean "block Reels". Grouped by app so the
 * relationship between the two sections is obvious at a glance.
 *
 * Stateless by design: the caller owns [blockedSurfaceIds] and [blockedPackages] so this
 * can be previewed and tested without a ViewModel.
 */
@Composable
fun SurfacePickerSection(
  surfaces: List<BlockableSurface>,
  blockedSurfaceIds: Set<String>,
  blockedPackages: Set<String>,
  onToggleSurface: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // groupBy preserves first-seen order, so the grouping never reshuffles between frames.
    surfaces.groupBy { it.packageName }.forEach { (packageName, appSurfaces) ->
      Text(
        text = surfaceAppLabel(packageName),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
      )
      appSurfaces.forEach { surface ->
        key(surface.id) {
          SurfaceRow(
            surface = surface,
            state = surfaceRowState(surface, blockedSurfaceIds, blockedPackages),
            onToggle = { checked -> onToggleSurface(surface.id, checked) },
          )
        }
        Spacer(Modifier.height(8.dp))
      }
    }

    // Stated plainly rather than buried: surface detection reads the app's own view ids,
    // which are undocumented and can change without warning. Promising otherwise would be
    // a promise this app cannot keep.
    Text(
      text =
        "Surface blocking works by recognising each app's screen layout. When these apps " +
          "ship a redesign it can stop working until the app is updated. As a backstop, " +
          "ticking any surface in an app also blocks that app after about 15 seconds of " +
          "continuous scrolling anywhere in it - so moving to an unnamed screen doesn't " +
          "slip through.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp),
    )
  }
}

/**
 * One surface, plus — when it applies — the reason its checkbox is switched off.
 *
 * The superseded case is rendered as an explanation rather than an inert checkbox: a tick
 * box that silently does nothing is the exact confusion this feature has to avoid.
 */
@Composable
private fun SurfaceRow(
  surface: BlockableSurface,
  state: SurfaceRowState,
  onToggle: (Boolean) -> Unit,
) {
  val superseded = state == SurfaceRowState.SUPERSEDED_BY_APP_BLOCK
  val checked = state == SurfaceRowState.CHECKED
  val contentColor =
    if (superseded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    else MaterialTheme.colorScheme.onSurface

  Surface(
    color =
      MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha =
          when {
            superseded -> 0.25f
            checked -> 1f
            else -> 0.5f
          }
      ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier =
        Modifier.let { if (superseded) it else it.clickable { onToggle(!checked) } }
          .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
      Checkbox(
        checked = checked,
        onCheckedChange = if (superseded) null else onToggle,
        enabled = !superseded,
      )
      Spacer(Modifier.width(4.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = surface.label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = contentColor,
        )
        Text(
          text = surface.detail,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (superseded) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (superseded) {
          Spacer(Modifier.height(4.dp))
          Text(
            text =
              "${surfaceAppLabel(surface.packageName)} is blocked entirely — surface rules " +
                "don't apply.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------- previews

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SurfacePickerPreview() {
  JapaneseHabitLockTheme {
    SurfacePickerSection(
      surfaces = KnownSurfaces.ALL,
      blockedSurfaceIds = setOf("instagram_reels"),
      blockedPackages = emptySet(),
      onToggleSurface = { _, _ -> },
      modifier = Modifier.padding(16.dp),
    )
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SurfacePickerSupersededPreview() {
  JapaneseHabitLockTheme {
    SurfacePickerSection(
      surfaces = KnownSurfaces.ALL,
      blockedSurfaceIds = setOf("instagram_reels"),
      blockedPackages = setOf(KnownSurfaces.INSTAGRAM),
      onToggleSurface = { _, _ -> },
      modifier = Modifier.padding(16.dp),
    )
  }
}
