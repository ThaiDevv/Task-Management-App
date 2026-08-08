package com.team.taskmanagementapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import kotlinx.coroutines.launch

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableLiveData<UiState<Unit>>()
    val uiState: LiveData<UiState<Unit>> = _uiState

    private val _task = MutableLiveData<Task?>()
    val task: LiveData<Task?> = _task

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getTaskById(taskId)
            if (result != null) {
                _task.value = result
                _uiState.value = UiState.Success(Unit)
            } else {
                _uiState.value = UiState.Error("Task not found")
            }
        }
    }

    fun saveTask(
        id: Int = 0,
        title: String,
        description: String,
        dueDate: Long,
        dueTime: Long,
        priority: Priority,
        recurrenceType: RecurrenceType,
        reminderMinutes: Int,
        status: TaskStatus,
        isEdit: Boolean
    ) {
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Title is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                if (isEdit) {
                    val existingTask = repository.getTaskById(id.toLong())
                    if (existingTask != null) {
                        val updatedTask = existingTask.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            priority = priority,
                            recurrenceType = recurrenceType,
                            reminderMinutes = reminderMinutes,
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.update(updatedTask)
                    } else {
                        _uiState.value = UiState.Error("Task not found to update")
                        return@launch
                    }
                } else {
                    val now = System.currentTimeMillis()
                    val task = Task(
                        id = 0,
                        title = title.trim(),
                        description = description.trim(),
                        dueDate = dueDate,
                        dueTime = dueTime,
                        priority = priority,
                        recurrenceType = recurrenceType,
                        reminderMinutes = reminderMinutes,
                        status = status,
                        isCompleted = false,
                        createdAt = now,
                        updatedAt = now
                    )
                    repository.insert(task)
                }
                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to save task")
            }
        }
    }
}
