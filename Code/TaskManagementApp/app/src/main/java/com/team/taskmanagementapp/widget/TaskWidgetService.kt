package com.team.taskmanagementapp.widget

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.util.Constants
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService cung cấp dữ liệu cho ListView của widget.
 * Mỗi lần launcher cần vẽ lại danh sách (khi notifyAppWidgetViewDataChanged),
 * [TaskWidgetViewsFactory] load snapshot Room mới nhất và tạo RemoteViews từng dòng.
 */
class TaskWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskWidgetViewsFactory(applicationContext)
}

class TaskWidgetViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetTaskListBuilder.WidgetTaskUiModel> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val result = WidgetTaskListBuilder.build(
            runBlocking { WidgetUpdater.loadTodayTasks(context) }
        )
        items = result.items
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        views.setTextViewText(R.id.widget_item_title, item.title)
        views.setTextViewText(
            R.id.widget_item_time,
            item.timeText ?: context.getString(R.string.widget_no_time)
        )
        views.setInt(R.id.widget_item_priority_stripe, "setBackgroundResource", item.priorityStripeRes)

        if (item.isCompleted) {
            views.setInt(R.id.widget_item_title, "setPaintFlags",
                Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            views.setInt(R.id.widget_item_title, "setTextColor",
                context.getColor(R.color.widget_on_surface_variant))
            views.setImageViewResource(R.id.widget_item_checkbox, R.drawable.widget_ic_check_circle)
        } else {
            views.setInt(R.id.widget_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            views.setInt(R.id.widget_item_title, "setTextColor",
                context.getColor(R.color.widget_on_surface))
            views.setImageViewResource(R.id.widget_item_checkbox, R.drawable.widget_ic_check_circle_outline)
        }

        // Chạm vào dòng = mở chi tiết; chạm checkbox = tick hoàn thành.
        // Cả hai dùng chung PendingIntent template (đã đặt FLAG_MUTABLE trong
        // TaskWidgetProvider) và phân biệt hành động qua EXTRA_WIDGET_ACTION.
        views.setOnClickFillInIntent(
            R.id.widget_item_row,
            Intent().apply {
                putExtra(Constants.EXTRA_WIDGET_TASK_ID, item.id)
                putExtra(Constants.EXTRA_WIDGET_ACTION, Constants.WIDGET_ACTION_OPEN_TASK)
            }
        )
        views.setOnClickFillInIntent(
            R.id.widget_item_checkbox,
            Intent().apply {
                putExtra(Constants.EXTRA_WIDGET_TASK_ID, item.id)
                putExtra(Constants.EXTRA_WIDGET_ACTION, Constants.WIDGET_ACTION_TOGGLE_TASK)
            }
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun hasStableIds(): Boolean = true
}
