package com.team.taskmanagementapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.receiver.StreakReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Manages scheduling and evaluation for daily Streak Protection reminders.
 * Reminds users when current streak > 0 and no tasks have been completed today.
 */
object StreakReminderScheduler {

    private const val TAG = "StreakReminderScheduler"

    fun isStreakReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.KEY_STREAK_REMINDER_ENABLED, true)
    }

    fun setStreakReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.KEY_STREAK_REMINDER_ENABLED, enabled).apply()

        if (enabled) {
            scheduleStreakReminder(context)
        } else {
            cancelStreakReminder(context)
        }
    }

    fun getStreakReminderHour(context: Context): Int {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.KEY_STREAK_REMINDER_HOUR, Constants.DEFAULT_STREAK_REMINDER_HOUR)
    }

    fun getStreakReminderMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.KEY_STREAK_REMINDER_MINUTE, Constants.DEFAULT_STREAK_REMINDER_MINUTE)
    }

    fun saveStreakReminderTime(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(Constants.KEY_STREAK_REMINDER_HOUR, hour)
            .putInt(Constants.KEY_STREAK_REMINDER_MINUTE, minute)
            .apply()

        if (isStreakReminderEnabled(context)) {
            scheduleStreakReminder(context)
        }
    }

    fun getFormattedReminderTime(context: Context): String {
        val hour = getStreakReminderHour(context)
        val minute = getStreakReminderMinute(context)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(cal.time)
    }

    fun scheduleStreakReminder(context: Context) {
        val applicationContext = context.applicationContext
        if (!isStreakReminderEnabled(applicationContext)) {
            cancelStreakReminder(applicationContext)
            return
        }

        val hour = getStreakReminderHour(applicationContext)
        val minute = getStreakReminderMinute(applicationContext)

        val now = System.currentTimeMillis()
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.timeInMillis <= now) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(applicationContext, StreakReminderReceiver::class.java).apply {
            action = Constants.ACTION_STREAK_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            Constants.REQUEST_CODE_STREAK_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available to schedule streak reminder")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Streak reminder scheduled for: ${targetCal.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule streak reminder alarm", e)
        }
    }

    fun cancelStreakReminder(context: Context) {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, StreakReminderReceiver::class.java).apply {
            action = Constants.ACTION_STREAK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            Constants.REQUEST_CODE_STREAK_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        Log.d(TAG, "Streak reminder cancelled")
    }

    fun checkAndSendStreakReminder(context: Context) {
        val applicationContext = context.applicationContext
        if (!isStreakReminderEnabled(applicationContext)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val allTasks = db.taskDao().getAllTasksSync()
                val streakInfo = StreakCalculator.calculateStreak(allTasks, applicationContext)

                // Condition: Current streak > 0 AND no completed tasks today (or not 100% completed today)
                if (streakInfo.currentStreak > 0 && !streakInfo.isTodayCompleted) {
                    NotificationHelper.showStreakReminder(applicationContext, streakInfo.currentStreak)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking streak reminder status", e)
            } finally {
                // Re-schedule for next day
                scheduleStreakReminder(applicationContext)
            }
        }
    }
}
