package com.team.taskmanagementapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.model.ConflictAction
import com.team.taskmanagementapp.data.model.ImportResult
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.ui.base.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel(
    private val backupRepository: BackupRepository
) : ViewModel() {

    // UI States
    private val _importState = MutableStateFlow<UiState<ImportResult>>(UiState.Empty)
    val importState: StateFlow<UiState<ImportResult>> = _importState.asStateFlow()

    // Progress/Loading
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Complete event (for navigation back to MainActivity)
    private val _importComplete = MutableSharedFlow<Boolean>()
    val importComplete: SharedFlow<Boolean> = _importComplete.asSharedFlow()

    /**
     * Import tasks from JSON file
     */
    fun importFromJson(
        uri: Uri,
        conflictAction: ConflictAction = ConflictAction.SKIP
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _importState.value = UiState.Loading

            val result = backupRepository.importFromJson(uri, conflictAction)

            _isImporting.value = false

            if (result.isSuccess) {
                _importState.value = UiState.Success(result)
                val message = buildSuccessMessage(result)
                _userMessage.emit(message)
                _importComplete.emit(true)
            } else {
                _importState.value = UiState.Error(result.errorMessage ?: "Unknown error")
                _userMessage.emit("❌ ${result.errorMessage}")
            }
        }
    }

    /**
     * Build success message based on import stats
     */
    private fun buildSuccessMessage(result: ImportResult): String {
        return buildString {
            append("✅ Import hoàn tất:\n")
            append("• ${result.successCount} tasks imported\n")
            if (result.skipCount > 0) {
                append("• ${result.skipCount} tasks skipped (conflicts)\n")
            }
            if (result.failureCount > 0) {
                append("• ${result.failureCount} tasks invalid")
            }
        }
    }

    /**
     * Reset state (for retry or new import)
     */
    fun resetState() {
        _importState.value = UiState.Empty
        _isImporting.value = false
    }
}
