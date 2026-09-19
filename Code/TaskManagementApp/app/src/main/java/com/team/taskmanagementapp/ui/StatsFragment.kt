package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.model.stats.PomodoroFocusStats
import com.team.taskmanagementapp.data.model.stats.StatisticsUiState
import com.team.taskmanagementapp.data.model.stats.StatsTimeFilter
import com.team.taskmanagementapp.data.model.stats.TaskFocusSummary
import com.team.taskmanagementapp.data.model.stats.formatFocusDuration
import com.team.taskmanagementapp.databinding.FragmentStatsBinding
import com.team.taskmanagementapp.ui.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

/**
 * StatsFragment displays productivity statistics and task analytics dashboard
 * matching the Stitch TaskFlow UI/UX design (Bento Grid layout).
 */
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: StatsViewModel by viewModels {
        val db = com.team.taskmanagementapp.data.local.db.AppDatabase.getInstance(requireContext().applicationContext)
        com.team.taskmanagementapp.ui.viewmodel.StatsViewModelFactory(
            com.team.taskmanagementapp.data.repository.TaskRepository(db.taskDao()),
            com.team.taskmanagementapp.data.repository.PomodoroRepository(db.pomodoroDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnFilterStats.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderStats(state)
                }
            }
        }
    }

    private fun renderStats(state: StatisticsUiState) {
        // 1. Weekly Productivity Chart
        binding.chartWeeklyProductivity.setData(state.weeklyProductivity)
        binding.tvWeeklyProductivityTitle.setText(R.string.stats_completion_by_weekday)
        binding.tvWeeklyProductivitySubtitle.text = getString(R.string.stats_chart_period, state.completedSubtitle)
        binding.tvUnknownCompletionDates.visibility = if (state.hasUnknownCompletionDates) View.VISIBLE else View.GONE

        // 2. Completion Rate
        binding.viewCompletionRate.setProgress(state.completionRate)
        binding.tvCompletionRateStatus.text = state.completionRateLabel

        // 3. Completed Card
        binding.tvCompletedCount.text = state.completedCount.toString()
        binding.tvCompletedSubtitle.text = state.completedSubtitle

        // 4. Actual unfinished count in the same selected period.
        binding.tvPendingCount.text = state.pendingCount.toString()
        binding.tvPendingSubtitle.text = state.completedSubtitle

        // 5. Tasks by Priority
        val priorityStats = state.priorityStats
        binding.tvUrgentPriorityCount.text = resources.getQuantityString(R.plurals.stats_task_count, priorityStats.urgentCount, priorityStats.urgentCount)
        binding.progressUrgentPriority.progress = Math.round(priorityStats.urgentPercent * 100).coerceIn(0, 100)
        binding.tvHighPriorityCount.text = resources.getQuantityString(
            R.plurals.stats_task_count,
            priorityStats.highCount,
            priorityStats.highCount
        )
        binding.progressHighPriority.setProgress(
            Math.round(priorityStats.highPercent * 100).coerceIn(0, 100),
            true
        )

        binding.tvMediumPriorityCount.text = resources.getQuantityString(
            R.plurals.stats_task_count,
            priorityStats.mediumCount,
            priorityStats.mediumCount
        )
        binding.progressMediumPriority.setProgress(
            Math.round(priorityStats.mediumPercent * 100).coerceIn(0, 100),
            true
        )

        binding.tvLowPriorityCount.text = resources.getQuantityString(
            R.plurals.stats_task_count,
            priorityStats.lowCount,
            priorityStats.lowCount
        )
        binding.progressLowPriority.setProgress(
            Math.round(priorityStats.lowPercent * 100).coerceIn(0, 100),
            true
        )

        // 6. Pomodoro Focus card (Task 15)
        renderPomodoroStats(state)
    }

    /**
     * Render card "Pomodoro Focus": hôm nay / tuần này, số phiên đã hoàn thành trong kỳ
     * và top task tập trung nhiều nhất — kèm trạng thái rỗng khi chưa có dữ liệu.
     */
    private fun renderPomodoroStats(state: StatisticsUiState) {
        val pomodoro: PomodoroFocusStats = state.pomodoro

        binding.tvPomodoroToday.text = formatFocusDuration(pomodoro.todayMinutes)
        binding.tvPomodoroWeek.text = formatFocusDuration(pomodoro.weekMinutes)

        binding.tvPomodoroCompleted.text = if (pomodoro.hasPeriodFocus) {
            val completed = resources.getQuantityString(
                R.plurals.stats_pomodoro_completed,
                pomodoro.periodSessionCount,
                pomodoro.periodSessionCount
            )
            "$completed · ${state.completedSubtitle}"
        } else {
            getString(R.string.stats_pomodoro_no_sessions)
        }

        renderTopTasks(if (pomodoro.hasAnyFocus) pomodoro.topTasks else emptyList())
    }

    /** Tối đa 3 task tập trung nhiều nhất; hàng không có dữ liệu bị ẩn hẳn. */
    private fun renderTopTasks(topTasks: List<TaskFocusSummary>) {
        binding.layoutTopTasks.visibility = if (topTasks.isEmpty()) View.GONE else View.VISIBLE
        if (topTasks.isEmpty()) return

        val rows = listOf(
            binding.rowTopTask1 to (binding.tvTopTask1Name to binding.tvTopTask1Minutes),
            binding.rowTopTask2 to (binding.tvTopTask2Name to binding.tvTopTask2Minutes),
            binding.rowTopTask3 to (binding.tvTopTask3Name to binding.tvTopTask3Minutes)
        )

        rows.forEachIndexed { index, (row, views) ->
            val (nameView, metaView) = views
            val summary = topTasks.getOrNull(index)
            if (summary == null) {
                row.visibility = View.GONE
            } else {
                row.visibility = View.VISIBLE
                nameView.text = summary.taskTitle
                val sessions = resources.getQuantityString(
                    R.plurals.stats_session_count,
                    summary.sessionCount,
                    summary.sessionCount
                )
                metaView.text = "${formatFocusDuration(summary.totalMinutes)} · $sessions"
            }
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf(
            getString(R.string.stats_this_week),
            getString(R.string.stats_last_week),
            getString(R.string.stats_this_month),
            getString(R.string.stats_all_time)
        )

        val currentFilter = viewModel.timeFilter.value
        val selectedIndex = when (currentFilter) {
            StatsTimeFilter.THIS_WEEK -> 0
            StatsTimeFilter.LAST_WEEK -> 1
            StatsTimeFilter.THIS_MONTH -> 2
            StatsTimeFilter.ALL_TIME -> 3
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.stats_filter_dialog_title)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val newFilter = when (which) {
                    0 -> StatsTimeFilter.THIS_WEEK
                    1 -> StatsTimeFilter.LAST_WEEK
                    2 -> StatsTimeFilter.THIS_MONTH
                    3 -> StatsTimeFilter.ALL_TIME
                    else -> StatsTimeFilter.THIS_WEEK
                }
                viewModel.setTimeFilter(newFilter)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
