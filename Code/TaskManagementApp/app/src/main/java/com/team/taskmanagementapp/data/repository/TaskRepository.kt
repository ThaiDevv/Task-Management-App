package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.DueDateRange
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.SortOption
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

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

    fun getFilteredTasks(criteria: FilterCriteria): Flow<List<Task>> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        var startDate: Long? = null
        var endDate: Long? = null
        var isOverdueOnly = 0

        when (criteria.dueDateRange) {
            DueDateRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDate = calendar.timeInMillis
            }
            DueDateRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDate = calendar.timeInMillis
            }
            DueDateRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDate = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDate = calendar.timeInMillis
            }
            DueDateRange.OVERDUE -> {
                isOverdueOnly = 1
            }
            DueDateRange.ALL -> { /* startDate and endDate remain null */ }
        }

        return taskDao.getFilteredTasks(
            status = criteria.status,
            priority = criteria.priority,
            startDate = startDate,
            endDate = endDate,
            isOverdueOnly = isOverdueOnly,
            currentTime = currentTime
        ).map { tasks ->
            val now = System.currentTimeMillis()
            val sanitized = tasks.map { task ->
                val combined = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
                if (!task.isCompleted && task.status == TaskStatus.OVERDUE && combined > now) {
                    task.copy(status = TaskStatus.TODO)
                } else {
                    task
                }
            }
            when (criteria.sortOption) {
                SortOption.DUE_DATE_ASC -> sanitized.sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
                SortOption.DUE_DATE_DESC -> sanitized.sortedByDescending { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
                SortOption.PRIORITY_DESC -> sanitized.sortedByDescending { it.priority.ordinal }
            }
        }
    }
}
