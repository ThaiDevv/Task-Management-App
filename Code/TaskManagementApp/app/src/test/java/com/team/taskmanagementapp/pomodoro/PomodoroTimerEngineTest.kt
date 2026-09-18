package com.team.taskmanagementapp.pomodoro

import com.team.taskmanagementapp.data.model.enums.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [PomodoroTimerEngine].
 *
 * Chạy hoàn toàn trên JVM nhờ inject clock giả — không cần Robolectric hay thiết bị,
 * và không hề gọi `SystemClock` thật.
 */
class PomodoroTimerEngineTest {

    private companion object {
        const val MINUTE = 60_000L
        val FOCUS_25 = 25 * MINUTE
        val SHORT_BREAK_5 = 5 * MINUTE
        val LONG_BREAK_15 = 15 * MINUTE

        /**
         * Config không auto-start: giữ nguyên trạng thái COMPLETED để quan sát
         * transition rõ ràng (mặc định của app là autoStartBreaks = true).
         */
        val manualConfig = PomodoroConfig(autoStartBreaks = false, autoStartFocus = false)
    }

    /** Đồng hồ giả: tách riêng elapsedRealtime (đếm ngược) và wall clock (ghi DB). */
    private class TestClock(
        elapsed: Long = 10_000L,
        wall: Long = 1_700_000_000_000L
    ) {
        var elapsedRealtime: Long = elapsed
        var wallClock: Long = wall

        fun advance(millis: Long) {
            elapsedRealtime += millis
            wallClock += millis
        }
    }

    private fun engine(
        clock: TestClock,
        config: PomodoroConfig = manualConfig
    ) = PomodoroTimerEngine(
        initialConfig = config,
        elapsedRealtimeProvider = { clock.elapsedRealtime },
        wallClockProvider = { clock.wallClock }
    )

    /**
     * Chạy hết phiên hiện tại rồi bắt đầu phiên kế tiếp — lặp [times] lần.
     *
     * Điều kiện: engine PHẢI đang ở trạng thái RUNNING (đã gọi `start()`), nếu không
     * vòng lặp đầu tiên sẽ chạy trên state IDLE với `totalMillis = 0`.
     */
    private fun TestClock.completeAndAdvance(engine: PomodoroTimerEngine, times: Int) {
        repeat(times) {
            advance(engine.currentSnapshot.totalMillis)
            engine.tick()
            engine.start()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Transition cơ bản
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testInitialStateIsIdle() {
        val snapshot = engine(TestClock()).currentSnapshot

        assertEquals(PomodoroTimerState.IDLE, snapshot.state)
        assertEquals(SessionType.FOCUS, snapshot.sessionType)
        assertEquals(SessionType.FOCUS, snapshot.nextSessionType)
        assertEquals(0L, snapshot.remainingMillis)
        assertEquals(0L, snapshot.totalMillis)
        assertEquals(0L, snapshot.targetEndElapsedRealtime)
        assertEquals(0, snapshot.focusSessionsInCurrentSet)
        assertNull(snapshot.taskId)
        assertNull(snapshot.lastCompletedSession)
        assertFalse(snapshot.isActive)
    }

    @Test
    fun testStartBeginsFocusSessionAnchoredToElapsedRealtime() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 42L)
        val snapshot = engine.currentSnapshot

        assertEquals(PomodoroTimerState.RUNNING, snapshot.state)
        assertEquals(SessionType.FOCUS, snapshot.sessionType)
        assertEquals(SessionType.SHORT_BREAK, snapshot.nextSessionType)
        assertEquals(42L, snapshot.taskId)
        assertEquals(FOCUS_25, snapshot.totalMillis)
        assertEquals(FOCUS_25, snapshot.remainingMillis)
        assertEquals(clock.elapsedRealtime + FOCUS_25, snapshot.targetEndElapsedRealtime)
        assertTrue(snapshot.isActive)
    }

