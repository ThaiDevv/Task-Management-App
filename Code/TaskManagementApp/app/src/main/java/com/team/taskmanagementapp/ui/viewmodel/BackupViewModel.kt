package com.team.taskmanagementapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.data.repository.BackupUiState
import com.team.taskmanagementapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for backup and restore operations.
 * Exposes repository StateFlow and handles UI logic.
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = com.team.taskmanagementapp.data.local.db.AppDatabase.getInstance(application).taskDao()
    private val taskRepository = TaskRepository(taskDao)
    private val backupRepository = BackupRepository(application, taskDao, taskRepository)

    // Expose repository UI state
    val uiState: StateFlow<BackupUiState> = backupRepository.uiState

    // Local state for task count
    private val _taskCount = MutableStateFlow(0)
    val taskCount: StateFlow<Int> = _taskCount.asStateFlow()

    // Last backup time
    private val _lastBackupTime = MutableStateFlow<String?>(null)
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    init {
        loadTaskCount()
        loadLastBackupTime()
    }

    /**
     * Export tasks to the given URI.
     */
    fun exportTasks(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportTasks(uri)
            loadLastBackupTime()
        }
    }

    /**
     * Import tasks from the given URI.
     */
    fun importTasks(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importTasks(uri)
            loadTaskCount() // Refresh count after import
        }
    }

    /**
     * Reset UI state to idle.
     */
    fun resetState() {
        backupRepository.resetState()
    }

    /**
     * Load task count from repository.
     */
    private fun loadTaskCount() {
        viewModelScope.launch {
            _taskCount.value = backupRepository.getTaskCount()
        }
    }

    /**
     * Load last backup time from repository.
     */
    private fun loadLastBackupTime() {
        _lastBackupTime.value = backupRepository.getLastBackupTimeFormatted()
    }
}
