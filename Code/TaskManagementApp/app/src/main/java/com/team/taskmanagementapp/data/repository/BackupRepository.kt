package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
 * Repository for backup and restore operations.
 * Handles export/import of tasks to/from JSON files using SAF (Storage Access Framework).
 *
 * Features:
 * - Background I/O with Dispatchers.IO
 * - Error handling with runCatching{}
 * - StateFlow for UI state updates
 * - Last backup timestamp tracking
 */
class BackupRepository(
    private val taskRepository: TaskRepository,
    private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
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

    /**
     * Export all tasks to JSON file at the given URI.
     * Uses Dispatchers.IO for background work.
     *
     * @param uri The URI from SAF ACTION_CREATE_DOCUMENT
     * @return Result with number of tasks exported or error
     */
    suspend fun exportTasks(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            // Emit loading state
            _uiState.value = BackupUiState.Loading("Exporting tasks...")

            // Query all tasks from database
            val tasks = taskRepository.getAllTasks().first()

            // Create export data structure
            val exportData = ExportData(
                version = EXPORT_VERSION,
                exportDate = dateFormat.format(Date()),
                taskCount = tasks.size,
                tasks = tasks.map { it.toExportTask() }
            )

            // Serialize to JSON
            val jsonContent = gson.toJson(exportData)

            // Write to file via content resolver
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Failed to open output stream")

            // Save last backup timestamp
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
            // Emit loading state
            _uiState.value = BackupUiState.Loading("Importing tasks...")

            // Read JSON content
            val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: throw IOException("Failed to open input stream")

            // Parse JSON
            val exportData = gson.fromJson(jsonContent, ExportData::class.java)

            // Validate version
            if (exportData.version > EXPORT_VERSION) {
                throw UnsupportedVersionException("Unsupported export version: ${exportData.version}")
            }

            // Convert and insert tasks
            val tasks = exportData.tasks.map { it.toEntity() }

            // Clear existing tasks and insert new ones
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

    private fun saveLastBackupTime(timestamp: Long) {
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
