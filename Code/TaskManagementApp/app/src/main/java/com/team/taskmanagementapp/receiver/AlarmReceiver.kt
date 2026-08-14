package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receives a scheduled task alarm and displays its reminder notification. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TASK_REMINDER) return

        val taskId = intent.getIntExtra(Constants.EXTRA_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val task = AppDatabase.getInstance(context.applicationContext)
                    .taskDao()
                    .getTaskById(taskId.toLong())

                if (task != null && !task.isCompleted && task.reminderMinutes > 0) {
                    NotificationHelper.showTaskReminder(context.applicationContext, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TASK_REMINDER =
            "com.team.taskmanagementapp.action.TASK_REMINDER"

        private const val INVALID_TASK_ID = -1
    }
}
