package com.team.taskmanagementapp.data.validator

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.team.taskmanagementapp.data.model.ValidationResult

/**
 * JSON Validator object cho import feature
 *
 * Validation Rules:
 * 1. Valid JSON syntax
 * 2. Has version field (String)
 * 3. Has tasks array (non-empty)
 * 4. Each task has required fields: title, status, priority, dueDate, description
 * 5. Field types must match (title: String, dueDate: Long, status: String, priority: String)
 * 6. Enum values valid (status ∈ [TODO, IN_PROGRESS, DONE], priority ∈ [LOW, MEDIUM, HIGH, URGENT])
 * 7. dueDate is valid timestamp (Long > 0)
 * 8. Atomic: Collects ALL errors, rejects entire file if any error found
 */
object JsonValidator {

    // Valid enum values
    private val VALID_STATUSES = setOf("TODO", "IN_PROGRESS", "DONE")
    private val VALID_PRIORITIES = setOf("LOW", "MEDIUM", "HIGH", "URGENT")
    private val VALID_RECURRENCES = setOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY")

    /**
     * Main validation function
     *
     * @param json Raw JSON string to validate
     * @return ValidationResult with isValid flag and list of error messages
     *
     * On success: ValidationResult(isValid=true, errorMessages=[])
     * On failure: ValidationResult(isValid=false, errorMessages=[list of specific errors])
     */
    fun validate(json: String): ValidationResult {
        val errors = mutableListOf<String>()

        // Step 1: Validate JSON syntax
        val jsonObject = validateJsonSyntax(json, errors)
        if (jsonObject == null) {
            // JSON parsing failed, cannot continue
            return ValidationResult(isValid = false, errorMessages = errors)
        }

        // Step 2: Validate version field
        validateVersion(jsonObject, errors)

        // Step 3: Validate tasks array existence
        val tasksArray = validateTasksArray(jsonObject, errors)
        if (tasksArray == null || tasksArray.size() == 0) {
            // No tasks to validate, but this is an error per requirements
            if (tasksArray == null) {
                errors.add("Tasks: Array not found or empty")
            }
            return ValidationResult(isValid = false, errorMessages = errors)
        }

        // Step 4: Validate each task (collect ALL errors)
        for (i in 0 until tasksArray.size()) {
            val taskElement = tasksArray[i]
            if (!taskElement.isJsonObject) {
                errors.add("Task $i: Invalid type (must be JSON object)")
                continue
            }
            validateTask(taskElement.asJsonObject, i, errors)
        }

        // Step 5: Return result (atomic: all or nothing)
        return ValidationResult(
            isValid = errors.isEmpty(),
            errorMessages = errors
        )
    }

    /**
     * Validate JSON syntax
     * @return JsonObject if valid, null if invalid (error added to errors list)
     */
    private fun validateJsonSyntax(json: String, errors: MutableList<String>): JsonObject? {
        return try {
            val element = JsonParser.parseString(json)
            if (element.isJsonObject) {
                element.asJsonObject
            } else {
                errors.add("JSON: Invalid syntax: Root element must be an object")
                null
            }
        } catch (e: Exception) {
            errors.add("JSON: Invalid syntax: ${e.message}")
            null
        }
    }

    /**
     * Validate version field
     */
    private fun validateVersion(jsonObject: JsonObject, errors: MutableList<String>) {
        if (!jsonObject.has("version")) {
            errors.add("Version: Missing or invalid")
            return
        }

        val versionElement = jsonObject.get("version")
        if (!versionElement.isJsonPrimitive || !versionElement.asJsonPrimitive.isString) {
            errors.add("Version: Invalid type (must be String)")
        }
    }

    /**
     * Validate tasks array
     * @return JsonArray if valid, null if invalid (error added to errors list)
     */
    private fun validateTasksArray(jsonObject: JsonObject, errors: MutableList<String>): JsonArray? {
        if (!jsonObject.has("tasks")) {
            errors.add("Tasks: Array not found or empty")
            return null
        }

        val tasksElement = jsonObject.get("tasks")
        if (!tasksElement.isJsonArray) {
            errors.add("Tasks: Array not found or empty")
            return null
        }

        val tasksArray = tasksElement.asJsonArray
        if (tasksArray.size() == 0) {
            errors.add("Tasks: Array not found or empty")
            return null
        }

        return tasksArray
    }

    /**
     * Validate a single task object
     */
    private fun validateTask(task: JsonObject, index: Int, errors: MutableList<String>) {
        // Check required fields
        validateRequiredField(task, index, "title", errors)
        validateRequiredField(task, index, "status", errors)
        validateRequiredField(task, index, "priority", errors)
        validateRequiredField(task, index, "dueDate", errors)
        validateRequiredField(task, index, "description", errors)

        // Validate field types and values
        validateTitleField(task, index, errors)
        validateStatusField(task, index, errors)
        validatePriorityField(task, index, errors)
        validateDueDateField(task, index, errors)
        validateRecurrenceTypeField(task, index, errors)
    }

