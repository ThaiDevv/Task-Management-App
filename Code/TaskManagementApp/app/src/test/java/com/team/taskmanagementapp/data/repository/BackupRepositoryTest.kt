package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    @Test
    fun `task toExportTask maps all properties accurately`() {
        val originalTask = Task(
            id = 42,
            title = "Prepare quarterly report",
            description = "Gather financial metrics and KPIs",
            dueDate = 1788714000000L,
            dueTime = 1788775200000L,
            priority = Priority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            isCompleted = false,
            isRecurring = true,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceInterval = 1,
            reminderMinutes = 15,
            createdAt = 1788700000000L,
            updatedAt = 1788705000000L
        )

        val exportTask = originalTask.toExportTask()

        assertEquals(originalTask.id, exportTask.id)
        assertEquals(originalTask.title, exportTask.title)
        assertEquals(originalTask.description, exportTask.description)
        assertEquals(originalTask.dueDate, exportTask.dueDate)
        assertEquals(originalTask.dueTime, exportTask.dueTime)
        assertEquals(originalTask.priority, exportTask.priority)
        assertEquals(originalTask.status, exportTask.status)
        assertEquals(originalTask.isCompleted, exportTask.isCompleted)
        assertEquals(originalTask.isRecurring, exportTask.isRecurring)
        assertEquals(originalTask.recurrenceType, exportTask.recurrenceType)
        assertEquals(originalTask.recurrenceInterval, exportTask.recurrenceInterval)
        assertEquals(originalTask.reminderMinutes, exportTask.reminderMinutes)
        assertEquals(originalTask.createdAt, exportTask.createdAt)
        assertEquals(originalTask.updatedAt, exportTask.updatedAt)
    }

    @Test
    fun `exportTask toEntity resets id to zero for safe insertion`() {
        val exportTask = ExportTask(
            id = 99,
            title = "Buy supplies",
            description = "Markers and sticky notes",
            dueDate = 1788800000000L,
            dueTime = 1788810000000L,
            priority = Priority.MEDIUM,
            status = TaskStatus.TODO,
            isCompleted = false,
            isRecurring = false,
            recurrenceType = RecurrenceType.NONE,
            recurrenceInterval = 0,
            reminderMinutes = 0,
            createdAt = 1788700000000L,
            updatedAt = 1788700000000L
        )

        val entity = exportTask.toEntity()

        assertEquals(0, entity.id)
        assertEquals(exportTask.title, entity.title)
        assertEquals(exportTask.description, entity.description)
        assertEquals(exportTask.dueDate, entity.dueDate)
        assertEquals(exportTask.dueTime, entity.dueTime)
        assertEquals(exportTask.priority, entity.priority)
        assertEquals(exportTask.status, entity.status)
    }

    @Test
    fun `backupUiState representations handle all lifecycle states`() {
        val idle: BackupUiState = BackupUiState.Idle
        val loading: BackupUiState = BackupUiState.Loading("Exporting tasks...")
        val success: BackupUiState = BackupUiState.Success("Exported 10 tasks successfully", 10)
        val error: BackupUiState = BackupUiState.Error("Corrupted JSON", ErrorType.PARSE_ERROR)

        assertTrue(idle is BackupUiState.Idle)
        assertEquals("Exporting tasks...", (loading as BackupUiState.Loading).message)
        assertEquals(10, (success as BackupUiState.Success).taskCount)
        assertEquals(ErrorType.PARSE_ERROR, (error as BackupUiState.Error).errorType)
    }

    @Test
    fun `unsupportedVersionException conveys descriptive version message`() {
        val exception = UnsupportedVersionException("Unsupported export version: 99")
        assertEquals("Unsupported export version: 99", exception.message)
    }

    @Test
    fun `exportData structure serializes tasks and version correctly`() {
        val exportTask = ExportTask(
            id = 1,
            title = "Task 1",
            description = "Desc 1",
            dueDate = 1000L,
            dueTime = 2000L,
            priority = Priority.LOW,
            status = TaskStatus.TODO,
            isCompleted = false,
            isRecurring = false,
            recurrenceType = RecurrenceType.NONE,
            recurrenceInterval = 0,
            reminderMinutes = 10,
            createdAt = 500L,
            updatedAt = 600L
        )
        val exportData = ExportData(
            version = BackupRepository.EXPORT_VERSION,
            exportDate = "2026-09-07T11:00:00Z",
            taskCount = 1,
            tasks = listOf(exportTask)
        )

        assertEquals(1, exportData.version)
        assertEquals(1, exportData.taskCount)
        assertEquals("Task 1", exportData.tasks[0].title)
    }
}
