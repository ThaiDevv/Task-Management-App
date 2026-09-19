package com.team.taskmanagementapp.data.validator

import com.google.gson.Gson
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.data.repository.ExportData
import com.team.taskmanagementapp.data.repository.ExportPomodoroSession
import com.team.taskmanagementapp.data.repository.toEntity
import com.team.taskmanagementapp.data.repository.toExportSession
import com.team.taskmanagementapp.data.repository.toExportTask
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho **format backup JSON** (Task 16).
 *
 * Kiểm tra: validate trước khi restore, version hoá file, giữ nguyên ID và quan hệ
 * Task ↔ PomodoroSession qua vòng serialize → deserialize.
 *
 * Dùng đúng [BackupRepository.backupGson] mà app dùng khi ghi/đọc file.
 */
class BackupValidatorTest {

    private val gson: Gson = BackupRepository.backupGson

    private fun task(
        id: Int = 1,
        title: String = "Write report",
        completedPomodoros: Int = 2,
        totalFocusTimeMinutes: Int = 50
    ) = Task(
        id = id,
        title = title,
        description = "desc",
        dueDate = 1_700_000_000_000L,
        dueTime = 1_700_000_000_000L,
        priority = Priority.HIGH,
        status = TaskStatus.TODO,
        isCompleted = false,
        isRecurring = false,
        recurrenceType = RecurrenceType.NONE,
        recurrenceInterval = 1,
        reminderMinutes = 10,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        estimatedPomodoros = 4,
        completedPomodoros = completedPomodoros,
        totalFocusTimeMinutes = totalFocusTimeMinutes
    )

    private fun session(
        id: Long = 100,
        taskId: Long = 1,
        sessionType: SessionType = SessionType.FOCUS,
        isCompleted: Boolean = true
    ) = PomodoroSession(
        id = id,
        taskId = taskId,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_000_000L + 25 * 60_000L,
        durationInMinutes = 25,
        sessionType = sessionType,
        isCompleted = isCompleted
    )

    private fun validBackupJson(): String = gson.toJson(
        ExportData(
            version = BackupRepository.EXPORT_VERSION,
            exportDate = "2026-09-19T00:00:00Z",
            taskCount = 1,
            tasks = listOf(task().toExportTask()),
            pomodoroSessionCount = 1,
            pomodoroSessions = listOf(session().toExportSession())
        )
    )

    private fun validate(json: String) =
        BackupValidator.validate(json, BackupRepository.EXPORT_VERSION)

    // ══════════════════════════════════════════════════════════════════════════
    // 1. File hợp lệ
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `valid backup with task and pomodoro session passes validation`() {
        val result = validate(validBackupJson())

        assertTrue(result.errorMessages.toString(), result.isValid)
        assertTrue(result.errorMessages.isEmpty())
    }

    @Test
    fun `empty backup is valid so it can wipe data`() {
        val json = gson.toJson(
            ExportData(
                version = BackupRepository.EXPORT_VERSION,
                exportDate = "2026-09-19T00:00:00Z",
                taskCount = 0,
                tasks = emptyList(),
                pomodoroSessionCount = 0,
                pomodoroSessions = emptyList()
            )
        )

        assertTrue(validate(json).isValid)
    }

