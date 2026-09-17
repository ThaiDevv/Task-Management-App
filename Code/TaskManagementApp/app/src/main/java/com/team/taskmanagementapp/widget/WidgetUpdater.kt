package com.team.taskmanagementapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Điểm refresh trung tâm của widget. Mọi thay đổi dữ liệu task (thêm/sửa/xóa/hoàn thành)
 * đều gọi [updateAll] để widget vẽ lại từ Room — widget không giữ state riêng.
 *
 * Điều phối: AppWidgetManager cập nhật từng widget instance đang tồn tại,
 * nội dung danh sách do [TaskWidgetService] (RemoteViewsService + Factory) render khi
 * AppWidgetManager gọi setData trên collection view.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "WidgetUpdater"

    /** Vẽ lại toàn bộ widget đang nằm trên màn hình chính. */
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val manager = AppWidgetManager.getInstance(appContext)
                val provider = ComponentName(appContext, TaskWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(provider)
                if (ids.isEmpty()) return@launch

                TaskWidgetProvider.pushTodaySnapshot(appContext, manager, ids)
                manager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to refresh widgets", e)
            }
        }
    }

    /** Vị trí recall từ fragment/activity đơn giản — không bao giờ văng exception. */
    fun updateAllSafe(context: Context) {
        try {
            updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "updateAllSafe swallowed an error", e)
        }
    }

    /** Đặt lịch vẽ lại widget khi ngày mới bắt đầu (ngược cuộc gọi qua AlarmManager). */
    fun scheduleMidnightRefresh(context: Context) {
        WidgetMidnightScheduler.schedule(context)
    }

    /** Snapshot đồng bộ các task hôm nay — dùng chung bởi provider và factory. */
    suspend fun loadTodayTasks(context: Context): List<com.team.taskmanagementapp.data.local.entity.Task> {
        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).taskDao()
        return dao.getTasksForDateRangeSync(
            DateTimeUtils.getStartOfToday(),
            DateTimeUtils.getEndOfToday()
        )
    }
}
