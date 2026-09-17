package com.team.taskmanagementapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.databinding.ItemUpcomingHeaderBinding
import com.team.taskmanagementapp.databinding.ItemUpcomingTaskBinding
import com.team.taskmanagementapp.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Sealed model for items displayed in the Upcoming timeline RecyclerView:
 * Either a Date Header (grouping tasks for a specific date) or a Task Item.
 */
sealed class UpcomingItem {
    data class Header(val dateLabel: String, val dateSubtext: String) : UpcomingItem()
    data class TaskItem(val task: Task) : UpcomingItem()
}

/**
 * UpcomingTaskAdapter — Timeline adapter supporting grouped date headers and task items.
 */
class UpcomingTaskAdapter(
    private val onTaskClick: ((Task) -> Unit)? = null
) : ListAdapter<UpcomingItem, RecyclerView.ViewHolder>(UpcomingItemDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UpcomingItem.Header -> TYPE_HEADER
            is UpcomingItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemUpcomingHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemUpcomingTaskBinding.inflate(inflater, parent, false)
            TaskViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is UpcomingItem.Header -> (holder as HeaderViewHolder).bind(item)
            is UpcomingItem.TaskItem -> (holder as TaskViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemUpcomingHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: UpcomingItem.Header) {
            binding.upcomingHeaderLabel.text = header.dateLabel
            binding.upcomingHeaderSubtext.text = header.dateSubtext
        }
    }

    inner class TaskViewHolder(private val binding: ItemUpcomingTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UpcomingItem.TaskItem) {
            val task = item.task
            val context = binding.root.context

            binding.upcomingTaskTitle.text = task.title

            // Format time display
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.dueTime))
            binding.upcomingTaskTime.text = timeStr

            val combinedDue = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            val isOverdue = !task.isCompleted && (task.status == TaskStatus.OVERDUE || (combinedDue > 0L && combinedDue < System.currentTimeMillis()))

            if (isOverdue) {
                binding.timelineDot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_overdue))
                binding.upcomingPriorityDot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_overdue))
                binding.upcomingTaskTitle.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
            } else {
                // Dot color: first task item = primary blue, others = surface gray
                val isFirstTask = bindingAdapterPosition == 1 || (bindingAdapterPosition > 0 && getItem(bindingAdapterPosition - 1) is UpcomingItem.Header && bindingAdapterPosition <= 2)
                val dotColorRes = if (isFirstTask) R.color.primary else R.color.surface_container_high
                binding.timelineDot.setCardBackgroundColor(
                    ContextCompat.getColor(context, dotColorRes)
                )

                // Priority dot color
                val priorityColor = ContextCompat.getColor(context, when (task.priority) {
                    Priority.URGENT -> R.color.priority_urgent
                    Priority.HIGH   -> R.color.priority_high
                    Priority.MEDIUM -> R.color.priority_medium
                    Priority.LOW    -> R.color.priority_low
                })
                binding.upcomingPriorityDot.setCardBackgroundColor(priorityColor)
                binding.upcomingTaskTitle.setTextColor(ContextCompat.getColor(context, R.color.on_background))
            }

            binding.root.setOnClickListener { onTaskClick?.invoke(task) }
        }
    }

    private class UpcomingItemDiffCallback : DiffUtil.ItemCallback<UpcomingItem>() {
        override fun areItemsTheSame(oldItem: UpcomingItem, newItem: UpcomingItem): Boolean {
            return when {
                oldItem is UpcomingItem.Header && newItem is UpcomingItem.Header ->
                    oldItem.dateLabel == newItem.dateLabel
                oldItem is UpcomingItem.TaskItem && newItem is UpcomingItem.TaskItem ->
                    oldItem.task.id == newItem.task.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: UpcomingItem, newItem: UpcomingItem): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Helper to group a flat list of tasks into UpcomingItems (Headers + Tasks).
     */
    fun submitTaskList(tasks: List<Task>, commitCallback: Runnable? = null) {
        if (tasks.isEmpty()) {
            submitList(emptyList(), commitCallback)
            return
        }

        val items = mutableListOf<UpcomingItem>()
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        val sortedTasks = tasks.sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }

        val grouped = sortedTasks.groupBy { task ->
            val cal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
        }.entries.take(3)

        for ((_, dayTasks) in grouped) {
            val sortedDayTasks = dayTasks.sortedBy { DateTimeUtils.getCombinedDueTimestamp(it.dueDate, it.dueTime) }
            val firstTaskCal = Calendar.getInstance().apply { timeInMillis = sortedDayTasks.first().dueDate }

            val isTomorrow = firstTaskCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR)
                    && firstTaskCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

            val dateLabel = if (isTomorrow) "Tomorrow" else dateFormat.format(firstTaskCal.time)
            val dateSubtext = if (isTomorrow) dateFormat.format(firstTaskCal.time) else dayFormat.format(firstTaskCal.time)

            items.add(UpcomingItem.Header(dateLabel, dateSubtext))
            sortedDayTasks.forEach { task ->
                items.add(UpcomingItem.TaskItem(task))
            }
        }
        submitList(items, commitCallback)
    }
}
