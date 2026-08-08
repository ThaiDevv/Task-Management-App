package com.team.taskmanagementapp.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enum.Priority
import com.team.taskmanagementapp.data.model.enum.RecurrenceType
import com.team.taskmanagementapp.data.model.enum.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.ActivityTaskDetailBinding
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.DateTimeUtils
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding

    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }

    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar setup
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Fetch task ID from Intent
        val taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            finish()
            return
        }

        // Observe task data
        viewModel.getTaskById(taskId)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedTask.collect { task ->
                    if (task != null) {
                        currentTask = task
                        bindTaskData(task)
                    }
                }
            }
        }

        // Action button listeners
        setupCompleteButton()
        setupEditButton()
        setupDeleteButton()
    }

    private fun bindTaskData(task: Task) {
        // Task Title & Description
        binding.tvTaskTitle.text = task.title
        binding.tvDescription.text = task.description.ifBlank {
            getString(R.string.task_detail_no_description)
        }

        // Separate Due Date & Scheduled Time display to match polished design
        val dateStr = DateTimeUtils.formatTimestamp(task.dueDate, "MMM dd, yyyy")
        val timeStr = DateTimeUtils.formatTimestamp(task.dueTime, DateTimeUtils.FORMAT_TIME_ONLY)
        binding.tvDueDate.text = if (dateStr.isBlank()) "No Date" else dateStr
        binding.tvScheduledTime.text = if (timeStr.isBlank()) "No Time" else timeStr

        // Status & Priority Badges
        bindStatusBadge(task.status, task)
        bindPriorityBadge(task.priority)

        // Recurrence Card & Day Selector
        bindRecurrence(task)

        // Complete Button state & text
        val completeText = if (task.isCompleted) {
            getString(R.string.task_detail_button_uncomplete)
        } else {
            getString(R.string.task_detail_button_complete)
        }
        binding.btnComplete.text = completeText
    }

    private fun bindStatusBadge(status: TaskStatus, task: Task) {
        val (textResId, colorResId) = when {
            DateTimeUtils.isOverdue(task.dueDate, task.isCompleted) -> {
                R.string.task_status_overdue to R.color.status_overdue
            }
            else -> when (status) {
                TaskStatus.TODO -> R.string.task_status_todo to R.color.status_todo
                TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress to R.color.status_in_progress
                TaskStatus.COMPLETED -> R.string.task_status_completed to R.color.status_completed
                TaskStatus.OVERDUE -> R.string.task_status_overdue to R.color.status_overdue
            }
        }

        binding.tvStatusBadge.text = getString(textResId)
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, colorResId))

        val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_badge_status)?.mutate()
        bgDrawable?.setTint(ContextCompat.getColor(this, colorResId).let { color ->
            android.graphics.Color.argb(
                51,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color)
            )
        })
        binding.tvStatusBadge.background = bgDrawable
    }

    private fun bindPriorityBadge(priority: Priority) {
        val (textResId, colorResId) = when (priority) {
            Priority.LOW -> R.string.task_priority_low to R.color.priority_low
            Priority.MEDIUM -> R.string.task_priority_medium to R.color.priority_medium
            Priority.HIGH -> R.string.task_priority_high to R.color.priority_high
            Priority.URGENT -> R.string.task_priority_urgent to R.color.priority_urgent
        }

        binding.tvPriorityBadge.text = getString(textResId)
        binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(this, colorResId))

        val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_badge_priority)?.mutate()
        bgDrawable?.setTint(ContextCompat.getColor(this, colorResId).let { color ->
            android.graphics.Color.argb(
                51,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color)
            )
        })
        binding.tvPriorityBadge.background = bgDrawable
    }

    private fun bindRecurrence(task: Task) {
        if (!task.isRecurring || task.recurrenceType == RecurrenceType.NONE) {
            binding.cardRecurrence.visibility = View.GONE
            return
        }

        binding.cardRecurrence.visibility = View.VISIBLE

        val recurrenceText = when (task.recurrenceType) {
            RecurrenceType.DAILY -> getString(R.string.task_detail_recurrence_daily)
            RecurrenceType.WEEKLY -> getString(R.string.task_detail_recurrence_weekly)
            RecurrenceType.MONTHLY -> getString(R.string.task_detail_recurrence_monthly)
            RecurrenceType.NONE -> getString(R.string.task_detail_recurrence_none)
        }

        binding.tvRecurrenceType.text = recurrenceText
    }

    private fun setupCompleteButton() {
        val toggleAction = {
            currentTask?.let { task ->
                viewModel.toggleTaskComplete(task)
            }
        }
        binding.btnComplete.setOnClickListener { toggleAction() }
        binding.fabComplete.setOnClickListener { toggleAction() }
    }

    private fun setupDeleteButton() {
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun showDeleteConfirmDialog() {
        val task = currentTask ?: return

        if (task.isRecurring) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.task_detail_delete_recurring_title)
                .setMessage(R.string.task_detail_delete_recurring_msg)
                .setNeutralButton(R.string.action_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.task_detail_delete_only_this) { _, _ ->
                    // Logic to delete only this instance
                    viewModel.deleteTask(task)
                }
                .setPositiveButton(R.string.task_detail_delete_all_occurrences) { _, _ ->
                    // TODO: PART 2 - Delete all occurrences is not supported by current data model
                    // No action taken as recurring series cannot be identified
                }
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.task_detail_delete_confirm_title)
                .setMessage(R.string.task_detail_delete_confirm_msg)
                .setNegativeButton(R.string.task_detail_delete_confirm_negative) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.task_detail_delete_confirm_positive) { _, _ ->
                    viewModel.deleteTask(task)
                }
                .show()
        }
    }

    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener {
            currentTask?.let { task ->
                val intent = Intent(this, Class.forName("com.team.taskmanagementapp.ui.addedit.AddEditTaskActivity"))
                intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                startActivity(intent)
            }
        }
    }
}
