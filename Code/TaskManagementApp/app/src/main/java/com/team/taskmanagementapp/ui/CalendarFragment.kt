package com.team.taskmanagementapp.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.gridlayout.widget.GridLayout
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentCalendarBinding
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.DateTimeUtils
import com.team.taskmanagementapp.viewmodel.CalendarViewModel
import com.team.taskmanagementapp.viewmodel.CalendarViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CalendarFragment — displays month calendar grid + timeline schedule for selected date.
 * Uses CalendarScheduleAdapter (dedicated UI layout item_calendar_task.xml).
 *
 * ViewModel scoped to Activity (activityViewModels) to prevent recreation on fragment
 * navigation, which caused stale StateFlow emissions from old fragment-scoped instances.
 */
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleAdapter: CalendarScheduleAdapter

    private val viewModel: CalendarViewModel by activityViewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())

        CalendarViewModelFactory(requireActivity().application, repository)
    }

    private val displayCalendar = Calendar.getInstance()
    private var scheduleScrollState: Parcelable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            scheduleScrollState = savedInstanceState.getParcelable(STATE_SCHEDULE_SCROLL)
        }
        setupRecyclerView()
        setupMonthNavigation()
        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_SCHEDULE_SCROLL, binding.scheduleRecyclerView.layoutManager?.onSaveInstanceState())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }
    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        scheduleAdapter = CalendarScheduleAdapter(
            onTaskClick = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java)
                intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                startActivity(intent)
            }
        )
        binding.scheduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.navigateMonth(-1)
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.navigateMonth(1)
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
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

                launch {
                    viewModel.tasksForSelectedDate.collect { tasks ->
                        updateScheduleList(tasks)
                        updateScheduleTitle()
                    }
                }

                launch {
                    viewModel.isLoading.collect { }
                }
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
        binding.scheduleTitle.text = "Schedule  ${fmt.format(cal.time)}"
    }

    // ── Calendar Grid ─────────────────────────────────────────────────────────

    private fun buildCalendarGrid(cache: Map<Long, List<Task>>) {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        val tempCal = displayCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1

        repeat(firstDayOffset) { grid.addView(createEmptyCell()) }

        for (day in 1..maxDays) {
            grid.addView(createDayCell(day, tempCal, cache))
        }

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

        val dayCal = (tempCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dayKey = DateTimeUtils.getStartOfDay(dayCal.timeInMillis)

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

        val isSelected = dayKey == viewModel.selectedDate.value
        if (isSelected) {
            dayText.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
            dayText.setTextColor(Color.WHITE)
        }

        container.addView(dayText)

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

        container.setOnClickListener {
            viewModel.selectDate(dayKey)
            buildCalendarGrid(viewModel.monthCache.value)
        }

        return container
    }

    // ── Schedule list ─────────────────────────────────────────────────────────

    private fun updateScheduleList(tasks: List<Task>) {
        val sorted = tasks.sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
        scheduleAdapter.submitList(sorted) {
            scheduleScrollState?.let {
                binding.scheduleRecyclerView.layoutManager?.onRestoreInstanceState(it)
                scheduleScrollState = null
            }
        }

        if (sorted.isEmpty()) {
            binding.emptyScheduleText.visibility = View.VISIBLE
            binding.scheduleRecyclerView.visibility = View.GONE
        } else {
            binding.emptyScheduleText.visibility = View.GONE
            binding.scheduleRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun priorityColor(tasks: List<Task>): Int {
        val res = requireContext()
        return when {
            tasks.any { it.priority == Priority.URGENT } ->
                ContextCompat.getColor(res, R.color.priority_high)
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

    companion object {
        private const val STATE_SCHEDULE_SCROLL = "state_schedule_scroll"
    }
}
