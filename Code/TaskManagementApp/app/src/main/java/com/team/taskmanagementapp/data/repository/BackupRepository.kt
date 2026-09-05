package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for backup and restore operations.
 * Handles export/import of tasks to/from JSON files using SAF (Storage Access Framework).
 */
class BackupRepository(
    private val taskRepository: TaskRepository,
    private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    companion object {
        const val EXPORT_VERSION = 1
    }

    /**
     * Export all tasks to JSON file at the given URI.
     * Uses Dispatchers.IO for background work.
     *
     * @param uri The URI from SAF ACTION_CREATE_DOCUMENT
     * @return Number of tasks exported
     * @throws Exception if write fails
     */
    suspend fun exportToJson(uri: Uri): Int = withContext(Dispatchers.IO) {
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
        } ?: throw IllegalStateException("Failed to open output stream")

        tasks.size
    }

    /**
     * Import tasks from JSON file at the given URI.
     * Replaces all existing tasks.
     *
     * @param uri The URI from SAF ACTION_OPEN_DOCUMENT
     * @return Number of tasks imported
     * @throws Exception if read fails
     */
    suspend fun importFromJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        // Read JSON content
        val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw IllegalStateException("Failed to open input stream")

        // Parse JSON
        val exportData = gson.fromJson(jsonContent, ExportData::class.java)

        // Validate version
        if (exportData.version > EXPORT_VERSION) {
            throw IllegalStateException("Unsupported export version: ${exportData.version}")
        }

        // Convert and insert tasks
        val tasks = exportData.tasks.map { it.toEntity() }

        // Clear existing tasks and insert new ones
        taskRepository.deleteAllTasks()
        tasks.forEach { task ->
            taskRepository.insert(task)
        }

        tasks.size
    }

    /**
     * Get count of all tasks without loading them.
     */
    suspend fun getTaskCount(): Int = withContext(Dispatchers.IO) {
        taskRepository.getAllTasks().first().size
    }
}

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
