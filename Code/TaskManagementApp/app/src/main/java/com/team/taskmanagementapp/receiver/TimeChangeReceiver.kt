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
        Log.d("TimeChangeReceiver", "Received broadcast action: $action")

        when (action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                rescheduleAllAlarms(context)
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val db = AppDatabase.getInstance(appContext)
            val activeTasks = db.taskDao().getActiveTasksSync()
            
            Log.d("TimeChangeReceiver", "Rescheduling ${activeTasks.size} active tasks.")
            
            activeTasks.forEach { task ->
                val triggerAt = AlarmScheduler.calculateTriggerAtMillis(task)
                if (triggerAt != null && triggerAt > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleAlarm(appContext, task, triggerAt)
                } else {
                    AlarmScheduler.cancelAlarm(appContext, task.id)
                }
            }
        }
    }
}