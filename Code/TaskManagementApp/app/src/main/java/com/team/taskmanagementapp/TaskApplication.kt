package com.team.taskmanagementapp

import android.app.Application

/**
 * Custom Application class for Task Management App.
 * Responsible for initializing global resources, Notification Channels, and app-wide singletons.
 */
class TaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialization logic for notification channels, etc. will be registered here
    }
}
