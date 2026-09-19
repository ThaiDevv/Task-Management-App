package com.team.taskmanagementapp.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.repository.BackupHistoryItem
import com.team.taskmanagementapp.data.repository.BackupUiState
import com.team.taskmanagementapp.databinding.FragmentDataManagementBinding
import com.team.taskmanagementapp.ui.viewmodel.BackupViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TASK-26 / TASK-50 / TASK-54 / Task 16 — Backup & Restore screen.
 *
 * Export path  : exportLauncher → SAF CreateDocument → BackupViewModel.exportBackup(uri)
 * Restore path : importLauncher → SAF OpenDocument  → BackupViewModel.restoreBackup(uri)
 *
 * Task 16: file backup chứa **Task (+ counters Pomodoro)** và **PomodoroSession** (giữ nguyên ID).
 * Fragment **chỉ** nói chuyện với [BackupViewModel] — không có truy vấn Room nào ở đây
 * (UI → ViewModel → Repository → Room).
 */
class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: BackupViewModel by viewModels()

    /** URI của file backup vừa tạo — dùng để ghi lịch sử sau khi export thành công. */
    private var pendingExportUri: Uri? = null

    // ─── SAF: Export — CreateDocument ──────────────────────────────────────────
    private val exportLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { doExport(it) }
        }

    // ─── SAF: Restore — OpenDocument ──────────────────────────────────────────
    private val importLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // Show selected file name
                val fileName = getFileName(uri)
                showFilePreview(fileName)
                // Trigger restore
                doRestore(uri)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE & INFLATION
    // ═══════════════════════════════════════════════════════════════════════════

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

        // ─── Navigation ─────────────────────────────────────────────────────────
        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        // ─── Status-card action buttons (download / verify — future tasks) ──────
        binding.btnDownloadLast.setOnClickListener {
            showSnack(getString(R.string.data_feature_pending))
        }
        binding.btnVerifyIntegrity.setOnClickListener {
            showSnack(getString(R.string.data_feature_pending))
        }

        // ─── Clear selected file ─────────────────────────────────────────────
        binding.btnClearFile.setOnClickListener {
            binding.layoutFilePreview.visibility = View.GONE
        }

        // ─── Backup button (Task 16) ────────────────────────────────────────────
        binding.exportDataButton.setOnClickListener { launchExport() }

        // ─── Restore button & drop-zone: chọn file backup qua SAF (Task 16) ────
        val pickBackupFile = View.OnClickListener { launchRestore() }
        binding.restoreDataButton.setOnClickListener(pickBackupFile)
        binding.dropZone.setOnClickListener(pickBackupFile)

        // ─── View All History ─────────────────────────────────────────────────
        binding.btnViewAllHistory.setOnClickListener {
            showSnack(getString(R.string.data_feature_pending))
        }

        observeViewModel()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  OBSERVE VIEWMODEL
    // ═══════════════════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        // 1. Số liệu thống kê (task / completed / Pomodoro session)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.taskCount,
                    viewModel.completedTaskCount,
                    viewModel.pomodoroSessionCount
                ) { total, completed, sessions -> Triple(total, completed, sessions) }
                    .collect { (total, completed, sessions) ->
                        renderStats(total, completed, sessions)
                    }
            }
        }

        // 2. Lịch sử backup + thời điểm backup gần nhất
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.recentBackups,
                    viewModel.lastBackupTime
                ) { backups, lastBackup -> backups to lastBackup }
                    .collect { (backups, lastBackup) -> renderBackupStatus(backups, lastBackup) }
            }
        }

        // 3. Kết quả backup/restore → thông báo cho người dùng
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderResult(state) }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════════

    private fun renderStats(totalTasks: Int, completedTasks: Int, sessions: Int) {
        _binding?.let { b ->
            b.tvTotalTasksCount.text = totalTasks.toString()
            b.tvCompletedCount.text = completedTasks.toString()
            b.tvBackupContentSummary.text = getString(
                com.team.taskmanagementapp.R.string.data_backup_content_summary,
                totalTasks,
                sessions
            )
        }
    }

    private fun renderBackupStatus(recentBackups: List<BackupHistoryItem>, lastBackup: String?) {
        _binding?.let { b ->
            if (recentBackups.isEmpty()) {
                b.tvBackupStatusTitle.text = getString(com.team.taskmanagementapp.R.string.data_no_backup_yet)
                b.tvBackupStatusSub.text = lastBackup
                    ?: getString(com.team.taskmanagementapp.R.string.data_no_backup_subtitle)
                b.ivStatusBadge.setImageResource(com.team.taskmanagementapp.R.drawable.ic_settings_backup)
                b.layoutStatusActions.visibility = View.GONE

                b.tvNoRecentBackups.visibility = View.VISIBLE
                hideAllRecentItems()
                b.dividerHistory.visibility = View.GONE
                b.btnViewAllHistory.visibility = View.GONE
            } else {
                val latest = recentBackups.first()
                b.tvBackupStatusTitle.text = getString(com.team.taskmanagementapp.R.string.data_last_backup_success)
                b.tvBackupStatusSub.text = "${latest.formattedDate} • ${latest.sizeString}"
                b.ivStatusBadge.setImageResource(com.team.taskmanagementapp.R.drawable.ic_check_circle)
                b.layoutStatusActions.visibility = View.VISIBLE

                b.tvNoRecentBackups.visibility = View.GONE
                val itemBindings = listOf(
                    b.recentItem1,
                    b.recentItem2,
                    b.recentItem3,
                    b.recentItem4
                )
                itemBindings.forEachIndexed { index, itemBinding ->
                    val entry = recentBackups.getOrNull(index)
                    if (entry != null) {
                        itemBinding.root.visibility = View.VISIBLE
                        itemBinding.tvBackupFileName.text = entry.fileName
                        itemBinding.tvBackupFileMeta.text = "${entry.formattedDate} • ${entry.sizeString}"
                    } else {
                        itemBinding.root.visibility = View.GONE
                    }
                }
                b.dividerHistory.visibility = if (recentBackups.size > 4) View.VISIBLE else View.GONE
                b.btnViewAllHistory.visibility = if (recentBackups.size > 4) View.VISIBLE else View.GONE
            }
        }
    }

    private fun hideAllRecentItems() {
        _binding?.let { b ->
            b.recentItem1.root.visibility = View.GONE
            b.recentItem2.root.visibility = View.GONE
            b.recentItem3.root.visibility = View.GONE
            b.recentItem4.root.visibility = View.GONE
        }
    }

    /**
     * Hiển thị kết quả backup/restore: đang xử lý → progress bar; xong → Snackbar thông báo.
     */
    private fun renderResult(state: BackupUiState) {
        val b = _binding ?: return
        when (state) {
            is BackupUiState.Idle -> b.progressBar.visibility = View.GONE
            is BackupUiState.Loading -> b.progressBar.visibility = View.VISIBLE
            is BackupUiState.Success -> {
                b.progressBar.visibility = View.GONE
                pendingExportUri?.let { uri ->
                    pendingExportUri = null
                    recordBackupHistory(uri)
                }
                showSnack(getString(com.team.taskmanagementapp.R.string.data_backup_result_success, state.message))
                viewModel.resetState()
            }
            is BackupUiState.Error -> {
                b.progressBar.visibility = View.GONE
                pendingExportUri = null
                showSnack(getString(com.team.taskmanagementapp.R.string.data_backup_result_error, state.message))
                viewModel.resetState()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SAF — BACKUP (Task 16)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun launchExport() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        exportLauncher.launch("taskflow_backup_$timestamp.json")
    }

    private fun doExport(uri: Uri) {
        showSnack(getString(com.team.taskmanagementapp.R.string.data_export_started))
        pendingExportUri = uri
        viewModel.exportBackup(uri)
    }

    /** Ghi tên file + dung lượng vào lịch sử backup sau khi file đã được tạo. */
    private fun recordBackupHistory(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fileName = getFileName(uri)
            val sizeBytes = try {
                requireContext().contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                Log.w(TAG, "recordBackupHistory: ${e.message}")
                0L
            }
            val sizeKb = if (sizeBytes > 0) (sizeBytes / 1024).coerceAtLeast(1) else 1
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            val now = System.currentTimeMillis()
            viewModel.addRecentBackup(fileName, dateFormat.format(Date(now)), "$sizeKb KB", now)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SAF — RESTORE (Task 16)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun launchRestore() {
        importLauncher.launch(arrayOf("application/json"))
    }

    private fun doRestore(uri: Uri) {
        showSnack(getString(com.team.taskmanagementapp.R.string.data_restore_started))
        viewModel.restoreBackup(uri)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun showFilePreview(name: String) {
        _binding?.let { b ->
            b.tvSelectedFileName.text = name
            b.layoutFilePreview.visibility = View.VISIBLE
        }
    }

    private fun showSnack(message: String) {
        if (!isAdded) return
        _binding?.root?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    /** Resolve human-readable file name from a content:// URI */
    private fun getFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "backup.json"
        try {
            requireContext().contentResolver.query(
                uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) name = cursor.getString(col)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getFileName: ${e.message}")
        }
        return name
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DataManagementFragment"
    }
}
