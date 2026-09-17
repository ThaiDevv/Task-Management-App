package com.team.taskmanagementapp.util

import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class RecurrenceHelperTest {

    private fun millisFromDate(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testDailyRecurrence() {
        val startMillis = millisFromDate(2026, 8, 26) // Aug 26, 2026
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.DAILY, 1)

        val expectedMillis = millisFromDate(2026, 8, 27) // Aug 27, 2026
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testWeeklyRecurrence() {
        val startMillis = millisFromDate(2026, 8, 26) // Aug 26, 2026
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.WEEKLY, 1)

        val expectedMillis = millisFromDate(2026, 9, 2) // Sep 2, 2026
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testMonthlyEdgeCaseJan31() {
        // Jan 31, 2026 -> +1 month should land on Feb 28, 2026 (non-leap year)
        val startMillis = millisFromDate(2026, 1, 31)
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.MONTHLY, 1)

        val expectedMillis = millisFromDate(2026, 2, 28)
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testMonthlyEdgeCaseJan31LeapYear() {
        // Jan 31, 2024 -> +1 month should land on Feb 29, 2024 (leap year)
        val startMillis = millisFromDate(2024, 1, 31)
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.MONTHLY, 1)

        val expectedMillis = millisFromDate(2024, 2, 29)
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testMonthlyAug31() {
        // Aug 31, 2026 -> +1 month should land on Sep 30, 2026
        val startMillis = millisFromDate(2026, 8, 31)
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.MONTHLY, 1)

        val expectedMillis = millisFromDate(2026, 9, 30)
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testYearlyRecurrence() {
        val startMillis = millisFromDate(2026, 8, 26)
        val nextMillis = RecurrenceHelper.calculateNextDueDate(startMillis, RecurrenceType.YEARLY, 1)

        val expectedMillis = millisFromDate(2027, 8, 26)
        assertEquals(expectedMillis, nextMillis)
    }

    @Test
    fun testCalculateEndDateFromOccurrences() {
        val startMillis = millisFromDate(2026, 8, 26)
        // 3 days
        val dailyEnd = RecurrenceHelper.calculateEndDateFromOccurrences(startMillis, RecurrenceType.DAILY, 3)
        assertEquals(millisFromDate(2026, 8, 29), dailyEnd)

        // 2 weeks
        val weeklyEnd = RecurrenceHelper.calculateEndDateFromOccurrences(startMillis, RecurrenceType.WEEKLY, 2)
        assertEquals(millisFromDate(2026, 9, 9), weeklyEnd)

        // 2 years
        val yearlyEnd = RecurrenceHelper.calculateEndDateFromOccurrences(startMillis, RecurrenceType.YEARLY, 2)
        assertEquals(millisFromDate(2028, 8, 26), yearlyEnd)
    }

    @Test
    fun testIsRecurrenceEnded() {
        val startMillis = millisFromDate(2026, 8, 26)
        val task = com.team.taskmanagementapp.data.local.entity.Task(
            id = 1,
            title = "Test",
            description = "",
            dueDate = startMillis,
            dueTime = 0L,
            priority = com.team.taskmanagementapp.data.model.enums.Priority.MEDIUM,
            isRecurring = true,
            recurrenceType = RecurrenceType.DAILY,
            repeatLimitCount = 3,
            currentOccurrence = 3
        )
        val nextMillis = millisFromDate(2026, 8, 27)
        assertEquals(true, RecurrenceHelper.isRecurrenceEnded(task, nextMillis))

        val taskNotEnded = task.copy(currentOccurrence = 2)
        assertEquals(false, RecurrenceHelper.isRecurrenceEnded(taskNotEnded, nextMillis))

        val taskWithEndDate = task.copy(
            repeatLimitCount = 0,
            repeatEndDate = millisFromDate(2026, 8, 28)
        )
        assertEquals(false, RecurrenceHelper.isRecurrenceEnded(taskWithEndDate, millisFromDate(2026, 8, 27)))
        assertEquals(true, RecurrenceHelper.isRecurrenceEnded(taskWithEndDate, millisFromDate(2026, 8, 29)))
    }
}