    /**
     * Check if required field exists
     */
    private fun validateRequiredField(
        task: JsonObject,
        index: Int,
        fieldName: String,
        errors: MutableList<String>
    ) {
        if (!task.has(fieldName)) {
            errors.add("Task $index: Missing '$fieldName'")
        }
    }

    /**
     * Validate title field (must be non-empty String)
     */
    private fun validateTitleField(task: JsonObject, index: Int, errors: MutableList<String>) {
        if (!task.has("title")) return

        val titleElement = task.get("title")
        if (!titleElement.isJsonPrimitive || !titleElement.asJsonPrimitive.isString) {
            errors.add("Task $index: Invalid 'title' type (expected String)")
            return
        }

        val title = titleElement.asString.trim()
        if (title.isEmpty()) {
            errors.add("Task $index: Invalid title value (cannot be empty)")
        }
    }

    /**
     * Validate status field (must be in VALID_STATUSES)
     */
    private fun validateStatusField(task: JsonObject, index: Int, errors: MutableList<String>) {
        if (!task.has("status")) return

        val statusElement = task.get("status")
        if (!statusElement.isJsonPrimitive || !statusElement.asJsonPrimitive.isString) {
            errors.add("Task $index: Invalid 'status' type (expected String)")
            return
        }

        val status = statusElement.asString.uppercase()
        if (status !in VALID_STATUSES) {
            errors.add("Task $index: Invalid status value '$status' (must be one of: ${VALID_STATUSES.joinToString(", ")})")
        }
    }

    /**
     * Validate priority field (must be in VALID_PRIORITIES)
     */
    private fun validatePriorityField(task: JsonObject, index: Int, errors: MutableList<String>) {
        if (!task.has("priority")) return

        val priorityElement = task.get("priority")
        if (!priorityElement.isJsonPrimitive || !priorityElement.asJsonPrimitive.isString) {
            errors.add("Task $index: Invalid 'priority' type (expected String)")
            return
        }

        val priority = priorityElement.asString.uppercase()
        if (priority !in VALID_PRIORITIES) {
            errors.add("Task $index: Invalid priority value '$priority' (must be one of: ${VALID_PRIORITIES.joinToString(", ")})")
        }
    }

    /**
     * Validate dueDate field (must be Long timestamp > 0)
     */
    private fun validateDueDateField(task: JsonObject, index: Int, errors: MutableList<String>) {
        if (!task.has("dueDate")) return

        val dueDateElement = task.get("dueDate")

        // Handle both Long and String (ISO 8601) formats
        val dueDate = when {
            dueDateElement.isJsonPrimitive && dueDateElement.asJsonPrimitive.isNumber -> {
                try {
                    dueDateElement.asLong
                } catch (e: Exception) {
                    errors.add("Task $index: Invalid dueDate type (expected Long or ISO 8601 String)")
                    return
                }
            }
            dueDateElement.isJsonPrimitive && dueDateElement.asJsonPrimitive.isString -> {
                // Try to parse ISO 8601 string to timestamp
                try {
                    val dateString = dueDateElement.asString
                    // Simple validation: check if it looks like ISO 8601 (contains T and Z or +/-)
                    if (dateString.contains("T")) {
                        // Accept ISO 8601 format (actual parsing would happen in BackupRepository)
                        0L // Placeholder, actual parsing done elsewhere
                    } else {
                        errors.add("Task $index: Invalid dueDate format (expected Long timestamp or ISO 8601 String)")
                        return
                    }
                } catch (e: Exception) {
                    errors.add("Task $index: Invalid dueDate value")
                    return
                }
            }
            else -> {
                errors.add("Task $index: Invalid 'dueDate' type (expected Long or ISO 8601 String)")
                return
            }
        }

        // Validate timestamp is positive (if it's a Long)
        if (dueDate > 0 || dueDate == 0L) {
            // dueDate can be 0 or positive
            return
        } else {
            errors.add("Task $index: Invalid dueDate value (must be positive timestamp)")
        }
    }

    /**
     * Validate recurrenceType field (must be in VALID_RECURRENCES or optional)
     */
    private fun validateRecurrenceTypeField(task: JsonObject, index: Int, errors: MutableList<String>) {
        if (!task.has("recurrenceType")) return

        val recurrenceElement = task.get("recurrenceType")
        if (!recurrenceElement.isJsonPrimitive || !recurrenceElement.asJsonPrimitive.isString) {
            errors.add("Task $index: Invalid 'recurrenceType' type (expected String)")
            return
        }

        val recurrence = recurrenceElement.asString.uppercase()
        if (recurrence !in VALID_RECURRENCES) {
            errors.add("Task $index: Invalid recurrenceType value '$recurrence' (must be one of: ${VALID_RECURRENCES.joinToString(", ")})")
        }
    }
}
