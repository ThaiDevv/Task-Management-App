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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.repository.BackupUiState
import com.team.taskmanagementapp.databinding.FragmentDataManagementBinding
import com.team.taskmanagementapp.ui.viewmodel.BackupViewModel
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

    private val viewModel: BackupViewModel by viewModels()

    // SAF result launcher for export
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.exportTasks(uri)
            }
        }
    }

    // SAF result launcher for import
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.importTasks(uri)
            }
        }
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
        setupClickListeners()
        observeState()
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

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state from repository
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                // Observe task count
                launch {
                    viewModel.taskCount.collect { count ->
                        binding.taskCountText.text = getString(R.string.data_task_count, count)
                    }
                }

                // Observe last backup time
                launch {
                    viewModel.lastBackupTime.collect { lastBackup ->
                        if (lastBackup != null) {
                            binding.lastBackupText.text = getString(R.string.data_last_backup, lastBackup)
                            binding.lastBackupText.isVisible = true
                        } else {
                            binding.lastBackupText.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun handleUiState(state: BackupUiState) {
        when (state) {
            is BackupUiState.Idle -> {
                hideProgress()
            }
            is BackupUiState.Loading -> {
                showProgress(state.message)
            }
            is BackupUiState.Success -> {
                hideProgress()
                showSnackbar(state.message)
                viewModel.resetState()
            }
            is BackupUiState.Error -> {
                hideProgress()
                showSnackbar(getString(R.string.data_error, state.message))
                viewModel.resetState()
            }
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

    private fun showProgress(message: String) {
        binding.progressBar.isVisible = true
        binding.progressText.text = message
        binding.progressText.isVisible = true
        binding.exportDataButton.isEnabled = false
        binding.restoreDataButton.isEnabled = false
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
        binding.progressText.isVisible = false
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
