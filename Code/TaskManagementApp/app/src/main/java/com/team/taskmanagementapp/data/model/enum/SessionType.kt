package com.team.taskmanagementapp.data.model.enums

/**
 * Loại phiên làm việc của Pomodoro Timer.
 *
 * - FOCUS: phiên tập trung chính (mặc định 25 phút)
 * - SHORT_BREAK: nghỉ ngắn giữa các phiên tập trung (mặc định 5 phút)
 * - LONG_BREAK: nghỉ dài sau khi hoàn thành đủ số chu kỳ (mặc định 15 phút)
 */
enum class SessionType {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}
