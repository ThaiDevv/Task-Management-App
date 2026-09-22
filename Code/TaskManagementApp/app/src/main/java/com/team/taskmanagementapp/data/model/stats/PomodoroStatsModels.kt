package com.team.taskmanagementapp.data.model.stats

/**
 * Kết quả tổng hợp thời gian tập trung của MỘT task trong một khoảng thời gian.
 *
 * Đây là projection POJO cho query `GROUP BY taskId` của `PomodoroDao`
 * (tên field phải khớp alias trong câu SELECT).
 */
data class TaskFocusStats(
    val taskId: Long,
    val totalMinutes: Int,
    val sessionCount: Int
)
