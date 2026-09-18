package com.team.taskmanagementapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.team.taskmanagementapp.pomodoro.PomodoroService
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import com.team.taskmanagementapp.pomodoro.PomodoroTimerController
import com.team.taskmanagementapp.pomodoro.PomodoroTimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel tối thiểu cho Pomodoro Timer Screen (Task 9).
 *
 * ViewModel này **không** giữ timer, không đếm ngược và không sao chép logic của
 * `PomodoroService`. Nó chỉ làm 2 việc:
 * 1. Expose `StateFlow<PomodoroSnapshot>` của [PomodoroTimerController] cho UI observe —
 *    đúng cùng một nguồn state mà Service đang cập nhật mỗi giây.
 * 2. Chuyển thao tác của người dùng thành lệnh cho [PomodoroService] (start/pause/resume/
 *    skip/stop). Không tự gọi engine, để Service luôn là nơi duy nhất điều khiển timer
 *    và giữ foreground notification đồng bộ.
 */
class PomodoroViewModel(
    private val appContext: Context
) : ViewModel() {

    /**
     * State của timer. Dùng getter (không cache) để luôn trỏ tới engine hiện hành —
     * kể cả khi sau này task recovery gọi `PomodoroTimerController.install(...)`.
     */
    val uiState: StateFlow<PomodoroSnapshot>
        get() = PomodoroTimerController.state

    /**
     * Nút hành động chính, nhãn thay đổi theo state:
     * IDLE → "Bắt đầu", PAUSED → "Tiếp tục", COMPLETED → "Phiên tiếp theo".
     */
    fun onPrimaryAction() {
        when (PomodoroTimerController.snapshot.state) {
            PomodoroTimerState.IDLE, PomodoroTimerState.COMPLETED ->
                PomodoroService.start(appContext)

            PomodoroTimerState.PAUSED -> PomodoroService.resume(appContext)

            // Đang chạy thì nút chính không có tác dụng (đã bị disable trên UI).
            PomodoroTimerState.RUNNING -> Unit
        }
    }

    fun onPauseClicked() {
        PomodoroService.pause(appContext)
    }

    fun onSkipClicked() {
        PomodoroService.skip(appContext)
    }

    fun onStopClicked() {
        PomodoroService.stop(appContext)
    }
}
