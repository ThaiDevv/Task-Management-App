package com.team.taskmanagementapp.data.validator

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.enums.TaskStatus

/**
 * Kết quả kiểm tra một file backup.
 *
 * @param isValid `true` khi file an toàn để restore
 * @param errorMessages toàn bộ lỗi tìm được (atomic: chỉ cần 1 lỗi là không restore)
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessages: List<String> = emptyList()
)

/**
 * Validator cho file backup JSON (Task 16).
 *
 * Kiểm tra trên **raw JSON** (không parse thẳng vào entity) để:
 * - bắt được cả lỗi kiểu dữ liệu lẫn lỗi ngữ nghĩa một cách tường minh;
 * - không phụ thuộc hành vi gán `null` cho enum lạ của Gson.
 *
 * Các nhóm kiểm tra:
 * 1. JSON hợp lệ + là object
 * 2. `version` là số và **không vượt quá** phiên bản app hỗ trợ
 * 3. `tasks`: id > 0, title không rỗng, dueDate > 0, enum hợp lệ, counters ≥ 0, id không trùng
 * 4. `pomodoroSessions` (tuỳ chọn): id > 0, times hợp lệ, duration > 0, sessionType hợp lệ,
 *    id không trùng và **`taskId` phải tồn tại trong danh sách tasks** (toàn vẹn khoá ngoại)
 *
 * Hàm thuần, không phụ thuộc Android → test được trên JVM.
 */
object BackupValidator {

    private val VALID_STATUSES: Set<String> =
        TaskStatus.entries.map { it.name }.toSet() + "DONE" // alias cũ của COMPLETED

    private val VALID_PRIORITIES: Set<String> = Priority.entries.map { it.name }.toSet()

    private val VALID_RECURRENCES: Set<String> = RecurrenceType.entries.map { it.name }.toSet()

    private val VALID_SESSION_TYPES: Set<String> = SessionType.entries.map { it.name }.toSet()

    /** Số phút tối đa của một phiên (1 ngày) — chặn dữ liệu vô lý. */
    private const val MAX_SESSION_MINUTES = 24 * 60

    /**
     * Validate nội dung file backup.
     *
     * @param json nội dung file
     * @param supportedVersion phiên bản format cao nhất mà app hiện tại đọc được
     */
    fun validate(json: String, supportedVersion: Int): BackupValidationResult {
        val errors = mutableListOf<String>()

        val root = parseRoot(json, errors) ?: return BackupValidationResult(false, errors)

        validateVersion(root, supportedVersion, errors)

        val taskIds = validateTasks(root, errors)

        validatePomodoroSessions(root, taskIds, errors)

        return BackupValidationResult(isValid = errors.isEmpty(), errorMessages = errors)
    }

    private fun parseRoot(json: String, errors: MutableList<String>): JsonObject? {
        if (json.isBlank()) {
            errors.add("Backup: File is empty")
            return null
        }
        return try {
            val element = JsonParser.parseString(json)
            if (element.isJsonObject) {
                element.asJsonObject
            } else {
                errors.add("Backup: Root element must be a JSON object")
                null
            }
        } catch (e: Exception) {
            errors.add("Backup: Invalid JSON syntax (${e.message})")
            null
        }
    }

    private fun validateVersion(
        root: JsonObject,
        supportedVersion: Int,
        errors: MutableList<String>
    ) {
        val versionElement = root.get("version")
        if (versionElement == null || !versionElement.isJsonPrimitive || !versionElement.asJsonPrimitive.isNumber) {
            errors.add("Version: Missing or not a number")
            return
        }

        val version = versionElement.asInt
        when {
            version <= 0 -> errors.add("Version: Must be a positive number (found $version)")
            version > supportedVersion ->
                errors.add("Version: Unsupported backup version $version (this app supports up to $supportedVersion)")
        }
    }

    /**
     * Validate mảng `tasks`.
     * @return tập id hợp lệ, dùng để kiểm tra khoá ngoại của `pomodoroSessions`
     */
    private fun validateTasks(root: JsonObject, errors: MutableList<String>): Set<Long> {
        val tasksElement = root.get("tasks")
        if (tasksElement == null || !tasksElement.isJsonArray) {
            errors.add("Tasks: Missing 'tasks' array")
            return emptySet()
        }

        val tasksArray = tasksElement.asJsonArray
        val ids = mutableSetOf<Long>()

        for (index in 0 until tasksArray.size()) {
            val element = tasksArray[index]
            if (!element.isJsonObject) {
                errors.add("Task $index: Must be a JSON object")
                continue
            }
            val task = element.asJsonObject
            val id = task.requirePositiveLong("id", "Task $index", errors)
            if (id != null && !ids.add(id)) {
                errors.add("Task $index: Duplicate id $id")
            }

            if (!task.hasNonNullString("title") || task.get("title").asString.isBlank()) {
                errors.add("Task $index: Missing or empty 'title'")
            }

            task.requirePositiveLong("dueDate", "Task $index", errors)

            task.requireEnum("priority", VALID_PRIORITIES, "Task $index", errors)
            task.requireEnum("status", VALID_STATUSES, "Task $index", errors)

            if (task.has("recurrenceType")) {
                task.requireEnum("recurrenceType", VALID_RECURRENCES, "Task $index", errors)
            }
            task.requireNonNegative("estimatedPomodoros", "Task $index", errors)
            task.requireNonNegative("completedPomodoros", "Task $index", errors)
            task.requireNonNegative("totalFocusTimeMinutes", "Task $index", errors)
        }

        val declaredCount = root.get("taskCount")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt
        if (declaredCount != null && declaredCount != tasksArray.size()) {
            errors.add("Tasks: Declared taskCount $declaredCount does not match ${tasksArray.size()} entries")
        }

        return ids
    }

