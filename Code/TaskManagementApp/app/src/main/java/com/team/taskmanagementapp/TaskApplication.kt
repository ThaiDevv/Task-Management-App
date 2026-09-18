package com.team.taskmanagementapp

import android.app.Application
import androidx.room.InvalidationTracker
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.security.PinRepository
import com.team.taskmanagementapp.security.PinRepositoryImpl
import com.team.taskmanagementapp.util.NotificationHelper
import com.team.taskmanagementapp.widget.WidgetMidnightScheduler
import com.team.taskmanagementapp.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    /**
     * Scope sống cùng process, dùng cho các thao tác ghi **bắt buộc phải xong**.
     *
     * Lý do tồn tại: `PomodoroService` có thể bị `stopSelf()` / bị hệ thống huỷ ngay sau khi
     * một phiên Pomodoro hoàn thành. Nếu việc lưu phiên chạy trên scope của Service thì
     * transaction có thể bị cancel giữa đường và mất dữ liệu. Scope này không bị huỷ khi
     * Service chết nên bản ghi luôn được hoàn tất.
     *
     * Lưu ý: chỉ dùng cho các ghi ngắn, không dùng cho tác vụ dài.
     */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
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
