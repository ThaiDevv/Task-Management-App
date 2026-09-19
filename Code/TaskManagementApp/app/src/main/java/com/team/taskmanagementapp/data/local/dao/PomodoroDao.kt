package com.team.taskmanagementapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.model.stats.TaskFocusStats
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho bảng `pomodoro_sessions` (lịch sử các phiên Pomodoro).
 *
 * Convention của project:
 * - Ghi dữ liệu (insert/delete)  → `suspend`
 * - Đọc để quan sát liên tục     → trả về `Flow` (tên bắt đầu bằng `observe`)
 * - Đọc một lần (export, service, tính toán) → `suspend` (tên bắt đầu bằng `get`)
 *
 * Mọi query thống kê đều:
 * - chỉ tính các phiên `sessionType = 'FOCUS'` (bỏ qua SHORT_BREAK / LONG_BREAK)
 * - chỉ tính các phiên `isCompleted = 1` (phiên bị ngắt giữa chừng không được tính)
 * - nhận cặp `[startTime, endTime]` dạng inclusive (BETWEEN), khớp convention
 *   `TaskDao.getTasksByDateRange`; biên ngày/tuần được tính ở Repository.
 */
@Dao
interface PomodoroDao {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Ghi & xoá
    // ══════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSession): Long

    /**
     * Ghi một phiên đã hoàn thành và (chỉ với phiên FOCUS) cộng dồn thống kê vào Task,
     * tất cả trong **một transaction** nên không thể rơi vào trạng thái nửa vời.
     *
     * @param focusMinutes số phút cần cộng vào `tasks.completedPomodoros` +
     *        `tasks.totalFocusTimeMinutes`; truyền `null` cho phiên nghỉ (không đụng thống kê Task).
     * @return `false` nếu Task không còn tồn tại — khi đó KHÔNG ghi phiên, tránh vi phạm
     *         khoá ngoại `pomodoro_sessions.taskId -> tasks.id`.
     */
    @Transaction
    suspend fun recordCompletedSession(
        session: PomodoroSession,
        focusMinutes: Int?
    ): Boolean {
        if (findTaskId(session.taskId) == null) return false

        insertSession(session)

        if (focusMinutes != null) {
            addCompletedFocusSessionStats(session.taskId, focusMinutes)
        }
        return true
    }

    /** Có tồn tại Task với id này không (dùng để tránh vi phạm khoá ngoại). */
    @Query("SELECT id FROM tasks WHERE id = :taskId")
    suspend fun findTaskId(taskId: Long): Int?

    /**
     * Cộng dồn thống kê Pomodoro cho một Task bằng **một câu UPDATE duy nhất**
     * (`completedPomodoros + 1`, `totalFocusTimeMinutes + :focusMinutes`).
     *
     * Cố ý KHÔNG cập nhật `updatedAt`: đây là số liệu cộng dồn, không phải người dùng sửa Task.
     * `StatsViewModel` dùng `updatedAt` làm mốc thời gian cho task đã hoàn thành, nên nếu
     * cập nhật nó thì một phiên Pomodoro sẽ vô tình đẩy Task sang kỳ thống kê khác.
     */
    @Query(
        """
        UPDATE tasks
        SET completedPomodoros = completedPomodoros + 1,
            totalFocusTimeMinutes = totalFocusTimeMinutes + :focusMinutes
        WHERE id = :taskId
        """
    )
    suspend fun addCompletedFocusSessionStats(taskId: Long, focusMinutes: Int): Int

    /** Bulk insert dùng cho restore JSON backup (giữ nguyên ID). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<PomodoroSession>): List<Long>

    @Delete
    suspend fun deleteSession(session: PomodoroSession)

    @Query("DELETE FROM pomodoro_sessions WHERE taskId = :taskId")
    suspend fun deleteSessionsByTaskId(taskId: Long)

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun deleteAllSessions()

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Truy vấn theo Task
    // ══════════════════════════════════════════════════════════════════════════

    /** Toàn bộ phiên (FOCUS + nghỉ) của một task, mới nhất trước. */
    @Query("SELECT * FROM pomodoro_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun observeSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>

    @Query("SELECT * FROM pomodoro_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    suspend fun getSessionsByTaskId(taskId: Long): List<PomodoroSession>

    /** Chỉ các phiên tập trung của một task. */
    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE taskId = :taskId AND sessionType = 'FOCUS' ORDER BY startTime DESC"
    )
    fun observeFocusSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>>

    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE taskId = :taskId AND sessionType = 'FOCUS' ORDER BY startTime DESC"
    )
    suspend fun getFocusSessionsByTaskId(taskId: Long): List<PomodoroSession>

