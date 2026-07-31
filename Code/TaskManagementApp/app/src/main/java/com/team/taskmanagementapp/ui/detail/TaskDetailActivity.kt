package com.team.taskmanagementapp.ui.detail

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

        // Step 3A: Nhận task ID từ Intent
        val taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            finish()
            return
        }

        // Load task data
        viewModel.getTaskById(taskId)

        // Observe selectedTask
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
    }

    // ========================================================================
    // Step 3B: Bind Task Data lên UI
    // ========================================================================

    private fun bindTaskData(task: Task) {
        // Title
        binding.tvTaskTitle.text = task.title

        // Description
        binding.tvDescription.text = task.description.ifBlank {
            getString(R.string.task_detail_no_description)
        }

        // Due Date & Time
        val dateStr = DateTimeUtils.formatTimestamp(task.dueDate, DateTimeUtils.FORMAT_FULL_DATE)
        val timeStr = DateTimeUtils.formatTimestamp(task.dueTime, DateTimeUtils.FORMAT_TIME_ONLY)
        binding.tvDueDateTime.text = "$dateStr  •  $timeStr"

        // Status Badge
        bindStatusBadge(task.status, task)

        // Priority Badge
        bindPriorityBadge(task.priority)

        // Recurrence Card
        bindRecurrence(task)

        // Timestamps
        val createdStr = DateTimeUtils.formatTimestamp(task.createdAt, DateTimeUtils.FORMAT_DATE_TIME)
        val updatedStr = DateTimeUtils.formatTimestamp(task.updatedAt, DateTimeUtils.FORMAT_DATE_TIME)
        binding.tvCreatedAt.text = "${getString(R.string.task_detail_created_at_label)} $createdStr"
        binding.tvUpdatedAt.text = "${getString(R.string.task_detail_updated_at_label)} $updatedStr"

        // Step 3C: Overdue Visual Indicator
        bindOverdueIndicator(task)

        // Complete button text
        binding.btnComplete.text = if (task.isCompleted) {
            getString(R.string.task_detail_button_uncomplete)
        } else {
            getString(R.string.task_detail_button_complete)
        }
    }

    // ========================================================================
    // Step 3B: Status Badge
    // ========================================================================

    private fun bindStatusBadge(status: TaskStatus, task: Task) {
        val (textResId, colorResId) = when {
            // Nếu task quá hạn và chưa hoàn thành → hiển thị OVERDUE
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

        // Tint background drawable
        val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_badge_status)?.mutate()
        bgDrawable?.setTint(ContextCompat.getColor(this, colorResId).let { color ->
            // Dùng màu nhạt hơn cho background (alpha 20%)
            android.graphics.Color.argb(
                51, // 20% alpha
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color)
            )
        })
        binding.tvStatusBadge.background = bgDrawable
    }

    // ========================================================================
    // Step 3B: Priority Badge
    // ========================================================================

    private fun bindPriorityBadge(priority: Priority) {
        val (textResId, colorResId) = when (priority) {
            Priority.LOW -> R.string.task_priority_low to R.color.priority_low
            Priority.MEDIUM -> R.string.task_priority_medium to R.color.priority_medium
            Priority.HIGH -> R.string.task_priority_high to R.color.priority_high
            Priority.URGENT -> R.string.task_priority_urgent to R.color.priority_urgent
        }

        binding.tvPriorityBadge.text = getString(textResId)
        binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(this, colorResId))

        // Tint background drawable
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

    // ========================================================================
    // Step 3B: Recurrence
    // ========================================================================

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

        // Hiển thị kèm interval nếu > 1
        binding.tvRecurrence.text = if (task.recurrenceInterval > 1) {
            "$recurrenceText (mỗi ${task.recurrenceInterval} lần)"
        } else {
            recurrenceText
        }
    }

    // ========================================================================
    // Step 3C: Overdue Visual Indicator
    // ========================================================================

    private fun bindOverdueIndicator(task: Task) {
        val isOverdue = DateTimeUtils.isOverdue(task.dueDate, task.isCompleted)

        if (isOverdue) {
            // Hiện banner cảnh báo
            binding.overdueBanner.visibility = View.VISIBLE

            // Đổi màu due date text thành đỏ
            binding.tvDueDateTime.setTextColor(ContextCompat.getColor(this, R.color.error))
        } else {
            // Ẩn banner
            binding.overdueBanner.visibility = View.GONE

            // Màu mặc định
            binding.tvDueDateTime.setTextColor(
                ContextCompat.getColor(this, R.color.on_surface)
            )
        }
    }
}
