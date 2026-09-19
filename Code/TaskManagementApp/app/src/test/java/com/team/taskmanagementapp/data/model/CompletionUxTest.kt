package com.team.taskmanagementapp.data.model

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.local.entity.withCompletionTracking
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import org.junit.Assert.*
import org.junit.Test

class CompletionUxTest {
    private val task = Task(title = "Test", description = "", dueDate = 0, dueTime = 0, priority = Priority.LOW)

    @Test fun completionSetsDateAndEditingPreservesIt() {
        val done = task.copy(status = TaskStatus.COMPLETED).withCompletionTracking(task, 100)
        assertTrue(done.isCompleted)
        assertEquals(100L, done.completedAt)
        assertEquals(100L, done.copy(title = "Edited").withCompletionTracking(done, 200).completedAt)
    }

    @Test fun reopeningClearsDateAndCompletingAgainSetsNewDate() {
        val done = task.copy(isCompleted = true, status = TaskStatus.COMPLETED, completedAt = 100)
        val reopened = done.copy(isCompleted = false, status = TaskStatus.TODO).withCompletionTracking(done, 200)
        assertNull(reopened.completedAt)
        assertEquals(300L, reopened.copy(isCompleted = true).withCompletionTracking(reopened, 300).completedAt)
    }

    @Test fun editingLegacyDoneDoesNotInventCompletionDate() {
        val legacy = task.copy(isCompleted = true, status = TaskStatus.COMPLETED)
        assertNull(legacy.copy(title = "Edited").withCompletionTracking(legacy, 200).completedAt)
    }

    @Test fun doneScopeRemovesConflictingFiltersButKeepsPriorityAndDate() {
        val normalized = FilterCriteria(completion = CompletionFilter.DONE,
            statuses = setOf(TaskStatus.TODO), dueDateRange = DueDateRange.OVERDUE,
            priorities = setOf(Priority.URGENT)).normalized()
        assertTrue(normalized.statuses.isEmpty())
        assertEquals(DueDateRange.ALL, normalized.dueDateRange)
        assertEquals(setOf(Priority.URGENT), normalized.priorities)
        assertEquals(DueDateRange.TODAY, normalized.copy(dueDateRange = DueDateRange.TODAY).normalized().dueDateRange)
    }
}