    /** Số phiên tập trung đã hoàn thành của một task (dùng cho Task Detail). */
    @Query(
        "SELECT COUNT(*) FROM pomodoro_sessions " +
            "WHERE taskId = :taskId AND sessionType = 'FOCUS' AND isCompleted = 1"
    )
    fun observeCompletedFocusSessionCountByTaskId(taskId: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pomodoro_sessions " +
            "WHERE taskId = :taskId AND sessionType = 'FOCUS' AND isCompleted = 1"
    )
    suspend fun getCompletedFocusSessionCountByTaskId(taskId: Long): Int

    /** Tổng số phút tập trung đã tích luỹ cho một task. */
    @Query(
        "SELECT IFNULL(SUM(durationInMinutes), 0) FROM pomodoro_sessions " +
            "WHERE taskId = :taskId AND sessionType = 'FOCUS' AND isCompleted = 1"
    )
    suspend fun getTotalFocusMinutesByTaskId(taskId: Long): Int

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Thống kê tổng hợp theo khoảng thời gian
    // ══════════════════════════════════════════════════════════════════════════

    /** Tổng số phiên tập trung đã hoàn thành trong toàn bộ lịch sử. */
    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1")
    fun observeTotalCompletedFocusSessions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1")
    suspend fun getTotalCompletedFocusSessions(): Int

    /** Tổng số phút tập trung trong khoảng [startTime, endTime] (dùng cho "hôm nay"/"tuần này"). */
    @Query(
        "SELECT IFNULL(SUM(durationInMinutes), 0) FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime"
    )
    fun observeTotalFocusMinutesInRange(startTime: Long, endTime: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(durationInMinutes), 0) FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime"
    )
    suspend fun getTotalFocusMinutesInRange(startTime: Long, endTime: Long): Int

    /** Số phiên tập trung đã hoàn thành trong khoảng thời gian. */
    @Query(
        "SELECT COUNT(*) FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime"
    )
    fun observeFocusSessionCountInRange(startTime: Long, endTime: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime"
    )
    suspend fun getFocusSessionCountInRange(startTime: Long, endTime: Long): Int

    /**
     * Các phiên tập trung thô trong khoảng thời gian (cũ nhất trước).
     * Dùng để vẽ biểu đồ / gom nhóm theo ngày ở tầng Kotlin.
     */
    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime ASC"
    )
    fun observeFocusSessionsInRange(startTime: Long, endTime: Long): Flow<List<PomodoroSession>>

    @Query(
        "SELECT * FROM pomodoro_sessions " +
            "WHERE sessionType = 'FOCUS' AND isCompleted = 1 " +
            "AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime ASC"
    )
    suspend fun getFocusSessionsInRange(startTime: Long, endTime: Long): List<PomodoroSession>

    /**
     * Thời gian tập trung nhóm theo từng Task trong khoảng thời gian
     * (phục vụ card "Top task tập trung nhiều nhất" ở Statistics).
     */
    @Query(
        """
        SELECT taskId AS taskId,
               IFNULL(SUM(durationInMinutes), 0) AS totalMinutes,
               COUNT(*) AS sessionCount
        FROM pomodoro_sessions
        WHERE sessionType = 'FOCUS' AND isCompleted = 1
          AND startTime BETWEEN :startTime AND :endTime
        GROUP BY taskId
        ORDER BY totalMinutes DESC
        """
    )
    suspend fun getFocusStatsByTask(startTime: Long, endTime: Long): List<TaskFocusStats>

    @Query(
        """
        SELECT taskId AS taskId,
               IFNULL(SUM(durationInMinutes), 0) AS totalMinutes,
               COUNT(*) AS sessionCount
        FROM pomodoro_sessions
        WHERE sessionType = 'FOCUS' AND isCompleted = 1
          AND startTime BETWEEN :startTime AND :endTime
        GROUP BY taskId
        ORDER BY totalMinutes DESC
        """
    )
    fun observeFocusStatsByTask(startTime: Long, endTime: Long): Flow<List<TaskFocusStats>>

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Phiên dở dang / mới nhất
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Các phiên chưa hoàn thành (bị ngắt do app bị kill hoặc người dùng Stop).
     * PomodoroService dùng để dọn dẹp / phục hồi trạng thái sau khi khởi động lại.
     */
    @Query("SELECT * FROM pomodoro_sessions WHERE isCompleted = 0 ORDER BY startTime DESC")
    suspend fun getIncompleteSessions(): List<PomodoroSession>

    /** Phiên mới nhất đã ghi — dùng để hiển thị "hoạt động gần nhất". */
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): PomodoroSession?

    /**
     * Toàn bộ phiên trong DB (cũ nhất trước) — dùng cho **backup JSON** (Task 16).
     *
     * Lấy cả phiên FOCUS, phiên nghỉ và phiên chưa hoàn thành để bản backup trung thực
     * với dữ liệu gốc.
     */
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startTime ASC")
    suspend fun getAllSessions(): List<PomodoroSession>
}
