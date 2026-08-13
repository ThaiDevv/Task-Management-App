package com.team.taskmanagementapp.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.data.local.entity.Task
import com.team.taskmanagementapp.databinding.ItemTaskSummaryBinding
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
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description

            // Format time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(task.dueTime))
            binding.taskTime.text = formattedTime

            // Set priority
            binding.taskPriority.text = task.priority.toString()

            // Set checkbox state
            binding.taskCheckbox.isChecked = task.isCompleted

            // UI feedback: strikethrough title, dimmed card
            if (task.isCompleted) {
binding.taskTitle.paintFlags =
                    binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskTitle.alpha = 0.6f
                binding.root.alpha = 0.6f
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
