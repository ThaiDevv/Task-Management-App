package com.team.taskmanagementapp.ui.pomodoro

import android.content.Context
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.util.DateTimeUtils

/**
 * Format phần mô tả công việc cho Pomodoro (Task 10), dùng chung cho
 * [PomodoroTaskAdapter] và màn hình Pomodoro để tránh trùng lặp logic hiển thị.
 */
internal object PomodoroTaskFormatter {

    /** Ví dụ: "Hạn 31/12/2026" hoặc "Hạn 31/12/2026 • Đã xong". */
    fun dueDateLabel(context: Context, task: Task): String {
        val dueDate = DateTimeUtils.formatTimestamp(
            task.dueDate.takeIf { it > 0L },
            DateTimeUtils.FORMAT_DATE_ONLY
        )
        val dueText = if (dueDate.isBlank()) {
            context.getString(R.string.pomodoro_task_no_due_date)
        } else {
            context.getString(R.string.pomodoro_task_due_format, dueDate)
        }
        return if (task.isCompleted) {
            dueText + " • " + context.getString(R.string.pomodoro_task_completed)
        } else {
            dueText
        }
    }
}
