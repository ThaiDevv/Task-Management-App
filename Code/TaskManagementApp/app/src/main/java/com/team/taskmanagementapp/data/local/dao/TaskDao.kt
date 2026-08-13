package com.team.taskmanagementapp.data.local.dao

import androidx.room.*
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // 1. Thêm công việc mới
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    // 2. Cập nhật công việc
    @Update
    suspend fun updateTask(task: Task)

    // 3. Xóa công việc
    @Delete
    suspend fun deleteTask(task: Task)

    // 4. Lấy tất cả công việc
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    // 5. Lấy 1 công việc theo ID
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: Long): Flow<Task?>

    // 6. Lọc công việc theo Trạng thái (TODO, IN_PROGRESS, COMPLETED, OVERDUE)
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    // 7. Lọc công việc theo Mức độ ưu tiên
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY dueDate ASC")
    fun getTasksByPriority(priority: Priority): Flow<List<Task>>

    // 8. Lấy công việc theo khoảng ngày
    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    fun getTasksByDateRange(startDate: Long, endDate: Long): Flow<List<Task>>

    // 9. Lấy danh sách công việc đã quá hạn
    @Query("SELECT * FROM tasks WHERE dueDate < :currentTime AND status != 'COMPLETED' ORDER BY dueDate ASC")
    fun getOverdueTasks(currentTime: Long): Flow<List<Task>>

    // 10. Tìm kiếm công việc theo Tiêu đề (Search by Title)
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' ORDER BY dueDate ASC")
    fun searchTasksByTitle(query: String): Flow<List<Task>>

    // 11. Lọc công việc nâng cao (Kết hợp nhiều điều kiện)
    @Query("""
        SELECT * FROM tasks 
        WHERE (:status IS NULL OR status = :status)
          AND (:priority IS NULL OR priority = :priority)
          AND (
              (:isOverdueOnly = 0 AND (:startDate IS NULL OR dueDate >= :startDate) AND (:endDate IS NULL OR dueDate <= :endDate))
              OR
              (:isOverdueOnly = 1 AND dueDate < :currentTime AND status != 'COMPLETED')
          )
        ORDER BY dueDate ASC
    """)
    fun getFilteredTasks(
        status: TaskStatus?,
        priority: Priority?,
        startDate: Long?,
        endDate: Long?,
        isOverdueOnly: Int,
        currentTime: Long
    ): Flow<List<Task>>

    // 12. Xóa tất cả công việc
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
