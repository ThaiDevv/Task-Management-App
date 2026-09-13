package com.team.taskmanagementapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.team.taskmanagementapp.MainActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.ui.activity.AddEditTaskActivity
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.TaskCompletionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AppWidgetProvider của widget "Việc hôm nay".
 *
 * Cơ chế:
 * - Header (tiêu đề, tiến độ, số việc còn lại) vẽ trực tiếp bằng RemoteViews trong
 *   [pushTodaySnapshot] — được cập nhật mỗi khi dữ liệu đổi.
 * - Danh sách task render bởi [TaskWidgetService] (RemoteViewsService) qua setRemoteAdapter,
 *   click từng dòng xử lý bằng PendingIntent template + fill-in intent.
 * - Mọi thay đổi task gọi [WidgetUpdater.updateAll] để refresh, widget không giữ state riêng.
 */
class TaskWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                WidgetMidnightScheduler.schedule(context)
                pushTodaySnapshot(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Constants.ACTION_WIDGET_TOGGLE_TASK -> {
                val taskId = intent.getIntExtra(Constants.EXTRA_WIDGET_TASK_ID, -1)
                if (taskId == -1) return
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        TaskCompletionHelper.toggleById(context, taskId)
                        WidgetUpdater.updateAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Constants.ACTION_WIDGET_OPEN_TASK -> {
                val taskId = intent.getIntExtra(Constants.EXTRA_WIDGET_TASK_ID, -1)
                if (taskId == -1) return
                // Template chỉ có MỘT action; loại click cụ thể (mở chi tiết hay hoàn
                // thành) được truyền qua fill-in intent của từng dòng trong Factory.
                when (intent.getIntExtra(Constants.EXTRA_WIDGET_ACTION, Constants.WIDGET_ACTION_OPEN_TASK)) {
                    Constants.WIDGET_ACTION_TOGGLE_TASK -> {
                        val pendingResult = goAsync()
                        scope.launch {
                            try {
                                TaskCompletionHelper.toggleById(context, taskId)
                                WidgetUpdater.updateAll(context)
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                    else -> openTaskDetail(context, taskId)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Không dọn gì cả: WorkManager periodic dùng chung cho mọi instance.
    }

    private fun openTaskDetail(context: Context, taskId: Int) {
        val intent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_TASK_ID, taskId.toLong())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {

        /**
         * Vẽ header của widget từ snapshot Room hiện tại và gắn adapter + click template.
         * Gọi từ onUpdate (khi launcher thêm widget) và từ [WidgetUpdater] (khi dữ liệu đổi).
         */
        suspend fun pushTodaySnapshot(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val appContext = context.applicationContext
            val result = WidgetTaskListBuilder.build(WidgetUpdater.loadTodayTasks(appContext))

            appWidgetIds.forEach { widgetId ->
                val views = buildRemoteViews(appContext, result)
                setRemoteAdapter(appContext, views, widgetId)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            result: WidgetTaskListBuilder.WidgetListResult
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today_tasks)

            views.setTextViewText(
                R.id.widget_date_text,
                SimpleDateFormat("EEEE, dd/MM", Locale.getDefault())
                    .format(Date())
                    .replaceFirstChar { it.uppercase(Locale.getDefault()) }
            )

            val pendingCount = result.totalCount - result.completedCount
            views.setTextViewText(
                R.id.widget_count_chip,
                if (pendingCount == 0) {
                    context.getString(R.string.widget_all_done)
                } else {
                    context.getString(R.string.widget_pending_count, pendingCount)
                }
            )

            val percent = if (result.totalCount == 0) 0
            else result.completedCount * 100 / result.totalCount
            // RemoteViews của SDK này không có setProgress — gọi gián tiếp qua setInt.
            views.setInt(R.id.widget_progress, "setMax", 100)
            views.setInt(R.id.widget_progress, "setProgress", percent)

            // Click hành động trên header
            views.setOnClickPendingIntent(
                R.id.widget_header,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_add_button,
                PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, AddEditTaskActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Click từng dòng trong danh sách: template chung, dòng cụ thể bổ sung
            // extras bằng fill-in intent (xem TaskWidgetViewsFactory).
            views.setPendingIntentTemplate(
                android.R.id.list,
                PendingIntent.getBroadcast(
                    context,
                    2,
                    Intent(context, TaskWidgetProvider::class.java).apply {
                        action = Constants.ACTION_WIDGET_OPEN_TASK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return views
        }

        private fun setRemoteAdapter(
            context: Context,
            views: RemoteViews,
            widgetId: Int
        ) {
            val intent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setRemoteAdapter(android.R.id.list, intent)
            } else {
                @Suppress("DEPRECATION")
                views.setRemoteAdapter(android.R.id.list, intent)
            }
            views.setEmptyView(android.R.id.list, R.id.widget_empty_view)
        }
    }
}
