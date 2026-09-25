package com.team.taskmanagementapp.util

import android.content.Context
import android.content.SharedPreferences
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.model.streak.DayStreakInfo
import com.team.taskmanagementapp.data.model.streak.StreakDayStatus
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Utility to calculate task streaks and manage streak milestone history.
 */
object StreakCalculator {

    private const val PREF_NAME = "task_streak_prefs"
    private const val KEY_BEST_STREAK = "key_best_streak"

    fun calculateStreak(
        tasks: List<Task>,
        context: Context? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): StreakInfo {
        val startOfToday = DateTimeUtils.getStartOfDay(currentTimeMillis)

        // Today evaluation
        val todayDueTasks = tasks.filter { DateTimeUtils.getStartOfDay(it.dueDate) == startOfToday }
        val todayCompletedTasks = tasks.filter { isTaskCompleted(it) && getCompletionDay(it) == startOfToday }

        val todayTotal = if (todayDueTasks.isNotEmpty()) todayDueTasks.size else todayCompletedTasks.size
        val todayCompletedCount = if (todayDueTasks.isNotEmpty()) {
            todayDueTasks.count { isTaskCompleted(it) }
        } else {
            todayCompletedTasks.size
        }

        val isTodayCompleted = if (todayDueTasks.isNotEmpty()) {
            todayDueTasks.all { isTaskCompleted(it) } && todayCompletedCount > 0
        } else {
            todayCompletedTasks.isNotEmpty()
        }

        // Calculate continuous streak backwards
        var streakCount: Int

        val earliestDate = tasks.minOfOrNull {
            minOf(
                if (it.dueDate > 0) DateTimeUtils.getStartOfDay(it.dueDate) else Long.MAX_VALUE,
                if (isTaskCompleted(it)) getCompletionDay(it) else Long.MAX_VALUE
            )
        }?.takeIf { it != Long.MAX_VALUE }

        if (isTodayCompleted) {
            streakCount = 1
            var checkDay = getPreviousDay(startOfToday)
            if (earliestDate != null) {
                while (checkDay >= earliestDate) {
                    if (!isPastDayCompleted(checkDay, tasks)) {
                        break
                    }
                    streakCount++
                    checkDay = getPreviousDay(checkDay)
                }
            }
        } else {
            // Today is not completed yet (in-progress / 0 tasks completed today).
            // Check if yesterday was completed.
            val yesterday = getPreviousDay(startOfToday)
            if (isPastDayCompleted(yesterday, tasks)) {
                // Yesterday was completed -> maintain streak from yesterday (giữ chuỗi)
                streakCount = 1
                var checkDay = getPreviousDay(yesterday)
                if (earliestDate != null) {
                    while (checkDay >= earliestDate) {
                        if (!isPastDayCompleted(checkDay, tasks)) {
                            break
                        }
                        streakCount++
                        checkDay = getPreviousDay(checkDay)
                    }
                }
            } else {
                // Yesterday was NOT completed (mất chuỗi qua 12h đêm) and today has no completed task -> reset to 0
                streakCount = 0
            }
        }

        // Handle Best Streak persistence if context is available
        var bestStreak = streakCount
        if (context != null) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val savedBest = prefs.getInt(KEY_BEST_STREAK, 0)
            if (streakCount > savedBest) {
                bestStreak = streakCount
                prefs.edit().putInt(KEY_BEST_STREAK, streakCount).apply()
            } else {
                bestStreak = savedBest
            }
        }

        // Build 7-day week overview (Monday -> Sunday of current week)
        val weekDays = buildWeekDays(startOfToday, tasks)

        return StreakInfo(
            currentStreak = streakCount,
            bestStreak = bestStreak,
            isTodayCompleted = isTodayCompleted,
            todayTotalTasks = todayTotal,
            todayCompletedTasks = todayCompletedCount,
            weekDays = weekDays
        )
    }

    private fun getCompletionDay(task: Task): Long {
        val ts = task.completedAt?.takeIf { it > 0 }
            ?: task.dueDate.takeIf { it > 0 }
            ?: task.updatedAt
        return DateTimeUtils.getStartOfDay(ts)
    }

    private fun isTaskCompleted(task: Task): Boolean {
        return task.isCompleted || task.status == TaskStatus.COMPLETED
    }

    private fun isPastDayCompleted(
        dayMillis: Long,
        tasks: List<Task>
    ): Boolean {
        val tasksDueOnDay = tasks.filter { DateTimeUtils.getStartOfDay(it.dueDate) == dayMillis }
        val tasksCompletedOnDay = tasks.filter { isTaskCompleted(it) && getCompletionDay(it) == dayMillis }

        return if (tasksDueOnDay.isNotEmpty()) {
            // All tasks due on this day must be completed AND not completed on a later day
            tasksDueOnDay.all { task ->
                if (!isTaskCompleted(task)) return@all false
                val completedDate = task.completedAt?.takeIf { it > 0 }?.let { DateTimeUtils.getStartOfDay(it) }
                completedDate == null || completedDate <= dayMillis
            }
        } else {
            // If no tasks were specifically due on this day, day is completed if at least 1 task was completed on this day
            tasksCompletedOnDay.isNotEmpty()
        }
    }

    private fun getPreviousDay(dayMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return DateTimeUtils.getStartOfDay(cal.timeInMillis)
    }

    private fun buildWeekDays(
        startOfToday: Long,
        tasks: List<Task>
    ): List<DayStreakInfo> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            // Set to Monday of current week
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val weekDays = mutableListOf<DayStreakInfo>()
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val todayDueTasks = tasks.filter { DateTimeUtils.getStartOfDay(it.dueDate) == startOfToday }
        val todayCompletedTasks = tasks.filter { isTaskCompleted(it) && getCompletionDay(it) == startOfToday }
        val isTodayDone = if (todayDueTasks.isNotEmpty()) {
            todayDueTasks.all { isTaskCompleted(it) } && todayCompletedTasks.isNotEmpty()
        } else {
            todayCompletedTasks.isNotEmpty()
        }

        for (i in 0 until 7) {
            val dayMillis = DateTimeUtils.getStartOfDay(calendar.timeInMillis)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val label = dayLabelFormat.format(calendar.time).take(1).uppercase(Locale.getDefault())

            val status = when {
                dayMillis > startOfToday -> StreakDayStatus.FUTURE
                dayMillis == startOfToday -> {
                    if (isTodayDone) {
                        StreakDayStatus.COMPLETED
                    } else {
                        StreakDayStatus.TODAY_IN_PROGRESS
                    }
                }
                else -> {
                    if (isPastDayCompleted(dayMillis, tasks)) {
                        StreakDayStatus.COMPLETED
                    } else {
                        StreakDayStatus.FAILED
                    }
                }
            }

            weekDays.add(
                DayStreakInfo(
                    dayLabel = label,
                    dayOfMonth = dayOfMonth,
                    dateMillis = dayMillis,
                    isToday = (dayMillis == startOfToday),
                    status = status
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return weekDays
    }

    /** Helper for unit testing or resetting best streak if needed. */
    fun clearBestStreak(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BEST_STREAK).apply()
    }
}
