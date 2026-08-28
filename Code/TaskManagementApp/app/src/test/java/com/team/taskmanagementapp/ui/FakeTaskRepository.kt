package com.team.taskmanagementapp.ui

import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Minimal stub DAO chỉ dùng trong unit test.
 * Không cần Room/DB; các hàm trả về giá trị no-op.
 */
private class FakeTaskDao : TaskDao {
    override suspend fun insertTask(task: Task): Long = 0L
    override suspend fun updateTask(task: Task) = Unit
    override suspend fun deleteTask(task: Task) = Unit
    override fun getAllTasks(): Flow<List<Task>> = emptyFlow()
    override suspend fun getActiveTasksSync(): List<Task> = emptyList()
    override suspend fun getTaskById(taskId: Long): Task? = null
    override fun observeTaskById(taskId: Long): Flow<Task?> = emptyFlow()
    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = emptyFlow()
    override fun getTasksByPriority(priority: Priority): Flow<List<Task>> = emptyFlow()
    override fun getTasksByDateRange(startDate: Long, endDate: Long): Flow<List<Task>> = emptyFlow()
    override fun getOverdueTasks(currentTime: Long): Flow<List<Task>> = emptyFlow()
    override suspend fun getOverdueTasksSync(currentTime: Long): List<Task> = emptyList()
    override suspend fun markOverdueTasks(now: Long) = Unit
    override fun searchTasksByTitle(query: String): Flow<List<Task>> = emptyFlow()
    override fun getFilteredTasks(
        status: TaskStatus?,
        priority: Priority?,
        startDate: Long?,
        endDate: Long?,
        isOverdueOnly: Int,
        currentTime: Long
    ): Flow<List<Task>> = emptyFlow()
    override suspend fun deleteAllTasks() = Unit
}

/**
 * Factory function – tạo TaskRepository dùng FakeTaskDao.
 * Dùng trong OverdueRevertTest để khởi tạo AddEditTaskViewModel.
 */
fun FakeTaskRepository(): TaskRepository = TaskRepository(FakeTaskDao())
