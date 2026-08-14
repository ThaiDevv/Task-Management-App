package com.team.taskmanagementapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

import android.content.SharedPreferences

class TaskViewModel(
    private val repository: TaskRepository,
    private val preferences: SharedPreferences? = null
) : ViewModel() {

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess = _deleteSuccess.asSharedFlow()

    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()


    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    init {
        loadAllTasks()
    }


    fun loadAllTasks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getFilteredTasks(_filterCriteria.value)
                .catch { e ->
                    _uiState.value = UiState.Error("Không thể tải danh sách công việc: ${e.localizedMessage}")
                }
                .collect { tasks ->
                    if (tasks.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(tasks)
                    }
                }
        }
    }

    fun insertTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.insert(task)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi thêm công việc: ${e.localizedMessage}")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi cập nhật công việc: ${e.localizedMessage}")
            }
        }
    }


    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.delete(task)
                _deleteSuccess.emit(true)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi xóa công việc: ${e.localizedMessage}")
            }
        }
    }


    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val wasCompleted = task.isCompleted

                val updatedTask = task.copy(
                    isCompleted = !wasCompleted,
                    status = if (!wasCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
                    updatedAt = now
                )

                // Mark task as completed/uncompleted first
                repository.update(updatedTask)

                // Recurring task completed -> create the next task instance
                if (!wasCompleted && task.isRecurring && task.recurrenceType != RecurrenceType.NONE) {
                    val nextInstance = task.copy(
                        id = 0,
                        isCompleted = false,
                        status = TaskStatus.TODO,
                        dueDate = calculateNextDueDate(task),
                        dueTime = task.dueTime,
                        createdAt = now,
                        updatedAt = now
                    )
                    repository.insert(nextInstance)
                }

                // Keep detail screen in sync
                if (_selectedTask.value?.id == task.id) {
                    _selectedTask.value = updatedTask
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi cập nhật trạng thái công việc: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Calculate the next due date based on the task's recurrence type and interval.
     */
    private fun calculateNextDueDate(task: Task): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        when (task.recurrenceType) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, task.recurrenceInterval)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, task.recurrenceInterval)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, task.recurrenceInterval)
            RecurrenceType.NONE -> Unit
        }
        return calendar.timeInMillis
    }


    fun searchTasks(query: String) {
        if (query.isBlank()) {
            loadAllTasks()
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.search(query)
                .catch { e ->
                    _uiState.value = UiState.Error("Lỗi tìm kiếm: ${e.localizedMessage}")
                }
                .collect { tasks ->
                    if (tasks.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(tasks)
                    }
                }
        }
    }

    fun applyFilter(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
        loadAllTasks()
    }

    fun clearFilter() {
        _filterCriteria.value = FilterCriteria()
        loadAllTasks()
    }


    fun filterByStatus(status: TaskStatus) {
        applyFilter(_filterCriteria.value.copy(status = status))
    }


    fun filterByPriority(priority: Priority) {
        applyFilter(_filterCriteria.value.copy(priority = priority))
    }

    fun getTaskById(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            _selectedTask.value = task
        }
    }
}
