package com.team.taskmanagementapp.data.model

import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus

data class FilterCriteria(
    val status: TaskStatus? = null,
    val priority: Priority? = null,
    val dueDateRange: DueDateRange = DueDateRange.ALL
)

enum class DueDateRange {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    OVERDUE
}