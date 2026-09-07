package com.team.taskmanagementapp.ui

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.ui.base.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskUiStateTest {

    @Test
    fun testUiStateSealedClassInstances() {
        val loading: UiState<List<Task>> = UiState.Loading
        val empty: UiState<List<Task>> = UiState.Empty
        val error: UiState<List<Task>> = UiState.Error("Test Error")

        val dummyTask = Task(
            id = 1,
            title = "Test Task",
            description = "Description",
            dueDate = System.currentTimeMillis() + 86400000L,
            dueTime = System.currentTimeMillis() + 86400000L,
            priority = Priority.HIGH,
            status = TaskStatus.TODO
        )
        val success: UiState<List<Task>> = UiState.Success(listOf(dummyTask))

        assertTrue(loading is UiState.Loading)
        assertTrue(empty is UiState.Empty)
        assertTrue(error is UiState.Error)
        assertEquals("Test Error", (error as UiState.Error).message)
        assertTrue(success is UiState.Success)
        assertEquals(1, (success as UiState.Success).data.size)
    }

    @Test
    fun testOverdueTaskDetection() {
        val now = System.currentTimeMillis()
        val pastDueTask = Task(
            id = 2,
            title = "Overdue Task",
            description = "Past due",
            dueDate = now - 3600000L, // 1 hour ago
            dueTime = now - 3600000L,
            priority = Priority.URGENT,
            status = TaskStatus.TODO,
            isCompleted = false
        )

        val completedPastDueTask = pastDueTask.copy(
            isCompleted = true,
            status = TaskStatus.COMPLETED
        )

        val futureTask = Task(
            id = 3,
            title = "Future Task",
            description = "Tomorrow",
            dueDate = now + 86400000L,
            dueTime = now + 86400000L,
            priority = Priority.MEDIUM,
            status = TaskStatus.TODO,
            isCompleted = false
        )

        // Helper function matching TaskAdapter & UpcomingTaskAdapter logic
        fun isOverdue(task: Task): Boolean {
            val combined = com.team.taskmanagementapp.util.DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            return !task.isCompleted && (task.status == TaskStatus.OVERDUE || (combined > 0L && combined < now))
        }

        assertTrue("Past due incomplete task should be overdue", isOverdue(pastDueTask))
        assertFalse("Completed task should never be overdue", isOverdue(completedPastDueTask))
        assertFalse("Future task should not be overdue", isOverdue(futureTask))
    }

    @Test
    fun testUpcomingAndTodayListFiltering() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        val tomorrowCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
        tomorrowCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        tomorrowCal.set(java.util.Calendar.MINUTE, 0)
        tomorrowCal.set(java.util.Calendar.SECOND, 0)
        tomorrowCal.set(java.util.Calendar.MILLISECOND, 0)
        val tomorrowStart = tomorrowCal.timeInMillis

        val tomorrowTask = Task(
            id = 10,
            title = "Tomorrow Task",
            description = "Due tomorrow",
            dueDate = tomorrowStart,
            dueTime = tomorrowStart + 36000000L,
            priority = Priority.MEDIUM,
            status = TaskStatus.TODO
        )

        val past2024Task = Task(
            id = 11,
            title = "2024 Task from sample backup",
            description = "Expired in 2024",
            dueDate = 1725206400000L,
            dueTime = 1725206400000L,
            priority = Priority.HIGH,
            status = TaskStatus.TODO
        )

        val allTasks = listOf(tomorrowTask, past2024Task)
        val todayList = allTasks.filter { it.dueDate <= endOfToday }
        val upcomingList = allTasks.filter { it.dueDate > endOfToday }

        // Past backup task goes to todayList (and is marked overdue)
        assertTrue(todayList.contains(past2024Task))
        assertFalse(upcomingList.contains(past2024Task))

        // Tomorrow task goes to upcomingList
        assertTrue(upcomingList.contains(tomorrowTask))
        assertFalse(todayList.contains(tomorrowTask))

        // Verify UpcomingTaskAdapter dateLabel logic for tomorrow
        val taskCal = java.util.Calendar.getInstance().apply { timeInMillis = tomorrowTask.dueDate }
        val isTomorrow = taskCal.get(java.util.Calendar.YEAR) == tomorrowCal.get(java.util.Calendar.YEAR)
                && taskCal.get(java.util.Calendar.DAY_OF_YEAR) == tomorrowCal.get(java.util.Calendar.DAY_OF_YEAR)
        val dateLabel = if (isTomorrow) "Tomorrow" else "Other Date"
        assertEquals("Tomorrow", dateLabel)
    }
}
