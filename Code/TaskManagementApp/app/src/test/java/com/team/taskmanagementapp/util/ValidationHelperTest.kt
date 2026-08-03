package com.team.taskmanagementapp.util

import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.util.ValidationHelper.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ValidationHelperTest {

    private val now = calendar(2026, Calendar.AUGUST, 3, 10, 30)

    @Test
    fun `title is required and cannot exceed 200 characters`() {
        assertEquals(ValidationError.TITLE_REQUIRED, ValidationHelper.validateTitle("   "))
        assertNull(ValidationHelper.validateTitle("a".repeat(200)))
        assertEquals(
            ValidationError.TITLE_TOO_LONG,
            ValidationHelper.validateTitle("a".repeat(201))
        )
    }

    @Test
    fun `description is optional and cannot exceed 1000 characters`() {
        assertNull(ValidationHelper.validateDescription(""))
        assertNull(ValidationHelper.validateDescription("a".repeat(1_000)))
        assertEquals(
            ValidationError.DESCRIPTION_TOO_LONG,
            ValidationHelper.validateDescription("a".repeat(1_001))
        )
    }

    @Test
    fun `new task requires a due date that is not in the past`() {
        assertEquals(
            ValidationError.DUE_DATE_REQUIRED,
            ValidationHelper.validateDueDate(null, isNewTask = true, nowMillis = now)
        )
        assertEquals(
            ValidationError.DUE_DATE_IN_PAST,
            ValidationHelper.validateDueDate(
                calendar(2026, Calendar.AUGUST, 2, 23, 59),
                isNewTask = true,
                nowMillis = now
            )
        )
        assertNull(
            ValidationHelper.validateDueDate(
                calendar(2026, Calendar.AUGUST, 3, 0, 0),
                isNewTask = true,
                nowMillis = now
            )
        )
    }

    @Test
    fun `edit mode allows an existing past due date`() {
        assertNull(
            ValidationHelper.validateDueDate(
                calendar(2026, Calendar.AUGUST, 2, 10, 0),
                isNewTask = false,
                nowMillis = now
            )
        )
    }

    @Test
    fun `today due time must be in the future`() {
        val today = calendar(2026, Calendar.AUGUST, 3, 0, 0)
        assertEquals(
            ValidationError.DUE_TIME_NOT_IN_FUTURE,
            ValidationHelper.validateDueTime(
                today,
                calendar(2026, Calendar.AUGUST, 3, 10, 30),
                now
            )
        )
        assertNull(
            ValidationHelper.validateDueTime(
                today,
                calendar(2026, Calendar.AUGUST, 3, 10, 31),
                now
            )
        )
    }

    @Test
    fun `future date does not constrain the selected time`() {
        assertNull(
            ValidationHelper.validateDueTime(
                calendar(2026, Calendar.AUGUST, 4, 0, 0),
                calendar(2026, Calendar.AUGUST, 3, 8, 0),
                now
            )
        )
    }

    @Test
    fun `priority is required`() {
        assertEquals(ValidationError.PRIORITY_REQUIRED, ValidationHelper.validatePriority(null))
        assertNull(ValidationHelper.validatePriority(Priority.MEDIUM))
    }

    @Test
    fun `validate all accepts valid data and rejects invalid data`() {
        val dueDate = calendar(2026, Calendar.AUGUST, 3, 0, 0)
        val dueTime = calendar(2026, Calendar.AUGUST, 3, 11, 0)

        assertTrue(
            ValidationHelper.validateAll(
                title = "Submit report",
                description = "",
                dueDateMillis = dueDate,
                dueTimeMillis = dueTime,
                priority = Priority.HIGH,
                isNewTask = true,
                nowMillis = now
            )
        )
        assertFalse(
            ValidationHelper.validateAll(
                title = "",
                description = "",
                dueDateMillis = dueDate,
                dueTimeMillis = dueTime,
                priority = Priority.HIGH,
                isNewTask = true,
                nowMillis = now
            )
        )
    }

    private fun calendar(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
