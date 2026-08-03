package com.team.taskmanagementapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.ui.base.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    init {
        loadAllTasks()
    }


    fun loadAllTasks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getAllTasks()
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
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Lỗi khi xóa công việc: ${e.localizedMessage}")
            }
        }
    }


    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                status = if (!task.isCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updatedTask)
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


    fun filterByStatus(status: TaskStatus) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getTasksByStatus(status)
                .catch { e ->
                    _uiState.value = UiState.Error("Lỗi lọc theo trạng thái: ${e.localizedMessage}")
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


    fun filterByPriority(priority: Priority) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getTasksByPriority(priority)
                .catch { e ->
                    _uiState.value = UiState.Error("Lỗi lọc theo mức độ ưu tiên: ${e.localizedMessage}")
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

    fun getTaskById(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            _selectedTask.value = task
        }
    }
}
