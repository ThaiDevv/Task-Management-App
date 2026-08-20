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
        fun isOverdue(task: Task): Boolean =
            !task.isCompleted && (task.status == TaskStatus.OVERDUE || task.dueDate < now)

        assertTrue("Past due incomplete task should be overdue", isOverdue(pastDueTask))
        assertFalse("Completed task should never be overdue", isOverdue(completedPastDueTask))
        assertFalse("Future task should not be overdue", isOverdue(futureTask))
    }
}
