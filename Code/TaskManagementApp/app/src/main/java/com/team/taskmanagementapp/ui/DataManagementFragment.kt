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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentDataManagementBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TASK-26 / TASK-54 — Backup & Restore screen.
 *
 * Export path  : exportLauncher → SAF CreateDocument → writes JSON via ContentResolver
 * Restore path : importLauncher → SAF OpenDocument  → reads JSON via ContentResolver
 *
 * JSON schema (per task):
 *   { id, title, description, dueDate, dueTime, priority, status,
 *     isComplete, isRecurring, recurrenceType, recurrenceInterval,
 *     reminderMinutes, createdAt, updatedAt }
 */
class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = requireNotNull(_binding)

    // ─── SAF: Export — CreateDocument ──────────────────────────────────────────
    private val exportLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { doExport(it) }
        }

    // ─── SAF: Import — OpenDocument ────────────────────────────────────────────
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

    // ───────────────────────────────────────────────────────────────────────────

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
            showSnack(getString(com.team.taskmanagementapp.R.string.data_feature_pending))
        }
        binding.btnVerifyIntegrity.setOnClickListener {
            showSnack(getString(com.team.taskmanagementapp.R.string.data_feature_pending))
        }

        // ─── Clear selected file ─────────────────────────────────────────────
        binding.btnClearFile.setOnClickListener {
            binding.layoutFilePreview.visibility = View.GONE
        }

        // ─── Load export stats ───────────────────────────────────────────────────
        loadExportStats()

        // ─── Populate Recent Backups placeholder rows ─────────────────────────
        setupRecentBackups()

        // ─── Export button ───────────────────────────────────────────────────────
        binding.exportDataButton.setOnClickListener { launchExport() }

        // ─── Restore button & drop-zone ─────────────────────────────────────────
        val openImportPicker = View.OnClickListener { launchImport() }
        binding.restoreDataButton.setOnClickListener(openImportPicker)
        binding.dropZone.setOnClickListener(openImportPicker)

        // ─── View All History ─────────────────────────────────────────────────
        binding.btnViewAllHistory.setOnClickListener {
            showSnack(getString(com.team.taskmanagementapp.R.string.data_feature_pending))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadExportStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).taskDao()
                val total = withContext(Dispatchers.IO) { dao.getAllTasksSync().size }
                val completed = withContext(Dispatchers.IO) { dao.getCompletedTasksCount() }
                _binding?.let { b ->
                    b.tvTotalTasksCount.text = total.toString()
                    b.tvCompletedCount.text = completed.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadExportStats error", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RECENT BACKUPS (placeholder data — real history in future task)
    // ═══════════════════════════════════════════════════════════════════════════

    private data class BackupEntry(val name: String, val meta: String)

    private fun setupRecentBackups() {
        val placeholders = listOf(
            BackupEntry("Backup_2023_10_27.json", "Oct 27, 2023 • 942 KB"),
            BackupEntry("Weekly_Snapshot_Oct20.j…", "Oct 20, 2023 • 1.1 MB"),
            BackupEntry("Pre_Migration_v2.json", "Oct 14, 2023 • 880 KB"),
            BackupEntry("Initial_Setup.json", "Sep 30, 2023 • 420 KB")
        )
        val itemBindings = listOf(
            binding.recentItem1,
            binding.recentItem2,
            binding.recentItem3,
            binding.recentItem4
        )
        placeholders.forEachIndexed { index, entry ->
            val itemBinding = itemBindings.getOrNull(index) ?: return@forEachIndexed
            itemBinding.tvBackupFileName.text = entry.name
            itemBinding.tvBackupFileMeta.text = entry.meta
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SAF — EXPORT
    // ═══════════════════════════════════════════════════════════════════════════


    private fun launchExport() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        exportLauncher.launch("taskflow_backup_$timestamp.json")
    }

    private fun doExport(uri: Uri) {
        showSnack(getString(com.team.taskmanagementapp.R.string.data_export_started))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tasks = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).taskDao().getAllTasksSync()
                }
                val json = tasksToJson(tasks)
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                // Mark latest status success
                updateStatusCard(success = true)
                showSnack(getString(com.team.taskmanagementapp.R.string.data_export_success))
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                showSnack(getString(com.team.taskmanagementapp.R.string.data_export_failed))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SAF — RESTORE / IMPORT
    // ═══════════════════════════════════════════════════════════════════════════

    private fun launchImport() {
        importLauncher.launch(arrayOf("application/json"))
    }

    private fun doRestore(uri: Uri) {
        showSnack(getString(com.team.taskmanagementapp.R.string.data_restore_started))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: throw IllegalStateException("Cannot open file")
                }
                val tasks = jsonToTasks(jsonString)
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(requireContext()).taskDao()
                    dao.deleteAllTasks()
                    dao.insertAllTasks(tasks)
                }
                // Refresh stats after restore
                loadExportStats()
                showSnack(getString(com.team.taskmanagementapp.R.string.data_restore_success))
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _binding?.layoutFilePreview?.visibility = View.GONE
                showSnack(getString(com.team.taskmanagementapp.R.string.data_restore_failed))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  JSON SERIALISATION
    // ═══════════════════════════════════════════════════════════════════════════

    private fun tasksToJson(tasks: List<Task>): String {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("dueDate", task.dueDate)
                put("dueTime", task.dueTime)
                put("priority", task.priority.name)
                put("status", task.status.name)
                put("isComplete", task.isCompleted)
                put("isRecurring", task.isRecurring)
                put("recurrenceType", task.recurrenceType.name)
                put("recurrenceInterval", task.recurrenceInterval)
                put("reminderMinutes", task.reminderMinutes)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
            }
            array.put(obj)
        }
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("tasks", array)
        return root.toString(2)
    }

    private fun jsonToTasks(json: String): List<Task> {
        val root = JSONObject(json)
        val array = root.getJSONArray("tasks")
        val tasks = mutableListOf<Task>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val task = Task(
                id = obj.optInt("id", 0),
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                dueDate = obj.optLong("dueDate", 0L),
                dueTime = obj.optLong("dueTime", 0L),
                priority = runCatching {
                    Priority.valueOf(obj.optString("priority", Priority.MEDIUM.name))
                }.getOrDefault(Priority.MEDIUM),
                status = runCatching {
                    TaskStatus.valueOf(obj.optString("status", TaskStatus.TODO.name))
                }.getOrDefault(TaskStatus.TODO),
                isCompleted = obj.optBoolean("isComplete", false),
                isRecurring = obj.optBoolean("isRecurring", false),
                recurrenceType = runCatching {
                    RecurrenceType.valueOf(obj.optString("recurrenceType", RecurrenceType.NONE.name))
                }.getOrDefault(RecurrenceType.NONE),
                recurrenceInterval = obj.optInt("recurrenceInterval", 1),
                reminderMinutes = obj.optInt("reminderMinutes", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            )
            tasks.add(task)
        }
        return tasks
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

    private fun updateStatusCard(success: Boolean) {
        if (!isAdded) return
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        if (success) {
            _binding?.let { b ->
                b.tvBackupStatusTitle.text =
                    getString(com.team.taskmanagementapp.R.string.data_last_backup_success)
                b.tvBackupStatusSub.text = "Today at $timestamp • —"
                b.ivStatusBadge.setImageResource(
                    com.team.taskmanagementapp.R.drawable.ic_check_circle
                )
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DataManagementFragment"
        private const val BACKUP_VERSION = 1
    }
}
