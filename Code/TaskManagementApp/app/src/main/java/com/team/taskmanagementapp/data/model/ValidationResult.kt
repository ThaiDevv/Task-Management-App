package com.team.taskmanagementapp.data.model

/**
 * Data class để hold kết quả validation của JSON import file
 *
 * @property isValid Boolean - True nếu file hợp lệ, False nếu có bất kỳ lỗi nào
 * @property errorMessages List<String> - Danh sách chi tiết các lỗi (nếu có)
 *
 * Error message format:
 * - JSON parsing: "JSON: Invalid syntax: ${error detail}"
 * - Version: "Version: Missing or invalid"
 * - Tasks array: "Tasks: Array not found or empty"
 * - Required fields: "Task ${index}: Missing '${fieldName}'"
 * - Type validation: "Task ${index}: Invalid '${fieldName}' type (expected ...)"
 * - Value validation: "Task ${index}: Invalid ${fieldName} value (expected ...)"
 *
 * Example - Valid file:
 * ValidationResult(isValid = true, errorMessages = [])
 *
 * Example - Multiple errors:
 * ValidationResult(
 *     isValid = false,
 *     errorMessages = listOf(
 *         "Task 0: Missing 'title'",
 *         "Task 0: Missing 'dueDate'",
 *         "Task 1: Missing 'status'"
 *     )
 * )
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessages: List<String> = emptyList()
)
