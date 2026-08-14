package com.team.taskmanagementapp.data.model

import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus

data class FilterCriteria(
    val status: TaskStatus? = null,
    val priority: Priority? = null,
    val dueDateRange: DueDateRange = DueDateRange.ALL,
    val sortOption: SortOption = SortOption.DUE_DATE_ASC
)

enum class DueDateRange {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    OVERDUE
}

enum class SortOption {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    PRIORITY_DESC
}