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

    @Test
    fun `export to json serialization produces valid json with all expected fields`() {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val originalTask = Task(
            id = 10,
            title = "Review PR #45",
            description = "Check unit tests and merge conflict resolution",
            dueDate = 1788714000000L,
            dueTime = 1788775200000L,
            priority = Priority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            isCompleted = false,
            isRecurring = true,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceInterval = 2,
            reminderMinutes = 30,
            createdAt = 1788700000000L,
            updatedAt = 1788705000000L
        )

        val exportData = ExportData(
            version = BackupRepository.EXPORT_VERSION,
            exportDate = "2026-09-14T00:00:00Z",
            taskCount = 1,
            tasks = listOf(originalTask.toExportTask())
        )

        val jsonString = gson.toJson(exportData)

        // Verify JSON string contains required top-level keys
        assertTrue(jsonString.contains("\"version\": 1"))
        assertTrue(jsonString.contains("\"exportDate\": \"2026-09-14T00:00:00Z\""))
        assertTrue(jsonString.contains("\"taskCount\": 1"))
        assertTrue(jsonString.contains("\"tasks\": ["))

        // Verify JSON string contains task fields
        assertTrue(jsonString.contains("\"title\": \"Review PR #45\""))
        assertTrue(jsonString.contains("\"priority\": \"HIGH\""))
        assertTrue(jsonString.contains("\"status\": \"IN_PROGRESS\""))
        assertTrue(jsonString.contains("\"recurrenceType\": \"DAILY\""))
        assertTrue(jsonString.contains("\"reminderMinutes\": 30"))

        // Verify JSON round-trip deserialization
        val deserialized = gson.fromJson(jsonString, ExportData::class.java)
        assertEquals(1, deserialized.version)
        assertEquals(1, deserialized.taskCount)
        assertEquals(1, deserialized.tasks.size)

        val exportedTask = deserialized.tasks[0]
        assertEquals(originalTask.title, exportedTask.title)
        assertEquals(originalTask.description, exportedTask.description)
        assertEquals(originalTask.priority, exportedTask.priority)
        assertEquals(originalTask.status, exportedTask.status)
        assertEquals(originalTask.isRecurring, exportedTask.isRecurring)
        assertEquals(originalTask.recurrenceType, exportedTask.recurrenceType)
        assertEquals(originalTask.recurrenceInterval, exportedTask.recurrenceInterval)
    }

    @Test
    fun `export multiple tasks maintains accurate count and task order`() {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val tasks = (1..5).map { index ->
            Task(
                id = index,
                title = "Task #$index",
                description = "Description for task $index",
                dueDate = 1788714000000L + index * 1000L,
                dueTime = 1788775200000L,
                priority = if (index % 2 == 0) Priority.HIGH else Priority.LOW,
                status = if (index == 1) TaskStatus.COMPLETED else TaskStatus.TODO,
                isCompleted = index == 1,
                isRecurring = false,
                recurrenceType = RecurrenceType.NONE,
                recurrenceInterval = 0,
                reminderMinutes = 0,
                createdAt = 1788700000000L,
                updatedAt = 1788700000000L
            )
        }

        val exportData = ExportData(
            version = BackupRepository.EXPORT_VERSION,
            exportDate = "2026-09-14T00:00:00Z",
            taskCount = tasks.size,
            tasks = tasks.map { it.toExportTask() }
        )

        val jsonString = gson.toJson(exportData)
        val deserialized = gson.fromJson(jsonString, ExportData::class.java)

        assertEquals(5, deserialized.taskCount)
        assertEquals(5, deserialized.tasks.size)
        for (i in 0 until 5) {
            assertEquals("Task #${i + 1}", deserialized.tasks[i].title)
            val entity = deserialized.tasks[i].toEntity()
            assertEquals(0, entity.id) // ID is reset for insertion
            assertEquals("Task #${i + 1}", entity.title)
        }
    }

    @Test
    fun `empty tasks export creates valid JSON with zero count`() {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val exportData = ExportData(
            version = BackupRepository.EXPORT_VERSION,
            exportDate = "2026-09-14T00:00:00Z",
            taskCount = 0,
            tasks = emptyList()
        )

        val jsonString = gson.toJson(exportData)
        val deserialized = gson.fromJson(jsonString, ExportData::class.java)

        assertEquals(0, deserialized.taskCount)
        assertTrue(deserialized.tasks.isEmpty())
        assertEquals(BackupRepository.EXPORT_VERSION, deserialized.version)
    }

    @Test
    fun `BackupHistoryItem serializes and deserializes accurately`() {
        val gson = com.google.gson.Gson()
        val item = BackupHistoryItem(
            fileName = "taskflow_backup_20260914_021500.json",
            formattedDate = "Sep 14, 2026 • 02:15",
            sizeString = "42 KB",
            timestamp = 1788700000000L
        )

        val json = gson.toJson(item)
        val deserialized = gson.fromJson(json, BackupHistoryItem::class.java)

        assertEquals(item.fileName, deserialized.fileName)
        assertEquals(item.formattedDate, deserialized.formattedDate)
        assertEquals(item.sizeString, deserialized.sizeString)
        assertEquals(item.timestamp, deserialized.timestamp)
    }

    @Test
    fun `list of BackupHistoryItem serializes and deserializes with correct order`() {
        val gson = com.google.gson.Gson()
        val list = listOf(
            BackupHistoryItem("backup_2.json", "Sep 14, 2026 • 02:15", "10 KB", 2000L),
            BackupHistoryItem("backup_1.json", "Sep 13, 2026 • 10:00", "8 KB", 1000L)
        )

        val json = gson.toJson(list)
        val type = object : com.google.gson.reflect.TypeToken<List<BackupHistoryItem>>() {}.type
        val deserialized: List<BackupHistoryItem> = gson.fromJson(json, type)

        assertEquals(2, deserialized.size)
        assertEquals("backup_2.json", deserialized[0].fileName)
        assertEquals("backup_1.json", deserialized[1].fileName)
    }
}
