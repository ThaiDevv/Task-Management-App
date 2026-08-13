package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.DueDateRange
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao
) {
    fun getAllTasks() = taskDao.getAllTasks()

    suspend fun getTaskById(id: Long) =
        taskDao.getTaskById(id)

    suspend fun insert(task: Task) =
        taskDao.insertTask(task)

    suspend fun update(task: Task) =
        taskDao.updateTask(task)

    suspend fun delete(task: Task) =
        taskDao.deleteTask(task)

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
        )
    }
}