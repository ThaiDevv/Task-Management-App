package com.team.taskmanagementapp.data.model

import com.team.taskmanagementapp.data.local.entity.Task

// ==================== CONFLICT HANDLING ====================
enum class ConflictAction {
    SKIP,          // Bỏ qua task trùng, chỉ import non-conflicting
    REPLACE,       // Xóa task cũ, insert task mới
    REPLACE_ALL    // Xóa ALL tasks, import all mới
}

// ==================== IMPORT RESULT ====================
data class ImportResult(
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val skipCount: Int = 0,
    val conflicts: List<TaskConflict> = emptyList(),
    val errorMessage: String? = null,
    val isSuccess: Boolean = errorMessage == null
)

// ==================== TASK CONFLICT ====================
data class TaskConflict(
    val newTask: Task,
    val existingTask: Task,
    val conflictKey: String = "${newTask.title}|${newTask.dueDate}|${newTask.recurrenceType}"
)

// ==================== JSON IMPORT MODEL ====================
data class ImportJsonFile(
    val version: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val exportedBy: String = "TaskManagementApp",
    val tasks: List<Task> = emptyList(),
    val taskCount: Int = tasks.size
)

// ==================== JSON DTO (For Deserialization) ====================
data class TaskJsonDto(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0,
    val dueTime: Long = 0,
    val priority: String = "MEDIUM",
    val status: String = "TODO",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1,
    val reminderMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
