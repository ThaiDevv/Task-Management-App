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

        // Group tasks by start of day timestamp based on dueDate
        val tasksByDay = tasks.groupBy { DateTimeUtils.getStartOfDay(it.dueDate) }

        // Evaluate today
        val todayTasks = tasksByDay[startOfToday] ?: emptyList()
        val todayTotal = todayTasks.size
        val todayCompletedCount = todayTasks.count { isTaskCompleted(it) }
        val isTodayCompleted = todayTotal > 0 && todayCompletedCount == todayTotal

        // Calculate continuous streak backwards
        var streakCount = 0

        // Start checking date: if today is 100% completed, include today in streak calculation.
        // If today is not completed yet, today is in-progress and streak is calculated backwards from yesterday.
        var checkCal = Calendar.getInstance().apply {
            timeInMillis = startOfToday
        }

        if (isTodayCompleted) {
            streakCount++
            checkCal.add(Calendar.DAY_OF_MONTH, -1)
        } else {
            checkCal.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Loop backwards through past days
        // We only check back as far as the earliest task date, or max 365 days to prevent infinite loops.
        val earliestTaskDate = tasks.minOfOrNull { DateTimeUtils.getStartOfDay(it.dueDate) }
        if (earliestTaskDate != null) {
            while (checkCal.timeInMillis >= earliestTaskDate) {
                val dayMillis = checkCal.timeInMillis
                val dayTasks = tasksByDay[dayMillis] ?: emptyList()

                // "Những ngày không có task hoặc làm không hoàn thành đủ task trong 1 ngày thì sẽ mất chuỗi"
                if (dayTasks.isEmpty() || !dayTasks.all { isTaskCompleted(it) }) {
                    // Streak is broken
                    break
                }

                streakCount++
                checkCal.add(Calendar.DAY_OF_MONTH, -1)
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
        val weekDays = buildWeekDays(startOfToday, tasksByDay)

        return StreakInfo(
            currentStreak = streakCount,
            bestStreak = bestStreak,
            isTodayCompleted = isTodayCompleted,
            todayTotalTasks = todayTotal,
            todayCompletedTasks = todayCompletedCount,
            weekDays = weekDays
        )
    }

    private fun isTaskCompleted(task: Task): Boolean {
        return task.isCompleted || task.status == TaskStatus.COMPLETED
    }

    private fun buildWeekDays(
        startOfToday: Long,
        tasksByDay: Map<Long, List<Task>>
    ): List<DayStreakInfo> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            // Set to Monday of current week
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val weekDays = mutableListOf<DayStreakInfo>()
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0 until 7) {
            val dayMillis = DateTimeUtils.getStartOfDay(calendar.timeInMillis)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val label = dayLabelFormat.format(calendar.time).take(1).uppercase(Locale.getDefault())

            val status = when {
                dayMillis > startOfToday -> StreakDayStatus.FUTURE
                dayMillis == startOfToday -> {
                    val todayTasks = tasksByDay[dayMillis] ?: emptyList()
                    if (todayTasks.isNotEmpty() && todayTasks.all { isTaskCompleted(it) }) {
                        StreakDayStatus.COMPLETED
                    } else {
                        StreakDayStatus.TODAY_IN_PROGRESS
                    }
                }
                else -> {
                    val dayTasks = tasksByDay[dayMillis] ?: emptyList()
                    if (dayTasks.isNotEmpty() && dayTasks.all { isTaskCompleted(it) }) {
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
