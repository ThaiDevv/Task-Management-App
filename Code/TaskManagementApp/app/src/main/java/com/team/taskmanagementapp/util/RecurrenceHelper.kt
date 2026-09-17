package com.team.taskmanagementapp.util

import android.content.Context
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

/**
 * Helper object for calculating next recurrence due dates using java.time.LocalDate,
 * ensuring robust edge case handling (e.g. end-of-month / 31st adjustments).
 */
object RecurrenceHelper {

    /**
     * Calculates the next due timestamp based on recurrence type and interval.
     */
    fun calculateNextDueDate(dueDateMillis: Long, type: RecurrenceType, interval: Int = 1): Long {
        if (dueDateMillis <= 0L || type == RecurrenceType.NONE) return dueDateMillis

        val zoneId = ZoneId.systemDefault()
        val currentLocalDate = Instant.ofEpochMilli(dueDateMillis)
            .atZone(zoneId)
            .toLocalDate()

        val nextLocalDate = when (type) {
            RecurrenceType.DAILY -> currentLocalDate.plusDays(interval.toLong())
            RecurrenceType.WEEKLY -> currentLocalDate.plusWeeks(interval.toLong())
            RecurrenceType.MONTHLY -> currentLocalDate.plusMonths(interval.toLong())
            RecurrenceType.YEARLY -> currentLocalDate.plusYears(interval.toLong())
            RecurrenceType.NONE -> currentLocalDate
        }

        // Convert back to millis preserving original time of day if needed, or start of day
        val calendar = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val milli = calendar.get(Calendar.MILLISECOND)

        return nextLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli().let { baseMillis ->
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = baseMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
                set(Calendar.MILLISECOND, milli)
            }
            targetCal.timeInMillis
        }
    }

    /**
     * Calculates the calculated end date based on starting date, recurrence type, and occurrence count.
     */
    fun calculateEndDateFromOccurrences(
        startDateMillis: Long,
        type: RecurrenceType,
        count: Int,
        interval: Int = 1
    ): Long {
        if (startDateMillis <= 0L || type == RecurrenceType.NONE || count <= 0) return 0L

        val zoneId = ZoneId.systemDefault()
        val startLocalDate = Instant.ofEpochMilli(startDateMillis)
            .atZone(zoneId)
            .toLocalDate()

        val totalSteps = (count * interval).toLong()
        val targetLocalDate = when (type) {
            RecurrenceType.DAILY -> startLocalDate.plusDays(totalSteps)
            RecurrenceType.WEEKLY -> startLocalDate.plusWeeks(totalSteps)
            RecurrenceType.MONTHLY -> startLocalDate.plusMonths(totalSteps)
            RecurrenceType.YEARLY -> startLocalDate.plusYears(totalSteps)
            RecurrenceType.NONE -> startLocalDate
        }

        return targetLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Checks if a recurring task instance has ended based on its end date or max count limit.
     */
    fun isRecurrenceEnded(task: Task, nextDueDateMillis: Long): Boolean {
        if (!task.isRecurring || task.recurrenceType == RecurrenceType.NONE) return true
        if (task.isPaused) return true

        if (task.repeatEndDate > 0L && nextDueDateMillis > task.repeatEndDate) {
            return true
        }

        if (task.repeatLimitCount > 0 && task.currentOccurrence >= task.repeatLimitCount) {
            return true
        }

        return false
    }

    /**
     * Get user-friendly recurrence text description.
     */
    fun getRecurrenceDisplayText(type: RecurrenceType, context: Context): String {
        return when (type) {
            RecurrenceType.DAILY -> context.getString(R.string.task_detail_recurrence_daily)
            RecurrenceType.WEEKLY -> context.getString(R.string.task_detail_recurrence_weekly)
            RecurrenceType.MONTHLY -> context.getString(R.string.task_detail_recurrence_monthly)
            RecurrenceType.YEARLY -> context.getString(R.string.task_detail_recurrence_yearly)
            RecurrenceType.NONE -> context.getString(R.string.task_detail_recurrence_none)
        }
    }
}
