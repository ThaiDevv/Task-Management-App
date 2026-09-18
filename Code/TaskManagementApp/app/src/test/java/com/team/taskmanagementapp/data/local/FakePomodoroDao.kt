package com.team.taskmanagementapp.data.local

import com.team.taskmanagementapp.data.local.dao.PomodoroDao
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.stats.TaskFocusStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake [PomodoroDao] chạy hoàn toàn trong bộ nhớ cho unit test JVM (project không có
 * Robolectric/room-testing).
 *
 * Các truy vấn được cài lại **đúng theo ngữ nghĩa SQL** trong `PomodoroDao`:
 * - Thống kê chỉ tính phiên `FOCUS` **đã hoàn thành** (`isCompleted = true`).
 * - Khoảng thời gian là inclusive trên `startTime` (tương ứng `BETWEEN :start AND :end`).
 *
 * Nhờ dùng chung một `MutableStateFlow` làm kho dữ liệu, các Flow trả về **có phản ứng**:
 * ghi thêm phiên rồi thì `observeX()` tự phát giá trị mới, giống Room.
 */
class FakePomodoroDao(vararg taskIds: Long) : PomodoroDao {

    private val existingTaskIds = taskIds.toMutableSet()
    private val sessionState = MutableStateFlow<List<PomodoroSession>>(emptyList())
    private val completedPomodoros = mutableMapOf<Long, Int>()
    private val focusMinutes = mutableMapOf<Long, Int>()
    private var nextId = 1L

    val sessions: List<PomodoroSession> get() = sessionState.value

    // ══════════════════════════════════════════════════════════════════════════
    // Helper cho test
    // ══════════════════════════════════════════════════════════════════════════

    fun addTask(taskId: Long) {
        existingTaskIds += taskId
    }

    fun removeTask(taskId: Long) {
        existingTaskIds -= taskId
    }

    fun completedPomodoros(taskId: Long): Int = completedPomodoros[taskId] ?: 0

    fun totalFocusMinutes(taskId: Long): Int = focusMinutes[taskId] ?: 0

    /** Thêm một phiên FOCUS đã hoàn thành (mặc định 25 phút). */
    fun seedCompletedFocus(
        taskId: Long,
        startTime: Long,
        minutes: Int = 25
    ): PomodoroSession = seed(
        PomodoroSession(
            taskId = taskId,
            startTime = startTime,
            endTime = startTime + minutes * MINUTE,
            durationInMinutes = minutes,
            sessionType = SessionType.FOCUS,
            isCompleted = true
        )
    )

    /** Thêm một phiên nghỉ đã hoàn thành (mặc định 5 phút). */
    fun seedCompletedBreak(
        taskId: Long,
        startTime: Long,
        sessionType: SessionType = SessionType.SHORT_BREAK,
        minutes: Int = 5
    ): PomodoroSession = seed(
        PomodoroSession(
            taskId = taskId,
            startTime = startTime,
            endTime = startTime + minutes * MINUTE,
            durationInMinutes = minutes,
            sessionType = sessionType,
            isCompleted = true
        )
    )

