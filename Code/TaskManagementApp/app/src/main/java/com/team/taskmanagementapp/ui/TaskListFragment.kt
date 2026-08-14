package com.team.taskmanagementapp.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.DueDateRange
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.SortOption
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentFilterBottomSheetBinding
import com.team.taskmanagementapp.databinding.FragmentTaskListBinding
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * TaskListFragment displays the home dashboard with greeting, summary metrics, and today's tasks.
 * Uses TaskViewModel to observe task data and update UI reactively.
 */
class TaskListFragment : Fragment() {

    private lateinit var binding: FragmentTaskListBinding
    private lateinit var taskAdapter: TaskAdapter
    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        updateGreeting()

        // Setup RecyclerView with toggle and click callbacks
        taskAdapter = TaskAdapter(
            onTaskToggleComplete = { task ->
                viewModel.toggleTaskComplete(task)
            },
            onTaskClick = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java)
                intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                startActivity(intent)
            }
        )
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        // Open filter bottom sheet when filter button is tapped
        binding.btnOpenFilter.setOnClickListener {
            val currentCriteria = viewModel.filterCriteria.value
            FilterBottomSheet.newInstance(currentCriteria) { action ->
                when (action) {
                    is FilterAction.Apply -> viewModel.applyFilter(action.criteria)
                    is FilterAction.Clear -> viewModel.clearFilter()
                }
            }.show(childFragmentManager, FilterBottomSheet.TAG)
        }
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        binding.dateText.text = dateFormat.format(calendar.time)

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
        binding.greetingText.text = "$greeting, Alex!"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe task list
                launch {
                    viewModel.uiState.collect { uiState ->
                        when (uiState) {
                            is UiState.Loading -> { /* Show loading if needed */ }
                            is UiState.Success -> {
                                taskAdapter.submitList(uiState.data)
                                binding.emptyStateText.visibility = View.GONE
                                binding.tasksRecyclerView.visibility = View.VISIBLE
                                updateMetrics(uiState.data)
                            }
                            is UiState.Empty -> {
                                binding.emptyStateText.visibility = View.VISIBLE
                                binding.tasksRecyclerView.visibility = View.GONE
                                updateMetrics(emptyList())
                            }
                            is UiState.Error -> {
                                binding.emptyStateText.text = uiState.message
                                binding.emptyStateText.visibility = View.VISIBLE
                                binding.tasksRecyclerView.visibility = View.GONE
                            }
                        }
                    }
                }
                // Observe filter state -> update active indicator
                launch {
                    viewModel.filterCriteria.collect { criteria ->
                        updateFilterIndicator(criteria)
                    }
                }
            }
        }
    }

    /** Shows/hides the active filter badge and tints the filter icon accordingly. */
    private fun updateFilterIndicator(criteria: FilterCriteria) {
        val isActive = criteria.status != null
                || criteria.priority != null
                || criteria.dueDateRange != DueDateRange.ALL
                || criteria.sortOption != SortOption.DUE_DATE_ASC

        binding.activeFilterBadge.visibility = if (isActive) View.VISIBLE else View.GONE

        val iconTint = if (isActive) {
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))
        } else {
            ColorStateList.valueOf(Color.parseColor("#737786"))
        }
        binding.ivFilterIcon.imageTintList = iconTint
    }

    private fun updateMetrics(tasks: List<com.team.taskmanagementapp.data.local.entity.Task>) {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = tasks.count {
            !it.isCompleted && it.status != TaskStatus.OVERDUE
        }
        val overdue = tasks.count {
            it.status == TaskStatus.OVERDUE
        }
        binding.totalTasksValue.text = total.toString()
        binding.completedValue.text = completed.toString()
        binding.pendingValue.text = pending.toString()
        binding.overdueValue.text = overdue.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter Action
    // ─────────────────────────────────────────────────────────────────────────

    sealed class FilterAction {
        data class Apply(val criteria: FilterCriteria) : FilterAction()
        object Clear : FilterAction()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FilterBottomSheet
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bottom sheet matching the TaskFlow "Filter & Sort" reference design:
     *   - STATUS: checkboxes (Todo / In Progress / Completed / Overdue)
     *   - PRIORITY: equal-width pill chips (Urgent / High / Medium / Low)
     *   - DUE DATE: radio rows (All / Today / This Week / This Month)
     *   - SORT BY: radio rows (Due date asc / Due date desc / Priority)
     *   - Apply Filters / Reset buttons
     */
    class FilterBottomSheet : BottomSheetDialogFragment() {

        companion object {
            const val TAG = "FilterBottomSheet"
            private const val ARG_STATUS     = "arg_status"
            private const val ARG_PRIORITY   = "arg_priority"
            private const val ARG_DATE_RANGE = "arg_date_range"
            private const val ARG_SORT       = "arg_sort"

            fun newInstance(
                criteria: FilterCriteria,
                onAction: (FilterAction) -> Unit
            ): FilterBottomSheet = FilterBottomSheet().also { sheet ->
                sheet.onFilterAction = onAction
                sheet.arguments = Bundle().apply {
                    putString(ARG_STATUS,     criteria.status?.name)
                    putString(ARG_PRIORITY,   criteria.priority?.name)
                    putString(ARG_DATE_RANGE, criteria.dueDateRange.name)
                    putString(ARG_SORT,       criteria.sortOption.name)
                }
            }
        }

        private var _binding: FragmentFilterBottomSheetBinding? = null
        private val binding get() = _binding!!

        var onFilterAction: ((FilterAction) -> Unit)? = null

        // Local draft selections
        private var selectedStatus: TaskStatus? = null
        private var selectedPriority: Priority? = null
        private var selectedDueDateRange: DueDateRange = DueDateRange.ALL
        private var selectedSortOption: SortOption = SortOption.DUE_DATE_ASC

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            restoreFromArgs()
            syncStatusUi()
            syncPriorityUi()
            syncDateRangeUi()
            syncSortUi()

            wireStatusRows()
            wirePriorityChips()
            wireDateRangeRows()
            wireSortRows()

            binding.btnApplyFilter.setOnClickListener {
                onFilterAction?.invoke(
                    FilterAction.Apply(
                        FilterCriteria(
                            status = selectedStatus,
                            priority = selectedPriority,
                            dueDateRange = selectedDueDateRange,
                            sortOption = selectedSortOption
                        )
                    )
                )
                dismiss()
            }

            binding.btnReset.setOnClickListener {
                onFilterAction?.invoke(FilterAction.Clear)
                dismiss()
            }
        }

        private fun restoreFromArgs() {
            selectedStatus = arguments?.getString(ARG_STATUS)
                ?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
            selectedPriority = arguments?.getString(ARG_PRIORITY)
                ?.let { runCatching { Priority.valueOf(it) }.getOrNull() }
            selectedDueDateRange = arguments?.getString(ARG_DATE_RANGE)
                ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() } ?: DueDateRange.ALL
            selectedSortOption = arguments?.getString(ARG_SORT)
                ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() } ?: SortOption.DUE_DATE_ASC
        }

        // ── STATUS ────────────────────────────────────────────────────────

        private fun wireStatusRows() {
            binding.statusTodo.setOnClickListener {
                selectedStatus = if (selectedStatus == TaskStatus.TODO) null else TaskStatus.TODO
                syncStatusUi()
            }
            binding.statusInProgress.setOnClickListener {
                selectedStatus = if (selectedStatus == TaskStatus.IN_PROGRESS) null else TaskStatus.IN_PROGRESS
                syncStatusUi()
            }
            binding.statusCompleted.setOnClickListener {
                selectedStatus = if (selectedStatus == TaskStatus.COMPLETED) null else TaskStatus.COMPLETED
                syncStatusUi()
            }
            binding.statusOverdue.setOnClickListener {
                selectedStatus = if (selectedStatus == TaskStatus.OVERDUE) null else TaskStatus.OVERDUE
                syncStatusUi()
            }
        }

        private fun syncStatusUi() {
            binding.cbStatusTodo.isChecked       = selectedStatus == TaskStatus.TODO
            binding.cbStatusInProgress.isChecked = selectedStatus == TaskStatus.IN_PROGRESS
            binding.cbStatusCompleted.isChecked  = selectedStatus == TaskStatus.COMPLETED
            binding.cbStatusOverdue.isChecked    = selectedStatus == TaskStatus.OVERDUE
        }

        // ── PRIORITY ──────────────────────────────────────────────────────

        private fun wirePriorityChips() {
            binding.chipPriorityUrgent.setOnClickListener {
                selectedPriority = if (selectedPriority == Priority.URGENT) null else Priority.URGENT
                syncPriorityUi()
            }
            binding.chipPriorityHigh.setOnClickListener {
                selectedPriority = if (selectedPriority == Priority.HIGH) null else Priority.HIGH
                syncPriorityUi()
            }
            binding.chipPriorityMedium.setOnClickListener {
                selectedPriority = if (selectedPriority == Priority.MEDIUM) null else Priority.MEDIUM
                syncPriorityUi()
            }
            binding.chipPriorityLow.setOnClickListener {
                selectedPriority = if (selectedPriority == Priority.LOW) null else Priority.LOW
                syncPriorityUi()
            }
        }

        private fun syncPriorityUi() {
            setChipSelected(binding.chipPriorityUrgent, selectedPriority == Priority.URGENT)
            setChipSelected(binding.chipPriorityHigh,   selectedPriority == Priority.HIGH)
            setChipSelected(binding.chipPriorityMedium, selectedPriority == Priority.MEDIUM)
            setChipSelected(binding.chipPriorityLow,    selectedPriority == Priority.LOW)
        }

        // ── DUE DATE RANGE ────────────────────────────────────────────────

        private fun wireDateRangeRows() {
            binding.dateRangeAll.setOnClickListener {
                selectedDueDateRange = DueDateRange.ALL
                syncDateRangeUi()
            }
            binding.dateRangeToday.setOnClickListener {
                selectedDueDateRange = DueDateRange.TODAY
                syncDateRangeUi()
            }
            binding.dateRangeThisWeek.setOnClickListener {
                selectedDueDateRange = DueDateRange.THIS_WEEK
                syncDateRangeUi()
            }
            binding.dateRangeThisMonth.setOnClickListener {
                selectedDueDateRange = DueDateRange.THIS_MONTH
                syncDateRangeUi()
            }
        }

        private fun syncDateRangeUi() {
            binding.rbDateRangeAll.isChecked       = selectedDueDateRange == DueDateRange.ALL
            binding.rbDateRangeToday.isChecked     = selectedDueDateRange == DueDateRange.TODAY
            binding.rbDateRangeThisWeek.isChecked  = selectedDueDateRange == DueDateRange.THIS_WEEK
            binding.rbDateRangeThisMonth.isChecked = selectedDueDateRange == DueDateRange.THIS_MONTH
        }

        // ── SORT BY ───────────────────────────────────────────────────────

        private fun wireSortRows() {
            binding.sortDueDateAsc.setOnClickListener {
                selectedSortOption = SortOption.DUE_DATE_ASC
                syncSortUi()
            }
            binding.sortDueDateDesc.setOnClickListener {
                selectedSortOption = SortOption.DUE_DATE_DESC
                syncSortUi()
            }
            binding.sortPriority.setOnClickListener {
                selectedSortOption = SortOption.PRIORITY_DESC
                syncSortUi()
            }
        }

        private fun syncSortUi() {
            binding.rbSortDueDateAsc.isChecked  = selectedSortOption == SortOption.DUE_DATE_ASC
            binding.rbSortDueDateDesc.isChecked = selectedSortOption == SortOption.DUE_DATE_DESC
            binding.rbSortPriority.isChecked    = selectedSortOption == SortOption.PRIORITY_DESC
        }

        // ── Chip visual helper ────────────────────────────────────────────

        private fun setChipSelected(chip: android.widget.TextView, selected: Boolean) {
            chip.isSelected = selected
            chip.setTextColor(if (selected) Color.WHITE else Color.parseColor("#1B1B1B"))
            chip.background = ContextCompat.getDrawable(
                requireContext(), R.drawable.bg_filter_chip_selector
            )?.also { it.state = chip.drawableState }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
}
