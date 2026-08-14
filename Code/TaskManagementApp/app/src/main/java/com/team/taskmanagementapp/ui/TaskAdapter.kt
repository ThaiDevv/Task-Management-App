package com.team.taskmanagementapp.ui

import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.databinding.ItemTaskSummaryBinding
import com.team.taskmanagementapp.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onTaskToggleComplete: ((Task) -> Unit)? = null,
    private val onTaskClick: ((Task) -> Unit)? = null
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(private val binding: ItemTaskSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            binding.taskTitle.text = task.title
            
            if (task.description.isBlank()) {
                binding.taskDescription.visibility = View.GONE
            } else {
                binding.taskDescription.visibility = View.VISIBLE
                binding.taskDescription.text = task.description
            }

            // Format date and time
            val dateStr = DateTimeUtils.formatTimestamp(task.dueDate, "MMM dd")
            val timeStr = DateTimeUtils.formatTimestamp(task.dueTime, "hh:mm a")
            binding.taskTime.text = "$dateStr, $timeStr"

            // Set priority pill styling
            val priorityText = task.priority.name.lowercase().replaceFirstChar { it.uppercase() }
            binding.taskPriority.text = priorityText

            val (priorityBgRes, priorityColorRes) = when (task.priority) {
                Priority.LOW -> R.color.priority_low_bg to R.color.priority_low
                Priority.MEDIUM -> R.color.priority_medium_bg to R.color.priority_medium
                Priority.HIGH -> R.color.priority_high_bg to R.color.priority_high
                Priority.URGENT -> R.color.priority_urgent_bg to R.color.priority_urgent
            }

            val priorityColor = ContextCompat.getColor(context, priorityColorRes)
            val priorityBg = ContextCompat.getColor(context, priorityBgRes)

            binding.taskPriority.setTextColor(priorityColor)
            binding.taskPriority.background = GradientDrawable().apply {
                setColor(priorityBg)
                cornerRadius = 24f
            }

            // Set checkbox state
            binding.taskCheckbox.isChecked = task.isCompleted

            // UI feedback: strikethrough title, dimmed card
            if (task.isCompleted) {
                binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskTitle.alpha = 0.5f
                binding.root.alpha = 0.65f
            } else {
                binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskTitle.alpha = 1.0f
                binding.root.alpha = 1.0f
            }

            // Checkbox click -> toggle complete/uncomplete
            binding.taskCheckbox.setOnClickListener {
                onTaskToggleComplete?.invoke(task)
            }

            // Item click -> open detail screen
            binding.root.setOnClickListener {
                onTaskClick?.invoke(task)
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem == newItem
    }
}
