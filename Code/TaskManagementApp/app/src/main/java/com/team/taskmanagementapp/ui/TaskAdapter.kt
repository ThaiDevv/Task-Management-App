package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.data.model.enums.RecurrenceType
import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.databinding.ItemTaskSummaryBinding
import com.team.taskmanagementapp.util.DateTimeUtils
import com.team.taskmanagementapp.util.RecurrenceHelper

class TaskAdapter(
    private val onTaskToggleComplete: ((Task) -> Unit)? = null,
    private val onTaskClick: ((Task) -> Unit)? = null
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            val dp = context.resources.displayMetrics.density

            // Ensure MaterialCardView clips its children (like priorityStripe) to its rounded corners
            binding.cardContainer.outlineProvider = ViewOutlineProvider.BACKGROUND
            binding.cardContainer.clipToOutline = true

            // --- 1. Title ---
            binding.taskTitle.text = task.title

            // --- 2. Description ---
            if (task.description.isBlank()) {
                binding.taskDescription.visibility = View.GONE
            } else {
                binding.taskDescription.visibility = View.VISIBLE
                binding.taskDescription.text = task.description
            }

            // --- 3. Date + Time ---
            val dateStr = DateTimeUtils.formatTimestamp(task.dueDate, "MMM dd")
            val timeStr = DateTimeUtils.formatTimestamp(task.dueTime, "hh:mm a")
            binding.taskTime.text = "$dateStr, $timeStr"

            // --- 4. Priority Pill Styling ---
            data class PriorityStyle(val label: String, val textColor: Int, val bgColor: Int)
            val pStyle = when (task.priority) {
                Priority.LOW    -> PriorityStyle("LOW",    Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                Priority.MEDIUM -> PriorityStyle("MEDIUM", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                Priority.HIGH   -> PriorityStyle("HIGH",   Color.parseColor("#EF4444"), Color.parseColor("#FFF1F2"))
                Priority.URGENT -> PriorityStyle("URGENT", Color.parseColor("#9333EA"), Color.parseColor("#F3E8FF"))
            }
            binding.taskPriority.text = pStyle.label
            binding.taskPriority.setTextColor(pStyle.textColor)
            binding.taskPriority.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(pStyle.bgColor)
                cornerRadius = 8f * dp
            }

            // --- 4.5. Recurrence Badge ---
            if (task.isRecurring && task.recurrenceType != RecurrenceType.NONE) {
                binding.recurrenceBadge.visibility = View.VISIBLE
                binding.recurrenceText.text = RecurrenceHelper.getRecurrenceDisplayText(
                    task.recurrenceType, context
                )
            } else {
                binding.recurrenceBadge.visibility = View.GONE
            }

            // --- 5. Overdue Detection ---
            val combinedDue = DateTimeUtils.getCombinedDueTimestamp(task.dueDate, task.dueTime)
            val isOverdue = !task.isCompleted &&
                    (task.status == TaskStatus.OVERDUE ||
                            (combinedDue > 0L && combinedDue < System.currentTimeMillis()))

            // --- 6. Task State Handling ---
            when {
                task.isCompleted -> {
                    // Blue Left Border Stripe (matches reference UI)
                    binding.priorityStripe.visibility = View.VISIBLE
                    binding.priorityStripe.setBackgroundColor(Color.parseColor("#0D6EFD"))

                    // Checkbox Container -> Solid Blue Circle with White Checkmark
                    binding.checkCircleContainer.setCardBackgroundColor(Color.parseColor("#0D6EFD"))
                    binding.checkCircleContainer.strokeWidth = 0
                    binding.checkMarkIcon.visibility = View.VISIBLE

                    // Title -> Strike-through & muted text
                    binding.taskTitle.apply {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        setTextColor(Color.parseColor("#64748B"))
                        alpha = 0.7f
                    }
                    binding.taskDescription.alpha = 0.6f
                    binding.root.alpha = 0.85f
                    binding.overdueBadge.visibility = View.GONE
                }

                isOverdue -> {
                    // Red Left Border Stripe
                    binding.priorityStripe.visibility = View.VISIBLE
                    binding.priorityStripe.setBackgroundColor(Color.parseColor("#EF4444"))

                    // Checkbox Container -> Outline Ring with Red Border
                    binding.checkCircleContainer.setCardBackgroundColor(Color.WHITE)
                    binding.checkCircleContainer.strokeColor = Color.parseColor("#EF4444")
                    binding.checkCircleContainer.strokeWidth = (2 * dp).toInt()
                    binding.checkMarkIcon.visibility = View.GONE

                    // Title -> Active bold text
                    binding.taskTitle.apply {
                        paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        setTextColor(Color.parseColor("#0F172A"))
                        alpha = 1.0f
                    }
                    binding.taskDescription.alpha = 1.0f
                    binding.root.alpha = 1.0f
                    binding.overdueBadge.visibility = View.VISIBLE
                }

                else -> {
                    // Normal Incomplete Task: Hide stripe for clean white look
                    binding.priorityStripe.visibility = View.GONE

                    // Checkbox Container -> Outline Ring with Slate Border
                    binding.checkCircleContainer.setCardBackgroundColor(Color.WHITE)
                    binding.checkCircleContainer.strokeColor = Color.parseColor("#94A3B8")
                    binding.checkCircleContainer.strokeWidth = (2 * dp).toInt()
                    binding.checkMarkIcon.visibility = View.GONE

                    // Title -> Normal text
                    binding.taskTitle.apply {
                        paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        setTextColor(Color.parseColor("#0F172A"))
                        alpha = 1.0f
                    }
                    binding.taskDescription.alpha = 1.0f
                    binding.root.alpha = 1.0f
                    binding.overdueBadge.visibility = View.GONE
                }
            }

            // --- 7. Event Listeners ---
            binding.checkCircleContainer.setOnClickListener {
                onTaskToggleComplete?.invoke(task)
            }

            binding.btnMoreOptions.setOnClickListener {
                onTaskClick?.invoke(task)
            }

            binding.root.setOnClickListener {
                onTaskClick?.invoke(task)
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
