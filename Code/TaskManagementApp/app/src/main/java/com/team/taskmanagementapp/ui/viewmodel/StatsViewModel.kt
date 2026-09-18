package com.team.taskmanagementapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.model.stats.DayProductivity
import com.team.taskmanagementapp.data.model.stats.PomodoroFocusStats
import com.team.taskmanagementapp.data.model.stats.PriorityStats
import com.team.taskmanagementapp.data.model.stats.StatisticsUiState
import com.team.taskmanagementapp.data.model.stats.StatsTimeFilter
import com.team.taskmanagementapp.data.model.stats.TaskFocusStats
import com.team.taskmanagementapp.data.model.stats.WeeklyProductivity
import com.team.taskmanagementapp.data.model.stats.buildTopTaskSummaries
import com.team.taskmanagementapp.data.model.stats.formatFocusDuration
import com.team.taskmanagementapp.data.repository.PomodoroRepository
import com.team.taskmanagementapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel for Statistics Dashboard.
 * Computes weekly productivity, completion rate, deep work focus time,
 * and priority breakdown reactively from TaskRepository.
 *
 * Task 15: "deep work" và các số liệu Pomodoro nay lấy từ dữ liệu thật trong
 * `pomodoro_sessions` (qua [PomodoroRepository]) thay vì ước lượng từ số task hoàn thành.
 */
