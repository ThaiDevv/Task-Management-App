package com.team.taskmanagementapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("TimeChangeReceiver", "Received broadcast action: $action")

        when (action) {
            Intent.ACTION_TIME_CHANGED -> {
                Log.d("TimeChangeReceiver", "System time was changed manually.")
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d("TimeChangeReceiver", "System timezone was changed.")
            }
            Intent.ACTION_DATE_CHANGED -> {
                Log.d("TimeChangeReceiver", "System date was changed.")
            }
        }
    }
}