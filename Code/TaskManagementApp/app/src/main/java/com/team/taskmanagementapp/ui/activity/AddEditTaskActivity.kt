package com.team.taskmanagementapp.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.enum.Priority
import com.team.taskmanagementapp.data.model.enum.RecurrenceType
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.ActivityAddEditTaskBinding
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.ui.viewmodel.AddEditTaskViewModel
import com.team.taskmanagementapp.ui.viewmodel.AddEditTaskViewModelFactory
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
    private var taskId: Long = -1L
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        isEditMode = taskId != -1L

        setupToolbar()
        setupDropdowns()
        setupDateTimePickers()
        setupClickListeners()
        observeViewModel()

        if (isEditMode) {
            viewModel.loadTask(taskId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) getString(R.string.edit_task) else getString(R.string.add_task)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.priority_array)
        )
        binding.priorityAutoComplete.setAdapter(priorityAdapter)
        binding.priorityAutoComplete.setText(priorityAdapter.getItem(1), false) // Default Medium

        val recurrenceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.recurrence_array)
        )
        binding.recurrenceAutoComplete.setAdapter(recurrenceAdapter)
        binding.recurrenceAutoComplete.setText(recurrenceAdapter.getItem(0), false) // Default None
    }

    private fun setupDateTimePickers() {
        updateDateLabel()
        updateTimeLabel()

        binding.dateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    updateDateLabel()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.timeEditText.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedTime.set(Calendar.MINUTE, minute)
                    updateTimeLabel()
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun updateDateLabel() {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.dateEditText.setText(format.format(selectedDate.time))
    }

    private fun updateTimeLabel() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.timeEditText.setText(format.format(selectedTime.time))
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { saveTask() }
    }

    private fun saveTask() {
        val title = binding.titleEditText.text.toString()
        val description = binding.descriptionEditText.text.toString()
        val priority = Priority.valueOf(binding.priorityAutoComplete.text.toString().uppercase())
        val recurrence = RecurrenceType.valueOf(binding.recurrenceAutoComplete.text.toString().uppercase())

        viewModel.saveTask(
            id = if (isEditMode) taskId.toInt() else 0,
            title = title,
            description = description,
            dueDate = selectedDate.timeInMillis,
            dueTime = selectedTime.timeInMillis,
            priority = priority,
            recurrenceType = recurrence,
            isEdit = isEditMode
        )
    }

    private fun observeViewModel() {
        viewModel.task.observe(this) { task ->
            task?.let {
                binding.titleEditText.setText(it.title)
                binding.descriptionEditText.setText(it.description)
                selectedDate.timeInMillis = it.dueDate
                selectedTime.timeInMillis = it.dueTime
                updateDateLabel()
                updateTimeLabel()

                val priorityStr = it.priority.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                binding.priorityAutoComplete.setText(priorityStr, false)

                val recurrenceStr = it.recurrenceType.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                binding.recurrenceAutoComplete.setText(recurrenceStr, false)
            }
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Show progress if needed
                }
                is UiState.Success -> {
                    val message = if (isEditMode) R.string.task_updated else R.string.task_saved
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is UiState.Error -> {
                    if (state.message == "Title is required") {
                        binding.titleInputLayout.error = getString(R.string.error_empty_title)
                    } else {
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
