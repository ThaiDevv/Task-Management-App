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
import com.team.taskmanagementapp.databinding.ItemUpcomingTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UpcomingTaskAdapter — Timeline style matching home_dashboard_polished Stitch spec.
 * Renders upcoming tasks as timeline items (dot + title + time).
 */
class UpcomingTaskAdapter(
    private val onTaskClick: ((Task) -> Unit)? = null
) : ListAdapter<Task, UpcomingTaskAdapter.UpcomingViewHolder>(UpcomingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpcomingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UpcomingViewHolder(private val binding: ItemUpcomingTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            binding.upcomingTaskTitle.text = task.title

            // Format time display
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.dueTime))
            binding.upcomingTaskTime.text = timeStr

            // Dot color: first/active task = blue primary, rest = gray
            val dotColorRes = if (bindingAdapterPosition == 0) R.color.primary else R.color.surface_container_high
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

            binding.root.setOnClickListener { onTaskClick?.invoke(task) }
        }
    }

    private class UpcomingDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
