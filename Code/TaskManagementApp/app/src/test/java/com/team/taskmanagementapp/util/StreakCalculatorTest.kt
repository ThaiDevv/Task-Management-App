package com.team.taskmanagementapp.util

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.model.streak.StreakDayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakCalculatorTest {

    private fun createSampleTask(
        id: Int,
        dueDateMillis: Long,
        isCompleted: Boolean,
        status: TaskStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.TODO
    ): Task {
        return Task(
            id = id,
            title = "Task $id",
            description = "Description $id",
            dueDate = dueDateMillis,
            dueTime = dueDateMillis,
            priority = Priority.MEDIUM,
            status = status,
            isCompleted = isCompleted
        )
    }

    private fun getMillisForDaysAgo(daysAgo: Int, baseMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_MONTH, -daysAgo)
        }
        return cal.timeInMillis
    }

    @Test
    fun `calculateStreak with empty tasks returns zero streak`() {
        val now = System.currentTimeMillis()
        val result = StreakCalculator.calculateStreak(emptyList(), currentTimeMillis = now)

        assertEquals(0, result.currentStreak)
        assertFalse(result.isTodayCompleted)
        assertEquals(0, result.todayTotalTasks)
    }

    @Test
    fun `calculateStreak with today completed task returns 1 streak`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = true)

        val result = StreakCalculator.calculateStreak(listOf(todayTask), currentTimeMillis = now)

        assertEquals(1, result.currentStreak)
        assertTrue(result.isTodayCompleted)
        assertEquals(1, result.todayTotalTasks)
        assertEquals(1, result.todayCompletedTasks)
    }

    @Test
    fun `calculateStreak with 3 consecutive fully completed days returns 3 streak`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = true)
        val yesterdayTask = createSampleTask(2, getMillisForDaysAgo(1, now), isCompleted = true)
        val day2AgoTask = createSampleTask(3, getMillisForDaysAgo(2, now), isCompleted = true)

        val result = StreakCalculator.calculateStreak(
            listOf(todayTask, yesterdayTask, day2AgoTask),
            currentTimeMillis = now
        )

        assertEquals(3, result.currentStreak)
        assertTrue(result.isTodayCompleted)
    }

    @Test
    fun `calculateStreak with yesterday and day 2 ago completed but today in progress returns 2 streak`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = false) // today pending
        val yesterdayTask = createSampleTask(2, getMillisForDaysAgo(1, now), isCompleted = true)
        val day2AgoTask = createSampleTask(3, getMillisForDaysAgo(2, now), isCompleted = true)

        val result = StreakCalculator.calculateStreak(
            listOf(todayTask, yesterdayTask, day2AgoTask),
            currentTimeMillis = now
        )

        assertEquals(2, result.currentStreak)
        assertFalse(result.isTodayCompleted)
    }

    @Test
    fun `calculateStreak breaks when a past day has zero tasks`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = true)
        // yesterday has NO task (missing day)
        val day2AgoTask = createSampleTask(3, getMillisForDaysAgo(2, now), isCompleted = true)

        val result = StreakCalculator.calculateStreak(
            listOf(todayTask, day2AgoTask),
            currentTimeMillis = now
        )

        // Yesterday was empty, so streak broken at yesterday. Only today counts (1 day streak).
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `calculateStreak breaks when a past day has incomplete task`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = true)
        val yesterdayTask1 = createSampleTask(2, getMillisForDaysAgo(1, now), isCompleted = true)
        val yesterdayTask2 = createSampleTask(3, getMillisForDaysAgo(1, now), isCompleted = false) // incomplete!

        val result = StreakCalculator.calculateStreak(
            listOf(todayTask, yesterdayTask1, yesterdayTask2),
            currentTimeMillis = now
        )

        // Yesterday had an incomplete task, so streak broken at yesterday. Only today counts.
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `calculateStreak resets to 0 when streak was lost yesterday and today has no completed tasks`() {
        val now = System.currentTimeMillis()
        val day2AgoTask = createSampleTask(1, getMillisForDaysAgo(2, now), isCompleted = true)
        // Yesterday had an incomplete task (lost streak yesterday)
        val yesterdayIncompleteTask = createSampleTask(2, getMillisForDaysAgo(1, now), isCompleted = false)
        // Today has 1 pending task (not completed yet)
        val todayPendingTask = createSampleTask(3, now, isCompleted = false)

        val result = StreakCalculator.calculateStreak(
            listOf(day2AgoTask, yesterdayIncompleteTask, todayPendingTask),
            currentTimeMillis = now
        )

        assertEquals(0, result.currentStreak)
        assertFalse(result.isTodayCompleted)
    }

    @Test
    fun `calculateStreak resets to 0 when past midnight and yesterday had no tasks at all`() {
        val now = System.currentTimeMillis()
        val day2AgoTask = createSampleTask(1, getMillisForDaysAgo(2, now), isCompleted = true)
        // Yesterday had 0 tasks (empty day -> streak lost yesterday)
        // Today has 0 completed tasks
        val todayPendingTask = createSampleTask(2, now, isCompleted = false)

        val result = StreakCalculator.calculateStreak(
            listOf(day2AgoTask, todayPendingTask),
            currentTimeMillis = now
        )

        assertEquals(0, result.currentStreak)
        assertFalse(result.isTodayCompleted)
    }

    @Test
    fun `calculateStreak starts new streak of 1 when completing task today after streak was lost yesterday`() {
        val now = System.currentTimeMillis()
        val day2AgoTask = createSampleTask(1, getMillisForDaysAgo(2, now), isCompleted = true)
        // Yesterday had no tasks (streak lost)
        // Today user completes a task
        val todayCompletedTask = createSampleTask(2, now, isCompleted = true)

        val result = StreakCalculator.calculateStreak(
            listOf(day2AgoTask, todayCompletedTask),
            currentTimeMillis = now
        )

        assertEquals(1, result.currentStreak)
        assertTrue(result.isTodayCompleted)
    }

    @Test
    fun `calculateStreak generates 7 week days with correct statuses`() {
        val now = System.currentTimeMillis()
        val todayTask = createSampleTask(1, now, isCompleted = true)

        val result = StreakCalculator.calculateStreak(listOf(todayTask), currentTimeMillis = now)

        assertEquals(7, result.weekDays.size)
        val todayItem = result.weekDays.find { it.isToday }
        assertTrue(todayItem != null)
        assertEquals(StreakDayStatus.COMPLETED, todayItem?.status)
    }
}
