package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.FragmentTaskListBinding
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.data.local.db.AppDatabase
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
        // Setup greeting
        updateGreeting()

        // Setup RecyclerView
        taskAdapter = TaskAdapter()
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        // FAB to Create Task
        binding.fabCreateTask.setOnClickListener {
            findNavController().navigate(R.id.createTaskFragment)
        }
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
