package com.team.taskmanagementapp.ui.detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.graphics.drawable.GradientDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.ActivityTaskDetailBinding
import com.team.taskmanagementapp.ui.activity.AddEditTaskActivity
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
        val preferences = applicationContext.getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        TaskViewModelFactory(repository, applicationContext, preferences)
    }

    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

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

        // 5-second motivational quote rotator with background images
        setupMotivationQuoteRotator()
    }

    private fun bindTaskData(task: Task) {
        // Task Title & Description
        binding.tvTaskTitle.text = task.title
        binding.tvDescription.text = task.description.ifBlank {
            getString(R.string.task_detail_no_description)
        }

        // Due Date & Scheduled Time
        val dateStr = DateTimeUtils.formatTimestamp(task.dueDate, "MMM dd, yyyy")
        val timeStr = DateTimeUtils.formatTimestamp(task.dueTime, DateTimeUtils.FORMAT_TIME_ONLY)
        binding.tvDueDate.text = if (dateStr.isBlank()) "No Date" else dateStr
        binding.tvScheduledTime.text = if (timeStr.isBlank()) "No Time" else timeStr

        // Reminder
        binding.tvReminder.text = when (task.reminderMinutes) {
            0 -> "None"
            60 -> "1 hour before"
            else -> "${task.reminderMinutes} mins before"
        }

        // Status & Priority Badges
        bindStatusBadge(task.status, task)
        bindPriorityBadge(task.priority)

        // Recurrence Card & Day Selector
        bindRecurrence(task)

        // Complete Button state, text, and icons
        if (task.isCompleted) {
            // Completed → green button
            binding.btnComplete.text = getString(R.string.task_detail_button_uncomplete)
            binding.btnComplete.setIconResource(R.drawable.ic_time)
            binding.btnComplete.backgroundTintList =
                ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            binding.btnComplete.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            binding.btnComplete.iconTint =
                ContextCompat.getColorStateList(this, android.R.color.white)

            binding.fabComplete.setImageResource(R.drawable.ic_time)
            binding.fabComplete.backgroundTintList =
                ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            binding.fabComplete.contentDescription = getString(R.string.action_mark_incomplete)
        } else {
            // Not completed → primary_container button (matches Stitch spec)
            binding.btnComplete.text = getString(R.string.task_detail_button_complete)
            binding.btnComplete.setIconResource(R.drawable.ic_check_circle)
            binding.btnComplete.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary_container)
            binding.btnComplete.setTextColor(
                ContextCompat.getColor(this, R.color.on_primary_container)
            )
            binding.btnComplete.iconTint =
                ContextCompat.getColorStateList(this, R.color.on_primary_container)

            binding.fabComplete.setImageResource(R.drawable.ic_check_circle)
            binding.fabComplete.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
            binding.fabComplete.contentDescription = getString(R.string.action_mark_complete)
        }
    }

    private fun bindStatusBadge(status: TaskStatus, task: Task) {
        val (textResId, colorResId) = when {
            DateTimeUtils.isOverdue(task) -> {
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
        val color = ContextCompat.getColor(this, colorResId)
        binding.tvStatusBadge.setTextColor(color)
        binding.badgeStatus.background = createBadgeBackground(color)
        binding.ivStatusBadge.setColorFilter(color)
    }

    private fun bindPriorityBadge(priority: Priority) {
        val (textResId, colorResId) = when (priority) {
            Priority.LOW -> R.string.task_priority_low to R.color.priority_low
            Priority.MEDIUM -> R.string.task_priority_medium to R.color.priority_medium
            Priority.HIGH -> R.string.task_priority_high to R.color.priority_high
            Priority.URGENT -> R.string.task_priority_urgent to R.color.priority_urgent
        }

        binding.tvPriorityBadge.text = getString(textResId)
        val color = ContextCompat.getColor(this, colorResId)
        binding.tvPriorityBadge.setTextColor(color)
        binding.badgePriority.background = createBadgeBackground(color)
        binding.ivPriorityBadge.setColorFilter(color)
    }

    private fun createBadgeBackground(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.radius_full)
            setColor(Color.argb(51, Color.red(color), Color.green(color), Color.blue(color)))
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

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.task_detail_delete_confirm_title)
            .setMessage(R.string.task_detail_delete_confirm_msg)
            .setNegativeButton(R.string.task_detail_delete_confirm_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.task_detail_delete_confirm_positive) { _, _ ->
                viewModel.deleteTask(task)
                finish()
            }
            .show()
    }

    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener {
            currentTask?.let { task ->
                val intent = Intent(this, AddEditTaskActivity::class.java)
                intent.putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                startActivity(intent)
            }
        }
    }

    // ===== Motivation Quote Rotator (5-second random rotation) =====

    private data class MotivationQuote(
        val content: String,
        val author: String,
        val imageResId: Int
    )

    private val motivationQuotes = listOf(
        MotivationQuote("“Hành trình vạn dặm bắt đầu bằng một bước chân.”", "— Lão Tử", R.drawable.img_quote_bg_1),
        MotivationQuote("“Không phải tôi thông minh, tôi chỉ ở lại với vấn đề lâu hơn.”", "— Albert Einstein", R.drawable.img_quote_bg_2),
        MotivationQuote("“Thành công không phải cuối cùng, thất bại không phải tận cùng. Sức dũng cảm bước tiếp mới là tất cả.”", "— Winston Churchill", R.drawable.img_quote_bg_3),
        MotivationQuote("“Đừng sợ đi chậm, chỉ sợ đứng yên.”", "— Tục ngữ", R.drawable.img_quote_bg_4),
        MotivationQuote("“Sự kiên trì là chìa khóa mở mọi cánh cửa của thành công.”", "— Thomas Edison", R.drawable.img_quote_bg_1),
        MotivationQuote("“Những khó khăn lớn nhất luôn tôi luyện nên những con người mạnh mẽ nhất.”", "— Triết lý cuộc sống", R.drawable.img_quote_bg_2),
        MotivationQuote("“Bạn chỉ thật sự thất bại khi bạn quyết định từ bỏ.”", "— Napoleon Hill", R.drawable.img_quote_bg_3),
        MotivationQuote("“Mỗi ngày cố gắng thêm 1%, sau một năm bạn sẽ vượt trội gấp 37 lần.”", "— Atomic Habits", R.drawable.img_quote_bg_4),
        MotivationQuote("“Giọt nước chảy mãi cũng làm mòn đá cứng.”", "— Thành ngữ", R.drawable.img_quote_bg_1),
        MotivationQuote("“Mặt trời luôn mọc sau đêm tối. Hãy kiên trì bước tiếp!”", "— Cảm hứng mỗi ngày", R.drawable.img_quote_bg_2),
        MotivationQuote("“Kỷ luật là cầu nối giữa mục tiêu và thành tựu.”", "— Jim Rohn", R.drawable.img_quote_bg_3),
        MotivationQuote("“Nỗ lực âm thầm của hôm nay sẽ là ánh hào quang rực rỡ của ngày mai.”", "— Động lực sống", R.drawable.img_quote_bg_4),
        MotivationQuote("“Người kiên trì là người hoàn thành những gì người khác bắt đầu.”", "— Triết lý thành công", R.drawable.img_quote_bg_1),
        MotivationQuote("“Ước mơ không tự đến, nó đòi hỏi mồ hôi và sự kiên trì mỗi ngày.”", "— Quản lý công việc", R.drawable.img_quote_bg_2),
        MotivationQuote("“Lửa thử vàng, gian gian thử sức, khó khăn thử thách lòng kiên trì.”", "— Ca dao Việt Nam", R.drawable.img_quote_bg_3),
        MotivationQuote("“Chiến thắng bản thân là chiến thắng hiển hách nhất.”", "— Đạo Phật", R.drawable.img_quote_bg_4)
    )

    private fun setupMotivationQuoteRotator() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var currentIndex = (motivationQuotes.indices).random()
                while (true) {
                    val quote = motivationQuotes[currentIndex % motivationQuotes.size]

                    // Smooth cross-fade animation when switching quote & background image
                    binding.cardMotivation.animate()
                        .alpha(0.4f)
                        .setDuration(350)
                        .withEndAction {
                            binding.ivQuoteBg.setImageResource(quote.imageResId)
                            binding.tvQuoteContent.text = quote.content
                            binding.tvQuoteAuthor.text = quote.author

                            binding.cardMotivation.animate()
                                .alpha(1.0f)
                                .setDuration(350)
                                .start()
                        }
                        .start()

                    currentIndex++
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
    }
}
