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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
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
import com.team.taskmanagementapp.util.DateTimeUtils
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

    private var todayScrollState: Parcelable? = null
    private var upcomingScrollState: Parcelable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            todayScrollState = savedInstanceState.getParcelable(STATE_TODAY_SCROLL)
            upcomingScrollState = savedInstanceState.getParcelable(STATE_UPCOMING_SCROLL)
        }
        // Register on childFragmentManager because FilterBottomSheet is shown as a child fragment.
        // Registered in onViewCreated (tied to viewLifecycleOwner) so it is re-registered each
        // time the view is recreated, which is exactly what we need after a configuration change.
        childFragmentManager.setFragmentResultListener(
            FilterBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(FilterBottomSheet.RESULT_ACTION)) {
                FilterBottomSheet.ACTION_APPLY -> {
                    val status = bundle.getString(FilterBottomSheet.RESULT_STATUS)
                        ?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
                    val priority = bundle.getString(FilterBottomSheet.RESULT_PRIORITY)
                        ?.let { runCatching { Priority.valueOf(it) }.getOrNull() }
                    val dueDateRange = bundle.getString(FilterBottomSheet.RESULT_DATE_RANGE)
                        ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() }
                        ?: DueDateRange.ALL
                    val sortOption = bundle.getString(FilterBottomSheet.RESULT_SORT)
                        ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
                        ?: SortOption.DUE_DATE_ASC
                    viewModel.applyFilter(
                        FilterCriteria(
                            status = status,
                            priority = priority,
                            dueDateRange = dueDateRange,
                            sortOption = sortOption
                        )
                    )
                }
                FilterBottomSheet.ACTION_CLEAR -> viewModel.clearFilter()
            }
        }
        setupUI()
        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_TODAY_SCROLL, binding.todayTasksRecyclerView.layoutManager?.onSaveInstanceState())
        outState.putParcelable(STATE_UPCOMING_SCROLL, binding.upcomingTasksRecyclerView.layoutManager?.onSaveInstanceState())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
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

        // Open filter bottom sheet — no lambda passed; results arrive via FragmentResult API
        binding.btnOpenFilter.setOnClickListener {
            val currentCriteria = viewModel.filterCriteria.value
            FilterBottomSheet.newInstance(currentCriteria)
                .show(childFragmentManager, FilterBottomSheet.TAG)
        }

        // Quick add task button on Insight Banner
        binding.btnQuickAddTask.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTaskActivity::class.java)
            startActivity(intent)
        }

        // View Full Calendar button
        binding.btnViewCalendar.setOnClickListener {
            findNavController().navigate(R.id.calendarFragment)
        }

        // State overlay button listeners
        binding.viewErrorState.btnErrorRetry.setOnClickListener {
            viewModel.loadAllTasks()
        }

        binding.viewEmptyState.btnEmptyCreateTask.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTaskActivity::class.java)
            startActivity(intent)
        }
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
                // Observe task list uiState
                launch {
                    viewModel.uiState.collect { uiState ->
                        when (uiState) {
                            is UiState.Loading -> {
                                showScreenState(ScreenState.LOADING)
                            }
                            is UiState.Success -> {
                                displayTaskList(uiState.data)
                                updateMetrics(uiState.data)
                                showScreenState(ScreenState.CONTENT)
                            }
                            is UiState.Empty -> {
                                showScreenState(ScreenState.EMPTY)
                            }
                            is UiState.Error -> {
                                binding.viewErrorState.tvErrorMessage.text = uiState.message
                                showScreenState(ScreenState.ERROR)
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
                // Observe user feedback messages (Snackbar)
                launch {
                    viewModel.userMessage.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    /**
     * Enum representing top-level screen visibility states.
     */
    private enum class ScreenState {
        CONTENT, LOADING, EMPTY, ERROR
    }

    /**
     * Switches top-level screen state with smooth crossfade animation.
     */
    private fun showScreenState(state: ScreenState) {
        val targetView = when (state) {
            ScreenState.CONTENT -> binding.contentContainer
            ScreenState.LOADING -> binding.viewLoadingState.root
            ScreenState.EMPTY   -> binding.viewEmptyState.root
            ScreenState.ERROR   -> binding.viewErrorState.root
        }

        val allStateViews = listOf(
            binding.contentContainer,
            binding.viewLoadingState.root,
            binding.viewEmptyState.root,
            binding.viewErrorState.root
        )

        allStateViews.forEach { view ->
            view.animate().cancel()
            if (view == targetView) {
                view.visibility = View.VISIBLE
                view.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setListener(null)
            } else {
                view.visibility = View.GONE
                view.alpha = 0f
            }
        }
    }

    private fun displayTaskList(allTasks: List<Task>) {
        val nowEndToday = getEndOfTodayMillis()
        val todayList = allTasks.filter { it.dueDate <= nowEndToday }
            .sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
        // Sort upcoming by combined due timestamp ascending so earliest appears at top
        val upcomingList = allTasks.filter { it.dueDate > nowEndToday }
            .sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }

        todayTaskAdapter.submitList(todayList) {
            todayScrollState?.let {
                binding.todayTasksRecyclerView.layoutManager?.onRestoreInstanceState(it)
                todayScrollState = null
            }
        }
        upcomingTaskAdapter.submitTaskList(upcomingList) {
            upcomingScrollState?.let {
                binding.upcomingTasksRecyclerView.layoutManager?.onRestoreInstanceState(it)
                upcomingScrollState = null
            }
        }

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
        val now = System.currentTimeMillis()
        val overdue = tasks.count {
            val combinedDue = DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime)
            !it.isCompleted && (it.status == TaskStatus.OVERDUE || (combinedDue > 0L && combinedDue < now))
        }
        val completed = tasks.count { it.isCompleted }
        val pending = (total - completed - overdue).coerceAtLeast(0)

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

    companion object {
        private const val STATE_TODAY_SCROLL = "state_today_scroll"
        private const val STATE_UPCOMING_SCROLL = "state_upcoming_scroll"
    }
}

/** Actions communicated from the FilterBottomSheet back to TaskListFragment. */
sealed class FilterAction {
    data class Apply(val criteria: FilterCriteria) : FilterAction()
    object Clear : FilterAction()
}

/**
 * Bottom Sheet DialogFragment for task filtering and sorting.
 *
 * Configuration-change safety:
 * - The 4 selection fields are initialised from [arguments] (initial criteria) in [restoreFromArgs].
 * - Mid-session changes (user picks a new option before rotating) are saved in
 *   [onSaveInstanceState] and restored first in [onViewCreated] before falling back to [arguments].
 * - The [onFilterAction] lambda pattern is replaced with the FragmentResult API so Apply/Clear
 *   work correctly after recreation without depending on the old Fragment instance.
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

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

        // Priority: savedInstanceState (mid-session edits) > arguments (initial criteria)
        if (savedInstanceState != null) {
            restoreFromSavedState(savedInstanceState)
        } else {
            restoreFromArgs()
        }

        // Sync all UI controls to reflect the restored selection state
        syncStatusUi()
        syncPriorityUi()
        syncDateRangeUi()
        syncSortUi()

        wireStatusRows()
        wirePriorityChips()
        wireDateRangeRows()
        wireSortRows()

        binding.btnApplyFilter.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    RESULT_ACTION     to ACTION_APPLY,
                    RESULT_STATUS     to selectedStatus?.name,
                    RESULT_PRIORITY   to selectedPriority?.name,
                    RESULT_DATE_RANGE to selectedDueDateRange.name,
                    RESULT_SORT       to selectedSortOption.name
                )
            )
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_ACTION to ACTION_CLEAR)
            )
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Persist mid-session edits across configuration changes
        outState.putString(STATE_STATUS,     selectedStatus?.name)
        outState.putString(STATE_PRIORITY,   selectedPriority?.name)
        outState.putString(STATE_DATE_RANGE, selectedDueDateRange.name)
        outState.putString(STATE_SORT,       selectedSortOption.name)
    }

    // ── Restore helpers ───────────────────────────────────────────────────

    /** Restores mid-session selection state saved in [onSaveInstanceState]. */
    private fun restoreFromSavedState(state: Bundle) {
        selectedStatus = state.getString(STATE_STATUS)
            ?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
        selectedPriority = state.getString(STATE_PRIORITY)
            ?.let { runCatching { Priority.valueOf(it) }.getOrNull() }
        selectedDueDateRange = state.getString(STATE_DATE_RANGE)
            ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() } ?: DueDateRange.ALL
        selectedSortOption = state.getString(STATE_SORT)
            ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() } ?: SortOption.DUE_DATE_ASC
    }

    /** Initialises selection state from the initial filter criteria passed via [arguments]. */
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

        // FragmentResult key — used by both FilterBottomSheet (sender) and TaskListFragment (receiver)
        const val REQUEST_KEY = "filter_bottom_sheet_result"

        // Action values inside the result bundle
        const val ACTION_APPLY = "action_apply"
        const val ACTION_CLEAR = "action_clear"

        // Result bundle keys
        const val RESULT_ACTION     = "result_action"
        const val RESULT_STATUS     = "result_status"
        const val RESULT_PRIORITY   = "result_priority"
        const val RESULT_DATE_RANGE = "result_date_range"
        const val RESULT_SORT       = "result_sort"

        // Arguments keys (initial criteria passed when creating the sheet)
        private const val ARG_STATUS     = "arg_status"
        private const val ARG_PRIORITY   = "arg_priority"
        private const val ARG_DATE_RANGE = "arg_date_range"
        private const val ARG_SORT       = "arg_sort"

        // onSaveInstanceState keys (mid-session edits)
        private const val STATE_STATUS     = "state_status"
        private const val STATE_PRIORITY   = "state_priority"
        private const val STATE_DATE_RANGE = "state_date_range"
        private const val STATE_SORT       = "state_sort"

        /**
         * Creates a new [FilterBottomSheet] pre-populated with the current filter criteria.
         * No callback is required — results are delivered via the FragmentResult API.
         */
        fun newInstance(currentCriteria: FilterCriteria): FilterBottomSheet {
            return FilterBottomSheet().apply {
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
