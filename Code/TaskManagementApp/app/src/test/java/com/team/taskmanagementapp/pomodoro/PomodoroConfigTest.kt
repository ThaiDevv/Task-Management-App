package com.team.taskmanagementapp.pomodoro

import com.team.taskmanagementapp.data.model.enums.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm tra các giới hạn cấu hình Pomodoro (Task 11):
 * Focus 15–60, Nghỉ ngắn 3–10, Nghỉ dài 10–30.
 *
 * Đây chính là các biên mà `Slider` trên màn hình Settings khai báo, nên test này bảo đảm
 * UI và tầng cấu hình không bao giờ lệch nhau.
 */
class PomodoroConfigTest {

    @Test
    fun testDefaultDurationsMatchProductSpec() {
        val config = PomodoroConfig()

        assertEquals(25, config.focusMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertEquals(4, config.cyclesBeforeLongBreak)
        assertTrue(config.autoStartBreaks)
        assertFalse(config.autoStartFocus)
    }

    @Test
    fun testRangeConstantsMatchUiSliderBounds() {
        assertEquals(15, PomodoroConfig.FOCUS_MINUTES_RANGE.first)
        assertEquals(60, PomodoroConfig.FOCUS_MINUTES_RANGE.last)
        assertEquals(3, PomodoroConfig.SHORT_BREAK_MINUTES_RANGE.first)
        assertEquals(10, PomodoroConfig.SHORT_BREAK_MINUTES_RANGE.last)
        assertEquals(10, PomodoroConfig.LONG_BREAK_MINUTES_RANGE.first)
        assertEquals(30, PomodoroConfig.LONG_BREAK_MINUTES_RANGE.last)
    }

    @Test
    fun testDefaultsAreInsideTheirRanges() {
        val config = PomodoroConfig()

        assertTrue(config.focusMinutes in PomodoroConfig.FOCUS_MINUTES_RANGE)
        assertTrue(config.shortBreakMinutes in PomodoroConfig.SHORT_BREAK_MINUTES_RANGE)
        assertTrue(config.longBreakMinutes in PomodoroConfig.LONG_BREAK_MINUTES_RANGE)
    }

    @Test
    fun testFocusDurationBoundaries() {
        assertEquals(15, PomodoroConfig(focusMinutes = 14).withValidDurations().focusMinutes)
        assertEquals(15, PomodoroConfig(focusMinutes = 15).withValidDurations().focusMinutes)
        assertEquals(60, PomodoroConfig(focusMinutes = 60).withValidDurations().focusMinutes)
        assertEquals(60, PomodoroConfig(focusMinutes = 61).withValidDurations().focusMinutes)
        assertEquals(60, PomodoroConfig(focusMinutes = 999).withValidDurations().focusMinutes)
    }

    @Test
    fun testShortBreakBoundaries() {
        assertEquals(3, PomodoroConfig(shortBreakMinutes = 2).withValidDurations().shortBreakMinutes)
        assertEquals(3, PomodoroConfig(shortBreakMinutes = 3).withValidDurations().shortBreakMinutes)
        assertEquals(10, PomodoroConfig(shortBreakMinutes = 10).withValidDurations().shortBreakMinutes)
        assertEquals(10, PomodoroConfig(shortBreakMinutes = 11).withValidDurations().shortBreakMinutes)
    }

    @Test
    fun testLongBreakBoundaries() {
        assertEquals(10, PomodoroConfig(longBreakMinutes = 9).withValidDurations().longBreakMinutes)
        assertEquals(10, PomodoroConfig(longBreakMinutes = 10).withValidDurations().longBreakMinutes)
        assertEquals(30, PomodoroConfig(longBreakMinutes = 30).withValidDurations().longBreakMinutes)
        assertEquals(30, PomodoroConfig(longBreakMinutes = 31).withValidDurations().longBreakMinutes)
    }

    @Test
    fun testValuesInsideRangeArePreserved() {
        val config = PomodoroConfig(
            focusMinutes = 45,
            shortBreakMinutes = 7,
            longBreakMinutes = 25
        ).withValidDurations()

        assertEquals(45, config.focusMinutes)
        assertEquals(7, config.shortBreakMinutes)
        assertEquals(25, config.longBreakMinutes)
    }

    @Test
    fun testCyclesAreClampedToSafeRange() {
        assertEquals(
            PomodoroConfig.MIN_CYCLES_BEFORE_LONG_BREAK,
            PomodoroConfig(cyclesBeforeLongBreak = 0).withValidDurations().cyclesBeforeLongBreak
        )
        assertEquals(
            PomodoroConfig.MAX_CYCLES_BEFORE_LONG_BREAK,
            PomodoroConfig(cyclesBeforeLongBreak = 99).withValidDurations().cyclesBeforeLongBreak
        )
        assertEquals(6, PomodoroConfig(cyclesBeforeLongBreak = 6).withValidDurations().cyclesBeforeLongBreak)
    }

    @Test
    fun testDurationMillisForEachSessionType() {
        val config = PomodoroConfig(
            focusMinutes = 30,
            shortBreakMinutes = 6,
            longBreakMinutes = 20
        )

        assertEquals(30 * 60_000L, config.durationMillisFor(SessionType.FOCUS))
        assertEquals(6 * 60_000L, config.durationMillisFor(SessionType.SHORT_BREAK))
        assertEquals(20 * 60_000L, config.durationMillisFor(SessionType.LONG_BREAK))
    }

    @Test
    fun testAutoStartEnabledForRespectiveSessionTypes() {
        val config = PomodoroConfig(autoStartBreaks = true, autoStartFocus = false)

        assertTrue(config.autoStartEnabledFor(SessionType.SHORT_BREAK))
        assertTrue(config.autoStartEnabledFor(SessionType.LONG_BREAK))
        assertFalse(config.autoStartEnabledFor(SessionType.FOCUS))

        val inverted = PomodoroConfig(autoStartBreaks = false, autoStartFocus = true)

        assertFalse(inverted.autoStartEnabledFor(SessionType.SHORT_BREAK))
        assertTrue(inverted.autoStartEnabledFor(SessionType.FOCUS))
    }
}
