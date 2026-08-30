package com.team.taskmanagementapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun `trigger combines due date and due time then subtracts reminder`() {
        val dueDate = calendar(2026, Calendar.AUGUST, 14, 0, 0)
        val dueTime = calendar(2026, Calendar.JANUARY, 1, 10, 0)
        val expectedTrigger = calendar(2026, Calendar.AUGUST, 14, 9, 30)

        assertEquals(
            expectedTrigger,
            AlarmScheduler.calculateTriggerAtMillis(
                dueDateMillis = dueDate,
                dueTimeMillis = dueTime,
                reminderMinutes = 30
            )
        )
    }

    @Test
    fun `none reminder does not create a trigger`() {
        assertNull(
            AlarmScheduler.calculateTriggerAtMillis(
                dueDateMillis = calendar(2026, Calendar.AUGUST, 14, 0, 0),
                dueTimeMillis = calendar(2026, Calendar.JANUARY, 1, 10, 0),
                reminderMinutes = -1
            )
        )
    }

    @Test
    fun `on time reminder creates trigger at exact due time`() {
        val dueDate = calendar(2026, Calendar.AUGUST, 14, 0, 0)
        val dueTime = calendar(2026, Calendar.JANUARY, 1, 10, 0)
        val expectedTrigger = calendar(2026, Calendar.AUGUST, 14, 10, 0)

        assertEquals(
            expectedTrigger,
            AlarmScheduler.calculateTriggerAtMillis(
                dueDateMillis = dueDate,
                dueTimeMillis = dueTime,
                reminderMinutes = 0
            )
        )
    }

    @Test
    fun `invalid due date or time does not create a trigger`() {
        assertNull(
            AlarmScheduler.calculateTriggerAtMillis(
                dueDateMillis = 0L,
                dueTimeMillis = calendar(2026, Calendar.JANUARY, 1, 10, 0),
                reminderMinutes = 15
            )
        )
        assertNull(
            AlarmScheduler.calculateTriggerAtMillis(
                dueDateMillis = calendar(2026, Calendar.AUGUST, 14, 0, 0),
                dueTimeMillis = 0L,
                reminderMinutes = 15
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
