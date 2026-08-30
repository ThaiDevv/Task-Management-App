package com.team.taskmanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
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
 * - Cache Map<startOfDay: Long, List<Task>> cho tháng hiện tại (tự động cập nhật các Recurring Tasks vào Schedule)
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

    // Toàn bộ task thuộc tháng đang hiển thị (bao gồm cả task lặp lại được chiếu vào tháng)
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
     * Tự động phản ứng với bất kỳ thay đổi thêm/sửa/xóa task nào trong DB,
     * và tự động cập nhật các Recurring Tasks vào toàn bộ các ngày tương ứng trong tháng.
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

                    // Nhóm tasks theo ngày trong tháng, tích hợp chiếu các task lặp lại (Recurrence)
                    val grouped = buildMonthCacheWithRecurrence(allTasks, year, month)
                    _monthCache.value = grouped
                    _datesWithTasks.value = grouped.keys

                    val allMonthTasks = grouped.values.flatten().distinctBy { it.id to it.dueDate }
                    _tasksForMonth.value = allMonthTasks

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
     * Xây dựng cache cho tháng [year]/[month]:
     * - Đưa task gốc vào đúng ngày dueDate nếu nằm trong tháng.
     * - Tự động chiếu (project) các công việc lặp lại (DAILY, WEEKLY, MONTHLY)
     *   vào các ngày tiếp theo trong tháng theo đúng quy luật chu kỳ.
     */
    private fun buildMonthCacheWithRecurrence(
        allTasks: List<Task>,
        year: Int,
        month: Int
    ): Map<Long, List<Task>> {
        val result = mutableMapOf<Long, MutableList<Task>>()

        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (task in allTasks) {
            if (task.dueDate <= 0L) continue

            val taskDate = startOfDay(task.dueDate)
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }

            // 1. Nếu task gốc có dueDate thuộc tháng này
            if (taskCal.get(Calendar.YEAR) == year && taskCal.get(Calendar.MONTH) == month) {
                result.getOrPut(taskDate) { mutableListOf() }.add(task)
            }

            // 2. Nếu là Recurring Task chưa hoàn thành -> Chiếu vào toàn bộ các ngày lặp lại trong tháng
            if (!task.isCompleted && (task.isRecurring || task.recurrenceType != RecurrenceType.NONE)) {
                val taskStartDay = startOfDay(task.dueDate)
                val startDayOfWeek = taskCal.get(Calendar.DAY_OF_WEEK)
                val startDayOfMonth = taskCal.get(Calendar.DAY_OF_MONTH)

                for (day in 1..maxDays) {
                    tempCal.set(Calendar.DAY_OF_MONTH, day)
                    val currentDayMillis = startOfDay(tempCal)

                    // Chỉ chiếu vào các ngày >= ngày bắt đầu của task
                    if (currentDayMillis <= taskStartDay) continue

                    val isMatch = when (task.recurrenceType) {
                        RecurrenceType.DAILY -> true
                        RecurrenceType.WEEKLY -> tempCal.get(Calendar.DAY_OF_WEEK) == startDayOfWeek
                        RecurrenceType.MONTHLY -> {
                            val maxDaysInThisMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val targetDay = minOf(startDayOfMonth, maxDaysInThisMonth)
                            tempCal.get(Calendar.DAY_OF_MONTH) == targetDay
                        }
                        RecurrenceType.NONE -> false
                    }

                    if (isMatch) {
                        val dayList = result.getOrPut(currentDayMillis) { mutableListOf() }
                        // Kiểm tra không thêm trùng lặp theo title
                        val alreadyExists = dayList.any { it.title.trim().equals(task.title.trim(), ignoreCase = true) }
                        if (!alreadyExists) {
                            val projectedTime = if (task.dueTime > 0L) {
                                val timeCal = Calendar.getInstance().apply { timeInMillis = task.dueTime }
                                val h = timeCal.get(Calendar.HOUR_OF_DAY)
                                val m = timeCal.get(Calendar.MINUTE)
                                val s = timeCal.get(Calendar.SECOND)
                                val ms = timeCal.get(Calendar.MILLISECOND)
                                Calendar.getInstance().apply {
                                    timeInMillis = currentDayMillis
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                    set(Calendar.SECOND, s)
                                    set(Calendar.MILLISECOND, ms)
                                }.timeInMillis
                            } else 0L

                            dayList.add(
                                task.copy(
                                    dueDate = currentDayMillis,
                                    dueTime = projectedTime
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sắp xếp các task trong từng ngày theo thứ tự: Chưa hoàn thành trước, sau đó theo giờ và mức độ ưu tiên
        return result.mapValues { (_, taskList) ->
            taskList.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { if (it.dueTime > 0L) it.dueTime else Long.MAX_VALUE }
                    .thenBy { it.priority.ordinal }
            )
        }
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
