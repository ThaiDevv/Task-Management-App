package com.team.taskmanagementapp.ui.activity

import android.Manifest
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
import com.team.taskmanagementapp.ui.activity.dialog.DatePickerDialogFragment
import com.team.taskmanagementapp.ui.activity.dialog.SingleChoiceDialogFragment
import com.team.taskmanagementapp.ui.activity.dialog.TimePickerDialogFragment
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

    private val reminderOptions = arrayOf(
        "None",
        "5 minutes before",
        "10 minutes before",
        "15 minutes before",
        "30 minutes before",
        "1 hour before"
    )
    private val reminderValues = intArrayOf(0, 5, 10, 15, 30, 60)

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
        registerDialogResultListeners()

        if (savedInstanceState != null) {
            // Restore form state sau configuration change để không mất dữ liệu user đang nhập/chọn.
            restoreFormState(savedInstanceState)
        } else if (!isEditMode) {
            val initialDueDate = intent.getLongExtra(Constants.EXTRA_TASK_DUE_DATE, -1L)
            if (initialDueDate != -1L) {
                selectedDate.timeInMillis = initialDueDate
                updateDateLabel()
            }
        }

        // Ở edit mode luôn observe; dữ liệu DB chỉ populate form lần đầu (guard isFormPopulated),
        // không ghi đè dữ liệu user đã restore sau recreation.
        if (isEditMode) {
            observeTask(taskId)
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
            val options = TaskStatus.values()
                .map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                .toTypedArray()
            SingleChoiceDialogFragment.newInstance(
                "Select Initial Status",
                options,
                REQUEST_KEY_STATUS
            ).show(supportFragmentManager, TAG_STATUS_DIALOG)
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
            SingleChoiceDialogFragment.newInstance(
                "Select Reminder",
                reminderOptions,
                REQUEST_KEY_REMINDER
            ).show(supportFragmentManager, TAG_REMINDER_DIALOG)
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.createTaskButton.setOnClickListener { saveTask() }

        binding.datePickerContainer.setOnClickListener {
            DatePickerDialogFragment.newInstance(selectedDate.timeInMillis)
                .show(supportFragmentManager, TAG_DATE_DIALOG)
        }

        binding.timePickerContainer.setOnClickListener {
            TimePickerDialogFragment.newInstance(selectedTime.timeInMillis)
                .show(supportFragmentManager, TAG_TIME_DIALOG)
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
        val isRecurringUpdate = existingTask.isRecurring || existingTask.recurrenceType != RecurrenceType.NONE
        executeTaskUpdate(existingTask, title, description, updateAllFuture = isRecurringUpdate)
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

                val updatedTask = viewModel.updateTask(
                    existingTask = existingTask,
                    editedTask = editedTask,
                    context = applicationContext
                )

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

    // ── Configuration change: save/restore form state ─────────────────────────

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_SELECTED_DATE, selectedDate.timeInMillis)
        outState.putLong(KEY_SELECTED_TIME, selectedTime.timeInMillis)
        outState.putString(KEY_SELECTED_PRIORITY, selectedPriority.name)
        outState.putString(KEY_SELECTED_RECURRENCE, selectedRecurrence.name)
        outState.putInt(KEY_SELECTED_REMINDER_MINUTES, selectedReminderMinutes)
        outState.putString(KEY_SELECTED_STATUS, selectedStatus.name)
        outState.putString(KEY_TITLE, binding.titleEditText.text?.toString().orEmpty())
        outState.putString(KEY_DESCRIPTION, binding.descriptionEditText.text?.toString().orEmpty())
        outState.putBoolean(KEY_IS_FORM_POPULATED, isFormPopulated)
        outState.putBoolean(KEY_RETRY_SAVE_AFTER_EXACT_ALARM, retrySaveAfterExactAlarmPermission)
        outState.putBoolean(KEY_IS_SAVING, isSaving)
    }

    private fun restoreFormState(savedInstanceState: Bundle) {
        selectedDate.timeInMillis =
            savedInstanceState.getLong(KEY_SELECTED_DATE, selectedDate.timeInMillis)
        selectedTime.timeInMillis =
            savedInstanceState.getLong(KEY_SELECTED_TIME, selectedTime.timeInMillis)
        selectedPriority = savedInstanceState.getString(KEY_SELECTED_PRIORITY)
            ?.let { runCatching { Priority.valueOf(it) }.getOrNull() } ?: Priority.MEDIUM
        selectedRecurrence = savedInstanceState.getString(KEY_SELECTED_RECURRENCE)
            ?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() } ?: RecurrenceType.NONE
        selectedReminderMinutes = savedInstanceState.getInt(
            KEY_SELECTED_REMINDER_MINUTES, selectedReminderMinutes
        )
        selectedStatus = savedInstanceState.getString(KEY_SELECTED_STATUS)
            ?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.TODO
        isFormPopulated = savedInstanceState.getBoolean(KEY_IS_FORM_POPULATED, false)
        retrySaveAfterExactAlarmPermission =
            savedInstanceState.getBoolean(KEY_RETRY_SAVE_AFTER_EXACT_ALARM, false)
        isSaving = savedInstanceState.getBoolean(KEY_IS_SAVING, false)

        binding.titleEditText.setText(savedInstanceState.getString(KEY_TITLE).orEmpty())
        binding.descriptionEditText.setText(savedInstanceState.getString(KEY_DESCRIPTION).orEmpty())

        updateDateLabel()
        updateTimeLabel()
        updatePriorityUI(selectedPriority)
        updateRecurrenceUI(selectedRecurrence)
        updateReminderLabel()
        updateStatusLabel()
    }

    // ── Dialog result listeners (auto re-register sau recreation) ─────────────

    private fun registerDialogResultListeners() {
        supportFragmentManager.setFragmentResultListener(
            DatePickerDialogFragment.REQUEST_KEY, this
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerDialogFragment.ARG_YEAR)
            val month = bundle.getInt(DatePickerDialogFragment.ARG_MONTH)
            val day = bundle.getInt(DatePickerDialogFragment.ARG_DAY)
            selectedDate.set(year, month, day)
            updateDateLabel()
            validateDateField()
            validateTimeField()
        }

        supportFragmentManager.setFragmentResultListener(
            TimePickerDialogFragment.REQUEST_KEY, this
        ) { _, bundle ->
            val hour = bundle.getInt(TimePickerDialogFragment.ARG_HOUR)
            val minute = bundle.getInt(TimePickerDialogFragment.ARG_MINUTE)
            selectedTime.set(Calendar.HOUR_OF_DAY, hour)
            selectedTime.set(Calendar.MINUTE, minute)
            updateTimeLabel()
            validateTimeField()
        }

        supportFragmentManager.setFragmentResultListener(
            REQUEST_KEY_STATUS, this
        ) { _, bundle ->
            val which = bundle.getInt(SingleChoiceDialogFragment.ARG_SELECTED_INDEX)
            selectedStatus = TaskStatus.values()[which]
            updateStatusLabel()
        }

        supportFragmentManager.setFragmentResultListener(
            REQUEST_KEY_REMINDER, this
        ) { _, bundle ->
            val which = bundle.getInt(SingleChoiceDialogFragment.ARG_SELECTED_INDEX)
            selectedReminderMinutes = reminderValues[which]
            updateReminderLabel()
        }
    }

    private fun updateReminderLabel() {
        binding.reminderText.text = when (selectedReminderMinutes) {
            0 -> "None"
            60 -> "1 hour before"
            else -> "$selectedReminderMinutes minutes before"
        }
    }

    private fun updateStatusLabel() {
        binding.statusText.text = selectedStatus.name.lowercase().replaceFirstChar { it.uppercase() }
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
        updateReminderLabel()
        selectedStatus = task.status
        updateStatusLabel()
    }

    companion object {
        const val EXTRA_TASK_ID = Constants.EXTRA_TASK_ID

        // Saved instance state keys
        private const val KEY_TITLE = "key_title"
        private const val KEY_DESCRIPTION = "key_description"
        private const val KEY_SELECTED_DATE = "key_selected_date"
        private const val KEY_SELECTED_TIME = "key_selected_time"
        private const val KEY_SELECTED_PRIORITY = "key_selected_priority"
        private const val KEY_SELECTED_RECURRENCE = "key_selected_recurrence"
        private const val KEY_SELECTED_REMINDER_MINUTES = "key_selected_reminder_minutes"
        private const val KEY_SELECTED_STATUS = "key_selected_status"
        private const val KEY_IS_FORM_POPULATED = "key_is_form_populated"
        private const val KEY_RETRY_SAVE_AFTER_EXACT_ALARM = "key_retry_save_after_exact_alarm"
        private const val KEY_IS_SAVING = "key_is_saving"

        // FragmentResult request keys
        private const val REQUEST_KEY_STATUS = "status_choice_result"
        private const val REQUEST_KEY_REMINDER = "reminder_choice_result"

        // Dialog tags
        private const val TAG_DATE_DIALOG = "date_picker_dialog"
        private const val TAG_TIME_DIALOG = "time_picker_dialog"
        private const val TAG_STATUS_DIALOG = "status_dialog"
        private const val TAG_REMINDER_DIALOG = "reminder_dialog"
    }
}
