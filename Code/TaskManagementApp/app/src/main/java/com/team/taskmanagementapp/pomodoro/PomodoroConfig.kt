package com.team.taskmanagementapp.pomodoro

import com.team.taskmanagementapp.data.model.enums.SessionType

/**
 * Cấu hình thời lượng cho Pomodoro Timer.
 *
 * Toàn bộ thời lượng đều cấu hình được (Pomodoro Settings), giá trị mặc định
 * theo chuẩn Pomodoro: FOCUS 25' → SHORT_BREAK 5' → … → LONG_BREAK 15'.
 *
 * @param focusMinutes độ dài phiên tập trung, mặc định 25 phút (hợp lệ 15–60)
 * @param shortBreakMinutes độ dài nghỉ ngắn, mặc định 5 phút (hợp lệ 3–10)
 * @param longBreakMinutes độ dài nghỉ dài, mặc định 15 phút (hợp lệ 10–30)
 * @param cyclesBeforeLongBreak số phiên FOCUS trước khi tới nghỉ dài, mặc định 4
 * @param autoStartBreaks tự động chạy phiên nghỉ ngay khi phiên tập trung kết thúc
 * @param autoStartFocus tự động chạy phiên tập trung ngay khi phiên nghỉ kết thúc
 *
 * Ghi chú: [SessionType] là kiểu ĐƯỢC LƯU vào Room nên nằm ở `data/model/enums`,
 * các kiểu chỉ tồn tại lúc chạy (state machine) nằm trong package `pomodoro`.
 */
data class PomodoroConfig(
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val shortBreakMinutes: Int = DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val cyclesBeforeLongBreak: Int = DEFAULT_CYCLES_BEFORE_LONG_BREAK,
    val autoStartBreaks: Boolean = DEFAULT_AUTO_START_BREAKS,
    val autoStartFocus: Boolean = DEFAULT_AUTO_START_FOCUS
) {

    /** Thời lượng (phút) của một loại phiên. */
    fun durationMinutesFor(sessionType: SessionType): Int = when (sessionType) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    }

    /** Thời lượng (milliseconds) của một loại phiên. */
    fun durationMillisFor(sessionType: SessionType): Long =
        durationMinutesFor(sessionType) * MILLIS_PER_MINUTE

    /** Có tự động bắt đầu [sessionType] khi phiên trước kết thúc hay không. */
    fun autoStartEnabledFor(sessionType: SessionType): Boolean = when (sessionType) {
        SessionType.FOCUS -> autoStartFocus
        SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> autoStartBreaks
    }

    /**
     * Chuẩn hoá các giá trị không hợp lệ (<= 0) về biên an toàn.
     * Dùng trước khi đưa config vào [PomodoroTimerEngine] để timer không bao giờ
     * chạy với thời lượng 0 hoặc số cycle bằng 0.
     */
    fun withValidDurations(): PomodoroConfig = copy(
        focusMinutes = focusMinutes.coerceIn(FOCUS_MINUTES_RANGE),
        shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_BREAK_MINUTES_RANGE),
        longBreakMinutes = longBreakMinutes.coerceIn(LONG_BREAK_MINUTES_RANGE),
        cyclesBeforeLongBreak = cyclesBeforeLongBreak.coerceIn(
            MIN_CYCLES_BEFORE_LONG_BREAK,
            MAX_CYCLES_BEFORE_LONG_BREAK
        )
    )

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L

        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_CYCLES_BEFORE_LONG_BREAK = 4
        const val DEFAULT_AUTO_START_BREAKS = true
        const val DEFAULT_AUTO_START_FOCUS = false

        /** Biên cho phép của màn hình Pomodoro Settings. */
        val FOCUS_MINUTES_RANGE = 15..60
        val SHORT_BREAK_MINUTES_RANGE = 3..10
        val LONG_BREAK_MINUTES_RANGE = 10..30
        const val MIN_CYCLES_BEFORE_LONG_BREAK = 1
        const val MAX_CYCLES_BEFORE_LONG_BREAK = 12

        /** Bộ cấu hình chuẩn: 25 / 5 / 15, 4 cycle. */
        val DEFAULT = PomodoroConfig()
    }
}
