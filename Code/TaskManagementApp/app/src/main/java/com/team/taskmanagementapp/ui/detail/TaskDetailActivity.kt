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
import com.team.taskmanagementapp.util.RecurrenceHelper
import com.team.taskmanagementapp.viewmodel.TaskViewModel
import com.team.taskmanagementapp.viewmodel.TaskViewModelFactory
import java.util.Calendar
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

        // Fragment Result for Delete confirmation
        supportFragmentManager.setFragmentResultListener(
            DeleteTaskDialogFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val task = currentTask ?: viewModel.selectedTask.value ?: return@setFragmentResultListener
            when (bundle.getInt(DeleteTaskDialogFragment.RESULT_DELETE_TYPE)) {
                DeleteTaskDialogFragment.DELETE_NORMAL -> {
                    viewModel.deleteTask(task)
                    finish()
                }
                DeleteTaskDialogFragment.DELETE_ONLY_THIS -> {
                    viewModel.deleteTask(task, deleteAllFuture = false)
                    finish()
                }
                DeleteTaskDialogFragment.DELETE_ALL -> {
                    viewModel.deleteTask(task, deleteAllFuture = true)
                    finish()
                }
            }
        }

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
        val baseRecurrenceText = RecurrenceHelper.getRecurrenceDisplayText(task.recurrenceType, this)
        binding.tvRecurrenceType.text = if (task.isPaused) {
            "$baseRecurrenceText (${getString(R.string.task_repeat_paused)})"
        } else {
            baseRecurrenceText
        }

        // Repeat End Info
        binding.tvRepeatEndInfo.text = when {
            task.repeatLimitCount > 0 -> {
                getString(R.string.task_repeat_end_summary_count, task.repeatLimitCount, task.currentOccurrence)
            }
            task.repeatEndDate > 0L -> {
                val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                getString(R.string.task_repeat_end_summary_date, format.format(java.util.Date(task.repeatEndDate)))
            }
            else -> {
                getString(R.string.task_repeat_end_summary_never)
            }
        }

        // Pause / Resume Button
        if (task.isPaused) {
            binding.btnTogglePause.text = getString(R.string.task_repeat_action_resume)
            binding.btnTogglePause.setIconResource(R.drawable.ic_check_circle)
        } else {
            binding.btnTogglePause.text = getString(R.string.task_repeat_action_pause)
            binding.btnTogglePause.setIconResource(R.drawable.ic_time)
        }
        binding.btnTogglePause.setOnClickListener {
            viewModel.toggleTaskPause(task)
        }

        val dayViews = listOf(
            binding.tvDayM,   // 0: Thứ 2 (Lẻ)
            binding.tvDayT,   // 1: Thứ 3 (Chẵn - đậm hơn)
            binding.tvDayW,   // 2: Thứ 4 (Lẻ)
            binding.tvDayTh,  // 3: Thứ 5 (Chẵn - đậm hơn)
            binding.tvDayF,   // 4: Thứ 6 (Lẻ)
            binding.tvDaySa,  // 5: Thứ 7 (Chẵn - đậm hơn)
            binding.tvDaySu   // 6: Chủ nhật (Lẻ)
        )

        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_day_circle_active)
        val oddBg = ContextCompat.getDrawable(this, R.drawable.bg_day_circle_odd)
        val evenBg = ContextCompat.getDrawable(this, R.drawable.bg_day_circle_even)

        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val whiteColor = Color.WHITE

        when (task.recurrenceType) {
            RecurrenceType.DAILY -> {
                dayViews.forEachIndexed { index, tv ->
                    val isEven = (index + 1) % 2 == 0
                    if (isEven) {
                        tv.background = evenBg
                        tv.setTextColor(primaryColor)
                    } else {
                        tv.background = oddBg
                        tv.setTextColor(whiteColor)
                    }
                }
            }
            RecurrenceType.WEEKLY -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                val dayOfWeekIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                dayViews.forEachIndexed { index, tv ->
                    val isSelectedDay = index == dayOfWeekIndex
                    val isEven = (index + 1) % 2 == 0

                    if (isSelectedDay) {
                        tv.background = activeBg
                        tv.setTextColor(primaryColor)
                    } else if (isEven) {
                        tv.background = evenBg
                        tv.setTextColor(primaryColor)
                    } else {
                        tv.background = oddBg
                        tv.setTextColor(whiteColor)
                    }
                }
            }
            else -> {
                dayViews.forEachIndexed { index, tv ->
                    val isEven = (index + 1) % 2 == 0
                    tv.background = if (isEven) evenBg else oddBg
                    tv.setTextColor(if (isEven) primaryColor else whiteColor)
                }
            }
        }
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

        if (supportFragmentManager.findFragmentByTag(DeleteTaskDialogFragment.TAG) != null) return

        val isRecurring = task.isRecurring && task.recurrenceType != RecurrenceType.NONE
        DeleteTaskDialogFragment.newInstance(isRecurring)
            .show(supportFragmentManager, DeleteTaskDialogFragment.TAG)
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
        MotivationQuote("“A journey of a thousand miles begins with a single step.”", "— Lao Tzu", R.drawable.img_quote_bg_1),
        MotivationQuote("“It’s not that I’m so smart, it’s just that I stay with problems longer.”", "— Albert Einstein", R.drawable.img_quote_bg_2),
        MotivationQuote("“Success is not final, failure is not fatal: it is the courage to continue that counts.”", "— Winston Churchill", R.drawable.img_quote_bg_3),
        MotivationQuote("“Do not fear going forward slowly, fear only to stand still.”", "— Chinese Proverb", R.drawable.img_quote_bg_4),
        MotivationQuote("“Perseverance is the key to opening every door of success.”", "— Thomas Edison", R.drawable.img_quote_bg_1),
        MotivationQuote("“The greatest difficulties are where the greatest strengths are forged.”", "— Philosophy", R.drawable.img_quote_bg_2),
        MotivationQuote("“You only truly fail when you decide to quit.”", "— Napoleon Hill", R.drawable.img_quote_bg_3),
        MotivationQuote("“Small habits don’t add up, they compound. Get 1% better every day.”", "— Atomic Habits", R.drawable.img_quote_bg_4),
        MotivationQuote("“Dripping water hollows out stone, not through force but through persistence.”", "— Ovid", R.drawable.img_quote_bg_1),
        MotivationQuote("“The sun always rises after the darkest night. Keep going!”", "— Daily Inspiration", R.drawable.img_quote_bg_2),
        MotivationQuote("“Discipline is the bridge between goals and accomplishment.”", "— Jim Rohn", R.drawable.img_quote_bg_3),
        MotivationQuote("“Quiet efforts today become brilliant triumphs tomorrow.”", "— Motivation", R.drawable.img_quote_bg_4),
        MotivationQuote("“A finisher is someone who completes what others only start.”", "— Success Insight", R.drawable.img_quote_bg_1),
        MotivationQuote("“Dreams do not work unless you do with daily dedication.”", "— Productivity Wisdom", R.drawable.img_quote_bg_2),
        MotivationQuote("“Gold is tested by fire, courage by adversity.”", "— Proverb", R.drawable.img_quote_bg_3),
        MotivationQuote("“To conquer oneself is the greatest victory.”", "— Wisdom", R.drawable.img_quote_bg_4)
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
