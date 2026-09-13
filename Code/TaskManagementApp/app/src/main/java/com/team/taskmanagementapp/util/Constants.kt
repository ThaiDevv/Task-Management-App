package com.team.taskmanagementapp.util

/**
 * Global Constants object storing application-wide configurations,
 * database name, notification channel info, and shared preference keys.
 */
object Constants {
    // Database Configuration
    const val DATABASE_NAME = "task_management_db"
    const val DATABASE_VERSION = 2

    // Notification Channel
    const val NOTIFICATION_CHANNEL_ID = "task_reminder_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Task Reminder Notifications"
    const val NOTIFICATION_CHANNEL_DESC = "Notifications for upcoming and due tasks"

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
