package com.liktun.japanesehabitlock.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liktun.japanesehabitlock.service.SurfaceDiagnostics

/**
 * Lets the user capture what the blocker actually sees on screen, and share it.
 *
 * Surface rules depend on view ids internal to Instagram and YouTube. Those cannot be
 * checked from a build machine — there is no logged-in Instagram there — so when a rule
 * fails the only source of truth is the user's own phone. Without this screen the loop
 * is "ship a guess, ask if it worked, repeat", which costs a release per attempt.
 *
 * It shows ids only, never text, because that is all the service ever reads.
 */
@Composable
fun DiagnosticsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var enabled by remember { mutableStateOf(SurfaceDiagnostics.enabled) }
  var report by remember { mutableStateOf(SurfaceDiagnostics.report()) }

  Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    TextButton(onClick = onNavigateBack) { Text("← Back") }
    Text(
      text = "Diagnose blocking",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text =
        "If a surface like Instagram Reels is not being blocked, turn this on, open " +
          "that screen in the app, then come back and share the report. It records the " +
          "identifiers of on-screen elements — never any text.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.weight(1f)) {
        Text("Record screens", fontWeight = FontWeight.Medium)
        Text(
          text = "Off by default. Nothing is stored on disk or sent anywhere.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(
        checked = enabled,
        onCheckedChange = {
          enabled = it
          SurfaceDiagnostics.enabled = it
          if (!it) SurfaceDiagnostics.clear()
          report = SurfaceDiagnostics.report()
        },
      )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
      Button(onClick = { report = SurfaceDiagnostics.report() }) { Text("Refresh") }
      Spacer(Modifier.width(8.dp))
      OutlinedButton(
        onClick = {
          val send =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Japanese Habit Lock diagnostics")
              putExtra(Intent.EXTRA_TEXT, SurfaceDiagnostics.report())
            }
          context.startActivity(Intent.createChooser(send, "Share report"))
        }
      ) {
        Text("Share")
      }
      Spacer(Modifier.width(8.dp))
      OutlinedButton(
        onClick = {
          SurfaceDiagnostics.clear()
          report = SurfaceDiagnostics.report()
        }
      ) {
        Text("Clear")
      }
    }

    Spacer(Modifier.height(14.dp))

    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = report,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        modifier = Modifier.padding(12.dp),
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}
