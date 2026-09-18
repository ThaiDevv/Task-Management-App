package com.team.taskmanagementapp.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
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
import com.team.taskmanagementapp.data.model.CompletionFilter
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
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
    private lateinit var completedTaskAdapter: TaskAdapter

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
                    val completion = bundle.getString(FilterBottomSheet.RESULT_COMPLETION)
                        ?.let { runCatching { CompletionFilter.valueOf(it) }.getOrNull() }
                        ?: CompletionFilter.ALL
                    val statuses = bundle.getStringArrayList(FilterBottomSheet.RESULT_STATUSES)
                        .orEmpty()
                        .mapNotNull { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
                        .toSet()
                    val priorities = bundle.getStringArrayList(FilterBottomSheet.RESULT_PRIORITIES)
                        .orEmpty()
                        .mapNotNull { runCatching { Priority.valueOf(it) }.getOrNull() }
                        .toSet()
                    val dueDateRange = bundle.getString(FilterBottomSheet.RESULT_DATE_RANGE)
                        ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() }
                        ?: DueDateRange.ALL
                    val sortOption = bundle.getString(FilterBottomSheet.RESULT_SORT)
                        ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
                        ?: SortOption.DUE_DATE_ASC
                    viewModel.applyFilter(
                        FilterCriteria(
                            completion = completion,
                            statuses = statuses,
                            priorities = priorities,
                            dueDateRange = dueDateRange,
                            customStartDate = bundle.getLong(FilterBottomSheet.RESULT_CUSTOM_START, 0L)
                                .takeIf { it > 0L },
                            customEndDate = bundle.getLong(FilterBottomSheet.RESULT_CUSTOM_END, 0L)
                                .takeIf { it > 0L },
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
        if (::binding.isInitialized) {
            outState.putParcelable(STATE_TODAY_SCROLL, binding.todayTasksRecyclerView.layoutManager?.onSaveInstanceState())
            outState.putParcelable(STATE_UPCOMING_SCROLL, binding.upcomingTasksRecyclerView.layoutManager?.onSaveInstanceState())
        }
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
        completedTaskAdapter = TaskAdapter(
            onTaskToggleComplete = { task -> viewModel.toggleTaskComplete(task) },
            onTaskClick = { openTaskDetail(it) }
        )
        binding.completedTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedTaskAdapter
        }
        binding.btnOpenFilter.contentDescription = getString(R.string.home_filter_description)

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
        binding.greetingText.text = "$greeting!"
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
        val todayList = allTasks.filter { !it.isCompleted && it.dueDate <= nowEndToday }
            .sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
            )
        // Upcoming contains only work still to do. Completed work has its own section.
        val upcomingList = allTasks.filter { !it.isCompleted && it.dueDate > nowEndToday }
            .sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
            )
        val completedList = allTasks.filter { it.isCompleted }
            .sortedByDescending { it.completedAt ?: it.updatedAt }
        completedTaskAdapter.submitList(completedList)
        binding.completedSection.visibility = if (completedList.isEmpty()) View.GONE else View.VISIBLE

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
        val isActive = criteria.completion != CompletionFilter.ALL
                || criteria.statuses.isNotEmpty()
                || criteria.priorities.isNotEmpty()
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
        val overdueColor = if (overdue > 0) Color.parseColor("#F43F5E")
            else ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        binding.overdueValue.setTextColor(overdueColor)
        binding.overdueSubtitle.setTextColor(overdueColor)
        binding.overdueSubtitle.setText(if (overdue > 0) R.string.home_action_needed else R.string.home_no_overdue)

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

    private var selectedCompletion: CompletionFilter = CompletionFilter.ALL
    private val selectedStatuses = linkedSetOf<TaskStatus>()
    private val selectedPriorities = linkedSetOf<Priority>()
    private var selectedDueDateRange: DueDateRange = DueDateRange.ALL
    private var selectedCustomStartDate: Long? = null
    private var selectedCustomEndDate: Long? = null
    private var selectedSortOption: SortOption = SortOption.DUE_DATE_ASC
    private var isSynchronizing = false
    private var previewJob: Job? = null
    private lateinit var repository: TaskRepository

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

        repository = TaskRepository(AppDatabase.getInstance(requireContext()).taskDao())

        // Priority: savedInstanceState (mid-session edits) > arguments (initial criteria)
        if (savedInstanceState != null) {
            restoreFromSavedState(savedInstanceState)
        } else {
            restoreFromArgs()
        }

        wireCompletionControl()
        wireStatusChips()
        wirePriorityChips()
        wireDateRangeChips()
        wireSortDropdown()
        syncAllUi()
        updatePreview()

        binding.btnApplyFilter.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    RESULT_ACTION to ACTION_APPLY,
                    RESULT_COMPLETION to selectedCompletion.name,
                    RESULT_STATUSES to ArrayList(selectedStatuses.map { it.name }),
                    RESULT_PRIORITIES to ArrayList(selectedPriorities.map { it.name }),
                    RESULT_DATE_RANGE to selectedDueDateRange.name,
                    RESULT_CUSTOM_START to (selectedCustomStartDate ?: 0L),
                    RESULT_CUSTOM_END to (selectedCustomEndDate ?: 0L),
                    RESULT_SORT to selectedSortOption.name
                )
            )
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            selectedCompletion = CompletionFilter.ALL
            selectedStatuses.clear()
            selectedPriorities.clear()
            selectedDueDateRange = DueDateRange.ALL
            selectedCustomStartDate = null
            selectedCustomEndDate = null
            selectedSortOption = SortOption.DUE_DATE_ASC
            syncAllUi()
            updatePreview()
        }
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        bottomSheetDialog?.behavior?.apply {
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_COMPLETION, selectedCompletion.name)
        outState.putStringArrayList(STATE_STATUSES, ArrayList(selectedStatuses.map { it.name }))
        outState.putStringArrayList(STATE_PRIORITIES, ArrayList(selectedPriorities.map { it.name }))
        outState.putString(STATE_DATE_RANGE, selectedDueDateRange.name)
        outState.putLong(STATE_CUSTOM_START, selectedCustomStartDate ?: 0L)
        outState.putLong(STATE_CUSTOM_END, selectedCustomEndDate ?: 0L)
        outState.putString(STATE_SORT, selectedSortOption.name)
    }

    // ── Restore helpers ───────────────────────────────────────────────────

    /** Restores mid-session selection state saved in [onSaveInstanceState]. */
    private fun restoreFromSavedState(state: Bundle) {
        selectedCompletion = state.getString(STATE_COMPLETION)
            ?.let { runCatching { CompletionFilter.valueOf(it) }.getOrNull() }
            ?: CompletionFilter.ALL
        selectedStatuses.clear()
        selectedStatuses += state.getStringArrayList(STATE_STATUSES).orEmpty()
            .mapNotNull { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
        selectedPriorities.clear()
        selectedPriorities += state.getStringArrayList(STATE_PRIORITIES).orEmpty()
            .mapNotNull { runCatching { Priority.valueOf(it) }.getOrNull() }
        selectedDueDateRange = state.getString(STATE_DATE_RANGE)
            ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() } ?: DueDateRange.ALL
        selectedCustomStartDate = state.getLong(STATE_CUSTOM_START, 0L).takeIf { it > 0L }
        selectedCustomEndDate = state.getLong(STATE_CUSTOM_END, 0L).takeIf { it > 0L }
        selectedSortOption = state.getString(STATE_SORT)
            ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() } ?: SortOption.DUE_DATE_ASC
    }

    /** Initialises selection state from the initial filter criteria passed via [arguments]. */
    private fun restoreFromArgs() {
        selectedCompletion = arguments?.getString(ARG_COMPLETION)
            ?.let { runCatching { CompletionFilter.valueOf(it) }.getOrNull() }
            ?: CompletionFilter.ALL
        selectedStatuses.clear()
        selectedStatuses += arguments?.getStringArrayList(ARG_STATUSES).orEmpty()
            .mapNotNull { runCatching { TaskStatus.valueOf(it) }.getOrNull() }
        selectedPriorities.clear()
        selectedPriorities += arguments?.getStringArrayList(ARG_PRIORITIES).orEmpty()
            .mapNotNull { runCatching { Priority.valueOf(it) }.getOrNull() }
        selectedDueDateRange = arguments?.getString(ARG_DATE_RANGE)
            ?.let { runCatching { DueDateRange.valueOf(it) }.getOrNull() } ?: DueDateRange.ALL
        selectedCustomStartDate = arguments?.getLong(ARG_CUSTOM_START, 0L)?.takeIf { it > 0L }
        selectedCustomEndDate = arguments?.getLong(ARG_CUSTOM_END, 0L)?.takeIf { it > 0L }
        selectedSortOption = arguments?.getString(ARG_SORT)
            ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() } ?: SortOption.DUE_DATE_ASC
    }

    private fun wireCompletionControl() {
        binding.completionToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isSynchronizing) return@addOnButtonCheckedListener
            selectedCompletion = when (checkedId) {
                R.id.completionNotDone -> CompletionFilter.NOT_DONE
                R.id.completionDone -> CompletionFilter.DONE
                else -> CompletionFilter.ALL
            }
            updatePreview()
        }
    }

    private fun wireStatusChips() {
        mapOf(
            binding.chipStatusTodo to TaskStatus.TODO,
            binding.chipStatusInProgress to TaskStatus.IN_PROGRESS,
            binding.chipStatusOverdue to TaskStatus.OVERDUE
        ).forEach { (chip, status) ->
            chip.setOnCheckedChangeListener { _, checked ->
                if (isSynchronizing) return@setOnCheckedChangeListener
                if (checked) selectedStatuses += status else selectedStatuses -= status
                updatePreview()
            }
        }
    }

    private fun wirePriorityChips() {
        mapOf(
            binding.chipPriorityUrgent to Priority.URGENT,
            binding.chipPriorityHigh to Priority.HIGH,
            binding.chipPriorityMedium to Priority.MEDIUM,
            binding.chipPriorityLow to Priority.LOW
        ).forEach { (chip, priority) ->
            chip.setOnCheckedChangeListener { _, checked ->
                if (isSynchronizing) return@setOnCheckedChangeListener
                if (checked) selectedPriorities += priority else selectedPriorities -= priority
                updatePreview()
            }
        }
    }

    private fun wireDateRangeChips() {
        binding.dueDateChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isSynchronizing || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            selectedDueDateRange = when (checkedIds.first()) {
                R.id.chipDueOverdue -> DueDateRange.OVERDUE
                R.id.chipDueToday -> DueDateRange.TODAY
                R.id.chipDueNextSevenDays -> DueDateRange.NEXT_7_DAYS
                R.id.chipDueThisMonth -> DueDateRange.THIS_MONTH
                R.id.chipDueNoDate -> DueDateRange.NO_DUE_DATE
                R.id.chipDueCustom -> DueDateRange.CUSTOM
                else -> DueDateRange.ALL
            }
            if (selectedDueDateRange == DueDateRange.CUSTOM) {
                showCustomDateRangePicker()
            } else {
                selectedCustomStartDate = null
                selectedCustomEndDate = null
                syncCustomRangeLabel()
                updatePreview()
            }
        }
    }

    private fun wireSortDropdown() {
        val labels = sortOptions.map { getString(it.second) }
        binding.sortDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.sortDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedSortOption = sortOptions[position].first
            updatePreview()
        }
    }

    private fun syncAllUi() {
        isSynchronizing = true
        binding.completionToggleGroup.check(
            when (selectedCompletion) {
                CompletionFilter.NOT_DONE -> R.id.completionNotDone
                CompletionFilter.DONE -> R.id.completionDone
                CompletionFilter.ALL -> R.id.completionAll
            }
        )
        binding.chipStatusTodo.isChecked = TaskStatus.TODO in selectedStatuses
        binding.chipStatusInProgress.isChecked = TaskStatus.IN_PROGRESS in selectedStatuses
        binding.chipStatusOverdue.isChecked = TaskStatus.OVERDUE in selectedStatuses
        binding.chipPriorityUrgent.isChecked = Priority.URGENT in selectedPriorities
        binding.chipPriorityHigh.isChecked = Priority.HIGH in selectedPriorities
        binding.chipPriorityMedium.isChecked = Priority.MEDIUM in selectedPriorities
        binding.chipPriorityLow.isChecked = Priority.LOW in selectedPriorities
        binding.dueDateChipGroup.check(
            when (selectedDueDateRange) {
                DueDateRange.OVERDUE -> R.id.chipDueOverdue
                DueDateRange.TODAY -> R.id.chipDueToday
                DueDateRange.NEXT_7_DAYS -> R.id.chipDueNextSevenDays
                DueDateRange.THIS_MONTH -> R.id.chipDueThisMonth
                DueDateRange.NO_DUE_DATE -> R.id.chipDueNoDate
                DueDateRange.CUSTOM -> R.id.chipDueCustom
                DueDateRange.ALL -> R.id.chipDueAll
            }
        )
        binding.sortDropdown.setText(
            getString(sortOptions.first { it.first == selectedSortOption }.second),
            false
        )
        isSynchronizing = false
        syncCustomRangeLabel()
    }

    private fun showCustomDateRangePicker() {
        val initialStart = Calendar.getInstance().apply {
            selectedCustomStartDate?.let { timeInMillis = it }
        }
        val startDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val start = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val initialEnd = Calendar.getInstance().apply {
                    timeInMillis = selectedCustomEndDate ?: start.timeInMillis
                }
                val endDialog = DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        val end = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDay, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        selectedCustomStartDate = start.timeInMillis
                        selectedCustomEndDate = end.timeInMillis.coerceAtLeast(start.timeInMillis)
                        syncCustomRangeLabel()
                        updatePreview()
                    },
                    initialEnd.get(Calendar.YEAR),
                    initialEnd.get(Calendar.MONTH),
                    initialEnd.get(Calendar.DAY_OF_MONTH)
                )
                endDialog.datePicker.minDate = start.timeInMillis
                endDialog.setTitle("Select end date")
                endDialog.show()
            },
            initialStart.get(Calendar.YEAR),
            initialStart.get(Calendar.MONTH),
            initialStart.get(Calendar.DAY_OF_MONTH)
        )
        startDialog.setTitle("Select start date")
        startDialog.setOnCancelListener {
            selectedDueDateRange = DueDateRange.ALL
            syncAllUi()
            updatePreview()
        }
        startDialog.show()
    }

    private fun syncCustomRangeLabel() {
        val start = selectedCustomStartDate
        val end = selectedCustomEndDate
        binding.tvCustomDateRange.visibility =
            if (selectedDueDateRange == DueDateRange.CUSTOM && start != null && end != null) View.VISIBLE
            else View.GONE
        if (start != null && end != null) {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            binding.tvCustomDateRange.text = "${formatter.format(start)} – ${formatter.format(end)}"
        }
    }

    private fun buildCriteria() = FilterCriteria(
        completion = selectedCompletion,
        statuses = selectedStatuses.toSet(),
        priorities = selectedPriorities.toSet(),
        dueDateRange = selectedDueDateRange,
        customStartDate = selectedCustomStartDate,
        customEndDate = selectedCustomEndDate,
        sortOption = selectedSortOption
    )

    private fun updatePreview() {
        val criteria = buildCriteria()
        val activeCount = listOf(
            criteria.completion != CompletionFilter.ALL,
            criteria.statuses.isNotEmpty(),
            criteria.priorities.isNotEmpty(),
            criteria.dueDateRange != DueDateRange.ALL,
            criteria.sortOption != SortOption.DUE_DATE_ASC
        ).count { it }
        binding.tvFilterSummary.text = if (activeCount == 0) {
            getString(R.string.filter_no_active)
        } else {
            resources.getQuantityString(R.plurals.filter_active_count, activeCount, activeCount)
        }

        previewJob?.cancel()
        previewJob = viewLifecycleOwner.lifecycleScope.launch {
            val count = repository.getFilteredTasks(criteria).first().size
            binding.btnApplyFilter.text =
                resources.getQuantityString(R.plurals.filter_show_task_count, count, count)
        }
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
        const val RESULT_ACTION = "result_action"
        const val RESULT_COMPLETION = "result_completion"
        const val RESULT_STATUSES = "result_statuses"
        const val RESULT_PRIORITIES = "result_priorities"
        const val RESULT_DATE_RANGE = "result_date_range"
        const val RESULT_CUSTOM_START = "result_custom_start"
        const val RESULT_CUSTOM_END = "result_custom_end"
        const val RESULT_SORT = "result_sort"

        // Arguments keys (initial criteria passed when creating the sheet)
        private const val ARG_COMPLETION = "arg_completion"
        private const val ARG_STATUSES = "arg_statuses"
        private const val ARG_PRIORITIES = "arg_priorities"
        private const val ARG_DATE_RANGE = "arg_date_range"
        private const val ARG_CUSTOM_START = "arg_custom_start"
        private const val ARG_CUSTOM_END = "arg_custom_end"
        private const val ARG_SORT = "arg_sort"

        // onSaveInstanceState keys (mid-session edits)
        private const val STATE_COMPLETION = "state_completion"
        private const val STATE_STATUSES = "state_statuses"
        private const val STATE_PRIORITIES = "state_priorities"
        private const val STATE_DATE_RANGE = "state_date_range"
        private const val STATE_CUSTOM_START = "state_custom_start"
        private const val STATE_CUSTOM_END = "state_custom_end"
        private const val STATE_SORT = "state_sort"

        private val sortOptions = listOf(
            SortOption.DUE_DATE_ASC to R.string.filter_sort_due_soonest,
            SortOption.DUE_DATE_DESC to R.string.filter_sort_due_latest,
            SortOption.PRIORITY_DESC to R.string.filter_sort_priority,
            SortOption.CREATED_DESC to R.string.filter_sort_created,
            SortOption.UPDATED_DESC to R.string.filter_sort_updated,
            SortOption.TITLE_ASC to R.string.filter_sort_title
        )

        /**
         * Creates a new [FilterBottomSheet] pre-populated with the current filter criteria.
         * No callback is required — results are delivered via the FragmentResult API.
         */
        fun newInstance(currentCriteria: FilterCriteria): FilterBottomSheet {
            return FilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_COMPLETION, currentCriteria.completion.name)
                    putStringArrayList(ARG_STATUSES, ArrayList(currentCriteria.statuses.map { it.name }))
                    putStringArrayList(ARG_PRIORITIES, ArrayList(currentCriteria.priorities.map { it.name }))
                    putString(ARG_DATE_RANGE, currentCriteria.dueDateRange.name)
                    putLong(ARG_CUSTOM_START, currentCriteria.customStartDate ?: 0L)
                    putLong(ARG_CUSTOM_END, currentCriteria.customEndDate ?: 0L)
                    putString(ARG_SORT, currentCriteria.sortOption.name)
                }
            }
        }
    }
}
