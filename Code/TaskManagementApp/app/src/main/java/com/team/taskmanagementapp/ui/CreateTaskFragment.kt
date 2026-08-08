package com.team.taskmanagementapp.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.databinding.FragmentCreateTaskBinding
import com.team.taskmanagementapp.util.ValidationHelper
import com.team.taskmanagementapp.util.ValidationHelper.ValidationError
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CreateTaskFragment displays a form to create a new task with validation.
 * Shows error states for required fields matching the design.
 */
class CreateTaskFragment : Fragment() {

    private lateinit var binding: FragmentCreateTaskBinding
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupValidation()
    }

    private fun setupUI() {
        // Due Date Picker
        binding.dueDateEditText.setOnClickListener {
            showDatePicker()
        }

        // Create Button
        binding.createButton.setOnClickListener {
            if (validateForm()) {
                // TODO: Create task logic
                findNavController().navigateUp()
            }
        }

        // Cancel Button
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateField()
                validateDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateField() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.dueDateEditText.setText(dateFormat.format(selectedDate.time))
    }

    private fun setupValidation() {
        binding.titleInputLayout.error = null
        binding.descriptionInputLayout.error = null
        binding.dueDateInputLayout.error = null
        binding.priorityErrorText.visibility = View.GONE
        binding.validationAlert.visibility = View.GONE

        binding.titleEditText.doAfterTextChanged { text ->
            showInputError(
                binding.titleInputLayout,
                ValidationHelper.validateTitle(text?.toString().orEmpty())
            )
            updateValidationAlert()
        }

        binding.descriptionEditText.doAfterTextChanged { text ->
            showInputError(
                binding.descriptionInputLayout,
                ValidationHelper.validateDescription(text?.toString().orEmpty())
            )
            updateValidationAlert()
        }

        binding.priorityToggleGroup.addOnButtonCheckedListener { _, _, _ ->
            showPriorityError(ValidationHelper.validatePriority(selectedPriority()))
            updateValidationAlert()
        }
    }

    private fun validateForm(): Boolean {
        val title = binding.titleEditText.text?.toString().orEmpty()
        val description = binding.descriptionEditText.text?.toString().orEmpty()
        val dueDateMillis = selectedDueDateMillis()
        val priority = selectedPriority()

        showInputError(binding.titleInputLayout, ValidationHelper.validateTitle(title))
        showInputError(
            binding.descriptionInputLayout,
            ValidationHelper.validateDescription(description)
        )
        validateDate()
        showPriorityError(ValidationHelper.validatePriority(priority))

        val isValid = ValidationHelper.validateAll(
            title = title,
            description = description,
            dueDateMillis = dueDateMillis,
            dueTimeMillis = null,
            priority = priority,
            isNewTask = true
        )
        binding.validationAlert.visibility = if (isValid) View.GONE else View.VISIBLE

        return isValid
    }

    private fun validateDate(): Boolean {
        val error = ValidationHelper.validateDueDate(
            dueDateMillis = selectedDueDateMillis(),
            isNewTask = true
        )
        showInputError(binding.dueDateInputLayout, error)
        updateValidationAlert()
        return error == null
    }

    private fun selectedDueDateMillis(): Long? =
        selectedDate.timeInMillis.takeIf { binding.dueDateEditText.text?.isNotBlank() == true }

    private fun selectedPriority(): Priority? =
        if (binding.priorityToggleGroup.checkedButtonId == View.NO_ID) null else Priority.MEDIUM

    private fun showInputError(layout: TextInputLayout, error: ValidationError?) {
        layout.error = validationMessage(error)
    }

    private fun showPriorityError(error: ValidationError?) {
        binding.priorityErrorText.text = validationMessage(error)
        binding.priorityErrorText.visibility = if (error == null) View.GONE else View.VISIBLE
    }

    private fun updateValidationAlert() {
        val hasError = binding.titleInputLayout.error != null ||
            binding.descriptionInputLayout.error != null ||
            binding.dueDateInputLayout.error != null ||
            binding.priorityErrorText.visibility == View.VISIBLE
        binding.validationAlert.visibility = if (hasError) View.VISIBLE else View.GONE
    }

    private fun validationMessage(error: ValidationError?): String? =
        error?.let { getString(it.messageRes) }
}
