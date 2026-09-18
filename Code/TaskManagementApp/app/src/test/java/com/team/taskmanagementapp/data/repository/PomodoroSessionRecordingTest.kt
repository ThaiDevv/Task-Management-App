package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.PomodoroDao
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.stats.TaskFocusStats
import com.team.taskmanagementapp.pomodoro.CompletedSessionRecord
import com.team.taskmanagementapp.pomodoro.PomodoroCompletionTracker
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho Task 14 — lưu phiên Pomodoro đã hoàn thành và cộng dồn thống kê Task.
 *
 * Chạy hoàn toàn trên JVM: `FakePomodoroDao` cài đặt [PomodoroDao] bằng bộ nhớ, nên
 * **thân hàm `recordCompletedSession` của DAO (findTaskId → insert → update counters)
 * vẫn là code thật** đang được kiểm tra, chỉ thiếu phần transaction của SQLite
 * (đã được Room kiểm tra lúc compile + verify bằng SQLite thật ở bước riêng).
 */
class PomodoroSessionRecordingTest {

    private companion object {
        const val TASK_ID = 7L
        const val FOCUS_MINUTES = 25
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Fake DAO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Fake DAO tối thiểu: chỉ cài đặt những hàm mà luồng "ghi phiên" cần.
     * Các hàm còn lại (đọc thống kê) không dùng tới nên ném lỗi để lộ sớm nếu bị gọi.
     */
    private class FakePomodoroDao(taskIds: List<Long> = emptyList()) : PomodoroDao {

        /** Task "còn tồn tại" — dùng để mô phỏng trường hợp Task đã bị xoá. */
        private val existingTaskIds = taskIds.toMutableSet()

        val sessions = mutableListOf<PomodoroSession>()
        private val completedPomodoros = mutableMapOf<Long, Int>()
        private val focusMinutes = mutableMapOf<Long, Int>()

        fun completedPomodoros(taskId: Long): Int = completedPomodoros[taskId] ?: 0

        fun totalFocusMinutes(taskId: Long): Int = focusMinutes[taskId] ?: 0

        fun removeTask(taskId: Long) {
            existingTaskIds.remove(taskId)
        }

        // ── Hàm thật sự được dùng ────────────────────────────────────────────

        override suspend fun findTaskId(taskId: Long): Int? =
            if (existingTaskIds.contains(taskId)) taskId.toInt() else null

        override suspend fun insertSession(session: PomodoroSession): Long {
            sessions += session.copy(id = sessions.size + 1L)
            return sessions.size.toLong()
        }

        override suspend fun addCompletedFocusSessionStats(
            taskId: Long,
            focusMinutes: Int
        ): Int {
            completedPomodoros[taskId] = completedPomodoros(taskId) + 1
            this.focusMinutes[taskId] = totalFocusMinutes(taskId) + focusMinutes
            return 1
        }

        override suspend fun getSessionsByTaskId(taskId: Long): List<PomodoroSession> =
            sessions.filter { it.taskId == taskId }

        override suspend fun getIncompleteSessions(): List<PomodoroSession> =
            sessions.filter { !it.isCompleted }

        override suspend fun getLatestSession(): PomodoroSession? = sessions.lastOrNull()

        // ── Không dùng trong test này ────────────────────────────────────────

        override suspend fun insertSessions(sessions: List<PomodoroSession>): List<Long> =
            notUsed()

        override suspend fun deleteSession(session: PomodoroSession) = notUsed()

        override suspend fun deleteSessionsByTaskId(taskId: Long) = notUsed()

        override suspend fun deleteAllSessions() = notUsed()

        override fun observeSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
            emptyFlow()

        override fun observeFocusSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
            emptyFlow()

        override suspend fun getFocusSessionsByTaskId(taskId: Long): List<PomodoroSession> =
            notUsed()

        override fun observeCompletedFocusSessionCountByTaskId(taskId: Long): Flow<Int> =
            emptyFlow()

        override suspend fun getCompletedFocusSessionCountByTaskId(taskId: Long): Int =
            notUsed()

        override suspend fun getTotalFocusMinutesByTaskId(taskId: Long): Int = notUsed()

        override fun observeTotalCompletedFocusSessions(): Flow<Int> = emptyFlow()

        override suspend fun getTotalCompletedFocusSessions(): Int = notUsed()

        override fun observeTotalFocusMinutesInRange(startTime: Long, endTime: Long): Flow<Int> =
            emptyFlow()

        override suspend fun getTotalFocusMinutesInRange(startTime: Long, endTime: Long): Int =
            notUsed()

        override fun observeFocusSessionCountInRange(startTime: Long, endTime: Long): Flow<Int> =
            emptyFlow()

        override suspend fun getFocusSessionCountInRange(startTime: Long, endTime: Long): Int =
            notUsed()

        override fun observeFocusSessionsInRange(
            startTime: Long,
            endTime: Long
        ): Flow<List<PomodoroSession>> = emptyFlow()

        override suspend fun getFocusSessionsInRange(
            startTime: Long,
            endTime: Long
        ): List<PomodoroSession> = notUsed()

        override suspend fun getFocusStatsByTask(
            startTime: Long,
            endTime: Long
        ): List<TaskFocusStats> = notUsed()

        override fun observeFocusStatsByTask(
            startTime: Long,
            endTime: Long
        ): Flow<List<TaskFocusStats>> = emptyFlow()

        private fun notUsed(): Nothing =
            throw AssertionError("Hàm DAO này không được dùng trong luồng ghi phiên")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers dựng dữ liệu
    // ══════════════════════════════════════════════════════════════════════════

    private fun record(
        sessionType: SessionType = SessionType.FOCUS,
        isCompleted: Boolean = true,
        taskId: Long? = TASK_ID,
        durationMinutes: Int = FOCUS_MINUTES
    ) = CompletedSessionRecord(
        taskId = taskId,
        sessionType = sessionType,
        startTimeMillis = 1_700_000_000_000L,
        endTimeMillis = 1_700_000_000_000L + durationMinutes * 60_000L,
        durationMinutes = durationMinutes,
        isCompleted = isCompleted
    )

    private fun snapshot(
        completionId: Int,
        record: CompletedSessionRecord? = null
    ) = PomodoroSnapshot(completionId = completionId, lastCompletedSession = record)

    /**
     * Mô phỏng đúng một nhịp `handleTick()` của `PomodoroService`:
     * hỏi tracker (chống trùng) rồi mới ghi vào repository.
     */
    private suspend fun simulateTick(
        tracker: PomodoroCompletionTracker,
        repository: PomodoroRepository,
        snapshot: PomodoroSnapshot
    ) {
        val completed = tracker.consumeCompletedSession(snapshot) ?: return
        repository.recordCompletedSession(completed)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Yêu cầu: lưu đúng MỘT phiên khi phiên FOCUS hoàn thành
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testCompletedFocusSessionIsPersistedWithFullData() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        val saved = repository.recordCompletedSession(record(SessionType.FOCUS))

        assertTrue("Phiên FOCUS hoàn thành phải lưu được", saved)
        assertEquals(1, dao.sessions.size)

        val session = dao.sessions.single()
        assertEquals(TASK_ID, session.taskId)
        assertEquals(SessionType.FOCUS, session.sessionType)
        assertEquals(FOCUS_MINUTES, session.durationInMinutes)
        assertTrue("Phiên lưu xuống phải là phiên đã hoàn thành", session.isCompleted)
        assertEquals(1_700_000_000_000L, session.startTime)
        assertEquals(1_700_000_000_000L + FOCUS_MINUTES * 60_000L, session.endTime)
        assertTrue("Room tự sinh khoá chính nên phải truyền id = 0", session.id > 0L)
    }

    @Test
    fun testCompletedFocusSessionIncrementsTaskCounters() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        repository.recordCompletedSession(record(SessionType.FOCUS))

        assertEquals("completedPomodoros phải +1", 1, dao.completedPomodoros(TASK_ID))
        assertEquals(
            "totalFocusTimeMinutes phải + thời lượng phiên",
            FOCUS_MINUTES,
            dao.totalFocusMinutes(TASK_ID)
        )
    }

    @Test
    fun testCountersAccumulateAcrossManySessionsAndIgnoreBreaks() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        repository.recordCompletedSession(record(SessionType.FOCUS, durationMinutes = 25))
        repository.recordCompletedSession(record(SessionType.SHORT_BREAK, durationMinutes = 5))
        repository.recordCompletedSession(record(SessionType.FOCUS, durationMinutes = 15))
        repository.recordCompletedSession(record(SessionType.LONG_BREAK, durationMinutes = 15))

        assertEquals("4 phiên đều được lưu", 4, dao.sessions.size)
        assertEquals("chỉ 2 phiên FOCUS được tính", 2, dao.completedPomodoros(TASK_ID))
        assertEquals(40, dao.totalFocusMinutes(TASK_ID))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Yêu cầu: chỉ FOCUS mới cộng thống kê, nghỉ thì không
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testCompletedShortBreakIsPersistedWithoutTouchingCounters() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        assertTrue(repository.recordCompletedSession(record(SessionType.SHORT_BREAK, durationMinutes = 5)))

        assertEquals(1, dao.sessions.size)
        assertEquals(SessionType.SHORT_BREAK, dao.sessions.single().sessionType)
        assertEquals(0, dao.completedPomodoros(TASK_ID))
        assertEquals(0, dao.totalFocusMinutes(TASK_ID))
    }

