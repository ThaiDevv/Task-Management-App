package com.team.taskmanagementapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.receiver.AlarmReceiver
import com.team.taskmanagementapp.worker.TaskReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Schedules task reminders without allowing platform scheduling failures to crash the app. */
object AlarmScheduler {

    enum class ScheduleResult {
        SCHEDULED,
        FALLBACK_SCHEDULED,
        SKIPPED,
        NOTIFICATIONS_DISABLED,
        FAILED
    }

    internal enum class ScheduleDecision {
        SKIP,
        NOTIFICATIONS_DISABLED,
        EXACT,
        FALLBACK
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

        return scheduleReminderAt(
            context = applicationContext,
            taskId = task.id,
            triggerAtMillis = triggerAtMillis,
            nowMillis = nowMillis
        )
    }

    /**
     * Schedules a reminder at a known timestamp. Snooze uses this entry point so it receives the
     * same permission checks, exception handling, and WorkManager fallback as normal reminders.
     */
    fun scheduleReminderAt(
        context: Context,
        taskId: Int,
        triggerAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleResult {
        val applicationContext = context.applicationContext

        if (taskId <= 0 || triggerAtMillis <= nowMillis) {
            cancelAlarm(applicationContext, taskId)
            return ScheduleResult.SKIPPED
        }

        val notificationsEnabled = NotificationHelper.areNotificationsEnabled(applicationContext)
        if (!notificationsEnabled) {
            Log.w(TAG, "Reminder scheduling skipped because notifications are disabled")
            cancelAlarm(applicationContext, taskId)
            return ScheduleResult.NOTIFICATIONS_DISABLED
        }

        val alarmManager = runCatching { alarmManager(applicationContext) }
            .onFailure { Log.w(TAG, "Alarm service unavailable; using deferred fallback") }
            .getOrNull()

        val exactAlarmAvailable = alarmManager?.let(::canScheduleExactAlarms) == true
        return when (
            scheduleDecision(
                taskId = taskId,
                isCompleted = false,
                triggerAtMillis = triggerAtMillis,
                nowMillis = nowMillis,
                notificationsEnabled = notificationsEnabled,
                exactAlarmAvailable = exactAlarmAvailable
            )
        ) {
            ScheduleDecision.SKIP -> ScheduleResult.SKIPPED
            ScheduleDecision.NOTIFICATIONS_DISABLED -> ScheduleResult.NOTIFICATIONS_DISABLED
            ScheduleDecision.FALLBACK -> {
                Log.w(TAG, "Exact alarm access unavailable; using deferred fallback")
                scheduleFallback(applicationContext, taskId, triggerAtMillis, nowMillis)
            }
            ScheduleDecision.EXACT -> scheduleExactOrFallback(
                context = applicationContext,
                alarmManager = requireNotNull(alarmManager),
                taskId = taskId,
                triggerAtMillis = triggerAtMillis,
                nowMillis = nowMillis
            )
        }
    }

    fun cancelAlarm(context: Context, taskId: Int) {
        if (taskId <= 0) return

        val applicationContext = context.applicationContext
        runCatching {
            val pendingIntent = reminderPendingIntent(applicationContext, taskId)
            alarmManager(applicationContext).cancel(pendingIntent)
            pendingIntent.cancel()
        }.onFailure {
            Log.w(TAG, "Unable to cancel platform alarm", it)
        }

        cancelFallback(applicationContext, taskId)
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
        runCatching { canScheduleExactAlarms(alarmManager(context.applicationContext)) }
            .onFailure { Log.w(TAG, "Unable to query exact alarm access", it) }
            .getOrDefault(false)

    fun exactAlarmPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )

    internal fun scheduleDecision(
        taskId: Int,
        isCompleted: Boolean,
        triggerAtMillis: Long?,
        nowMillis: Long,
        notificationsEnabled: Boolean,
        exactAlarmAvailable: Boolean
    ): ScheduleDecision = when {
        taskId <= 0 || isCompleted || triggerAtMillis == null || triggerAtMillis <= nowMillis ->
            ScheduleDecision.SKIP
        !notificationsEnabled -> ScheduleDecision.NOTIFICATIONS_DISABLED
        exactAlarmAvailable -> ScheduleDecision.EXACT
        else -> ScheduleDecision.FALLBACK
    }

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

    private fun scheduleExactOrFallback(
        context: Context,
        alarmManager: AlarmManager,
        taskId: Int,
        triggerAtMillis: Long,
        nowMillis: Long
    ): ScheduleResult {
        val exactResult = runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                reminderPendingIntent(context, taskId)
            )
        }

        if (exactResult.isSuccess) {
            cancelFallback(context, taskId)
            return ScheduleResult.SCHEDULED
        }

        val error = exactResult.exceptionOrNull()
        val reason = when (error) {
            is SecurityException -> "Exact alarm permission changed while scheduling"
            is IllegalStateException -> "Platform alarm limit reached"
            else -> "Exact alarm scheduling failed"
        }
        Log.w(TAG, "$reason; using deferred fallback", error)
        return scheduleFallback(context, taskId, triggerAtMillis, nowMillis)
    }

    private fun scheduleFallback(
        context: Context,
        taskId: Int,
        triggerAtMillis: Long,
        nowMillis: Long
    ): ScheduleResult {
        val delayMillis = (triggerAtMillis - nowMillis).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TaskReminderWorker.KEY_TASK_ID to taskId))
            .addTag(FALLBACK_WORK_TAG)
            .build()

        return runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                fallbackWorkName(taskId),
                ExistingWorkPolicy.REPLACE,
                request
            )
            ScheduleResult.FALLBACK_SCHEDULED
        }.onFailure {
            Log.w(TAG, "Unable to enqueue deferred reminder fallback", it)
        }.getOrDefault(ScheduleResult.FAILED)
    }

    private fun cancelFallback(context: Context, taskId: Int) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(fallbackWorkName(taskId))
        }.onFailure {
            Log.w(TAG, "Unable to cancel deferred reminder fallback", it)
        }
    }

    private fun alarmManager(context: Context): AlarmManager =
        requireNotNull(context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { alarmManager.canScheduleExactAlarms() }
                .onFailure { Log.w(TAG, "Exact alarm access check failed", it) }
                .getOrDefault(false)

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

    private fun fallbackWorkName(taskId: Int): String = "task-reminder-$taskId"

    private const val TAG = "AlarmScheduler"
    private const val FALLBACK_WORK_TAG = "task-reminder-fallback"
}