    private fun validatePomodoroSessions(
        root: JsonObject,
        taskIds: Set<Long>,
        errors: MutableList<String>
    ) {
        if (!root.has("pomodoroSessions") || root.get("pomodoroSessions").isJsonNull) return

        val sessionsElement = root.get("pomodoroSessions")
        if (!sessionsElement.isJsonArray) {
            errors.add("PomodoroSessions: Must be an array")
            return
        }

        val sessionsArray: JsonArray = sessionsElement.asJsonArray
        val ids = mutableSetOf<Long>()

        for (index in 0 until sessionsArray.size()) {
            val element = sessionsArray[index]
            if (!element.isJsonObject) {
                errors.add("PomodoroSession $index: Must be a JSON object")
                continue
            }
            val session = element.asJsonObject
            val label = "PomodoroSession $index"

            val id = session.requirePositiveLong("id", label, errors)
            if (id != null && !ids.add(id)) {
                errors.add("$label: Duplicate id $id")
            }

            // ── Toàn vẹn khoá ngoại: phiên phải trỏ tới một Task có trong file ──
            val taskId = session.requirePositiveLong("taskId", label, errors)
            if (taskId != null && !taskIds.contains(taskId)) {
                errors.add("$label: taskId $taskId does not exist in this backup")
            }

            val startTime = session.requirePositiveLong("startTime", label, errors)
            val endTime = session.requirePositiveLong("endTime", label, errors)
            if (startTime != null && endTime != null && endTime < startTime) {
                errors.add("$label: endTime is before startTime")
            }

            val duration = session.get("durationInMinutes")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.asInt
            if (duration == null) {
                errors.add("$label: Missing or invalid 'durationInMinutes'")
            } else if (duration <= 0 || duration > MAX_SESSION_MINUTES) {
                errors.add("$label: durationInMinutes must be between 1 and $MAX_SESSION_MINUTES")
            }

            session.requireEnum("sessionType", VALID_SESSION_TYPES, label, errors)

            val isCompleted = session.get("isCompleted")
            if (isCompleted == null || !isCompleted.isJsonPrimitive || !isCompleted.asJsonPrimitive.isBoolean) {
                errors.add("$label: Missing or invalid 'isCompleted'")
            }
        }

        val declaredCount = root.get("pomodoroSessionCount")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt
        if (declaredCount != null && declaredCount != sessionsArray.size()) {
            errors.add(
                "PomodoroSessions: Declared pomodoroSessionCount $declaredCount " +
                    "does not match ${sessionsArray.size()} entries"
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper kiểm tra từng field
    // ══════════════════════════════════════════════════════════════════════════

    private fun JsonObject.hasNonNullString(name: String): Boolean {
        val element = get(name) ?: return false
        return element.isJsonPrimitive && element.asJsonPrimitive.isString
    }

    private fun JsonObject.requirePositiveLong(
        name: String,
        label: String,
        errors: MutableList<String>
    ): Long? {
        val element = get(name)
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) {
            errors.add("$label: Missing or invalid '$name'")
            return null
        }
        val value = element.asLong
        if (value <= 0L) {
            errors.add("$label: '$name' must be greater than 0 (found $value)")
            return null
        }
        return value
    }

    private fun JsonObject.requireNonNegative(
        name: String,
        label: String,
        errors: MutableList<String>
    ) {
        if (!has(name)) return // field mới thêm ở v2 nên cho phép thiếu (mặc định 0)
        val element = get(name)
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) {
            errors.add("$label: Invalid '$name'")
            return
        }
        if (element.asInt < 0) {
            errors.add("$label: '$name' must not be negative")
        }
    }

    private fun JsonObject.requireEnum(
        name: String,
        allowed: Set<String>,
        label: String,
        errors: MutableList<String>
    ) {
        val element = get(name)
        if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isString) {
            errors.add("$label: Missing or invalid '$name'")
            return
        }
        val value = element.asString.trim().uppercase()
        if (value !in allowed) {
            errors.add("$label: Unknown '$name' value '$value'")
        }
    }
}
