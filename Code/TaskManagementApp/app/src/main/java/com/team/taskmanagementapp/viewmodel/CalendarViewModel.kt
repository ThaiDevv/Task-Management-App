package com.team.taskmanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

        // Không để cache của tháng trước xuất hiện tạm thời khi header đã đổi tháng.
        _tasksForMonth.value = emptyList()
        _monthCache.value = emptyMap()
        _datesWithTasks.value = emptySet()
        _tasksForSelectedDate.value = emptyList()

        val (startMs, endMs) = monthRange(year, month)

        monthJob = viewModelScope.launch {
            repository.getTasksDateRange(startMs, endMs)
                .catch { e ->
                    _isLoading.value = false
                    _errorMessage.value = "Không thể tải lịch: ${e.localizedMessage}"
                }
                .collect { tasks ->
                    _isLoading.value = false
                    _tasksForMonth.value = tasks
                    // Nhóm tasks theo ngày
                    val grouped = buildMonthCache(tasks)
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
     * Sau khi đổi tháng, dữ liệu tháng mới được query lại từ Room.
     */
    fun navigateMonth(offset: Int) {
        val cal = Calendar.getInstance().apply {
            // Đặt ngày về 1 trước khi đổi tháng để tránh Calendar tự normalize
            // sai tháng khi hôm nay là ngày 29, 30 hoặc 31.
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

    /** Cập nhật tasksForSelectedDate dựa trên cache mới nhất. */
    private fun refreshSelectedDateTasks(cache: Map<Long, List<Task>>) {
        _tasksForSelectedDate.value = cache[_selectedDate.value] ?: emptyList()
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
