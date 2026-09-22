package com.team.taskmanagementapp.data.model.stats

import com.team.taskmanagementapp.data.local.entity.Task

/**
 * Thống kê tập trung của MỘT task, đã gắn tên task để hiển thị lên UI.
 *
 * Khác với [TaskFocusStats] (projection thô của DAO chỉ có `taskId`), model này là
 * dữ liệu đã sẵn sàng để render.
 *
 * @param totalMinutes tổng số phút tập trung (chỉ tính phiên FOCUS đã hoàn thành)
 * @param sessionCount số phiên FOCUS đã hoàn thành
 */
data class TaskFocusSummary(
    val taskId: Long,
    val taskTitle: String,
    val totalMinutes: Int,
    val sessionCount: Int
)

/**
 * Toàn bộ số liệu Pomodoro hiển thị trên Statistics Dashboard (Task 15).
 *
 * Mọi số liệu đều đọc từ dữ liệu thật trong `pomodoro_sessions` theo đúng quy tắc:
 * **chỉ tính phiên FOCUS đã hoàn thành** (`sessionType = 'FOCUS' AND isCompleted = 1`).
 *
 * @param todayMinutes số phút tập trung hôm nay
 * @param weekMinutes số phút tập trung trong tuần hiện tại (bắt đầu từ Thứ Hai)
 * @param periodMinutes số phút tập trung trong kỳ đang chọn ở bộ lọc thời gian
 * @param periodSessionCount số phiên FOCUS đã hoàn thành trong kỳ đang chọn
 * @param topTasks các task tập trung nhiều nhất trong kỳ (giảm dần theo số phút)
 */
data class PomodoroFocusStats(
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val periodMinutes: Int = 0,
    val periodSessionCount: Int = 0,
    val topTasks: List<TaskFocusSummary> = emptyList()
) {
    /** Kỳ đang chọn có dữ liệu tập trung hay không. */
    val hasPeriodFocus: Boolean
        get() = periodMinutes > 0 || periodSessionCount > 0

    /** Có bất kỳ dữ liệu tập trung nào (hôm nay / tuần này / kỳ đang chọn) hay không. */
    val hasAnyFocus: Boolean
        get() = todayMinutes > 0 || weekMinutes > 0 || hasPeriodFocus
}

/** Số task tối đa hiển thị trong danh sách "task tập trung nhiều nhất". */
const val TOP_FOCUS_TASK_LIMIT = 3

/**
 * Định dạng số phút thành nhãn ngắn gọn: `0m`, `45m`, `1h`, `2h 5m`.
 *
 * Hàm thuần (không phụ thuộc Android) nên test được trên JVM.
 */
fun formatFocusDuration(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours == 0 -> "${remaining}m"
        remaining == 0 -> "${hours}h"
        else -> "${hours}h ${remaining}m"
    }
}

/**
 * Ghép số liệu thô theo task (`GROUP BY taskId` của DAO) với tên task và lấy top N.
 *
 * Quy tắc:
 * - Bỏ các `taskId` không còn tồn tại (task đã bị xoá) để không hiển thị dòng rỗng.
 * - Sắp xếp giảm dần theo số phút; cùng số phút thì nhiều phiên hơn đứng trước.
 * - Trả về danh sách rỗng khi chưa có dữ liệu ⇒ UI hiển thị trạng thái "chưa có phiên nào".
 */
fun buildTopTaskSummaries(
    focusStatsByTask: List<TaskFocusStats>,
    tasks: List<Task>,
    limit: Int = TOP_FOCUS_TASK_LIMIT
): List<TaskFocusSummary> {
    if (focusStatsByTask.isEmpty() || limit <= 0) return emptyList()

    val titleByTaskId = tasks.associate { it.id.toLong() to it.title }

    return focusStatsByTask
        .mapNotNull { stats ->
            val title = titleByTaskId[stats.taskId] ?: return@mapNotNull null
            TaskFocusSummary(
                taskId = stats.taskId,
                taskTitle = title,
                totalMinutes = stats.totalMinutes,
                sessionCount = stats.sessionCount
            )
        }
        .sortedWith(
            compareByDescending<TaskFocusSummary> { it.totalMinutes }
                .thenByDescending { it.sessionCount }
        )
        .take(limit)
}
