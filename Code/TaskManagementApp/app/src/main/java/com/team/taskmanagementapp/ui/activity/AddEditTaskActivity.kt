package com.team.taskmanagementapp.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
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
import com.team.taskmanagementapp.util.ValidationHelper
import com.team.taskmanagementapp.util.ValidationHelper.ValidationError
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
            viewModel.loadTask(taskId)
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
        view.setTextColor(if (isSelected) ContextCompat.getColor(this, R.color.white) else ContextCompat.getColor(this, R.color.black))
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
        if (!validateAll()) return

        val title = binding.titleEditText.text.toString()
        val description = binding.descriptionEditText.text.toString()
        isSaving = true

        viewModel.saveTask(
            id = if (isEditMode) taskId.toInt() else 0,
            title = title,
            description = description,
            dueDate = selectedDate.timeInMillis,
            dueTime = selectedTime.timeInMillis,
            priority = selectedPriority,
            recurrenceType = selectedRecurrence,
            reminderMinutes = selectedReminderMinutes,
            status = selectedStatus,
            isEdit = isEditMode
        )
    }

    private fun observeViewModel() {
        viewModel.task.observe(this) { task ->
            task?.let { populateTaskData(it) }
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    if (isSaving) {
                        Toast.makeText(this, if (isEditMode) "Task Updated" else "Task Created", Toast.LENGTH_SHORT).show()
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
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
