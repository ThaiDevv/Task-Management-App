package com.team.taskmanagementapp.pomodoro

import com.team.taskmanagementapp.data.model.enums.SessionType
import java.util.Locale

/**
 * Bản ghi một phiên Pomodoro đã kết thúc — nguyên liệu để lưu vào bảng
 * `pomodoro_sessions` (việc lưu DB do ViewModel/Service thực hiện, KHÔNG phải engine).
 *
 * Lưu ý về hai loại "thời gian":
 * - Đếm ngược dùng `elapsedRealtime` (đồng hồ đơn điệu, không bị ảnh hưởng khi
 *   người dùng đổi giờ hệ thống hoặc thiết bị sleep).
 * - Lưu DB dùng wall clock (`System.currentTimeMillis()`) để hiển thị/thống kê.
 *
 * @param taskId task gắn với phiên (null nếu tập trung không gắn task)
 * @param durationMinutes số phút của phiên: bằng thời lượng cấu hình nếu hoàn thành,
 *        bằng số phút thực tế đã chạy nếu bị skip
 * @param isCompleted true nếu phiên chạy hết giờ, false nếu bị skip/dừng giữa chừng
 */
data class CompletedSessionRecord(
    val taskId: Long?,
    val sessionType: SessionType,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean
)

/**
 * Toàn bộ trạng thái của Pomodoro Timer tại một thời điểm (immutable).
 *
 * Được phát ra qua `PomodoroTimerEngine.snapshot` (StateFlow) để Service và
 * ViewModel quan sát — UI không bao giờ tự tính toán trạng thái.
 *
 * @param state trạng thái hiện tại
 * @param config cấu hình đang áp dụng (UI dùng để render số chấm cycle, tổng thời lượng…)
 * @param sessionType phiên đang chạy / vừa kết thúc / sắp bắt đầu (khi IDLE)
 * @param nextSessionType phiên sẽ bắt đầu tiếp theo
 * @param taskId task đang được tập trung
 * @param remainingMillis thời gian còn lại đã tính tại lần `tick()` gần nhất
 * @param totalMillis tổng thời lượng của phiên hiện tại
 * @param targetEndElapsedRealtime mốc kết thúc theo `elapsedRealtime` — **nguồn sự thật**
 *        để chống drift. Bằng 0 khi không chạy (PAUSED/COMPLETED/IDLE).
 * @param focusSessionsInCurrentSet số phiên FOCUS đã xong trong đợt hiện tại,
 *        dùng cho Cycle Dots (0..`config.cyclesBeforeLongBreak`)
 * @param completedFocusSessions tổng số phiên FOCUS đã hoàn thành (không tính skip)
 * @param completedSessions tổng số phiên đã hoàn thành, gồm cả nghỉ
 * @param lastCompletedSession phiên vừa kết thúc, để tầng trên lưu vào Room
 * @param completionId tăng dần sau mỗi phiên kết thúc, giúp tầng trên khử trùng lặp
 *        khi lưu DB (chỉ lưu khi `completionId` thay đổi)
 */
data class PomodoroSnapshot(
    val state: PomodoroTimerState = PomodoroTimerState.IDLE,
    val config: PomodoroConfig = PomodoroConfig(),
    val sessionType: SessionType = SessionType.FOCUS,
    val nextSessionType: SessionType = SessionType.FOCUS,
    val taskId: Long? = null,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L,
    val targetEndElapsedRealtime: Long = 0L,
    val focusSessionsInCurrentSet: Int = 0,
    val completedFocusSessions: Int = 0,
    val completedSessions: Int = 0,
    val lastCompletedSession: CompletedSessionRecord? = null,
    val completionId: Int = 0
) {

    /** Đang có phiên chạy hoặc đang tạm dừng. */
    val isActive: Boolean
        get() = state == PomodoroTimerState.RUNNING || state == PomodoroTimerState.PAUSED

    /** Phiên vừa kết thúc có phải là phiên tập trung đã chạy hết giờ. */
    val lastCompletedWasFocus: Boolean
        get() = lastCompletedSession
            ?.let { it.isCompleted && it.sessionType == SessionType.FOCUS } == true

    /** Tổng số phiên tập trung của một chu kỳ (số chấm của Cycle Dots). */
    val totalCycles: Int
        get() = config.cyclesBeforeLongBreak.coerceAtLeast(1)

    /**
     * Chỉ số (1-based) của chu kỳ hiện tại, dùng cho nhãn "Chu kỳ 2/4" và Cycle Dots.
     *
     * - Đang chạy/đang tạm dừng một phiên FOCUS: chu kỳ hiện tại là phiên đang chạy
     *   (`focusSessionsInCurrentSet + 1`).
     * - Đang nghỉ hoặc phiên vừa kết thúc / IDLE: số phiên FOCUS đã hoàn thành trong chu kỳ.
     */
    val currentCycle: Int
        get() {
            val completedInSet = focusSessionsInCurrentSet.coerceIn(0, totalCycles)
            val isFocusRunning = sessionType == SessionType.FOCUS &&
                (state == PomodoroTimerState.RUNNING || state == PomodoroTimerState.PAUSED)
            return if (isFocusRunning) {
                (completedInSet + 1).coerceIn(1, totalCycles)
            } else {
                completedInSet
            }
        }

    /**
     * Thời gian còn lại tại mốc [nowElapsedRealtime].
     *
     * Khi đang RUNNING, giá trị được tính lại từ [targetEndElapsedRealtime] nên
     * không bị drift dù `tick()` có bị trễ (app bị background, CPU sleep).
     */
    fun remainingMillisAt(nowElapsedRealtime: Long): Long =
        if (state == PomodoroTimerState.RUNNING && targetEndElapsedRealtime > 0L) {
            (targetEndElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)
        } else {
            remainingMillis
        }

    /** Tiến độ 0f..1f để vẽ vòng tròn progress. */
    val progressFraction: Float
        get() = if (totalMillis <= 0L) {
            0f
        } else {
            ((totalMillis - remainingMillis).toFloat() / totalMillis.toFloat())
                .coerceIn(0f, 1f)
        }

    /**
     * Thời lượng hiển thị trên đồng hồ chính.
     *
     * Khi chưa chạy phiên nào (IDLE) thì xem trước độ dài phiên FOCUS theo cấu hình
     * (Pomodoro Settings) — nhờ đó người dùng thấy ngay thay đổi cài đặt trước khi Start.
     * Các trạng thái khác dùng thời gian còn lại thật.
     */
    val displayMillis: Long
        get() = if (state == PomodoroTimerState.IDLE) {
            config.durationMillisFor(SessionType.FOCUS)
        } else {
            remainingMillis
        }

    /** Chuỗi "MM:SS" (làm tròn lên) của thời gian còn lại — dùng cho notification. */
    val formattedRemaining: String
        get() = formatMillis(remainingMillis)

    /** Chuỗi "MM:SS" của [displayMillis] — dùng cho đồng hồ trên màn hình Pomodoro. */
    val formattedDisplay: String
        get() = formatMillis(displayMillis)

    private fun formatMillis(millis: Long): String {
        val totalSeconds = (millis + 999L) / 1000L
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }
}
