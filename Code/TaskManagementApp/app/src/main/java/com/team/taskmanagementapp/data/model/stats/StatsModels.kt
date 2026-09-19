package com.team.taskmanagementapp.data.model.stats

/**
 * Time filter options for statistics dashboard.
 */
enum class StatsTimeFilter {
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    ALL_TIME
}

/**
 * Task completion productivity for a single day.
 */
data class DayProductivity(
    val dayName: String, // e.g. "Mon", "Tue", etc.
    val dateMillis: Long,
    val completedCount: Int
)

/**
 * Weekly productivity chart data consisting of 7 days.
 */
data class WeeklyProductivity(
    val days: List<DayProductivity>,
    val maxCount: Int
)

/**
 * Task breakdown by priority.
 */
data class PriorityStats(
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val totalCount: Int,
    val highPercent: Float,
    val mediumPercent: Float,
    val lowPercent: Float,
    val urgentCount: Int = 0,
    val urgentPercent: Float = 0f
)

/**
 * UI State for Statistics Screen (Bento Grid).
 */
data class StatisticsUiState(
    val timeFilter: StatsTimeFilter = StatsTimeFilter.THIS_WEEK,
    val weeklyProductivity: WeeklyProductivity = WeeklyProductivity(emptyList(), 0),
    val completionRate: Int = 0,
    val completionRateLabel: String = "On track",
    val completedCount: Int = 0,
    val completedSubtitle: String = "This week",
    /**
     * Thời gian tập trung THẬT của kỳ đang chọn, đã định dạng (`45m`, `2h 5m`).
     *
     * ⚠️ Trước Task 15 giá trị này là ước lượng giả (`số task hoàn thành × 0.43h`);
     * nay lấy trực tiếp từ các phiên FOCUS đã hoàn thành trong `pomodoro_sessions`.
     */
    val deepWorkHours: String = "0m",
    val deepWorkSubtitle: String = "Focused time",
    val pendingCount: Int = 0,
    val hasUnknownCompletionDates: Boolean = false,
    val priorityStats: PriorityStats = PriorityStats(0, 0, 0, 0, 0f, 0f, 0f),
    /** Số liệu Pomodoro thật (hôm nay / tuần này / theo task) — Task 15. */
    val pomodoro: PomodoroFocusStats = PomodoroFocusStats(),
    val isLoading: Boolean = false
)
