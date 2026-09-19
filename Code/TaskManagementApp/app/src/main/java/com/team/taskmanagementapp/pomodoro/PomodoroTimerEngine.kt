package com.team.taskmanagementapp.pomodoro

import android.os.SystemClock
import com.team.taskmanagementapp.data.model.enums.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine + core logic của Pomodoro Timer.
 *
 * Class này KHÔNG phụ thuộc Android UI, KHÔNG tự lưu DB và KHÔNG hiển thị gì cả:
 * - `PomodoroService` (Foreground Service) sẽ giữ một instance và gọi [tick] mỗi giây.
 * - `PomodoroViewModel` chỉ observe [snapshot] và gọi các hàm điều khiển.
 *
 * ## Chống drift (quan trọng)
 * Đồng hồ **không** đếm `seconds--` mỗi giây. Khi bắt đầu một phiên, engine chốt
 * `targetEndElapsedRealtime = elapsedRealtime() + totalMillis`, và mỗi lần [tick]
 * tính lại `targetEnd - elapsedRealtime()`. Vì `elapsedRealtime` là đồng hồ đơn điệu
 * (kể cả khi CPU sleep / app bị background), thời gian hiển thị luôn đúng dù tick bị trễ.
 *
 * ## Luồng phiên chuẩn
 * ```
 * FOCUS 25' → SHORT_BREAK 5' → FOCUS 25' → SHORT_BREAK 5'
 *           → FOCUS 25' → SHORT_BREAK 5' → FOCUS 25' → LONG_BREAK 15' → (lặp lại)
 * ```
 * Cứ đủ [PomodoroConfig.cyclesBeforeLongBreak] phiên FOCUS thì chuyển sang LONG_BREAK.
 *
 * @param initialConfig cấu hình thời lượng (có thể đổi sau bằng [updateConfig])
 * @param elapsedRealtimeProvider nguồn thời gian đơn điệu, mặc định [SystemClock.elapsedRealtime]
 *        — có thể inject trong unit test
 * @param wallClockProvider nguồn giờ thực để ghi vào bản ghi session, mặc định
 *        `System.currentTimeMillis()` — có thể inject trong unit test
 */
