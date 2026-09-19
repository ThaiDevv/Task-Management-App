package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho **restore backup** (Task 16) — phần logic quan trọng nhất:
 * giữ nguyên ID, bảo toàn quan hệ Task ↔ PomodoroSession và **rollback khi lỗi**.
 *
 * `applyBackup` nhận sẵn các lambda thao tác DB (production truyền
 * `database.withTransaction { ... }` + các hàm DAO), nên ở đây thay bằng kho dữ liệu
 * trong bộ nhớ + transaction giả lập snapshot/rollback — không cần Room/Android.
 */
class BackupRestoreTest {

    // ══════════════════════════════════════════════════════════════════════════
    // Hạ tầng giả lập DB + transaction
    // ══════════════════════════════════════════════════════════════════════════

    /** Kho dữ liệu trong bộ nhớ + ghi lại thứ tự gọi để kiểm tra trình tự restore. */
    private class InMemoryStore {
        val tasks = mutableListOf<Task>()
        val sessions = mutableListOf<PomodoroSession>()
        val callLog = mutableListOf<String>()

        fun clearAllTasks() {
            callLog += "clearTasks"
            tasks.clear()
        }

        fun clearAllSessions() {
            callLog += "clearSessions"
            sessions.clear()
        }

        fun insertTasks(newTasks: List<Task>) {
            callLog += "insertTasks(${newTasks.size})"
            tasks += newTasks
        }

        fun insertSessions(newSessions: List<PomodoroSession>) {
            callLog += "insertSessions(${newSessions.size})"
            // Mô phỏng FK: phiên phải trỏ tới task đang tồn tại.
            val taskIds = tasks.map { it.id.toLong() }.toSet()
            newSessions.forEach { session ->
                if (session.taskId !in taskIds) {
                    throw IllegalStateException("FOREIGN KEY constraint failed (taskId=${session.taskId})")
                }
            }
            sessions += newSessions
        }
    }

    /**
     * Transaction giả lập theo đúng ngữ nghĩa Room: chụp snapshot trước khi chạy,
     * nếu block ném exception thì khôi phục nguyên trạng (rollback) rồi ném tiếp.
     */
    private fun InMemoryStore.transactionRunner(): suspend (suspend () -> Unit) -> Unit = { block ->
        val tasksSnapshot = tasks.toList()
        val sessionsSnapshot = sessions.toList()
        try {
            block()
        } catch (e: Throwable) {
            tasks.clear()
            tasks += tasksSnapshot
            sessions.clear()
            sessions += sessionsSnapshot
            throw e
        }
    }

    private suspend fun runRestore(store: InMemoryStore, backup: ExportData): BackupSummary =
        applyBackup(
            backup = backup,
            inTransaction = store.transactionRunner(),
            clearAllTasks = { store.clearAllTasks() },
            clearAllSessions = { store.clearAllSessions() },
            insertTasks = { store.insertTasks(it) },
            insertSessions = { store.insertSessions(it) }
        )

    // ══════════════════════════════════════════════════════════════════════════
    // Dữ liệu mẫu
    // ══════════════════════════════════════════════════════════════════════════

    private fun task(id: Int, title: String, completedPomodoros: Int = 0) = Task(
        id = id,
        title = title,
        description = "desc",
        dueDate = 1_700_000_000_000L,
        dueTime = 0L,
        priority = Priority.MEDIUM,
        status = TaskStatus.TODO,
        isCompleted = false,
        isRecurring = false,
        recurrenceType = RecurrenceType.NONE,
        recurrenceInterval = 1,
        reminderMinutes = 0,
        createdAt = 1L,
        updatedAt = 2L,
        completedPomodoros = completedPomodoros,
        totalFocusTimeMinutes = completedPomodoros * 25
    )

    private fun session(id: Long, taskId: Long, type: SessionType = SessionType.FOCUS) = PomodoroSession(
        id = id,
        taskId = taskId,
        startTime = 1_700_000_000_000L + id * 60_000L,
        endTime = 1_700_000_000_000L + id * 60_000L + 25 * 60_000L,
        durationInMinutes = 25,
        sessionType = type,
        isCompleted = true
    )

