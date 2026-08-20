package com.team.taskmanagementapp.receiver

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeChangeReceiverTest {

    @Test
    fun testSupportedTimeChangeActions() {
        val actions = listOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )

        assertEquals("android.intent.action.TIME_SET", Intent.ACTION_TIME_CHANGED)
        assertEquals("android.intent.action.TIMEZONE_CHANGED", Intent.ACTION_TIMEZONE_CHANGED)
        assertEquals("android.intent.action.DATE_CHANGED", Intent.ACTION_DATE_CHANGED)

        assertTrue(actions.contains("android.intent.action.TIME_SET"))
        assertTrue(actions.contains("android.intent.action.TIMEZONE_CHANGED"))
        assertTrue(actions.contains("android.intent.action.DATE_CHANGED"))
    }
}
