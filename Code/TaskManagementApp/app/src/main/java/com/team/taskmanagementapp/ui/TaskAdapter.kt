package com.team.taskmanagementapp.ui

import android.graphics.Color
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
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.databinding.ItemTaskSummaryBinding
import com.team.taskmanagementapp.util.DateTimeUtils

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

            // Set priority pill styling and left stripe
            val (priorityBgRes, priorityColorRes) = when (task.priority) {
                Priority.LOW -> R.color.priority_low_bg to R.color.priority_low
                Priority.MEDIUM -> R.color.priority_medium_bg to R.color.priority_medium
                Priority.HIGH -> R.color.priority_high_bg to R.color.priority_high
                Priority.URGENT -> R.color.priority_urgent_bg to R.color.priority_urgent
            }

            val priorityColor = ContextCompat.getColor(context, priorityColorRes)
            val priorityBg = ContextCompat.getColor(context, priorityBgRes)

            // Priority left border stripe — only shown on completed tasks (see isCompleted block below)
            // Background color pre-set to primary blue in XML; visibility controlled below

            // Priority Pill Badge
            binding.taskPriority.text = task.priority.name
            binding.taskPriority.setTextColor(priorityColor)
            binding.taskPriority.background = GradientDrawable().apply {
                setColor(priorityBg)
                cornerRadius = 16f
            }

            // Determine if the task is overdue based on combined date and time
            val combinedDue = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            val isOverdue = !task.isCompleted && (task.status == TaskStatus.OVERDUE || (combinedDue > 0L && combinedDue < System.currentTimeMillis()))

            // Custom Checkbox Circle Button state
            if (task.isCompleted) {
                // Show blue left stripe only on completed tasks
                binding.priorityStripe.visibility = View.VISIBLE
                binding.priorityStripe.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))

                binding.checkCircleContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                binding.checkCircleContainer.strokeWidth = 0
                binding.checkMarkIcon.visibility = View.VISIBLE

                binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskTitle.alpha = 0.5f
                binding.root.alpha = 0.65f
                
                binding.overdueBadge.visibility = View.GONE
                // Reset title color
                binding.taskTitle.setTextColor(ContextCompat.getColor(context, R.color.on_background))
            } else if (isOverdue) {
                // Show red left stripe for overdue tasks
                binding.priorityStripe.visibility = View.VISIBLE
                binding.priorityStripe.setBackgroundColor(ContextCompat.getColor(context, R.color.status_overdue))

                binding.checkCircleContainer.setCardBackgroundColor(Color.WHITE)
                binding.checkCircleContainer.strokeColor = ContextCompat.getColor(context, R.color.status_overdue)
                binding.checkCircleContainer.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                binding.checkMarkIcon.visibility = View.GONE

                binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskTitle.alpha = 1.0f
                binding.root.alpha = 1.0f

                binding.overdueBadge.visibility = View.VISIBLE
                // Highlight title in overdue color
                binding.taskTitle.setTextColor(ContextCompat.getColor(context, R.color.status_overdue))
            } else {
                // Hide stripe when task is incomplete and not overdue
                binding.priorityStripe.visibility = View.GONE

                binding.checkCircleContainer.setCardBackgroundColor(Color.WHITE)
                binding.checkCircleContainer.strokeColor = Color.parseColor("#737786")
                binding.checkCircleContainer.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                binding.checkMarkIcon.visibility = View.GONE

                binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskTitle.alpha = 1.0f
                binding.root.alpha = 1.0f

                binding.overdueBadge.visibility = View.GONE
                // Reset title color
                binding.taskTitle.setTextColor(ContextCompat.getColor(context, R.color.on_background))
            }

            // Checkbox click -> toggle complete
            binding.checkCircleContainer.setOnClickListener {
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
