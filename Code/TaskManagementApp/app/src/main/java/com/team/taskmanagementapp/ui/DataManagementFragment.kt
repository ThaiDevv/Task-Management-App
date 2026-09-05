package com.team.taskmanagementapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.TaskApplication
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentDataManagementBinding
import com.team.taskmanagementapp.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data management fragment for backup and restore features.
 * Handles export/import of tasks using Storage Access Framework (SAF).
 */
class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var backupRepository: BackupRepository

    // SAF result launcher for export
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportToJson(uri)
            }
        }
        hideProgress()
    }

    // SAF result launcher for import
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromJson(uri)
            }
        }
        hideProgress()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize BackupRepository
        val taskApplication = requireActivity().application as TaskApplication
        val taskRepository = TaskRepository(taskApplication)
        backupRepository = BackupRepository(taskRepository, requireContext())

        setupClickListeners()
        showTaskCount()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.exportDataButton.setOnClickListener {
            launchExportFilePicker()
        }

        binding.restoreDataButton.setOnClickListener {
            launchImportFilePicker()
        }
    }

    private fun showTaskCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = backupRepository.getTaskCount()
            binding.taskCountText.text = getString(R.string.data_task_count, count)
        }
    }

    /**
     * Launch SAF file picker for export.
     */
    private fun launchExportFilePicker() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val fileName = "taskflow_backup_$timestamp.json"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        exportLauncher.launch(intent)
    }

    /**
     * Launch SAF file picker for import.
     */
    private fun launchImportFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importLauncher.launch(intent)
    }

    /**
     * Export tasks to JSON file.
     */
    private fun exportToJson(uri: android.net.Uri) {
        showProgress()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val count = backupRepository.exportToJson(uri)
                showSnackbar(getString(R.string.data_export_success, count))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.data_export_error, e.message ?: "Unknown error"))
            } finally {
                hideProgress()
            }
        }
    }

    /**
     * Import tasks from JSON file.
     */
    private fun importFromJson(uri: android.net.Uri) {
        showProgress()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val count = backupRepository.importFromJson(uri)
                showSnackbar(getString(R.string.data_import_success, count))
                showTaskCount() // Refresh count
            } catch (e: Exception) {
                showSnackbar(getString(R.string.data_import_error, e.message ?: "Unknown error"))
            } finally {
                hideProgress()
            }
        }
    }

    private fun showProgress() {
        binding.progressBar.isVisible = true
        binding.exportDataButton.isEnabled = false
        binding.restoreDataButton.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
        binding.exportDataButton.isEnabled = true
        binding.restoreDataButton.isEnabled = true
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
