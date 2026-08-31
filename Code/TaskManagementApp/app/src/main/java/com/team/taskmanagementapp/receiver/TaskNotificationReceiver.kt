package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives notification action buttons and snoozed reminder alarms. */
class TaskNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(Constants.EXTRA_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) return

        when (intent.action) {
            ACTION_MARK_COMPLETE -> markTaskComplete(context, taskId)
            ACTION_SNOOZE_15_MINUTES -> snoozeReminder(context, taskId)
            ACTION_SHOW_REMINDER -> showReminder(context, taskId)
        }
    }

    private fun markTaskComplete(context: Context, taskId: Int) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = AppDatabase.getInstance(context).taskDao()
                val task = taskDao.getTaskById(taskId.toLong()) ?: return@launch

                taskDao.updateTask(
                    task.copy(
                        status = TaskStatus.COMPLETED,
                        isCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                NotificationHelper.cancelNotification(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun snoozeReminder(context: Context, taskId: Int) {
        val triggerAtMillis = System.currentTimeMillis() + SNOOZE_DURATION_MS
        AlarmScheduler.scheduleReminderAt(context, taskId, triggerAtMillis)

        NotificationHelper.cancelNotification(context, taskId)
    }

    private fun showReminder(context: Context, taskId: Int) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = AppDatabase.getInstance(context)
                    .taskDao()
                    .getTaskById(taskId.toLong())

                if (task != null && !task.isCompleted) {
                    NotificationHelper.showTaskReminder(context, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_COMPLETE =
            "com.team.taskmanagementapp.action.MARK_COMPLETE"
        const val ACTION_SNOOZE_15_MINUTES =
            "com.team.taskmanagementapp.action.SNOOZE_15_MINUTES"
        const val ACTION_SHOW_REMINDER =
            "com.team.taskmanagementapp.action.SHOW_REMINDER"

        const val SNOOZE_DURATION_MS = 15 * 60 * 1000L

        private const val INVALID_TASK_ID = -1
    }
}
