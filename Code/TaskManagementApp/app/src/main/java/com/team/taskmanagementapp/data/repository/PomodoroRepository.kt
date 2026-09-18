package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.PomodoroDao
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.model.stats.TaskFocusStats
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale

/**
 * Repository cho tính năng Pomodoro Timer.
 *
 * Vai trò:
 * - Bọc `PomodoroDao` theo đúng pattern của `TaskRepository` (class thuần, không interface).
 * - Chuyển các truy vấn thời gian thô của DAO thành API ngữ nghĩa
 *   ("hôm nay", "tuần này") bằng cách tính biên ngày/tuần ở tầng Kotlin
 *   (nhất quán với cách `StatsViewModel` / `TaskRepository.getFilteredTasks` đang làm).
 *
 * Khởi tạo từ ViewModel:
 * ```
 * val pomodoroRepository = PomodoroRepository(AppDatabase.getInstance(context).pomodoroDao())
 * ```
 */
class PomodoroRepository(
    private val pomodoroDao: PomodoroDao
) {

    // ══════════════════════════════════════════════════════════════════════════
    // Ghi & xoá
    // ══════════════════════════════════════════════════════════════════════════

    /** Lưu một phiên Pomodoro mới, trả về id vừa tạo. */
    suspend fun insertSession(session: PomodoroSession): Long =
        pomodoroDao.insertSession(session)

    /** Bulk insert dùng cho restore JSON backup. */
    suspend fun insertSessions(sessions: List<PomodoroSession>): List<Long> =
        pomodoroDao.insertSessions(sessions)

    suspend fun deleteSession(session: PomodoroSession) =
        pomodoroDao.deleteSession(session)

    suspend fun deleteSessionsByTaskId(taskId: Long) =
        pomodoroDao.deleteSessionsByTaskId(taskId)

    suspend fun deleteAllSessions() =
        pomodoroDao.deleteAllSessions()

    // ══════════════════════════════════════════════════════════════════════════
    // Theo Task (Task Detail / Task Selector)
    // ══════════════════════════════════════════════════════════════════════════

    /** Toàn bộ phiên (FOCUS + nghỉ) của một task, mới nhất trước. */
    fun observeSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
        pomodoroDao.observeSessionsByTaskId(taskId)

    suspend fun getSessionsByTaskId(taskId: Long): List<PomodoroSession> =
        pomodoroDao.getSessionsByTaskId(taskId)

    /** Chỉ các phiên tập trung của một task. */
    fun observeFocusSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> =
        pomodoroDao.observeFocusSessionsByTaskId(taskId)

    suspend fun getFocusSessionsByTaskId(taskId: Long): List<PomodoroSession> =
        pomodoroDao.getFocusSessionsByTaskId(taskId)

    /** Số phiên tập trung đã hoàn thành của một task. */
    fun observeCompletedFocusSessionCountByTaskId(taskId: Long): Flow<Int> =
        pomodoroDao.observeCompletedFocusSessionCountByTaskId(taskId)

    suspend fun getCompletedFocusSessionCountByTaskId(taskId: Long): Int =
        pomodoroDao.getCompletedFocusSessionCountByTaskId(taskId)

    /** Tổng số phút tập trung đã tích luỹ cho một task. */
    suspend fun getTotalFocusMinutesByTaskId(taskId: Long): Int =
        pomodoroDao.getTotalFocusMinutesByTaskId(taskId)

    // ══════════════════════════════════════════════════════════════════════════
    // Tổng hợp toàn bộ lịch sử
    // ══════════════════════════════════════════════════════════════════════════

    /** Tổng số phiên tập trung đã hoàn thành (all time). */
    fun observeTotalCompletedFocusSessions(): Flow<Int> =
        pomodoroDao.observeTotalCompletedFocusSessions()

    suspend fun getTotalCompletedFocusSessions(): Int =
        pomodoroDao.getTotalCompletedFocusSessions()

    // ══════════════════════════════════════════════════════════════════════════
    // Theo khoảng thời gian (Statistics)
    // ══════════════════════════════════════════════════════════════════════════

    /** Tổng số phút tập trung trong khoảng [startTime, endTime] (inclusive). */
    fun observeTotalFocusMinutesInRange(startTime: Long, endTime: Long): Flow<Int> =
        pomodoroDao.observeTotalFocusMinutesInRange(startTime, endTime)

    suspend fun getTotalFocusMinutesInRange(startTime: Long, endTime: Long): Int =
        pomodoroDao.getTotalFocusMinutesInRange(startTime, endTime)

    /** Số phiên tập trung đã hoàn thành trong khoảng thời gian. */
    fun observeFocusSessionCountInRange(startTime: Long, endTime: Long): Flow<Int> =
        pomodoroDao.observeFocusSessionCountInRange(startTime, endTime)

    suspend fun getFocusSessionCountInRange(startTime: Long, endTime: Long): Int =
        pomodoroDao.getFocusSessionCountInRange(startTime, endTime)

    /** Các phiên tập trung thô trong khoảng thời gian (để gom nhóm theo ngày ở tầng Kotlin). */
    fun observeFocusSessionsInRange(startTime: Long, endTime: Long): Flow<List<PomodoroSession>> =
        pomodoroDao.observeFocusSessionsInRange(startTime, endTime)

    suspend fun getFocusSessionsInRange(startTime: Long, endTime: Long): List<PomodoroSession> =
        pomodoroDao.getFocusSessionsInRange(startTime, endTime)

    /** Thời gian tập trung nhóm theo từng Task (card "Top task" ở Statistics). */
    fun observeFocusStatsByTask(startTime: Long, endTime: Long): Flow<List<TaskFocusStats>> =
        pomodoroDao.observeFocusStatsByTask(startTime, endTime)

    suspend fun getFocusStatsByTask(startTime: Long, endTime: Long): List<TaskFocusStats> =
        pomodoroDao.getFocusStatsByTask(startTime, endTime)

    // ══════════════════════════════════════════════════════════════════════════
    // Tiện ích "hôm nay" / "tuần này"
    // ══════════════════════════════════════════════════════════════════════════

    /** Tổng số phút tập trung hôm nay. */
    fun observeTodayFocusMinutes(): Flow<Int> {
        val range = getDayRange(System.currentTimeMillis())
        return pomodoroDao.observeTotalFocusMinutesInRange(range.first, range.last)
    }

    suspend fun getTodayFocusMinutes(): Int {
        val range = getDayRange(System.currentTimeMillis())
        return pomodoroDao.getTotalFocusMinutesInRange(range.first, range.last)
    }

    /** Tổng số phút tập trung trong tuần hiện tại (bắt đầu từ Thứ Hai). */
    fun observeThisWeekFocusMinutes(): Flow<Int> {
        val range = getWeekRange(System.currentTimeMillis())
        return pomodoroDao.observeTotalFocusMinutesInRange(range.first, range.last)
    }

    suspend fun getThisWeekFocusMinutes(): Int {
        val range = getWeekRange(System.currentTimeMillis())
        return pomodoroDao.getTotalFocusMinutesInRange(range.first, range.last)
    }

    /** Số phiên tập trung đã hoàn thành trong tuần hiện tại. */
    fun observeThisWeekFocusSessionCount(): Flow<Int> {
        val range = getWeekRange(System.currentTimeMillis())
        return pomodoroDao.observeFocusSessionCountInRange(range.first, range.last)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Phiên dở dang / mới nhất
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Các phiên chưa hoàn thành (bị ngắt do app bị kill hoặc người dùng Stop).
     * PomodoroService dùng để dọn dẹp / phục hồi trạng thái khi khởi động lại.
     */
    suspend fun getIncompleteSessions(): List<PomodoroSession> =
        pomodoroDao.getIncompleteSessions()

    suspend fun getLatestSession(): PomodoroSession? =
        pomodoroDao.getLatestSession()

    // ══════════════════════════════════════════════════════════════════════════
    // Biên thời gian (dùng chung với Statistics UI)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Biên của ngày chứa [millis]: `[00:00:00.000, 23:59:59.999]` (inclusive).
     */
    fun getDayRange(millis: Long): LongRange {
        val start = DateTimeUtils.getStartOfDay(millis)
        return start..(start + MILLIS_PER_DAY - 1)
    }

    /**
     * Biên của tuần chứa [millis], bắt đầu từ Thứ Hai:
     * `[Thứ Hai 00:00:00.000, Chủ Nhật 23:59:59.999]` (inclusive).
     */
    fun getWeekRange(millis: Long): LongRange {
        val calendar = Calendar.getInstance(Locale.getDefault()).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = millis
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        return start..(start + 7 * MILLIS_PER_DAY - 1)
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
