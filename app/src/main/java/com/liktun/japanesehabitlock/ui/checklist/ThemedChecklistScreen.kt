package com.liktun.japanesehabitlock.ui.checklist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liktun.japanesehabitlock.data.ChecklistRepository
import com.liktun.japanesehabitlock.data.habitLockDataStore
import com.liktun.japanesehabitlock.ui.styles.AppStyle
import com.liktun.japanesehabitlock.ui.styles.ThemeRegistry

/**
 * The real checklist, drawn in whichever theme the user chose.
 *
 * This is a thin shell on purpose: it owns the data and hands it to [ThemeRegistry],
 * which owns the drawing. The plain Material screen that used to live here has been
 * retired — every theme, including the default, is now a designed one, so there is no
 * "unthemed" path left to drift out of sync.
 */
@Composable
fun ThemedChecklistScreen(
  style: AppStyle,
  modifier: Modifier = Modifier,
) {
  // applicationContext, not the Activity: the DataStore outlives this screen.
  val appContext = LocalContext.current.applicationContext
  val viewModel: ChecklistViewModel = viewModel {
    ChecklistViewModel(ChecklistRepository(appContext.habitLockDataStore))
  }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  when (val current = state) {
    ChecklistUiState.Loading -> Unit // First frame only; DataStore resolves immediately after.
    is ChecklistUiState.Error ->
      Text(
        text = "Could not load your checklist: ${current.throwable.message}",
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
      )
    is ChecklistUiState.Ready ->
      ThemeRegistry.Render(
        style = style,
        checklist = current.checklist,
        onToggleTask = viewModel::setTaskCompleted,
        onOpenTask = { task -> TaskOpener.open(appContext, task.launch) },
        modifier = modifier.fillMaxSize(),
      )
  }
}
