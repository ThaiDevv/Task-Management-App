package com.team.taskmanagementapp.data.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonValidatorTest {

    @Test
    fun testValidJson_returnsSuccess() {
        val validJson = """
            {
                "version": "1.0",
                "exportedAt": "2026-08-31T10:30:00Z",
                "exportedBy": "TaskManagementApp",
                "tasks": [
                    {
                        "id": 1,
                        "title": "Buy groceries",
                        "description": "Buy milk, eggs, and bread",
                        "dueDate": 1725057600000,
                        "status": "TODO",
                        "priority": "MEDIUM",
                        "recurrenceType": "NONE"
                    },
                    {
                        "id": 2,
                        "title": "Finish project proposal",
                        "description": "Write final draft of proposal",
                        "dueDate": 1725144000000,
                        "status": "IN_PROGRESS",
                        "priority": "HIGH",
                        "recurrenceType": "NONE"
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(validJson)
        assertTrue(result.isValid)
        assertTrue(result.errorMessages.isEmpty())
    }

    @Test
    fun testInvalidJsonSyntax_returnsError() {
        val invalidJson = """
            {
                "version": "1.0",
                "tasks": [
                    {
                        "id": 1,
                        "title": "Buy groceries"
                    }
                
            }
        """.trimIndent() // Missing closing bracket for array/object

        val result = JsonValidator.validate(invalidJson)
        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("JSON: Invalid syntax") })
    }

    @Test
    fun testMissingVersion_returnsError() {
        val missingVersionJson = """
            {
                "tasks": [
                    {
                        "id": 1,
                        "title": "Buy groceries",
                        "description": "Buy milk",
                        "dueDate": 1725057600000,
                        "status": "TODO",
                        "priority": "MEDIUM"
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(missingVersionJson)
        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Version: Missing or invalid") })
    }

    @Test
    fun testMissingTasks_returnsError() {
        val missingTasksJson = """
            {
                "version": "1.0"
            }
        """.trimIndent()

        val result = JsonValidator.validate(missingTasksJson)
        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Tasks: Array not found or empty") })
    }

    @Test
    fun testEmptyTasksArray_returnsError() {
        val emptyTasksJson = """
            {
                "version": "1.0",
                "tasks": []
            }
        """.trimIndent()

        val result = JsonValidator.validate(emptyTasksJson)
        assertFalse(result.isValid)
        assertTrue(result.errorMessages.any { it.contains("Tasks: Array not found or empty") })
    }

    @Test
    fun testMissingRequiredFieldsInTask_returnsSpecificErrors() {
        val invalidTaskJson = """
            {
                "version": "1.0",
                "tasks": [
                    {
                        "id": 1,
                        "description": "Missing title, status, priority, dueDate"
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(invalidTaskJson)
        assertFalse(result.isValid)
        val errors = result.errorMessages
        assertTrue(errors.contains("Task 0: Missing 'title'"))
        assertTrue(errors.contains("Task 0: Missing 'status'"))
        assertTrue(errors.contains("Task 0: Missing 'priority'"))
        assertTrue(errors.contains("Task 0: Missing 'dueDate'"))
    }

    @Test
    fun testInvalidFieldTypes_returnsSpecificErrors() {
        val invalidTypesJson = """
            {
                "version": "1.0",
                "tasks": [
                    {
                        "id": 1,
                        "title": 123,
                        "description": true,
                        "dueDate": "not_a_number_or_iso8601",
                        "status": 1,
                        "priority": {}
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(invalidTypesJson)
        assertFalse(result.isValid)
        val errors = result.errorMessages
        assertTrue(errors.contains("Task 0: Invalid 'title' type (expected String)"))
        assertTrue(errors.contains("Task 0: Invalid 'status' type (expected String)"))
        assertTrue(errors.contains("Task 0: Invalid 'priority' type (expected String)"))
        assertTrue(errors.contains("Task 0: Invalid 'dueDate' type (expected Long or ISO 8601 String)"))
    }

    @Test
    fun testInvalidEnumValues_returnsSpecificErrors() {
        val invalidEnumsJson = """
            {
                "version": "1.0",
                "tasks": [
                    {
                        "id": 1,
                        "title": "Clean room",
                        "description": "",
                        "dueDate": 1725057600000,
                        "status": "COMPLETED",
                        "priority": "URGENT_HELL",
                        "recurrenceType": "EVERY_HOUR"
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(invalidEnumsJson)
        assertFalse(result.isValid)
        val errors = result.errorMessages
        assertTrue(errors.any { it.contains("Task 0: Invalid status value") })
        assertTrue(errors.any { it.contains("Task 0: Invalid priority value") })
        assertTrue(errors.any { it.contains("Task 0: Invalid recurrenceType value") })
    }

    @Test
    fun testAtomicRejection_withMultipleTasks() {
        // One valid task, one invalid task
        val mixedTasksJson = """
            {
                "version": "1.0",
                "tasks": [
                    {
                        "id": 1,
                        "title": "Valid task",
                        "description": "Good",
                        "dueDate": 1725057600000,
                        "status": "TODO",
                        "priority": "LOW",
                        "recurrenceType": "NONE"
                    },
                    {
                        "id": 2,
                        "title": "",
                        "description": "Bad - empty title, invalid status",
                        "dueDate": 1725057600000,
                        "status": "INVALID",
                        "priority": "MEDIUM"
                    }
                ]
            }
        """.trimIndent()

        val result = JsonValidator.validate(mixedTasksJson)
        // Atomic rejection check: entire file must be rejected if even 1 task is invalid
        assertFalse(result.isValid)
        assertEquals(2, result.errorMessages.size)
        assertTrue(result.errorMessages.contains("Task 1: Invalid title value (cannot be empty)"))
        assertTrue(result.errorMessages.any { it.contains("Task 1: Invalid status value") })
    }
}
