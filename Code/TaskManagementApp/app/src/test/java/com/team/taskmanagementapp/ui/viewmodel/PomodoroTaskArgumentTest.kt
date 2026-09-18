package com.team.taskmanagementapp.ui.viewmodel

import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import com.team.taskmanagementapp.pomodoro.PomodoroTimerState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kiểm tra quy tắc xử lý taskId truyền từ Task Detail (Task 13).
 *
 * Quan trọng nhất: **không được thay công việc của một phiên đang chạy**. Quy tắc này nằm ở
 * [decideTaskIdArgument] — hàm thuần nên kiểm tra được trên JVM.
 */
class PomodoroTaskArgumentTest {

    private fun snapshot(state: PomodoroTimerState, taskId: Long?) = PomodoroSnapshot(
        state = state,
        sessionType = SessionType.FOCUS,
        taskId = taskId,
        totalMillis = 25 * 60_000L,
        remainingMillis = 25 * 60_000L
    )

    @Test
    fun testRequestingSameTaskAsPendingIsNoOp() {
        // Mở màn hình với taskId đang là lựa chọn chờ -> không làm gì (tránh báo lỗi thừa).
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.IDLE, taskId = null),
            pendingTaskId = 5L,
            requestedTaskId = 5L
        )

        assertEquals(TaskIdArgumentDecision.ALREADY_SELECTED, decision)
    }

    @Test
    fun testRequestingSameTaskAsRunningSessionIsNoOp() {
        // Ví dụ: rotate màn hình khi đang chạy phiên của task 3 -> không báo gì.
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.RUNNING, taskId = 3L),
            pendingTaskId = 3L,
            requestedTaskId = 3L
        )

        assertEquals(TaskIdArgumentDecision.ALREADY_SELECTED, decision)
    }

    @Test
    fun testRunningSessionIsNotReplacedByAnotherTask() {
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.RUNNING, taskId = 3L),
            pendingTaskId = 3L,
            requestedTaskId = 9L
        )

        assertEquals(TaskIdArgumentDecision.SESSION_ACTIVE, decision)
    }

    @Test
    fun testRunningSessionWithoutTaskStillBlocksNewTask() {
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.RUNNING, taskId = null),
            pendingTaskId = null,
            requestedTaskId = 9L
        )

        assertEquals(TaskIdArgumentDecision.SESSION_ACTIVE, decision)
    }

    @Test
    fun testPausedSessionIsNotReplaced() {
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.PAUSED, taskId = 4L),
            pendingTaskId = 4L,
            requestedTaskId = 9L
        )

        assertEquals(TaskIdArgumentDecision.SESSION_ACTIVE, decision)
    }

    @Test
    fun testCompletedSessionIsNotReplaced() {
        // Chuỗi phiên vẫn tiếp tục cùng công việc cũ nên không được đổi giữa chừng.
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.COMPLETED, taskId = 4L),
            pendingTaskId = 4L,
            requestedTaskId = 9L
        )

        assertEquals(TaskIdArgumentDecision.SESSION_ACTIVE, decision)
    }

    @Test
    fun testIdleLooksUpRequestedTask() {
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.IDLE, taskId = null),
            pendingTaskId = null,
            requestedTaskId = 7L
        )

        assertEquals(TaskIdArgumentDecision.LOOKUP_TASK, decision)
    }

    @Test
    fun testIdleWithAnotherPendingSelectionLooksUpNewTask() {
        // Timer chưa chạy -> được phép đổi sang công việc vừa bấm từ Task Detail.
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.IDLE, taskId = null),
            pendingTaskId = 5L,
            requestedTaskId = 9L
        )

        assertEquals(TaskIdArgumentDecision.LOOKUP_TASK, decision)
    }

    @Test
    fun testAfterStopSameTaskIsNoOp() {
        // Sau Stop, engine xoá taskId nhưng lựa chọn chờ vẫn còn
        // -> bấm lại cùng công việc đó không gây thông báo gì.
        val decision = decideTaskIdArgument(
            snapshot = snapshot(PomodoroTimerState.IDLE, taskId = null),
            pendingTaskId = 5L,
            requestedTaskId = 5L
        )

        assertEquals(TaskIdArgumentDecision.ALREADY_SELECTED, decision)
    }
}
