package com.team.taskmanagementapp.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.model.streak.DayStreakInfo
import com.team.taskmanagementapp.data.model.streak.StreakDayStatus
import com.team.taskmanagementapp.databinding.ItemStreakDayBinding

class StreakWeekAdapter : ListAdapter<DayStreakInfo, StreakWeekAdapter.StreakDayViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreakDayViewHolder {
        val binding = ItemStreakDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StreakDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StreakDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StreakDayViewHolder(private val binding: ItemStreakDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayStreakInfo) {
            binding.tvDayLabel.text = item.dayLabel
            binding.tvDayNumber.text = item.dayOfMonth.toString()

            when (item.status) {
                StreakDayStatus.COMPLETED -> {
                    binding.dayCircleContainer.setBackgroundResource(R.drawable.bg_streak_day_completed)
                    binding.ivDayStatusIcon.visibility = View.VISIBLE
                    binding.ivDayStatusIcon.setImageResource(R.drawable.ic_fire)
                    binding.tvDayNumber.visibility = View.GONE
                    binding.tvDayLabel.setTextColor(Color.parseColor("#C05621"))
                }
                StreakDayStatus.TODAY_IN_PROGRESS -> {
                    binding.dayCircleContainer.setBackgroundResource(R.drawable.bg_streak_day_today)
                    binding.ivDayStatusIcon.visibility = View.GONE
                    binding.tvDayNumber.visibility = View.VISIBLE
                    binding.tvDayNumber.setTextColor(Color.parseColor("#105CDB"))
                    binding.tvDayLabel.setTextColor(Color.parseColor("#105CDB"))
                }
                StreakDayStatus.FAILED -> {
                    binding.dayCircleContainer.setBackgroundResource(R.drawable.bg_streak_day_inactive)
                    binding.ivDayStatusIcon.visibility = View.GONE
                    binding.tvDayNumber.visibility = View.VISIBLE
                    binding.tvDayNumber.setTextColor(Color.parseColor("#9CA3AF"))
                    binding.tvDayLabel.setTextColor(Color.parseColor("#737786"))
                }
                StreakDayStatus.FUTURE -> {
                    binding.dayCircleContainer.setBackgroundResource(R.drawable.bg_streak_day_inactive)
                    binding.ivDayStatusIcon.visibility = View.GONE
                    binding.tvDayNumber.visibility = View.VISIBLE
                    binding.tvDayNumber.setTextColor(Color.parseColor("#D1D5DB"))
                    binding.tvDayLabel.setTextColor(Color.parseColor("#9CA3AF"))
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DayStreakInfo>() {
            override fun areItemsTheSame(oldItem: DayStreakInfo, newItem: DayStreakInfo): Boolean {
                return oldItem.dateMillis == newItem.dateMillis
            }

            override fun areContentsTheSame(oldItem: DayStreakInfo, newItem: DayStreakInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
