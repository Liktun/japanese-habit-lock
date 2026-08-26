package com.example.japanesehabitlock.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesehabitlock.data.ChecklistRepository
import com.example.japanesehabitlock.domain.DailyChecklist
import com.example.japanesehabitlock.domain.Phase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistViewModel(private val repository: ChecklistRepository) : ViewModel() {

  val uiState: StateFlow<ChecklistUiState> =
    repository.checklist
      .map<DailyChecklist, ChecklistUiState> { ChecklistUiState.Ready(it) }
      .catch { emit(ChecklistUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistUiState.Loading)

  fun setTaskCompleted(taskId: String, completed: Boolean) {
    viewModelScope.launch { repository.setTaskCompleted(taskId, completed) }
  }

  fun advanceTo(phase: Phase) {
    viewModelScope.launch { repository.setPhase(phase) }
  }
}

sealed interface ChecklistUiState {
  data object Loading : ChecklistUiState

  data class Error(val throwable: Throwable) : ChecklistUiState

  data class Ready(val checklist: DailyChecklist) : ChecklistUiState
}
