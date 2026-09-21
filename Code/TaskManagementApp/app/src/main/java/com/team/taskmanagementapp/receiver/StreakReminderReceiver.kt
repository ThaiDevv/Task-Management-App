package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.StreakReminderScheduler

/**
 * BroadcastReceiver triggered by AlarmManager to check and show daily Streak Protection reminders.
 */
class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action
        Log.d("StreakReminderReceiver", "Received action: $action")

        if (action == Constants.ACTION_STREAK_REMINDER || action == Intent.ACTION_BOOT_COMPLETED) {
            StreakReminderScheduler.checkAndSendStreakReminder(context)
        }
    }
}
