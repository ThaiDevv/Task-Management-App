package com.team.taskmanagementapp.data.model

import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus

data class FilterCriteria(
    val completion: CompletionFilter = CompletionFilter.ALL,
    val statuses: Set<TaskStatus> = emptySet(),
    val priorities: Set<Priority> = emptySet(),
    val dueDateRange: DueDateRange = DueDateRange.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val sortOption: SortOption = SortOption.DUE_DATE_ASC
)

enum class CompletionFilter {
    ALL,
    NOT_DONE,
    DONE
}

enum class DueDateRange {
    ALL,
    OVERDUE,
    TODAY,
    NEXT_7_DAYS,
    THIS_MONTH,
    NO_DUE_DATE,
    CUSTOM
}

enum class SortOption {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    PRIORITY_DESC,
    CREATED_DESC,
    UPDATED_DESC,
    TITLE_ASC
}
