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
    fun getBadgeTitle(): String = getBadgeTitle(currentStreak)

    /**
     * Thêm hiện badge dùng cho màn Rewards khi xét theo max(currentStreak, bestStreak)
     * để danh hiệu khớp với badge đã mở.
     */
    fun getBadgeTitle(streak: Int): String {
        val badge = when {
            streak >= 365 -> "👑 Master Streak"
            streak >= 200 -> "🏆 Legend Streak"
            streak >= 100 -> "💎 Champion Streak"
            streak >= 50  -> "⚡ Achiever Streak"
            streak >= 30  -> "🔥 Streaker Streak"
            streak >= 7   -> "🌟 Sparkstarter Streak"
            streak >= 3   -> "🌱 Starter Streak"
            else -> "🔒 None Badge"
        }
        return "Streak: $badge"
    }

    /**
     * Danh hiệu cao nhất theo TỔNG số task đã hoàn thành.
     * Cùng cơ chế "badge cao hơn thay thế badge cũ" như [getBadgeTitle]:
     * chỉ trả về đúng 1 danh hiệu tại mốc cao nhất đã đạt được.
     */
    fun getTaskBadgeTitle(completedTasks: Int): String {
        val badge = when {
            completedTasks >= 2000 -> "👑 Task Master"
            completedTasks >= 1000 -> "🏆 Task Champion"
            completedTasks >= 500  -> "💎 Task Expert"
            completedTasks >= 250  -> "🎯 Task Executor"
            completedTasks >= 100  -> "⚡ Task Achiever"
            completedTasks >= 50   -> "📝 Task Doer"
            completedTasks >= 10   -> "🌱 Task Novice"
            else -> "🔒 None Badge"
        }
        return "Task Completed: $badge"
    }
}
