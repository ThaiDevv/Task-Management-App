package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.team.taskmanagementapp.pomodoro.PomodoroConfig
import com.team.taskmanagementapp.util.Constants

/**
 * Lưu / đọc cấu hình Pomodoro (Task 11).
 *
 * Dùng **SharedPreferences** theo đúng architecture hiện tại của app
 * (`SettingsFragment` cũng đọc `Constants.PREFS_NAME` trực tiếp) — không thêm DataStore
 * để tránh hai cơ chế lưu trữ song song trong cùng project.
 *
 * Phần ánh xạ dữ liệu được tách thành hai hàm thuần [configFrom] / [valuesFrom] nên
 * kiểm tra được vòng lặp lưu → đọc trên JVM mà không cần thiết bị.
 *
 * Cả hai chiều đều đi qua [PomodoroConfig.withValidDurations] nên các giới hạn
 * 15–60 (focus), 3–10 (nghỉ ngắn), 10–30 (nghỉ dài) luôn được thực thi ở tầng lưu trữ,
 * kể cả khi dữ liệu cũ trong máy nằm ngoài khoảng hợp lệ.
 *
 * Lưu ý: `cyclesBeforeLongBreak` không được expose trên UI nên không được persist —
 * [configFrom] luôn trả về giá trị mặc định (4) cho trường này.
 */
class PomodoroSettingsRepository(
    private val preferences: SharedPreferences
) {

    /** Đọc cấu hình đã lưu; chưa có gì thì trả về mặc định 25/5/15. */
    fun load(): PomodoroConfig = configFrom(preferences.all)

    /** Ghi cấu hình xuống máy (đã chuẩn hoá về khoảng hợp lệ). */
    fun save(config: PomodoroConfig) {
        preferences.edit()
            .putAllSettings(valuesFrom(config.withValidDurations()))
            .apply()
    }

    private fun SharedPreferences.Editor.putAllSettings(
        values: Map<String, Any>
    ): SharedPreferences.Editor {
        values.forEach { (key, value) ->
            when (value) {
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                else -> Unit // Không có kiểu nào khác trong cấu hình Pomodoro.
            }
        }
        return this
    }

    companion object {
        fun from(context: Context): PomodoroSettingsRepository = PomodoroSettingsRepository(
            context.applicationContext.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
            )
        )

        /**
         * Dựng cấu hình từ dữ liệu thô trong bộ nhớ.
         *
         * Đọc theo kiểu `as?` để dữ liệu sai kiểu (nếu có) rơi về giá trị mặc định thay vì
         * làm crash màn hình, sau đó chuẩn hoá về khoảng hợp lệ.
         */
        internal fun configFrom(values: Map<String, Any?>): PomodoroConfig = PomodoroConfig(
            focusMinutes = values[Constants.KEY_POMODORO_FOCUS_MINUTES] as? Int
                ?: PomodoroConfig.DEFAULT_FOCUS_MINUTES,
            shortBreakMinutes = values[Constants.KEY_POMODORO_SHORT_BREAK_MINUTES] as? Int
                ?: PomodoroConfig.DEFAULT_SHORT_BREAK_MINUTES,
            longBreakMinutes = values[Constants.KEY_POMODORO_LONG_BREAK_MINUTES] as? Int
                ?: PomodoroConfig.DEFAULT_LONG_BREAK_MINUTES,
            autoStartBreaks = values[Constants.KEY_POMODORO_AUTO_START_BREAKS] as? Boolean
                ?: PomodoroConfig.DEFAULT_AUTO_START_BREAKS,
            autoStartFocus = values[Constants.KEY_POMODORO_AUTO_START_FOCUS] as? Boolean
                ?: PomodoroConfig.DEFAULT_AUTO_START_FOCUS
        ).withValidDurations()

        /** Cặp key → giá trị sẽ được ghi xuống SharedPreferences. */
        internal fun valuesFrom(config: PomodoroConfig): Map<String, Any> = mapOf(
            Constants.KEY_POMODORO_FOCUS_MINUTES to config.focusMinutes,
            Constants.KEY_POMODORO_SHORT_BREAK_MINUTES to config.shortBreakMinutes,
            Constants.KEY_POMODORO_LONG_BREAK_MINUTES to config.longBreakMinutes,
            Constants.KEY_POMODORO_AUTO_START_BREAKS to config.autoStartBreaks,
            Constants.KEY_POMODORO_AUTO_START_FOCUS to config.autoStartFocus
        )
    }
}
