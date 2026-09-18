package com.team.taskmanagementapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.team.taskmanagementapp.data.repository.PomodoroSettingsRepository
import com.team.taskmanagementapp.data.repository.TaskRepository

/**
 * Factory cho [PomodoroViewModel] — theo đúng pattern của `TaskViewModelFactory` /
 * `StatsViewModelFactory` trong project.
 *
 * Cần [TaskRepository] cho Task Selector (Task 10) và [PomodoroSettingsRepository]
 * cho cấu hình Pomodoro (Task 11).
 */
class PomodoroViewModelFactory(
    private val taskRepository: TaskRepository,
    private val settingsRepository: PomodoroSettingsRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            // Chỉ giữ application context để không leak Activity/Fragment vào ViewModel.
            return PomodoroViewModel(
                taskRepository,
                settingsRepository,
                context.applicationContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
