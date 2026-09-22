package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.pomodoro.PomodoroConfig
import com.team.taskmanagementapp.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm tra việc lưu / đọc cấu hình Pomodoro (Task 11).
 *
 * Hai hàm [PomodoroSettingsRepository.configFrom] / [PomodoroSettingsRepository.valuesFrom] là
 * phần ánh xạ thuần giữa `SharedPreferences` và [PomodoroConfig], nên vòng lặp
 * "lưu → đóng app → mở lại" được kiểm tra trên JVM: giá trị người dùng đã lưu phải được
 * đọc lại **nguyên vẹn**, không bị ghi đè bằng giá trị mặc định.
 */
class PomodoroSettingsRepositoryTest {

    private fun roundTrip(config: PomodoroConfig): PomodoroConfig =
        PomodoroSettingsRepository.configFrom(PomodoroSettingsRepository.valuesFrom(config))

    @Test
    fun testUserSettingsSurviveSaveAndReload() {
        val saved = PomodoroConfig(
            focusMinutes = 45,
            shortBreakMinutes = 8,
            longBreakMinutes = 25,
            autoStartBreaks = false,
            autoStartFocus = true
        )

        assertEquals(saved, roundTrip(saved))
    }

    @Test
    fun testNonDefaultValuesAreNotReplacedByDefaults() {
        val restored = roundTrip(
            PomodoroConfig(
                focusMinutes = 60,
                shortBreakMinutes = 3,
                longBreakMinutes = 30,
                autoStartBreaks = false,
                autoStartFocus = true
            )
        )

        assertEquals(60, restored.focusMinutes)
        assertEquals(3, restored.shortBreakMinutes)
        assertEquals(30, restored.longBreakMinutes)
        assertFalse(restored.autoStartBreaks)
        assertTrue(restored.autoStartFocus)
    }

    @Test
    fun testDefaultsAreUsedWhenNothingWasSavedYet() {
        val config = PomodoroSettingsRepository.configFrom(emptyMap())

        assertEquals(25, config.focusMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertTrue(config.autoStartBreaks)
        assertFalse(config.autoStartFocus)
    }

    @Test
    fun testEverySettingIsWrittenWithItsOwnKey() {
        val values = PomodoroSettingsRepository.valuesFrom(PomodoroConfig())

        assertEquals(5, values.size)
        assertTrue(values.containsKey(Constants.KEY_POMODORO_FOCUS_MINUTES))
        assertTrue(values.containsKey(Constants.KEY_POMODORO_SHORT_BREAK_MINUTES))
        assertTrue(values.containsKey(Constants.KEY_POMODORO_LONG_BREAK_MINUTES))
        assertTrue(values.containsKey(Constants.KEY_POMODORO_AUTO_START_BREAKS))
        assertTrue(values.containsKey(Constants.KEY_POMODORO_AUTO_START_FOCUS))
    }

    @Test
    fun testStoredValuesAreClampedToAllowedRanges() {
        // Dữ liệu cũ / hỏng trong máy phải được kéo về đúng biên thay vì gây lỗi UI.
        val tooHigh = PomodoroSettingsRepository.configFrom(
            mapOf(
                Constants.KEY_POMODORO_FOCUS_MINUTES to 500,
                Constants.KEY_POMODORO_SHORT_BREAK_MINUTES to 99,
                Constants.KEY_POMODORO_LONG_BREAK_MINUTES to 999
            )
        )
        assertEquals(60, tooHigh.focusMinutes)
        assertEquals(10, tooHigh.shortBreakMinutes)
        assertEquals(30, tooHigh.longBreakMinutes)

        val tooLow = PomodoroSettingsRepository.configFrom(
            mapOf(
                Constants.KEY_POMODORO_FOCUS_MINUTES to 1,
                Constants.KEY_POMODORO_SHORT_BREAK_MINUTES to 0,
                Constants.KEY_POMODORO_LONG_BREAK_MINUTES to -10
            )
        )
        assertEquals(15, tooLow.focusMinutes)
        assertEquals(3, tooLow.shortBreakMinutes)
        assertEquals(10, tooLow.longBreakMinutes)
    }

    @Test
    fun testSaveNormalisesOutOfRangeConfigBeforeWriting() {
        val values = PomodoroSettingsRepository.valuesFrom(
            PomodoroConfig(
                focusMinutes = 999,
                shortBreakMinutes = 0,
                longBreakMinutes = 1
            ).withValidDurations()
        )

        assertEquals(60, values[Constants.KEY_POMODORO_FOCUS_MINUTES])
        assertEquals(3, values[Constants.KEY_POMODORO_SHORT_BREAK_MINUTES])
        assertEquals(10, values[Constants.KEY_POMODORO_LONG_BREAK_MINUTES])
    }

    @Test
    fun testWrongValueTypeFallsBackToDefaultInsteadOfCrashing() {
        val config = PomodoroSettingsRepository.configFrom(
            mapOf(
                Constants.KEY_POMODORO_FOCUS_MINUTES to "ba mươi",
                Constants.KEY_POMODORO_AUTO_START_BREAKS to 1
            )
        )

        assertEquals(PomodoroConfig.DEFAULT_FOCUS_MINUTES, config.focusMinutes)
        assertEquals(PomodoroConfig.DEFAULT_AUTO_START_BREAKS, config.autoStartBreaks)
    }

    @Test
    fun testCycleCountIsNotPersistedAndKeepsDefault() {
        val values = PomodoroSettingsRepository.valuesFrom(
            PomodoroConfig(cyclesBeforeLongBreak = 7)
        )

        assertFalse(values.containsKey("key_pomodoro_cycles"))
        assertEquals(
            PomodoroConfig.DEFAULT_CYCLES_BEFORE_LONG_BREAK,
            roundTrip(PomodoroConfig(cyclesBeforeLongBreak = 7)).cyclesBeforeLongBreak
        )
    }
}
