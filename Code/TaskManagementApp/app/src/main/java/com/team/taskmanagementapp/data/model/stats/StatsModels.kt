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
    val lowPercent: Float
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
    val deepWorkHours: String = "0h",
    val deepWorkSubtitle: String = "Focused time",
    val priorityStats: PriorityStats = PriorityStats(0, 0, 0, 0, 0f, 0f, 0f),
    val isLoading: Boolean = false
)
