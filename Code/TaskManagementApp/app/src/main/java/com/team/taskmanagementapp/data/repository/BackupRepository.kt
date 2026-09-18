package com.team.taskmanagementapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.team.taskmanagementapp.data.local.dao.PomodoroDao
import com.team.taskmanagementapp.data.local.dao.TaskDao
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.ConflictAction
import com.team.taskmanagementapp.data.model.ImportJsonFile
import com.team.taskmanagementapp.data.model.ImportResult
import com.team.taskmanagementapp.data.model.TaskConflict
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.validator.BackupValidator
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
 * Repository for backup and restore operations (TMA-50, TMA-51, TMA-52, Task 16).
 * Handles export/import of tasks to/from JSON files using SAF (Storage Access Framework).
 *
 * Task 16 bổ sung **backup/restore toàn bộ dữ liệu**:
 * - `exportBackup(uri)`: ghi file JSON có version chứa Task (+ counters Pomodoro) và PomodoroSession
 * - `restoreBackup(uri)`: validate file rồi ghi lại toàn bộ trong **một transaction** của Room
 */
class BackupRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    private val taskRepository: TaskRepository = TaskRepository(taskDao),
    private val pomodoroDao: PomodoroDao = AppDatabase.getInstance(context).pomodoroDao(),
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    /**
     * Secondary constructor accepting TaskRepository and Context for ViewModel or testing usage.
     */
    constructor(taskRepository: TaskRepository, context: Context) : this(
        context = context,
        taskDao = AppDatabase.getInstance(context).taskDao(),
        taskRepository = taskRepository
    )

    /**
     * Gson dùng cho MỌI file backup/import (cấu hình chung trong companion để test JVM
     * dùng lại đúng cấu hình thật).
     */
    val gson: Gson get() = backupGson

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    companion object {
        /**
         * Phiên bản format backup.
         * v1: chỉ có tasks. v2 (Task 16): thêm counters Pomodoro của Task + `pomodoroSessions`.
         */
        const val EXPORT_VERSION = 2
        private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
        private const val KEY_RECENT_BACKUPS = "key_recent_backups"
        private const val MAX_RECENT_BACKUPS = 10

        /**
         * Gson của tầng backup: pretty-print + chấp nhận cả `DONE` (định dạng cũ) cho `status`.
         */
        val backupGson: Gson = GsonBuilder()
            .setPrettyPrinting()
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
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TMA-50 / TMA-52: Export Tasks to JSON via SAF
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
            val sessions = pomodoroDao.getAllSessions()

            val exportData = ExportData(
                version = EXPORT_VERSION,
                exportDate = dateFormat.format(Date()),
                taskCount = tasks.size,
                tasks = tasks.map { it.toExportTask() },
                pomodoroSessionCount = sessions.size,
                pomodoroSessions = sessions.map { it.toExportSession() }
            )

            writeBackupToUri(uri, exportData)

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
     * Task 16 — **Backup toàn bộ dữ liệu** ra file JSON qua SAF.
     *
     * File gồm: danh sách Task (kèm counters Pomodoro) + toàn bộ PomodoroSession,
     * tất cả **giữ nguyên id** để quan hệ Task ↔ PomodoroSession được bảo toàn.
     *
     * @param uri URI từ SAF `ACTION_CREATE_DOCUMENT`
     * @return [BackupSummary] số lượng Task và PomodoroSession đã ghi
     */
    suspend fun exportBackup(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            _uiState.value = BackupUiState.Loading("Creating backup...")

            val tasks = taskDao.getAllTasksSync()
            val sessions = pomodoroDao.getAllSessions()

            val exportData = ExportData(
                version = EXPORT_VERSION,
                exportDate = dateFormat.format(Date()),
                taskCount = tasks.size,
                tasks = tasks.map { it.toExportTask() },
                pomodoroSessionCount = sessions.size,
                pomodoroSessions = sessions.map { it.toExportSession() }
            )

            writeBackupToUri(uri, exportData)
            saveLastBackupTime(System.currentTimeMillis())

            BackupSummary(taskCount = tasks.size, sessionCount = sessions.size)
        }.onSuccess { summary ->
            _uiState.value = BackupUiState.Success(
                message = "Backed up ${summary.taskCount} tasks and ${summary.sessionCount} sessions",
                taskCount = summary.taskCount
            )
        }.onFailure { error ->
            _uiState.value = BackupUiState.Error(
                message = getErrorMessage(error),
                errorType = getErrorType(error)
            )
        }
    }

    /**
     * Task 16 — **Restore từ file backup** (thay thế toàn bộ dữ liệu hiện có).
     *
     * Quy trình:
     * 1. Đọc file qua SAF (báo lỗi rõ ràng nếu không đọc được)
     * 2. **[BackupValidator]** kiểm tra phiên bản + cấu trúc + tính toàn vẹn khoá ngoại
     *    (mọi `PomodoroSession.taskId` phải tồn tại trong danh sách Task của file)
     * 3. Chỉ khi hợp lệ mới ghi vào Room, **toàn bộ trong một transaction**:
     *    xoá dữ liệu cũ → ghi Task (giữ id) → ghi PomodoroSession (giữ id).
     *    Nếu bất kỳ bước nào lỗi, transaction rollback nên không có restore dở dang.
     *
     * @return [BackupSummary] số lượng đã restore
     */
    suspend fun restoreBackup(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            _uiState.value = BackupUiState.Loading("Restoring backup...")

            val jsonContent = readTextFromUri(uri)

            val validation = BackupValidator.validate(jsonContent, EXPORT_VERSION)
            if (!validation.isValid) {
                throw InvalidBackupException(validation.errorMessages)
            }

            val exportData = gson.fromJson(jsonContent, ExportData::class.java)
                ?: throw InvalidBackupException(listOf("Backup file is empty"))

            val summary = applyBackup(
                backup = exportData,
                inTransaction = { block -> database.withTransaction { block() } },
                clearAllTasks = { taskDao.deleteAllTasks() },
                clearAllSessions = { pomodoroDao.deleteAllSessions() },
                insertTasks = { tasks -> taskDao.insertAllTasks(tasks) },
                insertSessions = { sessions -> pomodoroDao.insertSessions(sessions) }
            )

            summary
        }.onSuccess { summary ->
            _uiState.value = BackupUiState.Success(
                message = "Restored ${summary.taskCount} tasks and ${summary.sessionCount} sessions",
                taskCount = summary.taskCount
            )
        }.onFailure { error ->
            _uiState.value = BackupUiState.Error(
                message = getErrorMessage(error),
                errorType = getErrorType(error)
            )
        }
    }

    /**
     * Export all tasks to JSON file at the given URI.
     * TMA-50 direct API returning task count or throwing exception.
     *
     * @param uri The URI from SAF ACTION_CREATE_DOCUMENT
     * @return Number of tasks exported
     * @throws Exception if write fails
     */
    suspend fun exportToJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        exportTasks(uri).getOrThrow()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TMA-52: Import tasks (StateFlow UI State)
    // ═══════════════════════════════════════════════════════════════════════════

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

    /** Số task đã hoàn thành — dùng cho chip thống kê ở màn Backup. */
    suspend fun getCompletedTaskCount(): Int = withContext(Dispatchers.IO) {
        taskDao.getCompletedTasksCount()
    }

    /** Số phiên Pomodoro hiện có — dùng cho chip thống kê và thông báo backup/restore. */
    suspend fun getPomodoroSessionCount(): Int = withContext(Dispatchers.IO) {
        pomodoroDao.getAllSessions().size
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

    /**
     * Get list of recent backups sorted by newest first.
     */
    fun getRecentBackups(): List<BackupHistoryItem> {
        val json = prefs.getString(KEY_RECENT_BACKUPS, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<BackupHistoryItem>>() {}.type
            gson.fromJson<List<BackupHistoryItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a backup entry to recent history and update last backup time.
     */
    fun addRecentBackup(fileName: String, formattedDate: String, sizeString: String, timestamp: Long) {
        val current = getRecentBackups().toMutableList()
        current.removeAll { it.fileName == fileName }
        current.add(0, BackupHistoryItem(fileName, formattedDate, sizeString, timestamp))
        val limited = current.take(MAX_RECENT_BACKUPS)
        prefs.edit()
            .putString(KEY_RECENT_BACKUPS, gson.toJson(limited))
            .putLong(KEY_LAST_BACKUP_TIME, timestamp)
            .apply()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Task 16: Backup / Restore toàn bộ dữ liệu (SAF)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Ghi nội dung backup ra URI do SAF cấp. */
    private fun writeBackupToUri(uri: Uri, exportData: ExportData) {
        val jsonContent = gson.toJson(exportData)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Failed to open output stream")
    }

    /** Đọc toàn bộ nội dung file backup từ URI do SAF cấp. */
    private fun readTextFromUri(uri: Uri): String {
        val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Failed to open input stream")

        if (content.isBlank()) {
            throw InvalidBackupException(listOf("Backup file is empty"))
        }
        return content
    }

    private fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is JsonSyntaxException -> "Invalid backup file format"
            is InvalidBackupException -> error.message ?: "Invalid backup file"
            is IOException -> "Failed to read/write file: ${error.message}"
            is UnsupportedVersionException -> error.message ?: "Unsupported version"
            else -> error.message ?: "Unknown error occurred"
        }
    }

    private fun getErrorType(error: Throwable): ErrorType {
        return when (error) {
            is JsonSyntaxException -> ErrorType.PARSE_ERROR
            is InvalidBackupException -> ErrorType.VALIDATION_ERROR
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
     * Export tasks to JSON file (for TMA-51 / ImportJsonFile)
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
    VALIDATION_ERROR,
    UNKNOWN
}

/**
 * Custom exception for unsupported export versions.
 */
class UnsupportedVersionException(message: String) : Exception(message)

/**
 * Ném ra khi file backup sai cấu trúc / thiếu dữ liệu bắt buộc / vi phạm khoá ngoại.
 *
 * Thông điệp gộp tất cả lỗi validate (atomic: chỉ cần 1 lỗi là từ chối cả file).
 */
class InvalidBackupException(val errors: List<String>) : Exception(
    "Invalid backup file:\n" + errors.joinToString("\n")
)

/**
 * Áp một bản backup vào DB **trong đúng một transaction**.
 *
 * Trình tự: xoá dữ liệu hiện có → ghi Task (giữ nguyên id) → ghi PomodoroSession
 * (giữ nguyên id + taskId). Nhờ nằm trong transaction nên nếu bước nào lỗi
 * (ví dụ vi phạm khoá ngoại) thì Room rollback và DB giữ nguyên trạng thái cũ,
 * không bao giờ có restore dở dang.
 *
 * Tách thành hàm riêng (nhận lambda) để test được logic thứ tự + rollback trên JVM
 * mà không cần Room/Android.
 *
 * @param inTransaction bộ chạy transaction thật (production: `database.withTransaction`)
 * @return [BackupSummary] số Task / PomodoroSession đã ghi
 */
internal suspend fun applyBackup(
    backup: ExportData,
    inTransaction: suspend (suspend () -> Unit) -> Unit,
    clearAllTasks: suspend () -> Unit,
    clearAllSessions: suspend () -> Unit,
    insertTasks: suspend (List<Task>) -> Unit,
    insertSessions: suspend (List<PomodoroSession>) -> Unit
): BackupSummary {
    // Giữ nguyên ID để PomodoroSession.taskId vẫn trỏ đúng Task sau khi restore.
    val tasks = backup.tasks.map { it.toEntity(preserveId = true) }
    val sessions = backup.pomodoroSessions.mapNotNull { it.toEntity() }

    inTransaction {
        clearAllTasks()
        clearAllSessions()
        insertTasks(tasks)
        insertSessions(sessions)
    }

    return BackupSummary(taskCount = tasks.size, sessionCount = sessions.size)
}

/**
 * Export data structure for JSON serialization.
 *
 * @param version phiên bản format — tăng khi cấu trúc thay đổi để có thể từ chối file cũ/mới
 *        không tương thích. v2 (Task 16) bổ sung `pomodoroSessions` + counters Pomodoro của Task.
 * @param tasks danh sách Task **giữ nguyên id** để bảo toàn quan hệ với PomodoroSession
 * @param pomodoroSessions lịch sử phiên Pomodoro (giữ nguyên id + taskId)
 */
data class ExportData(
    val version: Int,
    val exportDate: String,
    val taskCount: Int,
    val tasks: List<ExportTask>,
    val pomodoroSessionCount: Int = 0,
    val pomodoroSessions: List<ExportPomodoroSession> = emptyList()
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
    val updatedAt: Long,
    // ── Pomodoro counters (Task 16) ──
    val estimatedPomodoros: Int = 0,
    val completedPomodoros: Int = 0,
    val totalFocusTimeMinutes: Int = 0
)

/**
 * PomodoroSession representation for JSON export (compatible with entity).
 *
 * `sessionType` lưu dạng chuỗi (`FOCUS` / `SHORT_BREAK` / `LONG_BREAK`) để file backup
 * không phụ thuộc vào thứ tự khai báo enum của phiên bản app.
 */
data class ExportPomodoroSession(
    val id: Long,
    val taskId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationInMinutes: Int,
    val sessionType: String,
    val isCompleted: Boolean
)

/** Kết quả một lần backup/restore để hiển thị thông báo cho người dùng. */
data class BackupSummary(
    val taskCount: Int,
    val sessionCount: Int
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
    updatedAt = updatedAt,
    estimatedPomodoros = estimatedPomodoros,
    completedPomodoros = completedPomodoros,
    totalFocusTimeMinutes = totalFocusTimeMinutes
)

/**
 * Extension function to convert ExportTask back to Task entity.
 *
 * @param preserveId `true` khi **restore backup** (giữ nguyên id để PomodoroSession vẫn
 *        trỏ đúng Task); `false` khi **import** task thường (để Room tự sinh id mới).
 */
fun ExportTask.toEntity(preserveId: Boolean = false) = Task(
    id = if (preserveId) id else 0, // Reset ID for new insertion
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
    updatedAt = updatedAt,
    estimatedPomodoros = estimatedPomodoros,
    completedPomodoros = completedPomodoros,
    totalFocusTimeMinutes = totalFocusTimeMinutes
)

/** Chuyển phiên Pomodoro thành DTO để ghi ra JSON (giữ nguyên id + taskId). */
fun PomodoroSession.toExportSession() = ExportPomodoroSession(
    id = id,
    taskId = taskId,
    startTime = startTime,
    endTime = endTime,
    durationInMinutes = durationInMinutes,
    sessionType = sessionType.name,
    isCompleted = isCompleted
)

/**
 * Chuyển DTO phiên Pomodoro thành entity.
 *
 * @return `null` nếu `sessionType` không hợp lệ — tầng validate đã chặn trước đó nhưng vẫn
 *         kiểm tra lại để không bao giờ ghi ra dữ liệu sai.
 */
fun ExportPomodoroSession.toEntity(): PomodoroSession? {
    val type = SessionType.entries.firstOrNull { it.name == sessionType.trim().uppercase(Locale.US) }
        ?: return null
    return PomodoroSession(
        id = id,
        taskId = taskId,
        startTime = startTime,
        endTime = endTime,
        durationInMinutes = durationInMinutes,
        sessionType = type,
        isCompleted = isCompleted
    )
}

/**
 * Model representing an entry in recent backups history.
 */
data class BackupHistoryItem(
    val fileName: String,
    val formattedDate: String,
    val sizeString: String,
    val timestamp: Long
)