    @Test
    fun testStartIsIgnoredWhileRunningOrPaused() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 1L)
        val target = engine.currentSnapshot.targetEndElapsedRealtime

        engine.start(taskId = 999L) // phải bị bỏ qua
        assertEquals(target, engine.currentSnapshot.targetEndElapsedRealtime)
        assertEquals(1L, engine.currentSnapshot.taskId)

        engine.pause()
        engine.start(taskId = 999L) // phải bị bỏ qua
        assertEquals(PomodoroTimerState.PAUSED, engine.currentSnapshot.state)
        assertEquals(1L, engine.currentSnapshot.taskId)
    }

    @Test
    fun testPauseResumeSkipTickAreNoOpWhenIdle() {
        val engine = engine(TestClock())

        engine.pause()
        engine.resume()
        engine.skip()
        engine.tick()

        assertEquals(PomodoroTimerState.IDLE, engine.currentSnapshot.state)
        assertEquals(0L, engine.currentSnapshot.totalMillis)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Pause → Resume
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testPauseThenResumeContinuesExactlyWhereItStopped() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(10 * MINUTE)
        engine.tick()
        assertEquals(15 * MINUTE, engine.currentSnapshot.remainingMillis)

        engine.pause()
        assertEquals(PomodoroTimerState.PAUSED, engine.currentSnapshot.state)
        assertEquals(15 * MINUTE, engine.currentSnapshot.remainingMillis)
        assertEquals(0L, engine.currentSnapshot.targetEndElapsedRealtime)
        assertTrue(engine.currentSnapshot.isActive)

        // App bị background 5 phút khi đang pause: thời gian còn lại phải giữ nguyên
        clock.advance(5 * MINUTE)
        engine.tick()
        assertEquals(15 * MINUTE, engine.currentSnapshot.remainingMillis)
        assertEquals(15 * MINUTE, engine.remainingMillisNow())

        engine.resume()
        assertEquals(PomodoroTimerState.RUNNING, engine.currentSnapshot.state)
        assertEquals(
            clock.elapsedRealtime + 15 * MINUTE,
            engine.currentSnapshot.targetEndElapsedRealtime
        )

        clock.advance(14 * MINUTE)
        engine.tick()
        assertEquals(MINUTE, engine.currentSnapshot.remainingMillis)
        assertEquals(PomodoroTimerState.RUNNING, engine.currentSnapshot.state)

        clock.advance(MINUTE)
        engine.tick()
        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
        assertEquals(0L, engine.currentSnapshot.remainingMillis)
    }

    @Test
    fun testPauseIsIgnoredWhenNotRunning() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.pause() // IDLE -> không đổi
        assertEquals(PomodoroTimerState.IDLE, engine.currentSnapshot.state)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick() // -> COMPLETED
        engine.pause()
        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
    }

    @Test
    fun testTimerStaysAccurateAfterLongGapWithoutAnyTick() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(3 * 60 * MINUTE) // thiết bị sleep 3 tiếng, không tick lần nào

        assertEquals(0L, engine.remainingMillisNow())
        engine.tick()
        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
        assertEquals(0L, engine.currentSnapshot.remainingMillis)
    }

    @Test
    fun testRemainingTimeNeverGoesNegative() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(FOCUS_25 + 10 * MINUTE)
        engine.tick()

        assertEquals(0L, engine.currentSnapshot.remainingMillis)
        assertTrue(engine.currentSnapshot.remainingMillis >= 0L)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Reset / Stop
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testResetReturnsToPristineIdleState() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 9L)
        clock.advance(5 * MINUTE)
        engine.tick()
        engine.reset()

        val snapshot = engine.currentSnapshot
        assertEquals(PomodoroTimerState.IDLE, snapshot.state)
        assertEquals(SessionType.FOCUS, snapshot.sessionType)
        assertEquals(SessionType.FOCUS, snapshot.nextSessionType)
        assertEquals(0L, snapshot.remainingMillis)
        assertEquals(0L, snapshot.totalMillis)
        assertEquals(0L, snapshot.targetEndElapsedRealtime)
        assertEquals(0, snapshot.focusSessionsInCurrentSet)
        assertEquals(0, snapshot.completedFocusSessions)
        assertEquals(0, snapshot.completedSessions)
        assertEquals(0, snapshot.completionId)
        assertNull(snapshot.taskId)
        assertNull(snapshot.lastCompletedSession)
    }

    @Test
    fun testResetKeepsConfigAndAllowsStartingAgain() {
        val clock = TestClock()
        val engine = engine(clock, PomodoroConfig(focusMinutes = 30))

        engine.start()
        engine.reset()
        assertEquals(30, engine.currentSnapshot.config.focusMinutes)

        engine.start(taskId = 3L)
        assertEquals(PomodoroTimerState.RUNNING, engine.currentSnapshot.state)
        assertEquals(30 * MINUTE, engine.currentSnapshot.totalMillis)
        assertEquals(3L, engine.currentSnapshot.taskId)
    }

    @Test
    fun testStopIsAliasOfReset() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 1L)
        clock.advance(MINUTE)
        engine.tick()
        engine.stop()

        assertEquals(PomodoroTimerState.IDLE, engine.currentSnapshot.state)
        assertEquals(0L, engine.currentSnapshot.totalMillis)
        assertNull(engine.currentSnapshot.taskId)
    }

    @Test
    fun testResetClearsCycleProgressInTheMiddleOfSet() {
        val clock = TestClock()
        val engine = engine(clock)

        // hoàn thành 2 phiên FOCUS (mỗi phiên FOCUS đi kèm 1 nghỉ ngắn)
        engine.start()
        repeat(2) {
            clock.advance(engine.currentSnapshot.totalMillis) // FOCUS
            engine.tick()
            engine.start() // -> SHORT_BREAK
            clock.advance(engine.currentSnapshot.totalMillis) // SHORT_BREAK
            engine.tick()
            engine.start() // -> FOCUS
        }
        assertEquals(2, engine.currentSnapshot.focusSessionsInCurrentSet)
        assertEquals(2, engine.currentSnapshot.completedFocusSessions)

        engine.reset()
        assertEquals(0, engine.currentSnapshot.focusSessionsInCurrentSet)
        assertEquals(0, engine.currentSnapshot.completedFocusSessions)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Skip
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testSkipFromFocusGoesToShortBreakAndIsNotCountedAsCompleted() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 5L)
        clock.advance(7 * MINUTE) // mới tập trung 7 phút rồi bỏ qua
        engine.tick()
        engine.skip()

        val snapshot = engine.currentSnapshot
        assertEquals(PomodoroTimerState.COMPLETED, snapshot.state)
        assertEquals(SessionType.FOCUS, snapshot.sessionType)
        assertEquals(SessionType.SHORT_BREAK, snapshot.nextSessionType)
        assertEquals(0, snapshot.completedFocusSessions) // không tính là hoàn thành
        assertEquals(0, snapshot.completedSessions)
        assertEquals(1, snapshot.focusSessionsInCurrentSet) // nhưng vị trí cycle vẫn tiến

        val record = snapshot.lastCompletedSession
        assertNotNull(record)
        assertEquals(SessionType.FOCUS, record!!.sessionType)
        assertEquals(5L, record.taskId)
        assertFalse(record.isCompleted)
        assertEquals(7, record.durationMinutes) // thời gian chạy thực tế
    }

    @Test
    fun testSkipFromPausedFocusKeepsActualDurationAndExcludesPauseTime() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(4 * MINUTE)
        engine.tick()
        engine.pause()
        clock.advance(30 * MINUTE) // thời gian pause KHÔNG được tính vào duration
        engine.skip()

        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.nextSessionType)
        assertEquals(4, engine.currentSnapshot.lastCompletedSession!!.durationMinutes)
    }

    @Test
    fun testSkipFromShortBreakGoesBackToFocus() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.nextSessionType)

        engine.start() // -> SHORT_BREAK
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.sessionType)
        clock.advance(MINUTE)
        engine.tick()
        engine.skip()

        assertEquals(SessionType.FOCUS, engine.currentSnapshot.nextSessionType)
        assertEquals(1, engine.currentSnapshot.focusSessionsInCurrentSet)
    }

    @Test
    fun testSkipFromCompletedJustStartsNextSession() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()
        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)

        engine.skip()
        assertEquals(PomodoroTimerState.RUNNING, engine.currentSnapshot.state)
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.sessionType)
    }

    @Test
    fun testResumeFromCompletedStartsNextSession() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()

        engine.resume()
        assertEquals(PomodoroTimerState.RUNNING, engine.currentSnapshot.state)
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.sessionType)
        assertEquals(SHORT_BREAK_5, engine.currentSnapshot.totalMillis)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Cycle 1–4 và Long Break
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testStandardFlowIsFocusShortFocusShortFocusShortFocusLong() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 7L)
        val order = mutableListOf(engine.currentSnapshot.sessionType)

        repeat(7) {
            clock.advance(engine.currentSnapshot.totalMillis)
            engine.tick()
            assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
            engine.start()
            order += engine.currentSnapshot.sessionType
        }

        assertEquals(
            listOf(
                SessionType.FOCUS,
                SessionType.SHORT_BREAK,
                SessionType.FOCUS,
                SessionType.SHORT_BREAK,
                SessionType.FOCUS,
                SessionType.SHORT_BREAK,
                SessionType.FOCUS,
                SessionType.LONG_BREAK
            ),
            order
        )
        assertEquals(4, engine.currentSnapshot.completedFocusSessions)
        assertEquals(4, engine.currentSnapshot.focusSessionsInCurrentSet)
        assertEquals(7, engine.currentSnapshot.completedSessions) // 4 focus + 3 short break
    }

    @Test
    fun testCycleDotsIncrementAndProduceLongBreakOnFourthFocus() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        assertEquals(0, engine.currentSnapshot.focusSessionsInCurrentSet)

        val dotsAfterEachFocus = listOf(1, 2, 3, 4)
        val nextAfterEachFocus = listOf(
            SessionType.SHORT_BREAK,
            SessionType.SHORT_BREAK,
            SessionType.SHORT_BREAK,
            SessionType.LONG_BREAK
        )

        for (i in 0 until 4) {
            clock.advance(engine.currentSnapshot.totalMillis) // hết phiên FOCUS hiện tại
            engine.tick()

            assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
            assertEquals(dotsAfterEachFocus[i], engine.currentSnapshot.focusSessionsInCurrentSet)
            assertEquals(nextAfterEachFocus[i], engine.currentSnapshot.nextSessionType)

            engine.start() // -> nghỉ (3 lần đầu là nghỉ ngắn, lần 4 là nghỉ dài)

            if (i < 3) {
                clock.advance(engine.currentSnapshot.totalMillis) // hết nghỉ ngắn
                engine.tick()
                engine.start() // -> FOCUS cho vòng lặp kế tiếp
            }
        }

        assertEquals(SessionType.LONG_BREAK, engine.currentSnapshot.sessionType)
        assertEquals(LONG_BREAK_15, engine.currentSnapshot.totalMillis)
    }

    @Test
    fun testLongBreakResetsCycleDotsForNextSet() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 11L)
        clock.completeAndAdvance(engine, 7) // chạy tới khi LONG_BREAK bắt đầu

        assertEquals(SessionType.LONG_BREAK, engine.currentSnapshot.sessionType)
        assertEquals(4, engine.currentSnapshot.focusSessionsInCurrentSet) // dots đầy 4/4
        assertEquals(LONG_BREAK_15, engine.currentSnapshot.totalMillis)
        clock.advance(LONG_BREAK_15) // hết nghỉ dài
        engine.tick()

        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
        assertEquals(0, engine.currentSnapshot.focusSessionsInCurrentSet) // mở đợt mới
        assertEquals(SessionType.FOCUS, engine.currentSnapshot.nextSessionType)

        engine.start()
        assertEquals(SessionType.FOCUS, engine.currentSnapshot.sessionType)
        assertEquals(0, engine.currentSnapshot.focusSessionsInCurrentSet)
    }

    @Test
    fun testCompletedFocusCounterAccumulatesAcrossSets() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start(taskId = 12L)
        clock.completeAndAdvance(engine, 9) // 4 FOCUS + 3 nghỉ + LONG_BREAK + FOCUS thứ 5

        assertEquals(5, engine.currentSnapshot.completedFocusSessions)
        assertEquals(1, engine.currentSnapshot.focusSessionsInCurrentSet)
    }

    @Test
    fun testAutoStartBreaksRunsBreakImmediatelyAfterFocus() {
        val clock = TestClock()
        val engine = engine(
            clock,
            PomodoroConfig(autoStartBreaks = true, autoStartFocus = false)
        )

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()

        val snapshot = engine.currentSnapshot
        assertEquals(PomodoroTimerState.RUNNING, snapshot.state)
        assertEquals(SessionType.SHORT_BREAK, snapshot.sessionType)
        assertEquals(SHORT_BREAK_5, snapshot.totalMillis)
        assertEquals(1, snapshot.completedFocusSessions)
        assertNotNull(snapshot.lastCompletedSession) // giữ bản ghi để tầng trên lưu DB
        assertEquals(1, snapshot.completionId)
    }

    @Test
    fun testAutoStartFocusRunsFocusImmediatelyAfterBreak() {
        val clock = TestClock()
        val engine = engine(
            clock,
            PomodoroConfig(autoStartBreaks = true, autoStartFocus = true)
        )

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()
        clock.advance(SHORT_BREAK_5)
        engine.tick()

        val snapshot = engine.currentSnapshot
        assertEquals(PomodoroTimerState.RUNNING, snapshot.state)
        assertEquals(SessionType.FOCUS, snapshot.sessionType)
        assertEquals(2, snapshot.completionId)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Cấu hình
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testDefaultConfigMatchesSpecification() {
        val config = PomodoroConfig()

        assertEquals(25, config.focusMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertEquals(4, config.cyclesBeforeLongBreak)
        assertEquals(FOCUS_25, config.durationMillisFor(SessionType.FOCUS))
        assertEquals(SHORT_BREAK_5, config.durationMillisFor(SessionType.SHORT_BREAK))
        assertEquals(LONG_BREAK_15, config.durationMillisFor(SessionType.LONG_BREAK))
    }

    @Test
    fun testCustomConfigDurationsAndCyclesAreUsed() {
        val clock = TestClock()
        val engine = engine(
            clock,
            PomodoroConfig(
                focusMinutes = 50,
                shortBreakMinutes = 8,
                longBreakMinutes = 20,
                cyclesBeforeLongBreak = 2,
                autoStartBreaks = false,
                autoStartFocus = false
            )
        )

        engine.start()
        assertEquals(50 * MINUTE, engine.currentSnapshot.totalMillis)
        clock.advance(50 * MINUTE)
        engine.tick()
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.nextSessionType)

        engine.start()
        assertEquals(8 * MINUTE, engine.currentSnapshot.totalMillis)
        clock.advance(8 * MINUTE)
        engine.tick()
        assertEquals(SessionType.FOCUS, engine.currentSnapshot.nextSessionType)

        engine.start()
        clock.advance(50 * MINUTE)
        engine.tick()
        assertEquals(SessionType.LONG_BREAK, engine.currentSnapshot.nextSessionType) // đủ 2 cycle

        engine.start()
        assertEquals(20 * MINUTE, engine.currentSnapshot.totalMillis)
    }

    @Test
    fun testInvalidConfigIsNormalisedToSafeRanges() {
        val clock = TestClock()
        val engine = engine(
            clock,
            PomodoroConfig(
                focusMinutes = 0,
                shortBreakMinutes = -5,
                longBreakMinutes = 999,
                cyclesBeforeLongBreak = 0,
                autoStartBreaks = false,
                autoStartFocus = false
            )
        )

        val config = engine.currentSnapshot.config
        assertEquals(PomodoroConfig.FOCUS_MINUTES_RANGE.first, config.focusMinutes)
        assertEquals(PomodoroConfig.SHORT_BREAK_MINUTES_RANGE.first, config.shortBreakMinutes)
        assertEquals(PomodoroConfig.LONG_BREAK_MINUTES_RANGE.last, config.longBreakMinutes)
        assertEquals(1, config.cyclesBeforeLongBreak)

        engine.start()
        assertEquals(config.focusMinutes * MINUTE, engine.currentSnapshot.totalMillis)

        // cycles = 1 -> hết 1 phiên FOCUS là tới LONG_BREAK luôn
        clock.advance(engine.currentSnapshot.totalMillis)
        engine.tick()
        assertEquals(SessionType.LONG_BREAK, engine.currentSnapshot.nextSessionType)
    }

    @Test
    fun testUpdateConfigAppliesToNextSessionsOnly() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(MINUTE)
        engine.tick()

        engine.updateConfig(
            PomodoroConfig(focusMinutes = 30, autoStartBreaks = false, autoStartFocus = false)
        )

        // phiên đang chạy giữ nguyên thời lượng cũ
        assertEquals(FOCUS_25, engine.currentSnapshot.totalMillis)
        assertEquals(30, engine.currentSnapshot.config.focusMinutes)

        // phiên kế tiếp dùng config mới
        clock.advance(24 * MINUTE)
        engine.tick()
        assertEquals(PomodoroTimerState.COMPLETED, engine.currentSnapshot.state)
        engine.start()
        assertEquals(SHORT_BREAK_5, engine.currentSnapshot.totalMillis)

        clock.advance(SHORT_BREAK_5)
        engine.tick()
        engine.start()
        assertEquals(30 * MINUTE, engine.currentSnapshot.totalMillis)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. Bản ghi phiên & snapshot
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testCompletionIdIncrementsOncePerFinishedSession() {
        val clock = TestClock()
        val engine = engine(clock)

        assertEquals(0, engine.currentSnapshot.completionId)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()
        assertEquals(1, engine.currentSnapshot.completionId)

        engine.start()
        clock.advance(SHORT_BREAK_5)
        engine.tick()
        assertEquals(2, engine.currentSnapshot.completionId)

        engine.start()
        engine.skip()
        assertEquals(3, engine.currentSnapshot.completionId)
    }

    @Test
    fun testCompletedSessionRecordCarriesWallClockTimestamps() {
        val clock = TestClock()
        val engine = engine(clock)
        val startWallClock = clock.wallClock

        engine.start(taskId = 3L)
        clock.advance(FOCUS_25)
        engine.tick()

        val record = engine.currentSnapshot.lastCompletedSession
        assertNotNull(record)
        assertEquals(3L, record!!.taskId)
        assertEquals(SessionType.FOCUS, record.sessionType)
        assertEquals(startWallClock, record.startTimeMillis)
        assertEquals(startWallClock + FOCUS_25, record.endTimeMillis)
        assertEquals(25, record.durationMinutes)
        assertTrue(record.isCompleted)
    }

    @Test
    fun testLastCompletedWasFocusFlag() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        clock.advance(FOCUS_25)
        engine.tick()
        assertTrue(engine.currentSnapshot.lastCompletedWasFocus)

        engine.start() // SHORT_BREAK
        clock.advance(SHORT_BREAK_5)
        engine.tick()
        assertFalse(engine.currentSnapshot.lastCompletedWasFocus)

        engine.start() // FOCUS rồi skip -> không tính là phiên tập trung hoàn thành
        clock.advance(2 * MINUTE)
        engine.tick()
        engine.skip()
        assertFalse(engine.currentSnapshot.lastCompletedWasFocus)
    }

    @Test
    fun testSnapshotFormattingAndProgressHelpers() {
        val fresh = PomodoroSnapshot(remainingMillis = FOCUS_25, totalMillis = FOCUS_25)
        assertEquals("25:00", fresh.formattedRemaining)
        assertEquals(0f, fresh.progressFraction, 0.0001f)

        val halfway = PomodoroSnapshot(remainingMillis = 60_000L, totalMillis = 120_000L)
        assertEquals("01:00", halfway.formattedRemaining)
        assertEquals(0.5f, halfway.progressFraction, 0.0001f)

        val almostDone = PomodoroSnapshot(remainingMillis = 1L, totalMillis = FOCUS_25)
        assertEquals("00:01", almostDone.formattedRemaining)

        val done = PomodoroSnapshot(remainingMillis = 0L, totalMillis = FOCUS_25)
        assertEquals("00:00", done.formattedRemaining)
        assertEquals(1f, done.progressFraction, 0.0001f)

        // không được chia cho 0
        val empty = PomodoroSnapshot()
        assertEquals("00:00", empty.formattedRemaining)
        assertEquals(0f, empty.progressFraction, 0.0001f)
    }

    @Test
    fun testRemainingMillisAtIsPureAndDoesNotMutateState() {
        val clock = TestClock()
        val engine = engine(clock)

        engine.start()
        val snapshot = engine.currentSnapshot

        assertEquals(20 * MINUTE, snapshot.remainingMillisAt(clock.elapsedRealtime + 5 * MINUTE))
        assertEquals(0L, snapshot.remainingMillisAt(clock.elapsedRealtime + 60 * MINUTE))
        assertEquals(FOCUS_25, snapshot.remainingMillis) // snapshot vẫn bất biến
        assertEquals(FOCUS_25, engine.remainingMillisNow())
        assertEquals(FOCUS_25, engine.currentSnapshot.remainingMillis)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. Thông tin chu kỳ expose cho Service/UI (currentCycle, totalCycles)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun testCycleInfoIsExposedForServiceAndUi() {
        val clock = TestClock()
        val engine = engine(clock)

        // IDLE: chưa có chu kỳ nào
        assertEquals(4, engine.currentSnapshot.totalCycles)
        assertEquals(0, engine.currentSnapshot.currentCycle)

        // Bắt đầu phiên FOCUS đầu tiên -> "Chu kỳ 1/4"
        engine.start()
        assertEquals(4, engine.currentSnapshot.totalCycles)
        assertEquals(1, engine.currentSnapshot.currentCycle)

        // Hết FOCUS #1 -> 1 phiên đã xong trong chu kỳ
        clock.advance(FOCUS_25)
        engine.tick()
        assertEquals(1, engine.currentSnapshot.currentCycle)

        // Đang nghỉ ngắn vẫn giữ chu kỳ 1/4
        engine.start()
        assertEquals(SessionType.SHORT_BREAK, engine.currentSnapshot.sessionType)
        assertEquals(1, engine.currentSnapshot.currentCycle)

        // Sang FOCUS #2 -> "Chu kỳ 2/4"
        clock.advance(SHORT_BREAK_5)
        engine.tick()
        engine.start()
        assertEquals(SessionType.FOCUS, engine.currentSnapshot.sessionType)
        assertEquals(2, engine.currentSnapshot.currentCycle)

        // Tạm dừng không làm mất thông tin chu kỳ
        engine.pause()
        assertEquals(PomodoroTimerState.PAUSED, engine.currentSnapshot.state)
        assertEquals(2, engine.currentSnapshot.currentCycle)

        // Stop -> về 0
        engine.stop()
        assertEquals(0, engine.currentSnapshot.currentCycle)
    }

    @Test
    fun testCycleInfoRespectsCustomCycleCount() {
        val engine = engine(TestClock(), PomodoroConfig(cyclesBeforeLongBreak = 6))

        assertEquals(6, engine.currentSnapshot.totalCycles)
        assertEquals(0, engine.currentSnapshot.currentCycle)

        engine.start()
        assertEquals(1, engine.currentSnapshot.currentCycle)
        assertEquals(6, engine.currentSnapshot.totalCycles)
    }
}
