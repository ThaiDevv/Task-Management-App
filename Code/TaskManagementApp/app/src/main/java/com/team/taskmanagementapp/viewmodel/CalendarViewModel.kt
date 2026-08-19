package com.team.taskmanagementapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.repository.TaskRepository
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
 * - Load tasks theo từng tháng qua Flow từ Room
 * - Cache Map<startOfDay: Long, List<Task>> cho tháng hiện tại
 * - StateFlow tasks của ngày được chọn
 * - Điều hướng tháng (prev/next)
 */
class CalendarViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    // ── Tháng đang hiển thị ──────────────────────────────────────────────────
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // ── Cache tasks của tháng: key = startOfDay millis, value = danh sách task ─
    private val _monthCache = MutableStateFlow<Map<Long, List<Task>>>(emptyMap())
    val monthCache: StateFlow<Map<Long, List<Task>>> = _monthCache.asStateFlow()

    // ── Tasks của ngày được chọn ─────────────────────────────────────────────
    private val _selectedDateTasks = MutableStateFlow<List<Task>>(emptyList())
    val selectedDateTasks: StateFlow<List<Task>> = _selectedDateTasks.asStateFlow()

    // ── Ngày đang được chọn (startOfDay millis) ──────────────────────────────
    private val _selectedDate = MutableStateFlow(startOfDay(Calendar.getInstance()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // ── Trạng thái loading/error ─────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Job của lần collect hiện tại — cancel khi chuyển tháng
    private var monthJob: Job? = null

    init {
        loadTasksForMonth(_currentYear.value, _currentMonth.value)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Load (hoặc reload) tasks cho tháng [year]/[month] từ Room qua Flow.
     * Kết quả được nhóm thành Map<startOfDay, List<Task>> và lưu vào cache.
     */
    fun loadTasksForMonth(year: Int, month: Int) {
        // Huỷ collect tháng cũ nếu còn đang chạy
        monthJob?.cancel()

        _currentYear.value = year
        _currentMonth.value = month
        _isLoading.value = true
        _errorMessage.value = null

        val (startMs, endMs) = monthRange(year, month)

        monthJob = viewModelScope.launch {
            repository.getTasksDateRange(startMs, endMs)
                .catch { e ->
                    _isLoading.value = false
                    _errorMessage.value = "Không thể tải lịch: ${e.localizedMessage}"
                }
                .collect { tasks ->
                    _isLoading.value = false
                    // Nhóm tasks theo ngày
                    val grouped = buildMonthCache(tasks)
                    _monthCache.value = grouped
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
     * Người dùng tap vào ngày [date] → cập nhật selectedDate và selectedDateTasks.
     * [date] là bất kỳ timestamp nào trong ngày đó.
     */
    fun selectDate(date: Long) {
        val key = startOfDay(date)
        _selectedDate.value = key
        _selectedDateTasks.value = _monthCache.value[key] ?: emptyList()
    }

    /** Chuyển sang tháng trước */
    fun goToPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _currentYear.value)
            set(Calendar.MONTH, _currentMonth.value)
            add(Calendar.MONTH, -1)
        }
        loadTasksForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    /** Chuyển sang tháng sau */
    fun goToNextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _currentYear.value)
            set(Calendar.MONTH, _currentMonth.value)
            add(Calendar.MONTH, 1)
        }
        loadTasksForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Trả về [startMs, endMs] của tháng [year]/[month] (toàn bộ từ 00:00:00.000
     * ngày đầu đến 23:59:59.999 ngày cuối tháng).
     */
    private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(year, month, start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return start.timeInMillis to end.timeInMillis
    }

    /**
     * Nhóm danh sách [tasks] thành Map<startOfDay, List<Task>>.
     * Mỗi key là timestamp đầu ngày (00:00:00.000) của ngày đó.
     */
    private fun buildMonthCache(tasks: List<Task>): Map<Long, List<Task>> {
        return tasks.groupBy { task -> startOfDay(task.dueDate) }
    }

    /** Cập nhật selectedDateTasks dựa trên cache mới nhất */
    private fun refreshSelectedDateTasks(cache: Map<Long, List<Task>>) {
        _selectedDateTasks.value = cache[_selectedDate.value] ?: emptyList()
    }

    /** Trả về timestamp 00:00:00.000 của ngày chứa [millis] */
    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Overload: nhận Calendar thay vì Long */
    private fun startOfDay(cal: Calendar): Long = startOfDay(cal.timeInMillis)

    override fun onCleared() {
        super.onCleared()
        monthJob?.cancel()
    }
}
