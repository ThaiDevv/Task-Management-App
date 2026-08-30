package com.team.taskmanagementapp

import android.app.Application
import com.team.taskmanagementapp.security.PinRepository
import com.team.taskmanagementapp.security.PinRepositoryImpl
import com.team.taskmanagementapp.util.NotificationHelper

/**
 * Custom Application class for Task Management App.
 * Responsible for initializing global resources, Notification Channels, and app-wide singletons.
 */
class TaskApplication : Application() {

    /**
     * Singleton PinRepository — dùng chung cho toàn ứng dụng.
     * Truy cập từ bất kỳ đâu:
     *   (context.applicationContext as TaskApplication).pinRepository
     * Hoặc dùng extension fun:
     *   context.pinRepository()
     */
    val pinRepository: PinRepository by lazy {
        PinRepositoryImpl.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}

/**
 * Extension function — truy cập PinRepository dễ dàng từ bất kỳ Context nào.
 * Ví dụ dùng trong Fragment/Activity:
 *   val pinRepo = requireContext().pinRepository()
 */
fun android.content.Context.pinRepository(): PinRepository =
    (applicationContext as TaskApplication).pinRepository
