package com.team.taskmanagementapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.util.NotificationHelper

/** Delivers a task reminder when AlarmManager exact scheduling is unavailable. */
class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt(KEY_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) return Result.failure()

        return runCatching {
            val task = AppDatabase.getInstance(applicationContext)
                .taskDao()
                .getTaskById(taskId.toLong())

            if (task != null && !task.isCompleted && task.reminderMinutes > 0) {
                NotificationHelper.showTaskReminder(applicationContext, task)
            }
            Result.success()
        }.onFailure {
            Log.w(TAG, "Deferred reminder delivery failed; requesting retry", it)
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"

        private const val INVALID_TASK_ID = -1
        private const val TAG = "TaskReminderWorker"
    }
}
