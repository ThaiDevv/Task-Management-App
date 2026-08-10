package com.team.taskmanagementapp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.team.taskmanagementapp.databinding.FragmentTaskListBinding
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import com.team.taskmanagementapp.data.repository.TaskRepository
import android.content.Intent
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.enums.SortOrder
import com.team.taskmanagementapp.data.model.enums.SortType
import com.team.taskmanagementapp.ui.detail.TaskDetailActivity
import com.team.taskmanagementapp.util.Constants
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
        val preferences = requireContext().getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        TaskViewModelFactory(repository, preferences)
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
        // Setup greeting
        updateGreeting()

        // Setup sort button + indicator
        setupSortUI()

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
    }

    private fun setupSortUI() {
        binding.sortButton.setOnClickListener { view -> showSortMenu(view) }
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        // Đánh dấu lựa chọn hiện tại
        popup.menu.findItem(
            when (viewModel.sortType.value) {
                SortType.DUE_DATE -> R.id.action_sort_due_date
                SortType.PRIORITY -> R.id.action_sort_priority
                SortType.CREATED_DATE -> R.id.action_sort_created_date
                SortType.TITLE -> R.id.action_sort_title
            }
        ).isChecked = true
        popup.menu.findItem(
            if (viewModel.sortOrder.value == SortOrder.ASC) {
                R.id.action_sort_ascending
            } else {
                R.id.action_sort_descending
            }
        ).isChecked = true

        popup.setOnMenuItemClickListener { item ->
            val currentType = viewModel.sortType.value
            val currentOrder = viewModel.sortOrder.value
            val newType = when (item.itemId) {
                R.id.action_sort_due_date -> SortType.DUE_DATE
                R.id.action_sort_priority -> SortType.PRIORITY
                R.id.action_sort_created_date -> SortType.CREATED_DATE
                R.id.action_sort_title -> SortType.TITLE
                else -> currentType
            }
            val newOrder = when (item.itemId) {
                R.id.action_sort_ascending -> SortOrder.ASC
                R.id.action_sort_descending -> SortOrder.DESC
                else -> currentOrder
            }
            viewModel.applySort(newType, newOrder)
            true
        }
        popup.show()
    }

    private fun updateSortIndicator(type: SortType, order: SortOrder) {
        val arrow = if (order == SortOrder.ASC) "↑" else "↓"
        val label = when (type) {
            SortType.DUE_DATE -> getString(R.string.sort_by_due_date)
            SortType.PRIORITY -> getString(R.string.sort_by_priority)
            SortType.CREATED_DATE -> getString(R.string.sort_by_created_date)
            SortType.TITLE -> getString(R.string.sort_by_title)
        }
        binding.sortIndicator.text = "$label $arrow"
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        binding.dateText.text = formattedDate

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
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is UiState.Loading -> {
                            // Show loading state if needed
                        }
                        is UiState.Success -> {
                            val tasks = uiState.data
                            taskAdapter.submitList(tasks)
                            binding.emptyStateText.visibility = View.GONE
                            binding.tasksRecyclerView.visibility = View.VISIBLE

                            // Update metrics
                            updateMetrics(tasks)
                        }
                        is UiState.Empty -> {
                            binding.emptyStateText.visibility = View.VISIBLE
                            binding.tasksRecyclerView.visibility = View.GONE
                            updateMetrics(emptyList())
                        }
                        is UiState.Error -> {
                            // Handle error state
                            binding.emptyStateText.text = uiState.message
                            binding.emptyStateText.visibility = View.VISIBLE
                            binding.tasksRecyclerView.visibility = View.GONE
                        }
                    }
                }

                // Observe sort state -> update sort indicator
                launch {
                    viewModel.sortType.collect { type ->
                        updateSortIndicator(type, viewModel.sortOrder.value)
                    }
                }
                launch {
                    viewModel.sortOrder.collect { order ->
                        updateSortIndicator(viewModel.sortType.value, order)
                    }
                }
            }
        }
    }

    private fun updateMetrics(tasks: List<com.team.taskmanagementapp.data.local.entity.Task>) {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = tasks.count { !it.isCompleted && it.status != com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE }
        val overdue = tasks.count { it.status == com.team.taskmanagementapp.data.model.enums.TaskStatus.OVERDUE }

        binding.totalTasksValue.text = total.toString()
        binding.completedValue.text = completed.toString()
        binding.pendingValue.text = pending.toString()
        binding.overdueValue.text = overdue.toString()
    }
}
