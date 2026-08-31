package com.team.taskmanagementapp.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.model.ConflictAction
import com.team.taskmanagementapp.data.repository.BackupRepository
import com.team.taskmanagementapp.databinding.ActivityImportBinding
import com.team.taskmanagementapp.ui.base.BaseActivity
import com.team.taskmanagementapp.ui.base.UiState
import com.team.taskmanagementapp.viewmodel.ImportViewModel
import com.team.taskmanagementapp.viewmodel.ImportViewModelFactory
import kotlinx.coroutines.launch

/**
 * Activity to handle JSON file import with SAF (Storage Access Framework)
 * Features:
 * - Launch SAF Intent to pick JSON file
 * - Show progress during import
 * - Display conflicts and allow conflict resolution (SKIP, REPLACE, REPLACE_ALL)
 * - Show success/error messages
 */
class ImportActivity : BaseActivity() {

    private lateinit var binding: ActivityImportBinding
    private lateinit var viewModel: ImportViewModel

    private var selectedFileUri: Uri? = null
    private var pendingConflictAction: ConflictAction? = null

    // SAF Launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            // Persist permission
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onFileSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        setupUI()
        observeViewModel()
    }

    /**
     * Initialize ViewModel with Factory
     */
    private fun initViewModel() {
        val database = AppDatabase.getInstance(this)
        val taskDao = database.taskDao()
        val backupRepository = BackupRepository(this, taskDao)
        val factory = ImportViewModelFactory(backupRepository)
        viewModel = ViewModelProvider(this, factory)[ImportViewModel::class.java]
    }

    /**
     * Setup UI components and listeners
     */
    private fun setupUI() {
        // Back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Import button → Launch SAF
        binding.btnImportFile.setOnClickListener {
            launchFilePicker()
        }

        // Skip conflicts button
        binding.btnSkipConflicts.setOnClickListener {
            selectedFileUri?.let {
                viewModel.importFromJson(it, ConflictAction.SKIP)
            }
        }

        // Replace all conflicts button
        binding.btnReplaceAll.setOnClickListener {
            selectedFileUri?.let {
                viewModel.importFromJson(it, ConflictAction.REPLACE_ALL)
            }
        }

        // Retry button
        binding.btnRetry.setOnClickListener {
            viewModel.resetState()
            binding.containerConflicts.visibility = View.GONE
            binding.containerResult.visibility = View.GONE
            binding.containerMain.visibility = View.VISIBLE
        }
    }

    /**
     * Observe ViewModel data changes
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            // Observe import state
            viewModel.importState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvStatus.text = "⏳ Đang import..."
                        binding.containerResult.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val result = state.data
                        showSuccessResult(result)
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        showErrorResult(state.message)
                    }
                    is UiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            // Observe user messages
            viewModel.userMessage.collect { message ->
                Toast.makeText(this@ImportActivity, message, Toast.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            // Observe import complete event → navigate back
            viewModel.importComplete.collect { isComplete ->
                if (isComplete) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    /**
     * Launch SAF file picker for JSON files
     */
    private fun launchFilePicker() {
        filePickerLauncher.launch(arrayOf("application/json"))
    }

    /**
     * Called when file is selected
     */
    private fun onFileSelected(uri: Uri) {
        // Try to read filename from URI
        val fileName = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                cursor.getString(cursor.getColumnIndexOrThrow("_display_name"))
            } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        binding.tvSelectedFile.text = "📄 $fileName"
        binding.containerConflicts.visibility = View.GONE
        binding.containerResult.visibility = View.GONE

        // Show conflict resolution options
        showConflictDialog()
    }

    /**
     * Show dialog for conflict resolution
     */
    private fun showConflictDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xử lý Task Trùng")
            .setMessage(
                "Nếu tìm thấy task cùng title/date, hãy chọn:\n" +
                        "• SKIP: Bỏ qua task trùng\n" +
                        "• REPLACE: Xóa task cũ, import mới\n" +
                        "• REPLACE ALL: Xóa tất cả, import toàn bộ"
            )
            .setPositiveButton("SKIP") { _, _ ->
                selectedFileUri?.let {
                    viewModel.importFromJson(it, ConflictAction.SKIP)
                }
            }
            .setNegativeButton("REPLACE") { _, _ ->
                selectedFileUri?.let {
                    viewModel.importFromJson(it, ConflictAction.REPLACE)
                }
            }
            .setNeutralButton("REPLACE ALL") { _, _ ->
                selectedFileUri?.let {
                    viewModel.importFromJson(it, ConflictAction.REPLACE_ALL)
                }
            }
            .show()
    }

    /**
     * Show success result
     */
    private fun showSuccessResult(result: com.team.taskmanagementapp.data.model.ImportResult) {
        binding.containerMain.visibility = View.GONE
        binding.containerResult.visibility = View.VISIBLE
        binding.tvResultTitle.text = "✅ Import Thành Công"
        binding.tvResultMessage.text = buildString {
            append("${result.successCount} tasks imported\n")
            if (result.skipCount > 0) {
                append("${result.skipCount} tasks skipped\n")
            }
            if (result.failureCount > 0) {
                append("${result.failureCount} tasks invalid\n")
            }
        }
        binding.tvResultMessage.setTextColor(getColor(android.R.color.holo_green_dark))
    }

    /**
     * Show error result
     */
    private fun showErrorResult(errorMessage: String) {
        binding.containerMain.visibility = View.GONE
        binding.containerResult.visibility = View.VISIBLE
        binding.tvResultTitle.text = "❌ Import Thất Bại"
        binding.tvResultMessage.text = errorMessage
        binding.tvResultMessage.setTextColor(getColor(android.R.color.holo_red_dark))
    }
}
