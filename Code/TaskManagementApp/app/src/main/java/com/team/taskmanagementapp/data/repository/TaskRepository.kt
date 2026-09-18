package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.DueDateRange
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.SortOption
import com.team.taskmanagementapp.data.model.CompletionFilter
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale

class TaskRepository(
    private val taskDao: TaskDao
) {
    fun getAllTasks() = taskDao.getAllTasks()

    suspend fun getTaskById(id: Long) =
        taskDao.getTaskById(id)

    fun observeTaskById(id: Long): Flow<Task?> =
        taskDao.observeTaskById(id)

    suspend fun insert(task: Task) =
        taskDao.insertTask(task)

    suspend fun update(task: Task) =
        taskDao.updateTask(task)

    suspend fun delete(task: Task) =
        taskDao.deleteTask(task)

    suspend fun deleteFutureRecurringTasks(title: String, recurrenceType: RecurrenceType, startDate: Long) =
        taskDao.deleteFutureRecurringTasks(title, recurrenceType, startDate)

    suspend fun getFutureRecurringTasks(title: String, recurrenceType: RecurrenceType, startDate: Long): List<Task> =
        taskDao.getFutureRecurringTasksSync(title, recurrenceType, startDate)

    suspend fun getUncompletedTasksByTitle(title: String, excludeTaskId: Long): List<Task> =
        taskDao.getUncompletedTasksByTitleSync(title, excludeTaskId)

    suspend fun updateFutureRecurringTasks(
        originalTitle: String,
        originalRecurrence: RecurrenceType,
        startDate: Long,
        newTitle: String,
        newDescription: String,
        newPriority: Priority,
        newRecurrenceType: RecurrenceType,
        newReminderMinutes: Int,
        updatedAt: Long
    ) = taskDao.updateFutureRecurringTasks(
        originalTitle,
        originalRecurrence,
        startDate,
        newTitle,
        newDescription,
        newPriority,
        newRecurrenceType,
        newReminderMinutes,
        updatedAt
    )

    fun search(query: String) =
        taskDao.searchTasksByTitle(query)

    fun getTasksByStatus(status: TaskStatus) =
        taskDao.getTasksByStatus(status)

    fun getTasksByPriority(priority: Priority) =
        taskDao.getTasksByPriority(priority)

    fun getTasksDateRange(startDate: Long, endDate: Long) =
        taskDao.getTasksByDateRange(startDate, endDate)

    fun getOverdueTasks(currentTime: Long) =
        taskDao.getOverdueTasks(currentTime)

    suspend fun deleteAllTasks() =
        taskDao.deleteAllTasks()

    /** Returns a synchronous snapshot of all tasks for JSON serialisation. */
    suspend fun getAllTasksSync(): List<Task> =
        taskDao.getAllTasksSync()

    /** Returns the count of completed tasks for the export stats chip. */
    suspend fun getCompletedTasksCount(): Int =
        taskDao.getCompletedTasksCount()

    /** Bulk-insert a list of tasks during restore (replaces on conflict). */
    suspend fun insertAllTasks(tasks: List<Task>) =
        taskDao.insertAllTasks(tasks)

    // Kiểm tra và tự động cập nhật task quá hạn sang OVERDUE
    suspend fun checkAndUpdateOverdueTasks() {
        val now = System.currentTimeMillis()
        taskDao.markOverdueTasks(now)
    }

    fun getFilteredTasks(requestedCriteria: FilterCriteria): Flow<List<Task>> {
        val criteria = requestedCriteria.normalized()
        return taskDao.getAllTasks().map { tasks ->
            val now = System.currentTimeMillis()
            val startOfToday = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfToday = endOfDay(startOfToday)
            val endOfNextSevenDays = endOfDay(
                Calendar.getInstance().apply {
                    timeInMillis = startOfToday
                    add(Calendar.DAY_OF_YEAR, 6)
                }.timeInMillis
            )
            val monthStart = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val monthEnd = Calendar.getInstance().apply {
                timeInMillis = monthStart
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }.let { endOfDay(it.timeInMillis) }

            val filtered = tasks.map { task ->
                val combined = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
                val completed = task.isCompleted || task.status == TaskStatus.COMPLETED
                val effectiveStatus = when {
                    completed -> TaskStatus.COMPLETED
                    combined > 0L && combined < now -> TaskStatus.OVERDUE
                    task.status == TaskStatus.OVERDUE && combined > now -> TaskStatus.TODO
                    else -> task.status
                }
                task.copy(status = effectiveStatus, isCompleted = completed)
            }.filter { task ->
                val completionMatches = when (criteria.completion) {
                    CompletionFilter.ALL -> true
                    CompletionFilter.NOT_DONE -> !task.isCompleted
                    CompletionFilter.DONE -> task.isCompleted
                }
                val statusMatches = criteria.statuses.isEmpty() || task.status in criteria.statuses
                val priorityMatches = criteria.priorities.isEmpty() || task.priority in criteria.priorities
                val combinedDueAt = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
                val dueDateMatches = when (criteria.dueDateRange) {
                    DueDateRange.ALL -> true
                    DueDateRange.OVERDUE -> !task.isCompleted && combinedDueAt > 0L && combinedDueAt < now
                    DueDateRange.TODAY -> task.dueDate in startOfToday..endOfToday
                    DueDateRange.NEXT_7_DAYS -> task.dueDate in startOfToday..endOfNextSevenDays
                    DueDateRange.THIS_MONTH -> task.dueDate in monthStart..monthEnd
                    DueDateRange.NO_DUE_DATE -> task.dueDate <= 0L
                    DueDateRange.CUSTOM -> {
                        val start = criteria.customStartDate
                        val end = criteria.customEndDate
                        start != null && end != null && task.dueDate in start..end
                    }
                }
                completionMatches && statusMatches && priorityMatches && dueDateMatches
            }

            when (criteria.sortOption) {
                SortOption.DUE_DATE_ASC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.dueDate <= 0L }
                        .thenBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
                )
                SortOption.DUE_DATE_DESC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.dueDate <= 0L }
                        .thenByDescending { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
                )
                SortOption.PRIORITY_DESC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }.thenByDescending { it.priority.ordinal }
                )
                SortOption.CREATED_DESC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }.thenByDescending { it.createdAt }
                )
                SortOption.UPDATED_DESC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }.thenByDescending { it.updatedAt }
                )
                SortOption.TITLE_ASC -> filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.title.lowercase(Locale.getDefault()) }
                )
            }
        }
    }

    private fun endOfDay(dayMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