    @Test
    fun `backup without pomodoroSessions field is still valid`() {
        val json = """
            {
              "version": 2,
              "exportDate": "2026-09-19T00:00:00Z",
              "taskCount": 1,
              "tasks": [
                {
                  "id": 1,
                  "title": "Legacy task",
                  "description": "",
                  "dueDate": 1700000000000,
                  "dueTime": 0,
                  "priority": "MEDIUM",
                  "status": "TODO",
                  "isCompleted": false,
                  "isRecurring": false,
                  "recurrenceType": "NONE",
                  "recurrenceInterval": 1,
                  "reminderMinutes": 0,
                  "createdAt": 1,
                  "updatedAt": 2
                }
              ]
            }
        """.trimIndent()

        assertTrue(validate(json).errorMessages.toString(), validate(json).isValid)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. File hỏng / sai định dạng
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `invalid json syntax is rejected gracefully`() {
        val result = validate("{ this is not json")

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Invalid JSON syntax", ignoreCase = true) })
    }

    @Test
    fun `empty file is rejected`() {
        val result = validate("   ")

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("empty", ignoreCase = true) })
    }

    @Test
    fun `json root that is not an object is rejected`() {
        assertFalse(validate("[1, 2, 3]").isValid)
    }

    @Test
    fun `missing tasks array is rejected`() {
        val result = validate("""{"version": 2}""")

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("tasks") })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Version
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `unsupported newer version is rejected`() {
        val json = validBackupJson().replace("\"version\": 2", "\"version\": 99")

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Unsupported backup version 99") })
    }

    @Test
    fun `version must be a positive number`() {
        assertFalse(validate(validBackupJson().replace("\"version\": 2", "\"version\": \"2\"")).isValid)
        assertFalse(validate(validBackupJson().replace("\"version\": 2", "\"version\": 0")).isValid)
        assertFalse(validate(validBackupJson().replace("\"version\": 2", "\"version\": -1")).isValid)
    }

    @Test
    fun `older version 1 backup is still supported`() {
        val json = validBackupJson().replace("\"version\": 2", "\"version\": 1")

        assertTrue(validate(json).isValid)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Lỗi ở Task
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `task with empty title is rejected`() {
        val result = validate(validBackupJson().replace("\"Write report\"", "\"\""))

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("title") })
    }

    @Test
    fun `task with unknown enum values is rejected`() {
        assertFalse(validate(validBackupJson().replace("\"HIGH\"", "\"SUPER_HIGH\"")).isValid)
        assertFalse(validate(validBackupJson().replace("\"NONE\"", "\"EVERY_DAY\"")).isValid)
    }

    @Test
    fun `task with duplicate id is rejected`() {
        val json = """
            {
              "version": 2,
              "taskCount": 2,
              "tasks": [
                $MINIMAL_TASK_JSON,
                $MINIMAL_TASK_JSON
              ]
            }
        """.trimIndent()

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Duplicate id") })
    }

    @Test
    fun `task with negative pomodoro counter is rejected`() {
        val json = validBackupJson().replace("\"completedPomodoros\": 2", "\"completedPomodoros\": -2")

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("completedPomodoros") })
    }

    @Test
    fun `declared taskCount mismatch is rejected`() {
        val json = validBackupJson().replace("\"taskCount\": 1", "\"taskCount\": 5")

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("taskCount") })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Lỗi ở PomodoroSession + toàn vẹn khoá ngoại
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `session pointing to a task that is not in the backup is rejected`() {
        val json = validBackupJson().replace("\"taskId\": 1", "\"taskId\": 777")

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(
            result.errorMessages.toString(),
            result.errorMessages.any { it.contains("taskId 777 does not exist") }
        )
    }

    @Test
    fun `session with invalid times or duration is rejected`() {
        val endBeforeStart = validBackupJson().replace("\"endTime\": 1700001500000", "\"endTime\": 1")
        assertFalse(validate(endBeforeStart).isValid)

        val zeroDuration = validBackupJson().replace("\"durationInMinutes\": 25", "\"durationInMinutes\": 0")
        assertFalse(validate(zeroDuration).isValid)
    }

    @Test
    fun `session with unknown type is rejected`() {
        val json = validBackupJson().replace("\"sessionType\": \"FOCUS\"", "\"sessionType\": \"SIESTA\"")

        val result = validate(json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("sessionType") })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Round-trip JSON: giữ nguyên ID + counters + quan hệ
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `task round trip keeps id and pomodoro counters`() {
        val original = task(id = 42, completedPomodoros = 3, totalFocusTimeMinutes = 75)

        val json = gson.toJson(ExportData(1, "", 1, listOf(original.toExportTask())))
        val parsed = gson.fromJson(json, ExportData::class.java)

        val restored = parsed.tasks.single().toEntity(preserveId = true)

        assertEquals(42, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.priority, restored.priority)
        assertEquals(original.status, restored.status)
        assertEquals(4, restored.estimatedPomodoros)
        assertEquals(3, restored.completedPomodoros)
        assertEquals(75, restored.totalFocusTimeMinutes)
    }

    @Test
    fun `import without preserveId still resets id for plain imports`() {
        val exportTask = task(id = 42).toExportTask()

        assertEquals(0, exportTask.toEntity().id)
        assertEquals(42, exportTask.toEntity(preserveId = true).id)
    }

    @Test
    fun `pomodoro session round trip keeps id taskId and type`() {
        val original = session(id = 7, taskId = 42, sessionType = SessionType.LONG_BREAK, isCompleted = false)

        val json = gson.toJson(ExportData(1, "", 1, emptyList(), 1, listOf(original.toExportSession())))
        val parsed = gson.fromJson(json, ExportData::class.java)

        val restored = parsed.pomodoroSessions.single().toEntity()

        assertNotNull(restored)
        assertEquals(7L, restored!!.id)
        assertEquals(42L, restored.taskId)
        assertEquals(SessionType.LONG_BREAK, restored.sessionType)
        assertEquals(25, restored.durationInMinutes)
        assertEquals(original.startTime, restored.startTime)
        assertEquals(original.endTime, restored.endTime)
        assertFalse(restored.isCompleted)
    }

    @Test
    fun `session with unknown type in json is dropped instead of crashing`() {
        val dto = ExportPomodoroSession(
            id = 1,
            taskId = 1,
            startTime = 1,
            endTime = 2,
            durationInMinutes = 1,
            sessionType = "NOT_A_TYPE",
            isCompleted = true
        )

        assertNull(dto.toEntity())
    }

    private companion object {
        /** Task tối thiểu hợp lệ, dùng để nhân bản trong các test về trùng id. */
        val MINIMAL_TASK_JSON = """
            {
              "id": 1,
              "title": "Task",
              "description": "",
              "dueDate": 1700000000000,
              "dueTime": 0,
              "priority": "MEDIUM",
              "status": "TODO",
              "isCompleted": false,
              "isRecurring": false,
              "recurrenceType": "NONE",
              "recurrenceInterval": 1,
              "reminderMinutes": 0,
              "createdAt": 1,
              "updatedAt": 2
            }
        """.trimIndent()
    }
}
