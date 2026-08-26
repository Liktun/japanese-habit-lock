package com.liktun.japanesehabitlock.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liktun.japanesehabitlock.service.HeartbeatStatus
import com.liktun.japanesehabitlock.service.OemGuidance
import com.liktun.japanesehabitlock.theme.JapaneseHabitLockTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * The warning shown when the OS reports our service as enabled but it has gone quiet.
 *
 * This is the failure mode that matters most: Samsung, Xiaomi, Huawei and OPPO reap
 * background processes without clearing the accessibility toggle, so the user checks
 * Settings, sees a switch that still reads "on", and believes they are protected while
 * nothing whatsoever is being blocked. Surfacing that silently would make the whole app
 * a placebo, so this card is loud, names the user's actual manufacturer, and gives the
 * exact menu path rather than generic "check your battery settings" advice.
 *
 * Rendered only for [HeartbeatStatus.Silenced] and [HeartbeatStatus.NeverStarted] — the
 * other two states are handled by the ordinary status card.
 */
@Composable
internal fun ServiceHealthWarning(
  status: HeartbeatStatus,
  guidance: OemGuidance,
  onOpenBatterySettings: () -> Unit,
) {
  if (status !is HeartbeatStatus.Silenced && status !is HeartbeatStatus.NeverStarted) return

  val silenced = status is HeartbeatStatus.Silenced
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = if (silenced) "Blocking may have stopped" else "Blocking hasn't started yet",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text =
          if (silenced) {
            "The switch is still on, but the service hasn't checked in for hours — " +
              "${guidance.manufacturerLabel} has probably killed it in the background. " +
              "Your apps are NOT being blocked right now."
          } else {
            "The service is switched on but has never run. Open the app once after " +
              "enabling it, and give it the exemptions below so it stays running."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )

      Spacer(Modifier.height(12.dp))
      Text(
        text = "ON ${guidance.manufacturerLabel.uppercase()}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      Spacer(Modifier.height(6.dp))
      guidance.steps.forEachIndexed { index, step ->
        Row(Modifier.padding(bottom = 4.dp)) {
          Text(
            text = "${index + 1}.  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Text(
            text = step,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }

      Spacer(Modifier.height(12.dp))
      Button(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
        Text("Open battery settings")
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ServiceHealthWarningSilencedPreview() {
  JapaneseHabitLockTheme {
    ServiceHealthWarning(
      status = HeartbeatStatus.Silenced,
      guidance =
        OemGuidance(
          manufacturerLabel = "Samsung",
          steps =
            listOf(
              "Settings → Battery → Background usage limits → Never sleeping apps → add Japanese Habit Lock.",
              "Settings → Apps → Japanese Habit Lock → Battery → Unrestricted.",
            ),
          settingsIntentAction = null,
          settingsComponent = "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
        ),
      onOpenBatterySettings = {},
    )
  }
}
