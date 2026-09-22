package com.team.taskmanagementapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.repository.BackupHistoryItem
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.data.repository.BackupUiState
import com.team.taskmanagementapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Backup & Restore screen (TMA-52 + Task 16).
 * Exposes repository StateFlow and handles UI logic.
 *
 * UI (fragment) chỉ nói chuyện với ViewModel — mọi thao tác Room nằm ở
 * [BackupRepository] nên fragment/activity không truy cập DAO.
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val taskDao = database.taskDao()
    private val taskRepository = TaskRepository(taskDao)
    private val backupRepository = BackupRepository(application, taskDao, taskRepository)

    // Expose repository UI state (loading / success / error → thông báo kết quả)
    val uiState: StateFlow<BackupUiState> = backupRepository.uiState

    // Local state for task count
    private val _taskCount = MutableStateFlow(0)
    val taskCount: StateFlow<Int> = _taskCount.asStateFlow()

    /** Số task đã hoàn thành — hiển thị ở chip thống kê của màn Backup. */
    private val _completedTaskCount = MutableStateFlow(0)
    val completedTaskCount: StateFlow<Int> = _completedTaskCount.asStateFlow()

    /** Số phiên Pomodoro sẽ được ghi vào file backup. */
    private val _pomodoroSessionCount = MutableStateFlow(0)
    val pomodoroSessionCount: StateFlow<Int> = _pomodoroSessionCount.asStateFlow()

    // Last backup time
    private val _lastBackupTime = MutableStateFlow<String?>(null)
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    /** Danh sách file backup gần đây (đọc từ SharedPreferences qua repository). */
    private val _recentBackups = MutableStateFlow<List<BackupHistoryItem>>(emptyList())
    val recentBackups: StateFlow<List<BackupHistoryItem>> = _recentBackups.asStateFlow()

    init {
        refreshStats()
    }

    /**
     * Task 16 — **Backup**: ghi Task (+ counters Pomodoro) và toàn bộ PomodoroSession ra file.
     */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportBackup(uri)
            refreshStats()
        }
    }

    /**
     * Task 16 — **Restore**: đọc file backup, validate rồi ghi lại toàn bộ trong 1 transaction.
     */
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.restoreBackup(uri)
            refreshStats()
        }
    }

    /**
     * Export tasks to the given URI (API cũ, nay ghi kèm cả dữ liệu Pomodoro).
     */
    fun exportTasks(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportTasks(uri)
            refreshStats()
        }
    }

    /**
     * Import tasks from the given URI (API cũ: import có xử lý trùng lặp, không dùng cho restore).
     */
    fun importTasks(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importTasks(uri)
            refreshStats() // Refresh count after import
        }
    }

    /**
     * Đọc lại số liệu hiển thị (task / completed / Pomodoro session) và lịch sử backup.
     */
    fun refreshStats() {
        viewModelScope.launch {
            _taskCount.value = backupRepository.getTaskCount()
            _completedTaskCount.value = backupRepository.getCompletedTaskCount()
            _pomodoroSessionCount.value = backupRepository.getPomodoroSessionCount()
        }
        _lastBackupTime.value = backupRepository.getLastBackupTimeFormatted()
        _recentBackups.value = backupRepository.getRecentBackups()
    }

    /**
     * Ghi nhận file backup vừa tạo vào lịch sử (tên file, thời điểm, dung lượng).
     */
    fun addRecentBackup(fileName: String, formattedDate: String, sizeString: String, timestamp: Long) {
        backupRepository.addRecentBackup(fileName, formattedDate, sizeString, timestamp)
        refreshStats()
    }

    /**
     * Reset UI state to idle.
     */
    fun resetState() {
        backupRepository.resetState()
    }
}
