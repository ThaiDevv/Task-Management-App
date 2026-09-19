package com.team.taskmanagementapp.ui.viewmodel

import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.pomodoro.CompletedSessionRecord
import com.team.taskmanagementapp.pomodoro.PomodoroSnapshot
import com.team.taskmanagementapp.pomodoro.PomodoroTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kiểm tra quy tắc đồng bộ công việc hiển thị giữa phiên đang chạy và lựa chọn chờ (Task 12).
 *
 * Hàm [resolveDisplayedTaskId] là hàm thuần nên kiểm tra được trên JVM; đây là quy tắc duy nhất
 * quyết định thẻ công việc trên màn hình Pomodoro hiển thị gì.
 */
class PomodoroDisplayedTaskTest {

    private fun idle(taskId: Long? = null) = PomodoroSnapshot(
        state = PomodoroTimerState.IDLE,
        taskId = taskId
    )

    private fun running(taskId: Long?) = PomodoroSnapshot(
        state = PomodoroTimerState.RUNNING,
        sessionType = SessionType.FOCUS,
        taskId = taskId,
        totalMillis = 25 * 60_000L,
        remainingMillis = 25 * 60_000L
    )

    private fun paused(taskId: Long?) = running(taskId).copy(state = PomodoroTimerState.PAUSED)

    private fun completed(taskId: Long?) = PomodoroSnapshot(
        state = PomodoroTimerState.COMPLETED,
        sessionType = SessionType.FOCUS,
        nextSessionType = SessionType.SHORT_BREAK,
        taskId = taskId,
        lastCompletedSession = CompletedSessionRecord(
            taskId = taskId,
            sessionType = SessionType.FOCUS,
            startTimeMillis = 0L,
            endTimeMillis = 1_500_000L,
            durationMinutes = 25,
            isCompleted = true
        )
    )

    @Test
    fun testIdleShowsPendingSelection() {
        assertEquals(7L, resolveDisplayedTaskId(idle(taskId = null), pendingTaskId = 7L))
    }

    @Test
    fun testIdleWithoutSelectionShowsNothing() {
        assertNull(resolveDisplayedTaskId(idle(), pendingTaskId = null))
    }

    @Test
    fun testRunningSessionAlwaysWins() {
        // Phiên đang chạy gắn với task 3 -> phải hiển thị task 3 dù lựa chọn chờ khác/null.
        assertEquals(3L, resolveDisplayedTaskId(running(taskId = 3L), pendingTaskId = 9L))
        assertEquals(3L, resolveDisplayedTaskId(running(taskId = 3L), pendingTaskId = null))
    }

    @Test
    fun testPausedSessionKeepsItsTask() {
        assertEquals(5L, resolveDisplayedTaskId(paused(taskId = 5L), pendingTaskId = null))
    }

    @Test
    fun testCompletedSessionKeepsItsTask() {
        assertEquals(4L, resolveDisplayedTaskId(completed(taskId = 4L), pendingTaskId = 11L))
    }

    @Test
    fun testPendingSelectionIsUsedWhenSessionHasNoTask() {
        // Trường hợp phiên được start không kèm công việc (ví dụ từ notification/adb).
        assertEquals(8L, resolveDisplayedTaskId(running(taskId = null), pendingTaskId = 8L))
        assertNull(resolveDisplayedTaskId(running(taskId = null), pendingTaskId = null))
    }
}
