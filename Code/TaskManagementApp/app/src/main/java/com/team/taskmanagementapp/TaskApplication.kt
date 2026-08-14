package com.team.taskmanagementapp

import android.app.Application
import com.team.taskmanagementapp.util.NotificationHelper

/**
 * Custom Application class for Task Management App.
 * Responsible for initializing global resources, Notification Channels, and app-wide singletons.
 */
class TaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // TODO exercise starts in NotificationHelper.createNotificationChannel().
        NotificationHelper.createNotificationChannel(this)
    }
}
