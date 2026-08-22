package com.team.taskmanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel riêng cho màn hình Calendar.
 *
 * Quản lý:
 * - Load tasks theo từng tháng từ Room qua Flow
 * - Cache Map<startOfDay: Long, List<Task>> cho tháng hiện tại
 * - StateFlow tasks của ngày được chọn
 * - Điều hướng tháng (prev/next)
 */
class CalendarViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    // ── Tháng đang hiển thị ──────────────────────────────────────────────────
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // ── Cache tasks của tháng: key = startOfDay millis, value = danh sách task ─
    private val _monthCache = MutableStateFlow<Map<Long, List<Task>>>(emptyMap())
    val monthCache: StateFlow<Map<Long, List<Task>>> = _monthCache.asStateFlow()

    // Toàn bộ task thuộc tháng đang hiển thị.
    private val _tasksForMonth = MutableStateFlow<List<Task>>(emptyList())
    val tasksForMonth: StateFlow<List<Task>> = _tasksForMonth.asStateFlow()

    // Tập các ngày có task, mỗi phần tử đã được chuẩn hóa về 00:00:00.000.
    private val _datesWithTasks = MutableStateFlow<Set<Long>>(emptySet())
    val datesWithTasks: StateFlow<Set<Long>> = _datesWithTasks.asStateFlow()

    // ── Tasks của ngày được chọn ─────────────────────────────────────────────
    private val _tasksForSelectedDate = MutableStateFlow<List<Task>>(emptyList())
    val tasksForSelectedDate: StateFlow<List<Task>> = _tasksForSelectedDate.asStateFlow()

    // ── Ngày đang được chọn (startOfDay millis) ──────────────────────────────
    private val _selectedDate = MutableStateFlow(startOfDay(Calendar.getInstance()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // ── Trạng thái loading/error ─────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var monthJob: Job? = null

    init {
        loadTasksForMonth(_currentYear.value, _currentMonth.value)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Load tasks cho tháng [year]/[month] từ Room qua Flow toàn bộ task.
     * Tự động phản ứng với bất kỳ thay đổi thêm/sửa/xóa task nào trong DB.
     */
    fun loadTasksForMonth(year: Int, month: Int) {
        monthJob?.cancel()

        _currentYear.value = year
        _currentMonth.value = month
        _isLoading.value = true
        _errorMessage.value = null

        monthJob = viewModelScope.launch {
            repository.getAllTasks()
                .catch { e ->
                    _isLoading.value = false
                    _errorMessage.value = "Không thể tải lịch: ${e.localizedMessage}"
                }
                .collect { allTasks ->
                    _isLoading.value = false
                    // Lọc tất cả task thuộc năm/tháng đang chọn
                    val monthTasks = allTasks.filter { task ->
                        isTaskInMonth(task, year, month)
                    }
                    _tasksForMonth.value = monthTasks

                    // Nhóm tasks theo ngày (startOfDay)
                    val grouped = buildMonthCache(monthTasks)
                    _monthCache.value = grouped
                    _datesWithTasks.value = grouped.keys

                    // Cập nhật lại danh sách ngày đang chọn
                    refreshSelectedDateTasks(grouped)
                }
        }
    }

    /**
     * Lấy danh sách tasks cho một ngày cụ thể từ cache.
     * [date] là bất kỳ timestamp nào trong ngày đó.
     */
    fun getTasksForDate(date: Long): List<Task> {
        val key = startOfDay(date)
        return _monthCache.value[key] ?: emptyList()
    }

    /**
     * Người dùng tap vào ngày [date] → cập nhật selectedDate và tasksForSelectedDate.
     * [date] là bất kỳ timestamp nào trong ngày đó.
     */
    fun selectDate(date: Long) {
        val key = startOfDay(date)
        _selectedDate.value = key
        _tasksForSelectedDate.value = _monthCache.value[key] ?: emptyList()
    }

    /**
     * Di chuyển tháng theo [offset]: -1 là tháng trước, 1 là tháng sau.
     */
    fun navigateMonth(offset: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, _currentYear.value)
            set(Calendar.MONTH, _currentMonth.value)
            add(Calendar.MONTH, offset)
        }
        loadTasksForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun goToPreviousMonth() = navigateMonth(-1)

    fun goToNextMonth() = navigateMonth(1)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Kiểm tra task có thuộc [year] và [month] đang hiển thị hay không.
     */
    private fun isTaskInMonth(task: Task, year: Int, month: Int): Boolean {
        if (task.dueDate <= 0L) return false
        val cal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    /**
     * Nhóm danh sách [tasks] thành Map<startOfDay, List<Task>>.
     */
    private fun buildMonthCache(tasks: List<Task>): Map<Long, List<Task>> {
        return tasks.groupBy { task -> startOfDay(task.dueDate) }
    }

    /** Cập nhật tasksForSelectedDate dựa trên cache mới nhất. */
    private fun refreshSelectedDateTasks(cache: Map<Long, List<Task>>) {
        _tasksForSelectedDate.value = cache[_selectedDate.value] ?: emptyList()
    }

    /** Trả về timestamp 00:00:00.000 của ngày chứa [millis] */
    private fun startOfDay(millis: Long): Long = DateTimeUtils.getStartOfDay(millis)

    private fun startOfDay(cal: Calendar): Long = DateTimeUtils.getStartOfDay(cal.timeInMillis)

    override fun onCleared() {
        super.onCleared()
        monthJob?.cancel()
    }
}
