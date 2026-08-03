package com.team.taskmanagementapp.util

import androidx.annotation.StringRes
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.model.enums.Priority
import java.util.Calendar

object ValidationHelper {

    const val TITLE_MAX_LENGTH = 200
    const val DESCRIPTION_MAX_LENGTH = 1_000

    enum class ValidationError(@StringRes val messageRes: Int) {
        TITLE_REQUIRED(R.string.validation_title_required),
        TITLE_TOO_LONG(R.string.validation_title_too_long),
        DESCRIPTION_TOO_LONG(R.string.validation_description_too_long),
        DUE_DATE_REQUIRED(R.string.validation_due_date_required),
        DUE_DATE_IN_PAST(R.string.validation_due_date_in_past),
        DUE_TIME_NOT_IN_FUTURE(R.string.validation_due_time_not_in_future),
        PRIORITY_REQUIRED(R.string.validation_priority_required)
    }

    fun validateTitle(title: String): ValidationError? = when {
        title.isBlank() -> ValidationError.TITLE_REQUIRED
        title.length > TITLE_MAX_LENGTH -> ValidationError.TITLE_TOO_LONG
        else -> null
    }

    fun validateDescription(description: String): ValidationError? =
        if (description.length > DESCRIPTION_MAX_LENGTH) {
            ValidationError.DESCRIPTION_TOO_LONG
        } else {
            null
        }

    fun validateDueDate(
        dueDateMillis: Long?,
        isNewTask: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): ValidationError? {
        if (dueDateMillis == null || dueDateMillis <= 0L) {
            return ValidationError.DUE_DATE_REQUIRED
        }

        if (!isNewTask) return null

        val dueDay = startOfDay(dueDateMillis)
        val today = startOfDay(nowMillis)
        return if (dueDay < today) ValidationError.DUE_DATE_IN_PAST else null
    }

    fun validateDueTime(
        dueDateMillis: Long?,
        dueTimeMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): ValidationError? {
        if (dueDateMillis == null || dueTimeMillis == null) return null
        if (!isSameDay(dueDateMillis, nowMillis)) return null

        val dueDate = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val dueTime = Calendar.getInstance().apply { timeInMillis = dueTimeMillis }
        val combinedDueDateTime = Calendar.getInstance().apply {
            clear()
            set(
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DAY_OF_MONTH),
                dueTime.get(Calendar.HOUR_OF_DAY),
                dueTime.get(Calendar.MINUTE),
                0
            )
            set(Calendar.MILLISECOND, 0)
        }

        return if (combinedDueDateTime.timeInMillis <= nowMillis) {
            ValidationError.DUE_TIME_NOT_IN_FUTURE
        } else {
            null
        }
    }

    fun validatePriority(priority: Priority?): ValidationError? =
        if (priority == null) ValidationError.PRIORITY_REQUIRED else null

    fun validateAll(
        title: String,
        description: String,
        dueDateMillis: Long?,
        dueTimeMillis: Long?,
        priority: Priority?,
        isNewTask: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = listOf(
        validateTitle(title),
        validateDescription(description),
        validateDueDate(dueDateMillis, isNewTask, nowMillis),
        validateDueTime(dueDateMillis, dueTimeMillis, nowMillis),
        validatePriority(priority)
    ).all { it == null }

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
            first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