class PomodoroTimerEngine(
    initialConfig: PomodoroConfig = PomodoroConfig(),
    private val elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallClockProvider: () -> Long = { System.currentTimeMillis() }
) {

    private var config: PomodoroConfig = initialConfig.withValidDurations()

    private val _snapshot = MutableStateFlow(PomodoroSnapshot(config = config))

    /** Trạng thái hiện tại — nguồn sự thật duy nhất cho Service và ViewModel. */
    val snapshot: StateFlow<PomodoroSnapshot> = _snapshot.asStateFlow()

    /** Ảnh chụp trạng thái hiện tại (đọc đồng bộ, tiện cho notification). */
    val currentSnapshot: PomodoroSnapshot
        get() = _snapshot.value

    /** Mốc `elapsedRealtime` phiên hiện tại bắt đầu — dùng để tính thời gian chạy thực tế. */
    private var sessionStartElapsedRealtime: Long = 0L

    /** Mốc wall clock phiên hiện tại bắt đầu — dùng để ghi `startTime` vào DB. */
    private var sessionStartWallClock: Long = 0L

    private var completionCounter: Int = 0

    // ══════════════════════════════════════════════════════════════════════════
    // Điều khiển
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Bắt đầu chạy.
     * - Từ [PomodoroTimerState.IDLE]: bắt đầu một phiên FOCUS (kèm [taskId] nếu có).
     * - Từ [PomodoroTimerState.COMPLETED]: bắt đầu phiên kế tiếp (theo
     *   [PomodoroSnapshot.nextSessionType]).
     * - Đang RUNNING/PAUSED: không làm gì (tránh start đè lên phiên đang chạy).
     */
    fun start(taskId: Long? = null) {
        val current = currentSnapshot
        when (current.state) {
            PomodoroTimerState.IDLE ->
                beginSession(SessionType.FOCUS, taskId ?: current.taskId)

            PomodoroTimerState.COMPLETED ->
                beginSession(current.nextSessionType, current.taskId)

            PomodoroTimerState.RUNNING, PomodoroTimerState.PAUSED -> Unit
        }
    }

    /**
     * Tạm dừng phiên đang chạy: "đóng băng" thời gian còn lại.
     * Chỉ có tác dụng khi đang RUNNING.
     */
    fun pause() {
        val current = currentSnapshot
        if (current.state != PomodoroTimerState.RUNNING) return

        val remaining = current.remainingMillisAt(elapsedRealtimeProvider())
        _snapshot.value = current.copy(
            state = PomodoroTimerState.PAUSED,
            remainingMillis = remaining,
            targetEndElapsedRealtime = 0L
        )
    }

    /**
     * Tiếp tục phiên đang tạm dừng: chốt lại mốc kết thúc mới từ thời gian còn lại.
     * - Từ [PomodoroTimerState.COMPLETED]: tương đương [start] (chạy phiên kế tiếp).
     * - Các trạng thái khác: không làm gì.
     */
    fun resume() {
        val current = currentSnapshot
        when (current.state) {
            PomodoroTimerState.PAUSED -> {
                val now = elapsedRealtimeProvider()
                val remaining = current.remainingMillis.coerceAtLeast(0L)
                // Dịch mốc bắt đầu phiên lên để "thời gian chạy thực tế" không tính cả lúc pause
                sessionStartElapsedRealtime = now - (current.totalMillis - remaining)
                _snapshot.value = current.copy(
                    state = PomodoroTimerState.RUNNING,
                    targetEndElapsedRealtime = now + remaining
                )
            }

            PomodoroTimerState.COMPLETED ->
                beginSession(current.nextSessionType, current.taskId)

            PomodoroTimerState.IDLE, PomodoroTimerState.RUNNING -> Unit
        }
    }

    /**
     * Bỏ qua phiên hiện tại.
     * - Đang RUNNING/PAUSED: kết thúc phiên ngay, **không** tính là đã hoàn thành,
     *   rồi chuyển sang phiên kế tiếp (vị trí cycle vẫn tiến lên).
     * - Từ [PomodoroTimerState.COMPLETED]: bắt đầu phiên kế tiếp.
     * - [PomodoroTimerState.IDLE]: không làm gì.
     */
    fun skip() {
        val current = currentSnapshot
        when (current.state) {
            PomodoroTimerState.RUNNING, PomodoroTimerState.PAUSED ->
                finishSession(completed = false)

            PomodoroTimerState.COMPLETED ->
                beginSession(current.nextSessionType, current.taskId)

            PomodoroTimerState.IDLE -> Unit
        }
    }

    /**
     * Reset / Stop: đưa timer về đúng trạng thái ban đầu ([PomodoroTimerState.IDLE]),
     * xoá tiến trình cycle, bộ đếm, task đang chọn và phiên vừa kết thúc.
     * Cấu hình [PomodoroConfig] được giữ nguyên.
     */
    fun reset() {
        sessionStartElapsedRealtime = 0L
        sessionStartWallClock = 0L
        completionCounter = 0
        _snapshot.value = PomodoroSnapshot(config = config)
    }

    /** Alias của [reset] — tên dùng cho action "Stop" trên notification. */
    fun stop() = reset()

    /**
     * Cập nhật cấu hình (Pomodoro Settings).
     *
     * Chỉ ảnh hưởng tới các phiên **bắt đầu sau đó**; phiên đang chạy giữ nguyên
     * thời lượng vì mốc `targetEnd` đã chốt.
     */
    fun updateConfig(newConfig: PomodoroConfig) {
        config = newConfig.withValidDurations()
        _snapshot.value = currentSnapshot.copy(config = config)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Vòng lặp thời gian
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cập nhật thời gian còn lại và tự động kết thúc phiên khi hết giờ.
     * Service gọi hàm này mỗi giây. Nếu bị gọi trễ (app background / CPU sleep)
     * thì giá trị vẫn đúng nhờ tính từ [PomodoroSnapshot.targetEndElapsedRealtime].
     */
    fun tick() {
        val current = currentSnapshot
        if (current.state != PomodoroTimerState.RUNNING) return

        val remaining = current.remainingMillisAt(elapsedRealtimeProvider())
        if (remaining <= 0L) {
            finishSession(completed = true)
        } else {
            _snapshot.value = current.copy(remainingMillis = remaining)
        }
    }

    /** Thời gian còn lại tính ngay tại thời điểm gọi (không cần chờ `tick`). */
    fun remainingMillisNow(): Long = currentSnapshot.remainingMillisAt(elapsedRealtimeProvider())

    // ══════════════════════════════════════════════════════════════════════════
    // Nội bộ
    // ══════════════════════════════════════════════════════════════════════════

    /** Bắt đầu một phiên mới và chốt mốc kết thúc theo `elapsedRealtime`. */
    private fun beginSession(sessionType: SessionType, taskId: Long?) {
        val current = currentSnapshot
        val now = elapsedRealtimeProvider()
        val total = config.durationMillisFor(sessionType)

        sessionStartElapsedRealtime = now
        sessionStartWallClock = wallClockProvider()

        _snapshot.value = current.copy(
            state = PomodoroTimerState.RUNNING,
            config = config,
            sessionType = sessionType,
            nextSessionType = nextSessionTypeAfter(sessionType, current.focusSessionsInCurrentSet),
            taskId = taskId,
            remainingMillis = total,
            totalMillis = total,
            targetEndElapsedRealtime = now + total
        )
    }

    /**
     * Kết thúc phiên hiện tại.
     *
     * @param completed true khi hết giờ, false khi bị skip
     */
    private fun finishSession(completed: Boolean) {
        val current = currentSnapshot
        val now = elapsedRealtimeProvider()
        val remainingBefore = current.remainingMillisAt(now).coerceIn(0L, current.totalMillis)
        val actualMinutes = ((current.totalMillis - remainingBefore) / PomodoroConfig.MILLIS_PER_MINUTE)
            .toInt()

        val record = CompletedSessionRecord(
            taskId = current.taskId,
            sessionType = current.sessionType,
            startTimeMillis = sessionStartWallClock,
            endTimeMillis = wallClockProvider(),
            durationMinutes = if (completed) {
                config.durationMinutesFor(current.sessionType)
            } else {
                actualMinutes
            },
            isCompleted = completed
        )

        completionCounter += 1

        val focusInSetBefore = current.focusSessionsInCurrentSet
        val nextType = nextSessionTypeAfter(current.sessionType, focusInSetBefore)
        val nextFocusInSet = focusSessionsInCurrentSetAfter(current.sessionType, focusInSetBefore)
        val isFocusSession = current.sessionType == SessionType.FOCUS

        _snapshot.value = current.copy(
            state = PomodoroTimerState.COMPLETED,
            sessionType = current.sessionType,
            nextSessionType = nextType,
            remainingMillis = 0L,
            targetEndElapsedRealtime = 0L,
            focusSessionsInCurrentSet = nextFocusInSet,
            completedFocusSessions = current.completedFocusSessions +
                if (completed && isFocusSession) 1 else 0,
            completedSessions = current.completedSessions + if (completed) 1 else 0,
            lastCompletedSession = record,
            completionId = completionCounter
        )

        if (config.autoStartEnabledFor(nextType)) {
            beginSession(nextType, current.taskId)
        }
    }

    /**
     * Phiên sẽ chạy sau [sessionType], với [focusSessionsInCurrentSet] là số phiên
     * FOCUS **đã hoàn thành trước khi** phiên này kết thúc.
     */
    private fun nextSessionTypeAfter(
        sessionType: SessionType,
        focusSessionsInCurrentSet: Int
    ): SessionType = when (sessionType) {
        SessionType.FOCUS ->
            if (focusSessionsInCurrentSet + 1 >= config.cyclesBeforeLongBreak) {
                SessionType.LONG_BREAK
            } else {
                SessionType.SHORT_BREAK
            }

        SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> SessionType.FOCUS
    }

    /** Số phiên FOCUS trong đợt hiện tại sau khi [sessionType] kết thúc. */
    private fun focusSessionsInCurrentSetAfter(sessionType: SessionType, before: Int): Int =
        when (sessionType) {
            SessionType.FOCUS ->
                (before + 1).coerceAtMost(config.cyclesBeforeLongBreak)

            SessionType.LONG_BREAK -> 0 // hết nghỉ dài → mở đợt 4 phiên mới

            SessionType.SHORT_BREAK -> before
        }
}
