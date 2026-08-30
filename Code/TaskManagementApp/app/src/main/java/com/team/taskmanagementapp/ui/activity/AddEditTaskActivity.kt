package com.team.taskmanagementapp.ui.activity

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar

import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.ActivityAddEditTaskBinding
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.ui.viewmodel.AddEditTaskViewModel
import com.team.taskmanagementapp.ui.viewmodel.AddEditTaskViewModelFactory
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.Constants
import com.team.taskmanagementapp.util.DateTimeUtils
import com.team.taskmanagementapp.util.NotificationPermissionManager
import com.team.taskmanagementapp.util.ValidationHelper
import com.team.taskmanagementapp.util.ValidationHelper.ValidationError
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private val viewModel: AddEditTaskViewModel by viewModels {
        val database = AppDatabase.getInstance(this)
        AddEditTaskViewModelFactory(TaskRepository(database.taskDao()))
    }

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()
    private var selectedPriority: Priority = Priority.MEDIUM
    private var selectedRecurrence: RecurrenceType = RecurrenceType.NONE
    private var selectedReminderMinutes: Int = 30
    private var selectedStatus: TaskStatus = TaskStatus.TODO
    private var taskId: Long = -1L
    private var isEditMode = false
    private var isSaving = false
    private var loadedTask: Task? = null
    private var isFormPopulated = false
    private var retrySaveAfterExactAlarmPermission = false

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!retrySaveAfterExactAlarmPermission) return@registerForActivityResult

        retrySaveAfterExactAlarmPermission = false
        if (AlarmScheduler.canScheduleExactAlarms(this)) {
            saveTask()
        } else {
            Toast.makeText(
                this,
                R.string.exact_alarm_permission_required,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // TASK-39: Permission launcher for re-checking after user returns from Settings
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                performSave()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        isEditMode = taskId != -1L

        setupUI()
        setupValidation()
        setupClickListeners()
        observeViewModel()

        if (isEditMode) {
            observeTask(taskId)
        } else {
            val initialDueDate = intent.getLongExtra(Constants.EXTRA_TASK_DUE_DATE, -1L)
            if (initialDueDate != -1L) {
                selectedDate.timeInMillis = initialDueDate
                updateDateLabel()
            }
        }
    }

    private fun setupUI() {
        binding.headerTitle.text = if (isEditMode) "Edit Task" else "Create New Task"
        binding.createTaskButton.text = if (isEditMode) "Update Task" else "Create Task"

        updateDateLabel()
        updateTimeLabel()
        setupPrioritySelection()
        setupRecurrenceSelection()
        setupStatusSelection()
        setupReminderSelection()
    }

    private fun setupStatusSelection() {
        binding.statusContainer.setOnClickListener {
            val options = TaskStatus.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Initial Status")
                .setItems(options) { _, which ->
                    selectedStatus = TaskStatus.values()[which]
                    binding.statusText.text = options[which]
                }
                .show()
        }
    }

    private fun setupPrioritySelection() {
        val priorities = mapOf(
            binding.priorityLow to Priority.LOW,
            binding.priorityMedium to Priority.MEDIUM,
            binding.priorityHigh to Priority.HIGH
        )

        priorities.forEach { (view, priority) ->
            view.setOnClickListener {
                updatePriorityUI(priority)
            }
        }
        updatePriorityUI(selectedPriority)
    }

    private fun updatePriorityUI(priority: Priority) {
        selectedPriority = priority
        binding.priorityLow.isSelected = priority == Priority.LOW
        binding.priorityMedium.isSelected = priority == Priority.MEDIUM
        binding.priorityHigh.isSelected = priority == Priority.HIGH

        // Update text colors
        updateSelectionTextColor(binding.priorityLow, priority == Priority.LOW)
        updateSelectionTextColor(binding.priorityMedium, priority == Priority.MEDIUM)
        updateSelectionTextColor(binding.priorityHigh, priority == Priority.HIGH)
        showInlineError(binding.priorityErrorText, ValidationHelper.validatePriority(priority))
    }

    private fun setupRecurrenceSelection() {
        val recurrences = mapOf(
            binding.repeatNone to RecurrenceType.NONE,
            binding.repeatDaily to RecurrenceType.DAILY,
            binding.repeatWeekly to RecurrenceType.WEEKLY,
            binding.repeatMonthly to RecurrenceType.MONTHLY
        )

        recurrences.forEach { (view, recurrence) ->
            view.setOnClickListener {
                updateRecurrenceUI(recurrence)
            }
        }
        updateRecurrenceUI(selectedRecurrence)
    }

    private fun updateRecurrenceUI(recurrence: RecurrenceType) {
        selectedRecurrence = recurrence
        binding.repeatNone.isSelected = recurrence == RecurrenceType.NONE
        binding.repeatDaily.isSelected = recurrence == RecurrenceType.DAILY
        binding.repeatWeekly.isSelected = recurrence == RecurrenceType.WEEKLY
        binding.repeatMonthly.isSelected = recurrence == RecurrenceType.MONTHLY

        updateSelectionTextColor(binding.repeatNone, recurrence == RecurrenceType.NONE)
        updateSelectionTextColor(binding.repeatDaily, recurrence == RecurrenceType.DAILY)
        updateSelectionTextColor(binding.repeatWeekly, recurrence == RecurrenceType.WEEKLY)
        updateSelectionTextColor(binding.repeatMonthly, recurrence == RecurrenceType.MONTHLY)
    }

    private fun updateSelectionTextColor(view: TextView, isSelected: Boolean) {
        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (isSelected) R.color.white else R.color.on_surface
            )
        )
    }

    private fun setupReminderSelection() {
        binding.reminderContainer.setOnClickListener {
            val options = arrayOf("None", "5 minutes before", "10 minutes before", "15 minutes before", "30 minutes before", "1 hour before")
            val values = intArrayOf(0, 5, 10, 15, 30, 60)

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Reminder")
                .setItems(options) { _, which ->
                    selectedReminderMinutes = values[which]
                    binding.reminderText.text = options[which]
                }
                .show()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.createTaskButton.setOnClickListener { saveTask() }

        binding.datePickerContainer.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                updateDateLabel()
                validateDateField()
                validateTimeField()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.timePickerContainer.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                selectedTime.set(Calendar.HOUR_OF_DAY, h)
                selectedTime.set(Calendar.MINUTE, min)
                updateTimeLabel()
                validateTimeField()
            }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), false).show()
        }
    }

    private fun updateDateLabel() {
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.dateText.text = format.format(selectedDate.time)
    }

    private fun updateTimeLabel() {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.timeText.text = format.format(selectedTime.time)
    }

    private fun setupValidation() {
        binding.titleEditText.doAfterTextChanged { text ->
            binding.titleInputLayout.error = validationMessage(
                ValidationHelper.validateTitle(text?.toString().orEmpty())
            )
        }

        binding.descriptionEditText.doAfterTextChanged { text ->
            binding.descriptionInputLayout.error = validationMessage(
                ValidationHelper.validateDescription(text?.toString().orEmpty())
            )
        }
    }

    private fun validateAll(): Boolean {
        val title = binding.titleEditText.text?.toString().orEmpty()
        val description = binding.descriptionEditText.text?.toString().orEmpty()
        val dueDate = selectedDate.timeInMillis
        val dueTime = selectedTime.timeInMillis

        binding.titleInputLayout.error = validationMessage(ValidationHelper.validateTitle(title))
        binding.descriptionInputLayout.error = validationMessage(
            ValidationHelper.validateDescription(description)
        )
        validateDateField()
        validateTimeField()
        showInlineError(
            binding.priorityErrorText,
            ValidationHelper.validatePriority(selectedPriority)
        )

        return ValidationHelper.validateAll(
            title = title,
            description = description,
            dueDateMillis = dueDate,
            dueTimeMillis = dueTime,
            priority = selectedPriority,
            isNewTask = !isEditMode
        )
    }

    private fun validateDateField() {
        showInlineError(
            binding.dateErrorText,
            ValidationHelper.validateDueDate(
                dueDateMillis = selectedDate.timeInMillis,
                isNewTask = !isEditMode
            )
        )
    }

    private fun validateTimeField() {
        showInlineError(
            binding.timeErrorText,
            ValidationHelper.validateDueTime(
                dueDateMillis = selectedDate.timeInMillis,
                dueTimeMillis = selectedTime.timeInMillis
            )
        )
    }

    private fun showInlineError(view: TextView, error: ValidationError?) {
        view.text = validationMessage(error)
        view.visibility = if (error == null) View.GONE else View.VISIBLE
    }

    private fun validationMessage(error: ValidationError?): String? =
        error?.let { getString(it.messageRes) }

    private fun saveTask() {
        if (!validateAll()) {
            Toast.makeText(this, getString(R.string.validation_form_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        // TASK-39: If user set a reminder and notification permission is missing, warn them
        if (selectedReminderMinutes > 0 && !NotificationPermissionManager.isGranted(this)) {
            showReminderPermissionBlockedDialog()
            return
        }

        performSave()
    }

    /**
     * TASK-39: Shows a dialog warning the user that saving with a reminder requires notification permission.
     * Offers two options:
     * - "Mở Cài đặt": Go to system Settings to enable permission, then come back.
     * - "Lưu bỏ qua": Save the task anyway (reminder won't fire).
     */
    private fun showReminderPermissionBlockedDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.notif_permission_reminder_blocked_title))
            .setMessage(getString(R.string.notif_permission_reminder_blocked_message))
            .setPositiveButton(getString(R.string.notif_permission_reminder_blocked_settings)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    NotificationPermissionManager.showRationaleDialog(this, notificationPermissionLauncher)
                } else {
                    NotificationPermissionManager.showSettingsRedirectDialog(this)
                }
            }
            .setNegativeButton(getString(R.string.notif_permission_reminder_blocked_save)) { _, _ ->
                performSave()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Executes the actual save / update logic after all permission and validation checks pass.
     */
    private fun performSave() {
        if (!ensureExactAlarmPermission()) return
        val title = binding.titleEditText.text.toString()
        val description = binding.descriptionEditText.text.toString()
        if (isEditMode) {
            val task = loadedTask
            if (task == null) {
                Toast.makeText(this, "Task is still loading", Toast.LENGTH_SHORT).show()
                return
            }

            updateExistingTask(task, title, description)
        } else {
            isSaving = true
            viewModel.saveTask(
                title = title,
                description = description,
                dueDate = getNormalizedDueDate(),
                dueTime = getNormalizedDueTime(),
                priority = selectedPriority,
                recurrenceType = selectedRecurrence,
                reminderMinutes = selectedReminderMinutes,
                status = selectedStatus,
                isEdit = false
            )
        }
    }

    private fun getNormalizedDueDate(): Long {
        return Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getNormalizedDueTime(): Long {
        val dateCal = Calendar.getInstance().apply { timeInMillis = selectedDate.timeInMillis }
        val timeCal = Calendar.getInstance().apply { timeInMillis = selectedTime.timeInMillis }
        return Calendar.getInstance().apply {
            clear()
            set(
                dateCal.get(Calendar.YEAR),
                dateCal.get(Calendar.MONTH),
                dateCal.get(Calendar.DAY_OF_MONTH),
                timeCal.get(Calendar.HOUR_OF_DAY),
                timeCal.get(Calendar.MINUTE),
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    if (isSaving) {
                        AlarmScheduler.scheduleAlarm(this, state.data)
                        val message = if (isEditMode) "Task updated successfully" else "Task created successfully"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                is UiState.Error -> {
                    isSaving = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun observeTask(taskId: Long) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeTask(taskId).collect { task ->
                    if (task == null) {
                        if (!isFormPopulated) {
                            Toast.makeText(this@AddEditTaskActivity, "Task not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@collect
                    }

                    loadedTask = task
                    if (!isFormPopulated) {
                        populateTaskData(task)
                        isFormPopulated = true
                    }
                }
            }
        }
    }

    private fun updateExistingTask(
        existingTask: Task,
        title: String,
        description: String
    ) {
        executeTaskUpdate(existingTask, title, description, updateAllFuture = false)
    }

    private fun executeTaskUpdate(
        existingTask: Task,
        title: String,
        description: String,
        updateAllFuture: Boolean
    ) {
        isSaving = true
        lifecycleScope.launch {
            try {
                val normalizedDueDate = getNormalizedDueDate()
                val normalizedDueTime = getNormalizedDueTime()
                val combined = DateTimeUtils.getCombinedDueTimestamp(normalizedDueDate, normalizedDueTime)
                val resolvedStatus = viewModel.resolveStatusOnUpdate(
                    currentStatus = existingTask.status,
                    newStatus = selectedStatus,
                    combinedDueTimestamp = combined
                )

                val editedTask = existingTask.copy(
                    title = title,
                    description = description,
                    dueDate = normalizedDueDate,
                    dueTime = normalizedDueTime,
                    priority = selectedPriority,
                    isRecurring = selectedRecurrence != RecurrenceType.NONE,
                    recurrenceType = selectedRecurrence,
                    reminderMinutes = selectedReminderMinutes,
                    status = resolvedStatus
                )

                val updatedTask = if (updateAllFuture) {
                    viewModel.updateFutureRecurringTasks(
                        originalTitle = existingTask.title,
                        originalRecurrence = existingTask.recurrenceType,
                        startDate = existingTask.dueDate,
                        editedTask = editedTask
                    )
                } else {
                    viewModel.updateTask(editedTask)
                }

                AlarmScheduler.rescheduleAlarm(this@AddEditTaskActivity, updatedTask)

                if (hasDueDateOrTimeChanged(existingTask, updatedTask)) {
                    dispatchTaskDateTimeChanged(updatedTask)
                }

                Toast.makeText(this@AddEditTaskActivity, "Task Updated", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (error: Exception) {
                isSaving = false
                Toast.makeText(
                    this@AddEditTaskActivity,
                    error.message ?: "Failed to update task",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun hasDueDateOrTimeChanged(oldTask: Task, newTask: Task): Boolean =
        oldTask.dueDate != newTask.dueDate || oldTask.dueTime != newTask.dueTime

    private fun ensureExactAlarmPermission(): Boolean {
        if (
            selectedReminderMinutes <= 0 ||
            AlarmScheduler.canScheduleExactAlarms(this)
        ) {
            return true
        }

        retrySaveAfterExactAlarmPermission = true
        exactAlarmPermissionLauncher.launch(
            AlarmScheduler.exactAlarmPermissionIntent(this)
        )
        return false
    }

    private fun dispatchTaskDateTimeChanged(task: Task) {
        sendBroadcast(
            Intent(Constants.ACTION_TASK_DATE_TIME_CHANGED).apply {
                setPackage(packageName)
                putExtra(Constants.EXTRA_TASK_ID, task.id.toLong())
                putExtra(Constants.EXTRA_TASK_DUE_DATE, task.dueDate)
                putExtra(Constants.EXTRA_TASK_DUE_TIME, task.dueTime)
            }
        )
    }

    private fun populateTaskData(task: Task) {
        binding.titleEditText.setText(task.title)
        binding.descriptionEditText.setText(task.description)
        selectedDate.timeInMillis = task.dueDate
        selectedTime.timeInMillis = task.dueTime
        updateDateLabel()
        updateTimeLabel()
        updatePriorityUI(task.priority)
        updateRecurrenceUI(task.recurrenceType)
        selectedReminderMinutes = task.reminderMinutes
        binding.reminderText.text = when(task.reminderMinutes) {
            0 -> "None"
            60 -> "1 hour before"
            else -> "${task.reminderMinutes} minutes before"
        }
        selectedStatus = task.status
        binding.statusText.text = task.status.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        const val EXTRA_TASK_ID = Constants.EXTRA_TASK_ID
    }
}
