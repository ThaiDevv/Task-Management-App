package com.team.taskmanagementapp.ui.viewmodel

import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.model.stats.StatsTimeFilter
import com.team.taskmanagementapp.ui.FakeTaskRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsViewModelTest {

    private fun createTask(
        id: Int,
        title: String,
        isCompleted: Boolean,
        priority: Priority = Priority.MEDIUM,
        dueDate: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Task = Task(
        id = id,
        title = title,
        description = "Description $id",
        dueDate = dueDate,
        dueTime = 0L,
        priority = priority,
        status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
        isCompleted = isCompleted,
        isRecurring = false,
        recurrenceType = RecurrenceType.NONE,
        recurrenceInterval = 0,
        reminderMinutes = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = updatedAt
    )

    @Test
    fun `calculateStats with empty task list returns zero and no tasks label`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        val state = viewModel.calculateStats(emptyList(), StatsTimeFilter.ALL_TIME)

        assertEquals(0, state.completionRate)
        assertEquals("No tasks", state.completionRateLabel)
        assertEquals(0, state.completedCount)
        assertEquals("0h", state.deepWorkHours)
        assertEquals(0, state.priorityStats.totalCount)
    }

    @Test
    fun `calculateStats accurately computes 75 percent completion rate matching design`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        // 3 completed, 1 todo => 75%
        val tasks = listOf(
            createTask(1, "Task 1", true),
            createTask(2, "Task 2", true),
            createTask(3, "Task 3", true),
            createTask(4, "Task 4", false)
        )

        val state = viewModel.calculateStats(tasks, StatsTimeFilter.ALL_TIME)

        assertEquals(75, state.completionRate)
        assertEquals("On track", state.completionRateLabel)
        assertEquals(3, state.completedCount)
    }

    @Test
    fun `calculateStats accurately computes excellent and needs attention rates`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        // 9 of 10 completed => 90% (Excellent)
        val highTasks = (1..10).map { i -> createTask(i, "T$i", i <= 9) }
        val highState = viewModel.calculateStats(highTasks, StatsTimeFilter.ALL_TIME)
        assertEquals(90, highState.completionRate)
        assertEquals("Excellent", highState.completionRateLabel)

        // 1 of 5 completed => 20% (Needs attention)
        val lowTasks = (1..5).map { i -> createTask(i, "T$i", i == 1) }
        val lowState = viewModel.calculateStats(lowTasks, StatsTimeFilter.ALL_TIME)
        assertEquals(20, lowState.completionRate)
        assertEquals("Needs attention", lowState.completionRateLabel)
    }

    @Test
    fun `calculateStats estimates 18h deep work for 42 completed tasks matching design`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        // 42 completed tasks
        val tasks = (1..42).map { i -> createTask(i, "Completed Task $i", true) }

        val state = viewModel.calculateStats(tasks, StatsTimeFilter.ALL_TIME)

        assertEquals(42, state.completedCount)
        assertEquals("18h", state.deepWorkHours)
        assertEquals("Focused time", state.deepWorkSubtitle)
    }

    @Test
    fun `calculateStats computes correct priority distribution and percentages`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        // 12 High, 20 Medium, 8 Low (Total 40)
        val tasks = mutableListOf<Task>()
        var id = 1
        repeat(12) { tasks.add(createTask(id++, "High Task", true, Priority.HIGH)) }
        repeat(20) { tasks.add(createTask(id++, "Med Task", true, Priority.MEDIUM)) }
        repeat(8) { tasks.add(createTask(id++, "Low Task", false, Priority.LOW)) }

        val state = viewModel.calculateStats(tasks, StatsTimeFilter.ALL_TIME)

        assertEquals(12, state.priorityStats.highCount)
        assertEquals(20, state.priorityStats.mediumCount)
        assertEquals(8, state.priorityStats.lowCount)
        assertEquals(40, state.priorityStats.totalCount)

        assertEquals(0.30f, state.priorityStats.highPercent, 0.01f)
        assertEquals(0.50f, state.priorityStats.mediumPercent, 0.01f)
        assertEquals(0.20f, state.priorityStats.lowPercent, 0.01f)
    }

    @Test
    fun `calculateStats weekly productivity generates 7 days with Mon to Sun labels`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        val state = viewModel.calculateStats(emptyList(), StatsTimeFilter.THIS_WEEK)
        val weekly = state.weeklyProductivity

        assertEquals(7, weekly.days.size)
        val expectedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        expectedDays.forEachIndexed { index, name ->
            assertEquals(name, weekly.days[index].dayName)
        }
        assertTrue(weekly.maxCount >= 12)
    }

    @Test
    fun `setTimeFilter updates filter state and subtitle`() {
        val viewModel = StatsViewModel(FakeTaskRepository(), kotlinx.coroutines.Dispatchers.Unconfined)

        viewModel.setTimeFilter(StatsTimeFilter.THIS_MONTH)
        assertEquals(StatsTimeFilter.THIS_MONTH, viewModel.timeFilter.value)

        val stateMonth = viewModel.calculateStats(emptyList(), StatsTimeFilter.THIS_MONTH)
        assertEquals("This month", stateMonth.completedSubtitle)

        val stateLastWeek = viewModel.calculateStats(emptyList(), StatsTimeFilter.LAST_WEEK)
        assertEquals("Last week", stateLastWeek.completedSubtitle)

        val stateAllTime = viewModel.calculateStats(emptyList(), StatsTimeFilter.ALL_TIME)
        assertEquals("All time", stateAllTime.completedSubtitle)
    }
}
