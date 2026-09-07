package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.ConflictAction
import com.team.taskmanagementapp.data.model.ImportJsonFile
import com.team.taskmanagementapp.data.model.ImportResult
import com.team.taskmanagementapp.data.model.TaskConflict
import com.team.taskmanagementapp.data.model.ValidationResult
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.validator.JsonValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepository(
    private val context: Context,
    private val taskDao: TaskDao
) {
    private val gson = Gson()

    /**
     * Read file content from SAF Uri
     */
    private suspend fun readFileFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))

            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse JSON string to ImportJsonFile
     */
    private suspend fun parseJsonFile(jsonString: String): Result<ImportJsonFile> = withContext(Dispatchers.Default) {
        try {
            val importFile = gson.fromJson(jsonString, ImportJsonFile::class.java)
            Result.success(importFile)
        } catch (e: JsonParseException) {
            Result.failure(Exception("Invalid JSON format: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Parse error: ${e.message}"))
        }
    }

    /**
     * Validate tasks list (check required fields)
     */
    private fun validateTasks(tasks: List<Task>): Pair<List<Task>, List<String>> {
        val validTasks = mutableListOf<Task>()
        val errors = mutableListOf<String>()

        tasks.forEachIndexed { index, task ->
            when {
                task.title.isBlank() -> {
                    errors.add("Task #${index + 1}: Title cannot be empty")
                }
                task.dueDate <= 0 -> {
                    errors.add("Task #${index + 1} '${task.title}': Invalid due date")
                }
                else -> {
                    validTasks.add(task)
                }
            }
        }

        return Pair(validTasks, errors)
    }

    /**
     * Check conflicts: find existing task with same (title + dueDate + recurrenceType)
     */
    private suspend fun checkConflicts(tasks: List<Task>): List<TaskConflict> = withContext(Dispatchers.IO) {
        val conflicts = mutableListOf<TaskConflict>()

        for (newTask in tasks) {
            val existing = taskDao.getConflictingTask(
                title = newTask.title,
                dueDate = newTask.dueDate,
                recurrenceType = newTask.recurrenceType
            )
            if (existing != null) {
                conflicts.add(TaskConflict(newTask, existing))
            }
        }

        conflicts
    }

    /**
     * Main import function: SAF + Parse + Validate + Conflict Check + Insert
     */
    suspend fun importFromJson(
        uri: Uri,
        conflictAction: ConflictAction = ConflictAction.SKIP
    ): ImportResult = withContext(Dispatchers.Default) {
        // 1. Read file
        val readResult = readFileFromUri(uri)
        if (readResult.isFailure) {
            return@withContext ImportResult(
                errorMessage = "Failed to read file: ${readResult.exceptionOrNull()?.message}"
            )
        }

        val jsonString = readResult.getOrNull() ?: return@withContext ImportResult(
            errorMessage = "Empty file"
        )

        // 2. VALIDATE JSON (Atomic: check all errors before parsing)
        val validationResult = JsonValidator.validate(jsonString)
        if (!validationResult.isValid) {
            return@withContext ImportResult(
                errorMessage = "Invalid JSON file:\n${validationResult.errorMessages.joinToString("\n")}"
            )
        }

        // 3. Parse JSON
        val parseResult = parseJsonFile(jsonString)
        if (parseResult.isFailure) {
            return@withContext ImportResult(
                errorMessage = "JSON parse error: ${parseResult.exceptionOrNull()?.message}"
            )
        }

        val importFile = parseResult.getOrNull() ?: return@withContext ImportResult(
            errorMessage = "Failed to parse JSON file"
        )

        if (importFile.tasks.isEmpty()) {
            return@withContext ImportResult(
                errorMessage = "No tasks found in file"
            )
        }

        // 4. Validate tasks (second-level validation after JSON schema)
        val (validTasks, validationErrors) = validateTasks(importFile.tasks)
        if (validTasks.isEmpty()) {
            return@withContext ImportResult(
                failureCount = importFile.tasks.size,
                errorMessage = "All tasks failed validation:\n${validationErrors.joinToString("\n")}"
            )
        }

        val failureCount = importFile.tasks.size - validTasks.size

        // 5. Check conflicts
        val conflicts = checkConflicts(validTasks)

        // 6. Determine tasks to insert based on conflict action
        val tasksToInsert = when (conflictAction) {
            ConflictAction.SKIP -> {
                // Insert only non-conflicting tasks
                validTasks.filterNot { task ->
                    conflicts.any { it.newTask.title == task.title && it.newTask.dueDate == task.dueDate }
                }
            }
            ConflictAction.REPLACE -> {
                // Delete conflicting tasks, then insert all valid tasks
                deleteConflictingTasks(conflicts)
                validTasks
            }
            ConflictAction.REPLACE_ALL -> {
                // Delete all existing tasks, then insert all new tasks
                taskDao.deleteAllTasks()
                validTasks
            }
        }

        // 7. Batch insert with @Transaction (atomic: all or nothing)
        return@withContext try {
            taskDao.insertBatch(tasksToInsert)
            ImportResult(
                successCount = tasksToInsert.size,
                failureCount = failureCount,
                skipCount = if (conflictAction == ConflictAction.SKIP) conflicts.size else 0,
                conflicts = if (conflictAction == ConflictAction.SKIP) conflicts else emptyList()
            )
        } catch (e: Exception) {
            ImportResult(
                failureCount = validTasks.size + failureCount,
                errorMessage = "Database error: ${e.message}"
            )
        }
    }

    /**
     * Delete conflicting tasks
     */
    private suspend fun deleteConflictingTasks(conflicts: List<TaskConflict>) {
        conflicts.forEach { conflict ->
            taskDao.deleteTask(conflict.existingTask)
        }
    }

    /**
     * Export tasks to JSON file
     */
    suspend fun exportToJson(tasks: List<Task>): String = withContext(Dispatchers.Default) {
        val exportFile = ImportJsonFile(
            version = "1.0",
            exportedAt = System.currentTimeMillis(),
            exportedBy = "TaskManagementApp",
            tasks = tasks,
            taskCount = tasks.size
        )
        gson.toJson(exportFile)
    }
}
