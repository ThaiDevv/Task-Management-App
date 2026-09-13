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
import com.team.taskmanagementapp.data.model.stats.StatisticsUiState
import com.team.taskmanagementapp.data.model.stats.StatsTimeFilter
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
            com.team.taskmanagementapp.data.repository.TaskRepository(db.taskDao())
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

        // 2. Completion Rate
        binding.viewCompletionRate.setProgress(state.completionRate)
        binding.tvCompletionRateStatus.text = state.completionRateLabel

        // 3. Completed Card
        binding.tvCompletedCount.text = state.completedCount.toString()
        binding.tvCompletedSubtitle.text = state.completedSubtitle

        // 4. Deep Work Card
        binding.tvDeepWorkHours.text = state.deepWorkHours
        binding.tvDeepWorkSubtitle.text = state.deepWorkSubtitle

        // 5. Tasks by Priority
        val priorityStats = state.priorityStats
        binding.tvHighPriorityCount.text = getString(
            R.string.stats_tasks_count_format,
            priorityStats.highCount
        )
        binding.progressHighPriority.setProgress(
            Math.round(priorityStats.highPercent * 100).coerceIn(0, 100),
            true
        )

        binding.tvMediumPriorityCount.text = getString(
            R.string.stats_tasks_count_format,
            priorityStats.mediumCount
        )
        binding.progressMediumPriority.setProgress(
            Math.round(priorityStats.mediumPercent * 100).coerceIn(0, 100),
            true
        )

        binding.tvLowPriorityCount.text = getString(
            R.string.stats_tasks_count_format,
            priorityStats.lowCount
        )
        binding.progressLowPriority.setProgress(
            Math.round(priorityStats.lowPercent * 100).coerceIn(0, 100),
            true
        )
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