    @Test
    fun testCompletedLongBreakIsPersistedWithoutTouchingCounters() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        assertTrue(repository.recordCompletedSession(record(SessionType.LONG_BREAK, durationMinutes = 15)))

        assertEquals(1, dao.sessions.size)
        assertEquals(SessionType.LONG_BREAK, dao.sessions.single().sessionType)
        assertEquals(0, dao.completedPomodoros(TASK_ID))
        assertEquals(0, dao.totalFocusMinutes(TASK_ID))
    }

    @Test
    fun testFocusMinutesForTaskStatsIsNullForBreaks() {
        assertEquals(FOCUS_MINUTES, record(SessionType.FOCUS).focusMinutesForTaskStats())
        assertNull(record(SessionType.SHORT_BREAK).focusMinutesForTaskStats())
        assertNull(record(SessionType.LONG_BREAK).focusMinutesForTaskStats())
    }

    @Test
    fun testRecordIsMappedToEntityWithGeneratedId() {
        val entity = record(SessionType.FOCUS).toPomodoroSession(taskId = TASK_ID)

        assertEquals(0L, entity.id)
        assertEquals(TASK_ID, entity.taskId)
        assertEquals(SessionType.FOCUS, entity.sessionType)
        assertEquals(FOCUS_MINUTES, entity.durationInMinutes)
        assertTrue(entity.isCompleted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Yêu cầu: STOP / SKIP / RESET / PAUSE không lưu phiên hoàn thành
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testStopPauseAndResetNeverPersistAnything() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        val tracker = PomodoroCompletionTracker()

        // STOP / RESET / PAUSE: engine không phát ra bản ghi nào (`lastCompletedSession = null`),
        // nhưng `completionId` vẫn tăng nên các nhịp tick sau đó vẫn phải bị bỏ qua.
        repeat(3) { simulateTick(tracker, repository, snapshot(completionId = 1)) }
        repeat(3) { simulateTick(tracker, repository, snapshot(completionId = 2)) }

        assertTrue("Không được có phiên nào được lưu", dao.sessions.isEmpty())
        assertEquals(0, dao.completedPomodoros(TASK_ID))
        assertEquals(0, dao.totalFocusMinutes(TASK_ID))
    }

    @Test
    fun testSkippedSessionIsNeverPersisted() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        val tracker = PomodoroCompletionTracker()

        // SKIP: có bản ghi nhưng isCompleted = false (đã chạy được 7 phút).
        val skipped = snapshot(
            completionId = 1,
            record = record(SessionType.FOCUS, isCompleted = false, durationMinutes = 7)
        )
        repeat(5) { simulateTick(tracker, repository, skipped) }

        assertTrue(dao.sessions.isEmpty())
        assertEquals(0, dao.completedPomodoros(TASK_ID))
        assertEquals(0, dao.totalFocusMinutes(TASK_ID))

        // Và không bao giờ ghi ra bản ghi "chưa hoàn thành" nào.
        assertTrue(dao.getIncompleteSessions().isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Yêu cầu: chống trùng khi cùng một lần hoàn thành đến từ nhiều đường
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testTickerAlarmAndNotificationPathsPersistExactlyOnce() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        val tracker = PomodoroCompletionTracker()

        val finished = snapshot(completionId = 1, record = record(SessionType.FOCUS))

        // 20 ticker tick + alarm Doze + 5 lệnh bấm trên notification: cùng MỘT lần hoàn thành.
        repeat(26) { simulateTick(tracker, repository, finished) }

        assertEquals("Chỉ được lưu đúng 1 phiên", 1, dao.sessions.size)
        assertEquals("completedPomodoros chỉ +1", 1, dao.completedPomodoros(TASK_ID))
        assertEquals(FOCUS_MINUTES, dao.totalFocusMinutes(TASK_ID))
    }

    @Test
    fun testTwoDifferentCompletionsPersistTwoSessions() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        val tracker = PomodoroCompletionTracker()

        val first = snapshot(completionId = 1, record = record(SessionType.FOCUS))
        val second = snapshot(completionId = 2, record = record(SessionType.SHORT_BREAK, durationMinutes = 5))

        repeat(4) { simulateTick(tracker, repository, first) }
        repeat(4) { simulateTick(tracker, repository, second) }

        assertEquals(2, dao.sessions.size)
        assertEquals(1, dao.completedPomodoros(TASK_ID))
        assertEquals(FOCUS_MINUTES, dao.totalFocusMinutes(TASK_ID))
    }

    @Test
    fun testServiceRestartDoesNotReplayAlreadyPersistedSession() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        val tracker = PomodoroCompletionTracker()
        val finished = snapshot(completionId = 5, record = record(SessionType.FOCUS))

        simulateTick(tracker, repository, finished)

        // Service bị tạo lại: onDestroy rồi onCreate, tracker mới được syncTo(completionId hiện tại).
        val restartedTracker = PomodoroCompletionTracker()
        restartedTracker.syncTo(completionId = 5)
        repeat(10) { simulateTick(restartedTracker, repository, finished) }

        assertEquals(1, dao.sessions.size)
        assertEquals(1, dao.completedPomodoros(TASK_ID))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Yêu cầu: Task đã bị xoá / không gắn Task thì bỏ qua an toàn
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testSessionWithoutTaskIsNotPersisted() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)

        val saved = repository.recordCompletedSession(record(SessionType.FOCUS, taskId = null))

        assertFalse("Phiên không gắn Task phải bị bỏ qua", saved)
        assertTrue(dao.sessions.isEmpty())
    }

    @Test
    fun testSessionForDeletedTaskIsSkippedWithoutCrashing() = runBlocking {
        val dao = FakePomodoroDao(listOf(TASK_ID))
        val repository = PomodoroRepository(dao)
        dao.removeTask(TASK_ID)

        val saved = repository.recordCompletedSession(record(SessionType.FOCUS))

        assertFalse("Task không còn tồn tại thì không ghi phiên", saved)
        assertTrue("Không được tạo bản ghi mồ côi vi phạm khoá ngoại", dao.sessions.isEmpty())
        assertEquals(0, dao.completedPomodoros(TASK_ID))
        assertEquals(0, dao.totalFocusMinutes(TASK_ID))
    }
}
