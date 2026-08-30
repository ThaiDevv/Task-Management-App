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
}
