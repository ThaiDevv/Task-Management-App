package com.team.taskmanagementapp.widget

import androidx.annotation.DrawableRes
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.util.DateTimeUtils

/**
 * Logic thuần để chuyển danh sách Task thô thành mô hình hiển thị trên widget:
 * sắp xếp (chưa xong trước, có giờ trước, đã xong xuống cuối) và thống kê tiến độ.
 * Tách riêng để unit test không cần Android.
 */
object WidgetTaskListBuilder {

    data class WidgetTaskUiModel(
        val id: Int,
        val title: String,
        val timeText: String?,
        @DrawableRes val priorityStripeRes: Int,
        val isCompleted: Boolean
    )

    data class WidgetListResult(
        val items: List<WidgetTaskUiModel>,
        val totalCount: Int,
        val completedCount: Int
    )

    fun build(tasks: List<Task>): WidgetListResult {
        val uiModels = tasks.map { it.toUiModel() }

        val pending = uiModels.filter { !it.isCompleted }
            .sortedWith(compareBy({ it.timeText == null }, { it.timeText ?: "" }))
        val completed = uiModels.filter { it.isCompleted }
            .sortedWith(compareBy({ it.timeText == null }, { it.timeText ?: "" }))

        return WidgetListResult(
            items = pending + completed,
            totalCount = uiModels.size,
            completedCount = completed.size
        )
    }

    private fun Task.toUiModel(): WidgetTaskUiModel = WidgetTaskUiModel(
        id = id,
        title = title,
        timeText = formatTimeForWidget(dueTime),
        priorityStripeRes = priority.toStripeRes(),
        isCompleted = isCompleted
    )

    private fun formatTimeForWidget(dueTimeMillis: Long): String? =
        if (dueTimeMillis > 0L) {
            DateTimeUtils.formatTimestamp(dueTimeMillis, DateTimeUtils.FORMAT_TIME_ONLY)
        } else null

    /** Widget vẽ màu bằng drawable (setBackgroundResource), không nhận color res trực tiếp. */
    private fun Priority.toStripeRes(): Int = when (this) {
        Priority.LOW -> R.drawable.widget_priority_stripe_low
        Priority.MEDIUM -> R.drawable.widget_priority_stripe_medium
        Priority.HIGH -> R.drawable.widget_priority_stripe_high
        Priority.URGENT -> R.drawable.widget_priority_stripe_urgent
    }
}
