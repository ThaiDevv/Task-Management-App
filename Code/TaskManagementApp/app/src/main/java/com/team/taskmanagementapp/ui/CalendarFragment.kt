package com.team.taskmanagementapp.ui


import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
=======
import android.content.Context

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentCalendarBinding
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.viewmodel.CalendarViewModel
import com.team.taskmanagementapp.viewmodel.CalendarViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CalendarFragment — hiển thị lịch tháng + danh sách task của ngày được chọn.
 *
 * Logic:
 * - CalendarViewModel.loadTasksForMonth() query Room theo tháng qua Flow
 * - monthCache (Map<startOfDay, List<Task>>) dùng để vẽ dot indicator trên lịch
 * - Tap ngày → CalendarViewModel.selectDate() → selectedDateTasks cập nhật tức thì từ cache
 * - Navigate tháng → goToPreviousMonth() / goToNextMonth() → re-query DB, rebuild grid
 */
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter

    private val viewModel: CalendarViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())

        CalendarViewModelFactory(repository)
=======
        val preferences = requireContext().getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        TaskViewModelFactory(
            repository,
            requireContext().applicationContext,
            preferences
        )

    }

    // Calendar local chỉ dùng để build grid UI — source of truth là ViewModel
    private val displayCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonthNavigation()
        observeViewModel()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskToggleComplete = { task ->
                // Toggle qua TaskViewModel nếu cần; CalendarViewModel tự cập nhật qua Flow
                val db = AppDatabase.getInstance(requireContext())
                val repo = TaskRepository(db.taskDao())
                viewLifecycleOwner.lifecycleScope.launch {
                    val updated = task.copy(
                        isCompleted = !task.isCompleted,
                        updatedAt = System.currentTimeMillis()
                    )
                    repo.update(updated)
                }
            },
            onTaskClick = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java)
                intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                startActivity(intent)
            }
        )
        binding.scheduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.goToPreviousMonth()
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.goToNextMonth()
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Khi year hoặc month thay đổi → sync displayCalendar và rebuild header + grid
            combine(
                viewModel.currentYear,
                viewModel.currentMonth,
                viewModel.monthCache
            ) { year, month, cache ->
                Triple(year, month, cache)
            }.collect { (year, month, cache) ->
                displayCalendar.set(Calendar.YEAR, year)
                displayCalendar.set(Calendar.MONTH, month)
                updateMonthYearHeader()
                buildCalendarGrid(cache)
            }
        }

        // Khi danh sách task của ngày chọn thay đổi → update RecyclerView
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDateTasks.collect { tasks ->
                updateScheduleList(tasks)
                updateScheduleTitle()
            }
        }

        // Loading indicator
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                // Có thể gắn ProgressBar nếu có; hiện tại bỏ qua
            }
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun updateMonthYearHeader() {
        val monthName = displayCalendar.getDisplayName(
            Calendar.MONTH, Calendar.LONG, Locale.getDefault()
        )
        val year = displayCalendar.get(Calendar.YEAR)
        binding.monthYearText.text = "$monthName $year"
    }

    private fun updateScheduleTitle() {
        val selectedMs = viewModel.selectedDate.value
        val cal = Calendar.getInstance().apply { timeInMillis = selectedMs }
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        binding.scheduleTitle.text = "Schedule ${fmt.format(cal.time)}"
    }

    // ── Calendar Grid ─────────────────────────────────────────────────────────

    /**
     * Rebuild toàn bộ lưới ngày cho tháng đang hiển thị.
     * [cache] dùng để biết ngày nào có task → vẽ dot.
     */
    private fun buildCalendarGrid(cache: Map<Long, List<Task>>) {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        val tempCal = displayCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // DAY_OF_WEEK: Sun=1..Sat=7 → offset 0..6
        val firstDayOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1

        // Ô trống trước ngày đầu tháng
        repeat(firstDayOffset) { grid.addView(createEmptyCell()) }

        // Ô từng ngày
        for (day in 1..maxDays) {
            grid.addView(createDayCell(day, tempCal, cache))
        }

        // Lấp đầy đến 42 ô (6 hàng × 7 cột)
        val remaining = 42 - (firstDayOffset + maxDays)
        repeat(remaining) { grid.addView(createEmptyCell()) }
    }

    private fun createEmptyCell(): View {
        return View(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.touch_target_minimum)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun createDayCell(
        day: Int,
        tempCal: Calendar,
        cache: Map<Long, List<Task>>
    ): FrameLayout {
        val container = FrameLayout(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.touch_target_minimum)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }

        // Tính startOfDay cho ô này
        val dayCal = (tempCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayKey = dayCal.timeInMillis

        // Số ngày
        val dayText = TextView(requireContext()).apply {
            text = day.toString()
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            val size = resources.getDimensionPixelSize(R.dimen.spacing_32)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
        }

        // Highlight ngày được chọn
        val isSelected = dayKey == viewModel.selectedDate.value
        if (isSelected) {
            dayText.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
            dayText.setTextColor(Color.WHITE)
        }

        container.addView(dayText)

        // Dot indicator nếu ngày có task
        val tasksOnDay = cache[dayKey]
        if (!tasksOnDay.isNullOrEmpty()) {
            val dotSize = resources.getDimensionPixelSize(R.dimen.spacing_4)
            val dot = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_4)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(priorityColor(tasksOnDay))
                }
            }
            container.addView(dot)
        }

        // Tap ngày → cập nhật ViewModel
        container.setOnClickListener {
            viewModel.selectDate(dayKey)
            // Rebuild grid để cập nhật highlight
            buildCalendarGrid(viewModel.monthCache.value)
        }

        return container
    }

    // ── Schedule list ─────────────────────────────────────────────────────────

    private fun updateScheduleList(tasks: List<Task>) {
        val sorted = tasks.sortedBy { it.dueTime }
        taskAdapter.submitList(sorted)

        if (sorted.isEmpty()) {
            binding.emptyScheduleText.visibility = View.VISIBLE
            binding.scheduleRecyclerView.visibility = View.GONE
        } else {
            binding.emptyScheduleText.visibility = View.GONE
            binding.scheduleRecyclerView.visibility = View.VISIBLE
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chọn màu dot theo priority cao nhất trong danh sách task của ngày.
     */
    private fun priorityColor(tasks: List<Task>): Int {
        val res = requireContext()
        return when {
            tasks.any { it.priority == Priority.HIGH } ->
                ContextCompat.getColor(res, R.color.priority_high)
            tasks.any { it.priority == Priority.MEDIUM } ->
                ContextCompat.getColor(res, R.color.priority_medium)
            else ->
                ContextCompat.getColor(res, R.color.priority_low)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
