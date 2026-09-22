package com.team.taskmanagementapp

import android.app.Application
import androidx.room.InvalidationTracker
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.security.PinRepository
import com.team.taskmanagementapp.security.PinRepositoryImpl
import com.team.taskmanagementapp.util.NotificationHelper
import com.team.taskmanagementapp.widget.WidgetMidnightScheduler
import com.team.taskmanagementapp.widget.WidgetUpdater

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
        AiAppCheckInitializer.initialize(this)
        NotificationHelper.createNotificationChannel(this)
        setupWidgetAutoRefresh()
    }

    /**
     * Widget tự cập nhật khi bảng tasks bị thay đổi (Room InvalidationTracker)
     * và đổi nội dung sang ngày mới sau nửa đêm (WorkManager periodic).
     */
    private fun setupWidgetAutoRefresh() {
        AppDatabase.getInstance(this).invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("tasks") {
                override fun onInvalidated(tables: Set<String>) {
                    WidgetUpdater.updateAll(this@TaskApplication)
                }
            }
        )
        WidgetMidnightScheduler.schedule(this)
    }
}

/**
 * Extension function — truy cập PinRepository dễ dàng từ bất kỳ Context nào.
 * Ví dụ dùng trong Fragment/Activity:
 *   val pinRepo = requireContext().pinRepository()
 */
fun android.content.Context.pinRepository(): PinRepository =
    (applicationContext as TaskApplication).pinRepository
