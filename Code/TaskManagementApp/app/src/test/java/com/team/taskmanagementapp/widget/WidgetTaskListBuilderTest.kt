package com.team.taskmanagementapp.widget

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho logic dựng danh sách widget (tách thuần, không cần Android):
 * sắp xếp chưa-xong trước / có-giờ trước / đã-xong xuống cuối, và số liệu tiến độ.
 */
class WidgetTaskListBuilderTest {

    private fun makeTask(
        id: Int,
        title: String,
        dueTime: Long = 0L,
        isCompleted: Boolean = false,
        priority: Priority = Priority.MEDIUM
    ) = Task(
        id = id,
        title = title,
        description = "",
        dueDate = 1757800000000L,
        dueTime = dueTime,
        priority = priority,
        status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
        isCompleted = isCompleted
    )

    @Test
    fun `empty list produces zero counts`() {
        val result = WidgetTaskListBuilder.build(emptyList())

        assertEquals(0, result.items.size)
        assertEquals(0, result.totalCount)
        assertEquals(0, result.completedCount)
    }

    @Test
    fun `pending tasks come before completed ones`() {
        val result = WidgetTaskListBuilder.build(
            listOf(
                makeTask(1, "Đã xong", isCompleted = true),
                makeTask(2, "Chưa xong"),
            )
        )

        assertEquals(listOf("Chưa xong", "Đã xong"), result.items.map { it.title })
    }

    @Test
    fun `tasks with time come before all-day tasks within same completion state`() {
        val result = WidgetTaskListBuilder.build(
            listOf(
                makeTask(1, "Cả ngày"),
                makeTask(2, "Có giờ", dueTime = timeOfDay(9, 0)),
                makeTask(3, "Cả ngày khác"),
            )
        )

        assertEquals("Có giờ", result.items.first().title)
        assertTrue(result.items.drop(1).all { it.timeText == null })
    }

    @Test
    fun `earlier time sorts first`() {
        val result = WidgetTaskListBuilder.build(
            listOf(
                makeTask(1, "Buổi chiều", dueTime = timeOfDay(14, 0)),
                makeTask(2, "Buổi sáng", dueTime = timeOfDay(8, 0)),
            )
        )

        assertEquals(listOf("Buổi sáng", "Buổi chiều"), result.items.map { it.title })
    }

    @Test
    fun `completed tasks keep their times sorted below pending ones`() {
        val result = WidgetTaskListBuilder.build(
            listOf(
                makeTask(1, "Xong có giờ sớm", dueTime = timeOfDay(7, 0), isCompleted = true),
                makeTask(2, "Chưa xong cả ngày"),
            )
        )

        assertEquals(listOf("Chưa xong cả ngày", "Xong có giờ sớm"), result.items.map { it.title })
    }

    @Test
    fun `counts reflect completion state`() {
        val result = WidgetTaskListBuilder.build(
            listOf(
                makeTask(1, "A"),
                makeTask(2, "B", isCompleted = true),
                makeTask(3, "C", isCompleted = true),
            )
        )

        assertEquals(3, result.totalCount)
        assertEquals(2, result.completedCount)
        assertEquals(1, result.items.count { !it.isCompleted })
    }

    @Test
    fun `time text is formatted or null for all-day`() {
        val withTime = WidgetTaskListBuilder.build(
            listOf(makeTask(1, "Có giờ", dueTime = timeOfDay(9, 30)))
        )
        val allDay = WidgetTaskListBuilder.build(listOf(makeTask(2, "Cả ngày")))

        assertEquals("09:30", withTime.items.first().timeText)
        assertEquals(null, allDay.items.first().timeText)
    }

    /** App lưu dueTime là timestamp của một ngày kèm giờ địa phương — dựng đúng cách đó. */
    private fun timeOfDay(hour: Int, minute: Int): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
