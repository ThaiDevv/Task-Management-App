package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.data.model.enums.Priority
import com.team.taskmanagementapp.databinding.ItemCalendarTaskBinding
import com.team.taskmanagementapp.util.DateTimeUtils

/**
 * Dedicated Adapter for Calendar Schedule View.
 * Displays tasks along a vertical timeline axis with separate card design from Today tasks.
 */
class CalendarScheduleAdapter(
    private val onTaskClick: ((Task) -> Unit)? = null
) : ListAdapter<Task, CalendarScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemCalendarTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(private val binding: ItemCalendarTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            val dp = context.resources.displayMetrics.density

            // --- 1. Left Time Label (e.g. 09:00) ---
            val timeLabel = if (task.dueTime > 0L) {
                DateTimeUtils.formatTimestamp(task.dueTime, "HH:mm")
            } else {
                "--:--"
            }
            binding.scheduleTimeLabel.text = timeLabel

            // --- 2. Task Title ---
            binding.scheduleTaskTitle.text = task.title

            // --- 3. Description ---
            if (task.description.isBlank()) {
                binding.scheduleTaskDescription.visibility = View.GONE
            } else {
                binding.scheduleTaskDescription.visibility = View.VISIBLE
                binding.scheduleTaskDescription.text = task.description
            }

            // --- 4. Time Range Text (e.g. 9:00 - 10:30 AM or formatted time) ---
            val timeStr = if (task.dueTime > 0L) {
                DateTimeUtils.formatTimestamp(task.dueTime, "hh:mm a")
            } else {
                DateTimeUtils.formatTimestamp(task.dueDate, "MMM dd")
            }
            binding.scheduleTimeRange.text = timeStr

            // --- 5. Priority Badge (Top-Right of Card) ---
            data class PStyle(val label: String, val textColor: Int, val bgColor: Int)
            val pStyle = when (task.priority) {
                Priority.LOW    -> PStyle("Low",    Color.parseColor("#10B981"), Color.parseColor("#ECFDF5"))
                Priority.MEDIUM -> PStyle("Medium", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                Priority.HIGH   -> PStyle("High",   Color.parseColor("#EF4444"), Color.parseColor("#FFF1F2"))
                Priority.URGENT -> PStyle("Urgent", Color.parseColor("#9333EA"), Color.parseColor("#F3E8FF"))
            }
            binding.scheduleTaskPriority.text = pStyle.label
            binding.scheduleTaskPriority.setTextColor(pStyle.textColor)
            binding.scheduleTaskPriority.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(pStyle.bgColor)
                cornerRadius = 6f * dp
            }

            // --- 6. Click Event ---
            binding.scheduleCardContainer.setOnClickListener {
                onTaskClick?.invoke(task)
            }
        }
    }

    private class ScheduleDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
