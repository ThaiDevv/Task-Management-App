package com.team.taskmanagementapp.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.ai.AiAssistantCommand
import com.team.taskmanagementapp.ai.AiTaskQuery
import com.team.taskmanagementapp.ai.AiTaskAssistant
import com.team.taskmanagementapp.data.local.db.AppDatabase
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.data.repository.TaskRepository
import com.team.taskmanagementapp.databinding.FragmentAiAssistantBottomSheetBinding
import com.team.taskmanagementapp.util.AlarmScheduler
import com.team.taskmanagementapp.util.DateTimeUtils
import com.team.taskmanagementapp.util.NotificationHelper
import com.team.taskmanagementapp.util.TaskCompletionHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

class AiAssistantBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAiAssistantBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var baseSheetPaddingBottom = 0
    private val assistant by lazy { AiTaskAssistant() }
    private val repository by lazy {
        TaskRepository(AppDatabase.getInstance(requireContext()).taskDao())
    }

    private val speechInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (spokenText.isNullOrEmpty()) {
            showNotice(R.string.ai_task_speech_empty)
        } else {
            binding.promptEditText.setText(spokenText)
            binding.promptEditText.setSelection(spokenText.length)
            binding.promptInputLayout.error = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistantBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addMessage(getString(R.string.ai_assistant_welcome), fromUser = false)

        binding.closeButton.setOnClickListener { dismiss() }
        binding.sendButton.setOnClickListener { submitPrompt() }
        binding.voiceButton.setOnClickListener { launchVoiceInput() }
        binding.promptEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitPrompt()
                true
            } else {
                false
            }
        }
        binding.todayTasksButton.setOnClickListener {
            binding.promptEditText.setText("Show today's tasks")
            submitPrompt()
        }
        binding.createTaskButton.setOnClickListener {
            preparePrompt("Create a task called ")
        }
        binding.updateProgressButton.setOnClickListener {
            preparePrompt("Move the task named  to In Progress")
            binding.promptEditText.setSelection("Move the task named ".length)
        }
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        sheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val bottomSheet = sheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        baseSheetPaddingBottom = bottomSheet.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomInset = maxOf(imeBottom, navigationBottom)
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                baseSheetPaddingBottom + bottomInset
            )
            insets
        }
        ViewCompat.requestApplyInsets(bottomSheet)
        BottomSheetBehavior.from(bottomSheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun preparePrompt(text: String) {
        binding.promptEditText.setText(text)
        binding.promptEditText.setSelection(text.length)
        binding.promptEditText.requestFocus()
    }

    private fun submitPrompt() {
        val request = binding.promptEditText.text?.toString()?.trim().orEmpty()
        if (request.isEmpty()) {
            binding.promptInputLayout.error = getString(R.string.ai_task_empty_prompt)
            return
        }

        binding.promptInputLayout.error = null
        binding.promptEditText.text?.clear()
        addMessage(request, fromUser = true)
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tasks = repository.getAllTasksSync()
                val command = assistant.understand(request, tasks)
                handleCommand(command, tasks)
            } catch (error: Exception) {
                Log.e(TAG, "AI assistant request failed", error)
                addMessage(getString(R.string.ai_assistant_error), fromUser = false)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun handleCommand(command: AiAssistantCommand, tasks: List<Task>) {
        when (command.action) {
            "CREATE_TASK" -> showCreateConfirmation(command)
            "UPDATE_TASK" -> showUpdateConfirmation(command, tasks)
            "LIST_TASKS" -> addMessage(formatTaskList(tasks, command.query), fromUser = false)
            "HELP" -> addMessage(getString(R.string.ai_help_message), fromUser = false)
            else -> addMessage(
                command.reply.ifBlank { getString(R.string.ai_unknown_message) },
                fromUser = false
            )
        }
    }

    private fun showCreateConfirmation(command: AiAssistantCommand) {
        if (command.title.isNullOrBlank()) {
            addMessage(getString(R.string.ai_unknown_message), fromUser = false)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ai_confirm_create_title)
            .setMessage(createSummary(command))
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_confirm_action) { _, _ -> createTask(command) }
            .show()
    }

    private fun showUpdateConfirmation(command: AiAssistantCommand, tasks: List<Task>) {
        val task = command.targetTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
        if (task == null || !hasRequestedUpdate(command)) {
            addMessage(
                command.reply.ifBlank { getString(R.string.ai_unknown_message) },
                fromUser = false
            )
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ai_confirm_update_title)
            .setMessage(updateSummary(task, command))
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_confirm_action) { _, _ -> updateTask(task, command) }
            .show()
    }

    private fun createTask(command: AiAssistantCommand) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val dueDate = LocalDate.parse(requireNotNull(command.dueDate))
                val dueTime = LocalTime.parse(requireNotNull(command.dueTime))
                val now = System.currentTimeMillis()
                val task = Task(
                    title = requireNotNull(command.title),
                    description = command.description.orEmpty(),
                    dueDate = dueDate.atStartOfDay(ZONE).toInstant().toEpochMilli(),
                    dueTime = LocalDateTime.of(dueDate, dueTime).atZone(ZONE).toInstant().toEpochMilli(),
                    priority = Priority.valueOf(requireNotNull(command.priority)),
                    status = TaskStatus.TODO,
                    isCompleted = false,
                    isRecurring = command.recurrence != "NONE",
                    recurrenceType = RecurrenceType.valueOf(requireNotNull(command.recurrence)),
                    reminderMinutes = requireNotNull(command.reminderMinutes),
                    createdAt = now,
                    updatedAt = now
                )
                val id = repository.insert(task)
                AlarmScheduler.scheduleAlarm(requireContext(), task.copy(id = id.toInt()))
            }.onSuccess {
                addMessage(getString(R.string.ai_task_created), fromUser = false)
            }.onFailure { error ->
                Log.e(TAG, "Unable to create assistant task", error)
                addMessage(getString(R.string.ai_assistant_error), fromUser = false)
            }
        }
    }

    private fun updateTask(original: Task, command: AiAssistantCommand) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                var base = original
                val requestedStatus = command.status?.let(TaskStatus::valueOf)

                if (original.isCompleted && requestedStatus != null && requestedStatus != TaskStatus.COMPLETED) {
                    TaskCompletionHelper.setCompleted(requireContext(), original, targetCompleted = false)
                    base = original.copy(
                        status = TaskStatus.TODO,
                        isCompleted = false,
                        completedAt = null
                    )
                }

                val (dueDate, dueTime) = resolveDueDateTime(base, command)
                val edited = base.copy(
                    dueDate = dueDate,
                    dueTime = dueTime,
                    priority = command.priority?.let(Priority::valueOf) ?: base.priority,
                    status = when {
                        requestedStatus == TaskStatus.COMPLETED -> base.status
                        requestedStatus != null -> requestedStatus
                        else -> base.status
                    },
                    isCompleted = if (requestedStatus == TaskStatus.TODO || requestedStatus == TaskStatus.IN_PROGRESS) {
                        false
                    } else {
                        base.isCompleted
                    },
                    completedAt = if (requestedStatus == TaskStatus.TODO || requestedStatus == TaskStatus.IN_PROGRESS) {
                        null
                    } else {
                        base.completedAt
                    },
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(edited)

                if (requestedStatus == TaskStatus.COMPLETED && !edited.isCompleted) {
                    TaskCompletionHelper.setCompleted(requireContext(), edited, targetCompleted = true)
                } else if (edited.isCompleted) {
                    AlarmScheduler.cancelAlarm(requireContext(), edited.id)
                    NotificationHelper.cancelNotification(requireContext(), edited.id)
                } else {
                    AlarmScheduler.rescheduleAlarm(requireContext(), edited)
                }
            }.onSuccess {
                addMessage(getString(R.string.ai_task_updated), fromUser = false)
            }.onFailure { error ->
                Log.e(TAG, "Unable to update assistant task", error)
                addMessage(getString(R.string.ai_assistant_error), fromUser = false)
            }
        }
    }

    private fun resolveDueDateTime(task: Task, command: AiAssistantCommand): Pair<Long, Long> {
        if (command.dueDate == null && command.dueTime == null) return task.dueDate to task.dueTime

        val existingDate = if (task.dueDate > 0L) {
            Instant.ofEpochMilli(task.dueDate).atZone(ZONE).toLocalDate()
        } else {
            LocalDate.now().plusDays(1)
        }
        val existingTime = if (task.dueTime > 0L) {
            Instant.ofEpochMilli(task.dueTime).atZone(ZONE).toLocalTime()
        } else {
            LocalTime.of(9, 0)
        }
        val date = command.dueDate?.let(LocalDate::parse) ?: existingDate
        val time = command.dueTime?.let(LocalTime::parse) ?: existingTime
        return date.atStartOfDay(ZONE).toInstant().toEpochMilli() to
            LocalDateTime.of(date, time).atZone(ZONE).toInstant().toEpochMilli()
    }

    private fun createSummary(command: AiAssistantCommand): String = buildString {
        appendLine(command.title)
        appendLine("Due: ${command.dueDate} at ${command.dueTime}")
        appendLine("Priority: ${displayValue(command.priority)}")
        appendLine("Status: Todo")
        append("Repeat: ${displayValue(command.recurrence)}")
    }

    private fun updateSummary(task: Task, command: AiAssistantCommand): String = buildString {
        appendLine(task.title)
        command.status?.let { appendLine("Status: ${displayValue(it)}") }
        command.dueDate?.let { appendLine("Due date: $it") }
        command.dueTime?.let { appendLine("Due time: $it") }
        command.priority?.let { appendLine("Priority: ${displayValue(it)}") }
    }.trim()

    private fun hasRequestedUpdate(command: AiAssistantCommand): Boolean =
        command.status != null || command.dueDate != null ||
            command.dueTime != null || command.priority != null

    private fun formatTaskList(tasks: List<Task>, query: AiTaskQuery?): String {
        val criteria = query ?: AiTaskQuery()
        val matching = tasks.filter { task -> matchesQuery(task, criteria) }
        if (matching.isEmpty()) return getString(R.string.ai_no_matching_tasks)

        val sorted = sortTasks(matching, criteria)
        if (criteria.resultType == "COUNT") {
            return "${matching.size} task${if (matching.size == 1) "" else "s"} match your filters."
        }
        if (criteria.resultType == "SUMMARY") {
            val grouped = matching.groupingBy { effectiveStatus(it) }.eachCount()
            return buildString {
                append("${matching.size} task${if (matching.size == 1) "" else "s"} found. ")
                append("Todo: ${grouped[TaskStatus.TODO] ?: 0}; ")
                append("In Progress: ${grouped[TaskStatus.IN_PROGRESS] ?: 0}; ")
                append("Completed: ${grouped[TaskStatus.COMPLETED] ?: 0}; ")
                append("Overdue: ${grouped[TaskStatus.OVERDUE] ?: 0}.")
            }
        }

        val shown = sorted.take(criteria.limit.coerceIn(1, MAX_LIST_ITEMS))
        return buildString {
            appendLine("${matching.size} task${if (matching.size == 1) "" else "s"}:")
            shown.forEach { task ->
                val status = effectiveStatus(task)
                val dueAt = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
                val dueText = if (task.dueDate > 0L) DateTimeUtils.formatTimestamp(dueAt) else "No due date"
                appendLine("• ${task.title} — ${displayValue(status.name)} — $dueText")
            }
            if (matching.size > shown.size) append("…and ${matching.size - shown.size} more")
        }.trim()
    }

    private fun matchesQuery(task: Task, query: AiTaskQuery): Boolean {
        val done = task.isCompleted || task.status == TaskStatus.COMPLETED
        if (query.completion == "DONE" && !done) return false
        if (query.completion == "NOT_DONE" && done) return false

        if (query.statuses.isNotEmpty() && effectiveStatus(task).name !in query.statuses) return false
        if (query.priorities.isNotEmpty() && task.priority.name !in query.priorities) return false
        if (!matchesDate(task, query)) return false

        if (query.recurrenceTypes.isNotEmpty()) {
            val recurrenceMatches = query.recurrenceTypes.any { value ->
                when (value) {
                    "ONE_TIME" -> !task.isRecurring || task.recurrenceType == RecurrenceType.NONE
                    "RECURRING" -> task.isRecurring && task.recurrenceType != RecurrenceType.NONE
                    "PAUSED" -> task.isPaused
                    else -> task.recurrenceType.name == value
                }
            }
            if (!recurrenceMatches) return false
        }

        when (query.reminder) {
            "ENABLED" -> if (task.reminderMinutes <= 0) return false
            "DISABLED" -> if (task.reminderMinutes > 0) return false
        }

        val keyword = query.keyword?.lowercase(Locale.US)
        if (keyword != null && !task.title.lowercase(Locale.US).contains(keyword) &&
            !task.description.lowercase(Locale.US).contains(keyword)
        ) return false

        return true
    }

    private fun matchesDate(task: Task, query: AiTaskQuery): Boolean {
        if (query.datePreset == "NO_DUE_DATE") return task.dueDate <= 0L
        if (query.datePreset == "OVERDUE") return DateTimeUtils.isOverdue(task)
        val window = resolveDateWindow(query) ?: return true
        val timestamp = when (query.dateField) {
            "DUE_DATE" -> task.dueDate.takeIf { it > 0L }
            "CREATED_AT" -> task.createdAt
            "UPDATED_AT" -> task.updatedAt
            "COMPLETED_AT" -> task.completedAt
            else -> task.dueDate.takeIf { it > 0L }
        } ?: return false
        return timestamp >= window.first && timestamp < window.second
    }

    private fun resolveDateWindow(query: AiTaskQuery): Pair<Long, Long>? {
        val today = LocalDate.now(ZONE)
        fun day(date: LocalDate): Pair<Long, Long> {
            val start = date.atStartOfDay(ZONE).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli()
            return start to end
        }
        return when (query.datePreset) {
            "TODAY" -> day(today)
            "TOMORROW" -> day(today.plusDays(1))
            "THIS_WEEK" -> {
                val start = today.with(DayOfWeek.MONDAY)
                day(start).first to day(start.plusDays(7)).first
            }
            "NEXT_7_DAYS" -> day(today).first to day(today.plusDays(7)).first
            "THIS_MONTH" -> {
                val start = today.withDayOfMonth(1)
                day(start).first to day(start.plusMonths(1)).first
            }
            "CUSTOM" -> {
                val start = query.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val end = query.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (start == null || end == null || end.isBefore(start)) null
                else day(start).first to day(end.plusDays(1)).first
            }
            else -> null
        }
    }

    private fun effectiveStatus(task: Task): TaskStatus = when {
        task.isCompleted || task.status == TaskStatus.COMPLETED -> TaskStatus.COMPLETED
        DateTimeUtils.isOverdue(task) -> TaskStatus.OVERDUE
        else -> task.status
    }

    private fun sortTasks(tasks: List<Task>, query: AiTaskQuery): List<Task> {
        val sorted = when (query.sortField) {
            "TITLE" -> tasks.sortedBy { it.title.lowercase(Locale.US) }
            "PRIORITY" -> tasks.sortedBy { priorityRank(it.priority) }
            "CREATED_AT" -> tasks.sortedBy { it.createdAt }
            "UPDATED_AT" -> tasks.sortedBy { it.updatedAt }
            else -> tasks.sortedBy { if (it.dueDate > 0L) DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) else Long.MAX_VALUE }
        }
        val ordered = if (query.sortDirection == "DESC") sorted.asReversed() else sorted
        val unfinished = ordered.filter { effectiveStatus(it) != TaskStatus.COMPLETED }
        val completed = ordered.filter { effectiveStatus(it) == TaskStatus.COMPLETED }
        return unfinished + completed
    }

    private fun priorityRank(priority: Priority): Int = when (priority) {
        Priority.LOW -> 1
        Priority.MEDIUM -> 2
        Priority.HIGH -> 3
        Priority.URGENT -> 4
    }

    private fun displayValue(value: String?): String = value.orEmpty()
        .lowercase(Locale.US)
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.ai_task_listening_prompt))
        }
        try {
            speechInputLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            showNotice(R.string.ai_task_speech_unavailable)
        }
    }

    private fun addMessage(message: String, fromUser: Boolean) {
        if (message.isBlank() || _binding == null) return
        val messageView = TextView(requireContext()).apply {
            text = message
            textSize = 14f
            typeface = resources.getFont(R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(requireContext(), if (fromUser) R.color.white else R.color.on_surface))
            setBackgroundResource(if (fromUser) R.drawable.bg_ai_bubble_user else R.drawable.bg_ai_bubble_assistant)
            maxWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
            setTextIsSelectable(true)
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (fromUser) Gravity.END else Gravity.START
            topMargin = dp(8)
            bottomMargin = dp(4)
        }
        binding.conversationContainer.addView(messageView, params)
        binding.conversationScroll.post {
            binding.conversationScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setLoading(loading: Boolean) {
        if (_binding == null) return
        binding.loadingContainer.isVisible = loading
        binding.sendButton.isEnabled = !loading
        binding.voiceButton.isEnabled = !loading
        binding.promptEditText.isEnabled = !loading
    }

    private fun showNotice(messageRes: Int) {
        if (_binding != null) Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AiAssistantBottomSheet"
        private const val MAX_LIST_ITEMS = 20
        private val ZONE: ZoneId = ZoneId.systemDefault()
    }
}
