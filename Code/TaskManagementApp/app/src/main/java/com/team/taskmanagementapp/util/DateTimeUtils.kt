package com.team.taskmanagementapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility object for handling Date and Time formatting, parsing,
 * and calculations throughout the application.
 */
object DateTimeUtils {

    const val FORMAT_DATE_TIME = "dd/MM/yyyy HH:mm"
    const val FORMAT_DATE_ONLY = "dd/MM/yyyy"
    const val FORMAT_TIME_ONLY = "HH:mm"
    const val FORMAT_FULL_DATE = "EEE, dd MMM yyyy"

    /**
     * Format a timestamp in milliseconds to a formatted string.
     */
    fun formatTimestamp(timestamp: Long?, pattern: String = FORMAT_DATE_TIME): String {
        if (timestamp == null || timestamp == 0L) return ""
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Parse a date string back into a timestamp in milliseconds.
     */
    fun parseToTimestamp(dateStr: String, pattern: String = FORMAT_DATE_TIME): Long? {
        if (dateStr.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a task is overdue based on its combined date + time timestamp and completion state.
     */
    fun isOverdue(dueDateTimestamp: Long?, isCompleted: Boolean): Boolean {
        if (dueDateTimestamp == null || dueDateTimestamp == 0L || isCompleted) {
            return false
        }
        return System.currentTimeMillis() > dueDateTimestamp
    }

    /**
     * Check if a Task entity is overdue considering both its dueDate and dueTime.
     */
    fun isOverdue(task: com.team.taskmanagementapp.data.local.entity.Task): Boolean {
        if (task.isCompleted || task.status == com.team.taskmanagementapp.data.model.enums.TaskStatus.COMPLETED) {
            return false
        }
        if (task.status == com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE) {
            return true
        }
        val combined = getCombinedDueTimestamp(task.dueDate, task.dueTime)
        return combined > 0L && combined < System.currentTimeMillis()
    }

    /**
     * Calculates the exact combined target timestamp (Date + Time) for a task.
     */
    fun getCombinedDueTimestamp(dueDateMillis: Long, dueTimeMillis: Long): Long {
        if (dueDateMillis <= 0L) return 0L
        val dateCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val timeCal = Calendar.getInstance().apply { timeInMillis = if (dueTimeMillis > 0L) dueTimeMillis else dueDateMillis }
        return Calendar.getInstance().apply {
            clear()
            set(
                dateCal.get(Calendar.YEAR),
                dateCal.get(Calendar.MONTH),
                dateCal.get(Calendar.DAY_OF_MONTH),
                timeCal.get(Calendar.HOUR_OF_DAY),
                timeCal.get(Calendar.MINUTE),
                59
            )
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Get the start timestamp of today (00:00:00.000).
     */
    fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get the end timestamp of today (23:59:59.999).
     */
    fun getEndOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Format timestamp to friendly relative time string (e.g. "Hôm nay", "Ngày mai", "Hôm qua").
     */
    fun getRelativeTimeString(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Không có hạn"

        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()

        val isSameYear = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        val isSameDay = isSameYear && targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = isSameYear && targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = isSameYear && targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val timeStr = formatTimestamp(timestamp, FORMAT_TIME_ONLY)

        return when {
            isSameDay -> "Hôm nay, $timeStr"
            isTomorrow -> "Ngày mai, $timeStr"
            isYesterday -> "Hôm qua, $timeStr"
            else -> formatTimestamp(timestamp, FORMAT_DATE_TIME)
        }
    }
}
