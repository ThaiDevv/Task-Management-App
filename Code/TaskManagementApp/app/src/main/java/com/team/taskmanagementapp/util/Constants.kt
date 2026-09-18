package com.team.taskmanagementapp.util

/**
 * Global Constants object storing application-wide configurations,
 * database name, notification channel info, and shared preference keys.
 */
object Constants {
    // Database Configuration
    const val DATABASE_NAME = "task_management_db"

    /**
     * Version 3: thêm bảng `pomodoro_sessions` + 3 cột theo dõi Pomodoro trên `tasks`.
     * Xem `AppDatabase.MIGRATION_2_3`.
     */
    const val DATABASE_VERSION = 3

    // Notification Channel
    const val NOTIFICATION_CHANNEL_ID = "task_reminder_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Task Reminder Notifications"
    const val NOTIFICATION_CHANNEL_DESC = "Notifications for upcoming and due tasks"

    // Pomodoro Timer (Foreground Service)
    const val POMODORO_CHANNEL_ID = "pomodoro_timer_channel"
    const val POMODORO_CHANNEL_NAME = "Đồng hồ Pomodoro"
    const val POMODORO_CHANNEL_DESC = "Đồng hồ tập trung đang chạy nền"

    /**
     * Channel CẢNH BÁO cho việc hết giờ. Tách riêng khỏi [POMODORO_CHANNEL_ID] để:
     * - channel ongoing (đồng hồ) phải im lặng vì được cập nhật mỗi giây;
     * - channel cảnh báo CÓ sound + vibration và người dùng có thể tắt riêng nó.
     */
    const val POMODORO_ALERT_CHANNEL_ID = "pomodoro_alert_channel"
    const val POMODORO_ALERT_CHANNEL_NAME = "Cảnh báo hết phiên"
    const val POMODORO_ALERT_CHANNEL_DESC =
        "Âm thanh và rung khi kết thúc phiên tập trung hoặc phiên nghỉ"

    /**
     * Id của notification ongoing do PomodoroService sở hữu.
     *
     * ⚠️ Dùng dải SỐ ÂM để không bao giờ trùng với notification nhắc việc:
     * `NotificationHelper.showTaskReminder()` dùng chính `task.id` (Room auto-increment,
     * luôn dương) làm notification id. Nếu trùng id thì hai notification sẽ đè lên nhau,
     * và `stopForeground(STOP_FOREGROUND_REMOVE)` của service sẽ xoá luôn reminder của task.
     *
     * Quy ước dải id notification của app:
     * - `task.id` (> 0)             → notification nhắc việc / widget action
     * - `-1000` … `-1999`           → notification ongoing của tính năng hệ thống (Pomodoro…)
     */
    const val POMODORO_NOTIFICATION_ID = -1001

    /**
     * Id notification CẢNH BÁO khi một phiên Pomodoro kết thúc (hết giờ).
     *
     * Khác [POMODORO_NOTIFICATION_ID] (notification ongoing hiển thị đồng hồ đang chạy):
     * notification này thuộc channel CÓ sound + vibration, không ongoing và tự tắt khi chạm.
     * Vẫn nằm trong dải số âm nên không thể trùng với notification nhắc việc.
     */
    const val POMODORO_COMPLETION_NOTIFICATION_ID = -1002

    /** Request code của PendingIntent dùng cho alarm đánh thức lúc phiên kết thúc. */
    const val POMODORO_SESSION_END_REQUEST_CODE = 2001

    /** Giá trị "không gắn task nào" cho [EXTRA_POMODORO_TASK_ID]. */
    const val NO_TASK_ID = -1L
    /**
     * TaskId dùng cho luồng Pomodoro — dùng chung cho 2 intent:
     * - `PomodoroService.start(...)` (task gắn với phiên tập trung)
     * - `MainActivity` khi mở Pomodoro từ Task Detail (Task 13)
     */
    const val EXTRA_POMODORO_TASK_ID = "extra_pomodoro_task_id"

    /** Mở màn hình Pomodoro khi người dùng chạm notification (màn hình sẽ làm ở task UI sau). */
    const val EXTRA_OPEN_POMODORO_TIMER = "extra_open_pomodoro_timer"

    // Preferences & Security Storage Keys
    const val PREFS_NAME = "task_app_prefs"
    const val ENCRYPTED_PREFS_NAME = "task_app_secure_prefs"
    const val KEY_PIN_HASH = "key_pin_hash"
    const val KEY_PIN_SALT = "key_pin_salt"
    const val KEY_PIN_ENABLED = "key_pin_enabled"
    const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
    const val KEY_AUTO_LOCK_TIMER = "key_auto_lock_timer"
    const val KEY_SORT_ORDER = "key_sort_order"
    const val KEY_SORT_TYPE = "key_sort_type"

    // Pomodoro Settings (Task 11) — lưu cùng PREFS_NAME với các cài đặt khác của app.
    // Giá trị mặc định nằm ở PomodoroConfig để chỉ có MỘT nguồn sự thật cho 25/5/15.
    const val KEY_POMODORO_FOCUS_MINUTES = "key_pomodoro_focus_minutes"
    const val KEY_POMODORO_SHORT_BREAK_MINUTES = "key_pomodoro_short_break_minutes"
    const val KEY_POMODORO_LONG_BREAK_MINUTES = "key_pomodoro_long_break_minutes"
    const val KEY_POMODORO_AUTO_START_BREAKS = "key_pomodoro_auto_start_breaks"
    const val KEY_POMODORO_AUTO_START_FOCUS = "key_pomodoro_auto_start_focus"

    // Sort Preferences Defaults
    const val DEFAULT_SORT_TYPE = "DUE_DATE"
    const val DEFAULT_SORT_ORDER = "ASC"

    // Intent Extras & Request Codes
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_DUE_DATE = "extra_task_due_date"
    const val EXTRA_TASK_DUE_TIME = "extra_task_due_time"
    const val ACTION_TASK_DATE_TIME_CHANGED =
        "com.team.taskmanagementapp.action.TASK_DATE_TIME_CHANGED"
    const val EXTRA_PIN_MODE = "extra_pin_mode"

    // Home Screen Widget (TMA-56)
    const val EXTRA_WIDGET_TASK_ID = "extra_widget_task_id"
    const val EXTRA_WIDGET_ACTION = "extra_widget_action"
    const val WIDGET_ACTION_OPEN_TASK = 0
    const val WIDGET_ACTION_TOGGLE_TASK = 1
    const val ACTION_WIDGET_OPEN_TASK =
        "com.team.taskmanagementapp.action.WIDGET_OPEN_TASK"
    const val ACTION_WIDGET_TOGGLE_TASK =
        "com.team.taskmanagementapp.action.WIDGET_TOGGLE_TASK"
    const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
    const val REQUEST_CODE_EXACT_ALARM_PERMISSION = 1002

    // PIN Lock Constants
    const val MAX_PIN_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
    const val AUTO_LOCK_TIMEOUT_MS = 60000L // 1 minute background re-lock
}
