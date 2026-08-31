package com.team.taskmanagementapp.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun cleanUp() {
        AlarmScheduler.cancelAlarm(context, TEST_TASK_ID)
    }

    @Test
    fun unavailableExactAlarmAccessUsesWorkManagerFallback() {
        assumeTrue(NotificationHelper.areNotificationsEnabled(context))
        assumeFalse(AlarmScheduler.canScheduleExactAlarms(context))

        val result = AlarmScheduler.scheduleReminderAt(
            context = context,
            taskId = TEST_TASK_ID,
            triggerAtMillis = System.currentTimeMillis() + 60_000L
        )

        assertEquals(AlarmScheduler.ScheduleResult.FALLBACK_SCHEDULED, result)
    }

    @Test
    fun pastReminderIsSkippedWithoutSchedulingWork() {
        val result = AlarmScheduler.scheduleReminderAt(
            context = context,
            taskId = TEST_TASK_ID,
            triggerAtMillis = System.currentTimeMillis() - 1_000L
        )

        assertEquals(AlarmScheduler.ScheduleResult.SKIPPED, result)
    }

    companion object {
        private const val TEST_TASK_ID = 2_000_045
    }
}
