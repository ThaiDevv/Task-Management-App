package com.team.taskmanagementapp.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.team.taskmanagementapp.databinding.FragmentCreateTaskBinding
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
        // Show error states by default (matching design)
        binding.titleInputLayout.error = "Title is required"
        binding.dueDateInputLayout.error = "Date cannot be in the past"
        binding.validationAlert.visibility = View.VISIBLE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate title
        val title = binding.titleEditText.text.toString().trim()
        if (title.isEmpty()) {
            binding.titleInputLayout.error = "Title is required"
            isValid = false
        } else {
            binding.titleInputLayout.error = null
        }

        // Validate date
        if (!validateDate()) {
            isValid = false
        }

        // Update validation alert visibility
        binding.validationAlert.visibility = if (isValid) View.GONE else View.VISIBLE

        return isValid
    }

    private fun validateDate(): Boolean {
        val dateText = binding.dueDateEditText.text.toString()
        if (dateText.isEmpty()) {
            binding.dueDateInputLayout.error = "Date is required"
            return false
        }

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        if (selectedDate.before(today)) {
            binding.dueDateInputLayout.error = "Date cannot be in the past"
            return false
        }

        binding.dueDateInputLayout.error = null
        return true
    }
}