class StatsViewModel(
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(StatsTimeFilter.THIS_WEEK)
    val timeFilter: StateFlow<StatsTimeFilter> = _timeFilter.asStateFlow()

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    /**
     * Gộp dữ liệu Task và dữ liệu Pomodoro của kỳ đang chọn.
     *
     * Dùng `collectLatest` để khi đổi bộ lọc thời gian thì vòng combine cũ bị huỷ,
     * tránh kết quả của kỳ cũ ghi đè kết quả của kỳ mới.
     */
    private fun observeStats() {
        viewModelScope.launch(dispatcher) {
            _timeFilter.collectLatest { filter ->
                combine(
                    taskRepository.getAllTasks(),
                    observePomodoroStats(filter)
                ) { tasks, pomodoro ->
                    calculateStats(tasks, filter, pomodoro.toFocusStats(tasks))
                }.collect { calculatedState ->
                    _uiState.value = calculatedState
                }
            }
        }
    }

    /**
     * Các Flow số liệu Pomodoro của một kỳ — tái sử dụng đúng các query đã có ở DAO,
     * không thêm logic timer hay thống kê trùng lặp.
     */
    private fun observePomodoroStats(filter: StatsTimeFilter): Flow<RawPomodoroStats> {
        val (periodStart, periodEnd) = getPeriodRange(filter)
        return combine(
            pomodoroRepository.observeTodayFocusMinutes(),
            pomodoroRepository.observeThisWeekFocusMinutes(),
            pomodoroRepository.observeTotalFocusMinutesInRange(periodStart, periodEnd),
            pomodoroRepository.observeFocusSessionCountInRange(periodStart, periodEnd),
            pomodoroRepository.observeFocusStatsByTask(periodStart, periodEnd)
        ) { today, week, period, sessionCount, byTask ->
            RawPomodoroStats(
                todayMinutes = today,
                weekMinutes = week,
                periodMinutes = period,
                periodSessionCount = sessionCount,
                byTask = byTask
            )
        }
    }

    /**
     * Switch time filter (This Week, Last Week, This Month, All Time).
     */
    fun setTimeFilter(filter: StatsTimeFilter) {
        _timeFilter.value = filter
    }

    /**
     * Core calculation logic for all statistics metrics.
     *
     * @param pomodoro số liệu Pomodoro thật của kỳ; mặc định rỗng (không có dữ liệu) nên
     *        hàm vẫn thuần tuý và test được mà không cần DB.
     */
    fun calculateStats(
        allTasks: List<Task>,
        filter: StatsTimeFilter,
        pomodoro: PomodoroFocusStats = PomodoroFocusStats()
    ): StatisticsUiState {
        val (periodStart, periodEnd) = getPeriodRange(filter)

        // Filter tasks that belong to the selected period
        val tasksInPeriod = if (filter == StatsTimeFilter.ALL_TIME) {
            allTasks
        } else {
            allTasks.filter { task ->
                val referenceTime = if (task.isCompleted || task.status == TaskStatus.COMPLETED) {
                    if (task.updatedAt > 0) task.updatedAt else task.dueDate
                } else {
                    task.dueDate
                }
                referenceTime in periodStart..periodEnd
            }
        }

        // 1. Weekly Productivity (7 days of Monday to Sunday)
        val weeklyProductivity = computeWeeklyProductivity(allTasks, filter)

        // 2. Completion Rate
        val totalTasks = tasksInPeriod.size
        val completedTasks = tasksInPeriod.count { it.isCompleted || it.status == TaskStatus.COMPLETED }
        val completionRate = if (totalTasks > 0) {
            (completedTasks * 100) / totalTasks
        } else 0

        val completionRateLabel = when {
            totalTasks == 0 -> "No tasks"
            completionRate >= 85 -> "Excellent"
            completionRate >= 65 -> "On track"
            completionRate >= 40 -> "In progress"
            else -> "Needs attention"
        }

        // 3. Stats Cards: Completed & Deep Work Hours
        val completedSubtitle = when (filter) {
            StatsTimeFilter.THIS_WEEK -> "This week"
            StatsTimeFilter.LAST_WEEK -> "Last week"
            StatsTimeFilter.THIS_MONTH -> "This month"
            StatsTimeFilter.ALL_TIME -> "All time"
        }

        // Deep work = thời gian tập trung THẬT của kỳ (từ các phiên FOCUS đã hoàn thành).
        // Trước Task 15 chỗ này là ước lượng giả `completedTasks * 0.43h`.
        val deepWorkLabel = formatFocusDuration(pomodoro.periodMinutes)
        val deepWorkSubtitle = if (pomodoro.hasPeriodFocus) {
            "Focused time"
        } else {
            "No focus sessions yet"
        }

        // 4. Tasks by Priority
        val highCount = tasksInPeriod.count { it.priority == Priority.HIGH }
        val mediumCount = tasksInPeriod.count { it.priority == Priority.MEDIUM }
        val lowCount = tasksInPeriod.count { it.priority == Priority.LOW }

        val highPercent = if (totalTasks > 0) highCount.toFloat() / totalTasks else 0f
        val mediumPercent = if (totalTasks > 0) mediumCount.toFloat() / totalTasks else 0f
        val lowPercent = if (totalTasks > 0) lowCount.toFloat() / totalTasks else 0f

        val priorityStats = PriorityStats(
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            totalCount = totalTasks,
            highPercent = highPercent,
            mediumPercent = mediumPercent,
            lowPercent = lowPercent
        )

        return StatisticsUiState(
            timeFilter = filter,
            weeklyProductivity = weeklyProductivity,
            completionRate = completionRate,
            completionRateLabel = completionRateLabel,
            completedCount = completedTasks,
            completedSubtitle = completedSubtitle,
            deepWorkHours = deepWorkLabel,
            deepWorkSubtitle = deepWorkSubtitle,
            priorityStats = priorityStats,
            pomodoro = pomodoro,
            isLoading = false
        )
    }

    /**
     * Compute completed tasks for Monday through Sunday.
     */
    private fun computeWeeklyProductivity(
        allTasks: List<Task>,
        filter: StatsTimeFilter
    ): WeeklyProductivity {
        val calendar = Calendar.getInstance(Locale.getDefault())

        if (filter == StatsTimeFilter.LAST_WEEK) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }

        // Set to Monday of the target week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayProductivity>()

        for (dayName in dayNames) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = calendar.timeInMillis - 1

            val count = allTasks.count { task ->
                val isDone = task.isCompleted || task.status == TaskStatus.COMPLETED
                if (!isDone) return@count false
                val completedTime = if (task.updatedAt > 0) task.updatedAt else task.dueDate
                completedTime in dayStart..dayEnd
            }

            days.add(DayProductivity(dayName = dayName, dateMillis = dayStart, completedCount = count))
        }

        val highest = days.maxOfOrNull { it.completedCount } ?: 0
        // Scale to nearest multiple of 4, with minimum 12 as per design
        val maxScale = when {
            highest <= 12 -> 12
            highest % 4 == 0 -> highest
            else -> ((highest / 4) + 1) * 4
        }

        return WeeklyProductivity(days = days, maxCount = maxScale)
    }

    /**
     * Determine [start, end] timestamp bounds for a given filter.
     */
    private fun getPeriodRange(filter: StatsTimeFilter): Pair<Long, Long> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        return when (filter) {
            StatsTimeFilter.THIS_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 7)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            StatsTimeFilter.LAST_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 7)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            StatsTimeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            StatsTimeFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    /**
     * Số liệu thô của DAO cho một kỳ, trước khi ghép tên task.
     */
    private data class RawPomodoroStats(
        val todayMinutes: Int,
        val weekMinutes: Int,
        val periodMinutes: Int,
        val periodSessionCount: Int,
        val byTask: List<TaskFocusStats>
    ) {
        fun toFocusStats(tasks: List<Task>): PomodoroFocusStats = PomodoroFocusStats(
            todayMinutes = todayMinutes,
            weekMinutes = weekMinutes,
            periodMinutes = periodMinutes,
            periodSessionCount = periodSessionCount,
            topTasks = buildTopTaskSummaries(byTask, tasks)
        )
    }
}

/**
 * Factory for creating StatsViewModel instances with TaskRepository + PomodoroRepository.
 */
class StatsViewModelFactory(
    private val repository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            return StatsViewModel(repository, pomodoroRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
