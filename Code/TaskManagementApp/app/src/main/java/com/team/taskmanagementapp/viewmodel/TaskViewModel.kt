package com.team.taskmanagementapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.NotificationHelper
import com.team.taskmanagementapp.util.RecurrenceHelper
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
        viewModelScope.launch {
            // Bước 1: Chờ UPDATE hoàn thành (suspend) trước khi collect Flow.
            // Đảm bảo database đã commit trạng thái OVERDUE mới nhất
            // trước khi UI nhận bất kỳ emission nào.
            repository.checkAndUpdateOverdueTasks()
            // Bước 2: Bắt đầu collect sau khi UPDATE đã xong.
            loadAllTasks()
        }
    }


    fun loadAllTasks() {
        taskListJob?.cancel()
        taskListJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getFilteredTasks(_filterCriteria.value)
                .catch { e ->
                    _uiState.value = UiState.Error("Unable to load tasks: ${e.localizedMessage}")
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
                val savedTask = task.copy(id = insertedId.toInt())
                val scheduleResult = AlarmScheduler.scheduleAlarm(
                    applicationContext,
                    savedTask
                )
                _userMessage.emit(
                    scheduleWarning(scheduleResult, savedTask.reminderMinutes)
                        ?: "Added task \"${task.title}\""
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error adding task: ${e.localizedMessage}")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task)
                val scheduleResult = AlarmScheduler.rescheduleAlarm(applicationContext, task)
                _userMessage.emit(
                    scheduleWarning(scheduleResult, task.reminderMinutes)
                        ?: "Updated task \"${task.title}\""
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error updating task: ${e.localizedMessage}")
            }
        }
    }


    fun deleteTask(task: Task, deleteAllFuture: Boolean = false) {
        viewModelScope.launch {
            try {
                if (deleteAllFuture && task.isRecurring && task.recurrenceType != RecurrenceType.NONE) {
                    val futureTasks = repository.getFutureRecurringTasks(task.title, task.recurrenceType, task.dueDate)
                    futureTasks.forEach { futureTask ->
                        AlarmScheduler.cancelAlarm(applicationContext, futureTask.id)
                        NotificationHelper.cancelNotification(applicationContext, futureTask.id)
                    }
                    repository.deleteFutureRecurringTasks(task.title, task.recurrenceType, task.dueDate)
                } else {
                    repository.delete(task)
                }
                AlarmScheduler.cancelAlarm(applicationContext, task.id)
                NotificationHelper.cancelNotification(applicationContext, task.id)
                _deleteSuccess.emit(true)
                _userMessage.emit("Deleted task \"${task.title}\"")
            } catch (e: Exception) {
                _userMessage.emit("Error deleting task: ${e.localizedMessage}")
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
                var reminderScheduleResult: AlarmScheduler.ScheduleResult? = null
                if (updatedTask.isCompleted) {
                    AlarmScheduler.cancelAlarm(applicationContext, updatedTask.id)
                    NotificationHelper.cancelNotification(applicationContext, updatedTask.id)
                } else {
                    reminderScheduleResult =
                        AlarmScheduler.scheduleAlarm(applicationContext, updatedTask)
                }

                val isRecurringTask = task.isRecurring || task.recurrenceType != RecurrenceType.NONE

                if (isRecurringTask) {
                    val nextDueDate = RecurrenceHelper.calculateNextDueDate(
                        task.dueDate,
                        task.recurrenceType,
                        task.recurrenceInterval
                    )
                    val nextDueTime = if (task.dueTime > 0L) {
                        RecurrenceHelper.calculateNextDueDate(
                            task.dueTime,
                            task.recurrenceType,
                            task.recurrenceInterval
                        )
                    } else task.dueTime

                    if (!wasCompleted) {
                        val isEnded = RecurrenceHelper.isRecurrenceEnded(task, nextDueDate)
                        if (!isEnded && !task.isPaused) {
                            // Khi đánh dấu hoàn thành: Chỉ tạo instance tiếp theo nếu chưa có task tương lai cùng title tồn tại
                            val existingFutureTasks = repository.getFutureRecurringTasks(
                                task.title,
                                task.recurrenceType,
                                nextDueDate
                            )
                            val anyRecurringFutureTasks = if (existingFutureTasks.isNotEmpty()) {
                                existingFutureTasks
                            } else {
                                repository.getFutureRecurringTasks(task.title, RecurrenceType.DAILY, nextDueDate) +
                                repository.getFutureRecurringTasks(task.title, RecurrenceType.WEEKLY, nextDueDate) +
                                repository.getFutureRecurringTasks(task.title, RecurrenceType.MONTHLY, nextDueDate) +
                                repository.getFutureRecurringTasks(task.title, RecurrenceType.YEARLY, nextDueDate)
                            }

                            if (anyRecurringFutureTasks.isEmpty()) {
                                val nextInstance = task.copy(
                                    id = 0,
                                    isCompleted = false,
                                    status = TaskStatus.TODO,
                                    isRecurring = true,
                                    recurrenceType = task.recurrenceType,
                                    dueDate = nextDueDate,
                                    dueTime = nextDueTime,
                                    repeatEndDate = task.repeatEndDate,
                                    repeatLimitCount = task.repeatLimitCount,
                                    currentOccurrence = task.currentOccurrence + 1,
                                    isPaused = task.isPaused,
                                    createdAt = now,
                                    updatedAt = now
                                )
                                val insertedId = repository.insert(nextInstance)
                                reminderScheduleResult = AlarmScheduler.scheduleAlarm(
                                    applicationContext,
                                    nextInstance.copy(id = insertedId.toInt())
                                )
                            }
                        }
                    } else {
                        // Khi hủy hoàn thành: Hủy alarm và xóa các task tương lai chưa hoàn thành đã tự sinh ra (bất kể kiểu lặp lại)
                        val futureTasks = (
                            repository.getFutureRecurringTasks(task.title, task.recurrenceType, nextDueDate) +
                            repository.getFutureRecurringTasks(task.title, RecurrenceType.DAILY, nextDueDate) +
                            repository.getFutureRecurringTasks(task.title, RecurrenceType.WEEKLY, nextDueDate) +
                            repository.getFutureRecurringTasks(task.title, RecurrenceType.MONTHLY, nextDueDate) +
                            repository.getFutureRecurringTasks(task.title, RecurrenceType.YEARLY, nextDueDate)
                        ).distinctBy { it.id }

                        futureTasks.forEach { futureTask ->
                            AlarmScheduler.cancelAlarm(applicationContext, futureTask.id)
                            repository.delete(futureTask)
                        }
                    }
                }

                // Keep detail screen in sync
                if (_selectedTask.value?.id == task.id) {
                    _selectedTask.value = updatedTask
                }

                val msg = if (!wasCompleted) {
                    "Completed \"${task.title}\""
                } else {
                    "Marked \"${task.title}\" as incomplete"
                }
                _userMessage.emit(
                    reminderScheduleResult?.let {
                        scheduleWarning(it, task.reminderMinutes)
                    } ?: msg
                )
            } catch (e: Exception) {
                _userMessage.emit("Error updating status: ${e.localizedMessage}")
            }
        }
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
                    _uiState.value = UiState.Error("Search error: ${e.localizedMessage}")
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
        applyFilter(_filterCriteria.value.copy(statuses = setOf(status)))
    }


    fun filterByPriority(priority: Priority) {
        applyFilter(_filterCriteria.value.copy(priorities = setOf(priority)))
    }

    fun getTaskById(taskId: Long) {
        viewModelScope.launch {
            repository.observeTaskById(taskId).collect { task ->
                _selectedTask.value = task
            }
        }
    }

    fun toggleTaskPause(task: Task) {
        viewModelScope.launch {
            try {
                val newPausedState = !task.isPaused
                val updatedTask = task.copy(
                    isPaused = newPausedState,
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(updatedTask)
                if (_selectedTask.value?.id == task.id) {
                    _selectedTask.value = updatedTask
                }
                _userMessage.emit(
                    if (newPausedState) "Paused recurring task"
                    else "Resumed recurring task"
                )
            } catch (e: Exception) {
                _userMessage.emit("Error: ${e.localizedMessage}")
            }
        }
    }

    private fun scheduleWarning(
        result: AlarmScheduler.ScheduleResult,
        reminderMinutes: Int
    ): String? = when (result) {
        AlarmScheduler.ScheduleResult.FALLBACK_SCHEDULED ->
            applicationContext.getString(R.string.notification_schedule_fallback)
        AlarmScheduler.ScheduleResult.NOTIFICATIONS_DISABLED ->
            applicationContext.getString(R.string.notification_schedule_disabled)
        AlarmScheduler.ScheduleResult.FAILED ->
            applicationContext.getString(R.string.notification_schedule_failed)
        AlarmScheduler.ScheduleResult.SKIPPED -> if (reminderMinutes > 0) {
            applicationContext.getString(R.string.notification_schedule_past)
        } else {
            null
        }
        AlarmScheduler.ScheduleResult.SCHEDULED -> null
    }
}
