package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.ConflictAction
import com.team.taskmanagementapp.data.model.ImportJsonFile
import com.team.taskmanagementapp.data.model.ImportResult
import com.team.taskmanagementapp.data.model.TaskConflict
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.validator.JsonValidator
import com.team.taskmanagementapp.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupRepository — Abstraction layer for backup, export, restore, and import operations.
 * Handles SAF reading/writing, JSON serialization/deserialization, and validation.
 */
class BackupRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    private val taskRepository: TaskRepository = TaskRepository(taskDao)
) {
    /**
     * Secondary constructor accepting TaskRepository and Context for ViewModel usage.
     */
    constructor(taskRepository: TaskRepository, context: Context) : this(
        context = context,
        taskDao = AppDatabase.getInstance(context).taskDao(),
        taskRepository = taskRepository
    )

    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(TaskStatus::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            val str = json.asString.uppercase()
            when (str) {
                "DONE", "COMPLETED" -> TaskStatus.COMPLETED
                "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
                "OVERDUE" -> TaskStatus.OVERDUE
                else -> TaskStatus.TODO
            }
        })
        .create()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    companion object {
        const val EXPORT_VERSION = 1
        private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TMA-52 / TMA-44: exportTasks & importTasks with StateFlow UI State
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Export all tasks to JSON file at the given URI.
     * Uses Dispatchers.IO for background work.
     *
     * @param uri The URI from SAF ACTION_CREATE_DOCUMENT
     * @return Result with number of tasks exported or error
     */
    suspend fun exportTasks(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            _uiState.value = BackupUiState.Loading("Exporting tasks...")

            val tasks = taskRepository.getAllTasks().first()

            val exportData = ExportData(
                version = EXPORT_VERSION,
                exportDate = dateFormat.format(Date()),
                taskCount = tasks.size,
                tasks = tasks.map { it.toExportTask() }
            )

            val jsonContent = gson.toJson(exportData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Failed to open output stream")

            saveLastBackupTime(System.currentTimeMillis())

            tasks.size
        }.onSuccess { count ->
            _uiState.value = BackupUiState.Success("Exported $count tasks successfully", count)
        }.onFailure { error ->
            _uiState.value = BackupUiState.Error(
                message = getErrorMessage(error),
                errorType = getErrorType(error)
            )
        }
    }

    /**
     * Import tasks from JSON file at the given URI.
     * Replaces all existing tasks.
     *
     * @param uri The URI from SAF ACTION_OPEN_DOCUMENT
     * @return Result with number of tasks imported or error
     */
    suspend fun importTasks(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            _uiState.value = BackupUiState.Loading("Importing tasks...")

            val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: throw IOException("Failed to open input stream")

            val exportData = gson.fromJson(jsonContent, ExportData::class.java)

            if (exportData.version > EXPORT_VERSION) {
                throw UnsupportedVersionException("Unsupported export version: ${exportData.version}")
            }

            val tasks = exportData.tasks.map { it.toEntity() }

            taskRepository.deleteAllTasks()
            tasks.forEach { task ->
                taskRepository.insert(task)
            }

            tasks.size
        }.onSuccess { count ->
            _uiState.value = BackupUiState.Success("Imported $count tasks successfully", count)
        }.onFailure { error ->
            _uiState.value = BackupUiState.Error(
                message = getErrorMessage(error),
                errorType = getErrorType(error)
            )
        }
    }

    /**
     * Get count of all tasks without loading them.
     */
    suspend fun getTaskCount(): Int = withContext(Dispatchers.IO) {
        taskRepository.getAllTasks().first().size
    }

    /**
     * Get last backup timestamp.
     * @return timestamp in milliseconds, or null if never backed up
     */
    fun getLastBackupTime(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, -1L)
        return if (timestamp == -1L) null else timestamp
    }

    /**
     * Reset UI state to idle.
     */
    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }

    /**
     * Get human-readable last backup time.
     */
    fun getLastBackupTimeFormatted(): String? {
        val timestamp = getLastBackupTime() ?: return null
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun saveLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
    }

    private fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is JsonSyntaxException -> "Invalid backup file format"
            is IOException -> "Failed to read/write file: ${error.message}"
            is UnsupportedVersionException -> error.message ?: "Unsupported version"
            else -> error.message ?: "Unknown error occurred"
        }
    }

    private fun getErrorType(error: Throwable): ErrorType {
        return when (error) {
            is JsonSyntaxException -> ErrorType.PARSE_ERROR
            is IOException -> ErrorType.IO_ERROR
            is UnsupportedVersionException -> ErrorType.VERSION_ERROR
            else -> ErrorType.UNKNOWN
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TMA-51: importFromJson (with JsonValidator & Conflict Resolution)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Read file content from SAF Uri
     */
    suspend fun readFileFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
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
    suspend fun parseJsonFile(jsonString: String): Result<ImportJsonFile> = withContext(Dispatchers.Default) {
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
    fun validateTasks(tasks: List<Task>): Pair<List<Task>, List<String>> {
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
    suspend fun checkConflicts(tasks: List<Task>): List<TaskConflict> = withContext(Dispatchers.IO) {
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

        // Normalize dueDate to local startOfDay so it matches app conventions
        val normalizedTasks = validTasks.map { task ->
            val normalizedDueDate = if (task.dueDate > 0) {
                com.team.taskmanagementapp.util.DateTimeUtils.getStartOfDay(task.dueDate)
            } else {
                task.dueDate
            }
            task.copy(dueDate = normalizedDueDate)
        }

        // 5. Check conflicts
        val conflicts = checkConflicts(normalizedTasks)

        // 6. Determine tasks to insert based on conflict action
        val tasksToInsert = when (conflictAction) {
            ConflictAction.SKIP -> {
                normalizedTasks.filterNot { task ->
                    conflicts.any { it.newTask.title == task.title && it.newTask.dueDate == task.dueDate }
                }
            }
            ConflictAction.REPLACE -> {
                deleteConflictingTasks(conflicts)
                normalizedTasks
            }
            ConflictAction.REPLACE_ALL -> {
                taskDao.deleteAllTasks()
                normalizedTasks
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

/**
 * UI State for backup operations.
 */
sealed class BackupUiState {
    data object Idle : BackupUiState()
    data class Loading(val message: String = "Processing...") : BackupUiState()
    data class Success(val message: String, val taskCount: Int) : BackupUiState()
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.UNKNOWN
    ) : BackupUiState()
}

/**
 * Error types for backup operations.
 */
enum class ErrorType {
    IO_ERROR,
    PARSE_ERROR,
    VERSION_ERROR,
    UNKNOWN
}

/**
 * Custom exception for unsupported export versions.
 */
class UnsupportedVersionException(message: String) : Exception(message)

/**
 * Export data structure for JSON serialization.
 */
data class ExportData(
    val version: Int,
    val exportDate: String,
    val taskCount: Int,
    val tasks: List<ExportTask>
)

/**
 * Task representation for JSON export (compatible with entity).
 */
data class ExportTask(
    val id: Int,
    val title: String,
    val description: String,
    val dueDate: Long,
    val dueTime: Long,
    val priority: Priority,
    val status: TaskStatus,
    val isCompleted: Boolean,
    val isRecurring: Boolean,
    val recurrenceType: RecurrenceType,
    val recurrenceInterval: Int,
    val reminderMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Extension function to convert Task entity to ExportTask.
 */
fun Task.toExportTask() = ExportTask(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    status = status,
    isCompleted = isCompleted,
    isRecurring = isRecurring,
    recurrenceType = recurrenceType,
    recurrenceInterval = recurrenceInterval,
    reminderMinutes = reminderMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Extension function to convert ExportTask back to Task entity.
 */
fun ExportTask.toEntity() = Task(
    id = 0, // Reset ID for new insertion
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    status = status,
    isCompleted = isCompleted,
    isRecurring = isRecurring,
    recurrenceType = recurrenceType,
    recurrenceInterval = recurrenceInterval,
    reminderMinutes = reminderMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
