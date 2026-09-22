package com.team.taskmanagementapp.pomodoro

/**
 * Trạng thái của Pomodoro Timer.
 *
 * Luồng chuyển trạng thái:
 * ```
 *            start()                pause()              resume()
 *   IDLE ───────────────► RUNNING ◄──────────► PAUSED ──────────► RUNNING
 *     ▲                      │                   │
 *     │                      │ hết giờ / skip()  │ skip()
 *     │                      ▼                   ▼
 *     │                  COMPLETED ◄─────────────┘
 *     │                      │
 *     │   reset()/stop()     │ start()/resume()/skip() = bắt đầu phiên kế tiếp
 *     └──────────────────────┴──────────────► RUNNING (phiên kế tiếp)
 * ```
 *
 * - [IDLE]: chưa có phiên nào chạy (trạng thái ban đầu / sau khi Stop).
 * - [RUNNING]: đang đếm ngược.
 * - [PAUSED]: tạm dừng, thời gian còn lại được "đóng băng".
 * - [COMPLETED]: phiên vừa kết thúc (hết giờ hoặc bị skip), đang chờ chuyển sang
 *   phiên kế tiếp. `PomodoroSnapshot.nextSessionType` cho biết phiên kế tiếp là gì.
 */
enum class PomodoroTimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}
