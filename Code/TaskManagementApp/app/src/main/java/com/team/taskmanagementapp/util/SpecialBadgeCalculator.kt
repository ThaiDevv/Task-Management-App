package com.team.taskmanagementapp.util

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.streak.StreakInfo
import java.util.Calendar

data class SpecialBadgeStatus(
    val isComebackKidUnlocked: Boolean = false,
    val isHardWorkerUnlocked: Boolean = false,
    val isEarlyBirdUnlocked: Boolean = false,
    val isNightOwlUnlocked: Boolean = false,
    val isMonthConquerorUnlocked: Boolean = false,
    val isUnstoppableUnlocked: Boolean = false,
    val isUltimateMasterUnlocked: Boolean = false,
    val hardTasksCount: Int = 0,
    val earlyBirdCount: Int = 0,
    val nightOwlCount: Int = 0
) {
    fun unlockedCount(): Int {
        var count = 0
        if (isComebackKidUnlocked) count++
        if (isHardWorkerUnlocked) count++
        if (isEarlyBirdUnlocked) count++
        if (isNightOwlUnlocked) count++
        if (isMonthConquerorUnlocked) count++
        if (isUnstoppableUnlocked) count++
        if (isUltimateMasterUnlocked) count++
        return count
    }
}

/**
 * Evaluates the 7 Special Badges based on task history and streak metrics.
 */
object SpecialBadgeCalculator {

    fun calculateSpecialBadges(allTasks: List<Task>, streakInfo: StreakInfo): SpecialBadgeStatus {
        val completedTasks = allTasks.filter { it.isCompleted || it.status.name == "COMPLETED" }

        // 1. Hard Worker: Complete 10 Hard Tasks (Priority.HIGH or Priority.URGENT)
        val hardTasksCount = completedTasks.count {
            it.priority == Priority.HIGH || it.priority == Priority.URGENT
        }
        val isHardWorker = hardTasksCount >= 10

        // 2 & 3. Early Bird (< 09:00 AM) & Night Owl (>= 09:00 PM / 21:00)
        var earlyBirdCount = 0
        var nightOwlCount = 0
        val cal = Calendar.getInstance()

        completedTasks.forEach { task ->
            val timestamp = task.completedAt ?: task.updatedAt.takeIf { it > 0 } ?: task.dueTime
            if (timestamp > 0) {
                cal.timeInMillis = timestamp
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour < 9) earlyBirdCount++
                if (hour >= 21) nightOwlCount++
            }
        }
        val isEarlyBird = earlyBirdCount >= 50
        val isNightOwl = nightOwlCount >= 50

        // 4. Month Conqueror: at least 1 completed task on every single day of a full calendar month
        val tasksByMonth = completedTasks.groupBy { task ->
            val ts = task.completedAt ?: task.dueDate
            cal.timeInMillis = ts
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }

        var isMonthConqueror = false
        for ((yearMonth, tasksInMonth) in tasksByMonth) {
            val (year, month) = yearMonth
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val uniqueDaysWithTasks = tasksInMonth.map { task ->
                val ts = task.completedAt ?: task.dueDate
                monthCal.timeInMillis = ts
                monthCal.get(Calendar.DAY_OF_MONTH)
            }.toSet()

            if (uniqueDaysWithTasks.size >= daysInMonth) {
                isMonthConqueror = true
                break
            }
        }

        // 5. Unstoppable: 100 tasks within 7 consecutive days
        val tasksByDay = completedTasks.groupBy { task ->
            val ts = task.completedAt ?: task.dueDate
            DateTimeUtils.getStartOfDay(ts)
        }.mapValues { it.value.size }

        val sortedDays = tasksByDay.keys.sorted()
        var isUnstoppable = false

        for (i in sortedDays.indices) {
            val startDay = sortedDays[i]
            val endDay = startDay + 6 * 86400000L // 7-day range
            var windowSum = 0
            for (j in i until sortedDays.size) {
                val currentDay = sortedDays[j]
                if (currentDay <= endDay) {
                    windowSum += tasksByDay[currentDay] ?: 0
                } else {
                    break
                }
            }
            if (windowSum >= 100) {
                isUnstoppable = true
                break
            }
        }

        // 6. Comeback Kid: Lost streak at least once, then reached 7+ day streak
        val currentStreak = streakInfo.currentStreak
        val bestStreak = streakInfo.bestStreak
        val isComebackKid = (bestStreak > currentStreak && currentStreak >= 7) ||
                (bestStreak >= 7 && allTasks.any { !it.isCompleted && DateTimeUtils.getStartOfDay(it.dueDate) < DateTimeUtils.getStartOfDay(System.currentTimeMillis()) }) ||
                (currentStreak >= 7 && bestStreak >= 7 && (allTasks.groupBy { DateTimeUtils.getStartOfDay(it.dueDate) }.size > currentStreak))

        // 7. Ultimate Master: Streak Master (365 days) + Task Master (2,000 tasks)
        val isUltimateMaster = (streakInfo.bestStreak >= 365 || streakInfo.currentStreak >= 365) && completedTasks.size >= 2000

        return SpecialBadgeStatus(
            isComebackKidUnlocked = isComebackKid,
            isHardWorkerUnlocked = isHardWorker,
            isEarlyBirdUnlocked = isEarlyBird,
            isNightOwlUnlocked = isNightOwl,
            isMonthConquerorUnlocked = isMonthConqueror,
            isUnstoppableUnlocked = isUnstoppable,
            isUltimateMasterUnlocked = isUltimateMaster,
            hardTasksCount = hardTasksCount,
            earlyBirdCount = earlyBirdCount,
            nightOwlCount = nightOwlCount
        )
    }
}
