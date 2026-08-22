package com.team.taskmanagementapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.receiver.AlarmReceiver
import java.util.Calendar

/** Schedules and cancels exact task reminder alarms. */
object AlarmScheduler {

    enum class ScheduleResult {
        SCHEDULED,
        SKIPPED,
        PERMISSION_REQUIRED
    }

    fun scheduleAlarm(
        context: Context,
        task: Task,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleResult {
        val applicationContext = context.applicationContext
        val triggerAtMillis = calculateTriggerAtMillis(task)

        if (
            task.id <= 0 ||
            task.isCompleted ||
            triggerAtMillis == null ||
            triggerAtMillis <= nowMillis
        ) {
            cancelAlarm(applicationContext, task.id)
            return ScheduleResult.SKIPPED
        }

        val alarmManager = alarmManager(applicationContext)
        if (!canScheduleExactAlarms(alarmManager)) {
            return ScheduleResult.PERMISSION_REQUIRED
        }

        return try {
            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    reminderPendingIntent(applicationContext, task.id)
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    reminderPendingIntent(applicationContext, task.id)
                )
            }
            ScheduleResult.SCHEDULED
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    reminderPendingIntent(applicationContext, task.id)
                )
                ScheduleResult.SCHEDULED
            } catch (_: Exception) {
                ScheduleResult.PERMISSION_REQUIRED
            }
        }
    }

    fun cancelAlarm(context: Context, taskId: Int) {
        if (taskId <= 0) return

        val applicationContext = context.applicationContext
        val pendingIntent = reminderPendingIntent(applicationContext, taskId)
        alarmManager(applicationContext).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAlarm(
        context: Context,
        task: Task,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleResult {
        cancelAlarm(context, task.id)
        return scheduleAlarm(context, task, nowMillis)
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        canScheduleExactAlarms(alarmManager(context.applicationContext))

    fun exactAlarmPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )

    internal fun calculateTriggerAtMillis(task: Task): Long? =
        calculateTriggerAtMillis(
            dueDateMillis = task.dueDate,
            dueTimeMillis = task.dueTime,
            reminderMinutes = task.reminderMinutes
        )

    internal fun calculateTriggerAtMillis(
        dueDateMillis: Long,
        dueTimeMillis: Long,
        reminderMinutes: Int
    ): Long? {
        if (dueDateMillis <= 0L || dueTimeMillis <= 0L || reminderMinutes < 0) {
            return null
        }

        val dueDate = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val dueTime = Calendar.getInstance().apply { timeInMillis = dueTimeMillis }
        return Calendar.getInstance().apply {
            clear()
            set(
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DAY_OF_MONTH),
                dueTime.get(Calendar.HOUR_OF_DAY),
                dueTime.get(Calendar.MINUTE),
                0
            )
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -reminderMinutes)
        }.timeInMillis
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderPendingIntent(context: Context, taskId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TASK_REMINDER
            putExtra(Constants.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
