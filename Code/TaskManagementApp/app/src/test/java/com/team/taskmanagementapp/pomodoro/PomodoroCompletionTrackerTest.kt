package com.team.taskmanagementapp.pomodoro

import com.team.taskmanagementapp.data.model.enums.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test cho [PomodoroCompletionTracker] — chứng minh yêu cầu
 * "mỗi session chỉ trigger Sound / Vibration / Completion Notification **một lần**".
 *
 * Chạy hoàn toàn trên JVM (class thuần Kotlin, không phụ thuộc Android).
 */
class PomodoroCompletionTrackerTest {

    private fun record(
        sessionType: SessionType = SessionType.FOCUS,
        isCompleted: Boolean = true
    ) = CompletedSessionRecord(
        taskId = 7L,
        sessionType = sessionType,
        startTimeMillis = 1_000L,
        endTimeMillis = 1_000L + 25 * 60_000L,
        durationMinutes = if (isCompleted) 25 else 7,
        isCompleted = isCompleted
    )

    private fun snapshot(
        completionId: Int,
        record: CompletedSessionRecord? = null
    ) = PomodoroSnapshot(completionId = completionId, lastCompletedSession = record)

    @Test
    fun testNoAlertBeforeAnySessionFinishes() {
        val tracker = PomodoroCompletionTracker()

        assertNull(tracker.consumeCompletedSession(snapshot(completionId = 0)))
    }

    @Test
    fun testCompletedFocusSessionAlertsExactlyOnce() {
        val tracker = PomodoroCompletionTracker()
        val finished = snapshot(completionId = 1, record = record(SessionType.FOCUS))

        val first = tracker.consumeCompletedSession(finished)
        assertNotNull(first)
        assertEquals(SessionType.FOCUS, first!!.sessionType)

        // Gọi lại nhiều lần với cùng snapshot (ticker + alarm + lệnh notification)
        assertNull(tracker.consumeCompletedSession(finished))
        assertNull(tracker.consumeCompletedSession(finished))
    }

    @Test
    fun testRepeatedTicksWithSameSnapshotAlertOnlyOnce() {
        val tracker = PomodoroCompletionTracker()
        val finished = snapshot(completionId = 3, record = record(SessionType.SHORT_BREAK))

        val alertCount = (1..20).count { tracker.consumeCompletedSession(finished) != null }

        assertEquals(1, alertCount)
    }

    @Test
    fun testCompletedBreakSessionAlertsOnce() {
        val tracker = PomodoroCompletionTracker()

        val shortBreak = tracker.consumeCompletedSession(
            snapshot(completionId = 2, record = record(SessionType.SHORT_BREAK))
        )
        assertEquals(SessionType.SHORT_BREAK, shortBreak!!.sessionType)
        assertNull(
            tracker.consumeCompletedSession(
                snapshot(completionId = 2, record = record(SessionType.SHORT_BREAK))
            )
        )

        val longBreak = tracker.consumeCompletedSession(
            snapshot(completionId = 3, record = record(SessionType.LONG_BREAK))
        )
        assertEquals(SessionType.LONG_BREAK, longBreak!!.sessionType)
    }

    @Test
    fun testSkippedSessionDoesNotAlertButIsMarkedHandled() {
        val tracker = PomodoroCompletionTracker()

        // Skip: completionId tăng nhưng phiên không hoàn thành -> KHÔNG phát cảnh báo
        assertNull(
            tracker.consumeCompletedSession(
                snapshot(completionId = 1, record = record(SessionType.FOCUS, isCompleted = false))
            )
        )

        // Phiên kết thúc thật sau đó vẫn phải phát đúng một lần
        val realCompletion = snapshot(completionId = 2, record = record(SessionType.FOCUS))
        assertNotNull(tracker.consumeCompletedSession(realCompletion))
        assertNull(tracker.consumeCompletedSession(realCompletion))
    }

    @Test
    fun testSyncToSuppressesAlertsForAlreadyFinishedSessions() {
        val tracker = PomodoroCompletionTracker()

        // Service được tạo lại khi phiên đã kết thúc từ trước -> không phát lại cảnh báo cũ
        tracker.syncTo(completionId = 5)
        assertNull(
            tracker.consumeCompletedSession(
                snapshot(completionId = 5, record = record(SessionType.FOCUS))
            )
        )
    }

    @Test
    fun testSessionFinishingAfterSyncStillAlerts() {
        val tracker = PomodoroCompletionTracker()
        tracker.syncTo(completionId = 5)

        val next = snapshot(completionId = 6, record = record(SessionType.FOCUS))

        // Phiên mới (sau mốc sync) phải phát cảnh báo đúng một lần
        assertEquals(1, (1..5).count { tracker.consumeCompletedSession(next) != null })
    }

    @Test
    fun testMissingRecordIsTreatedAsHandledWithoutRetry() {
        val tracker = PomodoroCompletionTracker()

        // completionId mới nhưng thiếu bản ghi -> không có gì để phát, và vẫn phải đánh dấu
        // đã xử lý để không thử lại mãi ở các nhịp tick sau.
        assertNull(tracker.consumeCompletedSession(snapshot(completionId = 1, record = null)))
        assertNull(tracker.consumeCompletedSession(snapshot(completionId = 1, record = record())))

        // nhịp kế tiếp có bản ghi đầy đủ -> phát bình thường
        assertNotNull(
            tracker.consumeCompletedSession(snapshot(completionId = 2, record = record()))
        )
    }

    @Test
    fun testTrackerStartsFromZeroForFreshInstance() {
        val tracker = PomodoroCompletionTracker()

        assertEquals(0, PomodoroSnapshot().completionId)
        assertNull(tracker.consumeCompletedSession(PomodoroSnapshot()))
        assertNotNull(
            tracker.consumeCompletedSession(snapshot(completionId = 1, record = record()))
        )
    }
}
