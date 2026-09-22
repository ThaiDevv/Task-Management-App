package com.team.taskmanagementapp.data.model.stats

import com.team.taskmanagementapp.data.local.FakePomodoroDao
import com.team.taskmanagementapp.data.local.entity.PomodoroSession
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.SessionType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.PomodoroRepository
import com.team.taskmanagementapp.ui.FakeTaskRepository
import com.team.taskmanagementapp.ui.viewmodel.StatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho Task 15 — Pomodoro Statistics.
 *
 * Phủ 3 yêu cầu số liệu: thời gian tập trung **hôm nay**, **tuần này** và
 * **số phiên hoàn thành theo từng Task**, cùng các trạng thái rỗng.
 *
 * Cách kiểm chứng:
 * - Tầng DAO → Repository: [FakePomodoroDao] cài lại đúng ngữ nghĩa SQL của `PomodoroDao`.
 * - Tầng ViewModel: dựng [StatsViewModel] thật với fake DAO (không cần Room/Android).
 * - Hàm thuần: [formatFocusDuration], [buildTopTaskSummaries].
 */
class PomodoroStatsTest {

    private companion object {
        const val TASK_A = 1L
        const val TASK_B = 2L
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE

        /** Chắc chắn nằm ở tuần TRƯỚC (tuần bắt đầu Thứ Hai): 8 ngày trước luôn < Thứ Hai tuần này. */
        const val LAST_WEEK_OFFSET = 8 * DAY
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun task(id: Int, title: String) = Task(
        id = id,
        title = title,
        description = "",
        dueDate = now(),
        dueTime = 0L,
        priority = Priority.MEDIUM,
        status = TaskStatus.TODO,
        isCompleted = false,
        isRecurring = false,
        createdAt = 0L,
        updatedAt = 0L
    )

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Thời gian tập trung hôm nay / tuần này (DAO → Repository)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `today and this week focus time are zero when no session was recorded`() = runBlocking {
        val repository = PomodoroRepository(FakePomodoroDao(TASK_A))

        assertEquals(0, repository.getTodayFocusMinutes())
        assertEquals(0, repository.getThisWeekFocusMinutes())
    }

    @Test
    fun `today focus time sums only today's completed focus sessions`() = runBlocking {
        val dao = FakePomodoroDao(TASK_A)
        val timestamp = now()
        dao.seedCompletedFocus(TASK_A, startTime = timestamp, minutes = 25)
        dao.seedCompletedFocus(TASK_A, startTime = timestamp + MINUTE, minutes = 15)

        val repository = PomodoroRepository(dao)

        assertEquals(40, repository.getTodayFocusMinutes())
        assertEquals("Session hôm nay luôn nằm trong tuần này", 40, repository.getThisWeekFocusMinutes())
    }

    @Test
    fun `this week focus time includes today but excludes last week`() = runBlocking {
        val dao = FakePomodoroDao(TASK_A)
        val timestamp = now()
        dao.seedCompletedFocus(TASK_A, startTime = timestamp, minutes = 25)
        dao.seedCompletedFocus(TASK_A, startTime = timestamp - LAST_WEEK_OFFSET, minutes = 50)

        val repository = PomodoroRepository(dao)

        assertEquals("Chỉ phiên hôm nay được tính", 25, repository.getTodayFocusMinutes())
        assertEquals("Tuần này chỉ tính từ Thứ Hai tuần này", 25, repository.getThisWeekFocusMinutes())
        assertEquals("Tuần trước vẫn nằm trong lịch sử", 75, repository.getTotalFocusMinutesInRange(0L, Long.MAX_VALUE))
    }

    @Test
    fun `breaks and interrupted sessions never count toward focus time`() = runBlocking {
        val dao = FakePomodoroDao(TASK_A)
        val timestamp = now()
        dao.seedCompletedFocus(TASK_A, startTime = timestamp, minutes = 25)
        dao.seedCompletedBreak(TASK_A, startTime = timestamp + MINUTE, sessionType = SessionType.SHORT_BREAK, minutes = 5)
        dao.seedCompletedBreak(TASK_A, startTime = timestamp + 2 * MINUTE, sessionType = SessionType.LONG_BREAK, minutes = 15)
        // Phiên FOCUS bị skip giữa chừng (isCompleted = false)
        dao.seed(
            PomodoroSession(
                taskId = TASK_A,
                startTime = timestamp + 3 * MINUTE,
                endTime = timestamp + 10 * MINUTE,
                durationInMinutes = 7,
                sessionType = SessionType.FOCUS,
                isCompleted = false
            )
        )

        val repository = PomodoroRepository(dao)

        assertEquals(25, repository.getTodayFocusMinutes())
        assertEquals(25, repository.getThisWeekFocusMinutes())
        assertEquals(1, repository.getFocusSessionCountInRange(0L, Long.MAX_VALUE))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Số phiên hoàn thành theo từng Task
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `focus stats per task group minutes and session counts correctly`() = runBlocking {
        val dao = FakePomodoroDao(TASK_A, TASK_B)
        val timestamp = now()
        dao.seedCompletedFocus(TASK_A, startTime = timestamp, minutes = 25)
        dao.seedCompletedFocus(TASK_A, startTime = timestamp + MINUTE, minutes = 25)
        dao.seedCompletedFocus(TASK_B, startTime = timestamp, minutes = 15)
        dao.seedCompletedBreak(TASK_A, startTime = timestamp + 2 * MINUTE)

        val repository = PomodoroRepository(dao)
        val range = repository.getDayRange(timestamp)
        val byTask = repository.getFocusStatsByTask(range.first, range.last)

        assertEquals(2, byTask.size)
        assertEquals("Task tập trung nhiều nhất đứng trước", TASK_A, byTask[0].taskId)
        assertEquals(50, byTask[0].totalMinutes)
        assertEquals(2, byTask[0].sessionCount)
        assertEquals(TASK_B, byTask[1].taskId)
        assertEquals(15, byTask[1].totalMinutes)
        assertEquals(1, byTask[1].sessionCount)
    }

    @Test
    fun `focus stats per task attach task titles and keep top three`() {
        val byTask = listOf(
            TaskFocusStats(taskId = 1L, totalMinutes = 50, sessionCount = 2),
            TaskFocusStats(taskId = 2L, totalMinutes = 90, sessionCount = 4),
            TaskFocusStats(taskId = 3L, totalMinutes = 10, sessionCount = 1),
            TaskFocusStats(taskId = 4L, totalMinutes = 5, sessionCount = 1)
        )
        val tasks = listOf(task(1, "Alpha"), task(2, "Beta"), task(3, "Gamma"), task(4, "Delta"))

        val summaries = buildTopTaskSummaries(byTask, tasks)

        assertEquals(TOP_FOCUS_TASK_LIMIT, summaries.size)
        assertEquals(listOf("Beta", "Alpha", "Gamma"), summaries.map { it.taskTitle })
        assertEquals(90, summaries[0].totalMinutes)
        assertEquals(4, summaries[0].sessionCount)
    }

    @Test
    fun `focus stats per task skip tasks that no longer exist`() {
        val byTask = listOf(TaskFocusStats(taskId = 99L, totalMinutes = 60, sessionCount = 3))

        assertTrue(buildTopTaskSummaries(byTask, listOf(task(1, "Alpha"))).isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Trạng thái rỗng / không có dữ liệu
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `empty data produces empty stats and zero labels`() {
        val stats = PomodoroFocusStats()

        assertFalse(stats.hasAnyFocus)
        assertFalse(stats.hasPeriodFocus)
        assertTrue(stats.topTasks.isEmpty())
        assertEquals("0m", formatFocusDuration(stats.todayMinutes))
    }

    @Test
    fun `focus duration label formats minutes hours and mixed values`() {
        assertEquals("0m", formatFocusDuration(0))
        assertEquals("0m", formatFocusDuration(-30))
        assertEquals("45m", formatFocusDuration(45))
        assertEquals("1h", formatFocusDuration(60))
        assertEquals("2h", formatFocusDuration(120))
        assertEquals("2h 5m", formatFocusDuration(125))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. ViewModel: DAO → Repository → ViewModel
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `viewModel exposes real today focus time and per task stats`() = runBlocking {
        val dao = FakePomodoroDao(TASK_A, TASK_B)
        val timestamp = now()
        dao.seedCompletedFocus(TASK_A, startTime = timestamp, minutes = 25)
        dao.seedCompletedFocus(TASK_A, startTime = timestamp + MINUTE, minutes = 20)
        dao.seedCompletedFocus(TASK_B, startTime = timestamp + 2 * MINUTE, minutes = 15)

        val viewModel = StatsViewModel(
            FakeTaskRepository(listOf(task(1, "Alpha"), task(2, "Beta"))),
            PomodoroRepository(dao),
            Dispatchers.Unconfined
        )

        val state = awaitStats(viewModel)

        assertEquals(60, state.pomodoro.todayMinutes)
        assertEquals(60, state.pomodoro.weekMinutes)
        assertEquals(60, state.pomodoro.periodMinutes)
        assertEquals(3, state.pomodoro.periodSessionCount)
        assertEquals("1h", state.deepWorkHours)
        assertEquals("Focused time", state.deepWorkSubtitle)

        assertEquals(listOf("Alpha", "Beta"), state.pomodoro.topTasks.map { it.taskTitle })
        assertEquals(45, state.pomodoro.topTasks[0].totalMinutes)
        assertEquals(2, state.pomodoro.topTasks[0].sessionCount)
        assertEquals(15, state.pomodoro.topTasks[1].totalMinutes)
    }

    @Test
    fun `viewModel shows empty state when there is no pomodoro data`() = runBlocking {
        val viewModel = StatsViewModel(
            FakeTaskRepository(listOf(task(1, "Alpha"))),
            PomodoroRepository(FakePomodoroDao(TASK_A)),
            Dispatchers.Unconfined
        )

        val state = awaitStats(viewModel)

        assertEquals("0m", state.deepWorkHours)
        assertEquals("No focus sessions yet", state.deepWorkSubtitle)
        assertFalse(state.pomodoro.hasAnyFocus)
        assertTrue(state.pomodoro.topTasks.isEmpty())
    }

    /** Chờ StateFlow phát giá trị đã tính xong (bỏ qua trạng thái `isLoading = true` ban đầu). */
    private suspend fun awaitStats(viewModel: StatsViewModel): StatisticsUiState =
        withTimeout(5_000) { viewModel.uiState.first { !it.isLoading } }
}
