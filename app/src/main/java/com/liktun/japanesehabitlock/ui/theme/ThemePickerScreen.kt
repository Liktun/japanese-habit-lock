package com.liktun.japanesehabitlock.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.ui.styles.AppStyle

/**
 * Lets the user pick which of the shipped themes the checklist is drawn in.
 *
 * Each row carries a swatch strip rather than only a name, because these themes differ
 * far more than their labels suggest — "Sumi-e" and "Neon Yokocho" mean nothing until
 * you see that one is paper and the other is a dark alley. The swatches are the actual
 * palette values, so what is previewed is what gets rendered.
 */
@Composable
fun ThemePickerScreen(
  selected: AppStyle,
  onSelect: (AppStyle) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Column {
        TextButton(onClick = onNavigateBack) { Text("← Back") }
        Text(
          text = "Theme",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "The checklist is drawn differently in each. Pick whichever you'll actually want to look at every morning.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    items(AppStyle.selectable.size) { index ->
      val style = AppStyle.selectable[index]
      ThemeRow(style = style, chosen = style == selected, onClick = { onSelect(style) })
    }

    item { Spacer(Modifier.height(24.dp)) }
  }
}

@Composable
private fun ThemeRow(style: AppStyle, chosen: Boolean, onClick: () -> Unit) {
  val border =
    if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .border(if (chosen) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp),
    ) {
      Swatches(style)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = style.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = style.japanese,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(2.dp))
        Text(
          text = style.blurb,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (chosen) {
        Text(
          text = "✓",
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
        )
      }
    }
  }
}

/** A vertical strip of the theme's real palette, so the choice is made by eye. */
@Composable
private fun Swatches(style: AppStyle) {
  Column(
    verticalArrangement = Arrangement.spacedBy(3.dp),
    modifier =
      Modifier.size(width = 44.dp, height = 56.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(swatchesFor(style).first()),
  ) {
    Spacer(Modifier.height(6.dp))
    swatchesFor(style).drop(1).forEach { colour ->
      Box(
        Modifier.padding(horizontal = 7.dp)
          .fillMaxWidth()
          .height(8.dp)
          .clip(CircleShape)
          .background(colour)
      )
    }
  }
}

/** Ground colour first, then the two or three accents that define the theme. */
private fun swatchesFor(style: AppStyle): List<Color> =
  when (style) {
    AppStyle.WA_MODERN -> listOf(Color(0xFFFBF7F0), Color(0xFFF2A8B8), Color(0xFF7A9A6B), Color(0xFFC9A227))
    AppStyle.KINARI -> listOf(Color(0xFFF7F4EE), Color(0xFFC0483A), Color(0xFF6E8A5F), Color(0xFFB99A4B))
    AppStyle.KISETSU -> listOf(Color(0xFFFDF6F2), Color(0xFFD9647E), Color(0xFF3E8C7F), Color(0xFFC4622D))
    AppStyle.SUMIE -> listOf(Color(0xFFF4F1EA), Color(0xFF1C1A17), Color(0xFFC8452F), Color(0xFFB8925A))
    AppStyle.NEON -> listOf(Color(0xFF0A0812), Color(0xFFFF2D95), Color(0xFF00E5FF), Color(0xFFFFB020))
    AppStyle.SUMIZOME -> listOf(Color(0xFF161A21), Color(0xFFE08A9E), Color(0xFF8FB37A), Color(0xFFD4AF6A))
  }
