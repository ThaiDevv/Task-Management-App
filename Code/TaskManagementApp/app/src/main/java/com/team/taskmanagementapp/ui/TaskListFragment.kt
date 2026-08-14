package com.team.taskmanagementapp.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.DueDateRange
import com.team.taskmanagementapp.data.model.FilterCriteria
import com.team.taskmanagementapp.data.model.SortOption
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentFilterBottomSheetBinding
import com.team.taskmanagementapp.databinding.FragmentTaskListBinding
import com.team.taskmanagementapp.ui.activity.AddEditTaskActivity
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
 * TaskListFragment displays the home dashboard with greeting, summary metrics,
 * Today's Tasks, Upcoming Tasks, and Productivity Insight banner.
 */
class TaskListFragment : Fragment() {

    private lateinit var binding: FragmentTaskListBinding
    private lateinit var todayTaskAdapter: TaskAdapter
    private lateinit var upcomingTaskAdapter: UpcomingTaskAdapter

    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository, requireContext().applicationContext)
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

        // Today's Tasks Adapter
        todayTaskAdapter = TaskAdapter(
            onTaskToggleComplete = { task -> viewModel.toggleTaskComplete(task) },
            onTaskClick = { openTaskDetail(it) }
        )
        binding.todayTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayTaskAdapter
        }

        // Upcoming Tasks Adapter (timeline style)
        upcomingTaskAdapter = UpcomingTaskAdapter(
            onTaskClick = { openTaskDetail(it) }
        )
        binding.upcomingTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingTaskAdapter
        }

        // Open filter bottom sheet
        binding.btnOpenFilter.setOnClickListener {
            val currentCriteria = viewModel.filterCriteria.value
            FilterBottomSheet.newInstance(currentCriteria) { action ->
                when (action) {
                    is FilterAction.Apply -> viewModel.applyFilter(action.criteria)
                    is FilterAction.Clear -> viewModel.clearFilter()
                }
            }.show(childFragmentManager, FilterBottomSheet.TAG)
        }

        // Quick add task button on Insight Banner
        binding.btnQuickAddTask.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTaskActivity::class.java)
            startActivity(intent)
        }

        // View Full Calendar button (no-op placeholder)
        binding.btnViewCalendar.setOnClickListener { /* TODO: navigate to calendar */ }
    }

    private fun openTaskDetail(task: Task) {
        val intent = Intent(requireContext(), TaskDetailActivity::class.java)
        intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
        startActivity(intent)
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
                            is UiState.Loading -> { /* Loading state */ }
                            is UiState.Success -> {
                                displayTaskList(uiState.data)
                                updateMetrics(uiState.data)
                            }
                            is UiState.Empty -> {
                                displayTaskList(emptyList())
                                updateMetrics(emptyList())
                            }
                            is UiState.Error -> {
                                binding.emptyStateText.text = uiState.message
                                binding.emptyStateText.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                // Observe filter state
                launch {
                    viewModel.filterCriteria.collect { criteria ->
                        updateFilterIndicator(criteria)
                    }
                }
            }
        }
    }

    private fun displayTaskList(allTasks: List<Task>) {
        binding.emptyStateText.visibility = View.GONE

        val nowEndToday = getEndOfTodayMillis()
        val todayList = allTasks.filter { it.dueDate <= nowEndToday }
        // Sort upcoming by dueDate ascending so earliest appears at top
        val upcomingList = allTasks.filter { it.dueDate > nowEndToday }
            .sortedBy { it.dueDate }

        todayTaskAdapter.submitList(todayList)
        upcomingTaskAdapter.submitTaskList(upcomingList)

        if (todayList.isEmpty()) {
            binding.todayEmptyStateText.visibility = View.VISIBLE
            binding.todayTasksRecyclerView.visibility = View.GONE
        } else {
            binding.todayEmptyStateText.visibility = View.GONE
            binding.todayTasksRecyclerView.visibility = View.VISIBLE
        }

        if (upcomingList.isEmpty()) {
            binding.upcomingEmptyStateText.visibility = View.VISIBLE
            binding.upcomingTasksRecyclerView.visibility = View.GONE
        } else {
            binding.upcomingEmptyStateText.visibility = View.GONE
            binding.upcomingTasksRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun getEndOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
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

    private fun updateMetrics(tasks: List<Task>) {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = tasks.count { !it.isCompleted && it.status != TaskStatus.OVERDUE }
        val overdue = tasks.count { !it.isCompleted && (it.status == TaskStatus.OVERDUE || (it.dueDate < System.currentTimeMillis())) }

        binding.totalTasksValue.text = total.toString()
        binding.completedValue.text = completed.toString()
        binding.pendingValue.text = pending.toString()
        binding.overdueValue.text = overdue.toString()

        // Dynamic progress bar weight calculation
        val completedRatio = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0
        val pendingRatio = if (total > 0) (pending.toFloat() / total.toFloat() * 100).toInt() else 0

        val completedParams = binding.completedProgressBar.layoutParams as? LinearLayout.LayoutParams
        completedParams?.weight = completedRatio.coerceIn(0, 100).toFloat()
        binding.completedProgressBar.layoutParams = completedParams

        val pendingParams = binding.pendingProgressBar.layoutParams as? LinearLayout.LayoutParams
        pendingParams?.weight = pendingRatio.coerceIn(0, 100).toFloat()
        binding.pendingProgressBar.layoutParams = pendingParams
    }
}

/** Actions communicated from the FilterBottomSheet back to TaskListFragment. */
sealed class FilterAction {
    data class Apply(val criteria: FilterCriteria) : FilterAction()
    object Clear : FilterAction()
}

/**
 * Bottom Sheet DialogFragment for task filtering and sorting.
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var onFilterAction: ((FilterAction) -> Unit)? = null

    private var selectedStatus: TaskStatus? = null
    private var selectedPriority: Priority? = null
    private var selectedDueDateRange: DueDateRange = DueDateRange.ALL
    private var selectedSortOption: SortOption = SortOption.DUE_DATE_ASC

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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

    companion object {
        const val TAG = "FilterBottomSheet"
        private const val ARG_STATUS     = "arg_status"
        private const val ARG_PRIORITY   = "arg_priority"
        private const val ARG_DATE_RANGE = "arg_date_range"
        private const val ARG_SORT       = "arg_sort"

        fun newInstance(
            currentCriteria: FilterCriteria,
            onAction: (FilterAction) -> Unit
        ): FilterBottomSheet {
            return FilterBottomSheet().apply {
                onFilterAction = onAction
                arguments = Bundle().apply {
                    currentCriteria.status?.let { putString(ARG_STATUS, it.name) }
                    currentCriteria.priority?.let { putString(ARG_PRIORITY, it.name) }
                    putString(ARG_DATE_RANGE, currentCriteria.dueDateRange.name)
                    putString(ARG_SORT, currentCriteria.sortOption.name)
                }
            }
        }
    }
}
