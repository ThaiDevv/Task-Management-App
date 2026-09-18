package com.team.taskmanagementapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.team.taskmanagementapp.MainActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Calendar

/** Temporary fixture rows only; existing tasks are never deleted by this test. */
class DashboardUxTest {
    @Test fun dashboardSeparatesCompletedWorkAndDisablesConflictingFilters() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dao = AppDatabase.getInstance(context).taskDao()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val base = Task(title = "Prepare presentation", description = "Review the task flow",
            dueDate = tomorrow, dueTime = tomorrow + 10 * 60 * 60 * 1000L, priority = Priority.HIGH)
        val ids = mutableListOf<Long>()
        try {
            ids += dao.insertTask(base)
            ids += dao.insertTask(base.copy(title = "Review project", priority = Priority.URGENT))
            ids += dao.insertTask(base.copy(title = "Finish documentation", isCompleted = true,
                status = TaskStatus.COMPLETED, completedAt = System.currentTimeMillis()))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                repeat(30) {
                    instrumentation.waitForIdleSync()
                    var ready = false
                    scenario.onActivity { ready = it.findViewById<TextView>(R.id.totalTasksValue)?.text?.toString()?.toIntOrNull()?.let { n -> n >= 3 } == true }
                    if (!ready) Thread.sleep(100)
                }
                scenario.onActivity {
                    val upcoming = it.findViewById<RecyclerView>(R.id.upcomingTasksRecyclerView).adapter as UpcomingTaskAdapter
                    assertFalse(upcoming.currentList.filterIsInstance<UpcomingItem.TaskItem>().any { item -> item.task.isCompleted })
                    val headers = upcoming.currentList.filterIsInstance<UpcomingItem.Header>()
                    assertEquals(headers.size, headers.map { header -> header.dateLabel }.distinct().size)
                    assertEquals("No overdue tasks", it.findViewById<TextView>(R.id.overdueSubtitle).text.toString())
                    assertFalse(it.findViewById<TextView>(R.id.greetingText).text.contains("Alex"))
                    assertTrue(it.findViewById<RecyclerView>(R.id.completedTasksRecyclerView).adapter!!.itemCount >= 1)
                }
                screenshot("ux-fixed-dashboard.png")
                onView(withText(R.string.home_completed)).perform(scrollTo())
                screenshot("ux-fixed-completed.png")
                onView(withId(R.id.tab_calendar)).perform(click())
                screenshot("ux-fixed-calendar.png")
                onView(withId(R.id.tab_stats)).perform(click())
                onView(withId(R.id.tvPendingCount)).check(matches(withText("2")))
                screenshot("ux-fixed-stats.png")
                onView(withText(R.string.stats_urgent_priority)).perform(scrollTo())
                onView(withId(R.id.tvUrgentPriorityCount)).check(matches(withText("1 task")))
                screenshot("ux-fixed-priorities.png")
                onView(withId(R.id.tab_home)).perform(click())
                onView(withId(btnFilter())).perform(scrollTo(), click())
                onView(withId(R.id.completionDone)).perform(click())
                onView(withId(R.id.chipStatusTodo)).check(matches(not(isEnabled())))
                onView(withId(R.id.chipStatusInProgress)).check(matches(not(isEnabled())))
                onView(withId(R.id.chipDueOverdue)).check(matches(not(isEnabled())))
                onView(withId(R.id.completionConstraintHint)).check(matches(isDisplayed()))
                screenshot("ux-fixed-done-filter.png")
            }
            val intent = Intent(context, TaskDetailActivity::class.java).putExtra(Constants.EXTRA_TASK_ID, ids.first())
            ActivityScenario.launch<TaskDetailActivity>(intent).use {
                onView(withId(R.id.tvPriorityBadge)).check(matches(isDisplayed()))
                screenshot("ux-fixed-detail.png")
            }
        } finally {
            ids.forEach { id -> dao.getTaskById(id)?.let { dao.deleteTask(it) } }
        }
    }

    private fun btnFilter() = R.id.btnOpenFilter

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Wait for scroll, chart and ring animations to reach their final rendered frame.
        Thread.sleep(650)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
