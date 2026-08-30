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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddEditTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableLiveData<UiState<Task>>()
    val uiState: LiveData<UiState<Task>> = _uiState

    private val _task = MutableLiveData<Task?>()
    val task: LiveData<Task?> = _task

    fun observeTask(taskId: Long): Flow<Task?> = repository.observeTaskById(taskId)

    suspend fun updateTask(task: Task): Task {
        val now = System.currentTimeMillis()
        val updatedTask = task.copy(updatedAt = maxOf(now, task.updatedAt + 1L))
        repository.update(updatedTask)
        return updatedTask
    }

    suspend fun updateFutureRecurringTasks(
        originalTitle: String,
        originalRecurrence: RecurrenceType,
        startDate: Long,
        editedTask: Task
    ): Task {
        val now = System.currentTimeMillis()
        val updatedTask = editedTask.copy(updatedAt = maxOf(now, editedTask.updatedAt + 1L))
        repository.updateFutureRecurringTasks(
            originalTitle = originalTitle,
            originalRecurrence = originalRecurrence,
            startDate = startDate,
            newTitle = updatedTask.title,
            newDescription = updatedTask.description,
            newPriority = updatedTask.priority,
            newRecurrenceType = updatedTask.recurrenceType,
            newReminderMinutes = updatedTask.reminderMinutes,
            updatedAt = updatedTask.updatedAt
        )
        repository.update(updatedTask)
        return updatedTask
    }

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getTaskById(taskId)
            if (result != null) {
                _task.value = result
                _uiState.value = UiState.Success(result)
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
                val savedTask = if (isEdit) {
                    val existingTask = repository.getTaskById(id.toLong())
                    if (existingTask != null) {
                        val updatedTask = existingTask.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            priority = priority,
                            isRecurring = recurrenceType != RecurrenceType.NONE,
                            recurrenceType = recurrenceType,
                            reminderMinutes = reminderMinutes,
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.update(updatedTask)
                        updatedTask
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
                        isRecurring = recurrenceType != RecurrenceType.NONE,
                        recurrenceType = recurrenceType,
                        reminderMinutes = reminderMinutes,
                        status = status,
                        isCompleted = false,
                        createdAt = now,
                        updatedAt = now
                    )
                    val insertedId = repository.insert(task)
                    task.copy(id = insertedId.toInt())
                }
                _uiState.value = UiState.Success(savedTask)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to save task")
            }
        }
    }
}
