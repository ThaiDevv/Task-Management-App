package com.team.taskmanagementapp.data.model.streak

/**
 * Status of a specific day regarding streak completion.
 */
enum class StreakDayStatus {
    COMPLETED,          // Day had task(s) and all were 100% completed
    FAILED,             // Day had incomplete tasks or no tasks in past
    TODAY_IN_PROGRESS,  // Today has pending tasks or is still in progress
    FUTURE              // Future day, not reached yet
}

/**
 * Information for a single day in the weekly streak bar.
 */
data class DayStreakInfo(
    val dayLabel: String,      // e.g., "M", "T", "W", "T", "F", "S", "S" or "Mon", "Tue"
    val dayOfMonth: Int,       // e.g., 18
    val dateMillis: Long,
    val isToday: Boolean,
    val status: StreakDayStatus
)

/**
 * Overall Streak state exposed to UI.
 */
data class StreakInfo(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isTodayCompleted: Boolean = false,
    val todayTotalTasks: Int = 0,
    val todayCompletedTasks: Int = 0,
    val weekDays: List<DayStreakInfo> = emptyList()
) {
    /**
     * Get user badge title based on current streak count.
     */
    fun getBadgeTitle(): String {
        return when {
            currentStreak >= 365 -> "👑 Master Streak"
            currentStreak >= 200 -> "🏆 Legend Streak"
            currentStreak >= 100 -> "💎 Champion Streak"
            currentStreak >= 50  -> "⚡ Achiever Streak"
            currentStreak >= 30  -> "🔥 Streaker Streak"
            currentStreak >= 7   -> "🌟 Sparkstarter Streak"
            currentStreak >= 3   -> "🌱 Starter Streak"
            else -> "⚡ No Active Streak"
        }
    }
}