    /** Thêm một phiên bất kỳ (để test phiên bị skip / chưa hoàn thành). */
    fun seed(session: PomodoroSession): PomodoroSession {
        val stored = session.copy(id = nextId++)
        sessionState.value = sessionState.value + stored
        return stored
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ghi
    // ══════════════════════════════════════════════════════════════════════════

    override suspend fun insertSession(session: PomodoroSession): Long = seed(session).id

    override suspend fun insertSessions(sessions: List<PomodoroSession>): List<Long> =
        sessions.map { insertSession(it) }

    override suspend fun deleteSession(session: PomodoroSession) {
        sessionState.value = sessionState.value.filterNot { it.id == session.id }
    }

    override suspend fun deleteSessionsByTaskId(taskId: Long) {
        sessionState.value = sessionState.value.filterNot { it.taskId == taskId }
    }

    override suspend fun deleteAllSessions() {
        sessionState.value = emptyList()
    }

    override suspend fun findTaskId(taskId: Long): Int? =
        if (existingTaskIds.contains(taskId)) taskId.toInt() else null

    override suspend fun addCompletedFocusSessionStats(taskId: Long, focusMinutes: Int): Int {
        completedPomodoros[taskId] = completedPomodoros(taskId) + 1
        this.focusMinutes[taskId] = totalFocusMinutes(taskId) + focusMinutes
        return 1
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Đọc theo Task
    // ══════════════════════════════════════════════════════════════════════════

    override fun observeSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
        sessionState.map { list -> list.filter { it.taskId == taskId }.sortedByDescending { it.startTime } }

    override suspend fun getSessionsByTaskId(taskId: Long): List<PomodoroSession> =
        sessions.filter { it.taskId == taskId }.sortedByDescending { it.startTime }

    override fun observeFocusSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
        sessionState.map { list ->
            list.filter { it.taskId == taskId && it.isFocus() }.sortedByDescending { it.startTime }
        }

    override suspend fun getFocusSessionsByTaskId(taskId: Long): List<PomodoroSession> =
        sessions.filter { it.taskId == taskId && it.isFocus() }.sortedByDescending { it.startTime }

    override fun observeCompletedFocusSessionCountByTaskId(taskId: Long): Flow<Int> =
        sessionState.map { list -> list.count { it.taskId == taskId && it.isCompletedFocus() } }

    override suspend fun getCompletedFocusSessionCountByTaskId(taskId: Long): Int =
        sessions.count { it.taskId == taskId && it.isCompletedFocus() }

    override suspend fun getTotalFocusMinutesByTaskId(taskId: Long): Int =
        sessions.filter { it.taskId == taskId && it.isCompletedFocus() }.sumOf { it.durationInMinutes }

    // ══════════════════════════════════════════════════════════════════════════
    // Thống kê tổng hợp
    // ══════════════════════════════════════════════════════════════════════════

    override fun observeTotalCompletedFocusSessions(): Flow<Int> =
        sessionState.map { list -> list.count { it.isCompletedFocus() } }

    override suspend fun getTotalCompletedFocusSessions(): Int =
        sessions.count { it.isCompletedFocus() }

    override fun observeTotalFocusMinutesInRange(startTime: Long, endTime: Long): Flow<Int> =
        sessionState.map { list -> list.completedFocusInRange(startTime, endTime).sumOf { it.durationInMinutes } }

    override suspend fun getTotalFocusMinutesInRange(startTime: Long, endTime: Long): Int =
        sessions.completedFocusInRange(startTime, endTime).sumOf { it.durationInMinutes }

    override fun observeFocusSessionCountInRange(startTime: Long, endTime: Long): Flow<Int> =
        sessionState.map { list -> list.completedFocusInRange(startTime, endTime).size }

    override suspend fun getFocusSessionCountInRange(startTime: Long, endTime: Long): Int =
        sessions.completedFocusInRange(startTime, endTime).size

    override fun observeFocusSessionsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<PomodoroSession>> =
        sessionState.map { list ->
            list.completedFocusInRange(startTime, endTime).sortedBy { it.startTime }
        }

    override suspend fun getFocusSessionsInRange(
        startTime: Long,
        endTime: Long
    ): List<PomodoroSession> = sessions.completedFocusInRange(startTime, endTime).sortedBy { it.startTime }

    override suspend fun getFocusStatsByTask(
        startTime: Long,
        endTime: Long
    ): List<TaskFocusStats> = sessions.completedFocusInRange(startTime, endTime).toFocusStatsByTask()

    override fun observeFocusStatsByTask(
        startTime: Long,
        endTime: Long
    ): Flow<List<TaskFocusStats>> =
        sessionState.map { list -> list.completedFocusInRange(startTime, endTime).toFocusStatsByTask() }

    // ══════════════════════════════════════════════════════════════════════════
    // Phiên dở dang / mới nhất
    // ══════════════════════════════════════════════════════════════════════════

    override suspend fun getIncompleteSessions(): List<PomodoroSession> =
        sessions.filterNot { it.isCompleted }.sortedByDescending { it.startTime }

    override suspend fun getLatestSession(): PomodoroSession? =
        sessions.maxByOrNull { it.startTime }

    // ══════════════════════════════════════════════════════════════════════════
    // Truy vấn dùng chung
    // ══════════════════════════════════════════════════════════════════════════

    private fun List<PomodoroSession>.completedFocusInRange(
        startTime: Long,
        endTime: Long
    ): List<PomodoroSession> =
        filter { it.isCompletedFocus() && it.startTime in startTime..endTime }

    private fun List<PomodoroSession>.toFocusStatsByTask(): List<TaskFocusStats> =
        groupBy { it.taskId }
            .map { (taskId, list) ->
                TaskFocusStats(
                    taskId = taskId,
                    totalMinutes = list.sumOf { it.durationInMinutes },
                    sessionCount = list.size
                )
            }
            .sortedByDescending { it.totalMinutes }

    private fun PomodoroSession.isFocus(): Boolean = sessionType == SessionType.FOCUS

    private fun PomodoroSession.isCompletedFocus(): Boolean = isFocus() && isCompleted

    private companion object {
        const val MINUTE = 60_000L
    }
}
