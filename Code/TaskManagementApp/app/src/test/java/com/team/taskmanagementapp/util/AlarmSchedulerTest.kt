package com.team.taskmanagementapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun `past reminder is skipped before platform scheduling`() {
        assertEquals(
            AlarmScheduler.ScheduleDecision.SKIP,
            AlarmScheduler.scheduleDecision(
                taskId = 1,
                isCompleted = false,
                triggerAtMillis = 999L,
                nowMillis = 1_000L,
                notificationsEnabled = true,
                exactAlarmAvailable = true
            )
        )
    }

    @Test
    fun `work fallback is selected when exact alarms are unavailable`() {
        assertEquals(
            AlarmScheduler.ScheduleDecision.FALLBACK,
            AlarmScheduler.scheduleDecision(
                taskId = 1,
                isCompleted = false,
                triggerAtMillis = 2_000L,
                nowMillis = 1_000L,
                notificationsEnabled = true,
                exactAlarmAvailable = false
            )
        )
    }

    @Test
    fun `disabled notifications prevent reminder scheduling`() {
        assertEquals(
            AlarmScheduler.ScheduleDecision.NOTIFICATIONS_DISABLED,
            AlarmScheduler.scheduleDecision(
                taskId = 1,
                isCompleted = false,
                triggerAtMillis = 2_000L,
                nowMillis = 1_000L,
                notificationsEnabled = false,
                exactAlarmAvailable = true
            )
        )
    }

    @Test
    fun `exact scheduling is selected only when access is available`() {
        assertEquals(
            AlarmScheduler.ScheduleDecision.EXACT,
            AlarmScheduler.scheduleDecision(
                taskId = 1,
                isCompleted = false,
                triggerAtMillis = 2_000L,
                nowMillis = 1_000L,
                notificationsEnabled = true,
                exactAlarmAvailable = true
            )
        )
    }

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
