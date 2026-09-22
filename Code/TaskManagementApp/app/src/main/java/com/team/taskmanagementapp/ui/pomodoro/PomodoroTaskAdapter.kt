package com.team.taskmanagementapp.ui.pomodoro

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
import com.team.taskmanagementapp.databinding.ItemPomodoroTaskBinding

/**
 * Adapter cho danh sách chọn công việc của Pomodoro (Task 10).
 *
 * Chỉ hiển thị dữ liệu Task có sẵn từ Room — không sinh dữ liệu mẫu, không hard-code id.
 * Khác `TaskAdapter` ở chỗ đây là danh sách **chọn một**, nên không có checkbox hoàn thành
 * và không có menu tuỳ chọn.
 */
class PomodoroTaskAdapter(
    private val onTaskClick: (Task) -> Unit
) : ListAdapter<Task, PomodoroTaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var selectedTaskId: Long? = null

    /**
     * Đánh dấu công việc đang được chọn. Danh sách trong sheet rất ngắn và chỉ đổi khi
     * người dùng mở lại sheet, nên refresh toàn bộ là đủ và đơn giản hơn diff theo payload.
     */
    fun setSelectedTaskId(taskId: Long?) {
        if (selectedTaskId == taskId) return
        selectedTaskId = taskId
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemPomodoroTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemPomodoroTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            binding.tvTaskTitle.text = task.title
            binding.priorityStripe.setBackgroundColor(priorityColor(task.priority))
            binding.ivSelected.visibility =
                if (task.id.toLong() == selectedTaskId) View.VISIBLE else View.INVISIBLE

            binding.tvTaskMeta.text = PomodoroTaskFormatter.dueDateLabel(
                binding.root.context,
                task
            )
            // Công việc đã xong vẫn chọn được nhưng được làm mờ để dễ phân biệt.
            binding.root.alpha = if (task.isCompleted) COMPLETED_ALPHA else 1f

            binding.root.setOnClickListener { onTaskClick(task) }

            binding.root.contentDescription = context.getString(
                R.string.pomodoro_task_label
            ) + ": " + task.title
        }

        private fun priorityColor(priority: Priority): Int = ContextCompat.getColor(
            binding.root.context,
            when (priority) {
                Priority.URGENT -> R.color.priority_urgent
                Priority.HIGH -> R.color.priority_high
                Priority.MEDIUM -> R.color.priority_medium
                Priority.LOW -> R.color.priority_low
            }
        )
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }

    private companion object {
        const val COMPLETED_ALPHA = 0.55f
    }
}
