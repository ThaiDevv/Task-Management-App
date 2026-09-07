package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pendingResult = goAsync()
        Log.d("TimeChangeReceiver", "Received broadcast action: $action")

        when (action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                scope.launch {
                    try {
                        rescheduleAllAlarms(context)
                        checkAndUpdateOverdueTasks(context)
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to update reminders after a system time change", error)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> pendingResult.finish()
        }
    }

    private suspend fun checkAndUpdateOverdueTasks(context: Context) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val dao = db.taskDao()
        val currentTime = System.currentTimeMillis()
        val activeTasks = dao.getActiveTasksSync()

        activeTasks.forEach { task ->
            if (task.isCompleted || task.status == com.team.taskmanagementapp.data.model.enums.TaskStatus.COMPLETED) return@forEach
            
            val combinedDue = com.team.taskmanagementapp.util.DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            val isOverdue = combinedDue > 0L && combinedDue < currentTime

            if (isOverdue && task.status != com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE) {
                Log.d(TAG, "Marking an overdue task after a system time change")
                dao.updateTask(task.copy(status = com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE, updatedAt = currentTime))
            } else if (!isOverdue && task.status == com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE) {
                Log.d(TAG, "Reverting an outdated overdue state after a system time change")
                dao.updateTask(task.copy(status = com.team.taskmanagementapp.data.model.enums.TaskStatus.TODO, updatedAt = currentTime))
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val activeTasks = db.taskDao().getActiveTasksSync()
        val now = System.currentTimeMillis()
        
        Log.d("TimeChangeReceiver", "Rescheduling ${activeTasks.size} active tasks.")
        
        activeTasks.forEach { task ->
            val triggerAt = AlarmScheduler.calculateTriggerAtMillis(task)
            if (triggerAt != null && triggerAt > now) {
                AlarmScheduler.scheduleAlarm(appContext, task, now)
            } else {
                AlarmScheduler.cancelAlarm(appContext, task.id)
            }
        }
    }

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }
}
