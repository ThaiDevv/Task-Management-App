package com.team.taskmanagementapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    private val _events = MutableSharedFlow<AddEditTaskEvent>()
    val events: SharedFlow<AddEditTaskEvent> = _events.asSharedFlow()

    private var loadTaskJob: Job? = null

    fun loadTask(taskId: Long) {
        loadTaskJob?.cancel()
        loadTaskJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.observeTaskById(taskId)
                .catch { throwable ->
                    _uiState.value = UiState.Error(
                        throwable.message ?: "Failed to load task",
                        throwable
                    )
                }
                .collect { task ->
                    if (task == null) {
                        _uiState.value = UiState.Error("Task not found")
                    } else {
                        _task.value = task
                        _uiState.value = UiState.Success(Unit)
                    }
                }
        }
    }

    suspend fun createTask(
        title: String,
        description: String,
        dueDate: Long,
        dueTime: Long,
        priority: Priority,
        recurrenceType: RecurrenceType,
        reminderMinutes: Int,
        status: TaskStatus
    ) {
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Title is required")
            return
        }

        _uiState.value = UiState.Loading
        try {
            val now = System.currentTimeMillis()
            val task = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                status = status,
                isCompleted = status == TaskStatus.COMPLETED,
                isRecurring = recurrenceType != RecurrenceType.NONE,
                recurrenceType = recurrenceType,
                reminderMinutes = reminderMinutes,
                createdAt = now,
                updatedAt = now
            )
            val taskId = repository.insert(task)
            val savedTask = task.copy(id = taskId.toInt())

            _uiState.value = UiState.Success(Unit)
            _events.emit(AddEditTaskEvent.TaskSaved(savedTask, false))
        } catch (exception: Exception) {
            _uiState.value = UiState.Error(exception.message ?: "Failed to save task", exception)
        }
    }

    suspend fun updateTask(
        id: Int,
        title: String,
        description: String,
        dueDate: Long,
        dueTime: Long,
        priority: Priority,
        recurrenceType: RecurrenceType,
        reminderMinutes: Int,
        status: TaskStatus
    ) {
        if (title.isBlank()) {
            _uiState.value = UiState.Error("Title is required")
            return
        }

        _uiState.value = UiState.Loading
        try {
            val existingTask = repository.getTaskById(id.toLong())
            if (existingTask == null) {
                _uiState.value = UiState.Error("Task not found to update")
                return
            }

            val shouldRescheduleNotification =
                existingTask.dueDate != dueDate ||
                    existingTask.dueTime != dueTime ||
                    existingTask.reminderMinutes != reminderMinutes
            val updatedTask = existingTask.copy(
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                status = status,
                isCompleted = status == TaskStatus.COMPLETED,
                isRecurring = recurrenceType != RecurrenceType.NONE,
                recurrenceType = recurrenceType,
                reminderMinutes = reminderMinutes,
                updatedAt = System.currentTimeMillis()
            )

            repository.update(updatedTask)
            _uiState.value = UiState.Success(Unit)
            _events.emit(
                AddEditTaskEvent.TaskSaved(updatedTask, shouldRescheduleNotification)
            )
        } catch (exception: Exception) {
            _uiState.value = UiState.Error(exception.message ?: "Failed to update task", exception)
        }
    }
}

sealed class AddEditTaskEvent {
    data class TaskSaved(
        val task: Task,
        val shouldRescheduleNotification: Boolean
    ) : AddEditTaskEvent()
}
