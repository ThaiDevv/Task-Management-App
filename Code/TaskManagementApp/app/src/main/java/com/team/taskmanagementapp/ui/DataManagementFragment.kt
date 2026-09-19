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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.BackupRepository
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
 * TASK-26 / TASK-50 / TASK-54 — Backup & Restore screen.
 *
 * Export path  : exportLauncher → SAF CreateDocument → writes JSON via BackupRepository
 * Restore path : importLauncher → SAF OpenDocument  → reads JSON via ContentResolver
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

        // ─── Populate Recent Backups ─────────────────────────────────────────────
        loadBackupStatus()

        // ─── Export button ───────────────────────────────────────────────────────
        binding.exportDataButton.setOnClickListener { launchExport() }

        // ─── Restore button & drop-zone: launch ImportActivity for JSON import ───
        val openImportActivity = View.OnClickListener {
            val intent = android.content.Intent(requireContext(), com.team.taskmanagementapp.ui.activity.ImportActivity::class.java)
            startActivity(intent)
        }
        binding.restoreDataButton.setOnClickListener(openImportActivity)
        binding.dropZone.setOnClickListener(openImportActivity)

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
    //  BACKUP STATUS & RECENT HISTORY
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadBackupStatus() {
        if (_binding == null || !isAdded) return
        val dao = AppDatabase.getInstance(requireContext()).taskDao()
        val repo = BackupRepository(requireContext(), dao)
        val recentBackups = repo.getRecentBackups()

        _binding?.let { b ->
            if (recentBackups.isEmpty()) {
                b.tvBackupStatusTitle.text = getString(com.team.taskmanagementapp.R.string.data_no_backup_yet)
                b.tvBackupStatusSub.text = getString(com.team.taskmanagementapp.R.string.data_no_backup_subtitle)
                b.ivStatusBadge.setImageResource(com.team.taskmanagementapp.R.drawable.ic_settings_backup)
                b.layoutStatusActions.visibility = View.GONE

                b.tvNoRecentBackups.visibility = View.VISIBLE
                b.recentItem1.root.visibility = View.GONE
                b.recentItem2.root.visibility = View.GONE
                b.recentItem3.root.visibility = View.GONE
                b.recentItem4.root.visibility = View.GONE
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

    // ═══════════════════════════════════════════════════════════════════════════
    //  SAF — EXPORT (TMA-50)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun launchExport() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        exportLauncher.launch("taskflow_backup_$timestamp.json")
    }

    private fun doExport(uri: Uri) {
        showSnack(getString(com.team.taskmanagementapp.R.string.data_export_started))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).taskDao()
                val repo = BackupRepository(requireContext(), dao)
                val count = withContext(Dispatchers.IO) {
                    repo.exportToJson(uri)
                }
                val fileName = getFileName(uri)
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                val now = System.currentTimeMillis()
                val sizeBytes = try {
                    requireContext().contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                } catch (e: Exception) {
                    0L
                }
                val sizeKb = if (sizeBytes > 0) (sizeBytes / 1024).coerceAtLeast(1) else 1
                repo.addRecentBackup(fileName, dateFormat.format(Date(now)), "$sizeKb KB", now)

                loadBackupStatus()
                loadExportStats()
                showSnack(getString(com.team.taskmanagementapp.R.string.data_export_success, count))
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
                put("isCompleted", task.isCompleted)
                put("isComplete", task.isCompleted)
                put("isRecurring", task.isRecurring)
                put("recurrenceType", task.recurrenceType.name)
                put("recurrenceInterval", task.recurrenceInterval)
                put("reminderMinutes", task.reminderMinutes)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
                put("completedAt", task.completedAt ?: JSONObject.NULL)
            }
            array.put(obj)
        }
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedBy", "TaskManagementApp")
        root.put("taskCount", tasks.size)
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
                completedAt = if (obj.optBoolean("isComplete", false))
                    obj.optLong("completedAt", 0L).takeIf { it > 0L } else null,
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
        loadExportStats()
        loadBackupStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DataManagementFragment"
        private const val BACKUP_VERSION = "1.0"
    }
}
