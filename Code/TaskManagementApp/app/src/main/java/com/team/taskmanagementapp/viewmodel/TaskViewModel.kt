package com.team.taskmanagementapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.util.AlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(
    private val repository: TaskRepository,
    context: Context,
    private val preferences: SharedPreferences? = null
) : ViewModel() {

    private val applicationContext = context.applicationContext

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess = _deleteSuccess.asSharedFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()

    private var taskListJob: Job? = null


    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    init {
        loadAllTasks()
    }


    fun loadAllTasks() {
        taskListJob?.cancel()
        taskListJob = viewModelScope.launch {
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
                val insertedId = repository.insert(task)
                AlarmScheduler.scheduleAlarm(
                    applicationContext,
                    task.copy(id = insertedId.toInt())
                )
                _userMessage.emit("Đã thêm công việc \"${task.title}\"")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi thêm công việc: ${e.localizedMessage}")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task)
                AlarmScheduler.rescheduleAlarm(applicationContext, task)
                _userMessage.emit("Đã cập nhật công việc \"${task.title}\"")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi cập nhật công việc: ${e.localizedMessage}")
            }
        }
    }


    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.delete(task)
                AlarmScheduler.cancelAlarm(applicationContext, task.id)
                _deleteSuccess.emit(true)
                _userMessage.emit("Đã xóa công việc \"${task.title}\"")
            } catch (e: Exception) {
                _userMessage.emit("Lỗi khi xóa công việc: ${e.localizedMessage}")
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
                if (updatedTask.isCompleted) {
                    AlarmScheduler.cancelAlarm(applicationContext, updatedTask.id)
                } else {
                    AlarmScheduler.scheduleAlarm(applicationContext, updatedTask)
                }

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
                    val insertedId = repository.insert(nextInstance)
                    AlarmScheduler.scheduleAlarm(
                        applicationContext,
                        nextInstance.copy(id = insertedId.toInt())
                    )
                }

                // Keep detail screen in sync
                if (_selectedTask.value?.id == task.id) {
                    _selectedTask.value = updatedTask
                }

                val msg = if (!wasCompleted) {
                    "Đã hoàn thành \"${task.title}\""
                } else {
                    "Đã đánh dấu chưa xong \"${task.title}\""
                }
                _userMessage.emit(msg)
            } catch (e: Exception) {
                _userMessage.emit("Lỗi khi cập nhật trạng thái: ${e.localizedMessage}")
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
            repository.observeTaskById(taskId).collect { task ->
                _selectedTask.value = task
            }
        }
    }
}