    private fun backupOf(tasks: List<Task>, sessions: List<PomodoroSession>) = ExportData(
        version = BackupRepository.EXPORT_VERSION,
        exportDate = "2026-09-19T00:00:00Z",
        taskCount = tasks.size,
        tasks = tasks.map { it.toExportTask() },
        pomodoroSessionCount = sessions.size,
        pomodoroSessions = sessions.map { it.toExportSession() }
    )

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Restore Task
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restore replaces existing data and preserves task ids`() = runBlocking {
        val store = InMemoryStore().apply {
            tasks += task(99, "Old task")
            sessions += session(999, 99)
        }
        val backup = backupOf(listOf(task(5, "Restored A"), task(7, "Restored B")), emptyList())

        val summary = runRestore(store, backup)

        assertEquals(2, summary.taskCount)
        assertEquals(listOf(5, 7), store.tasks.map { it.id })
        assertEquals(listOf("Restored A", "Restored B"), store.tasks.map { it.title })
        assertTrue("Dữ liệu cũ phải bị thay thế", store.tasks.none { it.title == "Old task" })
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun `restore keeps pomodoro counters of tasks`() = runBlocking {
        val store = InMemoryStore()
        val backup = backupOf(listOf(task(3, "Focused task", completedPomodoros = 4)), emptyList())

        runRestore(store, backup)

        assertEquals(4, store.tasks.single().completedPomodoros)
        assertEquals(100, store.tasks.single().totalFocusTimeMinutes)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Restore PomodoroSession + quan hệ với Task
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restore preserves session ids and task relationship`() = runBlocking {
        val store = InMemoryStore()
        val backup = backupOf(
            tasks = listOf(task(11, "Alpha"), task(22, "Beta")),
            sessions = listOf(
                session(101, 11),
                session(102, 11, SessionType.SHORT_BREAK),
                session(103, 22, SessionType.LONG_BREAK)
            )
        )

        val summary = runRestore(store, backup)

        assertEquals(2, summary.taskCount)
        assertEquals(3, summary.sessionCount)
        assertEquals(listOf(101L, 102L, 103L), store.sessions.map { it.id })
        assertEquals(listOf(11L, 11L, 22L), store.sessions.map { it.taskId })
        assertEquals(
            listOf(SessionType.FOCUS, SessionType.SHORT_BREAK, SessionType.LONG_BREAK),
            store.sessions.map { it.sessionType }
        )

        // Mọi phiên đều trỏ tới một Task tồn tại sau restore (toàn vẹn khoá ngoại)
        val restoredTaskIds = store.tasks.map { it.id.toLong() }.toSet()
        assertTrue(store.sessions.all { it.taskId in restoredTaskIds })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Empty data
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restoring an empty backup clears all data`() = runBlocking {
        val store = InMemoryStore().apply {
            tasks += task(1, "Old")
            sessions += session(1, 1)
        }

        val summary = runRestore(store, backupOf(emptyList(), emptyList()))

        assertEquals(0, summary.taskCount)
        assertEquals(0, summary.sessionCount)
        assertTrue(store.tasks.isEmpty())
        assertTrue(store.sessions.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Transaction / rollback
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restore runs entirely inside one transaction in the right order`() = runBlocking {
        val store = InMemoryStore()
        val backup = backupOf(listOf(task(1, "A")), listOf(session(1, 1)))

        runRestore(store, backup)

        assertEquals(
            listOf("clearTasks", "clearSessions", "insertTasks(1)", "insertSessions(1)"),
            store.callLog
        )
    }

    @Test
    fun `failed restore rolls back and keeps previous data intact`() = runBlocking {
        val store = InMemoryStore().apply {
            tasks += task(1, "Existing task")
            sessions += session(1, 1)
        }
        // File backup trỏ tới task 404 không tồn tại ⇒ giả lập vi phạm khoá ngoại khi ghi phiên
        val brokenBackup = backupOf(listOf(task(1, "New task")), listOf(session(50, 404)))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { runRestore(store, brokenBackup) }
        }

        // Rollback: không có restore dở dang, dữ liệu cũ còn nguyên
        assertEquals(listOf("Existing task"), store.tasks.map { it.title })
        assertEquals(listOf(1L), store.tasks.map { it.id.toLong() })
        assertEquals(listOf(1L), store.sessions.map { it.id })
        assertTrue("Task mới KHÔNG được giữ lại", store.tasks.none { it.title == "New task" })
    }

    @Test
    fun `failed restore on empty database leaves nothing behind`() = runBlocking {
        val store = InMemoryStore()
        val brokenBackup = backupOf(listOf(task(1, "A")), listOf(session(9, 999)))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { runRestore(store, brokenBackup) }
        }

        assertTrue(store.tasks.isEmpty())
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun `restore of a backup whose sessions belong to no task is rejected by foreign key`(): Unit = runBlocking {
        val store = InMemoryStore()
        val backup = backupOf(emptyList(), listOf(session(1, 5)))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { runRestore(store, backup) }
        }
    }
}
