package com.team.taskmanagementapp.ai

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiTaskAssistantTest {
    private val assistant = AiTaskAssistant()
    private val now = LocalDateTime.of(2026, 9, 22, 10, 0)

    @Test
    fun createCommandUsesSafeDefaultsForMissingFields() {
        val command = assistant.parseResponse(
            """{"action":"CREATE_TASK","title":"Write report"}""",
            now
        )

        assertEquals("CREATE_TASK", command.action)
        assertEquals("Write report", command.title)
        assertEquals("2026-09-23", command.dueDate)
        assertEquals("09:00", command.dueTime)
        assertEquals("MEDIUM", command.priority)
        assertEquals("NONE", command.recurrence)
        assertEquals(30, command.reminderMinutes)
    }

    @Test
    fun tomorrowCountQueryRemainsDistinctFromToday() {
        val command = assistant.parseResponse(
            """{"action":"LIST_TASKS","query":{"resultType":"COUNT","datePreset":"TOMORROW"}}""",
            now
        )

        assertEquals("LIST_TASKS", command.action)
        assertEquals("COUNT", command.query?.resultType)
        assertEquals("TOMORROW", command.query?.datePreset)
    }

    @Test
    fun updateCommandDoesNotInventUnrequestedFields() {
        val command = assistant.parseResponse(
            """{"action":"UPDATE_TASK","targetTaskId":42,"status":"IN_PROGRESS"}""",
            now
        )

        assertEquals(42, command.targetTaskId)
        assertEquals("IN_PROGRESS", command.status)
        assertNull(command.dueDate)
        assertNull(command.dueTime)
        assertNull(command.priority)
    }
}
