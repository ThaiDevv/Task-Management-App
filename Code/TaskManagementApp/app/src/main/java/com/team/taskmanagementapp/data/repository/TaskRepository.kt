package com.team.taskmanagementapp.data.repository

import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enum.Priority
import com.team.taskmanagementapp.data.model.enum.TaskStatus

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
}